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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import wtf.uhoh.newsoftkeyboard.linuxhost.fs.XdgPaths;
import wtf.uhoh.newsoftkeyboard.linuxhost.output.StdoutJsonTextOutputBackend;
import wtf.uhoh.newsoftkeyboard.linuxhost.output.UnixSocketJsonTextOutputBackend;
import wtf.uhoh.newsoftkeyboard.linuxhost.output.XdotoolTextOutputBackend;
import wtf.uhoh.newsoftkeyboard.linuxhost.packs.LinuxPackRepository;
import wtf.uhoh.newsoftkeyboard.linuxhost.prefs.FilePrefsStore;

public final class LinuxHostMain {
  private static final String APP_ID = "newsoftkeyboard";

  private LinuxHostMain() {}

  public static void main(String[] args) throws Exception {
    Map<String, String> env = System.getenv();
    Path packsRoot = defaultPacksRoot(env);
    Path prefsFile = defaultPrefsFile(env);
    var prefs = FilePrefsStore.load(prefsFile);
    var packRepository = new LinuxPackRepository(packsRoot);

    if (args.length == 0) {
      printUsage(packsRoot, prefsFile);
      System.exit(2);
      return;
    }

    // Backwards-compatible positional args: <pack-dir-or-id> [keyboard-id]
    if (!args[0].startsWith("--")) {
      var parsed = parseRunArgs(packRepository, prefs, args);
      runInteractive(parsed.packDir(), parsed.keyboardId(), parsed.outputConfig());
      return;
    }

    switch (args[0]) {
      case "--list-packs" -> listPacks(packRepository);
      case "--install-pack" -> {
        if (args.length < 2) {
          System.err.println("Missing argument: --install-pack <pack-dir>");
          System.exit(2);
          return;
        }
        boolean force = Arrays.asList(args).contains("--force");
        boolean allowInvalid = Arrays.asList(args).contains("--allow-invalid");
        installPack(
            packRepository, Path.of(args[1]).toAbsolutePath().normalize(), force, allowInvalid);
      }
      case "--run" -> {
        if (args.length < 2) {
          System.err.println("Missing argument: --run <pack-dir-or-id> [keyboard-id]");
          System.exit(2);
          return;
        }
        var parsed = parseRunArgs(packRepository, prefs, Arrays.copyOfRange(args, 1, args.length));
        runInteractive(parsed.packDir(), parsed.keyboardId(), parsed.outputConfig());
      }
      case "--smoke" -> {
        if (args.length < 2) {
          System.err.println("Missing argument: --smoke <pack-dir-or-id> [keyboard-id]");
          System.exit(2);
          return;
        }
        var parsed =
            parseSmokeArgs(packRepository, prefs, Arrays.copyOfRange(args, 1, args.length));
        runSmoke(parsed.packDir(), parsed.keyboardId(), parsed.outputConfig(), parsed.text());
      }
      default -> {
        printUsage(packsRoot, prefsFile);
        System.exit(2);
      }
    }
  }

  private static Path defaultPacksRoot(Map<String, String> environment) {
    String override = environment.get("NSK_PACKS_DIR");
    if (override != null && !override.isBlank()) {
      return Paths.get(override).toAbsolutePath().normalize();
    }
    return XdgPaths.dataHome(environment)
        .resolve(APP_ID)
        .resolve("packs")
        .toAbsolutePath()
        .normalize();
  }

  private static void printUsage(Path packsRoot) {
    System.err.println("NewSoftKeyboard Linux dev host");
    System.err.println();
    System.err.println("Commands:");
    System.err.println("  --list-packs");
    System.err.println("  --install-pack <pack-dir> [--force] [--allow-invalid]");
    System.err.println(
        "  --run <pack-dir-or-id> [keyboard-id] [--output=<editor|stdout|xdotool|ibus>]");
    System.err.println("       [--xdotool-window=<window-id>] [--xdotool-delay-ms=<n>]");
    System.err.println(
        "       [--ibus-socket=<socket-path>] [--ibus-control-socket=<socket-path>]"
            + " [--no-ibus-activation]");
    System.err.println(
        "  --smoke <pack-dir-or-id> [keyboard-id] [--text=<text>] [--output=<stdout|xdotool>]");
    System.err.println();
    System.err.println("Config keys (prefs file):");
    System.err.println("  output.mode=editor|stdout|xdotool|ibus");
    System.err.println("  xdotool.window=<window-id>");
    System.err.println("  xdotool.delay_ms=<integer>");
    System.err.println("  ibus.socket=<socket-path>");
    System.err.println("  ibus.control_socket=<socket-path>");
    System.err.println("  ibus.activation=true|false");
    System.err.println();
    System.err.println("Legacy usage (positional args):");
    System.err.println("  <pack-dir-or-id> [keyboard-id]");
    System.err.println();
    System.err.println("Default packs root: " + packsRoot);
    System.err.println("Override with NSK_PACKS_DIR=/path/to/packs");
    System.err.println("Override ibus socket with NSK_IBUS_SOCKET=/path/to/ibus.sock");
    System.err.println(
        "Override ibus control socket with NSK_IBUS_CONTROL_SOCKET=/path/to/ibus.control.sock");
  }

