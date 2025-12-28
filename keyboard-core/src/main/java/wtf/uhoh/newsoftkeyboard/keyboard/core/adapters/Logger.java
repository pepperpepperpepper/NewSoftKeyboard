package wtf.uhoh.newsoftkeyboard.keyboard.core.adapters;

public interface Logger {
  void debug(String message);

  void warn(String message, Throwable error);

  void error(String message, Throwable error);
}
