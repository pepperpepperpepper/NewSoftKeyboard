package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;

final class KeyFaceWallpaperGradientShaders {

  private final Paint keyFaceHighlightOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint keyFaceGradientOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint keyFaceVignetteOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  @Nullable private Shader cachedKeyFaceHighlightShaderVertical;
  @Nullable private Shader cachedKeyFaceHighlightShaderHorizontal;
  @Nullable private Shader cachedKeyFaceHighlightShaderVerticalReverse;
  @Nullable private Shader cachedKeyFaceHighlightShaderHorizontalReverse;
  @Nullable private Shader cachedKeyFaceGradientShaderVertical;
  @Nullable private Shader cachedKeyFaceGradientShaderHorizontal;
  @Nullable private Shader cachedKeyFaceGradientShaderVerticalReverse;
  @Nullable private Shader cachedKeyFaceGradientShaderHorizontalReverse;
  private int cachedKeyFaceHighlightWidth;
  private int cachedKeyFaceHighlightHeight;
  private int cachedKeyFaceGradientWidth;
  private int cachedKeyFaceGradientHeight;
  private int cachedKeyFaceVignetteWidth;
  private int cachedKeyFaceVignetteHeight;
  private int cachedKeyFaceHighlightDirection;
  private int cachedKeyFaceGradientDirection;
  private boolean cachedKeyFaceGradientIsCustomShader;
  private int cachedKeyFaceGradientCustomStartColor = Integer.MIN_VALUE;
  private int cachedKeyFaceGradientCustomEndColor = Integer.MIN_VALUE;
  @Nullable private List<KeyboardWallpaperLayer.GradientStop> cachedKeyFaceGradientCustomStops;
  @Nullable private Shader cachedKeyFaceGradientCustomShaderA;
  private int cachedKeyFaceGradientCustomShaderADirection = Integer.MIN_VALUE;
  private int cachedKeyFaceGradientCustomShaderAStartColor = Integer.MIN_VALUE;
  private int cachedKeyFaceGradientCustomShaderAEndColor = Integer.MIN_VALUE;
  @Nullable private Shader cachedKeyFaceGradientCustomShaderB;
  private int cachedKeyFaceGradientCustomShaderBDirection = Integer.MIN_VALUE;
  private int cachedKeyFaceGradientCustomShaderBStartColor = Integer.MIN_VALUE;
  private int cachedKeyFaceGradientCustomShaderBEndColor = Integer.MIN_VALUE;
  @Nullable private Shader cachedKeyFaceGradientCustomStopsShaderA;
  private int cachedKeyFaceGradientCustomStopsShaderADirection = Integer.MIN_VALUE;

  @Nullable
  private List<KeyboardWallpaperLayer.GradientStop> cachedKeyFaceGradientCustomStopsShaderAStops;

  @Nullable private Shader cachedKeyFaceGradientCustomStopsShaderB;
  private int cachedKeyFaceGradientCustomStopsShaderBDirection = Integer.MIN_VALUE;

  @Nullable
  private List<KeyboardWallpaperLayer.GradientStop> cachedKeyFaceGradientCustomStopsShaderBStops;

  KeyFaceWallpaperGradientShaders() {
    keyFaceHighlightOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceGradientOverlayPaint.setStyle(Paint.Style.FILL);
    keyFaceVignetteOverlayPaint.setStyle(Paint.Style.FILL);
    cachedKeyFaceHighlightDirection = Integer.MIN_VALUE;
    cachedKeyFaceGradientDirection = Integer.MIN_VALUE;
  }

