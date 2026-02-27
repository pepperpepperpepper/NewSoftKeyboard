package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.SystemClock;

/** Tracks whether we're waiting for a selection update after composing changes. */
final class SelectionExpectationTracker {
  private static final int DEFAULT_EXPECTED_SELECTION_UPDATES = 3;

  private final long neverTimeStamp;
  private long expectingSelectionUpdateBy;
  private int remainingExpectedSelectionUpdates;

  SelectionExpectationTracker(long neverTimeStamp) {
    this.neverTimeStamp = neverTimeStamp;
    this.expectingSelectionUpdateBy = neverTimeStamp;
    this.remainingExpectedSelectionUpdates = 0;
  }

  void markExpectingUntil(long timeStamp) {
    expectingSelectionUpdateBy = timeStamp;
    remainingExpectedSelectionUpdates = DEFAULT_EXPECTED_SELECTION_UPDATES;
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
    expectingSelectionUpdateBy = value;
    remainingExpectedSelectionUpdates = DEFAULT_EXPECTED_SELECTION_UPDATES;
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
