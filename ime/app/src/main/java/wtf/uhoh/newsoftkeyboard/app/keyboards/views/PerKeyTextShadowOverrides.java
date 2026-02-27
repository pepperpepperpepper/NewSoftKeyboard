package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import androidx.annotation.Nullable;

/** Optional per-key-type overrides for key label shadow. */
final class PerKeyTextShadowOverrides {

  static final class Overrides {
    @Nullable private Integer color;
    @Nullable private Integer radius;
    @Nullable private Integer offsetX;
    @Nullable private Integer offsetY;

    @Nullable
    Integer color() {
      return color;
    }

    @Nullable
    Integer radius() {
      return radius;
    }

    @Nullable
    Integer offsetX() {
      return offsetX;
    }

    @Nullable
    Integer offsetY() {
      return offsetY;
    }

    boolean isEmpty() {
      return color == null && radius == null && offsetX == null && offsetY == null;
    }

    void set(
        @Nullable Integer color,
        @Nullable Integer radius,
        @Nullable Integer offsetX,
        @Nullable Integer offsetY) {
      this.color = color;
      this.radius = radius;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
    }
  }

  private final Overrides special = new Overrides();
  private final Overrides spacebar = new Overrides();
  private final Overrides modifier = new Overrides();
  private final Overrides enter = new Overrides();

  Overrides special() {
    return special;
  }

  Overrides spacebar() {
    return spacebar;
  }

  Overrides modifier() {
    return modifier;
  }

  Overrides enter() {
    return enter;
  }
}
