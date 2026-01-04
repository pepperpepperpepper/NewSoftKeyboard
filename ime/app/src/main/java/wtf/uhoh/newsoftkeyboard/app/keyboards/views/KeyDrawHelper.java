package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import android.text.TextPaint;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

/**
 * Renders keys for {@link KeyboardViewBase}. Keeps the heavy draw loop out of the view class while
 * delegating to existing helpers for icons, labels, hints, and colors.
 */
final class KeyDrawHelper {

  private final Paint paint;
  private final DrawDecisions drawDecisions;
  private final KeyIconDrawer keyIconDrawer;
  private final KeyIconResolver keyIconResolver;
  private final Rect keyBackgroundPadding;
  private final KeyboardNameRenderer keyboardNameRenderer;
  private final KeyLabelRenderer keyLabelRenderer;
  private final KeyHintRenderer keyHintRenderer;
  private final LabelPaintConfigurator labelPaintConfigurator;
  private final KeyLabelRenderer.KeyTextPaintSetter keyTextPaintSetter;
  private final KeyLabelRenderer.LabelTextPaintSetter labelTextPaintSetter;
  private final KeyIconDrawer.KeyLabelGuesser keyLabelGuesser;
  private final Paint voiceBadgeBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final TextPaint voiceBadgeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private final RectF voiceBadgeRect = new RectF();
  private final Paint.FontMetrics voiceBadgeFontMetrics = new Paint.FontMetrics();

  private final Paint keyFaceUnionMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF keyFaceUnionMaskRect = new RectF();
  private final Canvas keyFaceUnionMaskCanvas = new Canvas();
  @Nullable private Bitmap cachedKeyFaceUnionMask;
  private int cachedKeyFaceUnionMaskWidth;
  private int cachedKeyFaceUnionMaskHeight;
  private int cachedKeyFaceUnionMaskPaddingLeft;
  private int cachedKeyFaceUnionMaskPaddingTop;
  @Nullable private Object cachedKeyFaceUnionMaskDrawableKey;
  @Nullable private Object cachedKeyFaceUnionMaskKeyboardKey;
  private int cachedKeyFaceUnionMaskKeyCount;

  KeyDrawHelper(
      Paint paint,
      DrawDecisions drawDecisions,
      KeyIconDrawer keyIconDrawer,
      KeyIconResolver keyIconResolver,
      Rect keyBackgroundPadding,
      KeyboardNameRenderer keyboardNameRenderer,
      KeyLabelRenderer keyLabelRenderer,
      KeyHintRenderer keyHintRenderer,
      LabelPaintConfigurator labelPaintConfigurator,
      KeyLabelRenderer.KeyTextPaintSetter keyTextPaintSetter,
      KeyLabelRenderer.LabelTextPaintSetter labelTextPaintSetter,
      KeyIconDrawer.KeyLabelGuesser keyLabelGuesser) {
    this.paint = paint;
    this.drawDecisions = drawDecisions;
    this.keyIconDrawer = keyIconDrawer;
    this.keyIconResolver = keyIconResolver;
    this.keyBackgroundPadding = keyBackgroundPadding;
    this.keyboardNameRenderer = keyboardNameRenderer;
    this.keyLabelRenderer = keyLabelRenderer;
    this.keyHintRenderer = keyHintRenderer;
    this.labelPaintConfigurator = labelPaintConfigurator;
    this.keyTextPaintSetter = keyTextPaintSetter;
    this.labelTextPaintSetter = labelTextPaintSetter;
    this.keyLabelGuesser = keyLabelGuesser;

    voiceBadgeBackgroundPaint.setColor(Color.argb(160, 0, 0, 0));
    voiceBadgeTextPaint.setColor(Color.WHITE);
    voiceBadgeTextPaint.setTextAlign(Paint.Align.CENTER);
    voiceBadgeTextPaint.setFakeBoldText(true);

    keyFaceUnionMaskPaint.setColor(Color.BLACK);
    keyFaceUnionMaskPaint.setStyle(Paint.Style.FILL);
  }

