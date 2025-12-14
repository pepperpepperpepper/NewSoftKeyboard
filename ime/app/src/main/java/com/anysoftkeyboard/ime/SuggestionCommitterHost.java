package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
/** Host adapter for {@link SuggestionCommitter} to live outside the service class. */
final class SuggestionCommitterHost implements SuggestionCommitter.Host {
  private final AnySoftKeyboardSuggestions host;

  SuggestionCommitterHost(AnySoftKeyboardSuggestions host) {
    this.host = host;
  }

  @Override
  public InputConnection currentInputConnection() {
    return host.mInputConnectionRouter.current();
  }

  @Override
  public boolean isSelectionUpdateDelayed() {
    return host.isSelectionUpdateDelayed();
  }

  @Override
  public void markExpectingSelectionUpdate() {
    host.markExpectingSelectionUpdate();
  }

  @Override
  public int getCursorPosition() {
    return host.getCursorPosition();
  }

  @Override
  public void clearSuggestions() {
    host.clearSuggestions();
  }
}
