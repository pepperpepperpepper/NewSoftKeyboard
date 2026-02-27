package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;

final class KeyFaceWallpaperEffectsRenderer {

  private static final Paint KEY_OVERLAY_BLEND_MULTIPLY =
      createBlendModePaint(PorterDuff.Mode.MULTIPLY);
  private static final Paint KEY_OVERLAY_BLEND_SCREEN =
      createBlendModePaint(PorterDuff.Mode.SCREEN);
  private static final Paint KEY_OVERLAY_BLEND_OVERLAY =
      createBlendModePaint(PorterDuff.Mode.OVERLAY);
  @Nullable private static Paint KEY_OVERLAY_BLEND_SOFT_LIGHT;
  private static final LruCache<Integer, PorterDuffColorFilter> KEY_FACE_TINT_FILTER_CACHE =
      new LruCache<>(8);

  private static final PorterDuffXfermode XFER_MULTIPLY =
      new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
  private static final PorterDuffXfermode XFER_SCREEN =
      new PorterDuffXfermode(PorterDuff.Mode.SCREEN);
  private static final PorterDuffXfermode XFER_OVERLAY =
      new PorterDuffXfermode(PorterDuff.Mode.OVERLAY);

  private final Paint keyFaceColorWashOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final KeyFaceWallpaperGradientShaders gradientShaders =
      new KeyFaceWallpaperGradientShaders();
  private final KeyFaceWallpaperProceduralOverlays proceduralOverlays =
      new KeyFaceWallpaperProceduralOverlays();

  KeyFaceWallpaperEffectsRenderer() {
    keyFaceColorWashOverlayPaint.setStyle(Paint.Style.FILL);
  }

  @Nullable
  Paint resolveKeyOverlayBlendPaint(int blendMode) {
    return switch (blendMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_MULTIPLY ->
          KEY_OVERLAY_BLEND_MULTIPLY;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SCREEN ->
          KEY_OVERLAY_BLEND_SCREEN;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_OVERLAY ->
          KEY_OVERLAY_BLEND_OVERLAY;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SOFT_LIGHT ->
          resolveSoftLightBlendPaint();
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL -> null;
      default -> null;
    };
  }

  void drawKeyboardOverlayStack(
      @NonNull Canvas canvas,
      @NonNull Rect dirtyRect,
      @NonNull Paint overlayPaint,
      @NonNull DrawInputs inputs) {
    canvas.drawRect(dirtyRect, overlayPaint);
    final int overlayAlpha = overlayPaint.getAlpha();

    final KeyboardWallpaperLayer[] layers = inputs.keyFaceWallpaperOverlayLayers;
    if (layers != null) {
      for (KeyboardWallpaperLayer layer : layers) {
        if (layer == null || !layer.enabled()) continue;
        final int opacityPercent = layer.opacityPercent();
        if (opacityPercent <= 0) continue;
        final int layerAlpha = alphaForPercent(opacityPercent);
        final int effectiveAlpha = (overlayAlpha * layerAlpha) / 255;
        if (effectiveAlpha <= 0) continue;

        switch (layer.type()) {
          case KeyboardWallpaperLayer.TYPE_COLOR_WASH, KeyboardWallpaperLayer.TYPE_SOLID_COLOR:
            final Integer argb = layer.argb();
            if (argb == null || ((argb >>> 24) == 0)) continue;
            final int colorAlpha = (argb >>> 24) & 0xFF;
            final int colorEffectiveAlpha = (effectiveAlpha * colorAlpha) / 255;
            if (colorEffectiveAlpha <= 0) continue;
            keyFaceColorWashOverlayPaint.setColor(
                (argb & 0x00FFFFFF) | (colorEffectiveAlpha << 24));
            configureBlendMode(keyFaceColorWashOverlayPaint, layer.blendMode());
            canvas.drawRect(dirtyRect, keyFaceColorWashOverlayPaint);
            break;
          case KeyboardWallpaperLayer.TYPE_DIM:
            keyFaceColorWashOverlayPaint.setColor((effectiveAlpha << 24));
            configureBlendMode(keyFaceColorWashOverlayPaint, layer.blendMode());
            canvas.drawRect(dirtyRect, keyFaceColorWashOverlayPaint);
            break;
          default:
            if (gradientShaders.drawLayerOnRect(canvas, dirtyRect, inputs, layer, effectiveAlpha)) {
              break;
            }
            proceduralOverlays.drawLayerOnRect(canvas, dirtyRect, layer, effectiveAlpha);
            break;
        }
      }
    }
  }