  private static Path defaultPrefsFile(Map<String, String> environment) {
    String override = environment.get("NSK_PREFS_FILE");
    if (override != null && !override.isBlank()) {
      return Paths.get(override).toAbsolutePath().normalize();
    }
    return XdgPaths.configHome(environment)
        .resolve(APP_ID)
        .resolve("prefs.properties")
        .toAbsolutePath()
        .normalize();
  }

  private static Path defaultIbusSocketPath(Map<String, String> environment) {
    String override = environment.get("NSK_IBUS_SOCKET");
    if (override != null && !override.isBlank()) {
      return Paths.get(override).toAbsolutePath().normalize();
    }
    return XdgPaths.runtimeDir(environment)
        .resolve(APP_ID)
        .resolve("ibus.sock")
        .toAbsolutePath()
        .normalize();
  }

  private static Path deriveIbusControlSocketPath(Path ibusSocketPath) {
    Objects.requireNonNull(ibusSocketPath);
    String fileName = ibusSocketPath.getFileName().toString();
    if (fileName.endsWith(".sock")) {
      return ibusSocketPath.resolveSibling(
          fileName.substring(0, fileName.length() - 4) + ".control.sock");
    }
    return ibusSocketPath.resolveSibling(fileName + ".control");
  }

  private static void printUsage(Path packsRoot, Path prefsFile) {
    printUsage(packsRoot);
    System.err.println("Default prefs file: " + prefsFile);
    System.err.println("Override with NSK_PREFS_FILE=/path/to/prefs.properties");
  }

  private static void listPacks(LinuxPackRepository packRepository) throws IOException {
    List<LinuxPackRepository.InstalledPack> packs = packRepository.listInstalledPacks();
    if (packs.isEmpty()) {
      System.out.println("No packs installed in " + packRepository.packsRoot());
      return;
    }
    System.out.println("Installed packs (" + packRepository.packsRoot() + "):");
    for (LinuxPackRepository.InstalledPack pack : packs) {
      System.out.println("- " + pack.id() + " (" + pack.name() + "): " + pack.dir());
    }
  }

  private static void installPack(
      LinuxPackRepository packRepository, Path sourceDir, boolean force, boolean allowInvalid)
      throws IOException {
    var result = packRepository.installPack(sourceDir, force, allowInvalid);
    if (!result.success()) {
      System.err.println(result.error().orElse("Unknown install error"));
      System.exit(1);
      return;
    }
    System.out.println("Installed pack at: " + result.installedDir().orElseThrow());
  }

