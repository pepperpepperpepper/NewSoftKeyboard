package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.DirectoryPackSource;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPackValidator;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;

public final class CustomKeyboardPackCreator {
  private static final int IO_BUFFER_BYTES = 8 * 1024;

  private static final String TEMPLATES_ROOT = "keyboard_designer_templates";
  private static final String BASIC_QWERTY_TEMPLATE_ID = "basic_qwerty";
  private static final String BASIC_QWERTY_MAIN_KEYBOARD_PATH =
      TEMPLATES_ROOT + "/" + BASIC_QWERTY_TEMPLATE_ID + "/keyboards/main.xml";

  private static final String DEFAULT_KEYBOARD_ENTRY_ID = "main";
  private static final String DEFAULT_KEYBOARD_FILE_PATH = "keyboards/main.xml";

  private CustomKeyboardPackCreator() {}

  @NonNull
  public static InstalledKeyboardPack createBasicQwertyKeyboardPack(
      @NonNull Context context, @NonNull String keyboardName) throws IOException {
    final Context appContext = Objects.requireNonNull(context).getApplicationContext();

    KeyboardPacksRepository repository = new KeyboardPacksRepository(appContext);
    File packsRoot = repository.packsRootDir();

    final String packId = generateUniquePackId(packsRoot, keyboardName);
    final File packDir = new File(packsRoot, packId);
    final File keyboardDir = new File(packDir, "keyboards");
    final File keyboardXml = new File(keyboardDir, "main.xml");
    final File manifestFile = new File(packDir, "manifest.json");

    if (!keyboardDir.mkdirs()) {
      throw new IOException("Failed creating keyboard dir at " + keyboardDir);
    }

    copyAsset(appContext.getAssets(), BASIC_QWERTY_MAIN_KEYBOARD_PATH, keyboardXml);

    PackEntry entry =
        new PackEntry(DEFAULT_KEYBOARD_ENTRY_ID, PackPath.parse(DEFAULT_KEYBOARD_FILE_PATH));
    PackManifest manifest =
        new PackManifest(
            PackManifest.SUPPORTED_SCHEMA_VERSION,
            packId,
            Objects.requireNonNull(keyboardName).trim(),
            1,
            null,
            Collections.singletonList(entry),
            Collections.emptyList());
    writeManifest(manifestFile, manifest);

    KeyboardPackValidator.ValidationResult validation =
        KeyboardPackValidator.validate(
            new KeyboardPack(manifest, new DirectoryPackSource(packDir)));
    if (!validation.isValid()) {
      deleteRecursively(packDir);
      StringBuilder builder = new StringBuilder("Pack validation failed:");
      for (String error : validation.errors()) {
        builder.append("\n- ").append(error);
      }
      throw new IOException(builder.toString());
    }

    return new InstalledKeyboardPack(packDir, manifest);
  }

  private static void copyAsset(
      @NonNull AssetManager assets, @NonNull String assetPath, @NonNull File outFile)
      throws IOException {
    File parent = outFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed creating directory " + parent);
    }

    try (InputStream in = new BufferedInputStream(assets.open(assetPath), IO_BUFFER_BYTES);
        OutputStream out =
            new BufferedOutputStream(new FileOutputStream(outFile), IO_BUFFER_BYTES)) {
      byte[] buffer = new byte[IO_BUFFER_BYTES];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
    }
  }

  private static void writeManifest(@NonNull File manifestFile, @NonNull PackManifest manifest)
      throws IOException {
    String json = toManifestJson(manifest);
    try (OutputStream out =
        new BufferedOutputStream(new FileOutputStream(manifestFile), IO_BUFFER_BYTES)) {
      out.write(json.getBytes(StandardCharsets.UTF_8));
    }
  }

  @NonNull
  private static String toManifestJson(@NonNull PackManifest manifest) {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n");
    builder.append("  \"schemaVersion\": ").append(manifest.schemaVersion()).append(",\n");
    builder.append("  \"id\": \"").append(jsonEscape(manifest.id())).append("\",\n");
    builder.append("  \"name\": \"").append(jsonEscape(manifest.name())).append("\",\n");
    builder.append("  \"version\": ").append(manifest.version()).append(",\n");
    builder.append("  \"keyboards\": [\n");
    for (int i = 0; i < manifest.keyboards().size(); i++) {
      PackEntry entry = manifest.keyboards().get(i);
      builder
          .append("    { \"id\": \"")
          .append(jsonEscape(entry.id()))
          .append("\", \"path\": \"")
          .append(jsonEscape(entry.path().value()))
          .append("\" }");
      if (i < manifest.keyboards().size() - 1) builder.append(",");
      builder.append("\n");
    }
    builder.append("  ],\n");
    builder.append("  \"themes\": []\n");
    builder.append("}\n");
    return builder.toString();
  }

  @NonNull
  private static String jsonEscape(@NonNull String raw) {
    StringBuilder builder = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      switch (c) {
        case '\\' -> builder.append("\\\\");
        case '"' -> builder.append("\\\"");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> builder.append(c);
      }
    }
    return builder.toString();
  }

  @NonNull
  private static String generateUniquePackId(
      @NonNull File packsRoot, @NonNull String keyboardName) {
    String base = sanitizeId(keyboardName);
    if (base.isEmpty()) base = "custom";

    String prefix = ("custom_" + base + "_").toLowerCase(Locale.ROOT);
    for (int i = 0; i < 100; i++) {
      String candidate =
          prefix + Long.toString(System.currentTimeMillis(), 36) + (i == 0 ? "" : "_" + i);
      if (!new File(packsRoot, candidate).exists()) return candidate;
    }
    return prefix + Long.toString(System.currentTimeMillis());
  }

  @NonNull
  private static String sanitizeId(@NonNull String raw) {
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return "";

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
    return builder.toString();
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
}
