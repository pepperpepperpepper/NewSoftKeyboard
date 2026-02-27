package wtf.uhoh.newsoftkeyboard.app.theme;

/** Constants and normalizers for keyboard wallpaper overrides. */
public final class KeyboardWallpaperOverrideConstants {

  public static final int WALLPAPER_MODE_BACKGROUND_ONLY = 0;
  public static final int WALLPAPER_MODE_BACKGROUND_KEY_TINT = 1;
  public static final int WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE = 2;

  public static final int WALLPAPER_BLEND_MODE_NORMAL = 0;
  public static final int WALLPAPER_BLEND_MODE_MULTIPLY = 1;
  public static final int WALLPAPER_BLEND_MODE_SCREEN = 2;
  public static final int WALLPAPER_BLEND_MODE_OVERLAY = 3;
  public static final int WALLPAPER_BLEND_MODE_SOFT_LIGHT = 4;

  public static final int KEY_LAYER_COLOR_WASH = 0;
  public static final int KEY_LAYER_HIGHLIGHT = 1;
  public static final int KEY_LAYER_GRADIENT = 2;
  public static final int KEY_LAYER_VIGNETTE = 3;
  public static final int KEY_LAYER_GRAIN = 4;

  public static final int BACKGROUND_LAYER_TINT = 0;
  public static final int BACKGROUND_LAYER_DIM = 1;
  public static final int BACKGROUND_LAYER_GRADIENT = 2;
  public static final int BACKGROUND_LAYER_VIGNETTE = 3;
  public static final int BACKGROUND_LAYER_GRAIN = 4;

  public static final int WALLPAPER_QUALITY_LOW = 0;
  public static final int WALLPAPER_QUALITY_BALANCED = 1;
  public static final int WALLPAPER_QUALITY_HIGH = 2;

  public static final int WALLPAPER_SCALE_MODE_CROP = 0;
  public static final int WALLPAPER_SCALE_MODE_FIT = 1;
  public static final int WALLPAPER_SCALE_MODE_STRETCH = 2;
  public static final int WALLPAPER_SCALE_MODE_TILE = 3;
  public static final int WALLPAPER_SCALE_MODE_MIRROR = 4;

  public static final int WALLPAPER_ANCHOR_TOP_LEFT = 0;
  public static final int WALLPAPER_ANCHOR_TOP = 1;
  public static final int WALLPAPER_ANCHOR_TOP_RIGHT = 2;
  public static final int WALLPAPER_ANCHOR_LEFT = 3;
  public static final int WALLPAPER_ANCHOR_CENTER = 4;
  public static final int WALLPAPER_ANCHOR_RIGHT = 5;
  public static final int WALLPAPER_ANCHOR_BOTTOM_LEFT = 6;
  public static final int WALLPAPER_ANCHOR_BOTTOM = 7;
  public static final int WALLPAPER_ANCHOR_BOTTOM_RIGHT = 8;

  public static final int DEFAULT_KEY_ALPHA_PERCENT = 20;

  private KeyboardWallpaperOverrideConstants() {}

  public static int normalizeBlendMode(int blendMode) {
    return switch (blendMode) {
      case WALLPAPER_BLEND_MODE_NORMAL,
          WALLPAPER_BLEND_MODE_MULTIPLY,
          WALLPAPER_BLEND_MODE_SCREEN,
          WALLPAPER_BLEND_MODE_OVERLAY,
          WALLPAPER_BLEND_MODE_SOFT_LIGHT ->
          blendMode;
      default -> WALLPAPER_BLEND_MODE_NORMAL;
    };
  }

  public static int normalizeRotationDegrees(int rotationDegrees) {
    final int normalized = ((rotationDegrees % 360) + 360) % 360;
    switch (normalized) {
      case 0:
      case 90:
      case 180:
      case 270:
        return normalized;
      default:
        return 0;
    }
  }
}
