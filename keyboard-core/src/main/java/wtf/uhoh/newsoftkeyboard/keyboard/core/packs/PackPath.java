package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.util.Objects;

/** A validated, pack-relative path using forward slashes. */
public final class PackPath {
  private final String value;

  private PackPath(String value) {
    this.value = value;
  }

  public static PackPath parse(String raw) {
    Objects.requireNonNull(raw);
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Pack path is empty");
    }

    String normalized = trimmed.replace('\\', '/');
    while (normalized.startsWith("/")) normalized = normalized.substring(1);

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Pack path resolves to empty");
    }

    if (normalized.startsWith("@")) {
      throw new IllegalArgumentException(
          "Pack path must not use Android resource references: " + raw);
    }

    if (normalized.contains(":")) {
      throw new IllegalArgumentException("Pack path must be relative (':' not allowed): " + raw);
    }

    StringBuilder builder = new StringBuilder(normalized.length());
    int startIndex = 0;
    while (startIndex < normalized.length()) {
      int slashIndex = normalized.indexOf('/', startIndex);
      String part =
          slashIndex == -1
              ? normalized.substring(startIndex)
              : normalized.substring(startIndex, slashIndex);
      startIndex = slashIndex == -1 ? normalized.length() : slashIndex + 1;

      if (part.isEmpty()) continue;
      if (part.equals(".") || part.equals("..")) {
        throw new IllegalArgumentException("Pack path must not contain '.' or '..': " + raw);
      }
      if (builder.length() > 0) builder.append('/');
      builder.append(part);
    }

    String result = builder.toString();
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Pack path resolves to empty");
    }
    return new PackPath(result);
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PackPath other)) return false;
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
