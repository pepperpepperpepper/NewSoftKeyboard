package wtf.uhoh.newsoftkeyboard.keyboard.core.theme;

import java.util.Map;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;

public final class ThemeModel {
  private final Map<String, Integer> colors;
  private final Map<String, PackPath> icons;
  private final Map<String, String> rawAttributes;

  public ThemeModel(
      Map<String, Integer> colors, Map<String, PackPath> icons, Map<String, String> rawAttributes) {
    this.colors = Map.copyOf(Objects.requireNonNull(colors));
    this.icons = Map.copyOf(Objects.requireNonNull(icons));
    this.rawAttributes = Map.copyOf(Objects.requireNonNull(rawAttributes));
  }

  public Map<String, Integer> colors() {
    return colors;
  }

  public Map<String, PackPath> icons() {
    return icons;
  }

  public Map<String, String> rawAttributes() {
    return rawAttributes;
  }
}
