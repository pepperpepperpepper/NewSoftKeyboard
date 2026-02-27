package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

final class KeyboardWallpaperFileStore {

  static final class ImportResult {
    final boolean hadExistingWallpaper;
    final int exifRotationDegrees;

    ImportResult(boolean hadExistingWallpaper, int exifRotationDegrees) {
      this.hadExistingWallpaper = hadExistingWallpaper;
      this.exifRotationDegrees = exifRotationDegrees;
    }
  }

  private final Context appContext;

  KeyboardWallpaperFileStore(@NonNull Context context) {
    this.appContext = context.getApplicationContext();
  }

  @NonNull
  File getWallpaperFile(@NonNull String themeId) {
    final File dir = new File(appContext.getFilesDir(), "wallpapers");
    if (!dir.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      dir.mkdirs();
    }
    return new File(dir, hashToFileName(themeId) + ".webp");
  }

  boolean hasWallpaper(@NonNull String themeId) {
    return getWallpaperFile(themeId).isFile();
  }

  void deleteWallpaperBestEffort(@NonNull String themeId) {
    final File file = getWallpaperFile(themeId);
    // best-effort; this is user storage and must never crash
    //noinspection ResultOfMethodCallIgnored
    file.delete();
  }

  void copyToTheme(@NonNull String sourceThemeId, @NonNull String targetThemeId)
      throws IOException {
    if (sourceThemeId.equals(targetThemeId)) return;

    final File source = getWallpaperFile(sourceThemeId);
    if (!source.isFile()) {
      throw new IOException("No wallpaper found for theme " + sourceThemeId);
    }

    final File target = getWallpaperFile(targetThemeId);
    copyFile(source, target);
  }

  @NonNull
  ImportResult importFromUri(
      @NonNull String themeId,
      @NonNull Uri sourceUri,
      int maxWidth,
      int maxHeight,
      boolean highQualityImportEnabled)
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
      final int quality = highQualityImportEnabled ? 100 : 90;
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

    return new ImportResult(hadExistingWallpaper, exifRotationDegrees);
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

  @Nullable
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
