package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.Owner;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.Scope;

final class AppearanceOwnerRegistryOwners {

  private AppearanceOwnerRegistryOwners() {}

  @NonNull
  static List<Owner> buildOwners() {
    final List<Owner> out = new ArrayList<>();

    // Global + system overlays (single owners)
    out.add(
        new Owner("nav:theme_selector", Scope.GLOBAL, R.id.keyboardThemeSelectorFragment, null));
    out.add(
        new Owner(
            "nav:keyboard_theme_wallpaper_customization",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            null));
    out.add(
        new Owner(
            "nav:night_mode_settings", Scope.SYSTEM_OVERLAY, R.id.nightModeSettingsFragment, null));
    out.add(
        new Owner(
            "nav:power_saving_settings",
            Scope.SYSTEM_OVERLAY,
            R.id.powerSavingSettingsFragment,
            null));
    out.add(
        new Owner(
            "settings_key_apply_remote_app_colors",
            Scope.GLOBAL,
            R.id.keyboardThemeSelectorFragment,
            null));
    out.add(
        new Owner(
            "settings_key_night_mode_theme_control",
            Scope.SYSTEM_OVERLAY,
            R.id.nightModeSettingsFragment,
            null));
    out.add(
        new Owner(
            "settings_key_power_save_mode_theme_control",
            Scope.SYSTEM_OVERLAY,
            R.id.powerSavingSettingsFragment,
            null));
    out.add(
        new Owner(
            "settings_key_theme_case_type_override",
            Scope.GLOBAL,
            R.id.lookAndFeelSettingsFragment,
            null));

    // Customize appearance (per-preset) section anchors
    out.add(
        new Owner("section:presets", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));
    out.add(
        new Owner(
            "section:background", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));
    out.add(
        new Owner("section:colors", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));
    out.add(
        new Owner(
            "section:typography", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));
    out.add(
        new Owner("section:shadows", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));
    out.add(
        new Owner("section:overlays", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));
    out.add(
        new Owner("section:reset", Scope.PRESET, R.id.keyboardThemeCustomizationFragment, null));

    // Presets
    out.add(
        new Owner(
            "keyboard_theme_preset_selection",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_preset_selection"));
    out.add(
        new Owner(
            "keyboard_theme_presets_save_as",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_save_as"));
    out.add(
        new Owner(
            "keyboard_theme_presets_export",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_export"));
    out.add(
        new Owner(
            "keyboard_theme_presets_import",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_import"));
    out.add(
        new Owner(
            "keyboard_theme_presets_per_app_bindings",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_per_app_bindings"));
    out.add(
        new Owner(
            "keyboard_theme_presets_bind_last_used_app",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_bind_last_used_app"));
    out.add(
        new Owner(
            "keyboard_theme_presets_clear_last_used_app_binding",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_clear_last_used_app_binding"));
    out.add(
        new Owner(
            "keyboard_theme_presets_bind_any_app",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_presets_bind_any_app"));

    AppearanceOwnerRegistryThemeCustomizationBackgroundOwners.addOwners(out);
    AppearanceOwnerRegistryThemeCustomizationColorsOwners.addOwners(out);

    // Typography
    out.add(
        new Owner(
            "keyboard_theme_override_font_family",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_font_family"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_custom_font_import",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_custom_font_import"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_custom_font_remove",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_custom_font_remove"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_font_style",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_font_style"));
    out.add(
        new Owner(
            "keyboard_theme_override_hint_font_family",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_hint_font_family"));
    out.add(
        new Owner(
            "keyboard_theme_override_hint_font_style",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_hint_font_style"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_font_family",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_font_family"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_font_style",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_font_style"));
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_name_font_family",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_name_font_family"));
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_name_font_style",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_name_font_style"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_text_size_multiplier",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_text_size_multiplier"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_hint_text_size_multiplier",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_hint_text_size_multiplier"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_text_size_multiplier",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_text_size_multiplier"));
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_name_text_size_multiplier",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_name_text_size_multiplier"));
    out.add(
        new Owner(
            "keyboard_theme_override_label_text_size_multiplier",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_label_text_size_multiplier"));
    out.add(
        new Owner(
            "keyboard_theme_override_label_show_hints",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_label_show_hints"));
    out.add(
        new Owner(
            "keyboard_theme_override_label_show_additional_keys",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_label_show_additional_keys"));
    out.add(
        new Owner(
            "keyboard_theme_override_label_show_main_keys",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_label_show_main_keys"));
    out.add(
        new Owner(
            "keyboard_theme_override_label_highlight_action",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_label_highlight_action"));

    // Shadows / elevation
    out.add(
        new Owner(
            "keyboard_theme_override_shadow_enabled",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_shadow_enabled"));
    out.add(
        new Owner(
            "keyboard_theme_override_shadow_token_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_shadow_token_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_shadow_token_radius_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_shadow_token_radius_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_shadow_token_offset_x_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_shadow_token_offset_x_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_shadow_token_offset_y_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_shadow_token_offset_y_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_enabled",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_enabled"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_use_secondary_shadow_token",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_use_secondary_shadow_token"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_radius_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_radius_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_offset_x_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_offset_x_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_offset_y_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_offset_y_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_text_shadow_target",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_text_shadow_target"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_shadow_enabled",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_shadow_enabled"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_shadow_use_secondary_shadow_token",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_shadow_use_secondary_shadow_token"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_shadow_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_shadow_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_shadow_offset_x_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_shadow_offset_x_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_shadow_offset_y_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_shadow_offset_y_dp"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_shadow_spread_dp",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_shadow_spread_dp"));

    // Reset
    out.add(
        new Owner(
            "keyboard_theme_appearance_reset_preset",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_appearance_reset_preset"));
    out.add(
        new Owner(
            "keyboard_theme_appearance_reset_all",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_appearance_reset_all"));

    return Collections.unmodifiableList(out);
  }
}
