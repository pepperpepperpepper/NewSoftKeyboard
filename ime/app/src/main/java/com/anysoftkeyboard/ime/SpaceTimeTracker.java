package com.anysoftkeyboard.ime;

import android.os.SystemClock;

/** Tracks timing of the last space press to support double-space period and space-swap logic. */
final class SpaceTimeTracker {
  static final long NEVER = -1L * 365L * 24L * 60L * 60L * 1000L; // a year ago.

  private long lastSpaceTimestamp = NEVER;

  void markSpace() {
    lastSpaceTimestamp = SystemClock.uptimeMillis();
  }

  void clear() {
    lastSpaceTimestamp = NEVER;
  }

  boolean hadSpace() {
    return lastSpaceTimestamp != NEVER;
  }

  boolean isDoubleSpace(long timeoutMillis) {
    final long now = SystemClock.uptimeMillis();
    return hadSpace() && (now - lastSpaceTimestamp) < timeoutMillis;
  }
}
