package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

public final class KeyboardThemePresetTransfer {

  public static final String MIME_TYPE_ZIP = "application/zip";

  private KeyboardThemePresetTransfer() {}

  public static final class PresetArchiveInfo {
    private final int archiveVersion;
    @NonNull private final String baseThemeId;
    @Nullable private final String baseThemeName;
    @NonNull private final String presetName;
    private final long exportedAtMillis;
    @Nullable private final String exportedByVersionName;
    private final boolean hasWallpaper;
    private final boolean hasColors;
    private final boolean hasTypography;
    private final boolean hasShadows;
    private final boolean hasPreviewImage;
    private final boolean hasCustomFont;
    @Nullable private final String customFontName;

    PresetArchiveInfo(
        int archiveVersion,
        @NonNull String baseThemeId,
        @Nullable String baseThemeName,
        @NonNull String presetName,
        long exportedAtMillis,
        @Nullable String exportedByVersionName,
        boolean hasWallpaper,
        boolean hasColors,
        boolean hasTypography,
        boolean hasShadows,
        boolean hasPreviewImage,
        boolean hasCustomFont,
        @Nullable String customFontName) {
      this.archiveVersion = archiveVersion;
      this.baseThemeId = baseThemeId;
      this.baseThemeName = baseThemeName;
      this.presetName = presetName;
      this.exportedAtMillis = exportedAtMillis;
      this.exportedByVersionName = exportedByVersionName;
      this.hasWallpaper = hasWallpaper;
      this.hasColors = hasColors;
      this.hasTypography = hasTypography;
      this.hasShadows = hasShadows;
      this.hasPreviewImage = hasPreviewImage;
      this.hasCustomFont = hasCustomFont;
      this.customFontName = customFontName;
    }

    public int archiveVersion() {
      return archiveVersion;
    }

    @NonNull
    public String baseThemeId() {
      return baseThemeId;
    }

    @Nullable
    public String baseThemeName() {
      return baseThemeName;
    }

    @NonNull
    public String presetName() {
      return presetName;
    }

    public long exportedAtMillis() {
      return exportedAtMillis;
    }

    @Nullable
    public String exportedByVersionName() {
      return exportedByVersionName;
    }

    public boolean hasWallpaper() {
      return hasWallpaper;
    }

    public boolean hasColors() {
      return hasColors;
    }

    public boolean hasTypography() {
      return hasTypography;
    }

    public boolean hasShadows() {
      return hasShadows;
    }

    public boolean hasPreviewImage() {
      return hasPreviewImage;
    }

    public boolean hasCustomFont() {
      return hasCustomFont;
    }

    @Nullable
    public String customFontName() {
      return customFontName;
    }
  }

  @NonNull
  public static PresetArchiveInfo readArchiveInfo(@NonNull InputStream inputStream)
      throws IOException {
    final String manifestText =
        KeyboardThemePresetTransferZipSupport.readZipEntryString(
            inputStream,
            KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST,
            KeyboardThemePresetTransferZipSupport.MAX_MANIFEST_BYTES);
    if (manifestText == null) {
      throw new IOException(
          "Missing "
              + KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST
              + " in preset archive.");
    }

    final JSONObject manifest = KeyboardThemePresetArchiveManifestCodec.parseManifest(manifestText);
    return KeyboardThemePresetArchiveManifestCodec.readArchiveInfo(manifest);
  }

  public static void exportPreset(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      @NonNull OutputStream outputStream)
      throws IOException {
    exportPreset(context, baseThemeId, presetId, outputStream, true);
  }

  public static void exportPreset(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      @NonNull OutputStream outputStream,
      boolean includeWallpaper)
      throws IOException {
    exportPreset(context, baseThemeId, presetId, outputStream, includeWallpaper, null);
  }

