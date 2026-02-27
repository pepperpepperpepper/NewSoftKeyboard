package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
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
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
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
    final String themeId = ThemeCustomizationSmokeTestUtils.ensureDefaultThemeEnabled(context);

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
          new Intent(context, MainSettingsActivity.class)
              .setAction(Intent.ACTION_VIEW)
              .setData(Uri.parse(context.getString(R.string.deeplink_url_themes)))
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      scenario = ActivityScenario.launch(intent);
      scenario.onActivity(
          activity -> {
            ThemeCustomizationSmokeTestUtils.ensureKeyboardEnabled(activity);
          });

      scenario.onActivity(
          activity -> {
            final View row = activity.findViewById(R.id.keyboard_theme_customize_row);
            if (row == null) throw new AssertionError("Customize row not found.");
            row.performClick();
          });
      ThemeCustomizationSmokeTestUtils.waitForLivePreviewBackgroundDrawable(scenario, 15_000L);
      final int baselineLuma =
          ThemeCustomizationSmokeTestUtils.captureLivePreviewBackgroundLuma(scenario);

      scenario.onActivity(
          activity -> {
            final KeyboardWallpaperLayer[] stack = store.getBackgroundLayerStack(themeId).clone();
            boolean updated = false;
            for (int i = 0; i < stack.length; i++) {
              final KeyboardWallpaperLayer layer = stack[i];
              if (layer == null) continue;
              if (layer.type() != KeyboardWallpaperLayer.TYPE_DIM) continue;
              stack[i] =
                  new KeyboardWallpaperLayer(
                      layer.type(),
                      true /*enabled*/,
                      60 /*opacityPercent*/,
                      layer.blendMode(),
                      layer.argb(),
                      layer.argb2(),
                      layer.direction(),
                      layer.scalePercent(),
                      layer.gradientStops());
              updated = true;
              break;
            }
            Assert.assertTrue("Dim layer not found in background layer stack", updated);
            store.setBackgroundLayerStack(themeId, stack);
          });
      ThemeCustomizationSmokeTestUtils.waitForLivePreviewBackgroundLumaAtMost(
          scenario, Math.max(0, baselineLuma - 5), 15_000L);

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
