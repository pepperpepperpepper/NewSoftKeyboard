package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Paint;
import androidx.annotation.Nullable;

public final class KeyFaceOverlay {
  private int mode = KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
  @Nullable private Paint paint;
  private boolean matchKeyShape = false;
  private int specialKeyAlpha = Integer.MIN_VALUE;
  private int spacebarAlpha = Integer.MIN_VALUE;
  private int modifierKeyAlpha = Integer.MIN_VALUE;
  private int enterKeyAlpha = Integer.MIN_VALUE;
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  @Nullable private KeyboardWallpaperLayer[] layerStack;

  public int mode() {
    return mode;
  }

  @Nullable
  public Paint paint() {
    return paint;
  }

  public boolean matchKeyShape() {
    return matchKeyShape;
  }

  public int specialKeyAlpha() {
    return specialKeyAlpha;
  }

  public int spacebarAlpha() {
    return spacebarAlpha;
  }

  public int modifierKeyAlpha() {
    return modifierKeyAlpha;
  }

  public int enterKeyAlpha() {
    return enterKeyAlpha;
  }

  public int blendMode() {
    return blendMode;
  }

  @Nullable
  public KeyboardWallpaperLayer[] layerStack() {
    return layerStack;
  }

  void reset() {
    mode = KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
    paint = null;
    matchKeyShape = false;
    specialKeyAlpha = Integer.MIN_VALUE;
    spacebarAlpha = Integer.MIN_VALUE;
    modifierKeyAlpha = Integer.MIN_VALUE;
    enterKeyAlpha = Integer.MIN_VALUE;
    blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
    layerStack = null;
  }

  void setMode(int mode) {
    this.mode = mode;
  }

  void setPaint(@Nullable Paint paint) {
    this.paint = paint;
  }

  void setMatchKeyShape(boolean matchKeyShape) {
    this.matchKeyShape = matchKeyShape;
  }

  void setSpecialKeyAlpha(int specialKeyAlpha) {
    this.specialKeyAlpha = specialKeyAlpha;
  }

  void setSpacebarAlpha(int spacebarAlpha) {
    this.spacebarAlpha = spacebarAlpha;
  }

  void setModifierKeyAlpha(int modifierKeyAlpha) {
    this.modifierKeyAlpha = modifierKeyAlpha;
  }

  void setEnterKeyAlpha(int enterKeyAlpha) {
    this.enterKeyAlpha = enterKeyAlpha;
  }

  void setBlendMode(int blendMode) {
    this.blendMode = blendMode;
  }

  void setLayerStack(@Nullable KeyboardWallpaperLayer[] layerStack) {
    this.layerStack = layerStack;
  }
}
