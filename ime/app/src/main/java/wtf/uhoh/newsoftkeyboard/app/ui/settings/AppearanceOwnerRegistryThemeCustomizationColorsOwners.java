package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.Owner;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AppearanceOwnerRegistry.Scope;

final class AppearanceOwnerRegistryThemeCustomizationColorsOwners {

  private AppearanceOwnerRegistryThemeCustomizationColorsOwners() {}

  static void addOwners(@NonNull List<Owner> out) {
    // Colors (auto)
    out.add(
        new Owner(
            "keyboard_theme_color_scheme_preset",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_scheme_preset"));
    out.add(
        new Owner(
            "keyboard_theme_color_scheme_preset_from_keyboard",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_scheme_preset_from_keyboard"));
    out.add(
        new Owner(
            "keyboard_theme_color_scheme_preset_from_wallpaper",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_scheme_preset_from_wallpaper"));
    out.add(
        new Owner(
            "keyboard_theme_color_scheme_preset_from_last_used_app",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_scheme_preset_from_last_used_app"));
    out.add(
        new Owner(
            "keyboard_theme_color_scheme_preset_from_any_app",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_scheme_preset_from_any_app"));
    out.add(
        new Owner(
            "keyboard_theme_color_scheme_preset_source_app_name",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_scheme_preset_source_app_name"));

    // Colors (manual)
    out.add(
        new Owner(
            "keyboard_theme_color_overrides_enabled",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_overrides_enabled"));
    out.add(
        new Owner(
            "keyboard_theme_color_overrides_ensure_readable",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_overrides_ensure_readable"));
    out.add(
        new Owner(
            "keyboard_theme_color_overrides_apply_all",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_color_overrides_apply_all"));

    // Keyboard background colors
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_background_opacity"));

    // Key background colors
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_background_opacity"));

    // Key pressed background colors
    out.add(
        new Owner(
            "keyboard_theme_override_key_pressed_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_pressed_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_pressed_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_pressed_background_opacity"));

    // Functional key background colors
    out.add(
        new Owner(
            "keyboard_theme_override_functional_key_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_functional_key_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_functional_key_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_functional_key_background_opacity"));

    // Spacebar background colors
    out.add(
        new Owner(
            "keyboard_theme_override_spacebar_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_spacebar_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_spacebar_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_spacebar_background_opacity"));

    // Enter key background colors
    out.add(
        new Owner(
            "keyboard_theme_override_enter_key_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_enter_key_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_enter_key_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_enter_key_background_opacity"));

    // Key text colors
    out.add(
        new Owner(
            "keyboard_theme_override_key_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_shifted_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_shifted_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_key_typed_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_key_typed_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_functional_key_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_functional_key_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_functional_key_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_functional_key_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_functional_key_shifted_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_functional_key_shifted_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_functional_key_typed_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_functional_key_typed_color"));

    // Labels
    out.add(
        new Owner(
            "keyboard_theme_override_spacebar_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_spacebar_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_spacebar_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_spacebar_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_enter_key_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_enter_key_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_enter_key_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_enter_key_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_action_key_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_action_key_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_action_key_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_action_key_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_settings_key_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_settings_key_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_settings_key_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_settings_key_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_bottom_row_key_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_bottom_row_key_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_bottom_row_key_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_bottom_row_key_hint_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_name_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_name_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_keyboard_name_hint_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_keyboard_name_hint_color"));

    // Suggestion strip
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_background_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_background_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_background_opacity",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_background_opacity"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_selected_text_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_selected_text_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_valid_word_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_valid_word_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_auto_complete_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_auto_complete_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_invalid_word_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_invalid_word_color"));
    out.add(
        new Owner(
            "keyboard_theme_override_suggestion_strip_typed_word_color",
            Scope.PRESET,
            R.id.keyboardThemeCustomizationFragment,
            "keyboard_theme_override_suggestion_strip_typed_word_color"));
  }
}
