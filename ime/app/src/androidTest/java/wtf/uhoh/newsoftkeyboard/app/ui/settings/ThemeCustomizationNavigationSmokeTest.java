package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;

@RunWith(AndroidJUnit4.class)
public class ThemeCustomizationNavigationSmokeTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void customizeFromThemeSelectorOpensBackgroundPhotoEditor() {
    final var targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ThemeCustomizationSmokeTestUtils.ensureDefaultThemeEnabled(targetContext);
    final Intent intent =
        new Intent(targetContext, MainSettingsActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(targetContext.getString(R.string.deeplink_url_themes)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    scenario = ActivityScenario.launch(intent);
    scenario.onActivity(
        activity -> {
          ThemeCustomizationSmokeTestUtils.ensureKeyboardEnabled(activity);
        });

    // Row navigation
    onView(withId(R.id.keyboard_theme_customize_row)).check(matches(isDisplayed()));
    scenario.onActivity(
        activity -> {
          final View row = activity.findViewById(R.id.keyboard_theme_customize_row);
          if (row == null) throw new AssertionError("Customize row not found.");
          row.performClick();
        });
    onView(withId(R.id.wallpaper_live_preview_keyboard)).check(matches(isDisplayed()));
    scenario.onActivity(
        activity -> {
          activity.getOnBackPressedDispatcher().onBackPressed();
        });
    onView(withId(R.id.keyboard_theme_customize_row)).check(matches(isDisplayed()));

    // Menu navigation
    scenario.onActivity(
        activity -> {
          final KeyboardThemeSelectorFragment selector =
              ThemeCustomizationSmokeTestUtils.findNavFragment(
                  activity, KeyboardThemeSelectorFragment.class);
          if (selector == null) throw new AssertionError("Theme selector fragment not found.");
          selector.onTweaksOptionSelected();
        });
    onView(withId(R.id.wallpaper_live_preview_keyboard)).check(matches(isDisplayed()));
  }
}