  public static void exportPreset(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      @NonNull OutputStream outputStream,
      boolean includeWallpaper,
      @Nullable byte[] previewPngBytes)
      throws IOException {
    final Context appContext = context.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(appContext);
    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(appContext);
    final KeyboardWallpaperOverrideStore wallpaperStore =
        new KeyboardWallpaperOverrideStore(appContext);

    final String resolvedName;
    final String storedName = presetStore.getPresetName(presetId);
    if (storedName != null && !storedName.trim().isEmpty()) {
      resolvedName = storedName.trim();
    } else if (presetId.equals(baseThemeId)) {
      resolvedName = "Default";
    } else {
      resolvedName = "Preset";
    }

    final JSONObject manifest = new JSONObject();
    final boolean presetHasWallpaper =
        wallpaperStore.hasWallpaper(presetId) && !wallpaperStore.isWallpaperInvalid(presetId);
    final boolean hasWallpaper = includeWallpaper && presetHasWallpaper;
    final boolean hasPreviewBytes =
        previewPngBytes != null
            && previewPngBytes.length > 0
            && previewPngBytes.length <= KeyboardThemePresetTransferZipSupport.MAX_PREVIEW_BYTES;
    final boolean includePreview = hasPreviewBytes && (includeWallpaper || !presetHasWallpaper);
    boolean usesCustomFont = false;
    try {
      final String baseThemeName = resolveBaseThemeNameOrNull(appContext, baseThemeId);
      KeyboardThemePresetArchiveManifestCodec.writeExportMetadataToManifest(
          manifest,
          System.currentTimeMillis(),
          BuildConfig.APPLICATION_ID,
          BuildConfig.VERSION_NAME,
          BuildConfig.VERSION_CODE,
          baseThemeId,
          baseThemeName,
          resolvedName);

      usesCustomFont =
          KeyboardThemePresetArchiveOverrides.writeToManifest(manifest, overridesStore, presetId);

      KeyboardThemePresetArchiveWallpaper.writeToManifest(
          manifest, wallpaperStore, presetId, hasWallpaper);

      KeyboardThemePresetArchiveManifestCodec.writePreviewToManifest(
          manifest, includePreview, previewPngBytes);
    } catch (JSONException e) {
      throw new IOException("Failed to build preset manifest.", e);
    }

    try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
      zip.putNextEntry(new ZipEntry(KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST));
      final String manifestText;
      try {
        manifestText = manifest.toString(2);
      } catch (JSONException e) {
        throw new IOException("Failed to serialize preset manifest.", e);
      }
      zip.write(manifestText.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();

      if (includePreview) {
        final byte[] bytes = previewPngBytes;
        if (bytes != null
            && bytes.length > 0
            && bytes.length <= KeyboardThemePresetTransferZipSupport.MAX_PREVIEW_BYTES) {
          zip.putNextEntry(new ZipEntry(KeyboardThemePresetArchiveManifestCodec.ENTRY_PREVIEW));
          zip.write(bytes);
          zip.closeEntry();
        }
      }

      if (usesCustomFont) {
        final File fontFile = overridesStore.getCustomKeyFontFile(presetId);
        if (!fontFile.isFile()) throw new IOException("Missing custom key font file.");
        zip.putNextEntry(new ZipEntry(KeyboardThemePresetArchiveManifestCodec.ENTRY_KEY_FONT));
        try (InputStream in = new FileInputStream(fontFile)) {
          copy(in, zip, KeyboardThemePresetTransferZipSupport.MAX_KEY_FONT_BYTES);
        }
        zip.closeEntry();
      }

      if (hasWallpaper) {
        final File wallpaperFile = wallpaperStore.getWallpaperFile(presetId);
        if (wallpaperFile.isFile()) {
          zip.putNextEntry(new ZipEntry(KeyboardThemePresetArchiveWallpaper.ENTRY_WALLPAPER));
          try (InputStream in = new FileInputStream(wallpaperFile)) {
            copy(in, zip, KeyboardThemePresetArchiveWallpaper.MAX_WALLPAPER_BYTES);
          }
          zip.closeEntry();
        }
      }
    }
  }

  @NonNull
  public static ImportedPreset importPreset(
      @NonNull Context context,
      @NonNull String expectedBaseThemeId,
      @NonNull InputStream inputStream)
      throws IOException {
    return importPreset(context, expectedBaseThemeId, inputStream, null);
  }

