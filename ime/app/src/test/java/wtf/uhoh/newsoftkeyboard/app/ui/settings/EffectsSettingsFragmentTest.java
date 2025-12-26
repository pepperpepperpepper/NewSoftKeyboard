package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.fragment.app.Fragment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class EffectsSettingsFragmentTest
    extends RobolectricFragmentTestCase<EffectsSettingsFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.effectsSettingsFragment;
  }

  @Test
  public void testNavigateToPowerSavingFragment() {
    final EffectsSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("settings_key_power_save_mode"));

    TestRxSchedulers.foregroundFlushAllJobs();
    final Fragment next = getCurrentFragment();
    Assert.assertTrue(next instanceof PowerSavingSettingsFragment);
  }
}
