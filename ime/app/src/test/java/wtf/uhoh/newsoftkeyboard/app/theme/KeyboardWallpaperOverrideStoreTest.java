package wtf.uhoh.newsoftkeyboard.app.theme;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardWallpaperOverrideStoreTest {

  @Test
  public void testDimClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme";

    store.setDimPercent(themeId, -1);
    assertEquals(0, store.getDimPercent(themeId));

    store.setDimPercent(themeId, 101);
    assertEquals(100, store.getDimPercent(themeId));
  }

  @Test
  public void testKeyAlphaClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-alpha";

    store.setKeyAlphaPercent(themeId, -1);
    assertEquals(0, store.getKeyAlphaPercent(themeId));

    store.setKeyAlphaPercent(themeId, 101);
    assertEquals(100, store.getKeyAlphaPercent(themeId));
  }

  @Test
  public void testModeNormalizesToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-mode";

    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY,
        store.getWallpaperMode(themeId));

    store.setWallpaperMode(themeId, 999);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY,
        store.getWallpaperMode(themeId));

    store.setWallpaperMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT,
        store.getWallpaperMode(themeId));
  }

  @Test
  public void testClearDeletesFileAndSignalsChange() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final String themeId = "test-theme-clear";

    final File file = store.getWallpaperFile(themeId);
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(new byte[] {0, 1, 2});
    }
    assertTrue(store.hasWallpaper(themeId));

    store.setDimPercent(themeId, 55);
    assertEquals(55, store.getDimPercent(themeId));
    store.setWallpaperMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    store.setKeyAlphaPercent(themeId, 12);
    store.setWallpaperRotationDegrees(themeId, 90);
    store.setMatchKeyShapeEnabled(themeId, true);
    assertEquals(90, store.getWallpaperRotationDegrees(themeId));

    store.markWallpaperInvalid(themeId);
    assertTrue(store.isWallpaperInvalid(themeId));

    final int tokenBefore = store.getWallpaperChangeToken(themeId);

    store.clear(themeId);

    assertFalse(store.hasWallpaper(themeId));
    assertFalse(file.exists());
    assertEquals(0, store.getDimPercent(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY,
        store.getWallpaperMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.DEFAULT_KEY_ALPHA_PERCENT,
        store.getKeyAlphaPercent(themeId));
    assertEquals(0, store.getWallpaperRotationDegrees(themeId));
    assertFalse(store.isMatchKeyShapeEnabled(themeId));
    assertFalse(store.isWallpaperInvalid(themeId));
    assertEquals(tokenBefore + 1, store.getWallpaperChangeToken(themeId));
  }

  @Test
  public void testCopyToThemeCopiesFileAndPrefsAndSignalsChange() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final String sourceId = "test-theme-source";
    final String targetId = "test-theme-target";

    store.clear(sourceId);
    store.clear(targetId);

    final File sourceFile = store.getWallpaperFile(sourceId);
    assertTrue(sourceFile.getParentFile().isDirectory() || sourceFile.getParentFile().mkdirs());
    try (FileOutputStream out = new FileOutputStream(sourceFile)) {
      out.write(new byte[] {1, 2, 3, 4});
    }
    store.setDimPercent(sourceId, 42);
    store.setWallpaperMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE);
    store.setKeyAlphaPercent(sourceId, 33);
    store.setWallpaperRotationDegrees(sourceId, 270);
    store.setMatchKeyShapeEnabled(sourceId, true);
    assertTrue(store.hasWallpaper(sourceId));

    store.setDimPercent(targetId, 7);
    store.setWallpaperMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    store.setKeyAlphaPercent(targetId, 1);
    store.markWallpaperInvalid(targetId);
    assertTrue(store.isWallpaperInvalid(targetId));

    final int tokenBefore = store.getWallpaperChangeToken(targetId);

    store.copyToTheme(sourceId, targetId);

    assertTrue(store.hasWallpaper(targetId));
    assertFalse(store.isWallpaperInvalid(targetId));
    assertEquals(42, store.getDimPercent(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE,
        store.getWallpaperMode(targetId));
    assertEquals(33, store.getKeyAlphaPercent(targetId));
    assertEquals(270, store.getWallpaperRotationDegrees(targetId));
    assertTrue(store.isMatchKeyShapeEnabled(targetId));
    assertEquals(tokenBefore + 1, store.getWallpaperChangeToken(targetId));

    final File targetFile = store.getWallpaperFile(targetId);
    assertEquals(sourceFile.length(), targetFile.length());
  }

  @Test
  public void testMarkInvalidSetsFlagAndSignalsChange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-invalid";

    final int tokenBefore = store.getWallpaperChangeToken(themeId);
    store.markWallpaperInvalid(themeId);

    assertTrue(store.isWallpaperInvalid(themeId));
    assertEquals(tokenBefore + 1, store.getWallpaperChangeToken(themeId));
  }

  @Test
  public void testWallpaperFileNameIsStablePerThemeId() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());

    final File a1 = store.getWallpaperFile("theme-a");
    final File a2 = store.getWallpaperFile("theme-a");
    final File b = store.getWallpaperFile("theme-b");

    assertEquals(a1.getAbsolutePath(), a2.getAbsolutePath());
    assertNotEquals(a1.getAbsolutePath(), b.getAbsolutePath());
  }

  @Test
  public void testRotationNormalizesAndSignalsChange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-rotation";

    assertEquals(0, store.getWallpaperRotationDegrees(themeId));

    final int tokenBefore = store.getWallpaperChangeToken(themeId);
    store.setWallpaperRotationDegrees(themeId, 45);
    assertEquals(0, store.getWallpaperRotationDegrees(themeId));
    assertEquals(tokenBefore + 1, store.getWallpaperChangeToken(themeId));

    final int tokenBefore2 = store.getWallpaperChangeToken(themeId);
    store.rotateWallpaperClockwise90(themeId);
    assertEquals(90, store.getWallpaperRotationDegrees(themeId));
    assertEquals(tokenBefore2 + 1, store.getWallpaperChangeToken(themeId));
  }

  @Test
  public void testHighQualityImportPreferencePersists() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());

    store.setHighQualityImportEnabled(true);
    assertTrue(store.isHighQualityImportEnabled());

    store.setHighQualityImportEnabled(false);
    assertFalse(store.isHighQualityImportEnabled());
  }

  @Test
  public void testMatchKeyShapeSignalsChange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-shape-mask";

    final int tokenBefore = store.getWallpaperChangeToken(themeId);
    store.setMatchKeyShapeEnabled(themeId, true);
    assertTrue(store.isMatchKeyShapeEnabled(themeId));
    assertEquals(tokenBefore + 1, store.getWallpaperChangeToken(themeId));
  }
}
