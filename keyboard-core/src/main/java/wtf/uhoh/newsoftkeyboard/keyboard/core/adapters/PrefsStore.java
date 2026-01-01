package wtf.uhoh.newsoftkeyboard.keyboard.core.adapters;

public interface PrefsStore {
  String getString(String key);

  boolean getBoolean(String key, boolean defaultValue);

  int getInt(String key, int defaultValue);
}
