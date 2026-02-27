package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationManualTextColorsUi {

  @NonNull private final KeyboardThemeCustomizationColorsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private EditTextPreference keyTextColorPref;
  @Nullable private EditTextPreference specialKeyTextColorPref;
  @Nullable private EditTextPreference modifierKeyTextColorPref;
  @Nullable private EditTextPreference enterKeyTextColorPref;
  @Nullable private EditTextPreference spacebarTextColorPref;
  @Nullable private EditTextPreference hintTextColorPref;

  KeyboardThemeCustomizationManualTextColorsUi(
      @NonNull KeyboardThemeCustomizationColorsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory colors) {
    keyTextColorPref = new EditTextPreference(context);
    keyTextColorPref.setKey("keyboard_theme_override_key_text_color");
    keyTextColorPref.setPersistent(false);
    keyTextColorPref.setTitle(R.string.keyboard_theme_appearance_key_text_color_title);
    keyTextColorPref.setDialogTitle(R.string.keyboard_theme_appearance_key_text_color_title);
    keyTextColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    keyTextColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearKeyTextColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setKeyTextColor(themeId, Color.parseColor(raw));
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
    colors.addPreference(keyTextColorPref);
    host.attachColorPickerDialog(keyTextColorPref);

    specialKeyTextColorPref = new EditTextPreference(context);
    specialKeyTextColorPref.setKey("keyboard_theme_override_special_key_text_color");
    specialKeyTextColorPref.setPersistent(false);
    specialKeyTextColorPref.setTitle(
        R.string.keyboard_theme_appearance_special_key_text_color_title);
    specialKeyTextColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_special_key_text_color_title);
    specialKeyTextColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    specialKeyTextColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearSpecialKeyTextColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setSpecialKeyTextColor(themeId, Color.parseColor(raw));
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
    colors.addPreference(specialKeyTextColorPref);
    host.attachColorPickerDialog(specialKeyTextColorPref);

    modifierKeyTextColorPref = new EditTextPreference(context);
    modifierKeyTextColorPref.setKey("keyboard_theme_override_modifier_key_text_color");
    modifierKeyTextColorPref.setPersistent(false);
    modifierKeyTextColorPref.setTitle(
        R.string.keyboard_theme_appearance_modifier_key_text_color_title);
    modifierKeyTextColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_modifier_key_text_color_title);
    modifierKeyTextColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    modifierKeyTextColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearModifierKeyTextColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setModifierKeyTextColor(themeId, Color.parseColor(raw));
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
    colors.addPreference(modifierKeyTextColorPref);
    host.attachColorPickerDialog(modifierKeyTextColorPref);

    enterKeyTextColorPref = new EditTextPreference(context);
    enterKeyTextColorPref.setKey("keyboard_theme_override_enter_key_text_color");
    enterKeyTextColorPref.setPersistent(false);
    enterKeyTextColorPref.setTitle(R.string.keyboard_theme_appearance_enter_key_text_color_title);
    enterKeyTextColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_enter_key_text_color_title);
    enterKeyTextColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    enterKeyTextColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearEnterKeyTextColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setEnterKeyTextColor(themeId, Color.parseColor(raw));
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
    colors.addPreference(enterKeyTextColorPref);
    host.attachColorPickerDialog(enterKeyTextColorPref);

    spacebarTextColorPref = new EditTextPreference(context);
    spacebarTextColorPref.setKey("keyboard_theme_override_spacebar_text_color");
    spacebarTextColorPref.setPersistent(false);
    spacebarTextColorPref.setTitle(R.string.keyboard_theme_appearance_spacebar_text_color_title);
    spacebarTextColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_spacebar_text_color_title);
    spacebarTextColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    spacebarTextColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearSpacebarTextColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setSpacebarTextColor(themeId, Color.parseColor(raw));
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
    colors.addPreference(spacebarTextColorPref);
    host.attachColorPickerDialog(spacebarTextColorPref);

    hintTextColorPref = new EditTextPreference(context);
    hintTextColorPref.setKey("keyboard_theme_override_hint_text_color");
    hintTextColorPref.setPersistent(false);
    hintTextColorPref.setTitle(R.string.keyboard_theme_appearance_hint_text_color_title);
    hintTextColorPref.setDialogTitle(R.string.keyboard_theme_appearance_hint_text_color_title);
    hintTextColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    hintTextColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearHintTextColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setHintTextColor(themeId, Color.parseColor(raw));
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
    colors.addPreference(hintTextColorPref);
    host.attachColorPickerDialog(hintTextColorPref);
  }

  void refreshState(@NonNull String themeId) {
    final Integer tokenPrimaryTextColor =
        themeOverridesStore != null ? themeOverridesStore.getTokenPrimaryTextColor(themeId) : null;
    final Integer tokenSecondaryTextColor =
        themeOverridesStore != null
            ? themeOverridesStore.getTokenSecondaryTextColor(themeId)
            : null;
    final Integer tokenAccentColor =
        themeOverridesStore != null ? themeOverridesStore.getTokenAccentColor(themeId) : null;

    final Integer keyTextColor =
        themeOverridesStore != null ? themeOverridesStore.getKeyTextColor(themeId) : null;
    final Integer specialKeyTextColor =
        themeOverridesStore != null ? themeOverridesStore.getSpecialKeyTextColor(themeId) : null;
    final Integer modifierKeyTextColor =
        themeOverridesStore != null ? themeOverridesStore.getModifierKeyTextColor(themeId) : null;
    final Integer enterKeyTextColor =
        themeOverridesStore != null ? themeOverridesStore.getEnterKeyTextColor(themeId) : null;
    final Integer spacebarTextColor =
        themeOverridesStore != null ? themeOverridesStore.getSpacebarTextColor(themeId) : null;
    final Integer hintTextColor =
        themeOverridesStore != null ? themeOverridesStore.getHintTextColor(themeId) : null;

    if (keyTextColorPref != null) {
      keyTextColorPref.setText(
          keyTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(keyTextColor)
              : "");
      keyTextColorPref.setSummary(
          keyTextColor != null
              ? keyTextColorPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(keyTextColor))
              : tokenPrimaryTextColor != null
                  ? keyTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenPrimaryTextColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      keyTextColorPref, R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          keyTextColorPref, keyTextColor != null ? keyTextColor : tokenPrimaryTextColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          keyTextColorPref,
          tokenPrimaryTextColor != null
              ? keyTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_primary_text_color_title)
              : keyTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenPrimaryTextColor);
    }

    if (specialKeyTextColorPref != null) {
      specialKeyTextColorPref.setText(
          specialKeyTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(specialKeyTextColor)
              : "");
      specialKeyTextColorPref.setSummary(
          specialKeyTextColor != null
              ? specialKeyTextColorPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(specialKeyTextColor))
              : tokenAccentColor != null
                  ? specialKeyTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenAccentColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      specialKeyTextColorPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          specialKeyTextColorPref,
          specialKeyTextColor != null ? specialKeyTextColor : tokenAccentColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          specialKeyTextColorPref,
          tokenAccentColor != null
              ? specialKeyTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_accent_color_title)
              : specialKeyTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenAccentColor);
    }

    if (modifierKeyTextColorPref != null) {
      modifierKeyTextColorPref.setText(
          modifierKeyTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(modifierKeyTextColor)
              : "");
      modifierKeyTextColorPref.setSummary(
          modifierKeyTextColor != null
              ? modifierKeyTextColorPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(modifierKeyTextColor))
              : tokenAccentColor != null
                  ? modifierKeyTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenAccentColor))
                  : specialKeyTextColor != null
                      ? modifierKeyTextColorPref
                          .getContext()
                          .getString(
                              R.string.keyboard_theme_appearance_color_inherited_summary,
                              KeyboardThemeCustomizationColorUiUtil.formatColor(
                                  specialKeyTextColor))
                      : KeyboardThemeCustomizationColorUiUtil.contextString(
                          modifierKeyTextColorPref,
                          R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          modifierKeyTextColorPref,
          modifierKeyTextColor != null
              ? modifierKeyTextColor
              : tokenAccentColor != null ? tokenAccentColor : specialKeyTextColor);

      final String modifierLinkedLabel =
          tokenAccentColor != null
              ? modifierKeyTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_accent_color_title)
              : specialKeyTextColor != null && specialKeyTextColorPref != null
                  ? modifierKeyTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_color_picker_linked_source_inherits_from,
                          specialKeyTextColorPref.getTitle())
                  : modifierKeyTextColorPref
                      .getContext()
                      .getString(R.string.keyboard_theme_appearance_color_default_summary);
      final Integer modifierLinkedColor =
          tokenAccentColor != null ? tokenAccentColor : specialKeyTextColor;
      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          modifierKeyTextColorPref, modifierLinkedLabel, modifierLinkedColor);
    }

    if (enterKeyTextColorPref != null) {
      enterKeyTextColorPref.setText(
          enterKeyTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(enterKeyTextColor)
              : "");
      enterKeyTextColorPref.setSummary(
          enterKeyTextColor != null
              ? enterKeyTextColorPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(enterKeyTextColor))
              : tokenAccentColor != null
                  ? enterKeyTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenAccentColor))
                  : specialKeyTextColor != null
                      ? enterKeyTextColorPref
                          .getContext()
                          .getString(
                              R.string.keyboard_theme_appearance_color_inherited_summary,
                              KeyboardThemeCustomizationColorUiUtil.formatColor(
                                  specialKeyTextColor))
                      : KeyboardThemeCustomizationColorUiUtil.contextString(
                          enterKeyTextColorPref,
                          R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          enterKeyTextColorPref,
          enterKeyTextColor != null
              ? enterKeyTextColor
              : tokenAccentColor != null ? tokenAccentColor : specialKeyTextColor);

      final String enterLinkedLabel =
          tokenAccentColor != null
              ? enterKeyTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_accent_color_title)
              : specialKeyTextColor != null && specialKeyTextColorPref != null
                  ? enterKeyTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_color_picker_linked_source_inherits_from,
                          specialKeyTextColorPref.getTitle())
                  : enterKeyTextColorPref
                      .getContext()
                      .getString(R.string.keyboard_theme_appearance_color_default_summary);
      final Integer enterLinkedColor =
          tokenAccentColor != null ? tokenAccentColor : specialKeyTextColor;
      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          enterKeyTextColorPref, enterLinkedLabel, enterLinkedColor);
    }

    if (spacebarTextColorPref != null) {
      spacebarTextColorPref.setText(
          spacebarTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(spacebarTextColor)
              : "");
      spacebarTextColorPref.setSummary(
          spacebarTextColor != null
              ? spacebarTextColorPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(spacebarTextColor))
              : tokenPrimaryTextColor != null
                  ? spacebarTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(tokenPrimaryTextColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      spacebarTextColorPref,
                      R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          spacebarTextColorPref,
          spacebarTextColor != null ? spacebarTextColor : tokenPrimaryTextColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          spacebarTextColorPref,
          tokenPrimaryTextColor != null
              ? spacebarTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_primary_text_color_title)
              : spacebarTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenPrimaryTextColor);
    }

    if (hintTextColorPref != null) {
      hintTextColorPref.setText(
          hintTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(hintTextColor)
              : "");
      hintTextColorPref.setSummary(
          hintTextColor != null
              ? hintTextColorPref
                  .getContext()
                  .getString(
                      R.string.keyboard_theme_appearance_color_overridden_summary,
                      KeyboardThemeCustomizationColorUiUtil.formatColor(hintTextColor))
              : tokenSecondaryTextColor != null
                  ? hintTextColorPref
                      .getContext()
                      .getString(
                          R.string.keyboard_theme_appearance_color_token_summary,
                          KeyboardThemeCustomizationColorUiUtil.formatColor(
                              tokenSecondaryTextColor))
                  : KeyboardThemeCustomizationColorUiUtil.contextString(
                      hintTextColorPref, R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(
          hintTextColorPref, hintTextColor != null ? hintTextColor : tokenSecondaryTextColor);

      KeyboardThemeCustomizationArgbColorPickerDialog.setLinkedValueInfo(
          hintTextColorPref,
          tokenSecondaryTextColor != null
              ? hintTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_token_secondary_text_color_title)
              : hintTextColorPref
                  .getContext()
                  .getString(R.string.keyboard_theme_appearance_color_default_summary),
          tokenSecondaryTextColor);
    }

    final boolean ensureReadableEnabled =
        themeOverridesStore != null && themeOverridesStore.isEnsureReadableTextEnabled(themeId);
    final boolean manualTextColorsEnabled = !ensureReadableEnabled;
    if (keyTextColorPref != null) keyTextColorPref.setEnabled(manualTextColorsEnabled);
    if (specialKeyTextColorPref != null)
      specialKeyTextColorPref.setEnabled(manualTextColorsEnabled);
    if (modifierKeyTextColorPref != null)
      modifierKeyTextColorPref.setEnabled(manualTextColorsEnabled);
    if (enterKeyTextColorPref != null) enterKeyTextColorPref.setEnabled(manualTextColorsEnabled);
    if (spacebarTextColorPref != null) spacebarTextColorPref.setEnabled(manualTextColorsEnabled);
    if (hintTextColorPref != null) hintTextColorPref.setEnabled(manualTextColorsEnabled);
  }
}
