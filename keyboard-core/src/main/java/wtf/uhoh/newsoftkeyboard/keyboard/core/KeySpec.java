package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KeySpec {
  private final List<KeyCode> codes;
  private final String label;
  private final String popupCharacters;
  private final Map<String, String> rawAttributes;

  public KeySpec(
      List<KeyCode> codes,
      String label,
      String popupCharacters,
      Map<String, String> rawAttributes) {
    this.codes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(codes)));
    this.label = label;
    this.popupCharacters = popupCharacters;
    this.rawAttributes =
        Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(rawAttributes)));
  }

  public List<KeyCode> codes() {
    return codes;
  }

  public String label() {
    return label;
  }

  public String popupCharacters() {
    return popupCharacters;
  }

  public Map<String, String> rawAttributes() {
    return rawAttributes;
  }
}
