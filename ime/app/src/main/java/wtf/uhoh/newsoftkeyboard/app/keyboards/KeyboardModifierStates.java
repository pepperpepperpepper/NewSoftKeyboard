package wtf.uhoh.newsoftkeyboard.app.keyboards;

final class KeyboardModifierStates {

  private static final int STICKY_KEY_OFF = 0;
  private static final int STICKY_KEY_ON = 1;
  private static final int STICKY_KEY_LOCKED = 2;

  private int shiftState = STICKY_KEY_OFF;
  private int controlState = STICKY_KEY_OFF;
  private int voiceState = STICKY_KEY_OFF;
  private int altState = STICKY_KEY_OFF;
  private int functionState = STICKY_KEY_OFF;

  void resetAltAndFunction() {
    altState = STICKY_KEY_OFF;
    functionState = STICKY_KEY_OFF;
  }

  boolean setShiftLocked(boolean shiftLocked) {
    final int initialState = shiftState;
    if (shiftLocked) {
      shiftState = STICKY_KEY_LOCKED;
    } else if (shiftState == STICKY_KEY_LOCKED) {
      shiftState = STICKY_KEY_ON;
    }
    return initialState != shiftState;
  }

  boolean isShifted() {
    return shiftState != STICKY_KEY_OFF;
  }

  boolean setShifted(boolean shiftState) {
    final int initialState = this.shiftState;
    if (shiftState) {
      if (this.shiftState == STICKY_KEY_OFF) {
        this.shiftState = STICKY_KEY_ON;
      }
    } else {
      this.shiftState = STICKY_KEY_OFF;
    }
    return this.shiftState != initialState;
  }

  boolean isShiftLocked() {
    return shiftState == STICKY_KEY_LOCKED;
  }

  boolean isControl() {
    return controlState != STICKY_KEY_OFF;
  }

  boolean setControl(boolean control) {
    final int initialState = controlState;
    if (control) {
      if (controlState == STICKY_KEY_OFF) {
        controlState = STICKY_KEY_ON;
      }
    } else {
      controlState = STICKY_KEY_OFF;
    }
    return controlState != initialState;
  }

  boolean isControlActive() {
    return controlState != STICKY_KEY_OFF;
  }

  boolean setAlt(boolean active, boolean locked) {
    final int initialState = altState;
    if (active) {
      if (locked) {
        altState = STICKY_KEY_LOCKED;
      } else if (altState == STICKY_KEY_OFF) {
        altState = STICKY_KEY_ON;
      }
    } else {
      altState = STICKY_KEY_OFF;
    }
    return altState != initialState;
  }

  boolean isAltActive() {
    return altState != STICKY_KEY_OFF;
  }

  boolean isAltLocked() {
    return altState == STICKY_KEY_LOCKED;
  }

  boolean setFunction(boolean active, boolean locked) {
    final int initialState = functionState;
    if (active) {
      if (locked) {
        functionState = STICKY_KEY_LOCKED;
      } else if (functionState == STICKY_KEY_OFF) {
        functionState = STICKY_KEY_ON;
      }
    } else {
      functionState = STICKY_KEY_OFF;
    }
    return functionState != initialState;
  }

  boolean isFunctionActive() {
    return functionState != STICKY_KEY_OFF;
  }

  boolean isFunctionLocked() {
    return functionState == STICKY_KEY_LOCKED;
  }

  boolean setVoice(boolean active, boolean locked) {
    final int initialState = voiceState;
    if (active) {
      if (locked) {
        voiceState = STICKY_KEY_LOCKED;
      } else if (voiceState == STICKY_KEY_OFF) {
        voiceState = STICKY_KEY_ON;
      }
    } else {
      voiceState = STICKY_KEY_OFF;
    }
    return voiceState != initialState;
  }

  boolean isVoiceActive() {
    return voiceState != STICKY_KEY_OFF;
  }

  boolean isVoiceLocked() {
    return voiceState == STICKY_KEY_LOCKED;
  }
}
