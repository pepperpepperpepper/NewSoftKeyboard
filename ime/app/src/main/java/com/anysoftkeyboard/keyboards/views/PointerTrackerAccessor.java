/*
 * Copyright (c) 2025 The NewSoftKeyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.keyboards.views;

import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Wraps pointer tracker lookups and listener/keyboard wiring. */
class PointerTrackerAccessor {
  private final PointerTrackerRegistry registry;

  PointerTrackerAccessor(PointerTrackerRegistry registry) {
    this.registry = registry;
  }

  @Nullable
  PointerTracker get(
      int id, @Nullable Keyboard.Key[] keys, @Nullable OnKeyboardActionListener listener) {
    final PointerTracker tracker = registry.get(id);
    wire(tracker, keys, listener);
    return tracker;
  }

  PointerTracker getForMotionEvent(
      @NonNull MotionEvent motionEvent,
      @Nullable Keyboard.Key[] keys,
      @Nullable OnKeyboardActionListener listener) {
    final int index = motionEvent.getActionIndex();
    final int id = motionEvent.getPointerId(index);
    return get(id, keys, listener);
  }

  void forEach(TrackerConsumer consumer) {
    registry.forEach(consumer::accept);
  }

  void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
    registry.forEach(tracker -> tracker.setOnKeyboardActionListener(listener));
  }

  private void wire(
      PointerTracker tracker,
      @Nullable Keyboard.Key[] keys,
      @Nullable OnKeyboardActionListener listener) {
    if (keys != null) {
      tracker.setKeyboard(keys);
    }
    if (listener != null) {
      tracker.setOnKeyboardActionListener(listener);
    }
  }

  interface TrackerConsumer {
    void accept(PointerTracker tracker);
  }
}
