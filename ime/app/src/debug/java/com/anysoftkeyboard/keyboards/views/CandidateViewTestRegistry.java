package com.anysoftkeyboard.keyboards.views;

import java.lang.ref.WeakReference;

/**
 * Debug-only helper so instrumentation can request candidate picks inside the app process.
 */
public final class CandidateViewTestRegistry {
  private static volatile WeakReference<CandidateView> sActive = new WeakReference<>(null);

  private CandidateViewTestRegistry() {}

  static void setActive(CandidateView view) {
    sActive = new WeakReference<>(view);
  }

  public static boolean pickByIndex(final int index) {
    final CandidateView v = sActive.get();
    if (v == null) return false;
    v.post(() -> v.pickCandidateAtForTest(index));
    return true;
  }
}

