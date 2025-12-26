package wtf.uhoh.newsoftkeyboard.app.testing;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.LauncherSettingsEntryActivity;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupSupport;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupWizardActivity;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class LauncherSettingsActivityTest {

  @Test
  public void testOnCreateWhenASKNotEnabled() throws Exception {
    // mocking ASK as disabled and inactive
    var application = RuntimeEnvironment.getApplication();
    InputMethodManagerShadow.setKeyboardEnabled(application, false);
    assertFalse(SetupSupport.isThisKeyboardEnabled(application));

    Settings.Secure.putString(
        getApplicationContext().getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD,
        new ComponentName("net.some.one.else", "net.some.one.else.IME").flattenToString());

    Assert.assertNull(
        Shadows.shadowOf((Application) getApplicationContext()).getNextStartedActivity());
    try (ActivityController<LauncherSettingsEntryActivity> controller =
        Robolectric.buildActivity(LauncherSettingsEntryActivity.class)) {
      controller.create().resume();
      Intent startWizardActivityIntent =
          Shadows.shadowOf((Application) getApplicationContext()).getNextStartedActivity();
      Assert.assertNotNull(startWizardActivityIntent);

      Intent expectIntent = new Intent(getApplicationContext(), SetupWizardActivity.class);

      Assert.assertEquals(expectIntent.getComponent(), startWizardActivityIntent.getComponent());
      Assert.assertEquals(expectIntent.getAction(), startWizardActivityIntent.getAction());
      Assert.assertFalse(
          startWizardActivityIntent.hasExtra(
              "FragmentChauffeurActivity_KEY_FRAGMENT_CLASS_TO_ADD"));
    }
  }

  @Test
  public void testOnCreateWhenASKEnabledAndActive() throws Exception {
    // mocking ASK as enable and inactive
    Settings.Secure.putString(
        getApplicationContext().getContentResolver(),
        Settings.Secure.ENABLED_INPUT_METHODS,
        new ComponentName("net.some.one.else", "net.some.one.else.IME").flattenToString()
            + ":"
            + new ComponentName(
                    getApplicationContext().getPackageName(),
                    getApplicationContext().getPackageName() + ".IME")
                .flattenToString());
    Settings.Secure.putString(
        getApplicationContext().getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD,
        new ComponentName(
                getApplicationContext().getPackageName(),
                getApplicationContext().getPackageName() + ".IME")
            .flattenToString());

    Assert.assertNull(
        Shadows.shadowOf((Application) getApplicationContext()).getNextStartedActivity());
    ActivityController<LauncherSettingsEntryActivity> controller =
        Robolectric.buildActivity(LauncherSettingsEntryActivity.class).create().resume();
    Intent startMainApp =
        Shadows.shadowOf((Application) getApplicationContext()).getNextStartedActivity();
    Assert.assertNotNull(startMainApp);

    Intent expectIntent = new Intent(controller.get(), MainSettingsActivity.class);

    Assert.assertEquals(expectIntent.getComponent(), startMainApp.getComponent());
    Assert.assertFalse(
        startMainApp.hasExtra("FragmentChauffeurActivity_KEY_FRAGMENT_CLASS_TO_ADD"));
  }

  @Test
  public void testOnCreateWhenASKEnabledAndInactive() throws Exception {
    // mocking ASK as enable and inactive
    Settings.Secure.putString(
        getApplicationContext().getContentResolver(),
        Settings.Secure.ENABLED_INPUT_METHODS,
        new ComponentName("net.some.one.else", "net.some.one.else.IME").flattenToString()
            + ":"
            + new ComponentName(
                    getApplicationContext().getPackageName(),
                    getApplicationContext().getPackageName() + ".IME")
                .flattenToString());
    Settings.Secure.putString(
        getApplicationContext().getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD,
        new ComponentName("net.some.one.else", "net.some.one.else.IME").flattenToString());

    Assert.assertNull(
        Shadows.shadowOf((Application) getApplicationContext()).getNextStartedActivity());
    ActivityController<LauncherSettingsEntryActivity> controller =
        Robolectric.buildActivity(LauncherSettingsEntryActivity.class).create().resume();
    Intent startMainApp =
        Shadows.shadowOf((Application) getApplicationContext()).getNextStartedActivity();
    Assert.assertNotNull(startMainApp);

    Intent expectIntent = new Intent(controller.get(), MainSettingsActivity.class);

    Assert.assertEquals(expectIntent.getComponent(), startMainApp.getComponent());
    Assert.assertFalse(
        startMainApp.hasExtra("FragmentChauffeurActivity_KEY_FRAGMENT_CLASS_TO_ADD"));
  }

  @Test
  public void testJustFinishIfResumedAgain() throws Exception {
    ActivityController<LauncherSettingsEntryActivity> controller =
        Robolectric.buildActivity(LauncherSettingsEntryActivity.class).create().resume();
    final Activity activity = controller.get();
    Assert.assertFalse(activity.isFinishing());
    controller.pause().stop();
    Assert.assertFalse(activity.isFinishing());
    controller.restart().resume();
    Assert.assertTrue(activity.isFinishing());
  }

  @Test
  public void testJustFinishIfCreatedAgain() throws Exception {
    ActivityController<LauncherSettingsEntryActivity> controller =
        Robolectric.buildActivity(LauncherSettingsEntryActivity.class).create().resume();
    Activity activity = controller.get();
    Assert.assertFalse(activity.isFinishing());
    controller.pause().stop();
    Assert.assertFalse(activity.isFinishing());
    Bundle state = new Bundle();
    controller.saveInstanceState(state).destroy();

    controller = Robolectric.buildActivity(LauncherSettingsEntryActivity.class).create(state);
    activity = controller.get();
    Assert.assertFalse(activity.isFinishing());
    controller.resume();
    Assert.assertTrue(activity.isFinishing());
  }
}
