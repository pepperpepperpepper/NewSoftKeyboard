package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import androidx.annotation.NonNull;
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
  }

  void drawKeys(@NonNull Canvas canvas, Rect dirtyRect, DrawInputs inputs) {

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
    final var mask =
        KeyBackgroundAlphaMaskCache.resolveAlphaMask(keyBackground, keyWidth, keyHeight);
    if (mask == null) {
      final float radius =
          KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
              keyBackground, keyWidth, keyHeight);
      canvas.drawRoundRect(0f, 0f, keyWidth, keyHeight, radius, radius, overlayPaint);
      return;
    }

    try {
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
