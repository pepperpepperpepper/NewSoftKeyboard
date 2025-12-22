package com.anysoftkeyboard.keyboards.views;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;

final class CandidateStripScrollController {

  private static final int SCROLL_PIXELS = 20;

  private boolean scrolled;
  private int targetScrollX;
  private int totalWidth;

  @NonNull
  GestureDetector.SimpleOnGestureListener createGestureListener(
      int touchSlop, @NonNull View candidateView) {
    return new CandidateStripGestureListener(touchSlop, candidateView);
  }

  int totalWidth() {
    return totalWidth;
  }

  void setTotalWidth(int totalWidth) {
    this.totalWidth = totalWidth;
  }

  boolean isScrolled() {
    return scrolled;
  }

  void setTargetScrollX(int targetScrollX) {
    this.targetScrollX = targetScrollX;
  }

  boolean shouldScrollToTarget(int currentScrollX) {
    return targetScrollX != currentScrollX;
  }

  void scrollToTarget(@NonNull View candidateView) {
    int scrollX = candidateView.getScrollX();
    if (targetScrollX > scrollX) {
      scrollX += SCROLL_PIXELS;
      if (scrollX >= targetScrollX) {
        scrollX = targetScrollX;
        candidateView.scrollTo(scrollX, candidateView.getScrollY());
        candidateView.requestLayout();
      } else {
        candidateView.scrollTo(scrollX, candidateView.getScrollY());
      }
    } else {
      scrollX -= SCROLL_PIXELS;
      if (scrollX <= targetScrollX) {
        scrollX = targetScrollX;
        candidateView.scrollTo(scrollX, candidateView.getScrollY());
        candidateView.requestLayout();
      } else {
        candidateView.scrollTo(scrollX, candidateView.getScrollY());
      }
    }
    candidateView.invalidate();
  }

  private final class CandidateStripGestureListener
      extends GestureDetector.SimpleOnGestureListener {
    private final int touchSlopSquare;
    @NonNull private final View candidateView;

    CandidateStripGestureListener(int touchSlop, @NonNull View candidateView) {
      this.touchSlopSquare = touchSlop * touchSlop;
      this.candidateView = candidateView;
    }

    @Override
    public boolean onDown(MotionEvent e) {
      scrolled = false;
      return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      if (!scrolled) {
        // This is applied only when we recognize that scrolling is starting.
        final int deltaX = (int) (e2.getX() - e1.getX());
        final int deltaY = (int) (e2.getY() - e1.getY());
        final int distance = (deltaX * deltaX) + (deltaY * deltaY);
        if (distance < touchSlopSquare) {
          return true;
        }
        scrolled = true;
      }

      final int width = candidateView.getWidth();
      scrolled = true;
      int scrollX = candidateView.getScrollX();
      scrollX += (int) distanceX;
      if (scrollX < 0) {
        scrollX = 0;
      }
      if (distanceX > 0 && scrollX + width > totalWidth) {
        scrollX -= (int) distanceX;
      }
      targetScrollX = scrollX;
      candidateView.scrollTo(scrollX, candidateView.getScrollY());
      candidateView.invalidate();
      return true;
    }
  }
}
