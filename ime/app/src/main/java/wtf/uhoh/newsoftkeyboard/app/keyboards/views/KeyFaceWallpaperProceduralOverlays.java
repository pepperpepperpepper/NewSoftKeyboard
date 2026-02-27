package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Random;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;

final class KeyFaceWallpaperProceduralOverlays {

  private static final int NOISE_SIZE_PX = 64;
  private static final long NOISE_SEED = 0x4E534B475241494EL; // "NSKGRAIN"
  private static final int DOTS_SIZE_PX = 64;
  private static final int GRID_SIZE_PX = 64;
  private static final int STRIPES_SIZE_PX = 64;
  private static final int CHECKER_SIZE_PX = 64;
  private static final int TRIANGLES_SIZE_PX = 64;
  private static final int HEX_SIZE_PX = 64;
  private static final int CHECKER_SQUARE_STEP_PX = CHECKER_SIZE_PX / 4;

  @Nullable private static Bitmap cachedNoiseBitmap;
  @Nullable private static Bitmap cachedDotsBitmap;
  @Nullable private static Bitmap cachedGridBitmap;
  @Nullable private static Bitmap cachedStripesBitmap;
  @Nullable private static Bitmap cachedCheckerBitmap;
  @Nullable private static Bitmap cachedTrianglesBitmap;
  @Nullable private static Bitmap cachedHexBitmap;

  private final Paint keyFaceGrainOverlayPaint = new Paint();
  private final BitmapShader keyFaceGrainOverlayShader;
  private final Matrix keyFaceGrainOverlayShaderMatrix = new Matrix();
  private final Paint keyFaceDotsOverlayPaint = new Paint();
  private final BitmapShader keyFaceDotsOverlayShader;
  private final Matrix keyFaceDotsOverlayShaderMatrix = new Matrix();
  private final Paint keyFaceGridOverlayPaint = new Paint();
  private final BitmapShader keyFaceGridOverlayShader;
  private final Matrix keyFaceGridOverlayShaderMatrix = new Matrix();
  private final Paint keyFaceCheckerOverlayPaint = new Paint();
  private final BitmapShader keyFaceCheckerOverlayShader;
  private final Matrix keyFaceCheckerOverlayShaderMatrix = new Matrix();
  private final Paint keyFaceCheckerOverlayPaintB = new Paint();
  private final BitmapShader keyFaceCheckerOverlayShaderB;
  private final Matrix keyFaceCheckerOverlayShaderMatrixB = new Matrix();
  private final Paint keyFaceStripesOverlayPaint = new Paint();
  private final BitmapShader keyFaceStripesOverlayShader;
  private final Matrix keyFaceStripesOverlayShaderMatrix = new Matrix();
  private final Paint keyFaceTrianglesOverlayPaint = new Paint();
  private final BitmapShader keyFaceTrianglesOverlayShader;
  private final Matrix keyFaceTrianglesOverlayShaderMatrix = new Matrix();
  private final Paint keyFaceHexOverlayPaint = new Paint();
  private final BitmapShader keyFaceHexOverlayShader;
  private final Matrix keyFaceHexOverlayShaderMatrix = new Matrix();

  private int cachedKeyFaceGrainScalePercent;
  private int cachedKeyFaceDotsScalePercent;
  private int cachedKeyFaceGridScalePercent;
  private int cachedKeyFaceCheckerScalePercent;
  private int cachedKeyFaceStripesScalePercent;
  private int cachedKeyFaceStripesDirection;
  private int cachedKeyFaceTrianglesScalePercent;
  private int cachedKeyFaceHexScalePercent;

