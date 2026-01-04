package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/** Stores user-selected keyboard wallpaper overrides per theme id. */
public class KeyboardWallpaperOverrideStore {

  private static final String PREF_DIM_PREFIX = "photo_wallpaper_dim::";
  private static final String PREF_CHANGE_PREFIX = "photo_wallpaper_change::";
  private static final String PREF_INVALID_PREFIX = "photo_wallpaper_invalid::";
  private static final String PREF_MODE_PREFIX = "photo_wallpaper_mode::";
  private static final String PREF_KEY_ALPHA_PREFIX = "photo_wallpaper_key_alpha::";
  private static final String PREF_ROTATION_PREFIX = "photo_wallpaper_rotation::";
  private static final String PREF_SCALE_MODE_PREFIX = "photo_wallpaper_scale_mode::";
  private static final String PREF_ANCHOR_PREFIX = "photo_wallpaper_anchor::";
  private static final String PREF_MATCH_KEY_SHAPE_PREFIX = "photo_wallpaper_match_key_shape::";
  private static final String PREF_IMPORT_HIGH_QUALITY = "photo_wallpaper_import_high_quality";

  public static final int WALLPAPER_MODE_BACKGROUND_ONLY = 0;
  public static final int WALLPAPER_MODE_BACKGROUND_KEY_TINT = 1;
  public static final int WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE = 2;

  public static final int WALLPAPER_SCALE_MODE_CROP = 0;
  public static final int WALLPAPER_SCALE_MODE_FIT = 1;
  public static final int WALLPAPER_SCALE_MODE_STRETCH = 2;
  public static final int WALLPAPER_SCALE_MODE_TILE = 3;
  public static final int WALLPAPER_SCALE_MODE_MIRROR = 4;

  public static final int WALLPAPER_ANCHOR_TOP_LEFT = 0;
  public static final int WALLPAPER_ANCHOR_TOP = 1;
  public static final int WALLPAPER_ANCHOR_TOP_RIGHT = 2;
  public static final int WALLPAPER_ANCHOR_LEFT = 3;
  public static final int WALLPAPER_ANCHOR_CENTER = 4;
  public static final int WALLPAPER_ANCHOR_RIGHT = 5;
  public static final int WALLPAPER_ANCHOR_BOTTOM_LEFT = 6;
  public static final int WALLPAPER_ANCHOR_BOTTOM = 7;
  public static final int WALLPAPER_ANCHOR_BOTTOM_RIGHT = 8;

  public static final int DEFAULT_KEY_ALPHA_PERCENT = 20;

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

  @NonNull
  public static String modeKey(@NonNull String themeId) {
    return PREF_MODE_PREFIX + themeId;
  }

  @NonNull
  public static String keyAlphaKey(@NonNull String themeId) {
    return PREF_KEY_ALPHA_PREFIX + themeId;
  }

  @NonNull
  public static String rotationKey(@NonNull String themeId) {
    return PREF_ROTATION_PREFIX + themeId;
  }

  @NonNull
  public static String scaleModeKey(@NonNull String themeId) {
    return PREF_SCALE_MODE_PREFIX + themeId;
  }

  @NonNull
  public static String anchorKey(@NonNull String themeId) {
    return PREF_ANCHOR_PREFIX + themeId;
  }

  @NonNull
  public static String matchKeyShapeKey(@NonNull String themeId) {
    return PREF_MATCH_KEY_SHAPE_PREFIX + themeId;
  }

  public int getDimPercent(@NonNull String themeId) {
    return prefs.getInt(dimKey(themeId), 0);
  }

  public void setDimPercent(@NonNull String themeId, int dimPercent) {
    prefs.edit().putInt(dimKey(themeId), clampPercent(dimPercent)).apply();
  }

  public int getWallpaperMode(@NonNull String themeId) {
    final boolean hasModeOverride = prefs.contains(modeKey(themeId));
    final int defaultMode =
        hasModeOverride || !hasWallpaper(themeId)
            ? WALLPAPER_MODE_BACKGROUND_ONLY
            : WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE;
    return normalizeMode(prefs.getInt(modeKey(themeId), defaultMode));
  }

