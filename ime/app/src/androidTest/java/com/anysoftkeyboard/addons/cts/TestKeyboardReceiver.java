package com.anysoftkeyboard.addons.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Minimal test receiver that exposes keyboard add-on metadata for both the NewSoftKeyboard and
 * legacy AnySoftKeyboard intent namespaces. The receiver itself is inert; discovery happens through
 * the manifest meta-data.
 */
public class TestKeyboardReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    // No-op: discovery relies on manifest meta-data only.
  }
}
