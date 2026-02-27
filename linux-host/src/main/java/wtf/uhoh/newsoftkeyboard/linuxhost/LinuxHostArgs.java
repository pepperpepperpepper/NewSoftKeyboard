package wtf.uhoh.newsoftkeyboard.linuxhost;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

enum OutputMode {
  EDITOR("editor"),
  STDOUT("stdout"),
  XDOTOOL("xdotool"),
  IBUS("ibus");

  private final String id;

  OutputMode(String id) {
    this.id = id;
  }

  String id() {
    return id;
  }

  static OutputMode parse(String raw) throws IOException {
    String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
    for (OutputMode mode : values()) {
      if (mode.id.equals(value)) return mode;
    }
    throw new IOException("Unsupported output mode: " + raw);
  }

  static OutputMode parseUnchecked(String raw) {
    if (raw == null) return EDITOR;
    String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
    for (OutputMode mode : values()) {
      if (mode.id.equals(value)) return mode;
    }
    return EDITOR;
  }
}

record OutputConfig(
    OutputMode mode,
    Optional<String> xdotoolWindowId,
    int xdotoolDelayMs,
    Path ibusSocketPath,
    Path ibusControlSocketPath,
    boolean ibusActivation) {
  OutputConfig {
    Objects.requireNonNull(mode);
    Objects.requireNonNull(xdotoolWindowId);
    Objects.requireNonNull(ibusSocketPath);
    Objects.requireNonNull(ibusControlSocketPath);
  }
}

record RunArgs(Path packDir, String keyboardId, OutputConfig outputConfig) {
  RunArgs {
    Objects.requireNonNull(packDir);
    Objects.requireNonNull(outputConfig);
  }
}

record SmokeArgs(Path packDir, String keyboardId, OutputConfig outputConfig, String text) {
  SmokeArgs {
    Objects.requireNonNull(packDir);
    Objects.requireNonNull(outputConfig);
    Objects.requireNonNull(text);
  }
}
