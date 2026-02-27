package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;

@RunWith(AndroidJUnit4.class)
public class NoMainThreadDecodeSmokeTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void noWallpaperDecodeRunsOnMainThread() throws Exception {
    KeyboardWallpaperResolver.resetMainThreadDecodeCounterForTests();

    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final String themeId = ThemeCustomizationSmokeTestUtils.ensureDefaultThemeEnabled(context);
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    try {
      store.setWallpaperMode(
          themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
      store.setDimPercent(themeId, 20);
      final Uri source =
          Uri.parse(
              ContentResolver.SCHEME_ANDROID_RESOURCE
                  + "://"
                  + context.getPackageName()
                  + "/"
                  + R.drawable.nsk_wallpaper);
      store.importFromUri(themeId, source);

      final Intent intent =
          new Intent(context, MainSettingsActivity.class)
              .setAction(Intent.ACTION_VIEW)
              .setData(Uri.parse(context.getString(R.string.deeplink_url_keyboards)))
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      scenario = ActivityScenario.launch(intent);
      scenario.onActivity(
          activity -> {
            ThemeCustomizationSmokeTestUtils.ensureKeyboardEnabled(activity);
          });
      onView(withId(R.id.recycler_view)).check(matches(isDisplayed()));

      // Trigger a bit of RecyclerView work; bounded to avoid flakiness if there are few items.
      final int[] scrollTarget = new int[] {0};
      scenario.onActivity(
          activity -> {
            final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
            final RecyclerView.Adapter<?> adapter =
                recyclerView != null ? recyclerView.getAdapter() : null;
            if (adapter == null) return;
            final int count = adapter.getItemCount();
            scrollTarget[0] = Math.max(0, Math.min(10, count - 1));
            recyclerView.scrollToPosition(scrollTarget[0]);
          });
      SystemClock.sleep(1_000L);

      Assert.assertEquals(0, KeyboardWallpaperResolver.getMainThreadDecodeCountForTests());
    } finally {
      store.clear(themeId);
    }
  }
}
