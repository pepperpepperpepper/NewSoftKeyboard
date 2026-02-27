package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;

/**
 * Renders keys for {@link KeyboardViewBase}. Keeps the heavy draw loop out of the view class while
 * delegating to existing helpers for icons, labels, hints, and colors.
 */
final class KeyDrawHelper {

  private static final PerKeyTextShadowOverrides.Overrides EMPTY_TEXT_SHADOW_OVERRIDES =
      new PerKeyTextShadowOverrides.Overrides();

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
  private final KeyLabelRenderer.KeyboardNamePaintSetter keyboardNamePaintSetter;
  private final KeyLabelRenderer.LabelTextPaintSetter labelTextPaintSetter;
  private final KeyHintRenderer.HintTextPaintSetter hintTextPaintSetter;
  private final KeyIconDrawer.KeyLabelGuesser keyLabelGuesser;
  private final KeyBackgroundRenderer keyBackgroundRenderer = new KeyBackgroundRenderer();
  private final Paint voiceBadgeBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final TextPaint voiceBadgeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private final RectF voiceBadgeRect = new RectF();
  private final Paint.FontMetrics voiceBadgeFontMetrics = new Paint.FontMetrics();
  private final KeyFaceOverlayMaskRenderer overlayMaskRenderer = new KeyFaceOverlayMaskRenderer();
  private final KeyFaceWallpaperOverlayRenderer keyFaceWallpaperOverlayRenderer =
      new KeyFaceWallpaperOverlayRenderer(overlayMaskRenderer);

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
      KeyLabelRenderer.KeyboardNamePaintSetter keyboardNamePaintSetter,
      KeyLabelRenderer.LabelTextPaintSetter labelTextPaintSetter,
      KeyHintRenderer.HintTextPaintSetter hintTextPaintSetter,
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
    this.keyboardNamePaintSetter = keyboardNamePaintSetter;
    this.labelTextPaintSetter = labelTextPaintSetter;
    this.hintTextPaintSetter = hintTextPaintSetter;
    this.keyLabelGuesser = keyLabelGuesser;

