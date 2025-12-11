package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.keyboards.AnyKeyboard;

final class SwipeDistanceCalculator {

  private final SwipeConfiguration swipeConfiguration;

  SwipeDistanceCalculator(SwipeConfiguration swipeConfiguration) {
    this.swipeConfiguration = swipeConfiguration;
  }

  void recomputeForKeyboard(AnyKeyboard keyboard) {
    swipeConfiguration.recomputeForKeyboard(keyboard);
  }

  int getSwipeVelocityThreshold() {
    return swipeConfiguration.getSwipeVelocityThreshold();
  }

  int getSwipeXDistanceThreshold() {
    return swipeConfiguration.getSwipeXDistanceThreshold();
  }

  int getSwipeSpaceXDistanceThreshold() {
    return swipeConfiguration.getSwipeSpaceXDistanceThreshold();
  }

  int getSwipeYDistanceThreshold() {
    return swipeConfiguration.getSwipeYDistanceThreshold();
  }

  void setSwipeXDistanceThreshold(int threshold) {
    swipeConfiguration.setSwipeXDistanceThreshold(threshold);
    swipeConfiguration.recomputeForKeyboard(null);
  }

  void setSwipeVelocityThreshold(int threshold) {
    swipeConfiguration.setSwipeVelocityThreshold(threshold);
  }
}
