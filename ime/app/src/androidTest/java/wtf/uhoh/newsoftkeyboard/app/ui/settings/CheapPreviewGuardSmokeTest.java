package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;

@RunWith(AndroidJUnit4.class)
public class CheapPreviewGuardSmokeTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void manageKeyboardsListPreviewsDisableExpensiveWallpaperEffects() {
    final var targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ThemeCustomizationSmokeTestUtils.ensureDefaultThemeEnabled(targetContext);
    final Intent intent =
        new Intent(targetContext, MainSettingsActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(targetContext.getString(R.string.deeplink_url_keyboards)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    scenario = ActivityScenario.launch(intent);
    scenario.onActivity(
        activity -> {
          ThemeCustomizationSmokeTestUtils.ensureKeyboardEnabled(activity);
        });
    onView(withId(R.id.recycler_view)).check(matches(isDisplayed()));

    // Wait for the list to render at least one item.
    final long deadline = SystemClock.uptimeMillis() + 10_000L;
    while (SystemClock.uptimeMillis() < deadline) {
      final int[] childCount = new int[] {0};
      scenario.onActivity(
          activity -> {
            final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
            childCount[0] = recyclerView != null ? recyclerView.getChildCount() : 0;
          });
      if (childCount[0] > 0) break;
      SystemClock.sleep(200L);
    }

    scenario.onActivity(
        activity -> {
          final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
          Assert.assertNotNull(recyclerView);

          int checked = 0;
          for (int i = 0; i < recyclerView.getChildCount(); i++) {
            final View child = recyclerView.getChildAt(i);
            final DemoKeyboardView preview = child.findViewById(R.id.item_keyboard_view);
            if (preview == null) continue;
            checked++;
            Assert.assertFalse(
                ThemeCustomizationSmokeTestUtils.getAllowExpensiveWallpaperEffects(preview));
          }
          Assert.assertTrue("Expected at least one keyboard preview item.", checked > 0);
        });
  }
}
