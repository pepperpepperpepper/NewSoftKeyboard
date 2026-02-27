package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;
import androidx.annotation.NonNull;

/** Calculates wallpaper shader mappings for user-selected keyboard background photos. */
public final class KeyboardWallpaperTransform {

  private KeyboardWallpaperTransform() {}

  @NonNull
  public static Shader.TileMode tileModeForScaleMode(int scaleMode) {
    switch (scaleMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE:
        return Shader.TileMode.REPEAT;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_MIRROR:
        return Shader.TileMode.MIRROR;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_STRETCH:
      default:
        return Shader.TileMode.CLAMP;
    }
  }

  public static void updateShaderMatrix(
      @NonNull Matrix outMatrix,
      int bitmapWidth,
      int bitmapHeight,
      @NonNull Rect bounds,
      int rotationDegrees,
      int scaleMode,
      int anchor) {
    if (bitmapWidth <= 0 || bitmapHeight <= 0) return;

    final float boundsW = bounds.width();
    final float boundsH = bounds.height();
    if (boundsW <= 0f || boundsH <= 0f) return;

    final int rotation =
        KeyboardWallpaperOverrideConstants.normalizeRotationDegrees(rotationDegrees);
    final int normalizedScaleMode = normalizeScaleMode(scaleMode);
    final int normalizedAnchor = normalizeAnchor(anchor);

    final float effectiveW = (rotation == 90 || rotation == 270) ? bitmapHeight : bitmapWidth;
    final float effectiveH = (rotation == 90 || rotation == 270) ? bitmapWidth : bitmapHeight;

    final float scaleX;
    final float scaleY;
    switch (normalizedScaleMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_STRETCH:
        scaleX = boundsW / effectiveW;
        scaleY = boundsH / effectiveH;
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT:
        final float fit = Math.min(boundsW / effectiveW, boundsH / effectiveH);
        scaleX = fit;
        scaleY = fit;
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_MIRROR:
        scaleX = 1f;
        scaleY = 1f;
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP:
      default:
        final float crop = Math.max(boundsW / effectiveW, boundsH / effectiveH);
        scaleX = crop;
        scaleY = crop;
        break;
    }

    final float anchorVectorX;
    final float anchorVectorY;
    {
      final int anchorInOriginal = anchorInOriginalForRotation(normalizedAnchor, rotation);
      final int anchorCol = anchorInOriginal % 3;
      final int anchorRow = anchorInOriginal / 3;
      final float anchorBitmapX = anchorCol * (bitmapWidth / 2f);
      final float anchorBitmapY = anchorRow * (bitmapHeight / 2f);
      anchorVectorX = anchorBitmapX - (bitmapWidth / 2f);
      anchorVectorY = anchorBitmapY - (bitmapHeight / 2f);
    }

    final float rotatedVectorX;
    final float rotatedVectorY;
    switch (rotation) {
      case 90:
        rotatedVectorX = -anchorVectorY;
        rotatedVectorY = anchorVectorX;
        break;
      case 180:
        rotatedVectorX = -anchorVectorX;
        rotatedVectorY = -anchorVectorY;
        break;
      case 270:
        rotatedVectorX = anchorVectorY;
        rotatedVectorY = -anchorVectorX;
        break;
      case 0:
      default:
        rotatedVectorX = anchorVectorX;
        rotatedVectorY = anchorVectorY;
        break;
    }

    final float scaledVectorX = rotatedVectorX * scaleX;
    final float scaledVectorY = rotatedVectorY * scaleY;

    final int anchorCol = normalizedAnchor % 3;
    final int anchorRow = normalizedAnchor / 3;
    final float targetX = bounds.left + (anchorCol * boundsW / 2f);
    final float targetY = bounds.top + (anchorRow * boundsH / 2f);

    final float translateX = targetX - scaledVectorX;
    final float translateY = targetY - scaledVectorY;

    outMatrix.reset();
    outMatrix.postTranslate(-bitmapWidth / 2f, -bitmapHeight / 2f);
    if (rotation != 0) {
      outMatrix.postRotate(rotation);
    }
    outMatrix.postScale(scaleX, scaleY);
    outMatrix.postTranslate(translateX, translateY);
  }

  private static int normalizeScaleMode(int scaleMode) {
    switch (scaleMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_STRETCH:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_MIRROR:
        return scaleMode;
      default:
        return KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP;
    }
  }

  private static int normalizeAnchor(int anchor) {
    return anchor >= KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_LEFT
            && anchor <= KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT
        ? anchor
        : KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER;
  }

  private static int anchorInOriginalForRotation(int anchor, int rotationDegrees) {
    final int row = anchor / 3;
    final int col = anchor % 3;
    final int r;
    final int c;
    switch (rotationDegrees) {
      case 90:
        // Inverse rotation: counterclockwise 90.
        r = 2 - col;
        c = row;
        break;
      case 180:
        r = 2 - row;
        c = 2 - col;
        break;
      case 270:
        // Inverse rotation: clockwise 90.
        r = col;
        c = 2 - row;
        break;
      case 0:
      default:
        r = row;
        c = col;
        break;
    }
    return (r * 3) + c;
  }
}
