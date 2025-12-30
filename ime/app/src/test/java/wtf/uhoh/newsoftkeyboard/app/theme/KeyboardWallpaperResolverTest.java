package wtf.uhoh.newsoftkeyboard.app.theme;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardWallpaperResolverTest {

  @Test
  public void testUsesPhotoOverrideAndCachesDrawable() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-photo";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.RED);
    store.setDimPercent(themeId, 50);

    final Drawable first = resolver.resolveImeWallpaper(theme);
    assertTrue(first instanceof LayerDrawable);

    final Drawable second = resolver.resolveImeWallpaper(theme);
    assertSame(first, second);
  }

  @Test
  public void testInvalidFlagDisablesPhotoOverride() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-invalid-flag";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.BLUE);
    store.setDimPercent(themeId, 50);

    final Drawable beforeInvalid = resolver.resolveImeWallpaper(theme);
    assertTrue(beforeInvalid instanceof LayerDrawable);

    store.markWallpaperInvalid(themeId);

    final Drawable afterInvalid = resolver.resolveImeWallpaper(theme);
    assertFalse(afterInvalid instanceof LayerDrawable);
  }

  @Test
  public void testDecodeFailureMarksInvalidAndDeletesFile() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver =
        new KeyboardWallpaperResolver(context) {
          @Override
          protected Bitmap decodePhotoFile(File file) {
            return null;
          }
        };

    final String themeId = "test-theme-decode-failure";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    final File file = store.getWallpaperFile(themeId);
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(new byte[] {0, 1, 2, 3});
    }
    assertTrue(store.hasWallpaper(themeId));

    final Drawable resolved = resolver.resolveImeWallpaper(theme);
    assertFalse(resolved instanceof LayerDrawable);

    assertTrue(store.isWallpaperInvalid(themeId));
    assertFalse(store.hasWallpaper(themeId));
  }

  @Test
  public void testResolvesKeyFaceOverlayWhenEnabled() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-key-overlay";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.RED);
    store.setWallpaperMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    store.setKeyAlphaPercent(themeId, 25);
    store.setDimPercent(themeId, 10);

    final Rect viewBounds = new Rect(0, 0, 480, 320);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds);

    assertEquals(KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT, overlay.mode());
    assertNotNull(overlay.paint());
    assertFalse(overlay.matchKeyShape());

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay2 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds);
    assertSame(overlay, overlay2);
    assertSame(overlay.paint(), overlay2.paint());
  }

  @Test
  public void testKeyFaceOverlayReportsMatchKeyShapeWhenEnabledAndTextureMode() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-key-overlay-mask";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.MAGENTA);
    store.setWallpaperMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE);
    store.setKeyAlphaPercent(themeId, 25);
    store.setMatchKeyShapeEnabled(themeId, true);

    final Rect viewBounds = new Rect(0, 0, 480, 320);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds);

    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE, overlay.mode());
    assertNotNull(overlay.paint());
    assertTrue(overlay.matchKeyShape());
  }

  @Test
  public void testKeyFaceOverlayShaderMatrixUpdatesWhenRotationChanges() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-key-overlay-rotate";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.GREEN);
    store.setWallpaperMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    store.setKeyAlphaPercent(themeId, 25);

    final Rect viewBounds = new Rect(0, 0, 480, 320);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds);
    assertNotNull(overlay.paint());
    assertNotNull(overlay.paint().getShader());

    final Matrix m1 = new Matrix();
    overlay.paint().getShader().getLocalMatrix(m1);
    final float[] v1 = new float[9];
    m1.getValues(v1);

    store.setWallpaperRotationDegrees(themeId, 90);

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay2 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds);
    assertNotNull(overlay2.paint());
    assertNotNull(overlay2.paint().getShader());

    final Matrix m2 = new Matrix();
    overlay2.paint().getShader().getLocalMatrix(m2);
    final float[] v2 = new float[9];
    m2.getValues(v2);

    assertFalse(Arrays.equals(v1, v2));
  }

  private static KeyboardTheme createLocalTheme(Context context, String themeId) {
    return new KeyboardTheme(
        context,
        context,
        1 /*apiVersion*/,
        themeId,
        "Test Theme",
        R.style.NskKeyboardBaseTheme,
        0,
        0,
        0,
        0,
        false,
        "",
        0);
  }

  private static void writeSmallBitmap(File file, int color) throws Exception {
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(color);
    try (FileOutputStream out = new FileOutputStream(file)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }
}
