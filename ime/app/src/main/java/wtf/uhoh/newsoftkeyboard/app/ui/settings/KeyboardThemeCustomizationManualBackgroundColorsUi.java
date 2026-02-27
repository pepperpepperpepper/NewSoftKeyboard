package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SeekBarPreference;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationManualBackgroundColorsUi {

  @FunctionalInterface
  interface ResetColorsEnabledUpdater {
    void update(@NonNull String themeId);
  }

  @NonNull private final KeyboardThemeCustomizationColorsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @NonNull private final ResetColorsEnabledUpdater resetColorsEnabledUpdater;

  @Nullable private EditTextPreference keyBackgroundTintPref;
  @Nullable private EditTextPreference specialKeyBackgroundTintPref;
  @Nullable private EditTextPreference modifierKeyBackgroundTintPref;
  @Nullable private EditTextPreference enterKeyBackgroundTintPref;
  @Nullable private EditTextPreference spacebarBackgroundTintPref;
  @Nullable private EditTextPreference keyboardBackgroundTintPref;

  @Nullable private SeekBarPreference keyBackgroundOpacityPref;
  @Nullable private SeekBarPreference keyboardBackgroundOpacityPref;

  KeyboardThemeCustomizationManualBackgroundColorsUi(
      @NonNull KeyboardThemeCustomizationColorsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull ResetColorsEnabledUpdater resetColorsEnabledUpdater) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.resetColorsEnabledUpdater = resetColorsEnabledUpdater;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory colors) {
    keyBackgroundTintPref = new EditTextPreference(context);
    keyBackgroundTintPref.setKey("keyboard_theme_override_key_background_tint");
    keyBackgroundTintPref.setPersistent(false);
    keyBackgroundTintPref.setTitle(R.string.keyboard_theme_appearance_key_background_tint_title);
    keyBackgroundTintPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_background_tint_title);
    keyBackgroundTintPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    keyBackgroundTintPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearKeyBackgroundTint(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setKeyBackgroundTint(themeId, Color.parseColor(raw));
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
    colors.addPreference(keyBackgroundTintPref);
    host.attachColorPickerDialog(keyBackgroundTintPref);

    specialKeyBackgroundTintPref = new EditTextPreference(context);
    specialKeyBackgroundTintPref.setKey("keyboard_theme_override_special_key_background_tint");
    specialKeyBackgroundTintPref.setPersistent(false);
    specialKeyBackgroundTintPref.setTitle(
        R.string.keyboard_theme_appearance_special_key_background_tint_title);
    specialKeyBackgroundTintPref.setDialogTitle(
        R.string.keyboard_theme_appearance_special_key_background_tint_title);
    specialKeyBackgroundTintPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    specialKeyBackgroundTintPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearSpecialKeyBackgroundTint(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setSpecialKeyBackgroundTint(themeId, Color.parseColor(raw));
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
    colors.addPreference(specialKeyBackgroundTintPref);
    host.attachColorPickerDialog(specialKeyBackgroundTintPref);

    modifierKeyBackgroundTintPref = new EditTextPreference(context);
    modifierKeyBackgroundTintPref.setKey("keyboard_theme_override_modifier_key_background_tint");
    modifierKeyBackgroundTintPref.setPersistent(false);
    modifierKeyBackgroundTintPref.setTitle(
        R.string.keyboard_theme_appearance_modifier_key_background_tint_title);
    modifierKeyBackgroundTintPref.setDialogTitle(
        R.string.keyboard_theme_appearance_modifier_key_background_tint_title);
    modifierKeyBackgroundTintPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    modifierKeyBackgroundTintPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearModifierKeyBackgroundTint(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setModifierKeyBackgroundTint(themeId, Color.parseColor(raw));
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
    colors.addPreference(modifierKeyBackgroundTintPref);
    host.attachColorPickerDialog(modifierKeyBackgroundTintPref);

    enterKeyBackgroundTintPref = new EditTextPreference(context);
    enterKeyBackgroundTintPref.setKey("keyboard_theme_override_enter_key_background_tint");
    enterKeyBackgroundTintPref.setPersistent(false);
    enterKeyBackgroundTintPref.setTitle(
        R.string.keyboard_theme_appearance_enter_key_background_tint_title);
    enterKeyBackgroundTintPref.setDialogTitle(
        R.string.keyboard_theme_appearance_enter_key_background_tint_title);
    enterKeyBackgroundTintPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    enterKeyBackgroundTintPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearEnterKeyBackgroundTint(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setEnterKeyBackgroundTint(themeId, Color.parseColor(raw));
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
    colors.addPreference(enterKeyBackgroundTintPref);
    host.attachColorPickerDialog(enterKeyBackgroundTintPref);

    spacebarBackgroundTintPref = new EditTextPreference(context);
    spacebarBackgroundTintPref.setKey("keyboard_theme_override_spacebar_background_tint");
    spacebarBackgroundTintPref.setPersistent(false);
    spacebarBackgroundTintPref.setTitle(
        R.string.keyboard_theme_appearance_spacebar_background_tint_title);
    spacebarBackgroundTintPref.setDialogTitle(
        R.string.keyboard_theme_appearance_spacebar_background_tint_title);
    spacebarBackgroundTintPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    spacebarBackgroundTintPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearSpacebarBackgroundTint(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setSpacebarBackgroundTint(themeId, Color.parseColor(raw));
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
    colors.addPreference(spacebarBackgroundTintPref);
    host.attachColorPickerDialog(spacebarBackgroundTintPref);

    keyboardBackgroundTintPref = new EditTextPreference(context);
    keyboardBackgroundTintPref.setKey("keyboard_theme_override_keyboard_background_tint");
    keyboardBackgroundTintPref.setPersistent(false);
    keyboardBackgroundTintPref.setTitle(
        R.string.keyboard_theme_appearance_keyboard_background_tint_title);
    keyboardBackgroundTintPref.setDialogTitle(
        R.string.keyboard_theme_appearance_keyboard_background_tint_title);
    keyboardBackgroundTintPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    keyboardBackgroundTintPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearKeyboardBackgroundTint(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setKeyboardBackgroundTint(themeId, Color.parseColor(raw));
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
    colors.addPreference(keyboardBackgroundTintPref);
    host.attachColorPickerDialog(keyboardBackgroundTintPref);

    keyBackgroundOpacityPref = new SeekBarPreference(context);
    keyBackgroundOpacityPref.setKey("keyboard_theme_override_key_background_opacity");
    keyBackgroundOpacityPref.setPersistent(false);
    keyBackgroundOpacityPref.setTitle(
        R.string.keyboard_theme_appearance_key_background_opacity_title);
    keyBackgroundOpacityPref.setSummary(
        R.string.keyboard_theme_appearance_key_background_opacity_summary);
    keyBackgroundOpacityPref.setMin(0);
    keyBackgroundOpacityPref.setMax(100);
    keyBackgroundOpacityPref.setShowSeekBarValue(true);
    keyBackgroundOpacityPref.setUpdatesContinuously(true);
    keyBackgroundOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final int percent = (Integer) newValue;
          if (percent >= 100) {
            themeOverridesStore.clearKeyBackgroundOpacityPercent(themeId);
          } else {
            themeOverridesStore.setKeyBackgroundOpacityPercent(themeId, percent);
          }
          host.updateLivePreview();
          resetColorsEnabledUpdater.update(themeId);
          host.updateResetEnabledStates(themeId);
          return true;
        });
    colors.addPreference(keyBackgroundOpacityPref);

    keyboardBackgroundOpacityPref = new SeekBarPreference(context);
    keyboardBackgroundOpacityPref.setKey("keyboard_theme_override_keyboard_background_opacity");
    keyboardBackgroundOpacityPref.setPersistent(false);
    keyboardBackgroundOpacityPref.setTitle(
        R.string.keyboard_theme_appearance_keyboard_background_opacity_title);
    keyboardBackgroundOpacityPref.setSummary(
        R.string.keyboard_theme_appearance_keyboard_background_opacity_summary);
    keyboardBackgroundOpacityPref.setMin(0);
    keyboardBackgroundOpacityPref.setMax(100);
    keyboardBackgroundOpacityPref.setShowSeekBarValue(true);
    keyboardBackgroundOpacityPref.setUpdatesContinuously(true);
    keyboardBackgroundOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final int percent = (Integer) newValue;
          if (percent >= 100) {
            themeOverridesStore.clearKeyboardBackgroundOpacityPercent(themeId);
          } else {
            themeOverridesStore.setKeyboardBackgroundOpacityPercent(themeId, percent);
          }
          host.updateLivePreview();
          resetColorsEnabledUpdater.update(themeId);
          host.updateResetEnabledStates(themeId);
          return true;
        });
    colors.addPreference(keyboardBackgroundOpacityPref);
  }

  void refreshState(@NonNull String themeId) {
    final Integer tokenAccentColor =
        themeOverridesStore != null ? themeOverridesStore.getTokenAccentColor(themeId) : null;
    final Integer tokenKeySurfaceColor =
        themeOverridesStore != null ? themeOverridesStore.getTokenKeySurfaceColor(themeId) : null;
    final Integer tokenBackgroundColor =
        themeOverridesStore != null ? themeOverridesStore.getTokenBackgroundColor(themeId) : null;

    final Integer keyBackgroundTint =
        themeOverridesStore != null ? themeOverridesStore.getKeyBackgroundTint(themeId) : null;
    final Integer specialKeyBackgroundTint =
        themeOverridesStore != null
            ? themeOverridesStore.getSpecialKeyBackgroundTint(themeId)
            : null;
    final Integer modifierKeyBackgroundTint =
        themeOverridesStore != null
            ? themeOverridesStore.getModifierKeyBackgroundTint(themeId)
            : null;
    final Integer enterKeyBackgroundTint =
        themeOverridesStore != null ? themeOverridesStore.getEnterKeyBackgroundTint(themeId) : null;
    final Integer spacebarBackgroundTint =
        themeOverridesStore != null ? themeOverridesStore.getSpacebarBackgroundTint(themeId) : null;
    final Integer keyboardBackgroundTint =
        themeOverridesStore != null ? themeOverridesStore.getKeyboardBackgroundTint(themeId) : null;
    final Integer keyBackgroundOpacityPercent =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyBackgroundOpacityPercent(themeId)
            : null;
    final Integer keyboardBackgroundOpacityPercent =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyboardBackgroundOpacityPercent(themeId)
            : null;

    if (keyBackgroundTintPref != null) {
      keyBackgroundTintPref.setText(
          keyBackgroundTint != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(keyBackgroundTint)
              : "");
      keyBackgroundTintPref.setSummary(
          keyBackgroundTint != null
              ? keyBackgroundTintPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(keyBackgroundTint))
              : tokenKeySurfaceColor != null
                  ? keyBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenKeySurfaceColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      keyBackgroundTintPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          keyBackgroundTintPref,
          keyBackgroundTint != null ? keyBackgroundTint : tokenKeySurfaceColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          keyBackgroundTintPref,
          tokenKeySurfaceColor != null
              ? keyBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_key_surface_color_title)
              : keyBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenKeySurfaceColor);
    }

    if (specialKeyBackgroundTintPref != null) {
      specialKeyBackgroundTintPref.setText(
          specialKeyBackgroundTint != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(specialKeyBackgroundTint)
              : "");
      specialKeyBackgroundTintPref.setSummary(
          specialKeyBackgroundTint != null
              ? specialKeyBackgroundTintPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(specialKeyBackgroundTint))
              : tokenAccentColor != null
                  ? specialKeyBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenAccentColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      specialKeyBackgroundTintPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          specialKeyBackgroundTintPref,
          specialKeyBackgroundTint != null ? specialKeyBackgroundTint : tokenAccentColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          specialKeyBackgroundTintPref,
          tokenAccentColor != null
              ? specialKeyBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_accent_color_title)
              : specialKeyBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenAccentColor);
    }

    if (modifierKeyBackgroundTintPref != null) {
      modifierKeyBackgroundTintPref.setText(
          modifierKeyBackgroundTint != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(modifierKeyBackgroundTint)
              : "");
      modifierKeyBackgroundTintPref.setSummary(
          modifierKeyBackgroundTint != null
              ? modifierKeyBackgroundTintPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(modifierKeyBackgroundTint))
              : tokenAccentColor != null
                  ? modifierKeyBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenAccentColor))
                  : specialKeyBackgroundTint != null
                      ? modifierKeyBackgroundTintPref
                          .getContext()
                          .getString(
                              R.string.keyboard_theme_appearance_color_inherited_summary,
                              KeyboardThemeCustomizationColorUiUtil.formatColor(
                                  specialKeyBackgroundTint))
                      : KeyboardThemeCustomizationColorUiUtil.contextString(
                          modifierKeyBackgroundTintPref,
                          R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          modifierKeyBackgroundTintPref,
          modifierKeyBackgroundTint != null
              ? modifierKeyBackgroundTint
              : tokenAccentColor != null ? tokenAccentColor : specialKeyBackgroundTint);

      final String modifierBgLinkedLabel =
          tokenAccentColor != null
              ? modifierKeyBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_accent_color_title)
              : specialKeyBackgroundTint != null && specialKeyBackgroundTintPref != null
                  ? modifierKeyBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_color_picker_linked_source_inherits_from,
                          specialKeyBackgroundTintPref.getTitle())
                  : modifierKeyBackgroundTintPref
                      .getContext()
                      .getString(R.string.keyboard_theme_appearance_color_default_summary);
      final Integer modifierBgLinkedColor =
          tokenAccentColor != null ? tokenAccentColor : specialKeyBackgroundTint;
      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          modifierKeyBackgroundTintPref, modifierBgLinkedLabel, modifierBgLinkedColor);
    }

    if (enterKeyBackgroundTintPref != null) {
      enterKeyBackgroundTintPref.setText(
          enterKeyBackgroundTint != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(enterKeyBackgroundTint)
              : "");
      enterKeyBackgroundTintPref.setSummary(
          enterKeyBackgroundTint != null
              ? enterKeyBackgroundTintPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(enterKeyBackgroundTint))
              : tokenAccentColor != null
                  ? enterKeyBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenAccentColor))
                  : specialKeyBackgroundTint != null
                      ? enterKeyBackgroundTintPref
                          .getContext()
                          .getString(
                              R.string.keyboard_theme_appearance_color_inherited_summary,
                              KeyboardThemeCustomizationColorUiUtil.formatColor(
                                  specialKeyBackgroundTint))
                      : KeyboardThemeCustomizationColorUiUtil.contextString(
                          enterKeyBackgroundTintPref,
                          R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          enterKeyBackgroundTintPref,
          enterKeyBackgroundTint != null
              ? enterKeyBackgroundTint
              : tokenAccentColor != null ? tokenAccentColor : specialKeyBackgroundTint);

      final String enterBgLinkedLabel =
          tokenAccentColor != null
              ? enterKeyBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_accent_color_title)
              : specialKeyBackgroundTint != null && specialKeyBackgroundTintPref != null
                  ? enterKeyBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_color_picker_linked_source_inherits_from,
                          specialKeyBackgroundTintPref.getTitle())
                  : enterKeyBackgroundTintPref
                      .getContext()
                      .getString(R.string.keyboard_theme_appearance_color_default_summary);
      final Integer enterBgLinkedColor =
          tokenAccentColor != null ? tokenAccentColor : specialKeyBackgroundTint;
      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          enterKeyBackgroundTintPref, enterBgLinkedLabel, enterBgLinkedColor);
    }

    if (spacebarBackgroundTintPref != null) {
      spacebarBackgroundTintPref.setText(
          spacebarBackgroundTint != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(spacebarBackgroundTint)
              : "");
      spacebarBackgroundTintPref.setSummary(
          spacebarBackgroundTint != null
              ? spacebarBackgroundTintPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(spacebarBackgroundTint))
              : tokenKeySurfaceColor != null
                  ? spacebarBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenKeySurfaceColor))
                  : keyBackgroundTint != null
                      ? spacebarBackgroundTintPref
                          .getContext()
                          .getString(
                              R.string.keyboard_theme_appearance_color_inherited_summary,
                              KeyboardThemeCustomizationColorUiUtil.formatColor(keyBackgroundTint))
                      : KeyboardThemeCustomizationColorUiUtil.contextString(
                          spacebarBackgroundTintPref,
                          R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          spacebarBackgroundTintPref,
          spacebarBackgroundTint != null
              ? spacebarBackgroundTint
              : tokenKeySurfaceColor != null ? tokenKeySurfaceColor : keyBackgroundTint);

      final String spacebarBgLinkedLabel =
          tokenKeySurfaceColor != null
              ? spacebarBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_key_surface_color_title)
              : keyBackgroundTint != null && keyBackgroundTintPref != null
                  ? spacebarBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_color_picker_linked_source_inherits_from,
                          keyBackgroundTintPref.getTitle())
                  : spacebarBackgroundTintPref
                      .getContext()
                      .getString(R.string.keyboard_theme_appearance_color_default_summary);
      final Integer spacebarBgLinkedColor =
          tokenKeySurfaceColor != null ? tokenKeySurfaceColor : keyBackgroundTint;
      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          spacebarBackgroundTintPref, spacebarBgLinkedLabel, spacebarBgLinkedColor);
    }

    if (keyboardBackgroundTintPref != null) {
      keyboardBackgroundTintPref.setText(
          keyboardBackgroundTint != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(keyboardBackgroundTint)
              : "");
      keyboardBackgroundTintPref.setSummary(
          keyboardBackgroundTint != null
              ? keyboardBackgroundTintPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(keyboardBackgroundTint))
              : tokenBackgroundColor != null
                  ? keyboardBackgroundTintPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenBackgroundColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      keyboardBackgroundTintPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          keyboardBackgroundTintPref,
          keyboardBackgroundTint != null ? keyboardBackgroundTint : tokenBackgroundColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          keyboardBackgroundTintPref,
          tokenBackgroundColor != null
              ? keyboardBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_background_color_title)
              : keyboardBackgroundTintPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenBackgroundColor);
    }

    if (keyBackgroundOpacityPref != null) {
      keyBackgroundOpacityPref.setValue(
          keyBackgroundOpacityPercent != null ? keyBackgroundOpacityPercent : 100);
    }

    if (keyboardBackgroundOpacityPref != null) {
      keyboardBackgroundOpacityPref.setValue(
          keyboardBackgroundOpacityPercent != null ? keyboardBackgroundOpacityPercent : 100);
    }
  }
}
