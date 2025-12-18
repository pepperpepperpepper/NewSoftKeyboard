package com.anysoftkeyboard;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.ime.ModifierKeyStateHandler;

final class AnySoftKeyboardModifierKeyStateHost implements ModifierKeyStateHandler.Host {

  @NonNull private final AnySoftKeyboard keyboard;

  AnySoftKeyboardModifierKeyStateHost(@NonNull AnySoftKeyboard keyboard) {
    this.keyboard = keyboard;
  }

  @Override
  public void toggleCaseOfSelectedCharacters() {
    keyboard.toggleCaseOfSelectedCharacters();
  }

  @Override
  public void handleShift() {
    keyboard.handleShift();
  }

  @Override
  public void handleControl() {
    keyboard.handleControl();
  }

  @Override
  public void handleAlt() {
    keyboard.handleAlt();
  }

  @Override
  public void handleFunction() {
    keyboard.handleFunction();
  }

  @Override
  public void updateShiftStateNow() {
    keyboard.updateShiftStateNow();
  }

  @Override
  public void updateVoiceKeyState() {
    keyboard.updateVoiceKeyState();
  }
}

