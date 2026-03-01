package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.SystemClock;

/** Tracks whether we're waiting for a selection update after composing changes. */
final class SelectionExpectationTracker {
  private static final int DEFAULT_EXPECTED_SELECTION_UPDATES = 1;

  private final long neverTimeStamp;
  private long expectingSelectionUpdateBy;
  private int remainingExpectedSelectionUpdates;

  SelectionExpectationTracker(long neverTimeStamp) {
    this.neverTimeStamp = neverTimeStamp;
    this.expectingSelectionUpdateBy = neverTimeStamp;
    this.remainingExpectedSelectionUpdates = 0;
  }

  void markExpectingUntil(long timeStamp) {
    markExpectingUntil(timeStamp, DEFAULT_EXPECTED_SELECTION_UPDATES);
  }

  void markExpectingUntil(long timeStamp, int expectedSelectionUpdates) {
    expectingSelectionUpdateBy = timeStamp;
    remainingExpectedSelectionUpdates = Math.max(0, expectedSelectionUpdates);
  }

  void clear() {
    expectingSelectionUpdateBy = neverTimeStamp;
    remainingExpectedSelectionUpdates = 0;
  }

  boolean isExpecting() {
    final long until = expectingSelectionUpdateBy;
    return until > 0 && SystemClock.uptimeMillis() < until;
  }

  long getExpectingSelectionUpdateBy() {
    return expectingSelectionUpdateBy;
  }

  void setExpectingSelectionUpdateBy(long value) {
    markExpectingUntil(value, DEFAULT_EXPECTED_SELECTION_UPDATES);
  }

  void markSelectionUpdateReceived() {
    if (remainingExpectedSelectionUpdates > 0) {
      remainingExpectedSelectionUpdates--;
    }
  }

  boolean isSelectionUpdatePending() {
    return isExpecting() && remainingExpectedSelectionUpdates > 0;
  }
}
