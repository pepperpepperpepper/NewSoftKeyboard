package wtf.uhoh.newsoftkeyboard.keyboard.core.io;

import java.io.IOException;
import java.io.InputStream;

public interface PackSource {
  InputStream open(String packRelativePath) throws IOException;
}
