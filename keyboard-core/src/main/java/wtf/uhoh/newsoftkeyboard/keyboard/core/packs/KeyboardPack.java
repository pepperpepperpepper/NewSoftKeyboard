package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.PackSource;

public final class KeyboardPack {
  private final PackManifest manifest;
  private final PackSource source;

  public KeyboardPack(PackManifest manifest, PackSource source) {
    this.manifest = Objects.requireNonNull(manifest);
    this.source = Objects.requireNonNull(source);
  }

  public PackManifest manifest() {
    return manifest;
  }

  public PackSource source() {
    return source;
  }
}
