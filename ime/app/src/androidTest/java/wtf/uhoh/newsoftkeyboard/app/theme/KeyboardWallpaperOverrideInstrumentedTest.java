package wtf.uhoh.newsoftkeyboard.app.theme;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;

@RunWith(AndroidJUnit4.class)
public class KeyboardWallpaperOverrideInstrumentedTest {

  @Test
  public void importFromResourceUriIsUsableByResolverAndClearWorks() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    final KeyboardWallpaperResolver resolver = new KeyboardWallpaperResolver(context);

    final String themeAId = "instrumented-theme-a";
    final String themeBId = "instrumented-theme-b";
    store.clear(themeAId);
    store.clear(themeBId);

    store.setDimPercent(themeAId, 30);
    final Uri source =
        Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://"
                + context.getPackageName()
                + "/"
                + R.drawable.nsk_wallpaper);
    store.importFromUri(themeAId, source);

    assertTrue(store.hasWallpaper(themeAId));
    assertFalse(store.isWallpaperInvalid(themeAId));

    final KeyboardTheme themeA = createLocalTheme(context, themeAId);
    assertTrue(resolver.resolveImeWallpaper(themeA) instanceof LayerDrawable);

    final KeyboardTheme themeB = createLocalTheme(context, themeBId);
    assertFalse(resolver.resolveImeWallpaper(themeB) instanceof LayerDrawable);

    store.clear(themeAId);
    assertFalse(store.hasWallpaper(themeAId));
    assertFalse(resolver.resolveImeWallpaper(themeA) instanceof LayerDrawable);
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
}
