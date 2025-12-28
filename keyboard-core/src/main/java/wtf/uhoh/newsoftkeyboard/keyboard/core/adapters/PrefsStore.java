package wtf.uhoh.newsoftkeyboard.keyboard.core.adapters;

import java.util.Optional;

public interface PrefsStore {
  Optional<String> getString(String key);

  boolean getBoolean(String key, boolean defaultValue);

  int getInt(String key, int defaultValue);
}
