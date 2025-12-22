package com.anysoftkeyboard.keyboards.physical;

public interface HardKeyboardAction {
  int getKeyCode();

  boolean isAltActive();

  boolean isShiftActive();

  void setNewKeyCode(int keyCode);
}
