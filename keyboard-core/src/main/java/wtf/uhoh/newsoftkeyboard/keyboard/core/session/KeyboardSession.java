package wtf.uhoh.newsoftkeyboard.keyboard.core.session;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    return Collections.emptyList();
  }

  public List<SemanticAction> pressKey(int rowIndex, int keyIndex) {
    KeySpec keySpec = keyboard.rows().get(rowIndex).keys().get(keyIndex);

    Integer primaryCode = primaryNumericCode(keySpec);
    if (primaryCode != null) {
      int code = primaryCode;
      if (code == CODE_SHIFT) {
        shiftOnce = !shiftOnce;
        return Collections.emptyList();
      }
      if (code == CODE_DELETE) {
        shiftOnce = false;
        return Collections.singletonList(new SemanticAction.DeleteBackward(1));
      }
    }

    var label = keySpec.label();
    if (label != null && !label.isEmpty()) {
      String commit = shiftOnce ? applyShift(label) : label;
      shiftOnce = false;
      return Collections.singletonList(new SemanticAction.CommitText(commit));
    }

    shiftOnce = false;
    return Collections.emptyList();
  }

  private static Integer primaryNumericCode(KeySpec keySpec) {
    for (KeyCode code : keySpec.codes()) {
      Integer numeric = code.asNumeric();
      if (numeric != null) return numeric;
    }
    return null;
  }

  private static String applyShift(String label) {
    if (label.length() == 1) return label.toUpperCase(Locale.ROOT);
    return label;
  }

  public List<SemanticAction> performEditorAction(EditorAction action) {
    shiftOnce = false;
    return Collections.singletonList(new SemanticAction.PerformEditorAction(action));
  }
}
