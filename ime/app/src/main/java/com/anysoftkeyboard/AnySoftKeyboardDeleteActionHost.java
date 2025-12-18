package com.anysoftkeyboard;

import androidx.annotation.NonNull;

final class AnySoftKeyboardDeleteActionHost implements DeleteActionHelper.Host {

  @NonNull private final AnySoftKeyboard ime;

  AnySoftKeyboardDeleteActionHost(@NonNull AnySoftKeyboard ime) {
    this.ime = ime;
  }

  @Override
  public boolean isPredictionOn() {
    return ime.isPredictionOnForDeleteActionHelperHost();
  }

  @Override
  public int getCursorPosition() {
    return ime.getCursorPositionForDeleteActionHelperHost();
  }

  @Override
  public boolean isSelectionUpdateDelayed() {
    return ime.isSelectionUpdateDelayedForDeleteActionHelperHost();
  }

  @Override
  public void markExpectingSelectionUpdate() {
    ime.markExpectingSelectionUpdateForDeleteActionHelperHost();
  }

  @Override
  public void postUpdateSuggestions() {
    ime.postUpdateSuggestionsForDeleteActionHelperHost();
  }

  @Override
  public void sendDownUpKeyEvents(int keyCode) {
    ime.sendDownUpKeyEventsForDeleteActionHelperHost(keyCode);
  }
}
