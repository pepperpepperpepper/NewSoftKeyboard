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

final class KeyboardThemeCustomizationShadowTokensUi {

  @NonNull private final KeyboardThemeCustomizationShadowsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private EditTextPreference tokenSecondaryTextShadowColorPref;
  @Nullable private EditTextPreference tokenSecondaryTextShadowRadiusDpPref;
  @Nullable private EditTextPreference tokenSecondaryTextShadowOffsetXDpPref;
  @Nullable private EditTextPreference tokenSecondaryTextShadowOffsetYDpPref;

  @Nullable private EditTextPreference tokenSecondaryKeyBackgroundShadowColorPref;
  @Nullable private EditTextPreference tokenSecondaryKeyBackgroundShadowOffsetXDpPref;
  @Nullable private EditTextPreference tokenSecondaryKeyBackgroundShadowOffsetYDpPref;
  @Nullable private EditTextPreference tokenSecondaryKeyBackgroundShadowSpreadDpPref;

  KeyboardThemeCustomizationShadowTokensUi(
      @NonNull KeyboardThemeCustomizationShadowsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory shadowTokensCategory) {
    final Preference info = new Preference(context);
    info.setSelectable(false);
    info.setTitle(R.string.keyboard_theme_appearance_tokens_info_title);
    info.setSummary(R.string.keyboard_theme_appearance_tokens_info_summary);
    shadowTokensCategory.addPreference(info);

    final String tokenLabel =
        context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry);

    final PreferenceCategory tokenSecondaryTextShadow = new PreferenceCategory(context);
    tokenSecondaryTextShadow.setTitle(
        context.getString(R.string.keyboard_theme_appearance_text_shadow_title)
            + " ("
            + tokenLabel
            + ")");
    shadowTokensCategory.addPreference(tokenSecondaryTextShadow);

