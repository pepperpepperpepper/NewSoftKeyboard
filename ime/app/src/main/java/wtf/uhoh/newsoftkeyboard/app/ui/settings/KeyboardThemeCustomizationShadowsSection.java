package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationShadowsSection {

  interface Host {
    @Nullable
    String getActiveThemeIdOrNull();

    void refreshState();

    void attachColorPickerDialog(@NonNull androidx.preference.EditTextPreference preference);
  }

  enum ShadowTarget {
    ALL_KEYS("all"),
    SPECIAL_KEYS("special"),
    SPACEBAR("spacebar"),
    MODIFIER_KEYS("modifier"),
    ENTER_KEY("enter");

    @NonNull private final String value;

    ShadowTarget(@NonNull String value) {
      this.value = value;
    }

    @NonNull
    static ShadowTarget fromValue(@NonNull String value) {
      final String normalized = value.trim().toLowerCase();
      for (ShadowTarget target : values()) {
        if (target.value.equals(normalized)) return target;
      }
      return ALL_KEYS;
    }
  }

  @NonNull private final Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @NonNull private final KeyboardThemeCustomizationShadowTokensUi shadowTokensUi;
  @NonNull private final KeyboardThemeCustomizationPerKeyShadowsUi perKeyShadowsUi;

  @NonNull private ShadowTarget shadowTarget = ShadowTarget.ALL_KEYS;
  @Nullable private ListPreference shadowTargetPref;

  KeyboardThemeCustomizationShadowsSection(
      @NonNull Host host, @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.shadowTokensUi = new KeyboardThemeCustomizationShadowTokensUi(host, themeOverridesStore);
    this.perKeyShadowsUi =
        new KeyboardThemeCustomizationPerKeyShadowsUi(
            host, themeOverridesStore, () -> shadowTarget);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    final PreferenceCategory shadows = new PreferenceCategory(context);
    shadows.setKey("section:shadows");
    shadows.setTitle(R.string.keyboard_theme_appearance_shadows_title);
    screen.addPreference(shadows);

    shadowTargetPref = new ListPreference(context);
    shadowTargetPref.setKey("keyboard_theme_override_shadow_target");
    shadowTargetPref.setPersistent(false);
    shadowTargetPref.setTitle(R.string.keyboard_theme_appearance_shadow_target_title);
    final CharSequence targetSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_shadow_target_summary);
    shadowTargetPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return targetSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return targetSummaryBase;
          return targetSummaryBase + "\n" + entry;
        });
    shadowTargetPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_shadow_target_all_keys_entry),
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_special_keys_label),
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_spacebar_label),
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_modifier_keys_label),
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_contrast_enter_key_label)
        });
    shadowTargetPref.setEntryValues(
        new CharSequence[] {
          ShadowTarget.ALL_KEYS.value,
          ShadowTarget.SPECIAL_KEYS.value,
          ShadowTarget.SPACEBAR.value,
          ShadowTarget.MODIFIER_KEYS.value,
          ShadowTarget.ENTER_KEY.value
        });
    shadowTargetPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          shadowTarget = ShadowTarget.fromValue(String.valueOf(newValue));
          host.refreshState();
          return true;
        });
    shadows.addPreference(shadowTargetPref);

    final PreferenceCategory shadowTokensCategory = new PreferenceCategory(context);
    shadowTokensCategory.setTitle(R.string.keyboard_theme_appearance_shadows_tokens_title);
    shadows.addPreference(shadowTokensCategory);
    shadowTokensUi.addPreferences(context, shadowTokensCategory);

    perKeyShadowsUi.addPreferences(context, shadows);
  }

  boolean hasAnyShadowsOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return shadowTokensUi.hasAnyShadowsOverride(themeId)
        || perKeyShadowsUi.hasAnyShadowsOverride(themeId);
  }

  void refreshState(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return;

    final ListPreference targetPref = shadowTargetPref;
    if (targetPref != null) {
      final String value = targetPref.getValue();
      if (value != null) shadowTarget = ShadowTarget.fromValue(value);
      targetPref.setValue(shadowTarget.value);
    }

    shadowTokensUi.refreshState(themeId);
    perKeyShadowsUi.refreshState(themeId, shadowTarget);
  }

  void dispose() {
    shadowTokensUi.dispose();
    perKeyShadowsUi.dispose();
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

  @NonNull
  static String contextString(@NonNull Preference preference, int resId, Object... formatArgs) {
    return preference.getContext().getString(resId, formatArgs);
  }
}
