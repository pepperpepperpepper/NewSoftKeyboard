package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class KeySpec {
  private final List<KeyCode> codes;
  private final Optional<String> label;
  private final Optional<String> popupCharacters;
  private final Map<String, String> rawAttributes;

  public KeySpec(
      List<KeyCode> codes,
      Optional<String> label,
      Optional<String> popupCharacters,
      Map<String, String> rawAttributes) {
    this.codes = List.copyOf(Objects.requireNonNull(codes));
    this.label = Objects.requireNonNull(label);
    this.popupCharacters = Objects.requireNonNull(popupCharacters);
    this.rawAttributes = Map.copyOf(Objects.requireNonNull(rawAttributes));
  }

  public List<KeyCode> codes() {
    return codes;
  }

  public Optional<String> label() {
    return label;
  }

  public Optional<String> popupCharacters() {
    return popupCharacters;
  }

  public Map<String, String> rawAttributes() {
    return rawAttributes;
  }
}
