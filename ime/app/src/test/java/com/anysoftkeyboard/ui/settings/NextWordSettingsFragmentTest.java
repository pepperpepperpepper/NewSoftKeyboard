package com.anysoftkeyboard.ui.settings;

import android.os.Build;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.RobolectricFragmentTestCase;
import com.anysoftkeyboard.rx.TestRxSchedulers;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
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

    com.anysoftkeyboard.rx.TestRxSchedulers.backgroundFlushAllJobs();
    TestRxSchedulers.foregroundFlushAllJobs();

    final Preference enStats = nextWordSettingsFragment.findPreference("en_stats");
    Assert.assertNotNull(enStats);
    Assert.assertEquals("en - English", enStats.getTitle());
  }

  @Test
  public void testShowsNeuralFailureSummary() {
    final NextWordSettingsFragment fragment = startFragment();

    com.anysoftkeyboard.rx.TestRxSchedulers.backgroundFlushAllJobs();
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
