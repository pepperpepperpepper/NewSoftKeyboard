package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardThemePresetTransferTest {

  // 1x1 transparent PNG.
  private static final byte[] ONE_BY_ONE_PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==");

  @Test
  public void testExportImportRoundTrip() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);
    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);
    final KeyboardWallpaperOverrideStore wallpaperStore =
        new KeyboardWallpaperOverrideStore(context);

    final String baseThemeId = "theme_test_transfer_round_trip";
    final KeyboardThemePresetStore.Preset created =
        presetStore.createPreset(baseThemeId, "My Look");
    final String presetId = created.id();

    overridesStore.setTokenPrimaryTextColor(presetId, 0xFF010101);
    overridesStore.setTokenSecondaryTextColor(presetId, 0xFF020202);
    overridesStore.setTokenAccentColor(presetId, 0xFF030303);
    overridesStore.setTokenKeySurfaceColor(presetId, 0xFF040404);
    overridesStore.setTokenBackgroundColor(presetId, 0xFF050505);

    overridesStore.setKeyTextColor(presetId, 0xFF112233);
    overridesStore.setSpecialKeyTextColor(presetId, 0xFF778899);
    overridesStore.setSpacebarTextColor(presetId, 0xFF99AABB);
    overridesStore.setModifierKeyTextColor(presetId, 0xFF00FF00);
    overridesStore.setEnterKeyTextColor(presetId, 0xFFFF00FF);
    overridesStore.setHintTextColor(presetId, 0xFF445566);
    overridesStore.setKeyBackgroundTint(presetId, 0x88123456);
    overridesStore.setSpecialKeyBackgroundTint(presetId, 0xFF102030);
    overridesStore.setSpacebarBackgroundTint(presetId, 0xFFA0B0C0);
    overridesStore.setModifierKeyBackgroundTint(presetId, 0xFF010203);
    overridesStore.setEnterKeyBackgroundTint(presetId, 0xFF0A0B0C);
    overridesStore.setKeyboardBackgroundTint(presetId, 0xFFABCDEF);
    overridesStore.setKeyBackgroundOpacityPercent(presetId, 73);
    overridesStore.setKeyboardBackgroundOpacityPercent(presetId, 55);
    overridesStore.setKeyFontFamily(presetId, "serif");
    overridesStore.setKeyFontStyle(presetId, 1);
    overridesStore.setHintFontFamily(presetId, "monospace");
    overridesStore.setHintFontStyle(presetId, 2);
    overridesStore.setSuggestionFontFamily(presetId, "sans");
    overridesStore.setSuggestionFontStyle(presetId, 3);
    overridesStore.setKeyboardNameFontFamily(presetId, "serif");
    overridesStore.setKeyboardNameFontStyle(presetId, 0);
    overridesStore.setKeyLabelAutoFitEnabled(presetId, false);
    overridesStore.setKeyLabelAutoFitMinSizePercent(presetId, 60);
    overridesStore.setKeyLabelEllipsizeEnabled(presetId, false);
    overridesStore.setKeyLabelTextSizePercent(presetId, 110);
    overridesStore.setHintTextSizePercent(presetId, 90);
    overridesStore.setSuggestionTextSizePercent(presetId, 110);
    overridesStore.setKeyboardNameTextSizePercent(presetId, 90);
    overridesStore.setKeyTextShadowColor(presetId, 0x55000000);
    overridesStore.setKeyTextShadowRadiusDp(presetId, 2);
    overridesStore.setKeyTextShadowOffsetXDp(presetId, -1);
    overridesStore.setKeyTextShadowOffsetYDp(presetId, 3);
    overridesStore.setSpecialKeyTextShadowColor(presetId, 0x66000000);
    overridesStore.setSpecialKeyTextShadowRadiusDp(presetId, 1);
    overridesStore.setSpecialKeyTextShadowOffsetXDp(presetId, 2);
    overridesStore.setSpecialKeyTextShadowOffsetYDp(presetId, 3);
    overridesStore.setSpacebarKeyTextShadowColor(presetId, 0x77000000);
    overridesStore.setSpacebarKeyTextShadowRadiusDp(presetId, 2);
    overridesStore.setSpacebarKeyTextShadowOffsetXDp(presetId, 0);
    overridesStore.setSpacebarKeyTextShadowOffsetYDp(presetId, -1);
    overridesStore.setModifierKeyTextShadowColor(presetId, 0x55000000);
    overridesStore.setModifierKeyTextShadowRadiusDp(presetId, 3);
    overridesStore.setModifierKeyTextShadowOffsetXDp(presetId, 1);
    overridesStore.setModifierKeyTextShadowOffsetYDp(presetId, 2);
    overridesStore.setEnterKeyTextShadowColor(presetId, 0x88000000);
    overridesStore.setEnterKeyTextShadowRadiusDp(presetId, 4);
    overridesStore.setEnterKeyTextShadowOffsetXDp(presetId, -2);
    overridesStore.setEnterKeyTextShadowOffsetYDp(presetId, 1);
    overridesStore.setKeyBackgroundShadowColor(presetId, 0x33000000);
    overridesStore.setKeyBackgroundShadowOffsetXDp(presetId, 1);
    overridesStore.setKeyBackgroundShadowOffsetYDp(presetId, 2);
    overridesStore.setKeyBackgroundShadowSpreadDp(presetId, 2);
    overridesStore.setSpecialKeyBackgroundShadowColor(presetId, 0x22000000);
    overridesStore.setSpecialKeyBackgroundShadowOffsetXDp(presetId, 1);
    overridesStore.setSpecialKeyBackgroundShadowOffsetYDp(presetId, 0);
    overridesStore.setSpacebarKeyBackgroundShadowColor(presetId, 0x44000000);
    overridesStore.setSpacebarKeyBackgroundShadowOffsetXDp(presetId, 0);
    overridesStore.setSpacebarKeyBackgroundShadowOffsetYDp(presetId, 1);
    overridesStore.setModifierKeyBackgroundShadowColor(presetId, 0x66000000);
    overridesStore.setModifierKeyBackgroundShadowOffsetXDp(presetId, -1);
    overridesStore.setModifierKeyBackgroundShadowOffsetYDp(presetId, 2);
    overridesStore.setEnterKeyBackgroundShadowColor(presetId, 0x88000000);
    overridesStore.setEnterKeyBackgroundShadowOffsetXDp(presetId, 2);
    overridesStore.setEnterKeyBackgroundShadowOffsetYDp(presetId, 3);
    overridesStore.setEnsureReadableTextEnabled(presetId, true);

    final File wallpaperFile = wallpaperStore.getWallpaperFile(presetId);
    try (FileOutputStream out = new FileOutputStream(wallpaperFile)) {
      out.write(ONE_BY_ONE_PNG);
    }
    wallpaperStore.setDimPercent(presetId, 12);
    wallpaperStore.setWallpaperMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE);
    wallpaperStore.setKeyAlphaPercent(presetId, 33);
    wallpaperStore.setKeyBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT);
    wallpaperStore.setSpecialKeyAlphaPercent(presetId, 18);
    wallpaperStore.setSpacebarAlphaPercent(presetId, 82);
    wallpaperStore.setModifierKeyAlphaPercent(presetId, 44);
    wallpaperStore.setEnterKeyAlphaPercent(presetId, 55);
    wallpaperStore.setKeyLayerOrder(
        presetId,
        new int[] {
          KeyboardWallpaperOverrideStore.KEY_LAYER_GRAIN,
          KeyboardWallpaperOverrideStore.KEY_LAYER_COLOR_WASH,
        });
    wallpaperStore.setBackgroundLayerOrder(
        presetId,
        new int[] {
          KeyboardWallpaperOverrideStore.BACKGROUND_LAYER_DIM,
          KeyboardWallpaperOverrideStore.BACKGROUND_LAYER_GRAIN
        });
    wallpaperStore.setKeyColorWashColor(presetId, 0x80FF0000);
    wallpaperStore.setKeyColorWashBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT);
    wallpaperStore.setKeyHighlightPercent(presetId, 27);
    wallpaperStore.setKeyHighlightBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    wallpaperStore.setKeyGradientBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    wallpaperStore.setKeyVignetteBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY);
    wallpaperStore.setKeyGrainBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    wallpaperStore.setBackgroundTintBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY);
    wallpaperStore.setBackgroundDimBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY);
    wallpaperStore.setBackgroundGradientBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN);
    wallpaperStore.setBackgroundVignetteBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT);
    wallpaperStore.setBackgroundGrainBlendMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL);
    wallpaperStore.setWallpaperRotationDegrees(presetId, 90);
    wallpaperStore.setWallpaperScaleMode(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_TILE);
    wallpaperStore.setWallpaperAnchor(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_BOTTOM_RIGHT);
    wallpaperStore.setMatchKeyShapeEnabled(presetId, true);
    wallpaperStore.setWallpaperQuality(
        presetId, KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_HIGH);
    wallpaperStore.setGradientPercent(presetId, 41);
    wallpaperStore.setVignettePercent(presetId, 25);
    wallpaperStore.setGrainPercent(presetId, 9);
    wallpaperStore.setSaturationPercent(presetId, 140);
    wallpaperStore.setContrastPercent(presetId, 120);
    wallpaperStore.setBrightnessPercent(presetId, 105);
    wallpaperStore.setTemperaturePercent(presetId, 140);

    final KeyboardWallpaperLayer[] exportedBackgroundLayerStack =
        new KeyboardWallpaperLayer[] {
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_THEME_TINT,
              true,
              100,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DIM,
              true,
              35,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRADIENT,
              true,
              10,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
              0x80112233,
              0x40EE9966,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              java.util.List.of(
                  new KeyboardWallpaperLayer.GradientStop(0, 0x40EE9966),
                  new KeyboardWallpaperLayer.GradientStop(55, 0x60775533),
                  new KeyboardWallpaperLayer.GradientStop(100, 0x80112233))),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_VIGNETTE,
              true,
              12,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
              0x80223344,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRAIN,
              true,
              5,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              150,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DOTS,
              true,
              8,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              0x80FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              120,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRID,
              true,
              6,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
              0x40FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              80,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_STRIPES,
              true,
              9,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              0x50FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              130,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES,
              true,
              7,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
              0x30FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              115,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_TRIANGLES,
              true,
              6,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              0x20FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              140,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_HEX,
              true,
              5,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
              0x20FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              160,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_SOLID_COLOR,
              true,
              20,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
              0x80FF00FF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
        };
    final KeyboardWallpaperLayer[] exportedKeyLayerStack =
        new KeyboardWallpaperLayer[] {
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_COLOR_WASH,
              true,
              100,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
              0x80FF0000,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_HIGHLIGHT,
              true,
              35,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
              0x80FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRADIENT,
              true,
              18,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
              0x8000FF00,
              0x200000FF,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              java.util.List.of(
                  new KeyboardWallpaperLayer.GradientStop(0, 0x200000FF),
                  new KeyboardWallpaperLayer.GradientStop(40, 0x5000FFFF),
                  new KeyboardWallpaperLayer.GradientStop(100, 0x8000FF00))),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_VIGNETTE,
              true,
              8,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              0x800000FF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_COLOR_WASH,
              true,
              50,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
              0x8000FF00,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRAIN,
              true,
              10,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              75,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DOTS,
              true,
              12,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
              0x40FF00FF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              90,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRID,
              true,
              7,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              0x30FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              110,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_STRIPES,
              true,
              11,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
              0x60FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              90,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES,
              true,
              13,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
              0x20FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              125,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_TRIANGLES,
              true,
              9,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
              0x40FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              95,
              null),
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_HEX,
              true,
              7,
              KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
              0x30FFFFFF,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              130,
              null),
        };
    wallpaperStore.setBackgroundLayerStack(presetId, exportedBackgroundLayerStack);
    wallpaperStore.setKeyLayerStack(presetId, exportedKeyLayerStack);

    final byte[] archiveBytes;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      KeyboardThemePresetTransfer.exportPreset(context, baseThemeId, presetId, outputStream);
      archiveBytes = outputStream.toByteArray();
    }

    final KeyboardThemePresetTransfer.ImportedPreset imported;
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      imported = KeyboardThemePresetTransfer.importPreset(context, baseThemeId, inputStream);
    }

    Assert.assertNotNull(imported);
    Assert.assertNotEquals(presetId, imported.presetId());

    Assert.assertEquals(
        (Integer) 0xFF010101, overridesStore.getTokenPrimaryTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF020202, overridesStore.getTokenSecondaryTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF030303, overridesStore.getTokenAccentColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF040404, overridesStore.getTokenKeySurfaceColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF050505, overridesStore.getTokenBackgroundColor(imported.presetId()));

    Assert.assertEquals((Integer) 0xFF112233, overridesStore.getKeyTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF778899, overridesStore.getSpecialKeyTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF99AABB, overridesStore.getSpacebarTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF00FF00, overridesStore.getModifierKeyTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFFFF00FF, overridesStore.getEnterKeyTextColor(imported.presetId()));
    Assert.assertEquals((Integer) 0xFF445566, overridesStore.getHintTextColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x88123456, overridesStore.getKeyBackgroundTint(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF102030, overridesStore.getSpecialKeyBackgroundTint(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFFA0B0C0, overridesStore.getSpacebarBackgroundTint(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF010203, overridesStore.getModifierKeyBackgroundTint(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFF0A0B0C, overridesStore.getEnterKeyBackgroundTint(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0xFFABCDEF, overridesStore.getKeyboardBackgroundTint(imported.presetId()));
    Assert.assertEquals(
        (Integer) 73, overridesStore.getKeyBackgroundOpacityPercent(imported.presetId()));
    Assert.assertEquals(
        (Integer) 55, overridesStore.getKeyboardBackgroundOpacityPercent(imported.presetId()));
    Assert.assertEquals("serif", overridesStore.getKeyFontFamily(imported.presetId()));
    Assert.assertEquals((Integer) 1, overridesStore.getKeyFontStyle(imported.presetId()));
    Assert.assertEquals("monospace", overridesStore.getHintFontFamily(imported.presetId()));
    Assert.assertEquals((Integer) 2, overridesStore.getHintFontStyle(imported.presetId()));
    Assert.assertEquals("sans", overridesStore.getSuggestionFontFamily(imported.presetId()));
    Assert.assertEquals((Integer) 3, overridesStore.getSuggestionFontStyle(imported.presetId()));
    Assert.assertEquals("serif", overridesStore.getKeyboardNameFontFamily(imported.presetId()));
    Assert.assertEquals((Integer) 0, overridesStore.getKeyboardNameFontStyle(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x55000000, overridesStore.getKeyTextShadowColor(imported.presetId()));
    Assert.assertEquals((Integer) 2, overridesStore.getKeyTextShadowRadiusDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) (-1), overridesStore.getKeyTextShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals((Integer) 3, overridesStore.getKeyTextShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x66000000, overridesStore.getSpecialKeyTextShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 1, overridesStore.getSpecialKeyTextShadowRadiusDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getSpecialKeyTextShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 3, overridesStore.getSpecialKeyTextShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x77000000, overridesStore.getSpacebarKeyTextShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getSpacebarKeyTextShadowRadiusDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0, overridesStore.getSpacebarKeyTextShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) (-1), overridesStore.getSpacebarKeyTextShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x55000000, overridesStore.getModifierKeyTextShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 3, overridesStore.getModifierKeyTextShadowRadiusDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 1, overridesStore.getModifierKeyTextShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getModifierKeyTextShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x88000000, overridesStore.getEnterKeyTextShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 4, overridesStore.getEnterKeyTextShadowRadiusDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) (-2), overridesStore.getEnterKeyTextShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 1, overridesStore.getEnterKeyTextShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x33000000, overridesStore.getKeyBackgroundShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 1, overridesStore.getKeyBackgroundShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getKeyBackgroundShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getKeyBackgroundShadowSpreadDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x22000000,
        overridesStore.getSpecialKeyBackgroundShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 1, overridesStore.getSpecialKeyBackgroundShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0, overridesStore.getSpecialKeyBackgroundShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x44000000,
        overridesStore.getSpacebarKeyBackgroundShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0, overridesStore.getSpacebarKeyBackgroundShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 1, overridesStore.getSpacebarKeyBackgroundShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x66000000,
        overridesStore.getModifierKeyBackgroundShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) (-1),
        overridesStore.getModifierKeyBackgroundShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getModifierKeyBackgroundShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x88000000, overridesStore.getEnterKeyBackgroundShadowColor(imported.presetId()));
    Assert.assertEquals(
        (Integer) 2, overridesStore.getEnterKeyBackgroundShadowOffsetXDp(imported.presetId()));
    Assert.assertEquals(
        (Integer) 3, overridesStore.getEnterKeyBackgroundShadowOffsetYDp(imported.presetId()));
    Assert.assertEquals(
        Boolean.TRUE, overridesStore.getEnsureReadableTextEnabled(imported.presetId()));
    Assert.assertEquals(
        Boolean.FALSE, overridesStore.getKeyLabelAutoFitEnabled(imported.presetId()));
    Assert.assertEquals(
        (Integer) 60, overridesStore.getKeyLabelAutoFitMinSizePercent(imported.presetId()));
    Assert.assertEquals(
        Boolean.FALSE, overridesStore.getKeyLabelEllipsizeEnabled(imported.presetId()));
    Assert.assertEquals(
        (Integer) 110, overridesStore.getKeyLabelTextSizePercent(imported.presetId()));
    Assert.assertEquals((Integer) 90, overridesStore.getHintTextSizePercent(imported.presetId()));
    Assert.assertEquals(
        (Integer) 110, overridesStore.getSuggestionTextSizePercent(imported.presetId()));
    Assert.assertEquals(
        (Integer) 90, overridesStore.getKeyboardNameTextSizePercent(imported.presetId()));

    Assert.assertTrue(wallpaperStore.hasWallpaper(imported.presetId()));
    Assert.assertEquals(12, wallpaperStore.getDimPercent(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE,
        wallpaperStore.getWallpaperMode(imported.presetId()));
    Assert.assertEquals(33, wallpaperStore.getKeyAlphaPercent(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
        wallpaperStore.getKeyBlendMode(imported.presetId()));
    Assert.assertEquals(18, wallpaperStore.getSpecialKeyAlphaPercent(imported.presetId()));
    Assert.assertEquals(82, wallpaperStore.getSpacebarAlphaPercent(imported.presetId()));
    Assert.assertEquals(44, wallpaperStore.getModifierKeyAlphaPercent(imported.presetId()));
    Assert.assertEquals(55, wallpaperStore.getEnterKeyAlphaPercent(imported.presetId()));
    Assert.assertArrayEquals(
        new int[] {
          KeyboardWallpaperOverrideStore.KEY_LAYER_GRAIN,
          KeyboardWallpaperOverrideStore.KEY_LAYER_COLOR_WASH,
        },
        wallpaperStore.getKeyLayerOrder(imported.presetId()));
    Assert.assertArrayEquals(
        new int[] {
          KeyboardWallpaperOverrideStore.BACKGROUND_LAYER_DIM,
          KeyboardWallpaperOverrideStore.BACKGROUND_LAYER_GRAIN
        },
        wallpaperStore.getBackgroundLayerOrder(imported.presetId()));
    Assert.assertEquals(
        (Integer) 0x80FF0000, wallpaperStore.getKeyColorWashColor(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
        wallpaperStore.getKeyColorWashBlendMode(imported.presetId()));
    Assert.assertEquals(27, wallpaperStore.getKeyHighlightPercent(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
        wallpaperStore.getKeyHighlightBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        wallpaperStore.getKeyGradientBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
        wallpaperStore.getKeyVignetteBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
        wallpaperStore.getKeyGrainBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_OVERLAY,
        wallpaperStore.getBackgroundTintBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_MULTIPLY,
        wallpaperStore.getBackgroundDimBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SCREEN,
        wallpaperStore.getBackgroundGradientBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_SOFT_LIGHT,
        wallpaperStore.getBackgroundVignetteBlendMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_BLEND_MODE_NORMAL,
        wallpaperStore.getBackgroundGrainBlendMode(imported.presetId()));
    Assert.assertEquals(90, wallpaperStore.getWallpaperRotationDegrees(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_TILE,
        wallpaperStore.getWallpaperScaleMode(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_BOTTOM_RIGHT,
        wallpaperStore.getWallpaperAnchor(imported.presetId()));
    Assert.assertTrue(wallpaperStore.isMatchKeyShapeEnabled(imported.presetId()));
    Assert.assertEquals(
        KeyboardWallpaperOverrideStore.WALLPAPER_QUALITY_HIGH,
        wallpaperStore.getWallpaperQuality(imported.presetId()));
    Assert.assertEquals(41, wallpaperStore.getGradientPercent(imported.presetId()));
    Assert.assertEquals(25, wallpaperStore.getVignettePercent(imported.presetId()));
    Assert.assertEquals(9, wallpaperStore.getGrainPercent(imported.presetId()));
    Assert.assertEquals(140, wallpaperStore.getSaturationPercent(imported.presetId()));
    Assert.assertEquals(120, wallpaperStore.getContrastPercent(imported.presetId()));
    Assert.assertEquals(105, wallpaperStore.getBrightnessPercent(imported.presetId()));
    Assert.assertEquals(140, wallpaperStore.getTemperaturePercent(imported.presetId()));

    Assert.assertTrue(wallpaperStore.hasBackgroundLayerStackOverride(imported.presetId()));
    Assert.assertArrayEquals(
        exportedBackgroundLayerStack, wallpaperStore.getBackgroundLayerStack(imported.presetId()));
    Assert.assertTrue(wallpaperStore.hasKeyLayerStackOverride(imported.presetId()));
    Assert.assertArrayEquals(
        exportedKeyLayerStack, wallpaperStore.getKeyLayerStack(imported.presetId()));

    presetStore.deletePreset(presetId);
    overridesStore.clearAllOverrides(presetId);
    wallpaperStore.clear(presetId);

    presetStore.deletePreset(imported.presetId());
    overridesStore.clearAllOverrides(imported.presetId());
    wallpaperStore.clear(imported.presetId());
  }

  @Test
  public void testImportRejectsDifferentBaseTheme() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);

    final String sourceBaseThemeId = "theme_test_transfer_source";
    final KeyboardThemePresetStore.Preset created =
        presetStore.createPreset(sourceBaseThemeId, "Source preset");

    final byte[] archiveBytes;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      KeyboardThemePresetTransfer.exportPreset(
          context, sourceBaseThemeId, created.id(), outputStream);
      archiveBytes = outputStream.toByteArray();
    }

    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      KeyboardThemePresetTransfer.importPreset(context, "theme_test_transfer_target", inputStream);
      Assert.fail("Expected import to fail for different base theme.");
    } catch (IOException e) {
      Assert.assertTrue(e.getMessage().contains("different base theme"));
    } finally {
      presetStore.deletePreset(created.id());
    }
  }

  @Test
  public void testExportImportRoundTripWithCustomKeyFont() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);
    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);

    final String baseThemeId = "theme_test_transfer_custom_font";
    final KeyboardThemePresetStore.Preset created =
        presetStore.createPreset(baseThemeId, "Custom font");
    final String presetId = created.id();

    // Minimal fake "font" bytes: TTF header (0x00010000) + payload.
    final byte[] fontBytes = new byte[] {0x00, 0x01, 0x00, 0x00, 5, 6};
    final File tempSourceFont = File.createTempFile("nsk-font", ".ttf", context.getCacheDir());
    try (FileOutputStream out = new FileOutputStream(tempSourceFont)) {
      out.write(fontBytes);
    }

    overridesStore.importCustomKeyFontFromFile(presetId, tempSourceFont, "Example.ttf");
    overridesStore.setSuggestionFontFamily(
        presetId, KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM);

    final byte[] archiveBytes;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      KeyboardThemePresetTransfer.exportPreset(context, baseThemeId, presetId, outputStream);
      archiveBytes = outputStream.toByteArray();
    }

    final KeyboardThemePresetTransfer.ImportedPreset imported;
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      imported = KeyboardThemePresetTransfer.importPreset(context, baseThemeId, inputStream);
    }

    Assert.assertNotNull(imported);
    Assert.assertNotEquals(presetId, imported.presetId());
    Assert.assertEquals(
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM,
        overridesStore.getSuggestionFontFamily(imported.presetId()));
    Assert.assertTrue(overridesStore.hasCustomKeyFont(imported.presetId()));
    Assert.assertEquals(
        "Example.ttf", overridesStore.getCustomKeyFontDisplayName(imported.presetId()));

    final File importedFont = overridesStore.getCustomKeyFontFile(imported.presetId());
    Assert.assertTrue(importedFont.isFile());
    final byte[] importedBytes = new byte[fontBytes.length];
    try (FileInputStream in = new FileInputStream(importedFont)) {
      Assert.assertEquals(fontBytes.length, in.read(importedBytes));
    }
    Assert.assertArrayEquals(fontBytes, importedBytes);

    //noinspection ResultOfMethodCallIgnored
    tempSourceFont.delete();
    presetStore.deletePreset(presetId);
    overridesStore.clearAllOverrides(presetId);

    presetStore.deletePreset(imported.presetId());
    overridesStore.clearAllOverrides(imported.presetId());
  }

  @Test
  public void testReadArchiveInfoAndImportWithNameOverride() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);

    final String baseThemeId = "theme_test_transfer_info";
    final KeyboardThemePresetStore.Preset created =
        presetStore.createPreset(baseThemeId, "Original");
    final String presetId = created.id();

    final byte[] archiveBytes;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      KeyboardThemePresetTransfer.exportPreset(context, baseThemeId, presetId, outputStream, false);
      archiveBytes = outputStream.toByteArray();
    }

    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      final KeyboardThemePresetTransfer.PresetArchiveInfo info =
          KeyboardThemePresetTransfer.readArchiveInfo(inputStream);
      Assert.assertEquals(1, info.archiveVersion());
      Assert.assertEquals(baseThemeId, info.baseThemeId());
      Assert.assertEquals("Original", info.presetName());
      Assert.assertFalse(info.hasWallpaper());
      Assert.assertFalse(info.hasColors());
      Assert.assertFalse(info.hasTypography());
      Assert.assertFalse(info.hasShadows());
      Assert.assertFalse(info.hasCustomFont());
    }

    final KeyboardThemePresetTransfer.ImportedPreset imported;
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      imported =
          KeyboardThemePresetTransfer.importPreset(
              context, baseThemeId, inputStream, "Renamed import");
    }

    Assert.assertNotNull(imported);
    Assert.assertEquals("Renamed import", imported.presetName());
    Assert.assertEquals("Renamed import", presetStore.getPresetName(imported.presetId()));

    presetStore.deletePreset(presetId);
    presetStore.deletePreset(imported.presetId());
  }

  @Test
  public void testExportIncludesPreviewImageWhenSafe() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);

    final String baseThemeId = "theme_test_transfer_preview";
    final KeyboardThemePresetStore.Preset created =
        presetStore.createPreset(baseThemeId, "Preview");
    final String presetId = created.id();

    final byte[] archiveBytes;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      KeyboardThemePresetTransfer.exportPreset(
          context, baseThemeId, presetId, outputStream, false, ONE_BY_ONE_PNG);
      archiveBytes = outputStream.toByteArray();
    }

    final KeyboardThemePresetTransfer.PresetArchiveInfo info;
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      info = KeyboardThemePresetTransfer.readArchiveInfo(inputStream);
    }

    Assert.assertEquals(baseThemeId, info.baseThemeId());
    Assert.assertFalse(info.hasWallpaper());
    Assert.assertTrue(info.hasPreviewImage());
    Assert.assertEquals(BuildConfig.VERSION_NAME, info.exportedByVersionName());

    final Bitmap previewBitmap;
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveBytes)) {
      previewBitmap = KeyboardThemePresetTransfer.readPreviewBitmap(inputStream);
    }
    Assert.assertNotNull(previewBitmap);
    Assert.assertEquals(1, previewBitmap.getWidth());
    Assert.assertEquals(1, previewBitmap.getHeight());
    previewBitmap.recycle();

    presetStore.deletePreset(presetId);
  }
}