    tokenSecondaryTextShadowColorPref = new EditTextPreference(context);
    tokenSecondaryTextShadowColorPref.setKey("keyboard_theme_token_secondary_text_shadow_color");
    tokenSecondaryTextShadowColorPref.setPersistent(false);
    tokenSecondaryTextShadowColorPref.setTitle(
        R.string.keyboard_theme_appearance_text_shadow_color_title);
    tokenSecondaryTextShadowColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_color_title);
    tokenSecondaryTextShadowColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#AARRGGBB");
        });
    tokenSecondaryTextShadowColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryTextShadowColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryTextShadowColor(themeId, Color.parseColor(raw));
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
    tokenSecondaryTextShadow.addPreference(tokenSecondaryTextShadowColorPref);
    host.attachColorPickerDialog(tokenSecondaryTextShadowColorPref);

    tokenSecondaryTextShadowRadiusDpPref = new EditTextPreference(context);
    tokenSecondaryTextShadowRadiusDpPref.setKey(
        "keyboard_theme_token_secondary_text_shadow_radius_dp");
    tokenSecondaryTextShadowRadiusDpPref.setPersistent(false);
    tokenSecondaryTextShadowRadiusDpPref.setTitle(
        R.string.keyboard_theme_appearance_text_shadow_radius_title);
    tokenSecondaryTextShadowRadiusDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_radius_title);
    tokenSecondaryTextShadowRadiusDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER);
          edit.setHint("0");
        });
    tokenSecondaryTextShadowRadiusDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryTextShadowRadiusDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryTextShadowRadiusDp(
                themeId, Math.max(0, Integer.parseInt(raw)));
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
    tokenSecondaryTextShadow.addPreference(tokenSecondaryTextShadowRadiusDpPref);

    tokenSecondaryTextShadowOffsetXDpPref = new EditTextPreference(context);
    tokenSecondaryTextShadowOffsetXDpPref.setKey(
        "keyboard_theme_token_secondary_text_shadow_offset_x_dp");
    tokenSecondaryTextShadowOffsetXDpPref.setPersistent(false);
    tokenSecondaryTextShadowOffsetXDpPref.setTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_x_title);
    tokenSecondaryTextShadowOffsetXDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_x_title);
    tokenSecondaryTextShadowOffsetXDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    tokenSecondaryTextShadowOffsetXDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryTextShadowOffsetXDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryTextShadowOffsetXDp(
                themeId, Integer.parseInt(raw));
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
    tokenSecondaryTextShadow.addPreference(tokenSecondaryTextShadowOffsetXDpPref);

    tokenSecondaryTextShadowOffsetYDpPref = new EditTextPreference(context);
    tokenSecondaryTextShadowOffsetYDpPref.setKey(
        "keyboard_theme_token_secondary_text_shadow_offset_y_dp");
    tokenSecondaryTextShadowOffsetYDpPref.setPersistent(false);
    tokenSecondaryTextShadowOffsetYDpPref.setTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_y_title);
    tokenSecondaryTextShadowOffsetYDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_text_shadow_offset_y_title);
    tokenSecondaryTextShadowOffsetYDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    tokenSecondaryTextShadowOffsetYDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryTextShadowOffsetYDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryTextShadowOffsetYDp(
                themeId, Integer.parseInt(raw));
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
    tokenSecondaryTextShadow.addPreference(tokenSecondaryTextShadowOffsetYDpPref);

    final PreferenceCategory tokenSecondaryKeyShadow = new PreferenceCategory(context);
    tokenSecondaryKeyShadow.setTitle(
        context.getString(R.string.keyboard_theme_appearance_key_shadow_title)
            + " ("
            + tokenLabel
            + ")");
    shadowTokensCategory.addPreference(tokenSecondaryKeyShadow);

    tokenSecondaryKeyBackgroundShadowColorPref = new EditTextPreference(context);
    tokenSecondaryKeyBackgroundShadowColorPref.setKey(
        "keyboard_theme_token_secondary_key_shadow_color");
    tokenSecondaryKeyBackgroundShadowColorPref.setPersistent(false);
    tokenSecondaryKeyBackgroundShadowColorPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_color_title);
    tokenSecondaryKeyBackgroundShadowColorPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_color_title);
    tokenSecondaryKeyBackgroundShadowColorPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#AARRGGBB");
        });
    tokenSecondaryKeyBackgroundShadowColorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryKeyBackgroundShadowColor(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryKeyBackgroundShadowColor(
                themeId, Color.parseColor(raw));
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
    tokenSecondaryKeyShadow.addPreference(tokenSecondaryKeyBackgroundShadowColorPref);
    host.attachColorPickerDialog(tokenSecondaryKeyBackgroundShadowColorPref);

    tokenSecondaryKeyBackgroundShadowOffsetXDpPref = new EditTextPreference(context);
    tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setKey(
        "keyboard_theme_token_secondary_key_shadow_offset_x_dp");
    tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setPersistent(false);
    tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_x_title);
    tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_x_title);
    tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryKeyBackgroundShadowOffsetXDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryKeyBackgroundShadowOffsetXDp(
                themeId, Integer.parseInt(raw));
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
    tokenSecondaryKeyShadow.addPreference(tokenSecondaryKeyBackgroundShadowOffsetXDpPref);

    tokenSecondaryKeyBackgroundShadowOffsetYDpPref = new EditTextPreference(context);
    tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setKey(
        "keyboard_theme_token_secondary_key_shadow_offset_y_dp");
    tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setPersistent(false);
    tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_y_title);
    tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_offset_y_title);
    tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
          edit.setHint("0");
        });
    tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryKeyBackgroundShadowOffsetYDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryKeyBackgroundShadowOffsetYDp(
                themeId, Integer.parseInt(raw));
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
    tokenSecondaryKeyShadow.addPreference(tokenSecondaryKeyBackgroundShadowOffsetYDpPref);

    tokenSecondaryKeyBackgroundShadowSpreadDpPref = new EditTextPreference(context);
    tokenSecondaryKeyBackgroundShadowSpreadDpPref.setKey(
        "keyboard_theme_token_secondary_key_shadow_spread_dp");
    tokenSecondaryKeyBackgroundShadowSpreadDpPref.setPersistent(false);
    tokenSecondaryKeyBackgroundShadowSpreadDpPref.setTitle(
        R.string.keyboard_theme_appearance_key_shadow_spread_title);
    tokenSecondaryKeyBackgroundShadowSpreadDpPref.setDialogTitle(
        R.string.keyboard_theme_appearance_key_shadow_spread_title);
    tokenSecondaryKeyBackgroundShadowSpreadDpPref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setInputType(InputType.TYPE_CLASS_NUMBER);
          edit.setHint("0");
        });
    tokenSecondaryKeyBackgroundShadowSpreadDpPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            themeOverridesStore.clearTokenSecondaryKeyBackgroundShadowSpreadDp(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryKeyBackgroundShadowSpreadDp(
                themeId, Math.max(0, Integer.parseInt(raw)));
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
    tokenSecondaryKeyShadow.addPreference(tokenSecondaryKeyBackgroundShadowSpreadDpPref);
  }

  boolean hasAnyShadowsOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return store.getTokenSecondaryTextShadowColor(themeId) != null
        || store.getTokenSecondaryTextShadowRadiusDp(themeId) != null
        || store.getTokenSecondaryTextShadowOffsetXDp(themeId) != null
        || store.getTokenSecondaryTextShadowOffsetYDp(themeId) != null
        || store.getTokenSecondaryKeyBackgroundShadowColor(themeId) != null
        || store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(themeId) != null
        || store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(themeId) != null
        || store.getTokenSecondaryKeyBackgroundShadowSpreadDp(themeId) != null;
  }

  void refreshState(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return;

    final Integer tokenSecondaryTextShadowColor = store.getTokenSecondaryTextShadowColor(themeId);
    final Integer tokenSecondaryTextShadowRadiusDp =
        store.getTokenSecondaryTextShadowRadiusDp(themeId);
    final Integer tokenSecondaryTextShadowOffsetXDp =
        store.getTokenSecondaryTextShadowOffsetXDp(themeId);
    final Integer tokenSecondaryTextShadowOffsetYDp =
        store.getTokenSecondaryTextShadowOffsetYDp(themeId);

    final Integer tokenSecondaryKeyShadowColor =
        store.getTokenSecondaryKeyBackgroundShadowColor(themeId);
    final Integer tokenSecondaryKeyShadowOffsetXDp =
        store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(themeId);
    final Integer tokenSecondaryKeyShadowOffsetYDp =
        store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(themeId);
    final Integer tokenSecondaryKeyShadowSpreadDp =
        store.getTokenSecondaryKeyBackgroundShadowSpreadDp(themeId);

    if (tokenSecondaryTextShadowColorPref != null) {
      tokenSecondaryTextShadowColorPref.setText(
          tokenSecondaryTextShadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(tokenSecondaryTextShadowColor)
              : "");
      tokenSecondaryTextShadowColorPref.setSummary(
          tokenSecondaryTextShadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(tokenSecondaryTextShadowColor)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowColorPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationShadowsSection.setColorIcon(
          tokenSecondaryTextShadowColorPref, tokenSecondaryTextShadowColor);
    }
    if (tokenSecondaryTextShadowRadiusDpPref != null) {
      tokenSecondaryTextShadowRadiusDpPref.setText(
          tokenSecondaryTextShadowRadiusDp != null
              ? String.valueOf(tokenSecondaryTextShadowRadiusDp)
              : "");
      tokenSecondaryTextShadowRadiusDpPref.setSummary(
          tokenSecondaryTextShadowRadiusDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowRadiusDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  tokenSecondaryTextShadowRadiusDp)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowRadiusDpPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
    }
    if (tokenSecondaryTextShadowOffsetXDpPref != null) {
      tokenSecondaryTextShadowOffsetXDpPref.setText(
          tokenSecondaryTextShadowOffsetXDp != null
              ? String.valueOf(tokenSecondaryTextShadowOffsetXDp)
              : "");
      tokenSecondaryTextShadowOffsetXDpPref.setSummary(
          tokenSecondaryTextShadowOffsetXDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowOffsetXDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  tokenSecondaryTextShadowOffsetXDp)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowOffsetXDpPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
    }
    if (tokenSecondaryTextShadowOffsetYDpPref != null) {
      tokenSecondaryTextShadowOffsetYDpPref.setText(
          tokenSecondaryTextShadowOffsetYDp != null
              ? String.valueOf(tokenSecondaryTextShadowOffsetYDp)
              : "");
      tokenSecondaryTextShadowOffsetYDpPref.setSummary(
          tokenSecondaryTextShadowOffsetYDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowOffsetYDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  tokenSecondaryTextShadowOffsetYDp)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryTextShadowOffsetYDpPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
    }

    if (tokenSecondaryKeyBackgroundShadowColorPref != null) {
      tokenSecondaryKeyBackgroundShadowColorPref.setText(
          tokenSecondaryKeyShadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(tokenSecondaryKeyShadowColor)
              : "");
      tokenSecondaryKeyBackgroundShadowColorPref.setSummary(
          tokenSecondaryKeyShadowColor != null
              ? KeyboardThemeCustomizationShadowsSection.formatColor(tokenSecondaryKeyShadowColor)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowColorPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationShadowsSection.setColorIcon(
          tokenSecondaryKeyBackgroundShadowColorPref, tokenSecondaryKeyShadowColor);
    }
    if (tokenSecondaryKeyBackgroundShadowOffsetXDpPref != null) {
      tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setText(
          tokenSecondaryKeyShadowOffsetXDp != null
              ? String.valueOf(tokenSecondaryKeyShadowOffsetXDp)
              : "");
      tokenSecondaryKeyBackgroundShadowOffsetXDpPref.setSummary(
          tokenSecondaryKeyShadowOffsetXDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowOffsetXDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  tokenSecondaryKeyShadowOffsetXDp)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowOffsetXDpPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
    }
    if (tokenSecondaryKeyBackgroundShadowOffsetYDpPref != null) {
      tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setText(
          tokenSecondaryKeyShadowOffsetYDp != null
              ? String.valueOf(tokenSecondaryKeyShadowOffsetYDp)
              : "");
      tokenSecondaryKeyBackgroundShadowOffsetYDpPref.setSummary(
          tokenSecondaryKeyShadowOffsetYDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowOffsetYDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  tokenSecondaryKeyShadowOffsetYDp)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowOffsetYDpPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
    }
    if (tokenSecondaryKeyBackgroundShadowSpreadDpPref != null) {
      tokenSecondaryKeyBackgroundShadowSpreadDpPref.setText(
          tokenSecondaryKeyShadowSpreadDp != null
              ? String.valueOf(tokenSecondaryKeyShadowSpreadDp)
              : "");
      tokenSecondaryKeyBackgroundShadowSpreadDpPref.setSummary(
          tokenSecondaryKeyShadowSpreadDp != null
              ? KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowSpreadDpPref,
                  R.string.keyboard_theme_appearance_text_shadow_dp_value_summary,
                  tokenSecondaryKeyShadowSpreadDp)
              : KeyboardThemeCustomizationShadowsSection.contextString(
                  tokenSecondaryKeyBackgroundShadowSpreadDpPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
    }
  }

  void dispose() {}
}
