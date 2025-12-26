package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.KeyEvent;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.utils.ModifierKeyState;

public final class ModifierKeyStateHandler {

  public interface Host {
    void toggleCaseOfSelectedCharacters();

    void handleShift();

    void handleControl();

    void handleAlt();

    void handleFunction();

    void updateShiftStateNow();

    void updateVoiceKeyState();
  }

  @NonNull private final Host host;
  @NonNull private final InputConnectionRouter inputConnectionRouter;
  @NonNull private final ModifierKeyState shiftKeyState;
  @NonNull private final ModifierKeyState controlKeyState;
  @NonNull private final ModifierKeyState altKeyState;
  @NonNull private final ModifierKeyState functionKeyState;
  @NonNull private final ModifierKeyState voiceKeyState;

  public ModifierKeyStateHandler(
      @NonNull Host host,
      @NonNull InputConnectionRouter inputConnectionRouter,
      @NonNull ModifierKeyState shiftKeyState,
      @NonNull ModifierKeyState controlKeyState,
      @NonNull ModifierKeyState altKeyState,
      @NonNull ModifierKeyState functionKeyState,
      @NonNull ModifierKeyState voiceKeyState) {
    this.host = host;
    this.inputConnectionRouter = inputConnectionRouter;
    this.shiftKeyState = shiftKeyState;
    this.controlKeyState = controlKeyState;
    this.altKeyState = altKeyState;
    this.functionKeyState = functionKeyState;
    this.voiceKeyState = voiceKeyState;
  }

  public void onPress(int primaryCode) {
    final int normalizedPrimaryCode =
        primaryCode == KeyCodes.CTRL_LOCK ? KeyCodes.CTRL : primaryCode;

    if (primaryCode == KeyCodes.SHIFT) {
      shiftKeyState.onPress();
      host.toggleCaseOfSelectedCharacters();
      host.handleShift();
    } else if (!isStickyModifier(primaryCode)) {
      shiftKeyState.onOtherKeyPressed();
    }

    if (normalizedPrimaryCode == KeyCodes.CTRL) {
      controlKeyState.onPress();
      host.handleControl();
      if (primaryCode == KeyCodes.CTRL) {
        inputConnectionRouter.sendKeyDown(113); // KeyEvent.KEYCODE_CTRL_LEFT (API 11 and up)
      }
    } else if (!isStickyModifier(primaryCode)) {
      controlKeyState.onOtherKeyPressed();
    }

    if (primaryCode == KeyCodes.ALT_MODIFIER) {
      altKeyState.onPress();
      host.handleAlt();
      inputConnectionRouter.sendKeyDown(KeyEvent.KEYCODE_ALT_LEFT);
    } else if (!isStickyModifier(primaryCode)) {
      altKeyState.onOtherKeyPressed();
    }

    if (primaryCode == KeyCodes.FUNCTION) {
      functionKeyState.onPress();
      host.handleFunction();
    } else if (!isStickyModifier(primaryCode)) {
      functionKeyState.onOtherKeyPressed();
    }
  }

  public void onRelease(int primaryCode, int multiTapTimeout, int longPressTimeout) {
    final int normalizedPrimaryCode =
        primaryCode == KeyCodes.CTRL_LOCK ? KeyCodes.CTRL : primaryCode;
    if (primaryCode == KeyCodes.SHIFT) {
      shiftKeyState.onRelease(multiTapTimeout, longPressTimeout);
      host.handleShift();
    } else if (!isStickyModifier(primaryCode)) {
      if (shiftKeyState.onOtherKeyReleased()) {
        host.updateShiftStateNow();
      }
    }

    if (normalizedPrimaryCode == KeyCodes.CTRL) {
      if (primaryCode == KeyCodes.CTRL) {
        inputConnectionRouter.sendKeyUp(113); // KeyEvent.KEYCODE_CTRL_LEFT
      }
      controlKeyState.onRelease(multiTapTimeout, longPressTimeout);
    } else if (!isStickyModifier(primaryCode)) {
      controlKeyState.onOtherKeyReleased();
    }

    if (primaryCode == KeyCodes.ALT_MODIFIER) {
      inputConnectionRouter.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
      altKeyState.onRelease(multiTapTimeout, longPressTimeout);
      host.handleAlt();
    } else if (!isStickyModifier(primaryCode)) {
      if (altKeyState.onOtherKeyReleased()) {
        host.handleAlt();
      }
    }

    if (primaryCode == KeyCodes.FUNCTION) {
      functionKeyState.onRelease(multiTapTimeout, longPressTimeout);
      host.handleFunction();
    } else if (!isStickyModifier(primaryCode) && functionKeyState.onOtherKeyReleased()) {
      host.handleFunction();
    }

    if (primaryCode == KeyCodes.VOICE_INPUT) {
      voiceKeyState.onRelease(multiTapTimeout, longPressTimeout);
      host.updateVoiceKeyState();
    } else {
      voiceKeyState.onOtherKeyReleased();
    }
    host.handleControl();
    host.handleAlt();
    host.handleFunction();
  }

  private static boolean isStickyModifier(int primaryCode) {
    return switch (primaryCode) {
      case KeyCodes.SHIFT,
          KeyCodes.SHIFT_LOCK,
          KeyCodes.CTRL,
          KeyCodes.CTRL_LOCK,
          KeyCodes.ALT,
          KeyCodes.ALT_MODIFIER,
          KeyCodes.FUNCTION ->
          true;
      default -> false;
    };
  }
}
