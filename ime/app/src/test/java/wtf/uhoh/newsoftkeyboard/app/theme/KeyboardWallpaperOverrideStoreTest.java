package wtf.uhoh.newsoftkeyboard.app.theme;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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
  public void testGradientClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-gradient";

    store.setGradientPercent(themeId, -1);
    assertEquals(0, store.getGradientPercent(themeId));

    store.setGradientPercent(themeId, 101);
    assertEquals(100, store.getGradientPercent(themeId));
  }

  @Test
  public void testGrainClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-grain";

    store.setGrainPercent(themeId, -1);
    assertEquals(0, store.getGrainPercent(themeId));

    store.setGrainPercent(themeId, 101);
    assertEquals(100, store.getGrainPercent(themeId));
  }

  @Test
  public void testSaturationClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-saturation";

    store.setSaturationPercent(themeId, -1);
    assertEquals(0, store.getSaturationPercent(themeId));

    store.setSaturationPercent(themeId, 201);
    assertEquals(200, store.getSaturationPercent(themeId));
  }

  @Test
  public void testContrastClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-contrast";

    store.setContrastPercent(themeId, -1);
    assertEquals(0, store.getContrastPercent(themeId));

    store.setContrastPercent(themeId, 201);
    assertEquals(200, store.getContrastPercent(themeId));
  }

  @Test
  public void testBrightnessClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-brightness";

    store.setBrightnessPercent(themeId, -1);
    assertEquals(0, store.getBrightnessPercent(themeId));

    store.setBrightnessPercent(themeId, 201);
    assertEquals(200, store.getBrightnessPercent(themeId));
  }

  @Test
  public void testTemperatureClampsToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-temperature";

    store.setTemperaturePercent(themeId, -1);
    assertEquals(0, store.getTemperaturePercent(themeId));

    store.setTemperaturePercent(themeId, 201);
    assertEquals(200, store.getTemperaturePercent(themeId));
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
  public void testKeyBlendModeNormalizesToValidRange() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-blend-mode";

    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL, store.getKeyBlendMode(themeId));

    store.setKeyBlendMode(themeId, 999);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL, store.getKeyBlendMode(themeId));

    store.setKeyBlendMode(themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        store.getKeyBlendMode(themeId));
  }

  @Test
  public void testLayerOrderCanBeASubsetOrEmpty() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-layer-order";

    final int[] keySubset =
        new int[] {
          KeyboardWallpaperOverrideStore.KEY_LAYER_HIGHLIGHT,
          KeyboardWallpaperOverrideStore.KEY_LAYER_GRAIN
        };
    store.setKeyLayerOrder(themeId, keySubset);
    assertArrayEquals(keySubset, store.getKeyLayerOrder(themeId));

    final int[] bgSubset =
        new int[] {
          KeyboardWallpaperOverrideStore.BACKGROUND_LAYER_DIM,
          KeyboardWallpaperOverrideStore.BACKGROUND_LAYER_VIGNETTE
        };
    store.setBackgroundLayerOrder(themeId, bgSubset);
    assertArrayEquals(bgSubset, store.getBackgroundLayerOrder(themeId));

    store.setKeyLayerOrder(themeId, new int[0]);
    assertEquals(0, store.getKeyLayerOrder(themeId).length);
  }

  @Test
  public void testWallpaperModeDefaultsToVisibleOptionWhenWallpaperExists() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final String themeId = "test-theme-mode-default-visible";

    store.clear(themeId);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY,
        store.getWallpaperMode(themeId));

    final File file = store.getWallpaperFile(themeId);
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(new byte[] {0, 1, 2});
    }
    assertTrue(store.hasWallpaper(themeId));

    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE,
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
    store.setSpecialKeyAlphaPercent(themeId, 34);
    store.setSpacebarAlphaPercent(themeId, 56);
    store.setModifierKeyAlphaPercent(themeId, 78);
    store.setEnterKeyAlphaPercent(themeId, 90);
    store.setKeyBlendMode(themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    store.setKeyGradientBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setKeyVignetteBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    store.setKeyGrainBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY);
    store.setBackgroundTintBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundDimBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    store.setBackgroundGradientBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY);
    store.setBackgroundVignetteBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT);
    store.setBackgroundGrainBlendMode(
        themeId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setWallpaperRotationDegrees(themeId, 90);
    store.setMatchKeyShapeEnabled(themeId, true);
    store.setWallpaperQuality(themeId, KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_HIGH);
    store.setVignettePercent(themeId, 17);
    store.setGradientPercent(themeId, 23);
    store.setGrainPercent(themeId, 44);
    store.setSaturationPercent(themeId, 150);
    store.setContrastPercent(themeId, 160);
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
    assertFalse(store.hasSpecialKeyAlphaPercentOverride(themeId));
    assertFalse(store.hasSpacebarAlphaPercentOverride(themeId));
    assertFalse(store.hasModifierKeyAlphaPercentOverride(themeId));
    assertFalse(store.hasEnterKeyAlphaPercentOverride(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.DEFAULT_KEY_ALPHA_PERCENT,
        store.getSpecialKeyAlphaPercent(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.DEFAULT_KEY_ALPHA_PERCENT,
        store.getSpacebarAlphaPercent(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.DEFAULT_KEY_ALPHA_PERCENT,
        store.getModifierKeyAlphaPercent(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.DEFAULT_KEY_ALPHA_PERCENT,
        store.getEnterKeyAlphaPercent(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL, store.getKeyBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getKeyGradientBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getKeyVignetteBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getKeyGrainBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getBackgroundTintBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getBackgroundDimBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getBackgroundGradientBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getBackgroundVignetteBlendMode(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        store.getBackgroundGrainBlendMode(themeId));
    assertEquals(0, store.getWallpaperRotationDegrees(themeId));
    assertFalse(store.isMatchKeyShapeEnabled(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_BALANCED,
        store.getWallpaperQuality(themeId));
    assertEquals(0, store.getVignettePercent(themeId));
    assertEquals(0, store.getGradientPercent(themeId));
    assertEquals(0, store.getGrainPercent(themeId));
    assertEquals(100, store.getSaturationPercent(themeId));
    assertEquals(100, store.getContrastPercent(themeId));
    assertEquals(100, store.getBrightnessPercent(themeId));
    assertEquals(100, store.getTemperaturePercent(themeId));
    assertFalse(store.isWallpaperInvalid(themeId));
    assertEquals(tokenBefore + 1, store.getWallpaperChangeToken(themeId));
  }

  @Test
  public void testModifierAndEnterAlphaInheritFromSpecialKeysByDefault() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-alpha-inherit-special";

    store.clear(themeId);
    store.setKeyAlphaPercent(themeId, 11);
    store.setSpecialKeyAlphaPercent(themeId, 22);

    assertFalse(store.hasModifierKeyAlphaPercentOverride(themeId));
    assertFalse(store.hasEnterKeyAlphaPercentOverride(themeId));

    assertEquals(22, store.getModifierKeyAlphaPercent(themeId));
    assertEquals(22, store.getEnterKeyAlphaPercent(themeId));
    assertEquals(11, store.getSpacebarAlphaPercent(themeId));

    store.setModifierKeyAlphaPercent(themeId, 33);
    assertTrue(store.hasModifierKeyAlphaPercentOverride(themeId));
    assertEquals(33, store.getModifierKeyAlphaPercent(themeId));
    store.clearModifierKeyAlphaPercent(themeId);
    assertFalse(store.hasModifierKeyAlphaPercentOverride(themeId));
    assertEquals(22, store.getModifierKeyAlphaPercent(themeId));

    store.setEnterKeyAlphaPercent(themeId, 44);
    assertTrue(store.hasEnterKeyAlphaPercentOverride(themeId));
    assertEquals(44, store.getEnterKeyAlphaPercent(themeId));
    store.clearEnterKeyAlphaPercent(themeId);
    assertFalse(store.hasEnterKeyAlphaPercentOverride(themeId));
    assertEquals(22, store.getEnterKeyAlphaPercent(themeId));
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
    store.setSpecialKeyAlphaPercent(sourceId, 22);
    store.setSpacebarAlphaPercent(sourceId, 44);
    store.setModifierKeyAlphaPercent(sourceId, 55);
    store.setEnterKeyAlphaPercent(sourceId, 66);
    store.setKeyBlendMode(sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setKeyGradientBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY);
    store.setKeyVignetteBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    store.setKeyGrainBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundTintBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    store.setBackgroundDimBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundGradientBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY);
    store.setBackgroundVignetteBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT);
    store.setBackgroundGrainBlendMode(
        sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setWallpaperRotationDegrees(sourceId, 270);
    store.setMatchKeyShapeEnabled(sourceId, true);
    store.setWallpaperQuality(sourceId, KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_HIGH);
    store.setVignettePercent(sourceId, 88);
    store.setGradientPercent(sourceId, 77);
    store.setGrainPercent(sourceId, 31);
    store.setSaturationPercent(sourceId, 120);
    store.setContrastPercent(sourceId, 115);
    store.setBrightnessPercent(sourceId, 105);
    store.setTemperaturePercent(sourceId, 140);
    assertTrue(store.hasWallpaper(sourceId));

    store.setDimPercent(targetId, 7);
    store.setWallpaperMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    store.setKeyAlphaPercent(targetId, 1);
    store.setSpecialKeyAlphaPercent(targetId, 1);
    store.setSpacebarAlphaPercent(targetId, 1);
    store.setModifierKeyAlphaPercent(targetId, 1);
    store.setEnterKeyAlphaPercent(targetId, 1);
    store.setKeyBlendMode(targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    store.setKeyGradientBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setKeyVignetteBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setKeyGrainBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundTintBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundDimBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundGradientBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundVignetteBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setBackgroundGrainBlendMode(
        targetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    store.setWallpaperQuality(targetId, KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_LOW);
    store.setVignettePercent(targetId, 3);
    store.setGradientPercent(targetId, 2);
    store.setGrainPercent(targetId, 1);
    store.setSaturationPercent(targetId, 90);
    store.setContrastPercent(targetId, 70);
    store.setBrightnessPercent(targetId, 90);
    store.setTemperaturePercent(targetId, 80);
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
    assertEquals(22, store.getSpecialKeyAlphaPercent(targetId));
    assertEquals(44, store.getSpacebarAlphaPercent(targetId));
    assertEquals(55, store.getModifierKeyAlphaPercent(targetId));
    assertEquals(66, store.getEnterKeyAlphaPercent(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        store.getKeyBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
        store.getKeyGradientBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
        store.getKeyVignetteBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        store.getKeyGrainBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
        store.getBackgroundTintBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        store.getBackgroundDimBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
        store.getBackgroundGradientBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
        store.getBackgroundVignetteBlendMode(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        store.getBackgroundGrainBlendMode(targetId));
    assertEquals(270, store.getWallpaperRotationDegrees(targetId));
    assertTrue(store.isMatchKeyShapeEnabled(targetId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_HIGH, store.getWallpaperQuality(targetId));
    assertEquals(88, store.getVignettePercent(targetId));
    assertEquals(77, store.getGradientPercent(targetId));
    assertEquals(31, store.getGrainPercent(targetId));
    assertEquals(120, store.getSaturationPercent(targetId));
    assertEquals(115, store.getContrastPercent(targetId));
    assertEquals(105, store.getBrightnessPercent(targetId));
    assertEquals(140, store.getTemperaturePercent(targetId));
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

  @Test
  public void testQualityDefaultsAndDisablesMatchKeyShapeWhenLowered() {
    final KeyboardWallpaperOverrideStore store =
        new KeyboardWallpaperOverrideStore(getApplicationContext());
    final String themeId = "test-theme-quality";

    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_BALANCED,
        store.getWallpaperQuality(themeId));

    store.setMatchKeyShapeEnabled(themeId, true);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_HIGH, store.getWallpaperQuality(themeId));

    store.setWallpaperQuality(themeId, KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_LOW);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_LOW, store.getWallpaperQuality(themeId));
    assertFalse(store.isMatchKeyShapeEnabled(themeId));
  }

  @Test
  public void testImportFromUriSetsVisibleDefaultMode() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final String themeId = "test-theme-import-default-mode";

    store.clear(themeId);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY,
        store.getWallpaperMode(themeId));

    final File sourceFile = new File(context.getCacheDir(), "nsk_wallpaper_source.png");
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.BLUE);
    try (FileOutputStream out = new FileOutputStream(sourceFile)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }

    store.importFromUri(themeId, Uri.fromFile(sourceFile), 64, 64);

    assertTrue(store.hasWallpaper(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT,
        store.getWallpaperMode(themeId));
    assertEquals(60, store.getKeyAlphaPercent(themeId));
  }

  @Test
  public void testImportFromUriMigratesOldBackgroundOnlyModeToVisibleDefault() throws Exception {
    final Context context = getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final String themeId = "test-theme-import-migrate-background-only";

    store.clear(themeId);
    store.setWallpaperMode(themeId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY);
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY,
        store.getWallpaperMode(themeId));
    assertFalse(store.hasWallpaper(themeId));

    final File sourceFile = new File(context.getCacheDir(), "nsk_wallpaper_source_migrate.png");
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.RED);
    try (FileOutputStream out = new FileOutputStream(sourceFile)) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }

    store.importFromUri(themeId, Uri.fromFile(sourceFile), 64, 64);

    assertTrue(store.hasWallpaper(themeId));
    assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT,
        store.getWallpaperMode(themeId));
    assertEquals(60, store.getKeyAlphaPercent(themeId));
  }
}
