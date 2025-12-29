package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/** Stores user-selected keyboard wallpaper overrides per theme id. */
public class KeyboardWallpaperOverrideStore {

  private static final String PREF_DIM_PREFIX = "photo_wallpaper_dim::";
  private static final String PREF_CHANGE_PREFIX = "photo_wallpaper_change::";
  private static final String PREF_INVALID_PREFIX = "photo_wallpaper_invalid::";

  private final Context appContext;
  private final SharedPreferences prefs;

  public KeyboardWallpaperOverrideStore(@NonNull Context context) {
    appContext = context.getApplicationContext();
    prefs = DirectBootAwareSharedPreferences.create(appContext);
  }

  @NonNull
  public static String dimKey(@NonNull String themeId) {
    return PREF_DIM_PREFIX + themeId;
  }

  @NonNull
  public static String changeKey(@NonNull String themeId) {
    return PREF_CHANGE_PREFIX + themeId;
  }

  @NonNull
  public static String invalidKey(@NonNull String themeId) {
    return PREF_INVALID_PREFIX + themeId;
  }

  public int getDimPercent(@NonNull String themeId) {
    return prefs.getInt(dimKey(themeId), 0);
  }

  public void setDimPercent(@NonNull String themeId, int dimPercent) {
    prefs.edit().putInt(dimKey(themeId), clampPercent(dimPercent)).apply();
  }

  public boolean isWallpaperInvalid(@NonNull String themeId) {
    return prefs.getBoolean(invalidKey(themeId), false);
  }

  public void markWallpaperInvalid(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(invalidKey(themeId), true);
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public int getWallpaperChangeToken(@NonNull String themeId) {
    return prefs.getInt(changeKey(themeId), 0);
  }

  private void markWallpaperChanged(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  private void markWallpaperChanged(
      @NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    final String key = changeKey(themeId);
    final int current = prefs.getInt(key, 0);
    editor.putInt(key, current + 1);
  }

  public void clear(@NonNull String themeId) {
    final File file = getWallpaperFile(themeId);
    // best-effort; this is user storage and must never crash
    //noinspection ResultOfMethodCallIgnored
    file.delete();
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(dimKey(themeId));
    editor.remove(invalidKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public void copyToTheme(@NonNull String sourceThemeId, @NonNull String targetThemeId)
      throws IOException {
    if (sourceThemeId.equals(targetThemeId)) return;

    final File source = getWallpaperFile(sourceThemeId);
    if (!source.isFile()) {
      throw new IOException("No wallpaper found for theme " + sourceThemeId);
    }

    final File target = getWallpaperFile(targetThemeId);
    copyFile(source, target);

    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(dimKey(targetThemeId), getDimPercent(sourceThemeId));
    editor.remove(invalidKey(targetThemeId));
    markWallpaperChanged(targetThemeId, editor);
    editor.apply();
  }

  @NonNull
  public File getWallpaperFile(@NonNull String themeId) {
    final File dir = new File(appContext.getFilesDir(), "wallpapers");
    if (!dir.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      dir.mkdirs();
    }
    return new File(dir, hashToFileName(themeId) + ".webp");
  }

  public boolean hasWallpaper(@NonNull String themeId) {
    return getWallpaperFile(themeId).isFile();
  }

  /**
   * Reads the image from the given {@link Uri}, scales it down, and stores it as an app-private
   * processed copy.
   */
  public void importFromUri(@NonNull String themeId, @NonNull Uri sourceUri) throws IOException {
    importFromUri(themeId, sourceUri, 2048, 2048);
  }

  /**
   * Reads the image from the given {@link Uri}, scales it down, and stores it as an app-private
   * processed copy.
   *
   * @param maxWidth The maximum width of the stored bitmap.
   * @param maxHeight The maximum height of the stored bitmap.
   */
  public void importFromUri(
      @NonNull String themeId, @NonNull Uri sourceUri, int maxWidth, int maxHeight)
      throws IOException {
    final ContentResolver resolver = appContext.getContentResolver();
    final Bitmap decoded =
        decodeDownscaledBitmap(resolver, sourceUri, Math.max(1, maxWidth), Math.max(1, maxHeight));
    if (decoded == null) {
      throw new IOException("Failed to decode bitmap from " + sourceUri);
    }

    final File target = getWallpaperFile(themeId);
    final FileOutputStream output = new FileOutputStream(target);
    try {
      if (!decoded.compress(Bitmap.CompressFormat.WEBP, 90, output)) {
        throw new IOException("Failed to write wallpaper for theme " + themeId);
      }
    } finally {
      try {
        output.close();
      } catch (IOException ignored) {
        // ignore
      }
      decoded.recycle();
    }

    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(invalidKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  private static Bitmap decodeDownscaledBitmap(
      @NonNull ContentResolver resolver, @NonNull Uri uri, int maxWidth, int maxHeight)
      throws IOException {
    final BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;

    try (InputStream in = resolver.openInputStream(uri)) {
      if (in == null) throw new IOException("ContentResolver returned null InputStream for " + uri);
      BitmapFactory.decodeStream(in, null, bounds);
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

    final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
    decodeOptions.inSampleSize =
        calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight);
    decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

    final Bitmap decoded;
    try (InputStream in = resolver.openInputStream(uri)) {
      if (in == null) throw new IOException("ContentResolver returned null InputStream for " + uri);
      try {
        decoded = BitmapFactory.decodeStream(in, null, decodeOptions);
      } catch (OutOfMemoryError oom) {
        throw new IOException("Out of memory while decoding bitmap from " + uri, oom);
      }
    }
    if (decoded == null) return null;

    final int width = decoded.getWidth();
    final int height = decoded.getHeight();
    final float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
    if (scale >= 1f) return decoded;

    final int targetW = Math.max(1, Math.round(width * scale));
    final int targetH = Math.max(1, Math.round(height * scale));
    final Bitmap scaled;
    try {
      scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true);
    } catch (OutOfMemoryError oom) {
      decoded.recycle();
      throw new IOException("Out of memory while scaling bitmap from " + uri, oom);
    }
    if (scaled != decoded) {
      decoded.recycle();
    }
    return scaled;
  }

  private static void copyFile(@NonNull File source, @NonNull File target) throws IOException {
    final File parent = target.getParentFile();
    if (parent != null && !parent.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      parent.mkdirs();
    }

    try (FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(target, false)) {
      final byte[] buffer = new byte[16 * 1024];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
    }
  }

  private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
    int inSampleSize = 1;
    while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
      inSampleSize *= 2;
    }
    return Math.max(1, inSampleSize);
  }

  private static int clampPercent(int value) {
    if (value < 0) return 0;
    if (value > 100) return 100;
    return value;
  }

  private static String hashToFileName(@NonNull String raw) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      final StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      // should never happen on Android, but keep it safe
      return Integer.toHexString(raw.hashCode());
    }
  }
}
