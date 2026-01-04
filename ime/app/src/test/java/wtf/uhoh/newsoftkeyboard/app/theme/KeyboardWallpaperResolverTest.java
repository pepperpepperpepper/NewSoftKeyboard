package wtf.uhoh.newsoftkeyboard.app.theme;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardWallpaperResolverTest {

  @Test
  public void testApplyPhotoOverrideAsyncSetsViewBackground() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-photo";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.RED);
    store.setDimPercent(themeId, 50);

    final CountDownLatch latch = new CountDownLatch(1);
    final View view =
        new View(context) {
          @Override
          public void setBackground(Drawable background) {
            super.setBackground(background);
            latch.countDown();
          }
        };
    view.layout(0, 0, 480, 320);

    resolver.applyPhotoOverrideIfAnyAsync(view, theme);
    assertTrue(awaitLatchAndIdleMainLooper(latch, 2, TimeUnit.SECONDS));
    assertTrue(view.getBackground() instanceof LayerDrawable);
  }

  @Test
  public void testInvalidFlagDisablesPhotoOverrideApply() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-invalid-flag";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.BLUE);
    store.setDimPercent(themeId, 50);

    store.markWallpaperInvalid(themeId);

    final CountDownLatch latch = new CountDownLatch(1);
    final View view =
        new View(context) {
          @Override
          public void setBackground(Drawable background) {
            super.setBackground(background);
            latch.countDown();
          }
        };
    view.layout(0, 0, 480, 320);

    resolver.applyPhotoOverrideIfAnyAsync(view, theme);
    assertFalse(awaitLatchAndIdleMainLooper(latch, 250, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testDecodeFailureMarksInvalidAndDeletesFile() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver =
        new KeyboardWallpaperResolver(
            context,
            new WallpaperBitmapLoader() {
              @Override
              public Bitmap getCached(File file, long lastModified, int requestedMaxDimPx) {
                return null;
              }

              @Override
              public void loadAsync(
                  File file,
                  long lastModified,
                  int requestedMaxDimPx,
                  WallpaperBitmapRepository.Callback cb) {
                cb.onBitmapReady(null);
              }
            });

    final String themeId = "test-theme-decode-failure";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.YELLOW);
    assertTrue(store.hasWallpaper(themeId));

    final View view = new View(context);
    view.layout(0, 0, 480, 320);
    resolver.applyPhotoOverrideIfAnyAsync(view, theme);

    assertTrue(waitForCondition(() -> store.isWallpaperInvalid(themeId), 2, TimeUnit.SECONDS));
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
    final CountDownLatch invalidateLatch = new CountDownLatch(1);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);

    assertEquals(KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY, overlay.mode());
    assertEquals(null, overlay.paint());
    assertTrue(awaitLatchAndIdleMainLooper(invalidateLatch, 2, TimeUnit.SECONDS));

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay2 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT, overlay2.mode());
    assertNotNull(overlay2.paint());
    assertFalse(overlay2.matchKeyShape());

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay3 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertSame(overlay2, overlay3);
    assertSame(overlay2.paint(), overlay3.paint());
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
    final CountDownLatch invalidateLatch = new CountDownLatch(1);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);

    assertEquals(KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY, overlay.mode());
    assertEquals(null, overlay.paint());
    assertTrue(awaitLatchAndIdleMainLooper(invalidateLatch, 2, TimeUnit.SECONDS));

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay2 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE, overlay2.mode());
    assertNotNull(overlay2.paint());
    assertTrue(overlay2.matchKeyShape());
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
    final CountDownLatch invalidateLatch = new CountDownLatch(1);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertEquals(KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY, overlay.mode());
    assertEquals(null, overlay.paint());
    assertTrue(awaitLatchAndIdleMainLooper(invalidateLatch, 2, TimeUnit.SECONDS));

    final KeyboardWallpaperResolver.KeyFaceOverlay overlayReady =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertNotNull(overlayReady.paint());
    assertNotNull(overlayReady.paint().getShader());

    final Matrix m1 = new Matrix();
    overlayReady.paint().getShader().getLocalMatrix(m1);
    final float[] v1 = new float[9];
    m1.getValues(v1);

    store.setWallpaperRotationDegrees(themeId, 90);

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay2 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertNotNull(overlay2.paint());
    assertNotNull(overlay2.paint().getShader());

    final Matrix m2 = new Matrix();
    overlay2.paint().getShader().getLocalMatrix(m2);
    final float[] v2 = new float[9];
    m2.getValues(v2);

    assertNotEquals(Arrays.toString(v1), Arrays.toString(v2));
  }

  @Test
  public void testKeyFaceOverlayShaderMatrixUpdatesWhenAnchorChanges() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeId = "test-theme-key-overlay-anchor";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    writeSmallBitmap(store.getWallpaperFile(themeId), Color.CYAN);
    store.setWallpaperMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    store.setKeyAlphaPercent(themeId, 25);

    final Rect viewBounds = new Rect(0, 0, 480, 320);
    final CountDownLatch invalidateLatch = new CountDownLatch(1);
    final KeyboardWallpaperResolver.KeyFaceOverlay overlay =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertEquals(KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY, overlay.mode());
    assertEquals(null, overlay.paint());
    assertTrue(awaitLatchAndIdleMainLooper(invalidateLatch, 2, TimeUnit.SECONDS));

    final KeyboardWallpaperResolver.KeyFaceOverlay overlayReady =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertNotNull(overlayReady.paint());
    assertNotNull(overlayReady.paint().getShader());

    final Matrix m1 = new Matrix();
    overlayReady.paint().getShader().getLocalMatrix(m1);
    final float[] v1 = new float[9];
    m1.getValues(v1);

    store.setWallpaperAnchor(themeId, KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_TOP_LEFT);

    final KeyboardWallpaperResolver.KeyFaceOverlay overlay2 =
        resolver.resolveKeyFaceOverlay(theme, viewBounds, invalidateLatch::countDown);
    assertNotNull(overlay2.paint());
    assertNotNull(overlay2.paint().getShader());

    final Matrix m2 = new Matrix();
    overlay2.paint().getShader().getLocalMatrix(m2);
    final float[] v2 = new float[9];
    m2.getValues(v2);

    assertNotEquals(Arrays.toString(v1), Arrays.toString(v2));
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

  private static boolean waitForCondition(Condition condition, long timeout, TimeUnit unit)
      throws InterruptedException {
    final long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (condition.evaluate()) return true;
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      Thread.sleep(10);
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    return condition.evaluate();
  }

  interface Condition {
    boolean evaluate();
  }

  private static boolean awaitLatchAndIdleMainLooper(
      CountDownLatch latch, long timeout, TimeUnit unit) throws InterruptedException {
    final long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (latch.getCount() == 0) return true;
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      if (latch.await(10, TimeUnit.MILLISECONDS)) return true;
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    return latch.getCount() == 0;
  }
}
