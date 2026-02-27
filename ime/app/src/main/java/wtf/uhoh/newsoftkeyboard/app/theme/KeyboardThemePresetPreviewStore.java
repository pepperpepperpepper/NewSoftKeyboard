package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stores per-preset preview thumbnails for faster preset lists and import/export previews. */
public final class KeyboardThemePresetPreviewStore {

  private final Context appContext;

  public KeyboardThemePresetPreviewStore(@NonNull Context context) {
    appContext = context.getApplicationContext();
  }

  @NonNull
  public File getPreviewFile(@NonNull String presetId) {
    final File dir = new File(appContext.getFilesDir(), "preset_previews");
    if (!dir.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      dir.mkdirs();
    }
    return new File(dir, hashToFileName(presetId) + ".png");
  }

  public boolean hasPreview(@NonNull String presetId) {
    return getPreviewFile(presetId).isFile();
  }

  public void deletePreviewBestEffort(@NonNull String presetId) {
    final File file = getPreviewFile(presetId);
    // best-effort; this is user storage and must never crash
    //noinspection ResultOfMethodCallIgnored
    file.delete();
  }

  public void savePreviewPngBytes(@NonNull String presetId, @NonNull byte[] pngBytes)
      throws IOException {
    final File target = getPreviewFile(presetId);
    final File parent = target.getParentFile();
    if (parent != null && !parent.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      parent.mkdirs();
    }
    try (FileOutputStream out = new FileOutputStream(target, false)) {
      out.write(pngBytes);
    }
  }

  @Nullable
  public Bitmap readPreviewBitmap(@NonNull String presetId, int maxWidthPx, int maxHeightPx) {
    final File file = getPreviewFile(presetId);
    if (!file.isFile()) return null;

    final BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

    final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
    decodeOptions.inSampleSize =
        computeInSampleSize(bounds.outWidth, bounds.outHeight, maxWidthPx, maxHeightPx);
    decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

    try {
      return BitmapFactory.decodeFile(file.getAbsolutePath(), decodeOptions);
    } catch (OutOfMemoryError oom) {
      return null;
    }
  }

  private static int computeInSampleSize(int width, int height, int reqWidth, int reqHeight) {
    int inSampleSize = 1;
    while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
      inSampleSize *= 2;
    }
    return Math.max(1, inSampleSize);
  }

  @NonNull
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
