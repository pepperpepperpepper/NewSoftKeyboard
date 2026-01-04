package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoMatchingViewException;
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
    final Intent intent =
        new Intent(targetContext, MainSettingsActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    scenario = ActivityScenario.launch(intent);
    scenario.onActivity(
        activity -> {
          ThemeCustomizationSmokeTestUtils.ensureKeyboardEnabled(activity);
          Navigation.findNavController(activity, R.id.nav_host_fragment)
              .navigate(R.id.keyboardThemeSelectorFragment);
        });

    // Row navigation
    onView(withId(R.id.keyboard_theme_customize_row)).check(matches(isDisplayed()));
    onView(withId(R.id.keyboard_theme_customize_row)).perform(click());
    onView(withText(R.string.keyboard_theme_wallpaper_customization_pick_title))
        .check(matches(isDisplayed()));
    pressBack();

    // Menu navigation
    try {
      onView(withId(R.id.tweaks_menu_option)).perform(click());
    } catch (NoMatchingViewException ignored) {
      openActionBarOverflowOrOptionsMenu(targetContext);
      onView(withText(R.string.keyboard_theme_customize_menu_title)).perform(click());
    }
    onView(withText(R.string.keyboard_theme_wallpaper_customization_pick_title))
        .check(matches(isDisplayed()));
  }
}
