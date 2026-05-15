package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.preference.Preference;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;

public class LookAndFeelSettingsFragmentTest
    extends RobolectricFragmentTestCase<LookAndFeelSettingsFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.lookAndFeelSettingsFragment;
  }

  @Test
  public void testEffectsShortcutPresent() {
    final LookAndFeelSettingsFragment fragment = startFragment();

    final Preference effectsShortcut = fragment.findPreference("nav:effects_settings");
    assertNotNull(effectsShortcut);
  }

  @Test
  public void testVibrationPrefsMovedToEffects() {
    final LookAndFeelSettingsFragment fragment = startFragment();

    assertNull(
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_vibrate_on_key_press_duration_int)));
    assertNull(
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_sound_on)));
    assertNull(
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_custom_keypress_sound_uri)));
  }
}
