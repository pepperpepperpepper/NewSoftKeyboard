package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.Color;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class WallpaperColorSchemeExtractorTest {

  @Test
  public void testExtractSolidRedChoosesWhiteText() {
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.RED);
    try {
      final WallpaperColorSchemeExtractor.Result result =
          WallpaperColorSchemeExtractor.extract(bitmap, 0);
      Assert.assertEquals(Color.RED, result.accentColor);
      Assert.assertEquals(Color.WHITE, result.keyTextColor);
      Assert.assertEquals(0xCCFFFFFF, result.hintTextColor);
    } finally {
      bitmap.recycle();
    }
  }

  @Test
  public void testExtractSolidWhiteChoosesBlackText() {
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.WHITE);
    try {
      final WallpaperColorSchemeExtractor.Result result =
          WallpaperColorSchemeExtractor.extract(bitmap, 0);
      Assert.assertEquals(Color.WHITE, result.accentColor);
      Assert.assertEquals(Color.BLACK, result.keyTextColor);
      Assert.assertEquals(0xCC000000, result.hintTextColor);
    } finally {
      bitmap.recycle();
    }
  }

  @Test
  public void testDimAffectsDarkDetection() {
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.WHITE);
    try {
      final WallpaperColorSchemeExtractor.Result result =
          WallpaperColorSchemeExtractor.extract(bitmap, 100);
      Assert.assertEquals(Color.WHITE, result.accentColor);
      Assert.assertEquals(Color.WHITE, result.keyTextColor);
    } finally {
      bitmap.recycle();
    }
  }

  @Test
  public void testPaletteSourceSelectsDifferentAccents() {
    final Bitmap bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888);
    try {
      for (int y = 0; y < 10; y++) {
        for (int x = 0; x < 20; x++) {
          bitmap.setPixel(x, y, x < 10 ? Color.RED : Color.GRAY);
        }
      }

      final WallpaperColorSchemeExtractor.Result vibrant =
          WallpaperColorSchemeExtractor.extract(
              bitmap, 0, WallpaperColorSchemeExtractor.PaletteSource.VIBRANT);
      final WallpaperColorSchemeExtractor.Result muted =
          WallpaperColorSchemeExtractor.extract(
              bitmap, 0, WallpaperColorSchemeExtractor.PaletteSource.MUTED);

      Assert.assertNotEquals(vibrant.accentColor, muted.accentColor);

      final int vibrantR = (vibrant.accentColor >> 16) & 0xFF;
      final int vibrantG = (vibrant.accentColor >> 8) & 0xFF;
      final int vibrantB = vibrant.accentColor & 0xFF;
      Assert.assertTrue(vibrantR > 200);
      Assert.assertTrue(vibrantG < 80);
      Assert.assertTrue(vibrantB < 80);

      final int mutedR = (muted.accentColor >> 16) & 0xFF;
      final int mutedG = (muted.accentColor >> 8) & 0xFF;
      final int mutedB = muted.accentColor & 0xFF;
      Assert.assertTrue(Math.abs(mutedR - mutedG) < 32);
      Assert.assertTrue(Math.abs(mutedR - mutedB) < 32);
      Assert.assertTrue(Math.abs(mutedG - mutedB) < 32);
    } finally {
      bitmap.recycle();
    }
  }
}
