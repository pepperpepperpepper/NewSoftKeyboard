package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Build;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class NextWordSettingsFragmentTest
    extends RobolectricFragmentTestCase<NextWordSettingsFragment> {
  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.nextWordSettingsFragment;
  }

  @Test
  public void testShowLanguageStats() {
    final NextWordSettingsFragment nextWordSettingsFragment = startFragment();

    wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers.backgroundFlushAllJobs();
    TestRxSchedulers.foregroundFlushAllJobs();

    final Preference enStats = nextWordSettingsFragment.findPreference("en_stats");
    Assert.assertNotNull(enStats);
    Assert.assertEquals("en - English", enStats.getTitle());
  }

  @Test
  public void testShowsNeuralFailureSummary() {
    final NextWordSettingsFragment fragment = startFragment();

    wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers.backgroundFlushAllJobs();
    TestRxSchedulers.foregroundFlushAllJobs();

    final long timestamp = System.currentTimeMillis();
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_prediction_engine_last_neural_error, timestamp + "|runtime failure");

    final ListPreference enginePreference =
        (ListPreference)
            fragment.findPreference(
                fragment.getString(R.string.settings_key_prediction_engine_mode));
    Assert.assertNotNull(enginePreference);
    final CharSequence summary = enginePreference.getSummary();
    Assert.assertNotNull(summary);
    final String summaryText = summary.toString();
    Assert.assertTrue(summaryText.contains("Neural suggestions disabled"));
    Assert.assertTrue(summaryText.contains("runtime failure"));
  }
}
