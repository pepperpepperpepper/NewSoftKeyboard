package wtf.uhoh.newsoftkeyboard.keyboard.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FileSystemPackSource implements PackSource {
  private final Path rootDir;

  public FileSystemPackSource(Path rootDir) {
    this.rootDir = Objects.requireNonNull(rootDir).normalize();
  }

  @Override
  public InputStream open(String packRelativePath) throws IOException {
    Path resolved = rootDir.resolve(packRelativePath).normalize();
    if (!resolved.startsWith(rootDir)) {
      throw new IOException("Path escapes pack root: " + packRelativePath);
    }
    return Files.newInputStream(resolved);
  }
}