  KeyFaceWallpaperProceduralOverlays() {
    keyFaceGrainOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceGrainOverlayPaint.setFilterBitmap(false);
    keyFaceGrainOverlayPaint.setDither(false);
    keyFaceGrainOverlayShader =
        new BitmapShader(getNoiseBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceGrainOverlayPaint.setShader(keyFaceGrainOverlayShader);

    keyFaceDotsOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceDotsOverlayPaint.setFilterBitmap(false);
    keyFaceDotsOverlayPaint.setDither(false);
    keyFaceDotsOverlayShader =
        new BitmapShader(getDotsBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceDotsOverlayPaint.setShader(keyFaceDotsOverlayShader);

    keyFaceGridOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceGridOverlayPaint.setFilterBitmap(false);
    keyFaceGridOverlayPaint.setDither(false);
    keyFaceGridOverlayShader =
        new BitmapShader(getGridBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceGridOverlayPaint.setShader(keyFaceGridOverlayShader);

    keyFaceCheckerOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceCheckerOverlayPaint.setFilterBitmap(false);
    keyFaceCheckerOverlayPaint.setDither(false);
    keyFaceCheckerOverlayShader =
        new BitmapShader(getCheckerBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceCheckerOverlayPaint.setShader(keyFaceCheckerOverlayShader);
    keyFaceCheckerOverlayPaintB.setStyle(Paint.Style.FILL);
    keyFaceCheckerOverlayPaintB.setFilterBitmap(false);
    keyFaceCheckerOverlayPaintB.setDither(false);
    keyFaceCheckerOverlayShaderB =
        new BitmapShader(getCheckerBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceCheckerOverlayPaintB.setShader(keyFaceCheckerOverlayShaderB);

    keyFaceStripesOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceStripesOverlayPaint.setFilterBitmap(false);
    keyFaceStripesOverlayPaint.setDither(false);
    keyFaceStripesOverlayShader =
        new BitmapShader(getStripesBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceStripesOverlayPaint.setShader(keyFaceStripesOverlayShader);

    keyFaceTrianglesOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceTrianglesOverlayPaint.setFilterBitmap(false);
    keyFaceTrianglesOverlayPaint.setDither(false);
    keyFaceTrianglesOverlayShader =
        new BitmapShader(getTrianglesBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceTrianglesOverlayPaint.setShader(keyFaceTrianglesOverlayShader);

    keyFaceHexOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceHexOverlayPaint.setFilterBitmap(false);
    keyFaceHexOverlayPaint.setDither(false);
    keyFaceHexOverlayShader =
        new BitmapShader(getHexBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    keyFaceHexOverlayPaint.setShader(keyFaceHexOverlayShader);

    cachedKeyFaceGrainScalePercent = Integer.MIN_VALUE;
    cachedKeyFaceDotsScalePercent = Integer.MIN_VALUE;
    cachedKeyFaceGridScalePercent = Integer.MIN_VALUE;
    cachedKeyFaceCheckerScalePercent = Integer.MIN_VALUE;
    cachedKeyFaceStripesScalePercent = Integer.MIN_VALUE;
    cachedKeyFaceStripesDirection = Integer.MIN_VALUE;
    cachedKeyFaceTrianglesScalePercent = Integer.MIN_VALUE;
    cachedKeyFaceHexScalePercent = Integer.MIN_VALUE;
  }

  boolean drawLayerOnRect(
      @NonNull Canvas canvas,
      @NonNull Rect dirtyRect,
      @NonNull KeyboardWallpaperLayer layer,
      int effectiveAlpha) {
    switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_GRAIN:
        ensureKeyFaceGrainScale(layer);
        keyFaceGrainOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceGrainOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceGrainOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_DOTS:
        final Integer dotsArgb = layer.argb();
        if (dotsArgb == null || ((dotsArgb >>> 24) == 0)) return true;
        ensureKeyFaceDotsScale(layer);
        keyFaceDotsOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(dotsArgb));
        keyFaceDotsOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceDotsOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceDotsOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_GRID:
        final Integer gridArgb = layer.argb();
        if (gridArgb == null || ((gridArgb >>> 24) == 0)) return true;
        ensureKeyFaceGridScale(layer);
        keyFaceGridOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(gridArgb));
        keyFaceGridOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceGridOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceGridOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_CHECKER:
        final Integer checkerArgb = layer.argb();
        final Integer checkerArgb2 = layer.argb2();
        final boolean hasChecker = checkerArgb != null && ((checkerArgb >>> 24) != 0);
        final boolean hasChecker2 = checkerArgb2 != null && ((checkerArgb2 >>> 24) != 0);
        if (!hasChecker && !hasChecker2) return true;
        ensureKeyFaceCheckerScale(layer);
        if (hasChecker) {
          keyFaceCheckerOverlayPaint.setColorFilter(
              KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(checkerArgb));
          keyFaceCheckerOverlayPaint.setAlpha(effectiveAlpha);
          KeyFaceWallpaperEffectsRenderer.configureBlendMode(
              keyFaceCheckerOverlayPaint, layer.blendMode());
          canvas.drawRect(dirtyRect, keyFaceCheckerOverlayPaint);
        }
        if (hasChecker2) {
          keyFaceCheckerOverlayPaintB.setColorFilter(
              KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(checkerArgb2));
          keyFaceCheckerOverlayPaintB.setAlpha(effectiveAlpha);
          KeyFaceWallpaperEffectsRenderer.configureBlendMode(
              keyFaceCheckerOverlayPaintB, layer.blendMode());
          canvas.drawRect(dirtyRect, keyFaceCheckerOverlayPaintB);
        }
        return true;
      case KeyboardWallpaperLayer.TYPE_TRIANGLES:
        final Integer trianglesArgb = layer.argb();
        if (trianglesArgb == null || ((trianglesArgb >>> 24) == 0)) return true;
        ensureKeyFaceTrianglesScale(layer);
        keyFaceTrianglesOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(trianglesArgb));
        keyFaceTrianglesOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceTrianglesOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceTrianglesOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_HEX:
        final Integer hexArgb = layer.argb();
        if (hexArgb == null || ((hexArgb >>> 24) == 0)) return true;
        ensureKeyFaceHexScale(layer);
        keyFaceHexOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(hexArgb));
        keyFaceHexOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceHexOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceHexOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES:
        final Integer diagonalStripesArgb = layer.argb();
        if (diagonalStripesArgb == null || ((diagonalStripesArgb >>> 24) == 0)) return true;
        ensureKeyFaceStripesTransform(layer, true);
        keyFaceStripesOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(diagonalStripesArgb));
        keyFaceStripesOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceStripesOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceStripesOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_STRIPES:
        final Integer stripesArgb = layer.argb();
        if (stripesArgb == null || ((stripesArgb >>> 24) == 0)) return true;
        ensureKeyFaceStripesTransform(layer, false);
        keyFaceStripesOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(stripesArgb));
        keyFaceStripesOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceStripesOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceStripesOverlayPaint);
        return true;
      default:
        return false;
    }
  }

