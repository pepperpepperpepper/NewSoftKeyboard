package wtf.uhoh.newsoftkeyboard.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class NskApplicationBaseHapticsMigrationTest {

  private NskApplicationBase mApplication;
  private SharedPreferences mPrefs;
  private String mUseSystemVibrationKey;
  private String mKeypressDurationKey;
  private String mCustomDurationBackupKey;
  private int mDefaultDuration;

  @Before
  public void setUp() {
    mApplication = ApplicationProvider.getApplicationContext();
    mPrefs = DirectBootAwareSharedPreferences.create(mApplication);
    mUseSystemVibrationKey = mApplication.getString(R.string.settings_key_use_system_vibration);
    mKeypressDurationKey =
        mApplication.getString(R.string.settings_key_vibrate_on_key_press_duration_int);
    mCustomDurationBackupKey =
        mApplication.getString(
            R.string.settings_key_vibrate_on_key_press_duration_custom_backup_int);
    mDefaultDuration =
        mApplication
            .getResources()
            .getInteger(R.integer.settings_default_vibrate_on_key_press_duration_int);

    mPrefs.edit().clear().commit();
  }

  @Test
  @Config(sdk = {29})
  public void migrateSystemVibrationPrefsDefaultsToCustomDurationWhenLegacyFlagMissing() {
    invokeMigration();

    assertFalse(mPrefs.getBoolean(mUseSystemVibrationKey, true));
    assertEquals(mDefaultDuration, mPrefs.getInt(mKeypressDurationKey, -1));
  }

  @Test
  @Config(sdk = {29})
  public void migrateSystemVibrationPrefsRepairsLegacySystemDefaultState() {
    mPrefs
        .edit()
        .putBoolean(mUseSystemVibrationKey, true)
        .putInt(mKeypressDurationKey, -1)
        .putInt(mCustomDurationBackupKey, mDefaultDuration)
        .commit();

    invokeMigration();

    assertFalse(mPrefs.getBoolean(mUseSystemVibrationKey, true));
    assertEquals(mDefaultDuration, mPrefs.getInt(mKeypressDurationKey, -1));
    assertEquals(mDefaultDuration, mPrefs.getInt(mCustomDurationBackupKey, -1));
  }

  @Test
  @Config(sdk = {29})
  public void migrateSystemVibrationPrefsKeepsExplicitSystemModeWhenBackupIsCustom() {
    final int customBackup = 37;
    mPrefs
        .edit()
        .putBoolean(mUseSystemVibrationKey, true)
        .putInt(mKeypressDurationKey, -1)
        .putInt(mCustomDurationBackupKey, customBackup)
        .commit();

    invokeMigration();

    assertTrue(mPrefs.getBoolean(mUseSystemVibrationKey, false));
    assertEquals(-1, mPrefs.getInt(mKeypressDurationKey, 0));
    assertEquals(customBackup, mPrefs.getInt(mCustomDurationBackupKey, 0));
  }

  private void invokeMigration() {
    ReflectionHelpers.callInstanceMethod(
        mApplication,
        "migrateSystemVibrationPrefsToTriStateDuration",
        ReflectionHelpers.ClassParameter.from(SharedPreferences.class, mPrefs));
  }
}