  private static RunArgs parseRunArgs(
      LinuxPackRepository packRepository, FilePrefsStore prefs, String[] args) throws IOException {
    OutputMode outputMode =
        prefs.getString("output.mode").map(OutputMode::parseUnchecked).orElse(OutputMode.EDITOR);
    String xdotoolWindow = prefs.getString("xdotool.window").orElse(null);
    int xdotoolDelayMs = prefs.getInt("xdotool.delay_ms", 0);
    boolean ibusActivation = prefs.getBoolean("ibus.activation", true);
    Path ibusSocketPath =
        prefs
            .getString("ibus.socket")
            .map(raw -> Paths.get(raw).toAbsolutePath().normalize())
            .orElse(null);
    if (ibusSocketPath == null) ibusSocketPath = defaultIbusSocketPath(System.getenv());
    Path ibusControlSocketPath =
        prefs
            .getString("ibus.control_socket")
            .map(raw -> Paths.get(raw).toAbsolutePath().normalize())
            .orElse(null);
    if (ibusControlSocketPath == null) {
      String controlOverride = System.getenv().get("NSK_IBUS_CONTROL_SOCKET");
      if (controlOverride != null && !controlOverride.isBlank()) {
        ibusControlSocketPath = Paths.get(controlOverride).toAbsolutePath().normalize();
      }
    }
    if (ibusControlSocketPath == null)
      ibusControlSocketPath = deriveIbusControlSocketPath(ibusSocketPath);

    var positional = new ArrayList<String>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--output=")) {
        outputMode = OutputMode.parse(arg.substring("--output=".length()));
      } else if (arg.equals("--output")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --output");
        outputMode = OutputMode.parse(args[++i]);
      } else if (arg.startsWith("--xdotool-window=")) {
        xdotoolWindow = arg.substring("--xdotool-window=".length()).trim();
      } else if (arg.equals("--xdotool-window")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --xdotool-window");
        xdotoolWindow = args[++i].trim();
      } else if (arg.startsWith("--xdotool-delay-ms=")) {
        xdotoolDelayMs =
            parseIntArg("--xdotool-delay-ms", arg.substring("--xdotool-delay-ms=".length()));
      } else if (arg.equals("--xdotool-delay-ms")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --xdotool-delay-ms");
        xdotoolDelayMs = parseIntArg("--xdotool-delay-ms", args[++i]);
      } else if (arg.startsWith("--ibus-socket=")) {
        ibusSocketPath =
            Paths.get(arg.substring("--ibus-socket=".length())).toAbsolutePath().normalize();
      } else if (arg.equals("--ibus-socket")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --ibus-socket");
        ibusSocketPath = Paths.get(args[++i]).toAbsolutePath().normalize();
      } else if (arg.startsWith("--ibus-control-socket=")) {
        ibusControlSocketPath =
            Paths.get(arg.substring("--ibus-control-socket=".length()))
                .toAbsolutePath()
                .normalize();
      } else if (arg.equals("--ibus-control-socket")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --ibus-control-socket");
        ibusControlSocketPath = Paths.get(args[++i]).toAbsolutePath().normalize();
      } else if (arg.equals("--ibus-activation")) {
        ibusActivation = true;
      } else if (arg.equals("--no-ibus-activation")) {
        ibusActivation = false;
      } else if (arg.startsWith("--")) {
        throw new IOException("Unknown option: " + arg);
      } else {
        positional.add(arg);
      }
    }

    if (positional.isEmpty()) {
      throw new IOException("Missing required argument: <pack-dir-or-id>");
    }
    if (positional.size() > 2) {
      throw new IOException(
          "Too many positional arguments. Expected: <pack-dir-or-id> [keyboard-id]");
    }

    Path packDir = packRepository.resolvePackDir(positional.get(0));
    String keyboardId = positional.size() == 2 ? positional.get(1) : null;
    return new RunArgs(
        packDir,
        keyboardId,
        new OutputConfig(
            outputMode,
            Optional.ofNullable(xdotoolWindow).filter(s -> !s.isBlank()),
            xdotoolDelayMs,
            ibusSocketPath,
            ibusControlSocketPath,
            ibusActivation));
  }

  private static SmokeArgs parseSmokeArgs(
      LinuxPackRepository packRepository, FilePrefsStore prefs, String[] args) throws IOException {
    String text = "abc";
    var remainingArgs = new ArrayList<String>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--text=")) {
        text = arg.substring("--text=".length());
      } else if (arg.equals("--text")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --text");
        text = args[++i];
      } else {
        remainingArgs.add(arg);
      }
    }

    RunArgs parsed = parseRunArgs(packRepository, prefs, remainingArgs.toArray(String[]::new));
    return new SmokeArgs(parsed.packDir(), parsed.keyboardId(), parsed.outputConfig(), text);
  }

  private static int parseIntArg(String flagName, String raw) throws IOException {
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new IOException("Invalid integer for " + flagName + ": " + raw, e);
    }
  }

  private static void runInteractive(Path packDir, String keyboardId, OutputConfig outputConfig)
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

  private static void runSmoke(
      Path packDir, String keyboardId, OutputConfig outputConfig, String text) throws IOException {
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
      backend.apply(session.pressKey(position.rowIndex(), position.keyIndex()));
    }
  }

  private static Optional<KeyPosition> findKeyByLabel(KeyboardModel model, String label) {
    String needle = label.trim();
    if (needle.isEmpty()) return Optional.empty();

    List<KeyboardRow> rows = model.rows();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      List<KeySpec> keys = rows.get(rowIndex).keys();
      for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
        Optional<String> keyLabel =
            keys.get(keyIndex).label().map(String::trim).filter(s -> !s.isEmpty());
        if (keyLabel.isEmpty()) continue;
        String keyValue = keyLabel.get();
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

    if (keyboardId == null || keyboardId.isBlank()) return keyboards.get(0);

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

    TextOutputBackend externalBackend = createExternalBackend(outputConfig);
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
              if (externalBackend == null) return;
              if (externalExecutor != null) {
                var ignored = externalExecutor.submit(() -> externalBackend.apply(actions));
              } else {
                externalBackend.apply(actions);
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
      startIbusActivationListener(
          outputConfig.ibusControlSocketPath(),
          active ->
              SwingUtilities.invokeLater(
                  () -> {
                    frame.setVisible(active);
                    if (active) frame.toFront();
                  }));
      if (!requestIbusActivationStatus(outputConfig.ibusSocketPath())) {
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

  private static void startIbusActivationListener(
      Path socketPath, Consumer<Boolean> onActiveChanged) {
    Objects.requireNonNull(socketPath);
    Objects.requireNonNull(onActiveChanged);
    Thread thread =
        new Thread(
            () -> runIbusActivationServer(socketPath, onActiveChanged), "ibus-activation-listener");
    thread.setDaemon(true);
    thread.start();
  }

  private static void runIbusActivationServer(Path socketPath, Consumer<Boolean> onActiveChanged) {
    try {
      Files.createDirectories(socketPath.getParent());
      Files.deleteIfExists(socketPath);
    } catch (IOException e) {
      System.err.println("Failed preparing ibus control socket path: " + socketPath + ": " + e);
      return;
    }

    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
    try (ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
      server.bind(address);
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
      System.err.println("Failed binding ibus control socket: " + socketPath + ": " + e);
    }
  }

  private static Optional<Boolean> parseIbusControlLine(String line) {
    if (line == null) return Optional.empty();
    String raw = line.trim();
    if (raw.isEmpty()) return Optional.empty();
    String compact = compactWhitespace(raw);
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
    Optional<String> label = keySpec.label().map(String::trim).filter(s -> !s.isEmpty());
    if (label.isPresent()) return label.get();

    OptionalInt primaryNumeric = primaryNumericCode(keySpec);
    if (primaryNumeric.isPresent()) {
      int code = primaryNumeric.getAsInt();
      return switch (code) {
        case -1 -> "⇧";
        case -5 -> "⌫";
        default -> Integer.toString(code);
      };
    }

    return "<?>"; // no label, no codes
  }

  private static OptionalInt primaryNumericCode(KeySpec keySpec) {
    for (KeyCode code : keySpec.codes()) {
      OptionalInt numeric = code.asNumeric();
      if (numeric.isPresent()) return numeric;
    }
    return OptionalInt.empty();
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

  private enum OutputMode {
    EDITOR("editor"),
    STDOUT("stdout"),
    XDOTOOL("xdotool"),
    IBUS("ibus");

    private final String id;

    OutputMode(String id) {
      this.id = id;
    }

    String id() {
      return id;
    }

    static OutputMode parse(String raw) throws IOException {
      String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
      for (OutputMode mode : values()) {
        if (mode.id.equals(value)) return mode;
      }
      throw new IOException("Unsupported output mode: " + raw);
    }

    static OutputMode parseUnchecked(String raw) {
      if (raw == null) return EDITOR;
      String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
      for (OutputMode mode : values()) {
        if (mode.id.equals(value)) return mode;
      }
      return EDITOR;
    }
  }

  private record OutputConfig(
      OutputMode mode,
      Optional<String> xdotoolWindowId,
      int xdotoolDelayMs,
      Path ibusSocketPath,
      Path ibusControlSocketPath,
      boolean ibusActivation) {
    private OutputConfig {
      Objects.requireNonNull(mode);
      Objects.requireNonNull(xdotoolWindowId);
      Objects.requireNonNull(ibusSocketPath);
      Objects.requireNonNull(ibusControlSocketPath);
    }
  }

  private record RunArgs(Path packDir, String keyboardId, OutputConfig outputConfig) {
    private RunArgs {
      Objects.requireNonNull(packDir);
      Objects.requireNonNull(outputConfig);
    }
  }

  private record SmokeArgs(
      Path packDir, String keyboardId, OutputConfig outputConfig, String text) {
    private SmokeArgs {
      Objects.requireNonNull(packDir);
      Objects.requireNonNull(outputConfig);
      Objects.requireNonNull(text);
    }
  }

  private record KeyPosition(int rowIndex, int keyIndex) {}
}
