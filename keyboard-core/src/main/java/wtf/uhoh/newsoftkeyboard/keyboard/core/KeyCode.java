package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.Objects;

public sealed interface KeyCode permits KeyCode.Numeric, KeyCode.Symbolic {
  Integer asNumeric();

  record Numeric(int value) implements KeyCode {
    @Override
    public Integer asNumeric() {
      return value;
    }
  }

  record Symbolic(String value) implements KeyCode {
    public Symbolic {
      Objects.requireNonNull(value);
    }

    @Override
    public Integer asNumeric() {
      return null;
    }
  }
}
