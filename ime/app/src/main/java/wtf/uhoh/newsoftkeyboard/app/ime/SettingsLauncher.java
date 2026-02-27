package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Intent;
import com.anysoftkeyboard.api.KeyboardApiContract;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;

/** Launches the settings activity with optional navigation hints. */
public final class SettingsLauncher {

  private static final String SETTINGS_DESTINATION_TYPING = "nav:category_typing";

  private SettingsLauncher() {}

  public static void launch(android.content.Context context) {
    Intent intent = new Intent(context, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  public static void launchTypingSettings(android.content.Context context, String scrollToPrefKey) {
    Intent intent = new Intent(context, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(KeyboardApiContract.EXTRA_DESTINATION_ID, SETTINGS_DESTINATION_TYPING);
    intent.putExtra(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY, scrollToPrefKey);
    context.startActivity(intent);
  }

  public static void launchOpenAI(android.content.Context context) {
    Intent intent = new Intent(context, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("navigate_to_openai_settings", true);
    context.startActivity(intent);
  }
}
