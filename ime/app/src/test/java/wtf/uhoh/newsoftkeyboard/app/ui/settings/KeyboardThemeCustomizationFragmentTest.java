package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

public class KeyboardThemeCustomizationFragmentTest
    extends RobolectricFragmentTestCase<KeyboardThemeCustomizationFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.keyboardThemeCustomizationFragment;
  }

  @Test
  public void testAnchorVisibilityTogglesWithScaleMode() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    writeSmallBitmap(store.getWallpaperFile(themeId), Color.RED);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final ListPreference scalePref =
        fragment.findPreference("keyboard_theme_wallpaper_customization_scale_mode");
    final ListPreference anchorPref =
        fragment.findPreference("keyboard_theme_wallpaper_customization_anchor");
    Assert.assertNotNull(scalePref);
    Assert.assertNotNull(anchorPref);

    Assert.assertTrue(scalePref.isVisible());
    Assert.assertTrue(anchorPref.isVisible());

    Assert.assertTrue(
        scalePref.callChangeListener(
            String.valueOf(KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_TILE)));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_TILE,
        store.getWallpaperScaleMode(themeId));
    Assert.assertFalse(anchorPref.isVisible());

    Assert.assertTrue(
        scalePref.callChangeListener(
            String.valueOf(KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_FIT)));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_FIT,
        store.getWallpaperScaleMode(themeId));
    Assert.assertTrue(anchorPref.isVisible());
  }

  @Test
  public void testAnchorPreferenceUpdatesStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    writeSmallBitmap(store.getWallpaperFile(themeId), Color.GREEN);

    final KeyboardThemeCustomizationFragment fragment = startFragment();
    final ListPreference anchorPref =
        fragment.findPreference("keyboard_theme_wallpaper_customization_anchor");
    Assert.assertNotNull(anchorPref);
    Assert.assertTrue(anchorPref.isVisible());

    Assert.assertTrue(
        anchorPref.callChangeListener(
            String.valueOf(KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_BOTTOM_RIGHT)));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_BOTTOM_RIGHT,
        store.getWallpaperAnchor(themeId));
  }

  private static void writeSmallBitmap(java.io.File file, int color) throws Exception {
    final java.io.File parent = file.getParentFile();
    Assert.assertTrue(parent.isDirectory() || parent.mkdirs());
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(color);
    try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
      Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }
}
