package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifestJson;

public final class KeyboardPacksRepository {
  private static final String TAG = "KeyboardPacksRepo";
  private static final String PACKS_DIR_NAME = "keyboard_packs";
  static final String MANIFEST_FILE_NAME = "manifest.json";

  @NonNull private final Context applicationContext;
  @NonNull private final File packsRootDir;

  public KeyboardPacksRepository(@NonNull Context context) {
    applicationContext = Objects.requireNonNull(context).getApplicationContext();
    packsRootDir = new File(applicationContext.getFilesDir(), PACKS_DIR_NAME);
  }

  @NonNull
  public File packsRootDir() throws IOException {
    if (!packsRootDir.exists() && !packsRootDir.mkdirs()) {
      throw new IOException("Failed creating keyboard packs directory at " + packsRootDir);
    }
    return packsRootDir;
  }

  @NonNull
  public List<InstalledKeyboardPack> listInstalledPacks() throws IOException {
    File root = packsRootDir();
    File[] children = root.listFiles(File::isDirectory);
    if (children == null || children.length == 0) return Collections.emptyList();

    var packs = new ArrayList<InstalledKeyboardPack>(children.length);
    for (File candidateDir : children) {
      File manifestFile = new File(candidateDir, MANIFEST_FILE_NAME);
      if (!manifestFile.exists() || !manifestFile.isFile()) continue;
      try {
        PackManifest manifest = readManifest(manifestFile);
        packs.add(new InstalledKeyboardPack(candidateDir, manifest));
      } catch (IOException e) {
        Logger.w(TAG, "Skipping invalid pack at %s: %s", candidateDir, e.getMessage());
      }
    }

    packs.sort(Comparator.comparing(p -> p.manifest().name(), String.CASE_INSENSITIVE_ORDER));
    return Collections.unmodifiableList(packs);
  }

  @Nullable
  public InstalledKeyboardPack findInstalledPackById(@NonNull String packId) throws IOException {
    for (InstalledKeyboardPack pack : listInstalledPacks()) {
      if (pack.manifest().id().equals(packId)) return pack;
    }
    return null;
  }

  @NonNull
  private static PackManifest readManifest(@NonNull File manifestFile) throws IOException {
    try (InputStream in = new FileInputStream(manifestFile)) {
      return PackManifestJson.parse(in);
    }
  }
}
