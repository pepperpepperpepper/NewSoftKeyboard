package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupSupport;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupWizardActivity;

/**
 * The real launcher/settings entrypoint logic for NewSoftKeyboard.
 *
 * <p>We keep multiple manifest entrypoint class names for upgrade stability and compatibility with
 * older explicit intents (e.g., {@code com.menny.android.anysoftkeyboard.LauncherSettingsActivity}
 * in askCompat). Those entrypoints should delegate to this owned implementation.
 */
public class LauncherSettingsEntryActivity extends Activity {

  private static final String LAUNCHED_KEY = "LAUNCHED_KEY";

  /**
   * This flag will help us keeping this activity inside the task, thus returning to the TASK when
   * relaunching (and not to re-create the activity).
   */
  private boolean mLaunched = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) mLaunched = savedInstanceState.getBoolean(LAUNCHED_KEY, false);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mLaunched) {
      finish();
    } else {
      if (SetupSupport.isThisKeyboardEnabled(getApplication())) {
        startActivity(new Intent(this, MainSettingsActivity.class).putExtras(getIntent()));
      } else {
        startActivity(new Intent(this, SetupWizardActivity.class).putExtras(getIntent()));
      }
    }

    mLaunched = true;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(LAUNCHED_KEY, mLaunched);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mLaunched = savedInstanceState.getBoolean(LAUNCHED_KEY);
  }
}
