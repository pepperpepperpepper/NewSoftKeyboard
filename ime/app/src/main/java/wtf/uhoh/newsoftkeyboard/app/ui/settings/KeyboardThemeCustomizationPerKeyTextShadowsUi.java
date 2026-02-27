package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationShadowsSection.ShadowTarget;

final class KeyboardThemeCustomizationPerKeyTextShadowsUi {

  @NonNull private final KeyboardThemeCustomizationShadowsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @NonNull
  private final KeyboardThemeCustomizationPerKeyShadowsUi.ShadowTargetProvider shadowTargetProvider;

  @Nullable private PreferenceCategory textShadowCategory;

  @Nullable private Preference textShadowStylePref;
  @Nullable private EditTextPreference keyTextShadowColorPref;
  @Nullable private EditTextPreference keyTextShadowRadiusDpPref;
  @Nullable private EditTextPreference keyTextShadowOffsetXDpPref;
  @Nullable private EditTextPreference keyTextShadowOffsetYDpPref;
  @Nullable private Preference resetTextShadowPref;

  KeyboardThemeCustomizationPerKeyTextShadowsUi(
      @NonNull KeyboardThemeCustomizationShadowsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull
          KeyboardThemeCustomizationPerKeyShadowsUi.ShadowTargetProvider shadowTargetProvider) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.shadowTargetProvider = shadowTargetProvider;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory shadows) {
    textShadowCategory = new PreferenceCategory(context);
    textShadowCategory.setTitle(R.string.keyboard_theme_appearance_text_shadow_title);
    shadows.addPreference(textShadowCategory);

    textShadowStylePref = new Preference(context);
    textShadowStylePref.setKey("keyboard_theme_override_key_text_shadow_style");
    textShadowStylePref.setPersistent(false);
    textShadowStylePref.setTitle(R.string.keyboard_theme_appearance_text_shadow_style_title);
    textShadowStylePref.setSummary(R.string.keyboard_theme_appearance_text_shadow_style_summary);
    textShadowStylePref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          KeyboardThemeCustomizationTextShadowStyleDialog.show(
              context, themeId, shadowTarget, themeOverridesStore, host::refreshState);
          return true;
        });
    textShadowCategory.addPreference(textShadowStylePref);

    keyTextShadowColorPref = new EditTextPreference(context);
    keyTextShadowColorPref.setKey("keyboard_theme_override_key_text_shadow_color");
    keyTextShadowColorPref.setPersistent(false);
    keyTextShadowColorPref.setTitle(R.string.keyboard_theme_appearance_text_shadow_color_title);
    keyTextShadowColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_color_title);
    keyTextShadowColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#AARRGGBB");
        });
    keyTextShadowColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            KeyboardThemeCustomizationTextShadowOverrides.clearColor(
                themeOverridesStore, themeId, shadowTarget);
            host.refreshState();
            return true;
          }
          try {
            final int argb = Color.parseColor(raw);
            KeyboardThemeCustomizationTextShadowOverrides.setColor(
                themeOverridesStore, themeId, shadowTarget, argb);
            host.refreshState();
            return true;
          } catch (IllegalArgumentException e) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_invalid_color_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          }
        });
    textShadowCategory.addPreference(keyTextShadowColorPref);
    host.attachColorPickerDialog(keyTextShadowColorPref);

    keyTextShadowRadiusDpPref = new EditTextPreference(context);
    keyTextShadowRadiusDpPref.setKey("keyboard_theme_override_key_text_shadow_radius_dp");
    keyTextShadowRadiusDpPref.setPersistent(false);
    keyTextShadowRadiusDpPref.setTitle(R.string.keyboard_theme_appearance_text_shadow_radius_title);
    keyTextShadowRadiusDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_radius_title);
    keyTextShadowRadiusDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER);
          edit.setHint("0");
        });
    keyTextShadowRadiusDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            KeyboardThemeCustomizationTextShadowOverrides.clearRadiusDp(
                themeOverridesStore, themeId, shadowTarget);
            host.refreshState();
            return true;
          }
          try {
            final int value = Math.max(0, Integer.parseInt(raw));
            KeyboardThemeCustomizationTextShadowOverrides.setRadiusDp(
                themeOverridesStore, themeId, shadowTarget, value);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_invalid_number_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          }
        });
    textShadowCategory.addPreference(keyTextShadowRadiusDpPref);

    keyTextShadowOffsetXDpPref = new EditTextPreference(context);
    keyTextShadowOffsetXDpPref.setKey("keyboard_theme_override_key_text_shadow_offset_x_dp");
    keyTextShadowOffsetXDpPref.setPersistent(false);
    keyTextShadowOffsetXDpPref.setTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_x_title);
    keyTextShadowOffsetXDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_x_title);
    keyTextShadowOffsetXDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    keyTextShadowOffsetXDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            KeyboardThemeCustomizationTextShadowOverrides.clearOffsetXDp(
                themeOverridesStore, themeId, shadowTarget);
            host.refreshState();
            return true;
          }
          try {
            final int value = Integer.parseInt(raw);
            KeyboardThemeCustomizationTextShadowOverrides.setOffsetXDp(
                themeOverridesStore, themeId, shadowTarget, value);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_invalid_number_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          }
        });
    textShadowCategory.addPreference(keyTextShadowOffsetXDpPref);

    keyTextShadowOffsetYDpPref = new EditTextPreference(context);
    keyTextShadowOffsetYDpPref.setKey("keyboard_theme_override_key_text_shadow_offset_y_dp");
    keyTextShadowOffsetYDpPref.setPersistent(false);
    keyTextShadowOffsetYDpPref.setTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_y_title);
    keyTextShadowOffsetYDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_y_title);
    keyTextShadowOffsetYDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    keyTextShadowOffsetYDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            KeyboardThemeCustomizationTextShadowOverrides.clearOffsetYDp(
                themeOverridesStore, themeId, shadowTarget);
            host.refreshState();
            return true;
          }
          try {
            final int value = Integer.parseInt(raw);
            KeyboardThemeCustomizationTextShadowOverrides.setOffsetYDp(
                themeOverridesStore, themeId, shadowTarget, value);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_invalid_number_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          }
        });
    textShadowCategory.addPreference(keyTextShadowOffsetYDpPref);

    resetTextShadowPref = new Preference(context);
    resetTextShadowPref.setTitle(R.string.keyboard_theme_appearance_reset_text_shadow_title);
    resetTextShadowPref.setSummary(R.string.keyboard_theme_appearance_reset_text_shadow_summary);
    resetTextShadowPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          KeyboardThemeCustomizationTextShadowOverrides.clearAllOverrides(
              themeOverridesStore, themeId, shadowTarget);
          Toast.makeText(
                  context,
                  R.string.keyboard_theme_appearance_reset_text_shadow_toast,
                  Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    textShadowCategory.addPreference(resetTextShadowPref);
  }

  boolean hasAnyTextShadowsOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return KeyboardThemeCustomizationTextShadowOverrides.hasAnyOverride(store, themeId);
  }

  void refreshState(@NonNull String themeId, @NonNull ShadowTarget target) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return;

    final Preference textShadowStyle = textShadowStylePref;
    if (textShadowStyle == null) return;
    final Context context = textShadowStyle.getContext();

    final String targetLabel =
        KeyboardThemeCustomizationPerKeyShadowsUi.labelForShadowTarget(context, target);
    if (textShadowCategory != null) {
      textShadowCategory.setTitle(
          context.getString(R.string.keyboard_theme_appearance_text_shadow_title)
              + " ("
              + targetLabel
              + ")");
    }

    final KeyboardThemeCustomizationTextShadowOverrides.EffectiveValues effectiveValues =
        KeyboardThemeCustomizationTextShadowOverrides.readEffectiveValues(store, themeId, target);
    final boolean textShadowUsesTokenSecondary = effectiveValues.usesTokenSecondary();
    final Integer shadowColor = effectiveValues.shadowColor();
    final Integer shadowRadiusDp = effectiveValues.shadowRadiusDp();
    final Integer shadowOffsetXDp = effectiveValues.shadowOffsetXDp();
    final Integer shadowOffsetYDp = effectiveValues.shadowOffsetYDp();

    textShadowStyle.setSummary(
        textShadowUsesTokenSecondary
            ? context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry)
            : KeyboardThemeCustomizationTextShadowStyleDialog.describeTextShadowStyleForTarget(
                context, target, shadowColor, shadowRadiusDp, shadowOffsetXDp, shadowOffsetYDp));

    if (keyTextShadowColorPref != null) {
      keyTextShadowColorPref.setEnabled(!textShadowUsesTokenSecondary);
      keyTextShadowColorPref.setText(
          shadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(shadowColor)
              : "");
      keyTextShadowColorPref.setSummary(
          shadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(shadowColor)
              : target != ShadowTarget.ALL_KEYS
                  ? KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowColorPref,
                      R.string.keyboard_theme_appearance_shadow_inherit_summary)
                  : KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowColorPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationShadowsSection.setColorIcon(keyTextShadowColorPref, shadowColor);
    }

    if (keyTextShadowRadiusDpPref != null) {
      keyTextShadowRadiusDpPref.setEnabled(!textShadowUsesTokenSecondary);
      keyTextShadowRadiusDpPref.setText(
          shadowRadiusDp != null ? String.valueOf(shadowRadiusDp) : "");
      keyTextShadowRadiusDpPref.setSummary(
          shadowRadiusDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  keyTextShadowRadiusDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  shadowRadiusDp)
              : target != ShadowTarget.ALL_KEYS
                  ? KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowRadiusDpPref,
                      R.string.keyboard_theme_appearance_shadow_inherit_summary)
                  : KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowRadiusDpPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
    }

    if (keyTextShadowOffsetXDpPref != null) {
      keyTextShadowOffsetXDpPref.setEnabled(!textShadowUsesTokenSecondary);
      keyTextShadowOffsetXDpPref.setText(
          shadowOffsetXDp != null ? String.valueOf(shadowOffsetXDp) : "");
      keyTextShadowOffsetXDpPref.setSummary(
          shadowOffsetXDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  keyTextShadowOffsetXDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  shadowOffsetXDp)
              : target != ShadowTarget.ALL_KEYS
                  ? KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowOffsetXDpPref,
                      R.string.keyboard_theme_appearance_shadow_inherit_summary)
                  : KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowOffsetXDpPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
    }

    if (keyTextShadowOffsetYDpPref != null) {
      keyTextShadowOffsetYDpPref.setEnabled(!textShadowUsesTokenSecondary);
      keyTextShadowOffsetYDpPref.setText(
          shadowOffsetYDp != null ? String.valueOf(shadowOffsetYDp) : "");
      keyTextShadowOffsetYDpPref.setSummary(
          shadowOffsetYDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  keyTextShadowOffsetYDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  shadowOffsetYDp)
              : target != ShadowTarget.ALL_KEYS
                  ? KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowOffsetYDpPref,
                      R.string.keyboard_theme_appearance_shadow_inherit_summary)
                  : KeyboardThemeCustomizationShadowsSection.contextString(
                      keyTextShadowOffsetYDpPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
    }

    if (resetTextShadowPref != null) {
      resetTextShadowPref.setEnabled(effectiveValues.hasAnyOverride());
    }
  }

  void dispose() {}
}
