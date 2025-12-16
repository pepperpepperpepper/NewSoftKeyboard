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

/** Updates swipe thresholds and triggers recomputation on the host. */
class SwipeThresholdApplier {
  interface Host {
    void recalculateSwipeDistances();
  }

  private final SwipeConfiguration swipeConfiguration;
  private final Host host;

  SwipeThresholdApplier(SwipeConfiguration swipeConfiguration, Host host) {
    this.swipeConfiguration = swipeConfiguration;
    this.host = host;
  }

  void setSwipeXDistanceThreshold(int threshold) {
    swipeConfiguration.setSwipeXDistanceThreshold(threshold);
    host.recalculateSwipeDistances();
  }

  void setSwipeVelocityThreshold(int threshold) {
    swipeConfiguration.setSwipeVelocityThreshold(threshold);
  }

  void setSwipeYDistanceThreshold(int threshold) {
    swipeConfiguration.setSwipeYDistanceThreshold(threshold);
  }
}
