package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.preference.Preference;
import org.junit.Test;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

public class LookAndFeelSettingsFragmentTest
    extends RobolectricFragmentTestCase<LookAndFeelSettingsFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.lookAndFeelSettingsFragment;
  }

  @Test
  @Config(sdk = {28})
  public void testHapticsPreferencesVisibleOnSdk28() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_key_press_duration_int, 0);
    final LookAndFeelSettingsFragment fragment = startFragment();

    final Preference vibrationDurationPref =
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_vibrate_on_key_press_duration_int));
    assertNotNull(vibrationDurationPref);
    assertTrue(vibrationDurationPref.isVisible());
    assertTrue(vibrationDurationPref.isEnabled());

    final Preference systemVibrationPref =
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_use_system_vibration));
    assertTrue(systemVibrationPref == null || !systemVibrationPref.isVisible());

    final Preference fallbackDurationPref =
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_system_vibration_fallback_duration_int));
    assertNotNull(fallbackDurationPref);
    assertFalse(fallbackDurationPref.isVisible());
    assertFalse(fallbackDurationPref.isEnabled());
  }

  @Test
  @Config(sdk = {29})
  public void testSystemVibrationPreferenceRemovedOnSdk29() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_key_press_duration_int, 0);
    final LookAndFeelSettingsFragment fragment = startFragment();

    final Preference systemVibrationPref =
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_use_system_vibration));
    assertTrue(systemVibrationPref == null || !systemVibrationPref.isVisible());

    final Preference fallbackDurationPref =
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_system_vibration_fallback_duration_int));
    assertNotNull(fallbackDurationPref);
    assertFalse(fallbackDurationPref.isVisible());
  }

  @Test
  public void testLongPressCheckboxDisabledWhenSliderIsOff() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_key_press_duration_int, 0);
    final LookAndFeelSettingsFragment fragment = startFragment();

    final Preference longPressPref =
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_vibrate_on_long_press));
    assertNotNull(longPressPref);
    assertFalse(longPressPref.isEnabled());
  }

  @Test
  public void testLongPressCheckboxEnabledWhenSliderHasDuration() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_key_press_duration_int, 20);
    final LookAndFeelSettingsFragment fragment = startFragment();

    final Preference longPressPref =
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_vibrate_on_long_press));
    assertNotNull(longPressPref);
    assertTrue(longPressPref.isEnabled());
  }

  @Test
  public void testLongPressCheckboxEnabledInSystemMode() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, true);
    final LookAndFeelSettingsFragment fragment = startFragment();

    final Preference longPressPref =
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_vibrate_on_long_press));
    assertNotNull(longPressPref);
    assertTrue(longPressPref.isEnabled());
  }
}
