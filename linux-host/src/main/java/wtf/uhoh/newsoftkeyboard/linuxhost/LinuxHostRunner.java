package wtf.uhoh.newsoftkeyboard.linuxhost;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.EditorAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.adapters.TextOutputBackend;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.FileSystemKeyboardPackLoader;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.session.KeyboardSession;
import wtf.uhoh.newsoftkeyboard.linuxhost.output.StdoutJsonTextOutputBackend;
import wtf.uhoh.newsoftkeyboard.linuxhost.output.UnixSocketJsonTextOutputBackend;
import wtf.uhoh.newsoftkeyboard.linuxhost.output.XdotoolTextOutputBackend;

final class LinuxHostRunner {

  private LinuxHostRunner() {}

  static void runInteractive(Path packDir, String keyboardId, OutputConfig outputConfig)
      throws IOException {
    KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(packDir);
    PackEntry keyboardEntry = selectKeyboard(pack, keyboardId);

    KeyboardModel model;
    try (InputStream inputStream = pack.source().open(keyboardEntry.path().value())) {
      model = AskXmlKeyboardParser.parse(inputStream);
    }

    var session = new KeyboardSession(model);
    SwingUtilities.invokeLater(
        () -> createAndShowUi(packDir, keyboardEntry.id(), model, session, outputConfig));
  }

  static void runSmoke(Path packDir, String keyboardId, OutputConfig outputConfig, String text)
      throws IOException {
    KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(packDir);
    PackEntry keyboardEntry = selectKeyboard(pack, keyboardId);

    KeyboardModel model;
    try (InputStream inputStream = pack.source().open(keyboardEntry.path().value())) {
      model = AskXmlKeyboardParser.parse(inputStream);
    }

    var session = new KeyboardSession(model);
    TextOutputBackend backend = createExternalBackend(outputConfig);
    if (backend == null) backend = new StdoutJsonTextOutputBackend();

    for (int i = 0; i < text.length(); i++) {
      String token = String.valueOf(text.charAt(i));
      var position =
          findKeyByLabel(model, token)
              .orElseThrow(
                  () ->
                      new IOException(
                          "No key found with label '"
                              + token
                              + "' in keyboard "
                              + keyboardEntry.id()));
      try {
        backend.apply(session.pressKey(position.rowIndex(), position.keyIndex()));
      } catch (RuntimeException e) {
        if (outputConfig.mode() == OutputMode.IBUS) {
          throw new IOException(formatIbusOutputError(outputConfig.ibusSocketPath(), e), e);
        }
        throw e;
      }
    }
  }

