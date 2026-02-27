package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

final class KeyboardThemeCustomizationColorUiUtil {

  private KeyboardThemeCustomizationColorUiUtil() {}

  static int scalePercent(int percent, int intensityPercent) {
    final int clamped = Math.max(0, Math.min(100, percent));
    final int intensity = Math.max(0, Math.min(100, intensityPercent));
    return Math.round(clamped * (intensity / 100f));
  }

  static int blendColors(int fromArgb, int toArgb, float toAmount) {
    final float clamped = Math.max(0f, Math.min(1f, toAmount));
    final float fromAmount = 1f - clamped;

    final int fa = (fromArgb >>> 24) & 0xFF;
    final int fr = (fromArgb >>> 16) & 0xFF;
    final int fg = (fromArgb >>> 8) & 0xFF;
    final int fb = fromArgb & 0xFF;

    final int ta = (toArgb >>> 24) & 0xFF;
    final int tr = (toArgb >>> 16) & 0xFF;
    final int tg = (toArgb >>> 8) & 0xFF;
    final int tb = toArgb & 0xFF;

    final int a = Math.round(fa * fromAmount + ta * clamped);
    final int r = Math.round(fr * fromAmount + tr * clamped);
    final int g = Math.round(fg * fromAmount + tg * clamped);
    final int b = Math.round(fb * fromAmount + tb * clamped);
    return Color.argb(a, r, g, b);
  }

  @NonNull
  static String formatColor(int argb) {
    final int alpha = (argb >>> 24) & 0xFF;
    if (alpha == 0xFF) {
      return String.format("#%06X", argb & 0x00FF_FFFF);
    }
    return String.format("#%08X", argb);
  }

  static void setColorIcon(@NonNull Preference preference, @Nullable Integer argb) {
    if (argb == null) {
      preference.setIcon(null);
      preference.setIconSpaceReserved(false);
      return;
    }

    preference.setIcon(new android.graphics.drawable.ColorDrawable(argb));
    preference.setIconSpaceReserved(true);
  }

  static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
  }

  @Nullable
  static Context preferenceContext(@Nullable Preference preference) {
    return preference != null ? preference.getContext() : null;
  }

  @NonNull
  static String contextString(@NonNull Preference preference, int resId) {
    return preference.getContext().getString(resId);
  }
}
