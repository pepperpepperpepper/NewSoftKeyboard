package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.DirectoryPackSource;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPackValidator;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifestJson;

public final class KeyboardPacksInstaller {
  private static final String TAG = "KeyboardPacksInstaller";

  private static final int MAX_ZIP_ENTRIES = 2_000;
  private static final long MAX_UNCOMPRESSED_BYTES = 50L * 1024L * 1024L; // 50 MiB
  private static final int IO_BUFFER_BYTES = 8 * 1024;

  @NonNull private final Context applicationContext;
  @NonNull private final KeyboardPacksRepository repository;

  public KeyboardPacksInstaller(@NonNull Context context) {
    applicationContext = Objects.requireNonNull(context).getApplicationContext();
    repository = new KeyboardPacksRepository(applicationContext);
  }

  @NonNull
  public InstalledKeyboardPack installPackZip(@NonNull InputStream zipInputStream)
      throws IOException {
    File packsRoot = repository.packsRootDir();
    File tempDir = createTempDir();
    File extractedRoot = tempDir;
    try {
      unpackZip(zipInputStream, tempDir);
      extractedRoot = locatePackRoot(tempDir);

      PackManifest manifest =
          readManifest(new File(extractedRoot, KeyboardPacksRepository.MANIFEST_FILE_NAME));
      validatePack(extractedRoot, manifest);

      File destDir = new File(packsRoot, sanitizeDirName(manifest.id()));
      replaceDirectory(destDir, extractedRoot);
      return new InstalledKeyboardPack(destDir, manifest);
    } finally {
      if (!deleteRecursively(tempDir)) {
        Logger.w(TAG, "Failed to delete temp install dir %s", tempDir);
      }
    }
  }

  public void exportPackZip(@NonNull InstalledKeyboardPack pack, @NonNull OutputStream outputStream)
      throws IOException {
    File packDir = pack.directory();
    if (!packDir.exists() || !packDir.isDirectory()) {
      throw new IOException("Pack directory not found: " + packDir);
    }

    String canonicalRoot = packDir.getCanonicalPath();
    if (!canonicalRoot.endsWith(File.separator)) canonicalRoot += File.separator;

    try (ZipOutputStream zipOut =
        new ZipOutputStream(new BufferedOutputStream(outputStream, IO_BUFFER_BYTES))) {
      addDirToZip(zipOut, packDir, canonicalRoot);
    }
  }

