package wtf.uhoh.newsoftkeyboard.app.testing;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.NskLauncherSettingsActivity;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class NskApplicationTest {

  @Test
  public void testSettingsAppIcon() {
    final PackageManager packageManager = getApplicationContext().getPackageManager();
    final ComponentName legacyComponentName =
        new ComponentName(
            getApplicationContext(), "com.menny.android.anysoftkeyboard.LauncherSettingsActivity");
    final ComponentName nskComponentName =
        new ComponentName(getApplicationContext(), NskLauncherSettingsActivity.class);

    ComponentName componentName;
    try {
      packageManager.getActivityInfo(nskComponentName, 0);
      componentName = nskComponentName;
    } catch (PackageManager.NameNotFoundException e) {
      componentName = legacyComponentName;
    }

    Assert.assertEquals(
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        packageManager.getComponentEnabledSetting(componentName));

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_show_settings_app, false);

    Assert.assertEquals(
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        packageManager.getComponentEnabledSetting(componentName));

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_show_settings_app, true);

    Assert.assertEquals(
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        packageManager.getComponentEnabledSetting(componentName));
  }
}