  public void setWallpaperMode(@NonNull String themeId, int mode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(modeKey(themeId), normalizeMode(mode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public int getKeyAlphaPercent(@NonNull String themeId) {
    return clampPercent(prefs.getInt(keyAlphaKey(themeId), DEFAULT_KEY_ALPHA_PERCENT));
  }

  public void setKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyAlphaKey(themeId), clampPercent(alphaPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public int getWallpaperRotationDegrees(@NonNull String themeId) {
    return normalizeRotationDegrees(prefs.getInt(rotationKey(themeId), 0));
  }

  public void setWallpaperRotationDegrees(@NonNull String themeId, int rotationDegrees) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(rotationKey(themeId), normalizeRotationDegrees(rotationDegrees));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public void rotateWallpaperClockwise90(@NonNull String themeId) {
    final int current = getWallpaperRotationDegrees(themeId);
    final int next = (current + 90) % 360;
    setWallpaperRotationDegrees(themeId, next);
  }

  public int getWallpaperScaleMode(@NonNull String themeId) {
    return normalizeScaleMode(prefs.getInt(scaleModeKey(themeId), WALLPAPER_SCALE_MODE_CROP));
  }

  public void setWallpaperScaleMode(@NonNull String themeId, int scaleMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(scaleModeKey(themeId), normalizeScaleMode(scaleMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public int getWallpaperAnchor(@NonNull String themeId) {
    return normalizeAnchor(prefs.getInt(anchorKey(themeId), WALLPAPER_ANCHOR_CENTER));
  }

  public void setWallpaperAnchor(@NonNull String themeId, int anchor) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(anchorKey(themeId), normalizeAnchor(anchor));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  public boolean isMatchKeyShapeEnabled(@NonNull String themeId) {
    return prefs.getBoolean(matchKeyShapeKey(themeId), false);
  }

  public void setMatchKeyShapeEnabled(@NonNull String themeId, boolean enabled) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(matchKeyShapeKey(themeId), enabled);
    markWallpaperChanged(themeId, editor);
    editor.apply();
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

  public boolean isHighQualityImportEnabled() {
    return prefs.getBoolean(PREF_IMPORT_HIGH_QUALITY, false);
  }

  public void setHighQualityImportEnabled(boolean enabled) {
    prefs.edit().putBoolean(PREF_IMPORT_HIGH_QUALITY, enabled).apply();
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
    editor.remove(modeKey(themeId));
    editor.remove(keyAlphaKey(themeId));
    editor.remove(rotationKey(themeId));
    editor.remove(scaleModeKey(themeId));
    editor.remove(anchorKey(themeId));
    editor.remove(matchKeyShapeKey(themeId));
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
    editor.putInt(modeKey(targetThemeId), getWallpaperMode(sourceThemeId));
    editor.putInt(keyAlphaKey(targetThemeId), getKeyAlphaPercent(sourceThemeId));
    editor.putInt(rotationKey(targetThemeId), getWallpaperRotationDegrees(sourceThemeId));
    editor.putInt(scaleModeKey(targetThemeId), getWallpaperScaleMode(sourceThemeId));
    editor.putInt(anchorKey(targetThemeId), getWallpaperAnchor(sourceThemeId));
    editor.putBoolean(matchKeyShapeKey(targetThemeId), isMatchKeyShapeEnabled(sourceThemeId));
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
    final int exifRotationDegrees = readExifRotationDegrees(resolver, sourceUri);
    final Bitmap decoded =
        decodeDownscaledBitmap(resolver, sourceUri, Math.max(1, maxWidth), Math.max(1, maxHeight));
    if (decoded == null) {
      throw new IOException("Failed to decode bitmap from " + sourceUri);
    }

    final File target = getWallpaperFile(themeId);
    final boolean hadExistingWallpaper = target.isFile();
    final FileOutputStream output = new FileOutputStream(target);
    try {
      final int quality = isHighQualityImportEnabled() ? 100 : 90;
      try {
        if (!decoded.compress(Bitmap.CompressFormat.WEBP, quality, output)) {
          throw new IOException("Failed to write wallpaper for theme " + themeId);
        }
      } catch (OutOfMemoryError oom) {
        throw new IOException("Out of memory while encoding wallpaper for theme " + themeId, oom);
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
    // Default to a visible mode when a user first imports a wallpaper, since many themes have an
    // opaque keyboard background and "background only" would appear to do nothing.
    //
    // Also: if an older build persisted "background only" before any wallpaper existed, treat the
    // first import as a migration and still default to a visible mode.
    if (!prefs.contains(modeKey(themeId))
        || (!hadExistingWallpaper
            && normalizeMode(prefs.getInt(modeKey(themeId), WALLPAPER_MODE_BACKGROUND_ONLY))
                == WALLPAPER_MODE_BACKGROUND_ONLY)) {
      editor.putInt(modeKey(themeId), WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE);
    }
    // When importing a wallpaper for the first time, default the key overlay opacity to a visible
    // value so the photo doesn't appear "invisible" on opaque themes.
    if (!hadExistingWallpaper && !prefs.contains(keyAlphaKey(themeId))) {
      editor.putInt(keyAlphaKey(themeId), 60);
    }
    if (exifRotationDegrees != 0) {
      editor.putInt(rotationKey(themeId), exifRotationDegrees);
    } else {
      editor.remove(rotationKey(themeId));
    }
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  private static int readExifRotationDegrees(@NonNull ContentResolver resolver, @NonNull Uri uri) {
    // Best-effort: framework ExifInterface exists from API 24+. Use reflection so we don't risk
    // class-verification issues on older Android versions.
    if (Build.VERSION.SDK_INT < 24) return 0;

    try (InputStream in = resolver.openInputStream(uri)) {
      if (in == null) return 0;

      final Class<?> exifClass = Class.forName("android.media.ExifInterface");
      final Constructor<?> ctor = exifClass.getConstructor(InputStream.class);
      final Object exif = ctor.newInstance(in);

      final Method getAttributeInt =
          exifClass.getMethod("getAttributeInt", String.class, int.class);
      final int orientation = (int) getAttributeInt.invoke(exif, "Orientation", 1 /*normal*/);
      switch (orientation) {
        case 3: // ORIENTATION_ROTATE_180
        case 4: // ORIENTATION_FLIP_VERTICAL (mirror unsupported; keep rotation)
          return 180;
        case 6: // ORIENTATION_ROTATE_90
        case 5: // ORIENTATION_TRANSPOSE (mirror unsupported; keep rotation)
          return 90;
        case 8: // ORIENTATION_ROTATE_270
        case 7: // ORIENTATION_TRANSVERSE (mirror unsupported; keep rotation)
          return 270;
        default:
          return 0;
      }
    } catch (Exception ignored) {
      return 0;
    }
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

  private static int normalizeMode(int mode) {
    switch (mode) {
      case WALLPAPER_MODE_BACKGROUND_KEY_TINT:
      case WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE:
      case WALLPAPER_MODE_BACKGROUND_ONLY:
        return mode;
      default:
        return WALLPAPER_MODE_BACKGROUND_ONLY;
    }
  }

  public static int normalizeRotationDegrees(int rotationDegrees) {
    final int normalized = ((rotationDegrees % 360) + 360) % 360;
    switch (normalized) {
      case 0:
      case 90:
      case 180:
      case 270:
        return normalized;
      default:
        return 0;
    }
  }

  private static int normalizeScaleMode(int scaleMode) {
    switch (scaleMode) {
      case WALLPAPER_SCALE_MODE_CROP:
      case WALLPAPER_SCALE_MODE_FIT:
      case WALLPAPER_SCALE_MODE_STRETCH:
      case WALLPAPER_SCALE_MODE_TILE:
      case WALLPAPER_SCALE_MODE_MIRROR:
        return scaleMode;
      default:
        return WALLPAPER_SCALE_MODE_CROP;
    }
  }

  private static int normalizeAnchor(int anchor) {
    switch (anchor) {
      case WALLPAPER_ANCHOR_TOP_LEFT:
      case WALLPAPER_ANCHOR_TOP:
      case WALLPAPER_ANCHOR_TOP_RIGHT:
      case WALLPAPER_ANCHOR_LEFT:
      case WALLPAPER_ANCHOR_CENTER:
      case WALLPAPER_ANCHOR_RIGHT:
      case WALLPAPER_ANCHOR_BOTTOM_LEFT:
      case WALLPAPER_ANCHOR_BOTTOM:
      case WALLPAPER_ANCHOR_BOTTOM_RIGHT:
        return anchor;
      default:
        return WALLPAPER_ANCHOR_CENTER;
    }
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