  private static Optional<KeyPosition> findKeyByLabel(KeyboardModel model, String label) {
    String needle = label.trim();
    if (needle.isEmpty()) return Optional.empty();

    List<KeyboardRow> rows = model.rows();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      List<KeySpec> keys = rows.get(rowIndex).keys();
      for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
        String rawLabel = keys.get(keyIndex).label();
        if (rawLabel == null) continue;
        String keyValue = rawLabel.trim();
        if (keyValue.isEmpty()) continue;
        if (keyValue.equals(needle) || keyValue.equalsIgnoreCase(needle)) {
          return Optional.of(new KeyPosition(rowIndex, keyIndex));
        }
      }
    }
    return Optional.empty();
  }

  private static PackEntry selectKeyboard(KeyboardPack pack, String keyboardId) throws IOException {
    List<PackEntry> keyboards = pack.manifest().keyboards();
    if (keyboards.isEmpty()) {
      throw new IOException("Pack has no keyboards: " + pack.manifest().id());
    }

    if (keyboardId == null || keyboardId.trim().isEmpty()) return keyboards.get(0);

    for (PackEntry entry : keyboards) {
      if (keyboardId.equals(entry.id())) return entry;
    }

    throw new IOException(
        "Keyboard id not found: '"
            + keyboardId
            + "'. Available: "
            + keyboards.stream().map(PackEntry::id).toList());
  }

  private static void createAndShowUi(
      Path packDir,
      String keyboardId,
      KeyboardModel model,
      KeyboardSession session,
      OutputConfig outputConfig) {
    JFrame frame = new JFrame("NewSoftKeyboard (Linux host dev) — " + keyboardId);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    if (outputConfig.mode() == OutputMode.IBUS) {
      frame.setFocusableWindowState(false);
      frame.setAlwaysOnTop(true);
    }

    JTextArea editor = new JTextArea(5, 60);
    editor.setEditable(false);
    frame.add(new JScrollPane(editor), BorderLayout.CENTER);

    var externalBackendRef = new AtomicReference<>(createExternalBackend(outputConfig));
    ExecutorService externalExecutor =
        outputConfig.mode() == OutputMode.XDOTOOL
            ? Executors.newSingleThreadExecutor(r -> new Thread(r, "xdotool-output"))
            : null;

    JPanel keyboardPanel = new JPanel();
    keyboardPanel.setLayout(new javax.swing.BoxLayout(keyboardPanel, javax.swing.BoxLayout.Y_AXIS));

    List<KeyboardRow> rows = model.rows();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      KeyboardRow row = rows.get(rowIndex);
      JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

      List<KeySpec> keys = row.keys();
      for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
        KeySpec key = keys.get(keyIndex);
        JButton button = new JButton(displayLabel(key));
        final int finalRowIndex = rowIndex;
        final int finalKeyIndex = keyIndex;
        button.addActionListener(
            e -> {
              List<SemanticAction> actions = session.pressKey(finalRowIndex, finalKeyIndex);
              // Always echo locally (helps debug even when outputting elsewhere).
              applyActions(editor, actions);
              TextOutputBackend externalBackend = externalBackendRef.get();
              if (externalBackend == null) return;
              if (externalExecutor != null) {
                var ignored =
                    externalExecutor.submit(
                        () -> {
                          try {
                            externalBackend.apply(actions);
                          } catch (RuntimeException ex) {
                            System.err.println(ex.getMessage());
                          }
                        });
              } else {
                try {
                  externalBackend.apply(actions);
                } catch (RuntimeException ex) {
                  String message = formatIbusOutputError(outputConfig.ibusSocketPath(), ex);
                  editor.append("\n\nERROR: " + message + "\n");
                  System.err.println(message);
                  externalBackendRef.set(null);
                }
              }
            });
        rowPanel.add(button);
      }

      keyboardPanel.add(rowPanel);
    }

    frame.add(keyboardPanel, BorderLayout.SOUTH);
    frame.pack();
    frame.setLocationRelativeTo(null);
    if (outputConfig.mode() == OutputMode.IBUS && outputConfig.ibusActivation()) {
      System.err.println(
          "IBus activation enabled: window will show/hide based on the focused text field.");
      boolean activationListenerStarted =
          startIbusActivationListener(
              outputConfig.ibusControlSocketPath(),
              active ->
                  SwingUtilities.invokeLater(
                      () -> {
                        frame.setVisible(active);
                        if (active) frame.toFront();
                      }));
      if (!activationListenerStarted) {
        System.err.println("Unable to bind IBus activation socket; leaving the window visible.");
        frame.setVisible(true);
      } else if (!requestIbusActivationStatus(outputConfig.ibusSocketPath())) {
        System.err.println(
            "Unable to query IBus engine status; leaving the window visible for debugging.");
        frame.setVisible(true);
      } else {
        frame.setVisible(false);
      }
    } else {
      frame.setVisible(true);
    }

    editor.append("Loaded pack: " + packDir + "\n");
    editor.append("Keyboard id: " + keyboardId + "\n\n");
    editor.append("Output mode: " + outputConfig.mode().id() + "\n");
    outputConfig.xdotoolWindowId().ifPresent(id -> editor.append("xdotool window: " + id + "\n"));
    if (outputConfig.mode() == OutputMode.IBUS) {
      editor.append("ibus socket: " + outputConfig.ibusSocketPath() + "\n");
      editor.append("ibus control socket: " + outputConfig.ibusControlSocketPath() + "\n");
      editor.append("ibus activation: " + outputConfig.ibusActivation() + "\n");
    }
    if (outputConfig.mode() == OutputMode.XDOTOOL) {
      editor.append("Tip: capture a target window id with: xdotool getactivewindow\n");
    }
    editor.append("\n");

    if (externalExecutor != null) {
      frame.addWindowListener(
          new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
              shutdownExecutor(externalExecutor);
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
              shutdownExecutor(externalExecutor);
            }
          });
    }
  }

  private static void shutdownExecutor(ExecutorService executor) {
    executor.shutdownNow();
    try {
      executor.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static boolean startIbusActivationListener(
      Path socketPath, Consumer<Boolean> onActiveChanged) {
    Objects.requireNonNull(socketPath);
    Objects.requireNonNull(onActiveChanged);
    ServerSocketChannel server;
    try {
      Files.createDirectories(socketPath.getParent());
      Files.deleteIfExists(socketPath);
      UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
      server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
      server.bind(address);
    } catch (IOException e) {
      System.err.println("Failed binding ibus control socket: " + socketPath + ": " + e);
      return false;
    }
    Thread thread =
        new Thread(
            () -> runIbusActivationServer(server, onActiveChanged), "ibus-activation-listener");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private static void runIbusActivationServer(
      ServerSocketChannel server, Consumer<Boolean> onActiveChanged) {
    try (ServerSocketChannel serverChannel = server) {
      while (true) {
        try (SocketChannel client = server.accept()) {
          if (client == null) continue;
          try (BufferedReader reader =
              new BufferedReader(
                  new InputStreamReader(Channels.newInputStream(client), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
              parseIbusControlLine(line).ifPresent(onActiveChanged);
            }
          }
        } catch (IOException e) {
          // Continue accepting new connections; this is a best-effort control channel.
        }
      }
    } catch (IOException e) {
      // Best-effort channel; if it dies the OSK still works without activation.
    }
  }

  private static String formatIbusOutputError(Path socketPath, RuntimeException exception) {
    Objects.requireNonNull(socketPath);
    StringBuilder builder = new StringBuilder();
    builder.append("IBus output failed. Unable to reach engine socket: ").append(socketPath);
    builder.append("\n");
    builder.append("Ensure the NewSoftKeyboard IBus engine is running and registered.");
    builder.append("\n");
    builder.append("Override the socket with NSK_IBUS_SOCKET=/path/to/ibus.sock.");
    String cause = exception.getMessage();
    if (cause != null && !cause.isBlank()) {
      builder.append("\n");
      builder.append("Cause: ").append(cause);
    }
    return builder.toString();
  }

  private static Optional<Boolean> parseIbusControlLine(String line) {
    if (line == null) return Optional.empty();
    String compact = compactWhitespace(line);
    if (compact.contains("\"type\":\"activate\"")) return Optional.of(true);
    if (compact.contains("\"type\":\"deactivate\"")) return Optional.of(false);
    return Optional.empty();
  }

  private static String compactWhitespace(String raw) {
    StringBuilder builder = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (Character.isWhitespace(c)) continue;
      builder.append(c);
    }
    return builder.toString();
  }

  private static boolean requestIbusActivationStatus(Path ibusSocketPath) {
    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(ibusSocketPath);
    try (SocketChannel channel = SocketChannel.open(address)) {
      String payload = "{\"type\":\"status\"}\n";
      channel.write(StandardCharsets.UTF_8.encode(payload));
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private static TextOutputBackend createExternalBackend(OutputConfig outputConfig) {
    return switch (outputConfig.mode()) {
      case EDITOR -> null;
      case STDOUT -> new StdoutJsonTextOutputBackend();
      case XDOTOOL ->
          new XdotoolTextOutputBackend(
              outputConfig.xdotoolWindowId(), outputConfig.xdotoolDelayMs());
      case IBUS -> new UnixSocketJsonTextOutputBackend(outputConfig.ibusSocketPath());
    };
  }

  private static String displayLabel(KeySpec keySpec) {
    String rawLabel = keySpec.label();
    if (rawLabel != null) {
      String label = rawLabel.trim();
      if (!label.isEmpty()) return label;
    }

    Integer primaryNumeric = primaryNumericCode(keySpec);
    if (primaryNumeric != null) {
      int code = primaryNumeric;
      return switch (code) {
        case -1 -> "⇧";
        case -5 -> "⌫";
        default -> Integer.toString(code);
      };
    }

    return "<?>"; // no label, no codes
  }

  private static Integer primaryNumericCode(KeySpec keySpec) {
    for (KeyCode code : keySpec.codes()) {
      Integer numeric = code.asNumeric();
      if (numeric != null) return numeric;
    }
    return null;
  }

  private static void applyActions(JTextArea editor, List<SemanticAction> actions) {
    for (SemanticAction action : actions) {
      if (action instanceof SemanticAction.CommitText commitText) {
        editor.append(commitText.text());
      } else if (action instanceof SemanticAction.DeleteBackward deleteBackward) {
        deleteBackward(editor, deleteBackward.count());
      } else if (action instanceof SemanticAction.DeleteForward) {
        // no-op for this demo
      } else if (action instanceof SemanticAction.PerformEditorAction editorAction) {
        applyEditorAction(editor, editorAction.action());
      }
    }
  }

  private static void deleteBackward(JTextArea editor, int count) {
    String text = editor.getText();
    if (text.isEmpty() || count <= 0) return;
    int newLength = Math.max(0, text.length() - count);
    editor.setText(text.substring(0, newLength));
  }

  private static void applyEditorAction(JTextArea editor, EditorAction action) {
    switch (action) {
      case ENTER -> editor.append("\n");
      case TAB -> editor.append("\t");
      case NEXT, DONE -> editor.append("\n");
    }
  }

  private record KeyPosition(int rowIndex, int keyIndex) {}
}
