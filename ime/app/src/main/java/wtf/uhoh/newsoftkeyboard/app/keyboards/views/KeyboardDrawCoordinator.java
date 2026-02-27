package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyDrawableStateProvider;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;

final class KeyboardDrawCoordinator {

  private final InvalidateHelper invalidateHelper;
  private final ClipRegionHolder clipRegionHolder;
  private final Rect keyboardViewBounds = new Rect(0, 0, 0, 0);
  private final DrawInputsBuilder drawInputsBuilder;
  private final KeyDrawHelper keyDrawHelper;
  private final KeyTextStyleState keyTextStyleState;
  private final KeyDisplayState keyDisplayState;
  private final KeyShadowStyle keyShadowStyle;
  private final KeyBackgroundShadowStyle keyBackgroundShadowStyle;
  private final PerKeyTextShadowOverrides perKeyTextShadowOverrides;
  private final PerKeyBackgroundShadowOverrides perKeyBackgroundShadowOverrides;
  private final KeyDetector keyDetector;

  KeyboardDrawCoordinator(
      InvalidateHelper invalidateHelper,
      ClipRegionHolder clipRegionHolder,
      DrawInputsBuilder drawInputsBuilder,
      KeyDrawHelper keyDrawHelper,
      KeyTextStyleState keyTextStyleState,
      KeyDisplayState keyDisplayState,
      KeyShadowStyle keyShadowStyle,
      KeyBackgroundShadowStyle keyBackgroundShadowStyle,
      PerKeyTextShadowOverrides perKeyTextShadowOverrides,
      PerKeyBackgroundShadowOverrides perKeyBackgroundShadowOverrides,
      KeyDetector keyDetector) {
    this.invalidateHelper = invalidateHelper;
    this.clipRegionHolder = clipRegionHolder;
    this.drawInputsBuilder = drawInputsBuilder;
    this.keyDrawHelper = keyDrawHelper;
    this.keyTextStyleState = keyTextStyleState;
    this.keyDisplayState = keyDisplayState;
    this.keyShadowStyle = keyShadowStyle;
    this.keyBackgroundShadowStyle = keyBackgroundShadowStyle;
    this.perKeyTextShadowOverrides = perKeyTextShadowOverrides;
    this.perKeyBackgroundShadowOverrides = perKeyBackgroundShadowOverrides;
    this.keyDetector = keyDetector;
  }

  void draw(
      @NonNull Canvas canvas,
      @Nullable KeyboardTheme theme,
      @Nullable KeyboardDefinition keyboard,
      @Nullable Keyboard.Key[] keys,
      @Nullable KeyDrawableStateProvider drawableStateProvider,
      CharSequence keyboardName,
      @Nullable CharSequence spacebarVoiceBadgeText,
      @Nullable Integer specialKeyTextColorOverride,
      @Nullable Integer modifierKeyTextColorOverride,
      @Nullable Integer enterKeyTextColorOverride,
      @Nullable Integer userKeyBackgroundTint,
      @Nullable Integer userSpecialKeyBackgroundTint,
      @Nullable Integer userSpacebarBackgroundTint,
      @Nullable Integer userModifierKeyBackgroundTint,
      @Nullable Integer userEnterKeyBackgroundTint,
      boolean allowExpensiveWallpaperEffects,
      int viewWidth,
      int viewHeight,
      int paddingLeft,
      int paddingTop) {
    final Rect dirtyRect = invalidateHelper.dirtyRect();
    if (keyboard == null || keys == null || keys.length == 0) {
      return;
    }

    if (!ClipAndDirtyRegionPrep.prepare(
        canvas, dirtyRect, clipRegionHolder.rect(), keys, paddingLeft, paddingTop)) {
      return;
    }

    keyboardViewBounds.set(0, 0, viewWidth, viewHeight);

    final DrawInputs drawInputs =
        drawInputsBuilder.build(
            canvas,
            dirtyRect,
            theme,
            keyboard,
            keyboardName,
            spacebarVoiceBadgeText,
            specialKeyTextColorOverride,
            modifierKeyTextColorOverride,
            enterKeyTextColorOverride,
            keys,
            invalidateHelper.invalidatedKey(),
            clipRegionHolder.rect(),
            keyboardViewBounds,
            paddingLeft,
            paddingTop,
            keyTextStyleState.keyboardNameTextSize(),
            keyTextStyleState.hintTextSize(),
            keyTextStyleState.hintTextSizeMultiplier(),
            keyDisplayState.alwaysUseDrawText(),
            keyShadowStyle.radius(),
            keyShadowStyle.offsetX(),
            keyShadowStyle.offsetY(),
            keyShadowStyle.color(),
            perKeyTextShadowOverrides,
            keyBackgroundShadowStyle.enabled(),
            keyBackgroundShadowStyle.offsetX(),
            keyBackgroundShadowStyle.offsetY(),
            keyBackgroundShadowStyle.spread(),
            keyBackgroundShadowStyle.color(),
            perKeyBackgroundShadowOverrides,
            keyTextStyleState.textCaseForceOverrideType(),
            keyTextStyleState.textCaseType(),
            keyDetector,
            keyTextStyleState.keyTextSize(),
            keyTextStyleState.autoFitKeyLabels(),
            keyTextStyleState.keyLabelAutoFitMinScale(),
            keyTextStyleState.ellipsizeKeyLabels(),
            keyTextStyleState.themeHintLabelAlign(),
            keyTextStyleState.themeHintLabelVAlign(),
            userKeyBackgroundTint,
            userSpecialKeyBackgroundTint,
            userSpacebarBackgroundTint,
            userModifierKeyBackgroundTint,
            userEnterKeyBackgroundTint,
            allowExpensiveWallpaperEffects,
            drawableStateProvider);
    keyDrawHelper.drawKeys(canvas, dirtyRect, drawInputs);
    invalidateHelper.clearAfterDraw();
  }
}
