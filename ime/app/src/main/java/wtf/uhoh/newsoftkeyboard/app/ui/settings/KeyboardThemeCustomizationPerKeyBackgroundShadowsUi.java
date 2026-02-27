package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationShadowsSection.ShadowTarget;

final class KeyboardThemeCustomizationPerKeyBackgroundShadowsUi {

  @NonNull private final KeyboardThemeCustomizationShadowsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @NonNull
  private final KeyboardThemeCustomizationPerKeyShadowsUi.ShadowTargetProvider shadowTargetProvider;

  @Nullable private PreferenceCategory keyShadowCategory;

  @Nullable private Preference keyShadowStylePref;
  @Nullable private EditTextPreference keyBackgroundShadowColorPref;
  @Nullable private EditTextPreference keyBackgroundShadowOffsetXDpPref;
  @Nullable private EditTextPreference keyBackgroundShadowOffsetYDpPref;
  @Nullable private EditTextPreference keyBackgroundShadowSpreadDpPref;
  @Nullable private Preference resetKeyBackgroundShadowPref;

  KeyboardThemeCustomizationPerKeyBackgroundShadowsUi(
      @NonNull KeyboardThemeCustomizationShadowsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull
          KeyboardThemeCustomizationPerKeyShadowsUi.ShadowTargetProvider shadowTargetProvider) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.shadowTargetProvider = shadowTargetProvider;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory shadows) {
    keyShadowCategory = new PreferenceCategory(context);
    keyShadowCategory.setTitle(R.string.keyboard_theme_appearance_key_shadow_title);
    shadows.addPreference(keyShadowCategory);

    keyShadowStylePref = new Preference(context);
    keyShadowStylePref.setKey("keyboard_theme_override_key_background_shadow_style");
    keyShadowStylePref.setPersistent(false);
    keyShadowStylePref.setTitle(R.string.keyboard_theme_appearance_key_shadow_style_title);
    keyShadowStylePref.setSummary(R.string.keyboard_theme_appearance_key_shadow_style_summary);
    keyShadowStylePref.setOnPreferenceClickListener(
        ignored -> {
          showKeyShadowStyleDialog(context);
          return true;
        });
    keyShadowCategory.addPreference(keyShadowStylePref);

    keyBackgroundShadowColorPref = new EditTextPreference(context);
    keyBackgroundShadowColorPref.setKey("keyboard_theme_override_key_background_shadow_color");
    keyBackgroundShadowColorPref.setPersistent(false);
    keyBackgroundShadowColorPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_color_title);
    keyBackgroundShadowColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_color_title);
    keyBackgroundShadowColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#AARRGGBB");
        });
    keyBackgroundShadowColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            switch (shadowTarget) {
              case ALL_KEYS -> themeOverridesStore.clearKeyBackgroundShadowOverrides(themeId);
              case SPECIAL_KEYS ->
                  themeOverridesStore.clearSpecialKeyBackgroundShadowOverrides(themeId);
              case SPACEBAR ->
                  themeOverridesStore.clearSpacebarKeyBackgroundShadowOverrides(themeId);
              case MODIFIER_KEYS ->
                  themeOverridesStore.clearModifierKeyBackgroundShadowOverrides(themeId);
              case ENTER_KEY -> themeOverridesStore.clearEnterKeyBackgroundShadowOverrides(themeId);
            }
            host.refreshState();
            return true;
          }
          try {
            final int argb = Color.parseColor(raw);
            switch (shadowTarget) {
              case ALL_KEYS -> themeOverridesStore.setKeyBackgroundShadowColor(themeId, argb);
              case SPECIAL_KEYS ->
                  themeOverridesStore.setSpecialKeyBackgroundShadowColor(themeId, argb);
              case SPACEBAR ->
                  themeOverridesStore.setSpacebarKeyBackgroundShadowColor(themeId, argb);
              case MODIFIER_KEYS ->
                  themeOverridesStore.setModifierKeyBackgroundShadowColor(themeId, argb);
              case ENTER_KEY -> themeOverridesStore.setEnterKeyBackgroundShadowColor(themeId, argb);
            }
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
    keyShadowCategory.addPreference(keyBackgroundShadowColorPref);
    host.attachColorPickerDialog(keyBackgroundShadowColorPref);

    keyBackgroundShadowOffsetXDpPref = new EditTextPreference(context);
    keyBackgroundShadowOffsetXDpPref.setKey(
        "keyboard_theme_override_key_background_shadow_offset_x_dp");
    keyBackgroundShadowOffsetXDpPref.setPersistent(false);
    keyBackgroundShadowOffsetXDpPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_x_title);
    keyBackgroundShadowOffsetXDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_x_title);
    keyBackgroundShadowOffsetXDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    keyBackgroundShadowOffsetXDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            switch (shadowTarget) {
              case ALL_KEYS -> themeOverridesStore.clearKeyBackgroundShadowOffsetXDp(themeId);
              case SPECIAL_KEYS ->
                  themeOverridesStore.clearSpecialKeyBackgroundShadowOffsetXDp(themeId);
              case SPACEBAR ->
                  themeOverridesStore.clearSpacebarKeyBackgroundShadowOffsetXDp(themeId);
              case MODIFIER_KEYS ->
                  themeOverridesStore.clearModifierKeyBackgroundShadowOffsetXDp(themeId);
              case ENTER_KEY -> themeOverridesStore.clearEnterKeyBackgroundShadowOffsetXDp(themeId);
            }
            host.refreshState();
            return true;
          }
          try {
            final int value = Integer.parseInt(raw);
            switch (shadowTarget) {
              case ALL_KEYS -> themeOverridesStore.setKeyBackgroundShadowOffsetXDp(themeId, value);
              case SPECIAL_KEYS ->
                  themeOverridesStore.setSpecialKeyBackgroundShadowOffsetXDp(themeId, value);
              case SPACEBAR ->
                  themeOverridesStore.setSpacebarKeyBackgroundShadowOffsetXDp(themeId, value);
              case MODIFIER_KEYS ->
                  themeOverridesStore.setModifierKeyBackgroundShadowOffsetXDp(themeId, value);
              case ENTER_KEY ->
                  themeOverridesStore.setEnterKeyBackgroundShadowOffsetXDp(themeId, value);
            }
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
    keyShadowCategory.addPreference(keyBackgroundShadowOffsetXDpPref);

    keyBackgroundShadowOffsetYDpPref = new EditTextPreference(context);
    keyBackgroundShadowOffsetYDpPref.setKey(
        "keyboard_theme_override_key_background_shadow_offset_y_dp");
    keyBackgroundShadowOffsetYDpPref.setPersistent(false);
    keyBackgroundShadowOffsetYDpPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_y_title);
    keyBackgroundShadowOffsetYDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_y_title);
    keyBackgroundShadowOffsetYDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    keyBackgroundShadowOffsetYDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          if (raw.isEmpty()) {
            switch (shadowTarget) {
              case ALL_KEYS -> themeOverridesStore.clearKeyBackgroundShadowOffsetYDp(themeId);
              case SPECIAL_KEYS ->
                  themeOverridesStore.clearSpecialKeyBackgroundShadowOffsetYDp(themeId);
              case SPACEBAR ->
                  themeOverridesStore.clearSpacebarKeyBackgroundShadowOffsetYDp(themeId);
              case MODIFIER_KEYS ->
                  themeOverridesStore.clearModifierKeyBackgroundShadowOffsetYDp(themeId);
              case ENTER_KEY -> themeOverridesStore.clearEnterKeyBackgroundShadowOffsetYDp(themeId);
            }
            host.refreshState();
            return true;
          }
          try {
            final int value = Integer.parseInt(raw);
            switch (shadowTarget) {
              case ALL_KEYS -> themeOverridesStore.setKeyBackgroundShadowOffsetYDp(themeId, value);
              case SPECIAL_KEYS ->
                  themeOverridesStore.setSpecialKeyBackgroundShadowOffsetYDp(themeId, value);
              case SPACEBAR ->
                  themeOverridesStore.setSpacebarKeyBackgroundShadowOffsetYDp(themeId, value);
              case MODIFIER_KEYS ->
                  themeOverridesStore.setModifierKeyBackgroundShadowOffsetYDp(themeId, value);
              case ENTER_KEY ->
                  themeOverridesStore.setEnterKeyBackgroundShadowOffsetYDp(themeId, value);
            }
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
    keyShadowCategory.addPreference(keyBackgroundShadowOffsetYDpPref);

    keyBackgroundShadowSpreadDpPref = new EditTextPreference(context);
    keyBackgroundShadowSpreadDpPref.setKey(
        "keyboard_theme_override_key_background_shadow_spread_dp");
    keyBackgroundShadowSpreadDpPref.setPersistent(false);
    keyBackgroundShadowSpreadDpPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_spread_title);
    keyBackgroundShadowSpreadDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_spread_title);
    keyBackgroundShadowSpreadDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER);
          edit.setHint("0");
        });
    keyBackgroundShadowSpreadDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearKeyBackgroundShadowSpreadDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setKeyBackgroundShadowSpreadDp(themeId, Integer.parseInt(raw));
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
    keyShadowCategory.addPreference(keyBackgroundShadowSpreadDpPref);

    resetKeyBackgroundShadowPref = new Preference(context);
    resetKeyBackgroundShadowPref.setTitle(
        R.string.keyboard_theme_appearance_reset_key_shadow_title);
    resetKeyBackgroundShadowPref.setSummary(
        R.string.keyboard_theme_appearance_reset_key_shadow_summary);
    resetKeyBackgroundShadowPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
          switch (shadowTarget) {
            case ALL_KEYS -> themeOverridesStore.clearKeyBackgroundShadowOverrides(themeId);
            case SPECIAL_KEYS ->
                themeOverridesStore.clearSpecialKeyBackgroundShadowOverrides(themeId);
            case SPACEBAR -> themeOverridesStore.clearSpacebarKeyBackgroundShadowOverrides(themeId);
            case MODIFIER_KEYS ->
                themeOverridesStore.clearModifierKeyBackgroundShadowOverrides(themeId);
            case ENTER_KEY -> themeOverridesStore.clearEnterKeyBackgroundShadowOverrides(themeId);
          }
          Toast.makeText(
                  context,
                  R.string.keyboard_theme_appearance_reset_key_shadow_toast,
                  Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    keyShadowCategory.addPreference(resetKeyBackgroundShadowPref);
  }

  boolean hasAnyKeyShadowsOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return store.isKeyBackgroundShadowUseTokenSecondary(themeId)
        || store.isSpecialKeyBackgroundShadowUseTokenSecondary(themeId)
        || store.isSpacebarKeyBackgroundShadowUseTokenSecondary(themeId)
        || store.isModifierKeyBackgroundShadowUseTokenSecondary(themeId)
        || store.isEnterKeyBackgroundShadowUseTokenSecondary(themeId)
        || store.getKeyBackgroundShadowColor(themeId) != null
        || store.getKeyBackgroundShadowOffsetXDp(themeId) != null
        || store.getKeyBackgroundShadowOffsetYDp(themeId) != null
        || store.getKeyBackgroundShadowSpreadDp(themeId) != null
        || store.getSpecialKeyBackgroundShadowColor(themeId) != null
        || store.getSpecialKeyBackgroundShadowOffsetXDp(themeId) != null
        || store.getSpecialKeyBackgroundShadowOffsetYDp(themeId) != null
        || store.getSpacebarKeyBackgroundShadowColor(themeId) != null
        || store.getSpacebarKeyBackgroundShadowOffsetXDp(themeId) != null
        || store.getSpacebarKeyBackgroundShadowOffsetYDp(themeId) != null
        || store.getModifierKeyBackgroundShadowColor(themeId) != null
        || store.getModifierKeyBackgroundShadowOffsetXDp(themeId) != null
        || store.getModifierKeyBackgroundShadowOffsetYDp(themeId) != null
        || store.getEnterKeyBackgroundShadowColor(themeId) != null
        || store.getEnterKeyBackgroundShadowOffsetXDp(themeId) != null
        || store.getEnterKeyBackgroundShadowOffsetYDp(themeId) != null;
  }

  void refreshState(@NonNull String themeId, @NonNull ShadowTarget target) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return;

    final Preference keyShadowStyle = keyShadowStylePref;
    if (keyShadowStyle == null) return;
    final Context context = keyShadowStyle.getContext();

    final String targetLabel =
        KeyboardThemeCustomizationPerKeyShadowsUi.labelForShadowTarget(context, target);
    if (keyShadowCategory != null) {
      keyShadowCategory.setTitle(
          context.getString(R.string.keyboard_theme_appearance_key_shadow_title)
              + " ("
              + targetLabel
              + ")");
    }

    final Integer tokenSecondaryKeyShadowColor =
        store.getTokenSecondaryKeyBackgroundShadowColor(themeId);
    final Integer tokenSecondaryKeyShadowOffsetXDp =
        store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(themeId);
    final Integer tokenSecondaryKeyShadowOffsetYDp =
        store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(themeId);
    final Integer tokenSecondaryKeyShadowSpreadDp =
        store.getTokenSecondaryKeyBackgroundShadowSpreadDp(themeId);

    final boolean keyShadowUsesTokenSecondary =
        switch (target) {
          case ALL_KEYS -> store.isKeyBackgroundShadowUseTokenSecondary(themeId);
          case SPECIAL_KEYS -> store.isSpecialKeyBackgroundShadowUseTokenSecondary(themeId);
          case SPACEBAR -> store.isSpacebarKeyBackgroundShadowUseTokenSecondary(themeId);
          case MODIFIER_KEYS -> store.isModifierKeyBackgroundShadowUseTokenSecondary(themeId);
          case ENTER_KEY -> store.isEnterKeyBackgroundShadowUseTokenSecondary(themeId);
        };

    Integer keyShadowColor = null;
    Integer keyShadowOffsetXDp = null;
    Integer keyShadowOffsetYDp = null;
    switch (target) {
      case ALL_KEYS -> {
        keyShadowColor =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowColor
                : store.getKeyBackgroundShadowColor(themeId);
        keyShadowOffsetXDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetXDp
                : store.getKeyBackgroundShadowOffsetXDp(themeId);
        keyShadowOffsetYDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetYDp
                : store.getKeyBackgroundShadowOffsetYDp(themeId);
      }
      case SPECIAL_KEYS -> {
        keyShadowColor =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowColor
                : store.getSpecialKeyBackgroundShadowColor(themeId);
        keyShadowOffsetXDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetXDp
                : store.getSpecialKeyBackgroundShadowOffsetXDp(themeId);
        keyShadowOffsetYDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetYDp
                : store.getSpecialKeyBackgroundShadowOffsetYDp(themeId);
      }
      case SPACEBAR -> {
        keyShadowColor =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowColor
                : store.getSpacebarKeyBackgroundShadowColor(themeId);
        keyShadowOffsetXDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetXDp
                : store.getSpacebarKeyBackgroundShadowOffsetXDp(themeId);
        keyShadowOffsetYDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetYDp
                : store.getSpacebarKeyBackgroundShadowOffsetYDp(themeId);
      }
      case MODIFIER_KEYS -> {
        keyShadowColor =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowColor
                : store.getModifierKeyBackgroundShadowColor(themeId);
        keyShadowOffsetXDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetXDp
                : store.getModifierKeyBackgroundShadowOffsetXDp(themeId);
        keyShadowOffsetYDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetYDp
                : store.getModifierKeyBackgroundShadowOffsetYDp(themeId);
      }
      case ENTER_KEY -> {
        keyShadowColor =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowColor
                : store.getEnterKeyBackgroundShadowColor(themeId);
        keyShadowOffsetXDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetXDp
                : store.getEnterKeyBackgroundShadowOffsetXDp(themeId);
        keyShadowOffsetYDp =
            keyShadowUsesTokenSecondary
                ? tokenSecondaryKeyShadowOffsetYDp
                : store.getEnterKeyBackgroundShadowOffsetYDp(themeId);
      }
    }

    final Integer keyShadowSpreadDp =
        keyShadowUsesTokenSecondary
            ? tokenSecondaryKeyShadowSpreadDp
            : store.getKeyBackgroundShadowSpreadDp(themeId);
    final boolean hasAnyKeyShadowOverride =
        keyShadowUsesTokenSecondary
            || keyShadowColor != null
            || keyShadowOffsetXDp != null
            || keyShadowOffsetYDp != null
            || (target == ShadowTarget.ALL_KEYS && keyShadowSpreadDp != null);

    keyShadowStyle.setSummary(
        keyShadowUsesTokenSecondary
            ? context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry)
            : describeKeyShadowStyleForTarget(
                context,
                target,
                keyShadowColor,
                keyShadowOffsetXDp,
                keyShadowOffsetYDp,
                keyShadowSpreadDp));

    if (keyBackgroundShadowColorPref != null) {
      keyBackgroundShadowColorPref.setEnabled(!keyShadowUsesTokenSecondary);
      keyBackgroundShadowColorPref.setText(
          keyShadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(keyShadowColor)
              : "");
      keyBackgroundShadowColorPref.setSummary(
          keyShadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(keyShadowColor)
              : target == ShadowTarget.ALL_KEYS
                  ? KeyboardThemeCustomizationShadowsSection.contextString(
                      keyBackgroundShadowColorPref,
                      R.string.keyboard_theme_appearance_shadow_off_summary)
                  : KeyboardThemeCustomizationShadowsSection.contextString(
                      keyBackgroundShadowColorPref,
                      R.string.keyboard_theme_appearance_shadow_inherit_summary));
      KeyboardThemeCustomizationShadowsSection.setColorIcon(
          keyBackgroundShadowColorPref, keyShadowColor);
    }

    final boolean baseKeyShadowUsesTokenSecondary =
        store.isKeyBackgroundShadowUseTokenSecondary(themeId);
    final Integer baseKeyShadowColor =
        baseKeyShadowUsesTokenSecondary
            ? tokenSecondaryKeyShadowColor
            : store.getKeyBackgroundShadowColor(themeId);

    if (keyBackgroundShadowOffsetXDpPref != null) {
      final boolean keyShadowEnabled =
          target == ShadowTarget.ALL_KEYS
              ? keyShadowColor != null
              : keyShadowColor != null || baseKeyShadowColor != null;
      keyBackgroundShadowOffsetXDpPref.setEnabled(keyShadowEnabled && !keyShadowUsesTokenSecondary);
      keyBackgroundShadowOffsetXDpPref.setText(
          keyShadowOffsetXDp != null ? String.valueOf(keyShadowOffsetXDp) : "");
      keyBackgroundShadowOffsetXDpPref.setSummary(
          KeyboardThemeCustomizationShadowsSection.contextString(
              keyBackgroundShadowOffsetXDpPref,
              R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
              keyShadowOffsetXDp != null ? keyShadowOffsetXDp : 0));
    }

    if (keyBackgroundShadowOffsetYDpPref != null) {
      final boolean keyShadowEnabled =
          target == ShadowTarget.ALL_KEYS
              ? keyShadowColor != null
              : keyShadowColor != null || baseKeyShadowColor != null;
      keyBackgroundShadowOffsetYDpPref.setEnabled(keyShadowEnabled && !keyShadowUsesTokenSecondary);
      keyBackgroundShadowOffsetYDpPref.setText(
          keyShadowOffsetYDp != null ? String.valueOf(keyShadowOffsetYDp) : "");
      keyBackgroundShadowOffsetYDpPref.setSummary(
          KeyboardThemeCustomizationShadowsSection.contextString(
              keyBackgroundShadowOffsetYDpPref,
              R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
              keyShadowOffsetYDp != null ? keyShadowOffsetYDp : 0));
    }

    if (keyBackgroundShadowSpreadDpPref != null) {
      final boolean keyShadowEnabled = target == ShadowTarget.ALL_KEYS && keyShadowColor != null;
      keyBackgroundShadowSpreadDpPref.setEnabled(keyShadowEnabled && !keyShadowUsesTokenSecondary);
      keyBackgroundShadowSpreadDpPref.setText(
          keyShadowSpreadDp != null ? String.valueOf(keyShadowSpreadDp) : "");
      keyBackgroundShadowSpreadDpPref.setSummary(
          KeyboardThemeCustomizationShadowsSection.contextString(
              keyBackgroundShadowSpreadDpPref,
              R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
              keyShadowSpreadDp != null ? keyShadowSpreadDp : 0));
    }

    if (resetKeyBackgroundShadowPref != null) {
      resetKeyBackgroundShadowPref.setEnabled(hasAnyKeyShadowOverride);
    }
  }

  private void showKeyShadowStyleDialog(@NonNull Context context) {
    final String themeId = host.getActiveThemeIdOrNull();
    if (themeId == null || themeOverridesStore == null) return;

    final ShadowTarget shadowTarget = shadowTargetProvider.getShadowTarget();
    final CharSequence[] items =
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_style_flat_entry),
          context.getString(R.string.keyboard_theme_appearance_style_soft_entry),
          context.getString(R.string.keyboard_theme_appearance_style_elevated_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry)
        };
    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_theme_appearance_key_shadow_style_dialog_title)
        .setItems(
            items,
            (dialog, which) -> {
              dialog.dismiss();
              switch (which) {
                case 0:
                  switch (shadowTarget) {
                    case ALL_KEYS -> themeOverridesStore.clearKeyBackgroundShadowOverrides(themeId);
                    case SPECIAL_KEYS ->
                        themeOverridesStore.clearSpecialKeyBackgroundShadowOverrides(themeId);
                    case SPACEBAR ->
                        themeOverridesStore.clearSpacebarKeyBackgroundShadowOverrides(themeId);
                    case MODIFIER_KEYS ->
                        themeOverridesStore.clearModifierKeyBackgroundShadowOverrides(themeId);
                    case ENTER_KEY ->
                        themeOverridesStore.clearEnterKeyBackgroundShadowOverrides(themeId);
                  }
                  break;
                case 1:
                  setKeyShadowUseTokenSecondary(themeId, shadowTarget, false);
                  switch (shadowTarget) {
                    case ALL_KEYS -> {
                      themeOverridesStore.setKeyBackgroundShadowColor(themeId, 0x3300_0000);
                      themeOverridesStore.setKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setKeyBackgroundShadowOffsetYDp(themeId, 1);
                      themeOverridesStore.setKeyBackgroundShadowSpreadDp(themeId, 1);
                    }
                    case SPECIAL_KEYS -> {
                      themeOverridesStore.setSpecialKeyBackgroundShadowColor(themeId, 0x3300_0000);
                      themeOverridesStore.setSpecialKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setSpecialKeyBackgroundShadowOffsetYDp(themeId, 1);
                    }
                    case SPACEBAR -> {
                      themeOverridesStore.setSpacebarKeyBackgroundShadowColor(themeId, 0x3300_0000);
                      themeOverridesStore.setSpacebarKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setSpacebarKeyBackgroundShadowOffsetYDp(themeId, 1);
                    }
                    case MODIFIER_KEYS -> {
                      themeOverridesStore.setModifierKeyBackgroundShadowColor(themeId, 0x3300_0000);
                      themeOverridesStore.setModifierKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setModifierKeyBackgroundShadowOffsetYDp(themeId, 1);
                    }
                    case ENTER_KEY -> {
                      themeOverridesStore.setEnterKeyBackgroundShadowColor(themeId, 0x3300_0000);
                      themeOverridesStore.setEnterKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setEnterKeyBackgroundShadowOffsetYDp(themeId, 1);
                    }
                  }
                  break;
                case 2:
                  setKeyShadowUseTokenSecondary(themeId, shadowTarget, false);
                  switch (shadowTarget) {
                    case ALL_KEYS -> {
                      themeOverridesStore.setKeyBackgroundShadowColor(themeId, 0x6600_0000);
                      themeOverridesStore.setKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setKeyBackgroundShadowOffsetYDp(themeId, 2);
                      themeOverridesStore.setKeyBackgroundShadowSpreadDp(themeId, 2);
                    }
                    case SPECIAL_KEYS -> {
                      themeOverridesStore.setSpecialKeyBackgroundShadowColor(themeId, 0x6600_0000);
                      themeOverridesStore.setSpecialKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setSpecialKeyBackgroundShadowOffsetYDp(themeId, 2);
                    }
                    case SPACEBAR -> {
                      themeOverridesStore.setSpacebarKeyBackgroundShadowColor(themeId, 0x6600_0000);
                      themeOverridesStore.setSpacebarKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setSpacebarKeyBackgroundShadowOffsetYDp(themeId, 2);
                    }
                    case MODIFIER_KEYS -> {
                      themeOverridesStore.setModifierKeyBackgroundShadowColor(themeId, 0x6600_0000);
                      themeOverridesStore.setModifierKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setModifierKeyBackgroundShadowOffsetYDp(themeId, 2);
                    }
                    case ENTER_KEY -> {
                      themeOverridesStore.setEnterKeyBackgroundShadowColor(themeId, 0x6600_0000);
                      themeOverridesStore.setEnterKeyBackgroundShadowOffsetXDp(themeId, 0);
                      themeOverridesStore.setEnterKeyBackgroundShadowOffsetYDp(themeId, 2);
                    }
                  }
                  break;
                case 3:
                  switch (shadowTarget) {
                    case ALL_KEYS -> themeOverridesStore.clearKeyBackgroundShadowOverrides(themeId);
                    case SPECIAL_KEYS ->
                        themeOverridesStore.clearSpecialKeyBackgroundShadowOverrides(themeId);
                    case SPACEBAR ->
                        themeOverridesStore.clearSpacebarKeyBackgroundShadowOverrides(themeId);
                    case MODIFIER_KEYS ->
                        themeOverridesStore.clearModifierKeyBackgroundShadowOverrides(themeId);
                    case ENTER_KEY ->
                        themeOverridesStore.clearEnterKeyBackgroundShadowOverrides(themeId);
                  }
                  setKeyShadowUseTokenSecondary(themeId, shadowTarget, true);
                  break;
                default:
                  break;
              }
              host.refreshState();
            })
        .show();
  }

  private void setKeyShadowUseTokenSecondary(
      @NonNull String themeId, @NonNull ShadowTarget target, boolean enabled) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return;
    switch (target) {
      case ALL_KEYS -> store.setKeyBackgroundShadowUseTokenSecondary(themeId, enabled);
      case SPECIAL_KEYS -> store.setSpecialKeyBackgroundShadowUseTokenSecondary(themeId, enabled);
      case SPACEBAR -> store.setSpacebarKeyBackgroundShadowUseTokenSecondary(themeId, enabled);
      case MODIFIER_KEYS -> store.setModifierKeyBackgroundShadowUseTokenSecondary(themeId, enabled);
      case ENTER_KEY -> store.setEnterKeyBackgroundShadowUseTokenSecondary(themeId, enabled);
    }
  }

  @NonNull
  private static String describeKeyShadowStyleForTarget(
      @NonNull Context context,
      @NonNull ShadowTarget target,
      @Nullable Integer shadowColor,
      @Nullable Integer shadowOffsetXDp,
      @Nullable Integer shadowOffsetYDp,
      @Nullable Integer shadowSpreadDp) {
    if (target != ShadowTarget.ALL_KEYS
        && shadowColor == null
        && shadowOffsetXDp == null
        && shadowOffsetYDp == null) {
      return context.getString(R.string.keyboard_theme_appearance_shadow_inherit_summary);
    }
    return describeKeyShadowStyle(
        context, shadowColor, shadowOffsetXDp, shadowOffsetYDp, shadowSpreadDp);
  }

  @NonNull
  private static String describeKeyShadowStyle(
      @NonNull Context context,
      @Nullable Integer shadowColor,
      @Nullable Integer shadowOffsetXDp,
      @Nullable Integer shadowOffsetYDp,
      @Nullable Integer shadowSpreadDp) {
    if (shadowColor == null) {
      return context.getString(R.string.keyboard_theme_appearance_style_flat_entry);
    }

    if (shadowOffsetXDp == null || shadowOffsetYDp == null || shadowSpreadDp == null) {
      return context.getString(R.string.keyboard_theme_appearance_style_custom_entry);
    }

    if (shadowColor == 0x3300_0000
        && shadowOffsetXDp == 0
        && shadowOffsetYDp == 1
        && shadowSpreadDp == 1) {
      return context.getString(R.string.keyboard_theme_appearance_style_soft_entry);
    }

    if (shadowColor == 0x6600_0000
        && shadowOffsetXDp == 0
        && shadowOffsetYDp == 2
        && shadowSpreadDp == 2) {
      return context.getString(R.string.keyboard_theme_appearance_style_elevated_entry);
    }

    return context.getString(R.string.keyboard_theme_appearance_style_custom_entry);
  }

  void dispose() {}
}
