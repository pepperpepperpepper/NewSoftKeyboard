package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import androidx.annotation.NonNull;
import java.io.File;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;

public final class InstalledKeyboardPack {
  @NonNull private final File directory;
  @NonNull private final PackManifest manifest;

  public InstalledKeyboardPack(@NonNull File directory, @NonNull PackManifest manifest) {
    this.directory = Objects.requireNonNull(directory);
    this.manifest = Objects.requireNonNull(manifest);
  }

  @NonNull
  public File directory() {
    return directory;
  }

  @NonNull
  public PackManifest manifest() {
    return manifest;
  }
}
