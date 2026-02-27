package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.ThemeCustomizationSearchEntry;

final class AppearanceOwnerRegistryThemeCustomizationSearchEntries {

  private AppearanceOwnerRegistryThemeCustomizationSearchEntries() {}

  @NonNull
  static List<ThemeCustomizationSearchEntry> buildThemeCustomizationSearchEntries() {
    final List<ThemeCustomizationSearchEntry> out = new ArrayList<>();

    // Source-of-truth list for Settings Search theme customization entries.

    // Presets
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_presets_save_as_title,
            R.string.keyboard_theme_presets_save_as_summary,
            "keyboard_theme_presets_save_as"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_presets_export_title,
            R.string.keyboard_theme_presets_export_summary,
            "keyboard_theme_presets_export"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_presets_import_title,
            R.string.keyboard_theme_presets_import_summary,
            "keyboard_theme_presets_import"));

    // Background photo
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_pick_title,
            R.string.keyboard_theme_wallpaper_customization_pick_summary,
            "keyboard_theme_wallpaper_customization_pick"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_high_quality_title,
            R.string.keyboard_theme_wallpaper_customization_high_quality_summary,
            "keyboard_theme_wallpaper_customization_high_quality_import"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_rotate_title,
            R.string.keyboard_theme_wallpaper_customization_rotate_summary,
            "keyboard_theme_wallpaper_customization_rotate"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_scale_title,
            R.string.keyboard_theme_wallpaper_customization_scale_summary,
            "keyboard_theme_wallpaper_customization_scale_mode"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_anchor_title,
            R.string.keyboard_theme_wallpaper_customization_anchor_summary,
            "keyboard_theme_wallpaper_customization_anchor"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_background_layer_stack_title,
            R.string.keyboard_theme_wallpaper_customization_background_layer_stack_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_dim_title,
            R.string.keyboard_theme_wallpaper_customization_dim_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_gradient_title,
            R.string.keyboard_theme_wallpaper_customization_gradient_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_background_gradient_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_background_gradient_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_gradient_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_key_gradient_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_vignette_title,
            R.string.keyboard_theme_wallpaper_customization_vignette_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_background_vignette_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_background_vignette_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_vignette_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_key_vignette_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_grain_title,
            R.string.keyboard_theme_wallpaper_customization_grain_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_background_grain_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_background_grain_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_grain_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_key_grain_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_saturation_title,
            R.string.keyboard_theme_wallpaper_customization_saturation_summary,
            "keyboard_theme_wallpaper_customization_saturation"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_contrast_title,
            R.string.keyboard_theme_wallpaper_customization_contrast_summary,
            "keyboard_theme_wallpaper_customization_contrast"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_mode_title,
            R.string.keyboard_theme_wallpaper_customization_mode_summary,
            "keyboard_theme_wallpaper_customization_mode"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_opacity_title,
            R.string.keyboard_theme_wallpaper_customization_key_opacity_summary,
            "keyboard_theme_wallpaper_customization_key_opacity"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_special_key_opacity_title,
            R.string.keyboard_theme_wallpaper_customization_special_key_opacity_summary,
            "keyboard_theme_wallpaper_customization_special_key_opacity"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_modifier_key_opacity_title,
            R.string.keyboard_theme_wallpaper_customization_modifier_key_opacity_summary,
            "keyboard_theme_wallpaper_customization_modifier_key_opacity"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_enter_key_opacity_title,
            R.string.keyboard_theme_wallpaper_customization_enter_key_opacity_summary,
            "keyboard_theme_wallpaper_customization_enter_key_opacity"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_spacebar_opacity_title,
            R.string.keyboard_theme_wallpaper_customization_spacebar_opacity_summary,
            "keyboard_theme_wallpaper_customization_spacebar_opacity"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_key_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_key_blend_mode"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_layer_stack_title,
            R.string.keyboard_theme_wallpaper_customization_key_layer_stack_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_color_wash_color_title,
            R.string.keyboard_theme_wallpaper_customization_key_color_wash_color_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_color_wash_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_key_color_wash_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_highlight_title,
            R.string.keyboard_theme_wallpaper_customization_key_highlight_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_highlight_blend_mode_title,
            R.string.keyboard_theme_wallpaper_customization_key_highlight_blend_mode_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_key_layer_order_title,
            R.string.keyboard_theme_wallpaper_customization_key_layer_order_summary,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_quality_title,
            R.string.keyboard_theme_wallpaper_customization_quality_summary,
            "keyboard_theme_wallpaper_customization_quality"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_background_layer_order_title,
            R.string.keyboard_theme_wallpaper_customization_background_layer_order_summary,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_match_key_shape_title,
            R.string.keyboard_theme_wallpaper_customization_match_key_shape_summary,
            "keyboard_theme_wallpaper_customization_match_key_shape"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_try_now_title,
            R.string.keyboard_theme_wallpaper_customization_try_now_summary,
            "keyboard_theme_wallpaper_customization_try_now"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_apply_to_all_title,
            R.string.keyboard_theme_wallpaper_customization_apply_to_all_summary,
            "keyboard_theme_wallpaper_customization_apply_to_all"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_wallpaper_customization_reset_title,
            R.string.keyboard_theme_wallpaper_customization_reset_summary,
            "keyboard_theme_wallpaper_customization_reset"));

    // Colors
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_auto_readable_title,
            R.string.keyboard_theme_appearance_auto_readable_summary,
            "keyboard_theme_appearance_auto_readable_colors"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_auto_photo_colors_title,
            R.string.keyboard_theme_appearance_auto_photo_colors_summary,
            "keyboard_theme_appearance_auto_photo_colors"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_high_contrast_dark_title,
            R.string.keyboard_theme_appearance_high_contrast_dark_summary,
            "keyboard_theme_appearance_high_contrast_dark"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_key_background_opacity_title,
            R.string.keyboard_theme_appearance_key_background_opacity_summary,
            "keyboard_theme_override_key_background_opacity"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_keyboard_background_opacity_title,
            R.string.keyboard_theme_appearance_keyboard_background_opacity_summary,
            "keyboard_theme_override_keyboard_background_opacity"));

    // Typography
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_key_font_family_title,
            R.string.keyboard_theme_appearance_key_font_family_summary,
            "keyboard_theme_override_key_font_family"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_key_font_style_title,
            R.string.keyboard_theme_appearance_key_font_style_summary,
            "keyboard_theme_override_key_font_style"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_hint_font_family_title,
            R.string.keyboard_theme_appearance_hint_font_family_summary,
            "keyboard_theme_override_hint_font_family"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_hint_font_style_title,
            R.string.keyboard_theme_appearance_hint_font_style_summary,
            "keyboard_theme_override_hint_font_style"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_suggestion_font_family_title,
            R.string.keyboard_theme_appearance_suggestion_font_family_summary,
            "keyboard_theme_override_suggestion_font_family"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_suggestion_font_style_title,
            R.string.keyboard_theme_appearance_suggestion_font_style_summary,
            "keyboard_theme_override_suggestion_font_style"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_keyboard_name_font_family_title,
            R.string.keyboard_theme_appearance_keyboard_name_font_family_summary,
            "keyboard_theme_override_keyboard_name_font_family"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_keyboard_name_font_style_title,
            R.string.keyboard_theme_appearance_keyboard_name_font_style_summary,
            "keyboard_theme_override_keyboard_name_font_style"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_custom_font_import_title,
            R.string.keyboard_theme_appearance_custom_font_import_summary,
            "keyboard_theme_override_key_custom_font_import"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_custom_font_remove_title,
            R.string.keyboard_theme_appearance_custom_font_remove_summary,
            "keyboard_theme_override_key_custom_font_remove"));

    // Reset
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_reset_preset_title,
            R.string.keyboard_theme_appearance_reset_preset_summary,
            "keyboard_theme_appearance_reset_preset"));
    out.add(
        new ThemeCustomizationSearchEntry(
            R.string.keyboard_theme_appearance_reset_all_title,
            R.string.keyboard_theme_appearance_reset_all_summary,
            "keyboard_theme_appearance_reset_all"));

    return Collections.unmodifiableList(out);
  }
}
