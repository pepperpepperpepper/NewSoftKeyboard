package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.LruCache;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.UserManagerCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class KeyboardThemeUserCustomFontStore {

  private static final long MAX_CUSTOM_FONT_BYTES = 10L * 1024L * 1024L;

  private static final Object CUSTOM_FONT_CACHE_LOCK = new Object();
  private static final LruCache<String, Typeface> customFontCache = new LruCache<>(8);

  private final Context appContext;
  private final SharedPreferences prefs;

  KeyboardThemeUserCustomFontStore(@NonNull Context context, @NonNull SharedPreferences prefs) {
    appContext = context.getApplicationContext();
    this.prefs = prefs;
  }

  @NonNull
  File getCustomKeyFontFile(@NonNull String themeId) {
    final File dir = new File(appContext.getFilesDir(), "theme-fonts");
    if (!dir.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      dir.mkdirs();
    }
    return new File(dir, hashToFileName(themeId) + ".ttf");
  }

  boolean hasCustomKeyFont(@NonNull String themeId) {
    return getCustomKeyFontFile(themeId).isFile();
  }

  @Nullable
  Typeface getCustomKeyFontTypefaceIfAny(@NonNull String themeId) {
    if (!UserManagerCompat.isUserUnlocked(appContext)) return null;

    final File file = getCustomKeyFontFile(themeId);
    if (!file.isFile()) return null;

    final String path = file.getAbsolutePath();
    Typeface cached;
    synchronized (CUSTOM_FONT_CACHE_LOCK) {
      cached = customFontCache.get(path);
    }
    if (cached != null) {
      return cached;
    }

    try {
      cached = Typeface.createFromFile(file);
      if (cached != null) {
        synchronized (CUSTOM_FONT_CACHE_LOCK) {
          customFontCache.put(path, cached);
        }
      }
      return cached;
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Nullable
  String getCustomKeyFontDisplayName(@NonNull String themeId) {
    final String key = KeyboardThemeUserOverridesTypographyPrefs.keyCustomFontNameKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getString(key, null);
  }

  void importCustomKeyFontFromUri(@NonNull String themeId, @NonNull Uri sourceUri)
      throws IOException {
    final ContentResolver resolver = appContext.getContentResolver();
    final File target = getCustomKeyFontFile(themeId);

    @Nullable String displayName = null;
    try (Cursor cursor =
        resolver.query(sourceUri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (index >= 0) displayName = cursor.getString(index);
      }
    } catch (Exception ignored) {
      displayName = null;
    }

    final File parent = target.getParentFile();
    if (parent != null && !parent.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      parent.mkdirs();
    }

    final File temp =
        File.createTempFile(
            "nsk_custom_font", ".tmp", parent != null ? parent : appContext.getCacheDir());
    try (InputStream in = resolver.openInputStream(sourceUri);
        FileOutputStream out = new FileOutputStream(temp)) {
      if (in == null) throw new IOException("Could not open custom font: " + sourceUri);
      copyWithLimit(in, out, MAX_CUSTOM_FONT_BYTES);
      validateCustomFontFileOrThrow(temp);
    } catch (IOException e) {
      //noinspection ResultOfMethodCallIgnored
      temp.delete();
      throw e;
    }

    //noinspection ResultOfMethodCallIgnored
    target.delete();
    if (!temp.renameTo(target)) {
      //noinspection ResultOfMethodCallIgnored
      temp.delete();
      throw new IOException("Could not write custom font file.");
    }

    synchronized (CUSTOM_FONT_CACHE_LOCK) {
      customFontCache.remove(target.getAbsolutePath());
    }

    final SharedPreferences.Editor editor = prefs.edit();
    if (displayName != null && !displayName.trim().isEmpty()) {
      editor.putString(
          KeyboardThemeUserOverridesTypographyPrefs.keyCustomFontNameKey(themeId),
          displayName.trim());
    } else {
      editor.remove(KeyboardThemeUserOverridesTypographyPrefs.keyCustomFontNameKey(themeId));
    }
    KeyboardThemeUserOverridesColorsPrefs.markChanged(prefs, themeId, editor);
    editor.apply();
  }

  void importCustomKeyFontFromFile(
      @NonNull String themeId, @NonNull File sourceFile, @Nullable String displayName)
      throws IOException {
    if (!sourceFile.isFile()) {
      throw new IOException("Custom font file does not exist: " + sourceFile);
    }

    final File target = getCustomKeyFontFile(themeId);
    final File parent = target.getParentFile();
    if (parent != null && !parent.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      parent.mkdirs();
    }

    final File temp =
        File.createTempFile(
            "nsk_custom_font", ".tmp", parent != null ? parent : appContext.getCacheDir());
    try (InputStream in = new FileInputStream(sourceFile);
        FileOutputStream out = new FileOutputStream(temp)) {
      copyWithLimit(in, out, MAX_CUSTOM_FONT_BYTES);
      validateCustomFontFileOrThrow(temp);
    } catch (IOException e) {
      //noinspection ResultOfMethodCallIgnored
      temp.delete();
      throw e;
    }

    //noinspection ResultOfMethodCallIgnored
    target.delete();
    if (!temp.renameTo(target)) {
      //noinspection ResultOfMethodCallIgnored
      temp.delete();
      throw new IOException("Could not write custom font file.");
    }

    synchronized (CUSTOM_FONT_CACHE_LOCK) {
      customFontCache.remove(target.getAbsolutePath());
    }

    final SharedPreferences.Editor editor = prefs.edit();
    if (displayName != null && !displayName.trim().isEmpty()) {
      editor.putString(
          KeyboardThemeUserOverridesTypographyPrefs.keyCustomFontNameKey(themeId),
          displayName.trim());
    } else {
      editor.remove(KeyboardThemeUserOverridesTypographyPrefs.keyCustomFontNameKey(themeId));
    }
    KeyboardThemeUserOverridesColorsPrefs.markChanged(prefs, themeId, editor);
    editor.apply();
  }

  void clearCustomKeyFont(@NonNull String themeId) {
    final File file = getCustomKeyFontFile(themeId);
    //noinspection ResultOfMethodCallIgnored
    file.delete();
    synchronized (CUSTOM_FONT_CACHE_LOCK) {
      customFontCache.remove(file.getAbsolutePath());
    }

    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(KeyboardThemeUserOverridesTypographyPrefs.keyCustomFontNameKey(themeId));
    KeyboardThemeUserOverridesColorsPrefs.markChanged(prefs, themeId, editor);
    editor.apply();
  }

  boolean canLoadCustomKeyFont(@NonNull String themeId) {
    if (!UserManagerCompat.isUserUnlocked(appContext)) return true;

    final File file = getCustomKeyFontFile(themeId);
    if (!file.isFile()) return false;

    try {
      return Typeface.createFromFile(file) != null;
    } catch (RuntimeException e) {
      return false;
    }
  }

  void clearCustomKeyFontFileNoChange(@NonNull String themeId) {
    final File file = getCustomKeyFontFile(themeId);
    //noinspection ResultOfMethodCallIgnored
    file.delete();
    customFontCache.remove(file.getAbsolutePath());
  }

  void copyCustomKeyFontFile(@NonNull String sourceThemeId, @NonNull String targetThemeId) {
    final File source = getCustomKeyFontFile(sourceThemeId);
    final File target = getCustomKeyFontFile(targetThemeId);
    if (!source.isFile()) {
      //noinspection ResultOfMethodCallIgnored
      target.delete();
      customFontCache.remove(target.getAbsolutePath());
      return;
    }

    final File parent = target.getParentFile();
    if (parent != null && !parent.isDirectory()) {
      //noinspection ResultOfMethodCallIgnored
      parent.mkdirs();
    }

    try (InputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(target)) {
      copyWithLimit(in, out, MAX_CUSTOM_FONT_BYTES);
      customFontCache.remove(target.getAbsolutePath());
    } catch (IOException e) {
      // Best-effort: leave target without a custom font.
      //noinspection ResultOfMethodCallIgnored
      target.delete();
      customFontCache.remove(target.getAbsolutePath());
    }
  }

  private static void validateCustomFontFileOrThrow(@NonNull File file) throws IOException {
    try (FileInputStream in = new FileInputStream(file)) {
      final byte[] header = new byte[4];
      if (in.read(header) != 4) throw new IOException("Could not read custom font file header.");

      final boolean isTrueType =
          (header[0] == 0x00 && header[1] == 0x01 && header[2] == 0x00 && header[3] == 0x00)
              || (header[0] == 't' && header[1] == 'r' && header[2] == 'u' && header[3] == 'e')
              || (header[0] == 't' && header[1] == 'y' && header[2] == 'p' && header[3] == '1');
      final boolean isOpenType =
          header[0] == 'O' && header[1] == 'T' && header[2] == 'T' && header[3] == 'O';
      final boolean isCollection =
          header[0] == 't' && header[1] == 't' && header[2] == 'c' && header[3] == 'f';
      if (!isTrueType && !isOpenType && !isCollection) {
        throw new IOException("Not a supported font file.");
      }
    }
  }

  private static void copyWithLimit(
      @NonNull InputStream in, @NonNull FileOutputStream out, long maxBytes) throws IOException {
    final byte[] buffer = new byte[8192];
    long total = 0L;
    int read;
    while ((read = in.read(buffer)) != -1) {
      total += read;
      if (total > maxBytes) throw new IOException("File too large.");
      out.write(buffer, 0, read);
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
      return Integer.toHexString(raw.hashCode());
    }
  }
}
