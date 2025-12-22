package com.anysoftkeyboard.ime;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;

public final class FullscreenExtractViewController {

  @Nullable private View fullScreenExtractView;
  @Nullable private EditText fullScreenExtractTextView;

  @Nullable
  public View onCreateExtractTextView(@Nullable View createdView) {
    fullScreenExtractView = createdView;
    if (fullScreenExtractView == null) {
      fullScreenExtractTextView = null;
    } else {
      fullScreenExtractTextView =
          fullScreenExtractView.findViewById(android.R.id.inputExtractEditText);
    }
    return createdView;
  }

  public void updateFullscreenMode(@Nullable InputViewBinder inputViewBinder) {
    if (fullScreenExtractView == null || inputViewBinder == null) {
      return;
    }

    final AnyKeyboardView anyKeyboardView = (AnyKeyboardView) inputViewBinder;
    Drawable background = anyKeyboardView.getBackground();
    if (background == null) {
      background = anyKeyboardView.getCurrentResourcesHolder().getKeyboardBackground();
    }
    ViewCompat.setBackground(fullScreenExtractView, background);
    if (fullScreenExtractTextView != null) {
      fullScreenExtractTextView.setTextColor(
          anyKeyboardView.getCurrentResourcesHolder().getKeyTextColor());
    }
  }
}