  @NonNull
  public static ImportedPreset importPreset(
      @NonNull Context context,
      @NonNull String expectedBaseThemeId,
      @NonNull InputStream inputStream,
      @Nullable String requestedNameOverride)
      throws IOException {
    final Context appContext = context.getApplicationContext();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(appContext);
    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(appContext);
    final KeyboardWallpaperOverrideStore wallpaperStore =
        new KeyboardWallpaperOverrideStore(appContext);

    @Nullable JSONObject manifest = null;
    @Nullable File tempWallpaper = null;
    @Nullable File tempKeyFont = null;
    @Nullable String createdPresetId = null;

    try {
      boolean expectsWallpaper = false;
      boolean expectsKeyFont = false;

      try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(inputStream))) {
        ZipEntry entry;
        int entriesSeen = 0;
        while ((entry = zip.getNextEntry()) != null) {
          if (entry.isDirectory()) continue;
          entriesSeen++;
          if (entriesSeen > KeyboardThemePresetTransferZipSupport.MAX_ARCHIVE_ENTRIES) {
            throw new IOException("Too many entries in preset archive.");
          }

          final String name = entry.getName();
          if (manifest == null) {
            if (KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST.equals(name)) {
              manifest =
                  KeyboardThemePresetArchiveManifestCodec.parseManifest(
                      KeyboardThemePresetTransferZipSupport.readEntryToString(
                          zip, KeyboardThemePresetTransferZipSupport.MAX_MANIFEST_BYTES));
              KeyboardThemePresetArchiveManifestCodec.validateForImport(
                  manifest, expectedBaseThemeId);
              expectsKeyFont = KeyboardThemePresetArchiveManifestCodec.expectsKeyFont(manifest);
              expectsWallpaper = KeyboardThemePresetArchiveManifestCodec.expectsWallpaper(manifest);
            } else {
              KeyboardThemePresetTransferZipSupport.skip(
                  zip, KeyboardThemePresetTransferZipSupport.MAX_IGNORED_ENTRY_BYTES);
            }
            zip.closeEntry();
            continue;
          }

          if (KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST.equals(name)) {
            throw new IOException(
                "Duplicate "
                    + KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST
                    + " in preset archive.");
          } else if (KeyboardThemePresetArchiveWallpaper.ENTRY_WALLPAPER.equals(name)) {
            if (!expectsWallpaper) {
              throw new IOException("Unexpected wallpaper entry in preset archive.");
            }
            if (tempWallpaper != null) {
              throw new IOException("Duplicate wallpaper entry in preset archive.");
            }
            tempWallpaper =
                File.createTempFile("nsk_preset_wallpaper", ".tmp", appContext.getCacheDir());
            try (OutputStream out = new FileOutputStream(tempWallpaper)) {
              copy(zip, out, KeyboardThemePresetArchiveWallpaper.MAX_WALLPAPER_BYTES);
            }
          } else if (KeyboardThemePresetArchiveManifestCodec.ENTRY_KEY_FONT.equals(name)) {
            if (!expectsKeyFont) {
              throw new IOException("Unexpected key font entry in preset archive.");
            }
            if (tempKeyFont != null) {
              throw new IOException("Duplicate key font entry in preset archive.");
            }
            tempKeyFont = File.createTempFile("nsk_preset_font", ".tmp", appContext.getCacheDir());
            try (OutputStream out = new FileOutputStream(tempKeyFont)) {
              copy(zip, out, KeyboardThemePresetTransferZipSupport.MAX_KEY_FONT_BYTES);
            }
          } else if (KeyboardThemePresetArchiveManifestCodec.ENTRY_PREVIEW.equals(name)) {
            KeyboardThemePresetTransferZipSupport.skip(
                zip, KeyboardThemePresetTransferZipSupport.MAX_PREVIEW_BYTES);
          } else {
            KeyboardThemePresetTransferZipSupport.skip(
                zip, KeyboardThemePresetTransferZipSupport.MAX_IGNORED_ENTRY_BYTES);
          }
          zip.closeEntry();
        }
      }

      if (manifest == null) {
        throw new IOException(
            "Missing "
                + KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST
                + " in preset archive.");
      }

      final String requestedName =
          KeyboardThemePresetArchiveManifestCodec.resolveRequestedNameForImport(
              manifest, requestedNameOverride);

      final String uniqueName =
          makeUniqueImportedName(presetStore, expectedBaseThemeId, requestedName);
      final KeyboardThemePresetStore.Preset created =
          presetStore.createPreset(expectedBaseThemeId, uniqueName);
      createdPresetId = created.id();

      KeyboardThemePresetArchiveOverrides.applyFromManifest(
          manifest, overridesStore, createdPresetId, tempKeyFont);

      KeyboardThemePresetArchiveWallpaper.applyFromManifest(
          appContext, wallpaperStore, createdPresetId, manifest, tempWallpaper);

