package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.Objects;
import java.util.OptionalInt;

public sealed interface KeyCode permits KeyCode.Numeric, KeyCode.Symbolic {
  OptionalInt asNumeric();

  record Numeric(int value) implements KeyCode {
    @Override
    public OptionalInt asNumeric() {
      return OptionalInt.of(value);
    }
  }

  record Symbolic(String value) implements KeyCode {
    public Symbolic {
      Objects.requireNonNull(value);
    }

    @Override
    public OptionalInt asNumeric() {
      return OptionalInt.empty();
    }
  }
}
