package com.anysoftkeyboard;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class TerminalKeySender {

  private TerminalKeySender() {}

  static boolean isTerminalEmulation(@Nullable EditorInfo editorInfo) {
    if (editorInfo == null) return false;

    switch (editorInfo.packageName) {
      case "org.connectbot":
      case "org.woltage.irssiconnectbot":
      case "com.pslib.connectbot":
      case "com.sonelli.juicessh":
        return editorInfo.inputType == 0;
      default:
        return false;
    }
  }

  static void sendTab(@NonNull InputConnection inputConnection, boolean terminalEmulation) {
    // Note: tab and ^I don't work in ConnectBot, hackish workaround
    if (terminalEmulation) {
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_I));
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_I));
    } else {
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
    }
  }

  static void sendEscape(
      @NonNull InputConnection inputConnection,
      boolean terminalEmulation,
      @NonNull Runnable sendEscapeChar) {
    if (terminalEmulation) {
      sendEscapeChar.run();
    } else {
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, 111 /* KEYCODE_ESCAPE */));
      inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, 111 /* KEYCODE_ESCAPE */));
    }
  }
}