  void drawKeys(@NonNull Canvas canvas, Rect dirtyRect, DrawInputs inputs) {
    final boolean canUseKeyboardMaskOverlay =
        inputs.keyFaceWallpaperOverlayPaint != null
            && !inputs.drawSingleKey
            && inputs.keyFaceWallpaperOverlayMode
                == KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE
            && inputs.keyFaceWallpaperOverlayMatchKeyShape;
    if (canUseKeyboardMaskOverlay) {
      drawKeysWithOptimizedKeyTextureOverlay(canvas, dirtyRect, inputs);
      return;
    }

    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;
      final boolean keyIsSpace = key.getPrimaryCode() == KeyCodes.SPACE;

      if (inputs.drawSingleKey && (inputs.invalidKey != key)) {
        continue;
      }
      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);

      int resolvedTextColor =
          drawDecisions.resolveTextColor(
              key,
              inputs.themeResourcesHolder,
              inputs.keyTextColor,
              keyIsSpace,
              inputs.modifierStates.functionModeActive,
              inputs.modifierStates.controlModeActive,
              inputs.modifierStates.altModeActive,
              inputs.modifierActiveTextColor,
              inputs.drawableStatesProvider);

      paint.setColor(resolvedTextColor);
      inputs.keyBackground.setState(drawableState);

