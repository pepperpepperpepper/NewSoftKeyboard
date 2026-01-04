package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

@RunWith(AndroidJUnit4.class)
public class BackgroundPhotoLivePreviewSmokeTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void livePreviewUpdatesDimAndKeyOpacity() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    try {
      store.setWallpaperMode(
          themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
      store.setDimPercent(themeId, 40);
      store.setKeyAlphaPercent(themeId, 40);
      final Uri source =
          Uri.parse(
              ContentResolver.SCHEME_ANDROID_RESOURCE
                  + "://"
                  + context.getPackageName()
                  + "/"
                  + R.drawable.nsk_wallpaper);
      store.importFromUri(themeId, source);

      final Intent intent =
          new Intent(context, MainSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      scenario = ActivityScenario.launch(intent);
      scenario.onActivity(
          activity -> {
            ThemeCustomizationSmokeTestUtils.ensureKeyboardEnabled(activity);
            Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.keyboardThemeCustomizationFragment);
          });

      ThemeCustomizationSmokeTestUtils.waitForDimOverlayAlpha(scenario, 102, 15_000L);

      scenario.onActivity(
          activity -> {
            final KeyboardThemeCustomizationFragment fragment =
                ThemeCustomizationSmokeTestUtils.findNavFragment(
                    activity, KeyboardThemeCustomizationFragment.class);
            Assert.assertNotNull(fragment);
            final Preference dimPref =
                ThemeCustomizationSmokeTestUtils.findPreferenceByTitle(
                    fragment.getPreferenceScreen(),
                    activity.getString(R.string.keyboard_theme_wallpaper_customization_dim_title));
            Assert.assertTrue(dimPref instanceof SeekBarPreference);
            Assert.assertTrue(dimPref.callChangeListener(60));
          });
      ThemeCustomizationSmokeTestUtils.waitForDimOverlayAlpha(
          scenario, Math.round(255f * 0.60f), 15_000L);

      scenario.onActivity(
          activity -> {
            final KeyboardThemeCustomizationFragment fragment =
                ThemeCustomizationSmokeTestUtils.findNavFragment(
                    activity, KeyboardThemeCustomizationFragment.class);
            Assert.assertNotNull(fragment);
            final Preference keyOpacityPref =
                ThemeCustomizationSmokeTestUtils.findPreferenceByTitle(
                    fragment.getPreferenceScreen(),
                    activity.getString(
                        R.string.keyboard_theme_wallpaper_customization_key_opacity_title));
            Assert.assertTrue(keyOpacityPref instanceof SeekBarPreference);
            Assert.assertTrue(keyOpacityPref.callChangeListener(80));
          });
      Assert.assertEquals(80, store.getKeyAlphaPercent(themeId));
    } finally {
      store.clear(themeId);
    }
  }
}
