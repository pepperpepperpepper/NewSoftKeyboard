package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.Application;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.junit.Assert;
import org.junit.Test;
import org.robolectric.Shadows;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;

public class AboutNewSoftKeyboardFragmentTest
    extends RobolectricFragmentTestCase<AboutNewSoftKeyboardFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.aboutNewSoftKeyboardFragment;
  }

  @Test
  public void testWebSiteClick() {
    AboutNewSoftKeyboardFragment fragment = startFragment();
    TextView link = fragment.getView().findViewById(R.id.about_web_site_link);
    Assert.assertNotNull(link);

    Shadows.shadowOf(link).checkedPerformClick();

    Intent intent =
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity();

    Assert.assertNotNull(intent);
    Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
    Assert.assertEquals(
        ApplicationProvider.getApplicationContext().getString(R.string.main_site_url),
        intent.getData().toString());
  }

  @Test
  public void testShareApp() {
    AboutNewSoftKeyboardFragment fragment = startFragment();
    View icon = fragment.getView().findViewById(R.id.share_app_details);
    Assert.assertNotNull(icon);

    Shadows.shadowOf(icon).checkedPerformClick();

    Intent intent =
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity();

    Assert.assertNotNull(intent);
    Assert.assertEquals(Intent.ACTION_CHOOSER, intent.getAction());

    Intent sharingIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
    Assert.assertNotNull(sharingIntent);
  }

  @Test
  public void testRateApp() {
    AboutNewSoftKeyboardFragment fragment = startFragment();
    View icon = fragment.getView().findViewById(R.id.rate_app_in_store);
    Assert.assertNotNull(icon);

    Shadows.shadowOf(icon).checkedPerformClick();

    Intent intent =
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity();

    Assert.assertNotNull(intent);
    Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
    Assert.assertEquals(
        ApplicationProvider.getApplicationContext()
            .getString(R.string.rate_app_in_store_url, BuildConfig.APPLICATION_ID),
        intent.getData().toString());
  }

  @Test
  public void testPrivacyPolicyClick() {
    AboutNewSoftKeyboardFragment fragment = startFragment();
    TextView link = fragment.getView().findViewById(R.id.about_privacy_link);
    Assert.assertNotNull(link);

    link.performClick();

    Intent intent =
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity();

    Assert.assertNotNull(intent);
    Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
    Assert.assertEquals(
        ApplicationProvider.getApplicationContext().getString(R.string.privacy_policy),
        intent.getData().toString());
  }

  @Test
  public void testAdditionalLicenses() {
    AboutNewSoftKeyboardFragment fragment = startFragment();
    TextView link = fragment.getView().findViewById(R.id.about_legal_stuff_link);
    Assert.assertNotNull(link);

    Shadows.shadowOf(link).getOnClickListener().onClick(link);

    ensureAllScheduledJobsAreDone();

    Fragment nextFragment = getCurrentFragment();

    Assert.assertNotNull(nextFragment);
    Assert.assertTrue(
        nextFragment instanceof AboutNewSoftKeyboardFragment.AdditionalSoftwareLicensesFragment);
  }

  @Test
  public void testVersionInfo() {
    AboutNewSoftKeyboardFragment fragment = startFragment();
    TextView copyright = fragment.getView().findViewById(R.id.about_copyright);
    int currentYear = new GregorianCalendar().get(Calendar.YEAR);
    String expectedCopyright =
        ApplicationProvider.getApplicationContext()
            .getString(R.string.about_copyright_text, currentYear);
    Assert.assertEquals(expectedCopyright, copyright.getText().toString());

    TextView version = fragment.getView().findViewById(R.id.about_app_version);
    Assert.assertTrue(version.getText().toString().contains(BuildConfig.VERSION_NAME));
    Assert.assertTrue(
        version.getText().toString().contains(Integer.toString(BuildConfig.VERSION_CODE)));
  }
}
