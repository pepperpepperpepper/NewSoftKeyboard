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

    store.markWallpaperInvalid(themeId);
    assertTrue(store.isWallpaperInvalid(themeId));

    final int tokenBefore = store.getWallpaperChangeToken(themeId);

    store.clear(themeId);

    assertFalse(store.hasWallpaper(themeId));
    assertFalse(file.exists());
    assertEquals(0, store.getDimPercent(themeId));
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
    assertTrue(store.hasWallpaper(sourceId));

    store.setDimPercent(targetId, 7);
    store.markWallpaperInvalid(targetId);
    assertTrue(store.isWallpaperInvalid(targetId));

    final int tokenBefore = store.getWallpaperChangeToken(targetId);

    store.copyToTheme(sourceId, targetId);

    assertTrue(store.hasWallpaper(targetId));
    assertFalse(store.isWallpaperInvalid(targetId));
    assertEquals(42, store.getDimPercent(targetId));
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
}
