package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PackManifest {
  public static final int SUPPORTED_SCHEMA_VERSION = 1;

  private final int schemaVersion;
  private final String id;
  private final String name;
  private final int version;
  private final String minCoreVersion;
  private final List<PackEntry> keyboards;
  private final List<PackEntry> themes;

  public PackManifest(
      int schemaVersion,
      String id,
      String name,
      int version,
      String minCoreVersion,
      List<PackEntry> keyboards,
      List<PackEntry> themes) {
    this.schemaVersion = schemaVersion;
    this.id = Objects.requireNonNull(id);
    this.name = Objects.requireNonNull(name);
    this.version = version;
    this.minCoreVersion = minCoreVersion;
    this.keyboards =
        Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(keyboards)));
    this.themes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(themes)));
  }

  public int schemaVersion() {
    return schemaVersion;
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public int version() {
    return version;
  }

  public String minCoreVersion() {
    return minCoreVersion;
  }

  public List<PackEntry> keyboards() {
    return keyboards;
  }

  public List<PackEntry> themes() {
    return themes;
  }
}
