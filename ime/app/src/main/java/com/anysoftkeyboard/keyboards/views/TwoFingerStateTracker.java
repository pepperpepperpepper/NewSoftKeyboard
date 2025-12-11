package com.anysoftkeyboard.keyboards.views;

final class TwoFingerStateTracker {
  private final long lingerMs;
  private long lastTimeHadTwoFingers = 0;

  TwoFingerStateTracker(long lingerMs) {
    this.lingerMs = lingerMs;
  }

  void markTwoFingers() {
    lastTimeHadTwoFingers = android.os.SystemClock.elapsedRealtime();
  }

  boolean isAtTwoFingersState() {
    return android.os.SystemClock.elapsedRealtime() - lastTimeHadTwoFingers < lingerMs;
  }
}
