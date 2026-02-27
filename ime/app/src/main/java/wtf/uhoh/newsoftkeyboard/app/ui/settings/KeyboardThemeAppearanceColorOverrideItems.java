package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeAppearanceColorOverrideItems {

  @NonNull
  static List<KeyboardThemeAppearanceOverrideItem> build(
      @NonNull Context context,
      @NonNull String presetId,
      @Nullable KeyboardThemeUserOverridesStore store) {
    if (store == null) return Collections.emptyList();
    final List<KeyboardThemeAppearanceOverrideItem> out = new ArrayList<>();

    if (store.getEnsureReadableTextEnabled(presetId) != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_ensure_readable_text_title),
              context.getString(android.R.string.yes),
              "keyboard_theme_appearance_ensure_readable_text",
              () -> store.clearEnsureReadableTextEnabled(presetId)));
    }

    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_token_primary_text_color_title,
        store.getTokenPrimaryTextColor(presetId),
        "keyboard_theme_token_primary_text_color",
        () -> store.clearTokenPrimaryTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_token_secondary_text_color_title,
        store.getTokenSecondaryTextColor(presetId),
        "keyboard_theme_token_secondary_text_color",
        () -> store.clearTokenSecondaryTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_token_accent_color_title,
        store.getTokenAccentColor(presetId),
        "keyboard_theme_token_accent_color",
        () -> store.clearTokenAccentColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_token_key_surface_color_title,
        store.getTokenKeySurfaceColor(presetId),
        "keyboard_theme_token_key_surface_color",
        () -> store.clearTokenKeySurfaceColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_token_background_color_title,
        store.getTokenBackgroundColor(presetId),
        "keyboard_theme_token_background_color",
        () -> store.clearTokenBackgroundColor(presetId));

    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_key_text_color_title,
        store.getKeyTextColor(presetId),
        "keyboard_theme_override_key_text_color",
        () -> store.clearKeyTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_special_key_text_color_title,
        store.getSpecialKeyTextColor(presetId),
        "keyboard_theme_override_special_key_text_color",
        () -> store.clearSpecialKeyTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_modifier_key_text_color_title,
        store.getModifierKeyTextColor(presetId),
        "keyboard_theme_override_modifier_key_text_color",
        () -> store.clearModifierKeyTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_enter_key_text_color_title,
        store.getEnterKeyTextColor(presetId),
        "keyboard_theme_override_enter_key_text_color",
        () -> store.clearEnterKeyTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_spacebar_text_color_title,
        store.getSpacebarTextColor(presetId),
        "keyboard_theme_override_spacebar_text_color",
        () -> store.clearSpacebarTextColor(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_hint_text_color_title,
        store.getHintTextColor(presetId),
        "keyboard_theme_override_hint_text_color",
        () -> store.clearHintTextColor(presetId));

    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_key_background_tint_title,
        store.getKeyBackgroundTint(presetId),
        "keyboard_theme_override_key_background_tint",
        () -> store.clearKeyBackgroundTint(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_special_key_background_tint_title,
        store.getSpecialKeyBackgroundTint(presetId),
        "keyboard_theme_override_special_key_background_tint",
        () -> store.clearSpecialKeyBackgroundTint(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_modifier_key_background_tint_title,
        store.getModifierKeyBackgroundTint(presetId),
        "keyboard_theme_override_modifier_key_background_tint",
        () -> store.clearModifierKeyBackgroundTint(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_enter_key_background_tint_title,
        store.getEnterKeyBackgroundTint(presetId),
        "keyboard_theme_override_enter_key_background_tint",
        () -> store.clearEnterKeyBackgroundTint(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_spacebar_background_tint_title,
        store.getSpacebarBackgroundTint(presetId),
        "keyboard_theme_override_spacebar_background_tint",
        () -> store.clearSpacebarBackgroundTint(presetId));
    addColorOverride(
        context,
        out,
        R.string.keyboard_theme_appearance_keyboard_background_tint_title,
        store.getKeyboardBackgroundTint(presetId),
        "keyboard_theme_override_keyboard_background_tint",
        () -> store.clearKeyboardBackgroundTint(presetId));

    final Integer keyOpacity = store.getKeyBackgroundOpacityPercent(presetId);
    if (keyOpacity != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_key_background_opacity_title),
              keyOpacity + "%",
              "keyboard_theme_override_key_background_opacity",
              () -> store.clearKeyBackgroundOpacityPercent(presetId)));
    }

    final Integer keyboardOpacity = store.getKeyboardBackgroundOpacityPercent(presetId);
    if (keyboardOpacity != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_appearance_keyboard_background_opacity_title),
              keyboardOpacity + "%",
              "keyboard_theme_override_keyboard_background_opacity",
              () -> store.clearKeyboardBackgroundOpacityPercent(presetId)));
    }

    return out;
  }

  private static void addColorOverride(
      @NonNull Context context,
      @NonNull List<KeyboardThemeAppearanceOverrideItem> out,
      int titleResId,
      @Nullable Integer argb,
      @NonNull String scrollToKey,
      @NonNull Runnable resetAction) {
    if (argb == null) return;
    out.add(
        new KeyboardThemeAppearanceOverrideItem(
            context.getString(titleResId),
            KeyboardThemeCustomizationColorUiUtil.formatColor(argb),
            scrollToKey,
            resetAction));
  }
}
