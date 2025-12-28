package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.FileSystemPackSource;

public final class FileSystemKeyboardPackLoader {
  private static final String MANIFEST_FILENAME = "manifest.json";

  private FileSystemKeyboardPackLoader() {}

  public static KeyboardPack loadPack(Path packDir) throws IOException {
    Path manifestPath = packDir.resolve(MANIFEST_FILENAME);
    try (InputStream inputStream = Files.newInputStream(manifestPath)) {
      PackManifest manifest = PackManifestJson.parse(inputStream);
      return new KeyboardPack(manifest, new FileSystemPackSource(packDir));
    }
  }

  public static List<KeyboardPack> loadAllPacks(Path packsRoot) throws IOException {
    if (!Files.exists(packsRoot)) return List.of();

    var result = new ArrayList<KeyboardPack>();
    try (var stream = Files.list(packsRoot)) {
      for (Path child : stream.toList()) {
        if (!Files.isDirectory(child)) continue;
        Path manifest = child.resolve(MANIFEST_FILENAME);
        if (!Files.exists(manifest)) continue;
        result.add(loadPack(child));
      }
    }
    return List.copyOf(result);
  }
}
