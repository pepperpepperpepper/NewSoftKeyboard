package com.anysoftkeyboard.ime;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;

final class SoftInputWindowLayoutUpdater {

  private SoftInputWindowLayoutUpdater() {}

  static void update(
      Window window,
      boolean isFullscreenMode,
      @Nullable KeyboardViewContainerView inputViewContainer) {
    // Override layout parameters to expand SoftInputWindow to the entire screen.
    // See InputMethodService#setInputView(View) and
    // SoftInputWindow#updateWidthHeight(WindowManager.LayoutParams).
    updateLayoutHeightOf(window, ViewGroup.LayoutParams.MATCH_PARENT);

    // This method may be called before InputMethodService#setInputView(View).
    if (inputViewContainer == null) {
      return;
    }

    // In non-fullscreen mode, InputView and its parent inputArea should expand to the entire screen
    // and be placed at the bottom of SoftInputWindow.
    // In fullscreen mode, these shouldn't expand to the entire screen and should be coexistent with
    // extracted area above.
    // See InputMethodService#setInputView(View) and com.android.internal.R.layout.input_method.xml.
    final View inputArea = window.findViewById(android.R.id.inputArea);

    updateLayoutHeightOf(
        (View) inputArea.getParent(),
        isFullscreenMode
            ? ViewGroup.LayoutParams.MATCH_PARENT
            : ViewGroup.LayoutParams.WRAP_CONTENT);
    updateLayoutGravityOf((View) inputArea.getParent(), Gravity.BOTTOM);
  }

  private static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutHeightOf(final View view, final int layoutHeight) {
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private static void updateLayoutGravityOf(final View view, final int layoutGravity) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LinearLayout.LayoutParams) {
      final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else if (lp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else {
      throw new IllegalArgumentException(
          "Layout parameter doesn't have gravity: " + lp.getClass().getName());
    }
  }
}
