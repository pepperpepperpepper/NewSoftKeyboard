package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.Nullable;

public final class TerminalKeySender {

  private TerminalKeySender() {}

  public static boolean isTerminalEmulation(@Nullable EditorInfo editorInfo) {
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

  public static void sendTab(
      InputConnectionRouter inputConnectionRouter, boolean terminalEmulation) {
    if (!inputConnectionRouter.hasConnection()) {
      return;
    }
    // Note: tab and ^I don't work in ConnectBot, hackish workaround
    if (terminalEmulation) {
      inputConnectionRouter.sendKeyEvent(
          new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
      inputConnectionRouter.sendKeyEvent(
          new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
      inputConnectionRouter.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_I));
      inputConnectionRouter.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_I));
    } else {
      inputConnectionRouter.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
      inputConnectionRouter.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
    }
  }

  public static void sendEscape(
      InputConnectionRouter inputConnectionRouter,
      boolean terminalEmulation,
      Runnable sendEscapeChar) {
    if (!inputConnectionRouter.hasConnection()) {
      return;
    }
    if (terminalEmulation) {
      sendEscapeChar.run();
    } else {
      inputConnectionRouter.sendKeyEvent(
          new KeyEvent(KeyEvent.ACTION_DOWN, 111 /* KEYCODE_ESCAPE */));
      inputConnectionRouter.sendKeyEvent(
          new KeyEvent(KeyEvent.ACTION_UP, 111 /* KEYCODE_ESCAPE */));
    }
  }
}