      return new ImportedPreset(createdPresetId, uniqueName);
    } catch (Exception e) {
      if (createdPresetId != null) {
        presetStore.deletePreset(createdPresetId);
        overridesStore.clearAllOverrides(createdPresetId);
        wallpaperStore.clear(createdPresetId);
      }
      if (e instanceof IOException) throw (IOException) e;
      throw new IOException("Failed to import preset.", e);
    } finally {
      deleteQuietly(tempWallpaper);
      deleteQuietly(tempKeyFont);
    }
  }

  public static final class ImportedPreset {
    @NonNull private final String presetId;
    @NonNull private final String presetName;

    public ImportedPreset(@NonNull String presetId, @NonNull String presetName) {
      this.presetId = presetId;
      this.presetName = presetName;
    }

    @NonNull
    public String presetId() {
      return presetId;
    }

    @NonNull
    public String presetName() {
      return presetName;
    }
  }

  @Nullable
  public static Bitmap readPreviewBitmap(@NonNull InputStream inputStream) throws IOException {
    @Nullable
    final byte[] pngBytes =
        KeyboardThemePresetTransferZipSupport.readZipEntryBytes(
            inputStream,
            KeyboardThemePresetArchiveManifestCodec.ENTRY_PREVIEW,
            KeyboardThemePresetTransferZipSupport.MAX_PREVIEW_BYTES);
    if (pngBytes == null || pngBytes.length == 0) return null;

    final BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

    final int maxPx = 720;
    final int sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxPx, maxPx);
    final BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inSampleSize = sampleSize;
    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
    return BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, opts);
  }

  @Nullable
  public static byte[] readPreviewPngBytes(@NonNull InputStream inputStream) throws IOException {
    return KeyboardThemePresetTransferZipSupport.readZipEntryBytes(
        inputStream,
        KeyboardThemePresetArchiveManifestCodec.ENTRY_PREVIEW,
        KeyboardThemePresetTransferZipSupport.MAX_PREVIEW_BYTES);
  }

  private static int computeInSampleSize(int srcWidth, int srcHeight, int reqWidth, int reqHeight) {
    int inSampleSize = 1;
    if (srcHeight > reqHeight || srcWidth > reqWidth) {
      final int halfHeight = srcHeight / 2;
      final int halfWidth = srcWidth / 2;
      while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }
    return Math.max(1, inSampleSize);
  }

  @Nullable
  private static String resolveBaseThemeNameOrNull(
      @NonNull Context context, @NonNull String baseThemeId) {
    try {
      final KeyboardTheme theme =
          NskApplicationBase.getKeyboardThemeFactory(context).getAddOnById(baseThemeId);
      final CharSequence name = theme != null ? theme.getName() : null;
      if (name == null) return null;
      final String trimmed = String.valueOf(name).trim();
      return trimmed.isEmpty() ? null : trimmed;
    } catch (Exception ignored) {
      return null;
    }
  }

  private static void deleteQuietly(@Nullable File file) {
    if (file == null) return;
    //noinspection ResultOfMethodCallIgnored
    file.delete();
  }

  static void copy(@NonNull InputStream in, @NonNull OutputStream out, long maxBytes)
      throws IOException {
    KeyboardThemePresetTransferZipSupport.copy(in, out, maxBytes);
  }

  @NonNull
  private static String makeUniqueImportedName(
      @NonNull KeyboardThemePresetStore presetStore,
      @NonNull String baseThemeId,
      @NonNull String requestedName) {
    final Set<String> existing = new HashSet<>();
    final List<KeyboardThemePresetStore.Preset> presets = presetStore.listPresets(baseThemeId);
    for (KeyboardThemePresetStore.Preset preset : presets) {
      final String name = preset.isDefault() ? "Default" : preset.name();
      existing.add(normalizeName(name));
    }

    final String base = requestedName.trim();
    if (base.isEmpty()) return "Imported preset";

    final String normalizedBase = normalizeName(base);
    if (!existing.contains(normalizedBase)) return base;

    final String suffixed = base + " (Imported)";
    if (!existing.contains(normalizeName(suffixed))) return suffixed;

    int index = 2;
    while (true) {
      final String candidate = base + " (Imported " + index + ")";
      if (!existing.contains(normalizeName(candidate))) return candidate;
      index++;
    }
  }

  @NonNull
  private static String normalizeName(@NonNull String name) {
    return name.trim().toLowerCase(Locale.US);
  }
}
