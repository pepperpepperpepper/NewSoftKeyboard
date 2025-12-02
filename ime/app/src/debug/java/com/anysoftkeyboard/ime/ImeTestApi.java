package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import java.lang.ref.WeakReference;

/** Debug-only tiny API to help instrumentation seed IME context. */
public final class ImeTestApi {
  private static volatile WeakReference<AnySoftKeyboardSuggestions> sService =
      new WeakReference<>(null);

  private ImeTestApi() {}

  static void setService(AnySoftKeyboardSuggestions svc) {
    sService = new WeakReference<>(svc);
  }

  public static boolean commitText(String text) {
    final AnySoftKeyboardSuggestions svc = sService.get();
    if (svc == null) return false;
    final InputConnection ic = svc.getCurrentInputConnection();
    if (ic == null) return false;
    ic.commitText(text, 1);
    // ask for suggestions update after commit
    svc.performUpdateSuggestions();
    return true;
  }
}