  void drawKeyFacePhotoEffectOverlaysIfAny(
      @NonNull Canvas canvas,
      @NonNull DrawInputs inputs,
      int overlayAlpha,
      boolean matchKeyShape,
      @NonNull KeyboardKey key,
      @NonNull KeyFaceOverlayMaskRenderer maskRenderer) {
    final KeyboardWallpaperLayer[] layers = inputs.keyFaceWallpaperOverlayLayers;
    if (layers == null || layers.length == 0) return;

    ensureUpdated(inputs);

    final float radius =
        KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
            inputs.keyBackground, key.width, key.height);

    for (KeyboardWallpaperLayer layer : layers) {
      if (layer == null || !layer.enabled()) continue;
      final int opacityPercent = layer.opacityPercent();
      if (opacityPercent <= 0) continue;
      final int layerAlpha = alphaForPercent(opacityPercent);
      final int effectiveAlpha = (overlayAlpha * layerAlpha) / 255;
      if (effectiveAlpha <= 0) continue;

      switch (layer.type()) {
        case KeyboardWallpaperLayer.TYPE_COLOR_WASH, KeyboardWallpaperLayer.TYPE_SOLID_COLOR:
          final Integer argb = layer.argb();
          if (argb == null || ((argb >>> 24) == 0)) continue;
          final int colorAlpha = (argb >>> 24) & 0xFF;
          final int colorEffectiveAlpha = (effectiveAlpha * colorAlpha) / 255;
          if (colorEffectiveAlpha <= 0) continue;
          keyFaceColorWashOverlayPaint.setColor((argb & 0x00FFFFFF) | (colorEffectiveAlpha << 24));
          configureBlendMode(keyFaceColorWashOverlayPaint, layer.blendMode());
          drawKeyFaceOverlayShape(
              canvas,
              inputs.keyBackground,
              key,
              matchKeyShape,
              radius,
              keyFaceColorWashOverlayPaint,
              maskRenderer);
          break;
        case KeyboardWallpaperLayer.TYPE_DIM:
          keyFaceColorWashOverlayPaint.setColor((effectiveAlpha << 24));
          configureBlendMode(keyFaceColorWashOverlayPaint, layer.blendMode());
          drawKeyFaceOverlayShape(
              canvas,
              inputs.keyBackground,
              key,
              matchKeyShape,
              radius,
              keyFaceColorWashOverlayPaint,
              maskRenderer);
          break;
        default:
          if (gradientShaders.drawLayerOnKey(
              canvas,
              inputs.keyBackground,
              key,
              matchKeyShape,
              radius,
              inputs,
              layer,
              effectiveAlpha,
              maskRenderer)) {
            break;
          }
          proceduralOverlays.drawLayerOnKey(
              canvas,
              inputs.keyBackground,
              key,
              matchKeyShape,
              radius,
              layer,
              effectiveAlpha,
              maskRenderer);
          break;
      }
    }
  }

  void ensureUpdated(@NonNull DrawInputs inputs) {
    gradientShaders.ensureUpdated(inputs);
  }

  private static int alphaForPercent(int percent) {
    final int clamped = Math.max(0, Math.min(100, percent));
    return Math.round(255f * (clamped / 100f));
  }

  @NonNull
  private static Paint resolveSoftLightBlendPaint() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
      return KEY_OVERLAY_BLEND_OVERLAY;
    }
    final Paint cached = KEY_OVERLAY_BLEND_SOFT_LIGHT;
    if (cached != null) return cached;
    final Paint paint = new Paint();
    paint.setBlendMode(android.graphics.BlendMode.SOFT_LIGHT);
    KEY_OVERLAY_BLEND_SOFT_LIGHT = paint;
    return paint;
  }

  @NonNull
  private static Paint createBlendModePaint(@NonNull PorterDuff.Mode mode) {
    final Paint paint = new Paint();
    paint.setXfermode(new PorterDuffXfermode(mode));
    return paint;
  }

  static void configureBlendMode(@NonNull Paint paint, int blendMode) {
    paint.setXfermode(null);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      paint.setBlendMode(null);
    }
    switch (blendMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_MULTIPLY:
        paint.setXfermode(XFER_MULTIPLY);
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SCREEN:
        paint.setXfermode(XFER_SCREEN);
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_OVERLAY:
        paint.setXfermode(XFER_OVERLAY);
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SOFT_LIGHT:
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          paint.setBlendMode(android.graphics.BlendMode.SOFT_LIGHT);
        } else {
          paint.setXfermode(XFER_OVERLAY);
        }
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL:
      default:
        break;
    }
  }

  @Nullable
  static PorterDuffColorFilter resolveKeyFaceTintColorFilter(@Nullable Integer argb) {
    if (argb == null) return null;
    final PorterDuffColorFilter cached = KEY_FACE_TINT_FILTER_CACHE.get(argb);
    if (cached != null) return cached;
    final PorterDuffColorFilter created = new PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN);
    KEY_FACE_TINT_FILTER_CACHE.put(argb, created);
    return created;
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