      CharSequence label =
          key.label == null
              ? null
              : KeyLabelAdjuster.adjustLabelToShiftState(
                  inputs.keyboard,
                  inputs.keyDetector,
                  inputs.textCaseForceOverrideType,
                  inputs.textCaseType,
                  key);
      label = KeyLabelAdjuster.adjustLabelForFunctionState(inputs.keyboard, key, label);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);
      inputs.keyBackground.draw(canvas);

      if (inputs.keyFaceWallpaperOverlayPaint != null) {
        try {
          switch (inputs.keyFaceWallpaperOverlayMode) {
            case KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TINT:
              canvas.drawRect(0f, 0f, key.width, key.height, inputs.keyFaceWallpaperOverlayPaint);
              break;
            case KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE:
              if (inputs.keyFaceWallpaperOverlayMatchKeyShape) {
                drawKeyTextureOverlayWithMask(
                    canvas,
                    inputs.keyBackground,
                    key.width,
                    key.height,
                    inputs.keyFaceWallpaperOverlayPaint);
              } else {
                final float radius =
                    KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
                        inputs.keyBackground, key.width, key.height);
                canvas.drawRoundRect(
                    0f,
                    0f,
                    key.width,
                    key.height,
                    radius,
                    radius,
                    inputs.keyFaceWallpaperOverlayPaint);
              }
              break;
            case KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY:
            default:
              break;
          }
        } catch (RuntimeException | OutOfMemoryError ignored) {
          // Key wallpaper overlays should never crash the IME. If a renderer or device cannot
          // handle the shader/layer/mask combination, skip the overlay for this draw pass.
        }
      }

      label =
          keyIconDrawer.drawIconIfNeeded(
              canvas,
              key,
              drawableState,
              keyIconResolver,
              label,
              keyBackgroundPadding,
              keyLabelGuesser);

      label =
          keyboardNameRenderer.applyKeyboardNameIfNeeded(
              label, keyIsSpace, inputs.drawKeyboardNameText, inputs.keyboardName);

      if (label != null) {
        keyLabelRenderer.drawLabel(
            canvas,
            paint,
            label,
            key,
            keyBackgroundPadding,
            keyIsSpace,
            inputs.keyboardNameTextSize,
            keyboardNameRenderer,
            inputs.alwaysUseDrawText,
            keyTextPaintSetter,
            labelTextPaintSetter,
            (p, l, width) ->
                labelPaintConfigurator.adjustTextSizeForLabel(
                    p, l, width, keyIsSpace ? inputs.keyboardNameTextSize : inputs.keyTextSize),
            inputs.shadowRadius,
            inputs.shadowOffsetX,
            inputs.shadowOffsetY,
            inputs.shadowColor);
      }

      if (inputs.drawHintText
          && (inputs.hintTextSizeMultiplier > 0)
          && ((key.popupCharacters != null && key.popupCharacters.length() > 0)
              || (key.popupResId != 0)
              || (key.popupKeyboardPackPath != null && key.popupKeyboardPackPath.length() > 0)
              || (key.longPressCode != 0))) {
        Paint.Align oldAlign = paint.getTextAlign();
        keyHintRenderer.drawHint(
            canvas,
            paint,
            key,
            inputs.themeResourcesHolder,
            keyBackgroundPadding,
            inputs.hintAlign,
            inputs.hintVAlign,
            inputs.hintTextSize,
            inputs.hintTextSizeMultiplier,
            inputs.keyboardShifted,
            inputs.keyboardLocale);
        paint.setTextAlign(oldAlign);
      }

      if (keyIsSpace && inputs.spacebarVoiceBadgeText != null) {
        drawSpacebarVoiceBadge(canvas, key, inputs.spacebarVoiceBadgeText);
      }

      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }
  }

  private void drawKeysWithOptimizedKeyTextureOverlay(
      @NonNull Canvas canvas, @NonNull Rect dirtyRect, DrawInputs inputs) {
    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;

      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);
      inputs.keyBackground.setState(drawableState);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);
      inputs.keyBackground.draw(canvas);
      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }

    boolean overlayApplied = false;
    Trace.beginSection("NSK.WallpaperKeyOverlay");
    try {
      try {
        Trace.beginSection("NSK.WallpaperKeyOverlayMask");
        final Bitmap unionMask;
        try {
          unionMask = ensureKeyFaceUnionMask(inputs);
        } finally {
          Trace.endSection();
        }
        if (unionMask != null
            && !unionMask.isRecycled()
            && inputs.keyFaceWallpaperOverlayPaint != null) {
          Trace.beginSection("NSK.WallpaperKeyOverlayDraw");
          try {
            overlayApplied =
                drawKeyboardTextureOverlayWithMask(
                    canvas, dirtyRect, unionMask, inputs.keyFaceWallpaperOverlayPaint);
          } finally {
            Trace.endSection();
          }
        }
      } catch (RuntimeException | OutOfMemoryError ignored) {
        overlayApplied = false;
      }
    } finally {
      Trace.endSection();
    }

    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;
      final boolean keyIsSpace = key.getPrimaryCode() == KeyCodes.SPACE;

      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);

      int resolvedTextColor =
          drawDecisions.resolveTextColor(
              key,
              inputs.themeResourcesHolder,
              inputs.keyTextColor,
              keyIsSpace,
              inputs.modifierStates.functionModeActive,
              inputs.modifierStates.controlModeActive,
              inputs.modifierStates.altModeActive,
              inputs.modifierActiveTextColor,
              inputs.drawableStatesProvider);

      paint.setColor(resolvedTextColor);
      inputs.keyBackground.setState(drawableState);

      CharSequence label =
          key.label == null
              ? null
              : KeyLabelAdjuster.adjustLabelToShiftState(
                  inputs.keyboard,
                  inputs.keyDetector,
                  inputs.textCaseForceOverrideType,
                  inputs.textCaseType,
                  key);
      label = KeyLabelAdjuster.adjustLabelForFunctionState(inputs.keyboard, key, label);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);

      if (!overlayApplied && inputs.keyFaceWallpaperOverlayPaint != null) {
        try {
          drawKeyTextureOverlayWithMask(
              canvas,
              inputs.keyBackground,
              key.width,
              key.height,
              inputs.keyFaceWallpaperOverlayPaint);
        } catch (RuntimeException | OutOfMemoryError ignored) {
          // If the fallback path also fails, keep going without overlays.
        }
      }

      label =
          keyIconDrawer.drawIconIfNeeded(
              canvas,
              key,
              drawableState,
              keyIconResolver,
              label,
              keyBackgroundPadding,
              keyLabelGuesser);

      label =
          keyboardNameRenderer.applyKeyboardNameIfNeeded(
              label, keyIsSpace, inputs.drawKeyboardNameText, inputs.keyboardName);

      if (label != null) {
        keyLabelRenderer.drawLabel(
            canvas,
            paint,
            label,
            key,
            keyBackgroundPadding,
            keyIsSpace,
            inputs.keyboardNameTextSize,
            keyboardNameRenderer,
            inputs.alwaysUseDrawText,
            keyTextPaintSetter,
            labelTextPaintSetter,
            (p, l, width) ->
                labelPaintConfigurator.adjustTextSizeForLabel(
                    p, l, width, keyIsSpace ? inputs.keyboardNameTextSize : inputs.keyTextSize),
            inputs.shadowRadius,
            inputs.shadowOffsetX,
            inputs.shadowOffsetY,
            inputs.shadowColor);
      }

      if (inputs.drawHintText
          && (inputs.hintTextSizeMultiplier > 0)
          && ((key.popupCharacters != null && key.popupCharacters.length() > 0)
              || (key.popupResId != 0)
              || (key.popupKeyboardPackPath != null && key.popupKeyboardPackPath.length() > 0)
              || (key.longPressCode != 0))) {
        Paint.Align oldAlign = paint.getTextAlign();
        keyHintRenderer.drawHint(
            canvas,
            paint,
            key,
            inputs.themeResourcesHolder,
            keyBackgroundPadding,
            inputs.hintAlign,
            inputs.hintVAlign,
            inputs.hintTextSize,
            inputs.hintTextSizeMultiplier,
            inputs.keyboardShifted,
            inputs.keyboardLocale);
        paint.setTextAlign(oldAlign);
      }

      if (keyIsSpace && inputs.spacebarVoiceBadgeText != null) {
        drawSpacebarVoiceBadge(canvas, key, inputs.spacebarVoiceBadgeText);
      }

      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }
  }

  @Nullable
  private Bitmap ensureKeyFaceUnionMask(@NonNull DrawInputs inputs) {
    final int width = inputs.keyboardViewWidth;
    final int height = inputs.keyboardViewHeight;
    if (width <= 0 || height <= 0) return null;

    final Object drawableKey =
        inputs.keyBackground.getConstantState() != null
            ? inputs.keyBackground.getConstantState()
            : inputs.keyBackground;
    final Object keyboardKey = inputs.keyboard;
    final int keyCount = inputs.keys != null ? inputs.keys.length : 0;

    final Bitmap cached = cachedKeyFaceUnionMask;
    if (cached != null
        && !cached.isRecycled()
        && cachedKeyFaceUnionMaskWidth == width
        && cachedKeyFaceUnionMaskHeight == height
        && cachedKeyFaceUnionMaskPaddingLeft == inputs.kbdPaddingLeft
        && cachedKeyFaceUnionMaskPaddingTop == inputs.kbdPaddingTop
        && cachedKeyFaceUnionMaskDrawableKey == drawableKey
        && cachedKeyFaceUnionMaskKeyboardKey == keyboardKey
        && cachedKeyFaceUnionMaskKeyCount == keyCount) {
      return cached;
    }

    Trace.beginSection("NSK.KeyFaceUnionMaskBuild");
    try {
      final Bitmap mask;
      try {
        if (cached != null
            && !cached.isRecycled()
            && cached.getWidth() == width
            && cached.getHeight() == height) {
          mask = cached;
        } else {
          if (cached != null && !cached.isRecycled()) cached.recycle();
          mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        }
      } catch (OutOfMemoryError oom) {
        cachedKeyFaceUnionMask = null;
        return null;
      }

      mask.eraseColor(Color.TRANSPARENT);
      keyFaceUnionMaskCanvas.setBitmap(mask);

      try {
        for (Keyboard.Key keyBase : inputs.keys) {
          final KeyboardKey key = (KeyboardKey) keyBase;
          if (key.width <= 0 || key.height <= 0) continue;

          final int left = key.x + inputs.kbdPaddingLeft;
          final int top = key.y + inputs.kbdPaddingTop;

          final Bitmap keyMask =
              KeyBackgroundAlphaMaskCache.resolveAlphaMask(
                  inputs.keyBackground, key.width, key.height);
          if (keyMask != null && !keyMask.isRecycled()) {
            keyFaceUnionMaskCanvas.drawBitmap(keyMask, left, top, null);
          } else {
            final float radius =
                KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
                    inputs.keyBackground, key.width, key.height);
            keyFaceUnionMaskRect.set(left, top, left + key.width, top + key.height);
            keyFaceUnionMaskCanvas.drawRoundRect(
                keyFaceUnionMaskRect, radius, radius, keyFaceUnionMaskPaint);
          }
        }
      } catch (RuntimeException | OutOfMemoryError e) {
        return null;
      }

      cachedKeyFaceUnionMask = mask;
      cachedKeyFaceUnionMaskWidth = width;
      cachedKeyFaceUnionMaskHeight = height;
      cachedKeyFaceUnionMaskPaddingLeft = inputs.kbdPaddingLeft;
      cachedKeyFaceUnionMaskPaddingTop = inputs.kbdPaddingTop;
      cachedKeyFaceUnionMaskDrawableKey = drawableKey;
      cachedKeyFaceUnionMaskKeyboardKey = keyboardKey;
      cachedKeyFaceUnionMaskKeyCount = keyCount;

      return mask;
    } finally {
      Trace.endSection();
    }
  }

  private static boolean drawKeyboardTextureOverlayWithMask(
      @NonNull Canvas canvas,
      @NonNull Rect dirtyRect,
      @NonNull Bitmap unionMask,
      @NonNull Paint overlayPaint) {
    if (dirtyRect.isEmpty()) return false;
    final int saveCount =
        canvas.saveLayer(dirtyRect.left, dirtyRect.top, dirtyRect.right, dirtyRect.bottom, null);
    canvas.drawRect(dirtyRect, overlayPaint);
    canvas.drawBitmap(unionMask, 0f, 0f, KeyBackgroundAlphaMaskCache.dstInPaint());
    canvas.restoreToCount(saveCount);
    return true;
  }

  private void drawSpacebarVoiceBadge(
      @NonNull Canvas canvas, @NonNull KeyboardKey key, @NonNull CharSequence rawText) {
    final float margin = Math.max(2f, key.height * 0.06f);
    final float badgeHeight = Math.max(12f, key.height * 0.28f);
    final float maxBadgeWidth = Math.max(24f, key.width * 0.5f);
    final float textSize = badgeHeight * 0.55f;

    voiceBadgeTextPaint.setTextSize(textSize);
    final String text =
        TextUtils.ellipsize(
                rawText, voiceBadgeTextPaint, maxBadgeWidth - badgeHeight, TextUtils.TruncateAt.END)
            .toString();
    final float textWidth = voiceBadgeTextPaint.measureText(text);
    final float badgeWidth = Math.min(maxBadgeWidth, textWidth + badgeHeight);

    final float right = key.width - margin;
    final float left = Math.max(margin, right - badgeWidth);
    final float top = margin;
    final float bottom = top + badgeHeight;

    voiceBadgeRect.set(left, top, right, bottom);
    final float radius = badgeHeight / 2f;
    canvas.drawRoundRect(voiceBadgeRect, radius, radius, voiceBadgeBackgroundPaint);

    voiceBadgeTextPaint.getFontMetrics(voiceBadgeFontMetrics);
    final float textCenterY =
        top + (badgeHeight - voiceBadgeFontMetrics.ascent - voiceBadgeFontMetrics.descent) / 2f;
    canvas.drawText(text, voiceBadgeRect.centerX(), textCenterY, voiceBadgeTextPaint);
  }

  private static void drawKeyTextureOverlayWithMask(
      @NonNull Canvas canvas,
      @NonNull Drawable keyBackground,
      int keyWidth,
      int keyHeight,
      @NonNull Paint overlayPaint) {
    try {
      final var mask =
          KeyBackgroundAlphaMaskCache.resolveAlphaMask(keyBackground, keyWidth, keyHeight);
      if (mask == null) {
        final float radius =
            KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
                keyBackground, keyWidth, keyHeight);
        canvas.drawRoundRect(0f, 0f, keyWidth, keyHeight, radius, radius, overlayPaint);
        return;
      }

      final int saveCount = canvas.saveLayer(0f, 0f, keyWidth, keyHeight, null);
      canvas.drawRect(0f, 0f, keyWidth, keyHeight, overlayPaint);
      canvas.drawBitmap(mask, 0f, 0f, KeyBackgroundAlphaMaskCache.dstInPaint());
      canvas.restoreToCount(saveCount);
    } catch (RuntimeException | OutOfMemoryError e) {
      // Worst case this feature should gracefully degrade. Some renderers/devices have issues with
      // saveLayer + xfer modes. Fall back to a rounded-rect overlay instead of crashing the IME.
      final float radius =
          KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
              keyBackground, keyWidth, keyHeight);
      canvas.drawRoundRect(0f, 0f, keyWidth, keyHeight, radius, radius, overlayPaint);
    }
  }
}
