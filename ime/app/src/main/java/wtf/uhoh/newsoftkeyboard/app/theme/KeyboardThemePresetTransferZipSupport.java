package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class KeyboardThemePresetTransferZipSupport {

  static final int MAX_MANIFEST_BYTES = 256 * 1024;
  static final long MAX_PREVIEW_BYTES = 2L * 1024L * 1024L;
  static final long MAX_KEY_FONT_BYTES = 10L * 1024L * 1024L;

  static final int MAX_ARCHIVE_ENTRIES = 32;
  static final long MAX_IGNORED_ENTRY_BYTES = 64L * 1024L;

  private static final OutputStream NOOP_OUTPUT_STREAM =
      new OutputStream() {
        @Override
        public void write(int b) {}

        @Override
        public void write(byte[] b, int off, int len) {}
      };

  private KeyboardThemePresetTransferZipSupport() {}

  @Nullable
  static String readZipEntryString(
      @NonNull InputStream inputStream, @NonNull String entryName, int maxBytes)
      throws IOException {
    try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(inputStream))) {
      ZipEntry entry;
      int entriesSeen = 0;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        entriesSeen++;
        if (entriesSeen > MAX_ARCHIVE_ENTRIES) {
          throw new IOException("Too many entries in preset archive.");
        }

        if (entryName.equals(entry.getName())) {
          final String text = readEntryToString(zip, maxBytes);
          zip.closeEntry();
          return text;
        }

        copy(zip, NOOP_OUTPUT_STREAM, MAX_IGNORED_ENTRY_BYTES);
        zip.closeEntry();
      }
    }
    return null;
  }

  @NonNull
  static String readEntryToString(@NonNull ZipInputStream zip, int maxBytes) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(zip, out, maxBytes);
    return out.toString(StandardCharsets.UTF_8.name());
  }

  @Nullable
  static byte[] readZipEntryBytes(
      @NonNull InputStream inputStream, @NonNull String entryName, long maxBytes)
      throws IOException {
    try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(inputStream))) {
      ZipEntry entry;
      int entriesSeen = 0;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        entriesSeen++;
        if (entriesSeen > MAX_ARCHIVE_ENTRIES) {
          throw new IOException("Too many entries in preset archive.");
        }

        final String name = entry.getName();
        if (entryName.equals(name)) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream();
          copy(zip, out, maxBytes);
          zip.closeEntry();
          return out.toByteArray();
        }

        final long maxSkipBytes;
        if (KeyboardThemePresetArchiveManifestCodec.ENTRY_MANIFEST.equals(name)) {
          maxSkipBytes = MAX_MANIFEST_BYTES;
        } else if (KeyboardThemePresetArchiveManifestCodec.ENTRY_PREVIEW.equals(name)) {
          maxSkipBytes = MAX_PREVIEW_BYTES;
        } else if (KeyboardThemePresetArchiveManifestCodec.ENTRY_KEY_FONT.equals(name)) {
          maxSkipBytes = MAX_KEY_FONT_BYTES;
        } else if (KeyboardThemePresetArchiveWallpaper.ENTRY_WALLPAPER.equals(name)) {
          maxSkipBytes = KeyboardThemePresetArchiveWallpaper.MAX_WALLPAPER_BYTES;
        } else {
          maxSkipBytes = MAX_IGNORED_ENTRY_BYTES;
        }
        copy(zip, NOOP_OUTPUT_STREAM, maxSkipBytes);
        zip.closeEntry();
      }
    }
    return null;
  }

  static void copy(@NonNull InputStream in, @NonNull OutputStream out, long maxBytes)
      throws IOException {
    final byte[] buffer = new byte[8192];
    long copied = 0L;
    while (true) {
      final int read = in.read(buffer);
      if (read < 0) break;
      copied += read;
      if (copied > maxBytes) {
        throw new IOException("Entry is too large (" + copied + " bytes).");
      }
      out.write(buffer, 0, read);
    }
  }

  static void skip(@NonNull InputStream in, long maxBytes) throws IOException {
    copy(in, NOOP_OUTPUT_STREAM, maxBytes);
  }
}