    voiceBadgeBackgroundPaint.setColor(Color.argb(160, 0, 0, 0));
    voiceBadgeTextPaint.setColor(Color.WHITE);
    voiceBadgeTextPaint.setTextAlign(Paint.Align.CENTER);
    voiceBadgeTextPaint.setFakeBoldText(true);
  }

  private static boolean containsState(int[] drawableState, int stateAttrId) {
    for (int state : drawableState) {
      if (state == stateAttrId) return true;
    }
    return false;
  }

  static boolean isSpecialKey(int primaryCode, int[] drawableState, DrawInputs inputs) {
    if (primaryCode == KeyCodes.SPACE) return false;
    final int actionAttrId = inputs.drawableStatesProvider.KEY_STATE_ACTION_NORMAL[0];
    final int functionalAttrId = inputs.drawableStatesProvider.KEY_STATE_FUNCTIONAL_NORMAL[0];
    return containsState(drawableState, actionAttrId)
        || containsState(drawableState, functionalAttrId);
  }

  static boolean isEnterKey(int primaryCode) {
    return primaryCode == KeyCodes.ENTER;
  }

  static boolean isModifierKey(@NonNull KeyboardKey key) {
    if (key.modifier) return true;
    return switch (key.getPrimaryCode()) {
      case KeyCodes.SHIFT,
          KeyCodes.SHIFT_LOCK,
          KeyCodes.ALT,
          KeyCodes.ALT_MODIFIER,
          KeyCodes.CTRL,
          KeyCodes.CTRL_LOCK,
          KeyCodes.FUNCTION ->
          true;
      default -> false;
    };
  }

  @NonNull
  private static PerKeyTextShadowOverrides.Overrides resolveTextShadowOverridesForKey(
      @NonNull KeyboardKey key,
      boolean keyIsSpace,
      int primaryCode,
      int[] drawableState,
      @NonNull DrawInputs inputs) {
    if (keyIsSpace) return inputs.perKeyTextShadowOverrides.spacebar();

    if (isEnterKey(primaryCode)) {
      final var override = inputs.perKeyTextShadowOverrides.enter();
      return override.isEmpty() ? inputs.perKeyTextShadowOverrides.special() : override;
    }

    if (isModifierKey(key)) {
      final var override = inputs.perKeyTextShadowOverrides.modifier();
      return override.isEmpty() ? inputs.perKeyTextShadowOverrides.special() : override;
    }

    if (isSpecialKey(primaryCode, drawableState, inputs)) {
      return inputs.perKeyTextShadowOverrides.special();
    }

    return EMPTY_TEXT_SHADOW_OVERRIDES;
  }

  void drawKeys(@NonNull Canvas canvas, Rect dirtyRect, DrawInputs inputs) {
    final int keyOverlayLayerAnalysis =
        keyFaceWallpaperOverlayRenderer.analyzeOverlayLayers(inputs.keyFaceWallpaperOverlayLayers);
    if (keyFaceWallpaperOverlayRenderer.canUseKeyboardMaskOverlay(
        inputs, keyOverlayLayerAnalysis)) {
      drawKeysWithOptimizedKeyTextureOverlay(canvas, dirtyRect, inputs);
      return;
    }

    final boolean hasKeyOverlayEffects =
        keyFaceWallpaperOverlayRenderer.hasKeyOverlayEffects(keyOverlayLayerAnalysis);
    final boolean hasKeyOverlayBlendMode =
        keyFaceWallpaperOverlayRenderer.hasKeyOverlayBlendMode(inputs, keyOverlayLayerAnalysis);
    final boolean needsKeyOverlayLayer =
        keyFaceWallpaperOverlayRenderer.needsKeyOverlayLayer(inputs, hasKeyOverlayBlendMode);
    final Paint keyOverlayBlendPaint =
        keyFaceWallpaperOverlayRenderer.resolveKeyOverlayBlendPaintIfNeeded(
            inputs, needsKeyOverlayLayer);

    keyBackgroundRenderer.startFrame(inputs);
    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;
      final int primaryCodeForShadow = key.getPrimaryCode();
      final boolean keyIsSpace = primaryCodeForShadow == KeyCodes.SPACE;

      if (inputs.drawSingleKey && (inputs.invalidKey != key)) {
        continue;
      }
      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);
      final int resolvedTextColor =
          resolveTextColorForKey(key, primaryCodeForShadow, keyIsSpace, drawableState, inputs);

      paint.setColor(resolvedTextColor);
      inputs.keyBackground.setState(drawableState);
      keyBackgroundRenderer.maybeApplyBackgroundTintForKey(
          inputs, key, keyIsSpace, primaryCodeForShadow, drawableState);

      final CharSequence label = resolveKeyLabelForCurrentState(key, inputs);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);

      keyBackgroundRenderer.drawKeyBackground(
          canvas, key, keyIsSpace, primaryCodeForShadow, drawableState, inputs);
      keyFaceWallpaperOverlayRenderer.drawKeyFaceOverlayIfAny(
          canvas,
          key,
          drawableState,
          inputs,
          hasKeyOverlayEffects,
          needsKeyOverlayLayer,
          keyOverlayBlendPaint);

      drawKeyForeground(
          canvas,
          key,
          drawableState,
          label,
          resolvedTextColor,
          keyIsSpace,
          primaryCodeForShadow,
          inputs);

      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }
    keyBackgroundRenderer.finishFrame(inputs);
  }

  private void drawKeysWithOptimizedKeyTextureOverlay(
      @NonNull Canvas canvas, @NonNull Rect dirtyRect, DrawInputs inputs) {
    keyBackgroundRenderer.startFrame(inputs);
    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;
      final int primaryCodeForShadow = key.getPrimaryCode();
      final boolean keyIsSpace = primaryCodeForShadow == KeyCodes.SPACE;

      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);
      inputs.keyBackground.setState(drawableState);
      keyBackgroundRenderer.maybeApplyBackgroundTintForKey(
          inputs, key, keyIsSpace, primaryCodeForShadow, drawableState);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);
      keyBackgroundRenderer.drawKeyBackground(
          canvas, key, keyIsSpace, primaryCodeForShadow, drawableState, inputs);
      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }

    keyBackgroundRenderer.finishFrame(inputs);

    final boolean overlayApplied =
        keyFaceWallpaperOverlayRenderer.tryDrawKeyboardTextureOverlayStackWithMask(
            canvas, dirtyRect, inputs);

    for (Keyboard.Key keyBase : inputs.keys) {
      final KeyboardKey key = (KeyboardKey) keyBase;
      final int primaryCodeForShadow = key.getPrimaryCode();
      final boolean keyIsSpace = primaryCodeForShadow == KeyCodes.SPACE;

      if (!drawDecisions.shouldDrawKey(
          key, dirtyRect, inputs.kbdPaddingLeft, inputs.kbdPaddingTop)) {
        continue;
      }

      int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);
      final int resolvedTextColor =
          resolveTextColorForKey(key, primaryCodeForShadow, keyIsSpace, drawableState, inputs);

      paint.setColor(resolvedTextColor);
      inputs.keyBackground.setState(drawableState);

      final CharSequence label = resolveKeyLabelForCurrentState(key, inputs);

      drawDecisions.adjustBoundsIfNeeded(inputs.keyBackground, key);
      canvas.translate(key.x + inputs.kbdPaddingLeft, key.y + inputs.kbdPaddingTop);

      if (!overlayApplied) {
        keyFaceWallpaperOverlayRenderer.drawFallbackKeyFaceOverlayIfAny(
            canvas, key, drawableState, inputs);
      }

      drawKeyForeground(
          canvas,
          key,
          drawableState,
          label,
          resolvedTextColor,
          keyIsSpace,
          primaryCodeForShadow,
          inputs);

      canvas.translate(-key.x - inputs.kbdPaddingLeft, -key.y - inputs.kbdPaddingTop);
    }
  }

  private int resolveTextColorForKey(
      @NonNull KeyboardKey key,
      int primaryCode,
      boolean keyIsSpace,
      @NonNull int[] drawableState,
      @NonNull DrawInputs inputs) {
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
    final Integer specialKeyTextColorOverride = inputs.specialKeyTextColorOverride;
    final Integer modifierKeyTextColorOverride = inputs.modifierKeyTextColorOverride;
    final Integer enterKeyTextColorOverride = inputs.enterKeyTextColorOverride;
    if (!inputs.themeOverlayActive) {
      final boolean modifierKeyActive =
          (primaryCode == KeyCodes.FUNCTION && inputs.modifierStates.functionModeActive)
              || (primaryCode == KeyCodes.CTRL && inputs.modifierStates.controlModeActive)
              || (primaryCode == KeyCodes.ALT_MODIFIER && inputs.modifierStates.altModeActive);
      if (isEnterKey(primaryCode) && enterKeyTextColorOverride != null) {
        resolvedTextColor = enterKeyTextColorOverride;
      } else if (modifierKeyTextColorOverride != null && !modifierKeyActive && isModifierKey(key)) {
        resolvedTextColor = modifierKeyTextColorOverride;
      } else if (specialKeyTextColorOverride != null
          && !modifierKeyActive
          && isSpecialKey(primaryCode, drawableState, inputs)) {
        resolvedTextColor = specialKeyTextColorOverride;
      }
    }
    return resolvedTextColor;
  }

  @Nullable
  private static CharSequence resolveKeyLabelForCurrentState(
      @NonNull KeyboardKey key, @NonNull DrawInputs inputs) {
    CharSequence label =
        key.label == null
            ? null
            : KeyLabelAdjuster.adjustLabelToShiftState(
                inputs.keyboard,
                inputs.keyDetector,
                inputs.textCaseForceOverrideType,
                inputs.textCaseType,
                key);
    return KeyLabelAdjuster.adjustLabelForFunctionState(inputs.keyboard, key, label);
  }

  private void drawKeyForeground(
      @NonNull Canvas canvas,
      @NonNull KeyboardKey key,
      @NonNull int[] drawableState,
      @Nullable CharSequence label,
      int resolvedTextColor,
      boolean keyIsSpace,
      int primaryCodeForShadow,
      @NonNull DrawInputs inputs) {
    CharSequence resolvedLabel =
        keyIconDrawer.drawIconIfNeeded(
            canvas,
            key,
            drawableState,
            keyIconResolver,
            label,
            keyBackgroundPadding,
            resolvedTextColor,
            keyLabelGuesser);

    resolvedLabel =
        keyboardNameRenderer.applyKeyboardNameIfNeeded(
            resolvedLabel, keyIsSpace, inputs.drawKeyboardNameText, inputs.keyboardName);

    if (resolvedLabel != null) {
      final PerKeyTextShadowOverrides.Overrides textShadowOverrides =
          resolveTextShadowOverridesForKey(
              key, keyIsSpace, primaryCodeForShadow, drawableState, inputs);
      int keyShadowRadius = inputs.shadowRadius;
      int keyShadowOffsetX = inputs.shadowOffsetX;
      int keyShadowOffsetY = inputs.shadowOffsetY;
      int keyShadowColor = inputs.shadowColor;
      if (!textShadowOverrides.isEmpty()) {
        final Integer overrideShadowColor = textShadowOverrides.color();
        if (overrideShadowColor != null) keyShadowColor = overrideShadowColor;
        final Integer overrideShadowRadius = textShadowOverrides.radius();
        if (overrideShadowRadius != null) keyShadowRadius = overrideShadowRadius;
        final Integer overrideShadowOffsetX = textShadowOverrides.offsetX();
        if (overrideShadowOffsetX != null) keyShadowOffsetX = overrideShadowOffsetX;
        final Integer overrideShadowOffsetY = textShadowOverrides.offsetY();
        if (overrideShadowOffsetY != null) keyShadowOffsetY = overrideShadowOffsetY;
      }

      keyLabelRenderer.drawLabel(
          canvas,
          paint,
          resolvedLabel,
          key,
          keyBackgroundPadding,
          keyIsSpace,
          inputs.keyboardNameTextSize,
          inputs.ellipsizeKeyLabels,
          inputs.alwaysUseDrawText,
          keyTextPaintSetter,
          keyboardNamePaintSetter,
          labelTextPaintSetter,
          (p, l, width) -> {
            if (!inputs.autoFitKeyLabels) {
              return p.measureText(l, 0, l.length());
            }
            return labelPaintConfigurator.adjustTextSizeForLabel(
                p, l, width, p.getTextSize(), inputs.keyLabelAutoFitMinScale);
          },
          keyShadowRadius,
          keyShadowOffsetX,
          keyShadowOffsetY,
          keyShadowColor);
    }

    if (inputs.drawHintText
        && (inputs.hintTextSizeMultiplier > 0)
        && ((key.popupCharacters != null && key.popupCharacters.length() > 0)
            || (key.popupResId != 0)
            || (key.popupKeyboardPackPath != null && key.popupKeyboardPackPath.length() > 0)
            || (key.longPressCode != 0))) {
      Paint.Align oldAlign = paint.getTextAlign();
      hintTextPaintSetter.setPaintForHintText(paint);
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
}
