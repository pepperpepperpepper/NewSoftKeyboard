package wtf.uhoh.newsoftkeyboard.keyboard.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** A pack source backed by a directory on a filesystem. */
public final class DirectoryPackSource implements PackSource {
  private final File rootDir;
  private final String rootDirPathWithSeparator;

  public DirectoryPackSource(File rootDir) throws IOException {
    this.rootDir = Objects.requireNonNull(rootDir).getCanonicalFile();
    String rootPath = this.rootDir.getPath();
    if (!rootPath.endsWith(File.separator)) rootPath += File.separator;
    rootDirPathWithSeparator = rootPath;
  }

  @Override
  public InputStream open(String packRelativePath) throws IOException {
    File file = new File(rootDir, packRelativePath).getCanonicalFile();
    if (!file.getPath().startsWith(rootDirPathWithSeparator)) {
      throw new IOException("Path escapes pack root: " + packRelativePath);
    }
    return new FileInputStream(file);
  }
}
