package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyDrawableStateProvider;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeResourcesHolder;

/** Builds {@link DrawInputs} snapshots to keep {@link KeyboardViewBase} slimmer. */
final class DrawInputsBuilder {

  private final ThemeOverlayCombiner themeOverlayCombiner;
  private final KeyboardWallpaperResolver keyboardWallpaperResolver;
  private final DrawDecisions drawDecisions;
  private final HintLayoutCalculator hintLayoutCalculator;
  private final KeyboardNameHintController keyboardNameHintController;
  private final DirtyRegionDecider dirtyRegionDecider;
  private final Runnable requestInvalidateAllKeys;

  DrawInputsBuilder(
      ThemeOverlayCombiner themeOverlayCombiner,
      KeyboardWallpaperResolver keyboardWallpaperResolver,
      DrawDecisions drawDecisions,
      HintLayoutCalculator hintLayoutCalculator,
      KeyboardNameHintController keyboardNameHintController,
      DirtyRegionDecider dirtyRegionDecider,
      Runnable requestInvalidateAllKeys) {
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.keyboardWallpaperResolver = keyboardWallpaperResolver;
    this.drawDecisions = drawDecisions;
    this.hintLayoutCalculator = hintLayoutCalculator;
    this.keyboardNameHintController = keyboardNameHintController;
    this.dirtyRegionDecider = dirtyRegionDecider;
    this.requestInvalidateAllKeys = requestInvalidateAllKeys;
  }

  DrawInputs build(
      Canvas canvas,
      Rect dirtyRect,
      @Nullable KeyboardTheme theme,
      KeyboardDefinition keyboard,
      CharSequence keyboardName,
      @Nullable CharSequence spacebarVoiceBadgeText,
      Keyboard.Key[] keys,
      @Nullable Keyboard.Key invalidKey,
      Rect clipRegion,
      Rect keyboardViewBounds,
      int paddingLeft,
      int paddingTop,
      float keyboardNameTextSize,
      float hintTextSize,
      float hintTextSizeMultiplier,
      boolean alwaysUseDrawText,
      int shadowRadius,
      int shadowOffsetX,
      int shadowOffsetY,
      int shadowColor,
      int textCaseForceOverrideType,
      int textCaseType,
      KeyDetector keyDetector,
      float keyTextSize,
      int themeHintLabelAlign,
      int themeHintLabelVAlign,
      boolean allowExpensiveWallpaperEffects,
      @Nullable KeyDrawableStateProvider drawableStatesProvider) {

    final ThemeResourcesHolder themeResourcesHolder = themeOverlayCombiner.getThemeResources();
    final var keyTextColor = themeResourcesHolder.getKeyTextColor();

    final DrawDecisions.ModifierStates modifierStates = drawDecisions.modifierStates(keyboard);
    int modifierActiveTextColor =
        drawDecisions.resolveModifierActiveTextColor(keyTextColor, drawableStatesProvider);

    final int hintAlign =
        hintLayoutCalculator.resolveHintAlign(
            keyboardNameHintController.customHintGravity(), themeHintLabelAlign);
    final int hintVAlign =
        hintLayoutCalculator.resolveHintVAlign(
            keyboardNameHintController.customHintGravity(), themeHintLabelVAlign);

    final var keyBackground = themeResourcesHolder.getKeyBackground();
    final boolean drawSingleKey =
        dirtyRegionDecider.shouldDrawSingleKey(
            canvas, invalidKey, clipRegion, paddingLeft, paddingTop);

    final KeyboardWallpaperResolver.KeyFaceOverlay keyFaceOverlay =
        keyboardWallpaperResolver.resolveKeyFaceOverlay(
            theme, keyboardViewBounds, allowExpensiveWallpaperEffects, requestInvalidateAllKeys);

    final float effectiveKeyboardNameTextSize =
        keyboardNameTextSize > 1f ? keyboardNameTextSize : keyTextSize;

    return new DrawInputs(
        keyboard,
        keyboardName,
        spacebarVoiceBadgeText,
        keyboardNameHintController.shouldShowKeyboardName() && effectiveKeyboardNameTextSize > 1f,
        hintTextSize > 1 && keyboardNameHintController.shouldShowHints(),
        keyboard != null && keyboard.isShifted(),
        keyboard != null ? keyboard.getLocale() : Locale.getDefault(),
        themeResourcesHolder,
        keyTextColor,
        modifierStates,
        modifierActiveTextColor,
        hintAlign,
        hintVAlign,
        keyBackground,
        keys,
        invalidKey,
        drawSingleKey,
        paddingLeft,
        paddingTop,
        keyboardViewBounds.width(),
        keyboardViewBounds.height(),
        effectiveKeyboardNameTextSize,
        hintTextSize,
        hintTextSizeMultiplier,
        alwaysUseDrawText,
        shadowRadius,
        shadowOffsetX,
        shadowOffsetY,
        shadowColor,
        textCaseForceOverrideType,
        textCaseType,
        keyDetector,
        keyTextSize,
        keyFaceOverlay.mode(),
        keyFaceOverlay.matchKeyShape(),
        keyFaceOverlay.paint(),
        drawableStatesProvider);
  }
}
