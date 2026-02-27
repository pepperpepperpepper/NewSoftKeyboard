package net.evendanan.pixel;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ActivityScenario;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.TestFragmentActivity;

@RunWith(NskRobolectricTestRunner.class)
public class SlidePreferenceTest {

  private TestPrefFragment mTestPrefFragment;
  private SlidePreference mTestSlide;
  private SharedPreferences mSharedPreferences;

  private void runTest(Runnable runnable) {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    try (var scenario = ActivityScenario.launch(TestFragmentActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.setContentView(R.layout.test_activity);
            activity.setTheme(R.style.TestApp);
            mTestPrefFragment = new TestPrefFragment();
            activity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.root_test_fragment, mTestPrefFragment, "test_fragment")
                .commit();

            TestRxSchedulers.foregroundFlushAllJobs();

            mTestSlide = mTestPrefFragment.findPreference("test_slide");
            Assert.assertNotNull(mTestSlide);

            runnable.run();
          });
    }
  }

  @Test
  public void testCorrectlyReadsAttrs() {
    runTest(
        () -> {
          Assert.assertEquals(12, mTestSlide.getMin());
          Assert.assertEquals(57, mTestSlide.getMax());
          Assert.assertEquals(23, mTestSlide.getValue());
        });
  }

  @Test
  public void testValueTemplateChanges() {
    runTest(
        () -> {
          TextView templateView = mTestPrefFragment.getView().findViewById(R.id.pref_current_value);
          Assert.assertNotNull(templateView);
          Assert.assertEquals("23 milliseconds", templateView.getText().toString());
          mTestSlide.onProgressChanged(
              Mockito.mock(SeekBar.class), 15 /*this is zero-based*/, false);
          Assert.assertEquals("27 milliseconds", templateView.getText().toString());
        });
  }

  @Test
  public void testSlideChanges() {
    runTest(
        () -> {
          mTestSlide.onProgressChanged(
              Mockito.mock(SeekBar.class), 15 /*this is zero-based*/, false);
          Assert.assertEquals(15 + mTestSlide.getMin(), mTestSlide.getValue());
          Assert.assertEquals(
              15 + mTestSlide.getMin(), mSharedPreferences.getInt("test_slide", 11));
        });
  }

  @Test
  public void testRecycledViewHolderDoesNotPersistToPreviousPreference() {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    mSharedPreferences.edit().clear().commit();
    mSharedPreferences.edit().putInt("test_slide_one", 23).putInt("test_slide_two", 45).commit();

    try (var scenario = ActivityScenario.launch(TestFragmentActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.setContentView(R.layout.test_activity);
            activity.setTheme(R.style.TestApp);
            final var fragment = new TestTwoSlidesPrefFragment();
            activity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.root_test_fragment, fragment, "test_fragment")
                .commit();

            TestRxSchedulers.foregroundFlushAllJobs();

            final SlidePreference first = fragment.findPreference("test_slide_one");
            final SlidePreference second = fragment.findPreference("test_slide_two");
            Assert.assertNotNull(first);
            Assert.assertNotNull(second);

            final View view = LayoutInflater.from(activity).inflate(R.layout.slide_pref, null);
            final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(view);

            first.onBindViewHolder(holder);
            second.onBindViewHolder(holder);

            Assert.assertEquals(23, mSharedPreferences.getInt("test_slide_one", -1));
            Assert.assertEquals(45, mSharedPreferences.getInt("test_slide_two", -1));
          });
    }
  }

  public static class TestPrefFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      addPreferencesFromResource(R.xml.slide_pref_test);
    }
  }

  public static class TestTwoSlidesPrefFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      addPreferencesFromResource(R.xml.slide_pref_recycled_holder_test);
    }
  }
}
