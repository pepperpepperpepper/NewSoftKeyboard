package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationShadowsSection.ShadowTarget;

final class KeyboardThemeCustomizationPerKeyShadowsUi {

  @FunctionalInterface
  interface ShadowTargetProvider {
    @NonNull
    ShadowTarget getShadowTarget();
  }

  @NonNull private final KeyboardThemeCustomizationPerKeyTextShadowsUi textShadowsUi;
  @NonNull private final KeyboardThemeCustomizationPerKeyBackgroundShadowsUi backgroundShadowsUi;

  KeyboardThemeCustomizationPerKeyShadowsUi(
      @NonNull KeyboardThemeCustomizationShadowsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull ShadowTargetProvider shadowTargetProvider) {
    this.textShadowsUi =
        new KeyboardThemeCustomizationPerKeyTextShadowsUi(
            host, themeOverridesStore, shadowTargetProvider);
    this.backgroundShadowsUi =
        new KeyboardThemeCustomizationPerKeyBackgroundShadowsUi(
            host, themeOverridesStore, shadowTargetProvider);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory shadows) {
    textShadowsUi.addPreferences(context, shadows);
    backgroundShadowsUi.addPreferences(context, shadows);
  }

  boolean hasAnyShadowsOverride(@NonNull String themeId) {
    return textShadowsUi.hasAnyTextShadowsOverride(themeId)
        || backgroundShadowsUi.hasAnyKeyShadowsOverride(themeId);
  }

  void refreshState(@NonNull String themeId, @NonNull ShadowTarget target) {
    textShadowsUi.refreshState(themeId, target);
    backgroundShadowsUi.refreshState(themeId, target);
  }

  @NonNull
  static String labelForShadowTarget(@NonNull Context context, @NonNull ShadowTarget target) {
    return switch (target) {
      case ALL_KEYS ->
          context.getString(R.string.keyboard_theme_appearance_shadow_target_all_keys_entry);
      case SPECIAL_KEYS ->
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_special_keys_label);
      case SPACEBAR ->
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_spacebar_label);
      case MODIFIER_KEYS ->
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_modifier_keys_label);
      case ENTER_KEY ->
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_enter_key_label);
    };
  }

  void dispose() {
    textShadowsUi.dispose();
    backgroundShadowsUi.dispose();
  }
}
