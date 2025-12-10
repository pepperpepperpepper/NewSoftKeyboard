package com.anysoftkeyboard.keyboards.views;

import android.util.SparseArray;
import androidx.annotation.NonNull;

/**
 * Small helper that owns {@link PointerTracker} instances for a view.
 * Centralizes creation and iteration so touch plumbing can shrink over time.
 */
final class PointerTrackerRegistry {

  interface Factory {
    @NonNull
    PointerTracker create(int pointerId);
  }

  interface Consumer {
    void accept(@NonNull PointerTracker tracker);
  }

  private final SparseArray<PointerTracker> trackers = new SparseArray<>();
  private final Factory factory;

  PointerTrackerRegistry(@NonNull Factory factory) {
    this.factory = factory;
  }

  @NonNull
  PointerTracker get(int id) {
    PointerTracker tracker = trackers.get(id);
    if (tracker == null) {
      tracker = factory.create(id);
      trackers.put(id, tracker);
    }
    return tracker;
  }

  void forEach(@NonNull Consumer consumer) {
    for (int i = 0, size = trackers.size(); i < size; i++) {
      consumer.accept(trackers.valueAt(i));
    }
  }

  int size() {
    return trackers.size();
  }
}
