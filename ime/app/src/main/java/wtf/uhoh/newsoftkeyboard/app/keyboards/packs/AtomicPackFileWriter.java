package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Replaces pack files (keyboard XML, manifest, theme XML) atomically: content is written to a
 * temp file in the same directory, synced to disk, and renamed over the target. A crash or power
 * loss mid-write leaves the previous file intact instead of a truncated one that silently breaks
 * the whole pack.
 */
public final class AtomicPackFileWriter {
  private AtomicPackFileWriter() {}

  public interface ContentWriter {
    void writeTo(@NonNull OutputStream out) throws IOException;
  }

  public static void write(@NonNull File target, @NonNull ContentWriter content)
      throws IOException {
    File parent = target.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed creating directory at " + parent);
    }

    // The temp file must live in the same directory as the target for rename to be atomic.
    File temp = File.createTempFile(target.getName() + ".", ".tmp", parent);
    try {
      try (FileOutputStream out = new FileOutputStream(temp)) {
        content.writeTo(out);
        out.flush();
        out.getFD().sync();
      }
      if (!temp.renameTo(target)) {
        throw new IOException("Failed replacing " + target);
      }
    } catch (IOException | RuntimeException e) {
      //noinspection ResultOfMethodCallIgnored
      temp.delete();
      throw e;
    }
  }
}