  boolean drawLayerOnRect(
      @NonNull Canvas canvas,
      @NonNull Rect dirtyRect,
      @NonNull DrawInputs inputs,
      @NonNull KeyboardWallpaperLayer layer,
      int effectiveAlpha) {
    switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_HIGHLIGHT:
        ensureUpdated(inputs);
        ensureKeyFaceHighlightDirection(layer.direction());
        keyFaceHighlightOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(layer.argb()));
        keyFaceHighlightOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceHighlightOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceHighlightOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_GRADIENT:
        ensureUpdated(inputs);
        ensureKeyFaceGradientLayer(layer);
        keyFaceGradientOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceGradientOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceGradientOverlayPaint);
        return true;
      case KeyboardWallpaperLayer.TYPE_VIGNETTE:
        ensureUpdated(inputs);
        keyFaceVignetteOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(layer.argb()));
        keyFaceVignetteOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceVignetteOverlayPaint, layer.blendMode());
        canvas.drawRect(dirtyRect, keyFaceVignetteOverlayPaint);
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
      @NonNull DrawInputs inputs,
      @NonNull KeyboardWallpaperLayer layer,
      int effectiveAlpha,
      @NonNull KeyFaceOverlayMaskRenderer maskRenderer) {
    switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_HIGHLIGHT:
        ensureUpdated(inputs);
        ensureKeyFaceHighlightDirection(layer.direction());
        keyFaceHighlightOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(layer.argb()));
        keyFaceHighlightOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceHighlightOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceHighlightOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_GRADIENT:
        ensureUpdated(inputs);
        ensureKeyFaceGradientLayer(layer);
        keyFaceGradientOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceGradientOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceGradientOverlayPaint,
            maskRenderer);
        return true;
      case KeyboardWallpaperLayer.TYPE_VIGNETTE:
        ensureUpdated(inputs);
        keyFaceVignetteOverlayPaint.setColorFilter(
            KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(layer.argb()));
        keyFaceVignetteOverlayPaint.setAlpha(effectiveAlpha);
        KeyFaceWallpaperEffectsRenderer.configureBlendMode(
            keyFaceVignetteOverlayPaint, layer.blendMode());
        drawKeyFaceOverlayShape(
            canvas,
            keyBackground,
            key,
            matchKeyShape,
            cornerRadius,
            keyFaceVignetteOverlayPaint,
            maskRenderer);
        return true;
      default:
        return false;
    }
  }

  void ensureUpdated(@NonNull DrawInputs inputs) {
    final int width = inputs.keyboardViewWidth;
    final int height = inputs.keyboardViewHeight;

    if (width <= 0 || height <= 0) return;

    if (cachedKeyFaceHighlightWidth != width || cachedKeyFaceHighlightHeight != height) {
      cachedKeyFaceHighlightShaderVertical =
          new LinearGradient(
              0f,
              0f,
              0f,
              Math.max(1f, height),
              new int[] {0xFFFFFFFF, 0x00FFFFFF},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceHighlightShaderVerticalReverse =
          new LinearGradient(
              0f,
              0f,
              0f,
              Math.max(1f, height),
              new int[] {0x00FFFFFF, 0xFFFFFFFF},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceHighlightShaderHorizontal =
          new LinearGradient(
              0f,
              0f,
              Math.max(1f, width),
              0f,
              new int[] {0xFFFFFFFF, 0x00FFFFFF},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceHighlightShaderHorizontalReverse =
          new LinearGradient(
              0f,
              0f,
              Math.max(1f, width),
              0f,
              new int[] {0x00FFFFFF, 0xFFFFFFFF},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceHighlightWidth = width;
      cachedKeyFaceHighlightHeight = height;
      cachedKeyFaceHighlightDirection = Integer.MIN_VALUE;
    }

    if (cachedKeyFaceGradientWidth != width || cachedKeyFaceGradientHeight != height) {
      cachedKeyFaceGradientShaderVertical =
          new LinearGradient(
              0f,
              0f,
              0f,
              Math.max(1f, height),
              new int[] {0x00000000, 0xFF000000},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceGradientShaderVerticalReverse =
          new LinearGradient(
              0f,
              0f,
              0f,
              Math.max(1f, height),
              new int[] {0xFF000000, 0x00000000},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceGradientShaderHorizontal =
          new LinearGradient(
              0f,
              0f,
              Math.max(1f, width),
              0f,
              new int[] {0x00000000, 0xFF000000},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceGradientShaderHorizontalReverse =
          new LinearGradient(
              0f,
              0f,
              Math.max(1f, width),
              0f,
              new int[] {0xFF000000, 0x00000000},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
      cachedKeyFaceGradientWidth = width;
      cachedKeyFaceGradientHeight = height;
      cachedKeyFaceGradientDirection = Integer.MIN_VALUE;
      cachedKeyFaceGradientIsCustomShader = false;
      cachedKeyFaceGradientCustomStartColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomEndColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomStops = null;
      cachedKeyFaceGradientCustomShaderA = null;
      cachedKeyFaceGradientCustomShaderADirection = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomShaderAStartColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomShaderAEndColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomShaderB = null;
      cachedKeyFaceGradientCustomShaderBDirection = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomShaderBStartColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomShaderBEndColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomStopsShaderA = null;
      cachedKeyFaceGradientCustomStopsShaderADirection = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomStopsShaderAStops = null;
      cachedKeyFaceGradientCustomStopsShaderB = null;
      cachedKeyFaceGradientCustomStopsShaderBDirection = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomStopsShaderBStops = null;
    }

    if (cachedKeyFaceVignetteWidth != width || cachedKeyFaceVignetteHeight != height) {
      final float cx = width / 2f;
      final float cy = height / 2f;
      final float radius = (float) Math.hypot(width, height) / 2f;
      keyFaceVignetteOverlayPaint.setShader(
          new RadialGradient(
              cx,
              cy,
              Math.max(1f, radius),
              new int[] {0x00000000, 0xFF000000},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP));
      cachedKeyFaceVignetteWidth = width;
      cachedKeyFaceVignetteHeight = height;
    }
  }

  private void ensureKeyFaceHighlightDirection(int direction) {
    final int normalized =
        switch (direction) {
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
              direction;
          case KeyboardWallpaperLayer.DIRECTION_VERTICAL ->
              KeyboardWallpaperLayer.DIRECTION_VERTICAL;
          default -> KeyboardWallpaperLayer.DIRECTION_VERTICAL;
        };
    if (cachedKeyFaceHighlightDirection == normalized) return;
    final Shader shader =
        switch (normalized) {
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL ->
              cachedKeyFaceHighlightShaderHorizontal;
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE ->
              cachedKeyFaceHighlightShaderHorizontalReverse;
          case KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
              cachedKeyFaceHighlightShaderVerticalReverse;
          default -> cachedKeyFaceHighlightShaderVertical;
        };
    if (shader == null) return;
    keyFaceHighlightOverlayPaint.setShader(shader);
    cachedKeyFaceHighlightDirection = normalized;
  }

  private void ensureKeyFaceGradientDirection(int direction) {
    final int normalized =
        switch (direction) {
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
              direction;
          case KeyboardWallpaperLayer.DIRECTION_VERTICAL ->
              KeyboardWallpaperLayer.DIRECTION_VERTICAL;
          default -> KeyboardWallpaperLayer.DIRECTION_VERTICAL;
        };
    if (!cachedKeyFaceGradientIsCustomShader && cachedKeyFaceGradientDirection == normalized)
      return;
    final Shader shader =
        switch (normalized) {
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL -> cachedKeyFaceGradientShaderHorizontal;
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE ->
              cachedKeyFaceGradientShaderHorizontalReverse;
          case KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
              cachedKeyFaceGradientShaderVerticalReverse;
          default -> cachedKeyFaceGradientShaderVertical;
        };
    if (shader == null) return;
    keyFaceGradientOverlayPaint.setShader(shader);
    cachedKeyFaceGradientDirection = normalized;
    cachedKeyFaceGradientIsCustomShader = false;
    cachedKeyFaceGradientCustomStartColor = Integer.MIN_VALUE;
    cachedKeyFaceGradientCustomEndColor = Integer.MIN_VALUE;
    cachedKeyFaceGradientCustomStops = null;
  }

  private void ensureKeyFaceGradientLayer(@NonNull KeyboardWallpaperLayer layer) {
    final List<KeyboardWallpaperLayer.GradientStop> stops = layer.gradientStops();
    if (stops != null && stops.size() >= 2) {
      final int width = cachedKeyFaceGradientWidth;
      final int height = cachedKeyFaceGradientHeight;
      if (width <= 0 || height <= 0) return;

      final int direction =
          switch (layer.direction()) {
            case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
                KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
                KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
                layer.direction();
            case KeyboardWallpaperLayer.DIRECTION_VERTICAL ->
                KeyboardWallpaperLayer.DIRECTION_VERTICAL;
            default -> KeyboardWallpaperLayer.DIRECTION_VERTICAL;
          };
      if (cachedKeyFaceGradientIsCustomShader
          && cachedKeyFaceGradientDirection == direction
          && cachedKeyFaceGradientCustomStops != null
          && cachedKeyFaceGradientCustomStops.equals(stops)) {
        return;
      }

      final Shader shader =
          resolveOrCreateKeyFaceGradientCustomStopsShader(direction, width, height, stops);
      if (shader == null) return;
      keyFaceGradientOverlayPaint.setShader(shader);
      keyFaceGradientOverlayPaint.setColorFilter(null);
      cachedKeyFaceGradientIsCustomShader = true;
      cachedKeyFaceGradientDirection = direction;
      cachedKeyFaceGradientCustomStops = stops;
      cachedKeyFaceGradientCustomStartColor = Integer.MIN_VALUE;
      cachedKeyFaceGradientCustomEndColor = Integer.MIN_VALUE;
      return;
    }

    final Integer startArgbOrNull = layer.argb2();
    if (startArgbOrNull == null) {
      ensureKeyFaceGradientDirection(layer.direction());
      keyFaceGradientOverlayPaint.setColorFilter(
          KeyFaceWallpaperEffectsRenderer.resolveKeyFaceTintColorFilter(layer.argb()));
      return;
    }

    final int width = cachedKeyFaceGradientWidth;
    final int height = cachedKeyFaceGradientHeight;
    if (width <= 0 || height <= 0) return;

    final int direction =
        switch (layer.direction()) {
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
              layer.direction();
          case KeyboardWallpaperLayer.DIRECTION_VERTICAL ->
              KeyboardWallpaperLayer.DIRECTION_VERTICAL;
          default -> KeyboardWallpaperLayer.DIRECTION_VERTICAL;
        };
    final int startArgb = startArgbOrNull;
    final int endArgb = layer.argb() != null ? layer.argb() : Color.BLACK;
    if (cachedKeyFaceGradientIsCustomShader
        && cachedKeyFaceGradientDirection == direction
        && cachedKeyFaceGradientCustomStartColor == startArgb
        && cachedKeyFaceGradientCustomEndColor == endArgb) {
      return;
    }

    final Shader shader =
        resolveOrCreateKeyFaceGradientCustomShader(direction, width, height, startArgb, endArgb);
    if (shader == null) return;
    keyFaceGradientOverlayPaint.setShader(shader);
    keyFaceGradientOverlayPaint.setColorFilter(null);
    cachedKeyFaceGradientIsCustomShader = true;
    cachedKeyFaceGradientDirection = direction;
    cachedKeyFaceGradientCustomStartColor = startArgb;
    cachedKeyFaceGradientCustomEndColor = endArgb;
    cachedKeyFaceGradientCustomStops = null;
  }

  @Nullable
  private Shader resolveOrCreateKeyFaceGradientCustomShader(
      int direction, int width, int height, int startArgb, int endArgb) {
    final Shader cachedA = cachedKeyFaceGradientCustomShaderA;
    if (cachedA != null
        && cachedKeyFaceGradientCustomShaderADirection == direction
        && cachedKeyFaceGradientCustomShaderAStartColor == startArgb
        && cachedKeyFaceGradientCustomShaderAEndColor == endArgb) {
      return cachedA;
    }

    final Shader cachedB = cachedKeyFaceGradientCustomShaderB;
    if (cachedB != null
        && cachedKeyFaceGradientCustomShaderBDirection == direction
        && cachedKeyFaceGradientCustomShaderBStartColor == startArgb
        && cachedKeyFaceGradientCustomShaderBEndColor == endArgb) {
      cachedKeyFaceGradientCustomShaderB = cachedA;
      cachedKeyFaceGradientCustomShaderBDirection = cachedKeyFaceGradientCustomShaderADirection;
      cachedKeyFaceGradientCustomShaderBStartColor = cachedKeyFaceGradientCustomShaderAStartColor;
      cachedKeyFaceGradientCustomShaderBEndColor = cachedKeyFaceGradientCustomShaderAEndColor;

      cachedKeyFaceGradientCustomShaderA = cachedB;
      cachedKeyFaceGradientCustomShaderADirection = direction;
      cachedKeyFaceGradientCustomShaderAStartColor = startArgb;
      cachedKeyFaceGradientCustomShaderAEndColor = endArgb;
      return cachedB;
    }

    final Shader created =
        createKeyFaceColoredGradientShader(direction, width, height, startArgb, endArgb);
    cachedKeyFaceGradientCustomShaderB = cachedA;
    cachedKeyFaceGradientCustomShaderBDirection = cachedKeyFaceGradientCustomShaderADirection;
    cachedKeyFaceGradientCustomShaderBStartColor = cachedKeyFaceGradientCustomShaderAStartColor;
    cachedKeyFaceGradientCustomShaderBEndColor = cachedKeyFaceGradientCustomShaderAEndColor;

    cachedKeyFaceGradientCustomShaderA = created;
    cachedKeyFaceGradientCustomShaderADirection = direction;
    cachedKeyFaceGradientCustomShaderAStartColor = startArgb;
    cachedKeyFaceGradientCustomShaderAEndColor = endArgb;
    return created;
  }

  @Nullable
  private Shader resolveOrCreateKeyFaceGradientCustomStopsShader(
      int direction,
      int width,
      int height,
      @NonNull List<KeyboardWallpaperLayer.GradientStop> stops) {
    final Shader cachedA = cachedKeyFaceGradientCustomStopsShaderA;
    if (cachedA != null
        && cachedKeyFaceGradientCustomStopsShaderADirection == direction
        && cachedKeyFaceGradientCustomStopsShaderAStops != null
        && cachedKeyFaceGradientCustomStopsShaderAStops.equals(stops)) {
      return cachedA;
    }

    final Shader cachedB = cachedKeyFaceGradientCustomStopsShaderB;
    if (cachedB != null
        && cachedKeyFaceGradientCustomStopsShaderBDirection == direction
        && cachedKeyFaceGradientCustomStopsShaderBStops != null
        && cachedKeyFaceGradientCustomStopsShaderBStops.equals(stops)) {
      cachedKeyFaceGradientCustomStopsShaderB = cachedA;
      cachedKeyFaceGradientCustomStopsShaderBDirection =
          cachedKeyFaceGradientCustomStopsShaderADirection;
      cachedKeyFaceGradientCustomStopsShaderBStops = cachedKeyFaceGradientCustomStopsShaderAStops;

      cachedKeyFaceGradientCustomStopsShaderA = cachedB;
      cachedKeyFaceGradientCustomStopsShaderADirection = direction;
      cachedKeyFaceGradientCustomStopsShaderAStops = stops;
      return cachedB;
    }

    final Shader created = createKeyFaceMultiStopGradientShader(direction, width, height, stops);
    cachedKeyFaceGradientCustomStopsShaderB = cachedA;
    cachedKeyFaceGradientCustomStopsShaderBDirection =
        cachedKeyFaceGradientCustomStopsShaderADirection;
    cachedKeyFaceGradientCustomStopsShaderBStops = cachedKeyFaceGradientCustomStopsShaderAStops;

    cachedKeyFaceGradientCustomStopsShaderA = created;
    cachedKeyFaceGradientCustomStopsShaderADirection = direction;
    cachedKeyFaceGradientCustomStopsShaderAStops = stops;
    return created;
  }

  @NonNull
  private static LinearGradient createKeyFaceColoredGradientShader(
      int direction, int width, int height, int startArgb, int endArgb) {
    final float safeWidth = Math.max(1f, width);
    final float safeHeight = Math.max(1f, height);
    final float x0;
    final float y0;
    final float x1;
    final float y1;
    switch (direction) {
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL:
        x0 = 0f;
        y0 = 0f;
        x1 = safeWidth;
        y1 = 0f;
        break;
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE:
        x0 = safeWidth;
        y0 = 0f;
        x1 = 0f;
        y1 = 0f;
        break;
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE:
        x0 = 0f;
        y0 = safeHeight;
        x1 = 0f;
        y1 = 0f;
        break;
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL:
      default:
        x0 = 0f;
        y0 = 0f;
        x1 = 0f;
        y1 = safeHeight;
        break;
    }
    return new LinearGradient(
        x0,
        y0,
        x1,
        y1,
        new int[] {startArgb, endArgb},
        new float[] {0f, 1f},
        Shader.TileMode.CLAMP);
  }

  @NonNull
  private static LinearGradient createKeyFaceMultiStopGradientShader(
      int direction,
      int width,
      int height,
      @NonNull List<KeyboardWallpaperLayer.GradientStop> stops) {
    final float safeWidth = Math.max(1f, width);
    final float safeHeight = Math.max(1f, height);
    final float x0;
    final float y0;
    final float x1;
    final float y1;
    switch (direction) {
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL:
        x0 = 0f;
        y0 = 0f;
        x1 = safeWidth;
        y1 = 0f;
        break;
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE:
        x0 = safeWidth;
        y0 = 0f;
        x1 = 0f;
        y1 = 0f;
        break;
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE:
        x0 = 0f;
        y0 = safeHeight;
        x1 = 0f;
        y1 = 0f;
        break;
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL:
      default:
        x0 = 0f;
        y0 = 0f;
        x1 = 0f;
        y1 = safeHeight;
        break;
    }

    // Ensure positions are sorted (LinearGradient requires monotonic positions).
    final java.util.List<KeyboardWallpaperLayer.GradientStop> sortedStops;
    boolean sorted = true;
    int lastPos = Integer.MIN_VALUE;
    for (KeyboardWallpaperLayer.GradientStop stop : stops) {
      if (stop == null) continue;
      final int pos = stop.positionPercent();
      if (pos < lastPos) {
        sorted = false;
        break;
      }
      lastPos = pos;
    }
    if (sorted) {
      sortedStops = stops;
    } else {
      final java.util.ArrayList<KeyboardWallpaperLayer.GradientStop> copy =
          new java.util.ArrayList<>(stops);
      copy.sort(
          java.util.Comparator.comparingInt(KeyboardWallpaperLayer.GradientStop::positionPercent));
      sortedStops = copy;
    }

    final int count = sortedStops.size();
    final int[] colors = new int[count];
    final float[] positions = new float[count];
    for (int i = 0; i < count; i++) {
      final KeyboardWallpaperLayer.GradientStop stop = sortedStops.get(i);
      if (stop == null) {
        colors[i] = Color.TRANSPARENT;
        positions[i] = 0f;
      } else {
        colors[i] = stop.argb();
        positions[i] = Math.max(0f, Math.min(1f, stop.positionPercent() / 100f));
      }
    }

    return new LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP);
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