  boolean drawLayerOnKey(
      @NonNull Canvas canvas,
      @NonNull Drawable keyBackground,
      @NonNull KeyboardKey key,
      boolean matchKeyShape,
      float cornerRadius,
      @NonNull KeyboardWallpaperLayer layer,
      int effectiveAlpha,
      @NonNull KeyFaceOverlayMaskRenderer maskRenderer) {
    switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_GRAIN:
        ensureKeyFaceGrainScale(layer);
        keyFaceGrainOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceGrainOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceGrainOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_DOTS:
        final Integer dotsArgb = layer.argb();
        if (dotsArgb == null || ((dotsArgb >>> 24) == 0)) return true;
        ensureKeyFaceDotsScale(layer);
        keyFaceDotsOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(dotsArgb));
        keyFaceDotsOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceDotsOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceDotsOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_GRID:
        final Integer gridArgb = layer.argb();
        if (gridArgb == null || ((gridArgb >>> 24) == 0)) return true;
        ensureKeyFaceGridScale(layer);
        keyFaceGridOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(gridArgb));
        keyFaceGridOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceGridOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceGridOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_CHECKER:
        final Integer checkerArgb = layer.argb();
        final Integer checkerArgb2 = layer.argb2();
        final boolean hasChecker = checkerArgb != null && ((checkerArgb >>> 24) != 0);
        final boolean hasChecker2 = checkerArgb2 != null && ((checkerArgb2 >>> 24) != 0);
        if (!hasChecker && !hasChecker2) return true;
        ensureKeyFaceCheckerScale(layer);
        if (hasChecker) {
          keyFaceCheckerOverlayPaint.setColorFilter(
              KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(checkerArgb));
          keyFaceCheckerOverlayPaint.setAlpha(effectiveAlpha);
          KeyFaceWallpaperEffectsRenderer.configureBlendMode(
              keyFaceCheckerOverlayPaint, layer.blendMode());
          drawKeyFaceOverlayShape(
              canvas,
              keyBackground,
              key,
              matchKeyShape,
              cornerRadius,
              keyFaceCheckerOverlayPaint,
              maskRenderer);
        }
        if (hasChecker2) {
          keyFaceCheckerOverlayPaintB.setColorFilter(
              KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(checkerArgb2));
          keyFaceCheckerOverlayPaintB.setAlpha(effectiveAlpha);
          KeyFaceWallpaperEffectsRenderer.configureBlendMode(
              keyFaceCheckerOverlayPaintB, layer.blendMode());
          drawKeyFaceOverlayShape(
              canvas,
              keyBackground,
              key,
              matchKeyShape,
              cornerRadius,
              keyFaceCheckerOverlayPaintB,
              maskRenderer);
        }
        return true;
      case KeyboardWallpaperLayer.TYPE_TRIANGLES:
        final Integer trianglesArgb = layer.argb();
        if (trianglesArgb == null || ((trianglesArgb >>> 24) == 0)) return true;
        ensureKeyFaceTrianglesScale(layer);
        keyFaceTrianglesOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(trianglesArgb));
        keyFaceTrianglesOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceTrianglesOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceTrianglesOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_HEX:
        final Integer hexArgb = layer.argb();
        if (hexArgb == null || ((hexArgb >>> 24) == 0)) return true;
        ensureKeyFaceHexScale(layer);
        keyFaceHexOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(hexArgb));
        keyFaceHexOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceHexOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceHexOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES:
        final Integer diagonalStripesArgb = layer.argb();
        if (diagonalStripesArgb == null || ((diagonalStripesArgb >>> 24) == 0)) return true;
        ensureKeyFaceStripesTransform(layer, true);
        keyFaceStripesOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(diagonalStripesArgb));
        keyFaceStripesOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceStripesOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceStripesOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_STRIPES:
        final Integer stripesArgb = layer.argb();
        if (stripesArgb == null || ((stripesArgb >>> 24) == 0)) return true;
        ensureKeyFaceStripesTransform(layer, false);
        keyFaceStripesOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(stripesArgb));
        keyFaceStripesOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceStripesOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceStripesOverlayPaint,
            maskRenderer);
        return true;
      default:
        return false;
    }
  }

  private void ensureKeyFaceGrainScale(@NonNull KeyboardWallpaperLayer layer) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    if (cachedKeyFaceGrainScalePercent == desiredScalePercent) return;
    final float scale = desiredScalePercent / 100f;
    keyFaceGrainOverlayShaderMatrix.reset();
    keyFaceGrainOverlayShaderMatrix.setScale(scale, scale);
    keyFaceGrainOverlayShader.setLocalMatrix(keyFaceGrainOverlayShaderMatrix);
    cachedKeyFaceGrainScalePercent = desiredScalePercent;
  }

  private void ensureKeyFaceDotsScale(@NonNull KeyboardWallpaperLayer layer) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    if (cachedKeyFaceDotsScalePercent == desiredScalePercent) return;
    final float scale = desiredScalePercent / 100f;
    keyFaceDotsOverlayShaderMatrix.reset();
    keyFaceDotsOverlayShaderMatrix.setScale(scale, scale);
    keyFaceDotsOverlayShader.setLocalMatrix(keyFaceDotsOverlayShaderMatrix);
    cachedKeyFaceDotsScalePercent = desiredScalePercent;
  }

  private void ensureKeyFaceGridScale(@NonNull KeyboardWallpaperLayer layer) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    if (cachedKeyFaceGridScalePercent == desiredScalePercent) return;
    final float scale = desiredScalePercent / 100f;
    keyFaceGridOverlayShaderMatrix.reset();
    keyFaceGridOverlayShaderMatrix.setScale(scale, scale);
    keyFaceGridOverlayShader.setLocalMatrix(keyFaceGridOverlayShaderMatrix);
    cachedKeyFaceGridScalePercent = desiredScalePercent;
  }

  private void ensureKeyFaceCheckerScale(@NonNull KeyboardWallpaperLayer layer) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    if (cachedKeyFaceCheckerScalePercent == desiredScalePercent) return;
    final float scale = desiredScalePercent / 100f;
    keyFaceCheckerOverlayShaderMatrix.reset();
    keyFaceCheckerOverlayShaderMatrix.setScale(scale, scale);
    keyFaceCheckerOverlayShader.setLocalMatrix(keyFaceCheckerOverlayShaderMatrix);
    keyFaceCheckerOverlayShaderMatrixB.reset();
    keyFaceCheckerOverlayShaderMatrixB.setScale(scale, scale);
    keyFaceCheckerOverlayShaderMatrixB.postTranslate(CHECKER_SQUARE_STEP_PX, 0f);
    keyFaceCheckerOverlayShaderB.setLocalMatrix(keyFaceCheckerOverlayShaderMatrixB);
    cachedKeyFaceCheckerScalePercent = desiredScalePercent;
  }

  private void ensureKeyFaceTrianglesScale(@NonNull KeyboardWallpaperLayer layer) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    if (cachedKeyFaceTrianglesScalePercent == desiredScalePercent) return;
    final float scale = desiredScalePercent / 100f;
    keyFaceTrianglesOverlayShaderMatrix.reset();
    keyFaceTrianglesOverlayShaderMatrix.setScale(scale, scale);
    keyFaceTrianglesOverlayShader.setLocalMatrix(keyFaceTrianglesOverlayShaderMatrix);
    cachedKeyFaceTrianglesScalePercent = desiredScalePercent;
  }

  private void ensureKeyFaceHexScale(@NonNull KeyboardWallpaperLayer layer) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    if (cachedKeyFaceHexScalePercent == desiredScalePercent) return;
    final float scale = desiredScalePercent / 100f;
    keyFaceHexOverlayShaderMatrix.reset();
    keyFaceHexOverlayShaderMatrix.setScale(scale, scale);
    keyFaceHexOverlayShader.setLocalMatrix(keyFaceHexOverlayShaderMatrix);
    cachedKeyFaceHexScalePercent = desiredScalePercent;
  }

  private void ensureKeyFaceStripesTransform(
      @NonNull KeyboardWallpaperLayer layer, boolean diagonal) {
    final int desiredScalePercent = Math.max(25, Math.min(400, layer.scalePercent()));
    final int normalizedDirection;
    final float rotationDegrees;
    if (diagonal) {
      normalizedDirection = 2;
      rotationDegrees = 45f;
    } else if (layer.direction() == KeyboardWallpaperLayer.DIRECTION_HORIZONTAL
        || layer.direction() == KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE) {
      normalizedDirection = 1;
      rotationDegrees = 90f;
    } else {
      normalizedDirection = 0;
      rotationDegrees = 0f;
    }
    if (cachedKeyFaceStripesScalePercent == desiredScalePercent
        && cachedKeyFaceStripesDirection == normalizedDirection) {
      return;
    }

    final float scale = desiredScalePercent / 100f;
    keyFaceStripesOverlayShaderMatrix.reset();
    keyFaceStripesOverlayShaderMatrix.setScale(scale, scale);
    if (rotationDegrees != 0f) {
      keyFaceStripesOverlayShaderMatrix.postRotate(rotationDegrees);
    }
    keyFaceStripesOverlayShader.setLocalMatrix(keyFaceStripesOverlayShaderMatrix);
    cachedKeyFaceStripesScalePercent = desiredScalePercent;
    cachedKeyFaceStripesDirection = normalizedDirection;
  }

  @NonNull
  private static Bitmap getNoiseBitmap() {
    if (cachedNoiseBitmap != null && !cachedNoiseBitmap.isRecycled()) return cachedNoiseBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedNoiseBitmap != null && !cachedNoiseBitmap.isRecycled()) return cachedNoiseBitmap;
      cachedNoiseBitmap = generateNoiseBitmap();
      return cachedNoiseBitmap;
    }
  }

  @NonNull
  private static Bitmap generateNoiseBitmap() {
    final int size = NOISE_SIZE_PX;
    final int[] pixels = new int[size * size];
    final Random random = new Random(NOISE_SEED);
    for (int i = 0; i < pixels.length; i++) {
      final int jitter = random.nextInt(65) - 32; // [-32..32]
      final int value = 128 + jitter;
      pixels[i] = 0xFF000000 | (value << 16) | (value << 8) | value;
    }
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
    return bitmap;
  }

  @NonNull
  private static Bitmap getDotsBitmap() {
    if (cachedDotsBitmap != null && !cachedDotsBitmap.isRecycled()) return cachedDotsBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedDotsBitmap != null && !cachedDotsBitmap.isRecycled()) return cachedDotsBitmap;
      cachedDotsBitmap = generateDotsBitmap();
      return cachedDotsBitmap;
    }
  }

  @NonNull
  private static Bitmap generateDotsBitmap() {
    final int size = DOTS_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    dotPaint.setStyle(Paint.Style.FILL);
    dotPaint.setColor(Color.WHITE);
    canvas.drawCircle(size / 2f, size / 2f, size * 0.2f, dotPaint);
    return bitmap;
  }

  @NonNull
  private static Bitmap getGridBitmap() {
    if (cachedGridBitmap != null && !cachedGridBitmap.isRecycled()) return cachedGridBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedGridBitmap != null && !cachedGridBitmap.isRecycled()) return cachedGridBitmap;
      cachedGridBitmap = generateGridBitmap();
      return cachedGridBitmap;
    }
  }

  @NonNull
  private static Bitmap generateGridBitmap() {
    final int size = GRID_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    linePaint.setStyle(Paint.Style.STROKE);
    linePaint.setColor(Color.WHITE);
    linePaint.setStrokeWidth(Math.max(1f, size * 0.05f));
    // Draw only top+left to avoid doubling thickness at tile seams.
    canvas.drawLine(0f, 0f, size, 0f, linePaint);
    canvas.drawLine(0f, 0f, 0f, size, linePaint);
    return bitmap;
  }

  @NonNull
  private static Bitmap getCheckerBitmap() {
    if (cachedCheckerBitmap != null && !cachedCheckerBitmap.isRecycled())
      return cachedCheckerBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedCheckerBitmap != null && !cachedCheckerBitmap.isRecycled())
        return cachedCheckerBitmap;
      cachedCheckerBitmap = generateCheckerBitmap();
      return cachedCheckerBitmap;
    }
  }

  @NonNull
  private static Bitmap generateCheckerBitmap() {
    final int size = CHECKER_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint squarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    squarePaint.setStyle(Paint.Style.FILL);
    squarePaint.setColor(Color.WHITE);

    final int squares = 4;
    final float step = size / (float) squares;
    for (int y = 0; y < squares; y++) {
      for (int x = 0; x < squares; x++) {
        if (((x + y) & 1) != 0) continue;
        final float left = x * step;
        final float top = y * step;
        canvas.drawRect(left, top, left + step, top + step, squarePaint);
      }
    }
    return bitmap;
  }

  @NonNull
  private static Bitmap getStripesBitmap() {
    if (cachedStripesBitmap != null && !cachedStripesBitmap.isRecycled())
      return cachedStripesBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedStripesBitmap != null && !cachedStripesBitmap.isRecycled())
        return cachedStripesBitmap;
      cachedStripesBitmap = generateStripesBitmap();
      return cachedStripesBitmap;
    }
  }

  @NonNull
  private static Bitmap generateStripesBitmap() {
    final int size = STRIPES_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    stripePaint.setStyle(Paint.Style.FILL);
    stripePaint.setColor(Color.WHITE);
    final float stripeWidth = Math.max(1f, size * 0.18f);
    canvas.drawRect(0f, 0f, stripeWidth, size, stripePaint);
    return bitmap;
  }

  @NonNull
  private static Bitmap getHexBitmap() {
    if (cachedHexBitmap != null && !cachedHexBitmap.isRecycled()) return cachedHexBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedHexBitmap != null && !cachedHexBitmap.isRecycled()) return cachedHexBitmap;
      cachedHexBitmap = generateHexBitmap();
      return cachedHexBitmap;
    }
  }

  @NonNull
  private static Bitmap generateHexBitmap() {
    final int size = HEX_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint hexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    hexPaint.setStyle(Paint.Style.STROKE);
    hexPaint.setColor(Color.WHITE);
    hexPaint.setStrokeWidth(Math.max(1f, size * 0.05f));
    hexPaint.setStrokeJoin(Paint.Join.ROUND);
    hexPaint.setStrokeCap(Paint.Cap.ROUND);

    final float r = size / 7f;
    final float dx = (float) (Math.sqrt(3f) * r);
    final float dy = 1.5f * r;
    final float xOffset = r;
    final float yOffset = r;

    final int cols = (int) Math.ceil((size + r) / dx) + 2;
    final int rows = (int) Math.ceil((size + r) / dy) + 2;
    for (int col = -1; col <= cols; col++) {
      final float cx = xOffset + col * dx;
      final float colOffsetY = ((col & 1) == 0) ? 0f : (dy / 2f);
      for (int row = -1; row <= rows; row++) {
        final float cy = yOffset + row * dy + colOffsetY;
        final Path path = new Path();
        for (int i = 0; i < 6; i++) {
          final double angle = (Math.PI / 3d) * i + (Math.PI / 6d);
          final float vx = cx + (float) (r * Math.cos(angle));
          final float vy = cy + (float) (r * Math.sin(angle));
          if (i == 0) {
            path.moveTo(vx, vy);
          } else {
            path.lineTo(vx, vy);
          }
        }
        path.close();
        canvas.drawPath(path, hexPaint);
      }
    }
    return bitmap;
  }

  @NonNull
  private static Bitmap getTrianglesBitmap() {
    if (cachedTrianglesBitmap != null && !cachedTrianglesBitmap.isRecycled())
      return cachedTrianglesBitmap;
    synchronized (KeyFaceWallpaperProceduralOverlays.class) {
      if (cachedTrianglesBitmap != null && !cachedTrianglesBitmap.isRecycled())
        return cachedTrianglesBitmap;
      cachedTrianglesBitmap = generateTrianglesBitmap();
      return cachedTrianglesBitmap;
    }
  }

  @NonNull
  private static Bitmap generateTrianglesBitmap() {
    final int size = TRIANGLES_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint triPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    triPaint.setStyle(Paint.Style.FILL);
    triPaint.setColor(Color.WHITE);

    final int cells = 2;
    final float step = size / (float) cells;
    for (int y = 0; y < cells; y++) {
      for (int x = 0; x < cells; x++) {
        final float left = x * step;
        final float top = y * step;
        final Path path = new Path();
        if (((x + y) & 1) == 0) {
          path.moveTo(left + (step / 2f), top);
          path.lineTo(left, top + step);
          path.lineTo(left + step, top + step);
        } else {
          path.moveTo(left, top);
          path.lineTo(left + step, top);
          path.lineTo(left + (step / 2f), top + step);
        }
        path.close();
        canvas.drawPath(path, triPaint);
      }
    }
    return bitmap;
  }

  private static void drawKeyFaceOverlayShape(
      @NonNull Canvas canvas,
      @NonNull Drawable keyBackground,
      @NonNull KeyboardKey key,
      boolean matchKeyShape,
      float cornerRadius,
      @NonNull Paint overlayPaint,
      @NonNull KeyFaceOverlayMaskRenderer maskRenderer) {
    if (matchKeyShape) {
      maskRenderer.drawKeyTextureOverlayWithMask(
          canvas, keyBackground, key.width, key.height, overlayPaint);
    } else {
      canvas.drawRoundRect(0f, 0f, key.width, key.height, cornerRadius, cornerRadius, overlayPaint);
    }
  }
}
