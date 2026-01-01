package wtf.uhoh.newsoftkeyboard.linuxhost.prefs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import wtf.uhoh.newsoftkeyboard.keyboard.core.adapters.PrefsStore;

public final class FilePrefsStore implements PrefsStore {
  private final Properties properties;

  private FilePrefsStore(Properties properties) {
    this.properties = properties;
  }

  public static FilePrefsStore load(Path propertiesFile) throws IOException {
    var properties = new Properties();
    if (Files.exists(propertiesFile)) {
      try (InputStream inputStream = Files.newInputStream(propertiesFile)) {
        properties.load(inputStream);
      }
    }
    return new FilePrefsStore(properties);
  }

  @Override
  public String getString(String key) {
    String value = properties.getProperty(key);
    if (value == null) return null;
    String trimmed = value.trim();
    if (trimmed.isEmpty()) return null;
    return trimmed;
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    String value = getString(key);
    return value != null ? parseBoolean(value) : defaultValue;
  }

  @Override
  public int getInt(String key, int defaultValue) {
    String value = getString(key);
    if (value == null) return defaultValue;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static boolean parseBoolean(String value) {
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "1", "true", "yes", "y", "on" -> true;
      default -> false;
    };
  }
}
