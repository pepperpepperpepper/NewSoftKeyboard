package wtf.uhoh.newsoftkeyboard.keyboard.core.adapters;

import java.util.Optional;

public interface TextContext {
  Optional<String> surroundingText();

  int cursorPosition();
}
