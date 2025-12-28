package wtf.uhoh.newsoftkeyboard.keyboard.core.session;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.EditorAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;

public final class KeyboardSession {
  private static final int CODE_SHIFT = -1;
  private static final int CODE_DELETE = -5;

  private final KeyboardModel keyboard;

  private boolean shiftOnce;

  public KeyboardSession(KeyboardModel keyboard) {
    this.keyboard = keyboard;
  }

  public List<SemanticAction> handle(CoreInputEvent inputEvent) {
    if (inputEvent instanceof CoreInputEvent.KeyPress keyPress) {
      return pressKey(keyPress.rowIndex(), keyPress.keyIndex());
    }
    return List.of();
  }

  public List<SemanticAction> pressKey(int rowIndex, int keyIndex) {
    KeySpec keySpec = keyboard.rows().get(rowIndex).keys().get(keyIndex);

    OptionalInt primaryCode = primaryNumericCode(keySpec);
    if (primaryCode.isPresent()) {
      int code = primaryCode.getAsInt();
      if (code == CODE_SHIFT) {
        shiftOnce = !shiftOnce;
        return List.of();
      }
      if (code == CODE_DELETE) {
        shiftOnce = false;
        return List.of(new SemanticAction.DeleteBackward(1));
      }
    }

    var label = keySpec.label().orElse("");
    if (!label.isEmpty()) {
      String commit = shiftOnce ? applyShift(label) : label;
      shiftOnce = false;
      return List.of(new SemanticAction.CommitText(commit));
    }

    shiftOnce = false;
    return List.of();
  }

  private static OptionalInt primaryNumericCode(KeySpec keySpec) {
    for (KeyCode code : keySpec.codes()) {
      OptionalInt numeric = code.asNumeric();
      if (numeric.isPresent()) return numeric;
    }
    return OptionalInt.empty();
  }

  private static String applyShift(String label) {
    if (label.length() == 1) return label.toUpperCase(Locale.ROOT);
    return label;
  }

  public List<SemanticAction> performEditorAction(EditorAction action) {
    shiftOnce = false;
    return List.of(new SemanticAction.PerformEditorAction(action));
  }
}
