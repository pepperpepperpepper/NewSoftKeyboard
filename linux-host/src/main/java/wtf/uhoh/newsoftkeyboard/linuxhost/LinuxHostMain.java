package wtf.uhoh.newsoftkeyboard.linuxhost;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.FileSystemKeyboardPackLoader;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.session.KeyboardSession;

public final class LinuxHostMain {
  private LinuxHostMain() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: :linux-host:run --args=\"<pack-dir> [keyboard-id]\"");
      System.exit(2);
      return;
    }

    Path packDir = Paths.get(args[0]).toAbsolutePath().normalize();
    String keyboardId = args.length >= 2 ? args[1] : null;

    KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(packDir);
    PackEntry keyboardEntry = selectKeyboard(pack, keyboardId);

    KeyboardModel model;
    try (InputStream inputStream = pack.source().open(keyboardEntry.path().value())) {
      model = AskXmlKeyboardParser.parse(inputStream);
    }

    var session = new KeyboardSession(model);
    SwingUtilities.invokeLater(() -> createAndShowUi(packDir, keyboardEntry.id(), model, session));
  }

  private static PackEntry selectKeyboard(KeyboardPack pack, String keyboardId) throws IOException {
    List<PackEntry> keyboards = pack.manifest().keyboards();
    if (keyboards.isEmpty()) {
      throw new IOException("Pack has no keyboards: " + pack.manifest().id());
    }

    if (keyboardId == null || keyboardId.isBlank()) return keyboards.getFirst();

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
      Path packDir, String keyboardId, KeyboardModel model, KeyboardSession session) {
    JFrame frame = new JFrame("NewSoftKeyboard (Linux host dev) — " + keyboardId);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    JTextArea editor = new JTextArea(5, 60);
    editor.setEditable(false);
    frame.add(new JScrollPane(editor), BorderLayout.CENTER);

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
            e -> applyActions(editor, session.pressKey(finalRowIndex, finalKeyIndex)));
        rowPanel.add(button);
      }

      keyboardPanel.add(rowPanel);
    }

    frame.add(keyboardPanel, BorderLayout.SOUTH);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    editor.append("Loaded pack: " + packDir + "\n");
    editor.append("Keyboard id: " + keyboardId + "\n\n");
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
}
