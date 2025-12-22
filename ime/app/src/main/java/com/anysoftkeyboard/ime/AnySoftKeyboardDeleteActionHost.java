package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.AnySoftKeyboard;

public final class AnySoftKeyboardDeleteActionHost implements DeleteActionHelper.Host {

  @NonNull private final AnySoftKeyboard ime;

  public AnySoftKeyboardDeleteActionHost(@NonNull AnySoftKeyboard ime) {
    this.ime = ime;
  }

  @Override
  public boolean isPredictionOn() {
    return ime.isPredictionOn();
  }

  @Override
  public int getCursorPosition() {
    return ime.getCursorPosition();
  }

  @Override
  public boolean isSelectionUpdateDelayed() {
    return ime.isSelectionUpdateDelayed();
  }

  @Override
  public void markExpectingSelectionUpdate() {
    ime.markExpectingSelectionUpdate();
  }

  @Override
  public void postUpdateSuggestions() {
    ime.postUpdateSuggestions();
  }

  @Override
  public void sendDownUpKeyEvents(int keyCode) {
    ime.sendDownUpKeyEvents(keyCode);
  }
}
