package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

@RunWith(AndroidJUnit4.class)
public class GenericRowsInstrumentedTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  private void navigateToLookAndFeelSettings() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
    final var targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final Intent intent =
        new Intent(targetContext, MainSettingsActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    scenario = ActivityScenario.launch(intent);
    scenario.onActivity(
        activity -> {
          final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(activity);
          prefs
              .edit()
              .putBoolean(
                  activity.getString(R.string.settings_key_extension_keyboard_enabled), true)
              .apply();
          androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
              .navigate(R.id.lookAndFeelSettingsFragment);
        });
  }

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void topRowListContainsDevTools() {
    navigateToLookAndFeelSettings();
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(hasDescendant(withText(R.string.top_generic_row_group))));
    onView(withText(R.string.top_generic_row_group)).perform(click());
    onView(withId(R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(hasDescendant(withText(R.string.extension_kbd_top_dev))));
    onView(withText(R.string.extension_kbd_top_dev)).check(matches(isDisplayed()));
  }

  @Test
  public void topRowListContainsDevToolsWithSwitcher() {
    navigateToLookAndFeelSettings();
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(hasDescendant(withText(R.string.top_generic_row_group))));
    onView(withText(R.string.top_generic_row_group)).perform(click());
    onView(withId(R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.extension_kbd_top_dev_switcher))));
    onView(withText(R.string.extension_kbd_top_dev_switcher)).check(matches(isDisplayed()));
  }

  @Test
  public void bottomRowListContainsNoBottomRow() {
    navigateToLookAndFeelSettings();
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.bottom_generic_row_group))));
    onView(withText(R.string.bottom_generic_row_group)).perform(click());
    onView(withId(R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.extension_kbd_bottom_none))));
    onView(withText(R.string.extension_kbd_bottom_none)).check(matches(isDisplayed()));
  }
}
