package com.anysoftkeyboard.ime;

/** Tracks whether we're waiting for a selection update after composing changes. */
final class SelectionExpectationTracker {
  private final long neverTimeStamp;
  private long expectingSelectionUpdateBy;

  SelectionExpectationTracker(long neverTimeStamp) {
    this.neverTimeStamp = neverTimeStamp;
    this.expectingSelectionUpdateBy = neverTimeStamp;
  }

  void markExpectingUntil(long timeStamp) {
    expectingSelectionUpdateBy = timeStamp;
  }

  void clear() {
    expectingSelectionUpdateBy = neverTimeStamp;
  }

  boolean isExpecting() {
    return expectingSelectionUpdateBy > 0;
  }

  long getExpectingSelectionUpdateBy() {
    return expectingSelectionUpdateBy;
  }

  void setExpectingSelectionUpdateBy(long value) {
    expectingSelectionUpdateBy = value;
  }
}
