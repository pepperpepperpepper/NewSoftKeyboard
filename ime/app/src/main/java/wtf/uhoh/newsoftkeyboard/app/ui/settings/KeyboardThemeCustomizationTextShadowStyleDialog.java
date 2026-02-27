package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationShadowsSection.ShadowTarget;

final class KeyboardThemeCustomizationTextShadowStyleDialog {

  static void show(
      @NonNull Context context,
      @NonNull String themeId,
      @NonNull ShadowTarget shadowTarget,
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull Runnable onChanged) {
    final CharSequence[] items =
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_style_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_style_off_entry),
          context.getString(R.string.keyboard_theme_appearance_style_subtle_entry),
          context.getString(R.string.keyboard_theme_appearance_style_strong_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry)
        };
    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_theme_appearance_text_shadow_style_dialog_title)
        .setItems(
            items,
            (dialog, which) -> {
              dialog.dismiss();
              switch (which) {
                case 0:
                  KeyboardThemeCustomizationTextShadowOverrides.clearAllOverrides(
                      store, themeId, shadowTarget);
                  break;
                case 1:
                  KeyboardThemeCustomizationTextShadowOverrides.setUseTokenSecondary(
                      store, themeId, shadowTarget, false);
                  KeyboardThemeCustomizationTextShadowOverrides.applyPreset(
                      store, themeId, shadowTarget, Color.TRANSPARENT, 0, 0, 0);
                  break;
                case 2:
                  KeyboardThemeCustomizationTextShadowOverrides.setUseTokenSecondary(
                      store, themeId, shadowTarget, false);
                  KeyboardThemeCustomizationTextShadowOverrides.applyPreset(
                      store, themeId, shadowTarget, 0x6600_0000, 2, 0, 1);
                  break;
                case 3:
                  KeyboardThemeCustomizationTextShadowOverrides.setUseTokenSecondary(
                      store, themeId, shadowTarget, false);
                  KeyboardThemeCustomizationTextShadowOverrides.applyPreset(
                      store, themeId, shadowTarget, 0xAA00_0000, 3, 0, 2);
                  break;
                case 4:
                  KeyboardThemeCustomizationTextShadowOverrides.clearAllOverrides(
                      store, themeId, shadowTarget);
                  KeyboardThemeCustomizationTextShadowOverrides.setUseTokenSecondary(
                      store, themeId, shadowTarget, true);
                  break;
                default:
                  break;
              }
              onChanged.run();
            })
        .show();
  }

  @NonNull
  static String describeTextShadowStyleForTarget(
      @NonNull Context context,
      @NonNull ShadowTarget target,
      @Nullable Integer shadowColor,
      @Nullable Integer shadowRadiusDp,
      @Nullable Integer shadowOffsetXDp,
      @Nullable Integer shadowOffsetYDp) {
    if (target != ShadowTarget.ALL_KEYS
        && shadowColor == null
        && shadowRadiusDp == null
        && shadowOffsetXDp == null
        && shadowOffsetYDp == null) {
      return context.getString(R.string.keyboard_theme_appearance_shadow_inherit_summary);
    }
    return describeTextShadowStyle(
        context, shadowColor, shadowRadiusDp, shadowOffsetXDp, shadowOffsetYDp);
  }

  @NonNull
  private static String describeTextShadowStyle(
      @NonNull Context context,
      @Nullable Integer shadowColor,
      @Nullable Integer shadowRadiusDp,
      @Nullable Integer shadowOffsetXDp,
      @Nullable Integer shadowOffsetYDp) {
    if (shadowColor == null
        && shadowRadiusDp == null
        && shadowOffsetXDp == null
        && shadowOffsetYDp == null) {
      return context.getString(R.string.keyboard_theme_appearance_style_theme_default_entry);
    }

    if (shadowColor == null
        || shadowRadiusDp == null
        || shadowOffsetXDp == null
        || shadowOffsetYDp == null) {
      return context.getString(R.string.keyboard_theme_appearance_style_custom_entry);
    }

    if (shadowColor == Color.TRANSPARENT
        && shadowRadiusDp == 0
        && shadowOffsetXDp == 0
        && shadowOffsetYDp == 0) {
      return context.getString(R.string.keyboard_theme_appearance_style_off_entry);
    }

    if (shadowColor == 0x6600_0000
        && shadowRadiusDp == 2
        && shadowOffsetXDp == 0
        && shadowOffsetYDp == 1) {
      return context.getString(R.string.keyboard_theme_appearance_style_subtle_entry);
    }

    if (shadowColor == 0xAA00_0000
        && shadowRadiusDp == 3
        && shadowOffsetXDp == 0
        && shadowOffsetYDp == 2) {
      return context.getString(R.string.keyboard_theme_appearance_style_strong_entry);
    }

    return context.getString(R.string.keyboard_theme_appearance_style_custom_entry);
  }

  private KeyboardThemeCustomizationTextShadowStyleDialog() {}
}
