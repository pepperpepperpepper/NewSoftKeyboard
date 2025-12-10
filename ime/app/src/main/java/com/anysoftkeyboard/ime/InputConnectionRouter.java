package com.anysoftkeyboard.ime;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import java.util.function.Supplier;

/**
 * Lightweight helper that centralizes safe access to the current InputConnection.
 *
 * This keeps null checks and common operations (key events, batch edits) in one place so
 * AnySoftKeyboard can shrink over time without behavior changes.
 */
public final class InputConnectionRouter {

  private final Supplier<InputConnection> connectionProvider;

  public InputConnectionRouter(Supplier<InputConnection> connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public InputConnection current() {
    return connectionProvider.get();
  }

  public void sendKeyDown(int keyCode) {
    InputConnection ic = current();
    if (ic != null) {
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    }
  }

  public void sendKeyUp(int keyCode) {
    InputConnection ic = current();
    if (ic != null) {
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }
  }

  public void beginBatchEdit() {
    InputConnection ic = current();
    if (ic != null) {
      ic.beginBatchEdit();
    }
  }

  public void endBatchEdit() {
    InputConnection ic = current();
    if (ic != null) {
      ic.endBatchEdit();
    }
  }
}
