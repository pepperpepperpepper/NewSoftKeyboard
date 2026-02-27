package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.R;

public final class KeyboardApiSettingsDeepLinks {

  static final String DESTINATION_SEARCH = "nav:search";
  static final String DESTINATION_PROGRAMMABLE_API_SETTINGS = "nav:programmable_api_settings";
  static final String DESTINATION_CATEGORY_KEYBOARDS_LANGUAGE_PACKS =
      "nav:category_keyboards_language_packs";
  static final String DESTINATION_CATEGORY_TYPING = "nav:category_typing";
  static final String DESTINATION_CATEGORY_LOOK_AND_FEEL = "nav:category_look_and_feel";
  static final String DESTINATION_CATEGORY_GESTURES_QUICK_KEYS = "nav:category_gestures_quick_keys";
  static final String DESTINATION_CATEGORY_CLIPBOARD = "nav:category_clipboard";
  static final String DESTINATION_CATEGORY_VOICE = "nav:category_voice";
  static final String DESTINATION_CATEGORY_TROUBLESHOOTING_BACKUP =
      "nav:category_troubleshooting_backup";
  static final String DESTINATION_CATEGORY_SETTINGS_UI_LAUNCHER =
      "nav:category_settings_ui_launcher";
  static final String DESTINATION_CATEGORY_HELP_ABOUT = "nav:category_help_about";

  static final String DESTINATION_KEYBOARDS_MANAGER = "nav:keyboards_manager";
  static final String DESTINATION_THEMES = "nav:theme_selector";
  static final String DESTINATION_QUICK_KEYS_MANAGER = "nav:quick_keys_manager";
  static final String DESTINATION_POWER_SAVING_SETTINGS = "nav:power_saving_settings";
  static final String DESTINATION_NIGHT_MODE_SETTINGS = "nav:night_mode_settings";

  static final String DESTINATION_DEVELOPER_TOOLS = "nav:developer_tools";

  private KeyboardApiSettingsDeepLinks() {}

  @Nullable
  public static String toDeeplinkUri(@NonNull Context context, @NonNull String destinationId) {
    switch (destinationId) {
      case DESTINATION_SEARCH:
        return context.getString(R.string.deeplink_url_settings_search);
      case DESTINATION_PROGRAMMABLE_API_SETTINGS:
        return context.getString(R.string.deeplink_url_programmable_api_settings);
      case DESTINATION_CATEGORY_KEYBOARDS_LANGUAGE_PACKS:
        return context.getString(R.string.deeplink_url_keyboards_language_packs);
      case DESTINATION_CATEGORY_TYPING:
        return context.getString(R.string.deeplink_url_typing);
      case DESTINATION_CATEGORY_LOOK_AND_FEEL:
        return context.getString(R.string.deeplink_url_look_and_feel);
      case DESTINATION_CATEGORY_GESTURES_QUICK_KEYS:
        return context.getString(R.string.deeplink_url_gestures_quick_keys);
      case DESTINATION_CATEGORY_CLIPBOARD:
        return context.getString(R.string.deeplink_url_clipboard);
      case DESTINATION_CATEGORY_VOICE:
        return context.getString(R.string.deeplink_url_voice);
      case DESTINATION_CATEGORY_TROUBLESHOOTING_BACKUP:
        return context.getString(R.string.deeplink_url_troubleshooting_backup);
      case DESTINATION_CATEGORY_SETTINGS_UI_LAUNCHER:
        return context.getString(R.string.deeplink_url_settings_ui_launcher);
      case DESTINATION_CATEGORY_HELP_ABOUT:
        return context.getString(R.string.deeplink_url_help_about);
      case DESTINATION_KEYBOARDS_MANAGER:
        return context.getString(R.string.deeplink_url_keyboards);
      case DESTINATION_THEMES:
        return context.getString(R.string.deeplink_url_themes);
      case DESTINATION_QUICK_KEYS_MANAGER:
        return context.getString(R.string.deeplink_url_quick_text);
      case DESTINATION_POWER_SAVING_SETTINGS:
        return context.getString(R.string.deeplink_url_power_saving);
      case DESTINATION_NIGHT_MODE_SETTINGS:
        return context.getString(R.string.deeplink_url_night_mode);
      case DESTINATION_DEVELOPER_TOOLS:
        return context.getString(R.string.deeplink_url_dev_tools);
      default:
        return null;
    }
  }

  @Nullable
  public static Integer toNavDestinationId(@NonNull String destinationId) {
    switch (destinationId) {
      case DESTINATION_SEARCH:
        return R.id.settingsSearchFragment;
      case DESTINATION_PROGRAMMABLE_API_SETTINGS:
        return R.id.programmableApiSettingsFragment;
      case DESTINATION_CATEGORY_KEYBOARDS_LANGUAGE_PACKS:
        return R.id.keyboardsAndLanguagePacksFragment;
      case DESTINATION_CATEGORY_TYPING:
        return R.id.typingSettingsFragment;
      case DESTINATION_CATEGORY_LOOK_AND_FEEL:
        return R.id.lookAndFeelSettingsFragment;
      case DESTINATION_CATEGORY_GESTURES_QUICK_KEYS:
        return R.id.gesturesAndQuickKeysSettingsFragment;
      case DESTINATION_CATEGORY_CLIPBOARD:
        return R.id.clipboardSettingsFragment;
      case DESTINATION_CATEGORY_VOICE:
        return R.id.voiceSettingsFragment;
      case DESTINATION_CATEGORY_TROUBLESHOOTING_BACKUP:
        return R.id.troubleshootingAndBackupSettingsFragment;
      case DESTINATION_CATEGORY_SETTINGS_UI_LAUNCHER:
        return R.id.settingsUiAndLauncherFragment;
      case DESTINATION_CATEGORY_HELP_ABOUT:
        return R.id.helpAndAboutFragment;
      case DESTINATION_KEYBOARDS_MANAGER:
        return R.id.keyboardAddOnBrowserFragment;
      case DESTINATION_THEMES:
        return R.id.keyboardThemeSelectorFragment;
      case DESTINATION_QUICK_KEYS_MANAGER:
        return R.id.quickTextKeysBrowseFragment;
      case DESTINATION_POWER_SAVING_SETTINGS:
        return R.id.powerSavingSettingsFragment;
      case DESTINATION_NIGHT_MODE_SETTINGS:
        return R.id.nightModeSettingsFragment;
      case DESTINATION_DEVELOPER_TOOLS:
        return R.id.developerToolsFragment;
      default:
        return null;
    }
  }
}
