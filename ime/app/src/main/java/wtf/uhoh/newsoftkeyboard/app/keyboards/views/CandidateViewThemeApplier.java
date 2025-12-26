package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;

final class CandidateViewThemeApplier {

  private static final String TAG = "NSKCandidateTheme";

  record Result(
      float horizontalGap,
      @NonNull Drawable divider,
      @NonNull Drawable closeDrawable,
      @NonNull Drawable selectionHighlight,
      Drawable backgroundDrawable,
      float baseSuggestionTextSizePx) {}

  @NonNull
  static Result applyTheme(
      @NonNull Context context,
      @NonNull KeyboardTheme theme,
      @NonNull ThemeOverlayCombiner themeOverlayCombiner,
      @NonNull Paint paint) {
    final AddOn.AddOnResourceMapping remoteAttrs = theme.getResourceMapping();
    final int[] remoteStyleableArray =
        remoteAttrs.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewTheme);
    final TypedArray a =
        theme
            .getPackageContext()
            .obtainStyledAttributes(theme.getThemeResId(), remoteStyleableArray);

    themeOverlayCombiner.setThemeTextColor(
        new ColorStateList(
            new int[][] {{0}},
            new int[] {ContextCompat.getColor(context, R.color.candidate_normal)}));
    themeOverlayCombiner.setThemeNameTextColor(
        ContextCompat.getColor(context, R.color.candidate_recommended));
    themeOverlayCombiner.setThemeHintTextColor(
        ContextCompat.getColor(context, R.color.candidate_other));

    float horizontalGap =
        context.getResources().getDimensionPixelSize(R.dimen.candidate_strip_x_gap);
    Drawable divider = null;
    Drawable closeDrawable = null;
    Drawable selectionHighlight = null;
    Drawable backgroundDrawable = null;

    final float defaultFontSizePixel =
        context.getResources().getDimensionPixelSize(R.dimen.candidate_font_height);
    float keyTextSizePixel = Float.NaN;
    float suggestionTextSizePixel = Float.NaN;

    final int resolvedAttrsCount = a.getIndexCount();
    for (int attrIndex = 0; attrIndex < resolvedAttrsCount; attrIndex++) {
      final int remoteIndex = a.getIndex(attrIndex);
      try {
        switch (remoteAttrs.getLocalAttrId(remoteStyleableArray[remoteIndex])) {
          case R.attr.suggestionNormalTextColor:
            themeOverlayCombiner.setThemeNameTextColor(
                a.getColor(remoteIndex, ContextCompat.getColor(context, R.color.candidate_normal)));
            break;
          case R.attr.suggestionRecommendedTextColor:
            themeOverlayCombiner.setThemeTextColor(
                new ColorStateList(
                    new int[][] {{0}},
                    new int[] {
                      a.getColor(
                          remoteIndex,
                          ContextCompat.getColor(context, R.color.candidate_recommended))
                    }));
            break;
          case R.attr.suggestionOthersTextColor:
            themeOverlayCombiner.setThemeHintTextColor(
                a.getColor(remoteIndex, ContextCompat.getColor(context, R.color.candidate_other)));
            break;
          case R.attr.suggestionDividerImage:
            divider = a.getDrawable(remoteIndex);
            break;
          case R.attr.suggestionCloseImage:
            closeDrawable = a.getDrawable(remoteIndex);
            break;
          case R.attr.suggestionTextSize:
            suggestionTextSizePixel = a.getDimension(remoteIndex, defaultFontSizePixel);
            break;
          case R.attr.keyTextSize:
            keyTextSizePixel = a.getDimension(remoteIndex, defaultFontSizePixel);
            break;
          case R.attr.suggestionWordXGap:
            horizontalGap = a.getDimension(remoteIndex, horizontalGap);
            break;
          case R.attr.suggestionBackgroundImage:
            final Drawable stripImage = a.getDrawable(remoteIndex);
            if (stripImage != null) {
              themeOverlayCombiner.setThemeKeyboardBackground(stripImage);
              backgroundDrawable = themeOverlayCombiner.getThemeResources().getKeyboardBackground();
            }
            break;
          case R.attr.suggestionSelectionHighlight:
            selectionHighlight = a.getDrawable(remoteIndex);
            break;
        }
      } catch (Exception e) {
        Logger.w(TAG, "Got an exception while reading theme data", e);
      }
    }
    a.recycle();

    final float fontSizePixel;
    if (!Float.isNaN(keyTextSizePixel)) {
      fontSizePixel = keyTextSizePixel;
    } else if (!Float.isNaN(suggestionTextSizePixel)) {
      fontSizePixel = suggestionTextSizePixel;
    } else {
      fontSizePixel = defaultFontSizePixel;
    }

    if (divider == null) {
      divider = ContextCompat.getDrawable(context, R.drawable.dark_suggestions_divider);
    }
    if (closeDrawable == null) {
      closeDrawable = ContextCompat.getDrawable(context, R.drawable.close_suggestions_strip_icon);
    }
    if (selectionHighlight == null) {
      selectionHighlight =
          ContextCompat.getDrawable(context, R.drawable.dark_candidate_selected_background);
    }

    paint.setColor(themeOverlayCombiner.getThemeResources().getKeyTextColor().getDefaultColor());
    paint.setAntiAlias(true);
    paint.setStrokeWidth(0);
    paint.setTextAlign(Paint.Align.CENTER);

    // Ensure the base font size is at least a pixel to avoid an invisible strip.
    final float safeTextSizePx =
        Math.max(
            fontSizePixel,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics()));

    return new Result(
        horizontalGap,
        requireDrawable(divider, "divider"),
        requireDrawable(closeDrawable, "closeDrawable"),
        requireDrawable(selectionHighlight, "selectionHighlight"),
        backgroundDrawable,
        safeTextSizePx);
  }

  @NonNull
  private static Drawable requireDrawable(@Nullable Drawable drawable, @NonNull String name) {
    if (drawable == null) {
      throw new IllegalStateException("Expected non-null drawable for " + name);
    }
    return drawable;
  }
}
