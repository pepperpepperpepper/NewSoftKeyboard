package com.anysoftkeyboard.keyboards.views;

/**
 * Tracks whether the current pointer interaction should be treated as a gesture-typing path.
 *
 * <p>This is view-layer state only and is consumed by {@link PointerTracker}. It intentionally does
 * not know about listeners or key dispatch.
 */
final class GestureTypingPathTracker {

  // A non-positive value means gesture typing is not active/started for this pointer.
  // A value of 1 means "gesture typing was started", but is not considered "in gesture typing" yet.
  // A value >1 means we have moved across keys and are now "in gesture typing".
  private int keyCodesInPathLength = -1;

  void reset() {
    keyCodesInPathLength = -1;
  }

  void start() {
    keyCodesInPathLength = 1;
  }

  void markAdditionalKeyVisited() {
    keyCodesInPathLength++;
  }

  boolean isInGestureTyping() {
    return keyCodesInPathLength > 1;
  }

  boolean canDoGestureTyping() {
    return keyCodesInPathLength >= 1;
  }
}
