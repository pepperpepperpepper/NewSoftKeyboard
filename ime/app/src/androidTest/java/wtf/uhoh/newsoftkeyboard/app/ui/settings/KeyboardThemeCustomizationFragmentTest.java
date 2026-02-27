package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

@RunWith(AndroidJUnit4.class)
public class KeyboardThemeCustomizationFragmentTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void pickingWallpaperDoesNotCrashAndMarksPhotoAsSet() throws Exception {
    final var targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final AtomicReference<String> themeIdRef = new AtomicReference<>(null);

    final File sourceFile = new File(targetContext.getCacheDir(), "nsk_wallpaper_pick_test.png");
    final Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.MAGENTA);
    try (FileOutputStream out = new FileOutputStream(sourceFile)) {
      //noinspection ResultOfMethodCallIgnored
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      bitmap.recycle();
    }
    final Uri uri = Uri.fromFile(sourceFile);

    Intents.init();
    try {
      final Intent resultData = new Intent().setData(uri);
      final Instrumentation.ActivityResult result =
          new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
      intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(result);

      final Intent intent =
          new Intent(targetContext, MainSettingsActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      scenario = ActivityScenario.launch(intent);
      scenario.onActivity(
          activity -> {
            final var prefs = DirectBootAwareSharedPreferences.create(activity);
            prefs
                .edit()
                .putBoolean(
                    activity.getString(R.string.settings_key_extension_keyboard_enabled), true)
                .apply();

            final KeyboardTheme theme =
                NskApplicationBase.getKeyboardThemeFactory(activity).getEnabledAddOn();
            org.junit.Assert.assertNotNull(theme);
            themeIdRef.set(theme.getId());

            new KeyboardWallpaperOverrideStore(activity).clear(theme.getId());

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.keyboardThemeCustomizationFragment);
          });

      onView(withId(androidx.preference.R.id.recycler_view))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(
                      withText(R.string.keyboard_theme_wallpaper_customization_pick_title)),
                  click()));

      waitForTextDisplayed(
          R.string.keyboard_theme_wallpaper_customization_pick_summary_set, 10_000L);

      scenario.onActivity(
          activity -> {
            final String themeId = themeIdRef.get();
            org.junit.Assert.assertNotNull(themeId);
            org.junit.Assert.assertTrue(
                new KeyboardWallpaperOverrideStore(activity).hasWallpaper(themeId));
            new KeyboardWallpaperOverrideStore(activity).clear(themeId);
          });
    } finally {
      Intents.release();
    }
  }

  @Test
  public void changingWallpaperModeDoesNotCrashAndPersistsPreference() throws Exception {
    final var targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final AtomicReference<String> themeIdRef = new AtomicReference<>(null);

    final File sourceFile = new File(targetContext.getCacheDir(), "nsk_wallpaper_mode_test.png");
    final Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.BLUE);
    try (FileOutputStream out = new FileOutputStream(sourceFile)) {
      //noinspection ResultOfMethodCallIgnored
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      bitmap.recycle();
    }
    final Uri uri = Uri.fromFile(sourceFile);

    Intents.init();
    try {
      final Intent resultData = new Intent().setData(uri);
      final Instrumentation.ActivityResult result =
          new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
      intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(result);

      final Intent intent =
          new Intent(targetContext, MainSettingsActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      scenario = ActivityScenario.launch(intent);
      scenario.onActivity(
          activity -> {
            final var prefs = DirectBootAwareSharedPreferences.create(activity);
            prefs
                .edit()
                .putBoolean(
                    activity.getString(R.string.settings_key_extension_keyboard_enabled), true)
                .apply();

            final KeyboardTheme theme =
                NskApplicationBase.getKeyboardThemeFactory(activity).getEnabledAddOn();
            org.junit.Assert.assertNotNull(theme);
            themeIdRef.set(theme.getId());

            new KeyboardWallpaperOverrideStore(activity).clear(theme.getId());

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.keyboardThemeCustomizationFragment);
          });

      onView(withId(androidx.preference.R.id.recycler_view))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(
                      withText(R.string.keyboard_theme_wallpaper_customization_pick_title)),
                  click()));

      waitForTextDisplayed(
          R.string.keyboard_theme_wallpaper_customization_pick_summary_set, 10_000L);

      // Change mode to "Background + key texture"
      onView(withId(androidx.preference.R.id.recycler_view))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(
                      withText(R.string.keyboard_theme_wallpaper_customization_mode_title)),
                  click()));
      onView(withText(R.string.keyboard_theme_wallpaper_customization_mode_background_key_texture))
          .perform(click());

      scenario.onActivity(
          activity -> {
            final String themeId = themeIdRef.get();
            org.junit.Assert.assertNotNull(themeId);
            final int mode = new KeyboardWallpaperOverrideStore(activity).getWallpaperMode(themeId);
            org.junit.Assert.assertEquals(
                KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE, mode);
            new KeyboardWallpaperOverrideStore(activity).clear(themeId);
          });
    } finally {
      Intents.release();
    }
  }

  private static void waitForTextDisplayed(int stringResId, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
    Throwable lastFailure = null;
    while (SystemClock.uptimeMillis() < deadline) {
      try {
        onView(withText(stringResId)).check(matches(isDisplayed()));
        return;
      } catch (Throwable t) {
        lastFailure = t;
        SystemClock.sleep(200L);
      }
    }
    if (lastFailure != null) {
      throw new AssertionError("Timed out waiting for text " + stringResId, lastFailure);
    }
    onView(withText(stringResId)).check(matches(isDisplayed()));
  }
}
