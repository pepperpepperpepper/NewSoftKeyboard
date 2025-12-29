package wtf.uhoh.newsoftkeyboard.app.theme;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import java.io.File;
import java.io.FileOutputStream;
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
