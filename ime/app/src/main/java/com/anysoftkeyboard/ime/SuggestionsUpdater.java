package com.anysoftkeyboard.ime;

import android.os.Handler;
import android.os.Message;
import com.anysoftkeyboard.base.utils.Logger;
import java.lang.ref.WeakReference;

/** Handles delayed scheduling of suggestion updates via the keyboard UI handler. */
final class SuggestionsUpdater implements Handler.Callback {

  interface Host {
    void performUpdateSuggestions();
  }

  private static final String TAG = "SuggestionsUpdater";

  private final WeakReference<Host> hostRef;
  private final Handler handler;
  private final long delayMs;
  private final int messageWhat;

  SuggestionsUpdater(Handler handler, Host host, long delayMs, int messageWhat) {
    this.handler = handler;
    this.hostRef = new WeakReference<>(host);
    this.delayMs = delayMs;
    this.messageWhat = messageWhat;
  }

  void postUpdateSuggestions() {
    handler.removeMessages(messageWhat);
    handler.sendEmptyMessageDelayed(messageWhat, delayMs);
  }

  void cancel() {
    handler.removeMessages(messageWhat);
  }

  @Override
  public boolean handleMessage(Message msg) {
    if (msg.what != messageWhat) return false;
    Host host = hostRef.get();
    if (host == null) {
      Logger.w(TAG, "Host lost; skipping suggestions update");
      return true;
    }
    host.performUpdateSuggestions();
    return true;
  }
}