  private static void validatePack(@NonNull File packDir, @NonNull PackManifest manifest)
      throws IOException {
    try {
      KeyboardPack pack = new KeyboardPack(manifest, new DirectoryPackSource(packDir));
      KeyboardPackValidator.ValidationResult validation = KeyboardPackValidator.validate(pack);
      if (!validation.isValid()) {
        StringBuilder builder = new StringBuilder("Pack validation failed:");
        for (String error : validation.errors()) {
          builder.append("\n- ").append(error);
        }
        throw new IOException(builder.toString());
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Pack validation failed: " + e.getMessage(), e);
    }
  }

  private static PackManifest readManifest(@NonNull File manifestFile) throws IOException {
    try (InputStream in =
        new BufferedInputStream(new FileInputStream(manifestFile), IO_BUFFER_BYTES)) {
      return PackManifestJson.parse(in);
    }
  }

  private File createTempDir() throws IOException {
    File base = applicationContext.getCacheDir();
    File dir = new File(base, "keyboard_pack_install_" + System.currentTimeMillis());
    if (!dir.mkdirs()) {
      throw new IOException("Failed creating temp dir at " + dir);
    }
    return dir;
  }

  private static void unpackZip(@NonNull InputStream zipInputStream, @NonNull File destinationDir)
      throws IOException {
    String canonicalRoot = destinationDir.getCanonicalPath();
    if (!canonicalRoot.endsWith(File.separator)) canonicalRoot += File.separator;

    int entries = 0;
    long totalBytes = 0;
    byte[] buffer = new byte[IO_BUFFER_BYTES];

    try (ZipInputStream zipIn =
        new ZipInputStream(new BufferedInputStream(zipInputStream, IO_BUFFER_BYTES))) {
      ZipEntry entry;
      while ((entry = zipIn.getNextEntry()) != null) {
        if (++entries > MAX_ZIP_ENTRIES) {
          throw new IOException("Too many files in zip (max " + MAX_ZIP_ENTRIES + ")");
        }

        String name = entry.getName();
        if (name == null) continue;
        name = name.replace('\\', '/');
        while (name.startsWith("/")) name = name.substring(1);
        if (name.isEmpty()) continue;

        File outFile = new File(destinationDir, name);
        File canonicalOut = outFile.getCanonicalFile();
        if (!canonicalOut.getPath().startsWith(canonicalRoot)) {
          throw new IOException("Zip entry escapes destination: " + entry.getName());
        }

        if (entry.isDirectory()) {
          if (!canonicalOut.exists() && !canonicalOut.mkdirs()) {
            throw new IOException("Failed creating directory " + canonicalOut);
          }
          continue;
        }

        File parent = canonicalOut.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
          throw new IOException("Failed creating directory " + parent);
        }

        try (OutputStream out =
            new BufferedOutputStream(new FileOutputStream(canonicalOut), IO_BUFFER_BYTES)) {
          int read;
          while ((read = zipIn.read(buffer)) != -1) {
            totalBytes += read;
            if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
              throw new IOException("Zip is too large (max " + MAX_UNCOMPRESSED_BYTES + " bytes)");
            }
            out.write(buffer, 0, read);
          }
        }
      }
    }
  }

  private static File locatePackRoot(@NonNull File extractedDir) throws IOException {
    File directManifest = new File(extractedDir, KeyboardPacksRepository.MANIFEST_FILE_NAME);
    if (directManifest.exists() && directManifest.isFile()) return extractedDir;

    File[] dirs = extractedDir.listFiles(File::isDirectory);
    if (dirs != null && dirs.length == 1) {
      File nestedManifest = new File(dirs[0], KeyboardPacksRepository.MANIFEST_FILE_NAME);
      if (nestedManifest.exists() && nestedManifest.isFile()) return dirs[0];
    }

    throw new IOException(
        "Could not find "
            + KeyboardPacksRepository.MANIFEST_FILE_NAME
            + " at zip root (or within a single nested directory)");
  }

  private static String sanitizeDirName(@NonNull String raw) {
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return "pack";

    StringBuilder builder = new StringBuilder(trimmed.length());
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      boolean allowed =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '.'
              || c == '_'
              || c == '-';
      builder.append(allowed ? c : '_');
    }

    String result = builder.toString().toLowerCase(Locale.ROOT);
    if (result.isEmpty()) return "pack";
    return result;
  }

  private static void replaceDirectory(@NonNull File destination, @NonNull File source)
      throws IOException {
    if (destination.exists()) {
      if (!deleteRecursively(destination)) {
        throw new IOException("Failed deleting existing pack dir at " + destination);
      }
    }
    copyRecursively(source, destination);
  }

  private static void copyRecursively(@NonNull File source, @NonNull File destination)
      throws IOException {
    if (source.isDirectory()) {
      if (!destination.exists() && !destination.mkdirs()) {
        throw new IOException("Failed creating directory " + destination);
      }
      File[] children = source.listFiles();
      if (children == null) return;
      for (File child : children) {
        copyRecursively(child, new File(destination, child.getName()));
      }
    } else {
      File parent = destination.getParentFile();
      if (parent != null && !parent.exists() && !parent.mkdirs()) {
        throw new IOException("Failed creating directory " + parent);
      }
      try (InputStream in = new BufferedInputStream(new FileInputStream(source), IO_BUFFER_BYTES);
          OutputStream out =
              new BufferedOutputStream(new FileOutputStream(destination), IO_BUFFER_BYTES)) {
        byte[] buffer = new byte[IO_BUFFER_BYTES];
        int read;
        while ((read = in.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
      }
    }
  }

  private static boolean deleteRecursively(@NonNull File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          if (!deleteRecursively(child)) return false;
        }
      }
    }
    return file.delete();
  }

  private static void addDirToZip(
      @NonNull ZipOutputStream zipOut, @NonNull File dir, @NonNull String canonicalRoot)
      throws IOException {
    File[] children = dir.listFiles();
    if (children == null) return;

    for (File child : children) {
      if (child.isDirectory()) {
        addDirToZip(zipOut, child, canonicalRoot);
        continue;
      }

      String canonical = child.getCanonicalPath();
      if (!canonical.startsWith(canonicalRoot)) {
        throw new IOException("File escapes pack root: " + child);
      }
      String relative =
          canonical.substring(canonicalRoot.length()).replace(File.separatorChar, '/');
      if (relative.isEmpty() || relative.startsWith("/")) {
        throw new IOException("Invalid relative path for zip: " + relative);
      }

      ZipEntry entry = new ZipEntry(relative);
      zipOut.putNextEntry(entry);
      try (InputStream in = new BufferedInputStream(new FileInputStream(child), IO_BUFFER_BYTES)) {
        byte[] buffer = new byte[IO_BUFFER_BYTES];
        int read;
        while ((read = in.read(buffer)) != -1) {
          zipOut.write(buffer, 0, read);
        }
      }
      zipOut.closeEntry();
    }
  }
}
