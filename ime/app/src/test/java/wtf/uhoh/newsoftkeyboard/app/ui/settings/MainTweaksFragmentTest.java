package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import org.junit.Assert;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.ui.dev.DeveloperToolsFragment;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;

public class MainTweaksFragmentTest extends RobolectricFragmentTestCase<MainTweaksFragment> {
  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.mainTweaksFragment;
  }

  @Test
  public void testNavigateToDevTools() {
    MainTweaksFragment fragment = startFragment();

    final Preference preferenceDevTools = fragment.findPreference(MainTweaksFragment.DEV_TOOLS_KEY);
    preferenceDevTools.getOnPreferenceClickListener().onPreferenceClick(preferenceDevTools);

    TestRxSchedulers.foregroundFlushAllJobs();
    Fragment navigatedToFragment = getCurrentFragment();
    Assert.assertTrue(navigatedToFragment instanceof DeveloperToolsFragment);
  }
}
