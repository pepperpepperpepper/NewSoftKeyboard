package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.SystemClock;

/** Tracks whether we're waiting for a selection update after composing changes. */
final class SelectionExpectationTracker {
  private final long neverTimeStamp;
  private long expectingSelectionUpdateBy;
  private boolean selectionUpdateReceived;

  SelectionExpectationTracker(long neverTimeStamp) {
    this.neverTimeStamp = neverTimeStamp;
    this.expectingSelectionUpdateBy = neverTimeStamp;
    this.selectionUpdateReceived = false;
  }

  void markExpectingUntil(long timeStamp) {
    expectingSelectionUpdateBy = timeStamp;
    selectionUpdateReceived = false;
  }

  void clear() {
    expectingSelectionUpdateBy = neverTimeStamp;
    selectionUpdateReceived = false;
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
    selectionUpdateReceived = false;
  }

  void markSelectionUpdateReceived() {
    selectionUpdateReceived = true;
  }

  boolean isSelectionUpdatePending() {
    return isExpecting() && !selectionUpdateReceived;
  }
}
