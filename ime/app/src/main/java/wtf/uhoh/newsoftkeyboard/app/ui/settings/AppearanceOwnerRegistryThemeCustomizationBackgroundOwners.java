package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.Owner;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.Scope;

final class AppearanceOwnerRegistryThemeCustomizationBackgroundOwners {

  private AppearanceOwnerRegistryThemeCustomizationBackgroundOwners() {}

  static void addOwners(@NonNull List<Owner> out) {
    // Background / photo
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_pick",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_pick"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_high_quality_import",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_high_quality_import"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_rotate",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_rotate"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_scale_mode",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_scale_mode"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_anchor",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_anchor"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_saturation",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_saturation"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_contrast",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_contrast"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_brightness",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_brightness"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_temperature",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_temperature"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_mode",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_mode"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_key_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_key_opacity"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_special_key_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_special_key_opacity"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_modifier_key_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_modifier_key_opacity"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_enter_key_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_enter_key_opacity"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_spacebar_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_spacebar_opacity"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_key_blend_mode",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_key_blend_mode"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_key_layer_stack",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_background_layer_stack",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_match_key_shape",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_match_key_shape"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_quality",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_quality"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_background_layer_order",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_background_layer_order"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_key_layer_order",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_key_layer_order"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_try_now",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_try_now"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_apply_to_all",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_apply_to_all"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_reset",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_reset"));

    // Background layer stack editor (pseudo-keys for per-row deep-linking)
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_background_layer_stack_editor",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_background_layer_stack_editor_row",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_background_layer_stack"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_key_layer_stack_editor",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
    out.add(
        new Owner(
            "keyboard_theme_wallpaper_customization_key_layer_stack_editor_row",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_wallpaper_customization_key_layer_stack"));
  }
}
