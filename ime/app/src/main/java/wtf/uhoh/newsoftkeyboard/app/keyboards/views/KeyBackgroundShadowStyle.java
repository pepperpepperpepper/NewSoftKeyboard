package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

/** Holds key background shadow configuration to keep KeyboardViewBase slimmer. */
final class KeyBackgroundShadowStyle {
  private boolean enabled;
  private int color;
  private int offsetX;
  private int offsetY;
  private int spread;

  boolean enabled() {
    return enabled;
  }

  int color() {
    return color;
  }

  int offsetX() {
    return offsetX;
  }

  int offsetY() {
    return offsetY;
  }

  int spread() {
    return spread;
  }

  void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  void setColor(int color) {
    this.color = color;
  }

  void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
  }

  void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
  }

  void setSpread(int spread) {
    this.spread = spread;
  }

  void disable() {
    enabled = false;
    color = 0;
    offsetX = 0;
    offsetY = 0;
    spread = 0;
  }
}
