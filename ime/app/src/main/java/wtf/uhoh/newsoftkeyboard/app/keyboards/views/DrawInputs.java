package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyDrawableStateProvider;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeResourcesHolder;

/** Snapshot of per-frame draw inputs to keep {@link KeyboardViewBase#onDraw} slim. */
final class DrawInputs {
  final KeyboardDefinition keyboard;
  final CharSequence keyboardName;
  @Nullable final CharSequence spacebarVoiceBadgeText;
  @Nullable final Integer specialKeyTextColorOverride;
  @Nullable final Integer modifierKeyTextColorOverride;
  @Nullable final Integer enterKeyTextColorOverride;
  final boolean themeOverlayActive;
  final boolean drawKeyboardNameText;
  final boolean drawHintText;
  final boolean keyboardShifted;
  final Locale keyboardLocale;
  final ThemeResourcesHolder themeResourcesHolder;
  final ColorStateList keyTextColor;
  final DrawDecisions.ModifierStates modifierStates;
  final int modifierActiveTextColor;
  final int hintAlign;
  final int hintVAlign;
  @Nullable final Integer userKeyBackgroundTint;
  @Nullable final Integer userSpecialKeyBackgroundTint;
  @Nullable final Integer userSpacebarBackgroundTint;
  @Nullable final Integer userModifierKeyBackgroundTint;
  @Nullable final Integer userEnterKeyBackgroundTint;
  final Drawable keyBackground;
  final Keyboard.Key[] keys;
  final Keyboard.Key invalidKey;
  final boolean drawSingleKey;
  final int kbdPaddingLeft;
  final int kbdPaddingTop;
  final int keyboardViewWidth;
  final int keyboardViewHeight;
  final float keyboardNameTextSize;
  final float hintTextSize;
  final float hintTextSizeMultiplier;
  final boolean alwaysUseDrawText;
  final int shadowRadius;
  final int shadowOffsetX;
  final int shadowOffsetY;
  final int shadowColor;
  final PerKeyTextShadowOverrides perKeyTextShadowOverrides;
  final boolean keyBackgroundShadowEnabled;
  final int keyBackgroundShadowOffsetX;
  final int keyBackgroundShadowOffsetY;
  final int keyBackgroundShadowSpread;
  final int keyBackgroundShadowColor;
  final PerKeyBackgroundShadowOverrides perKeyBackgroundShadowOverrides;
  final int textCaseForceOverrideType;
  final int textCaseType;
  final KeyDetector keyDetector;
  final float keyTextSize;
  final boolean autoFitKeyLabels;
  final float keyLabelAutoFitMinScale;
  final boolean ellipsizeKeyLabels;
  final boolean allowExpensiveWallpaperEffects;
  final int keyFaceWallpaperOverlayMode;
  final int keyFaceWallpaperOverlayBlendMode;
  @Nullable final KeyboardWallpaperLayer[] keyFaceWallpaperOverlayLayers;
  final boolean keyFaceWallpaperOverlayMatchKeyShape;
  final Paint keyFaceWallpaperOverlayPaint;
  final int keyFaceWallpaperOverlaySpecialKeyAlpha;
  final int keyFaceWallpaperOverlaySpacebarAlpha;
  final int keyFaceWallpaperOverlayModifierKeyAlpha;
  final int keyFaceWallpaperOverlayEnterKeyAlpha;
  final KeyDrawableStateProvider drawableStatesProvider;

  DrawInputs(
      KeyboardDefinition keyboard,
      CharSequence keyboardName,
      @Nullable CharSequence spacebarVoiceBadgeText,
      @Nullable Integer specialKeyTextColorOverride,
      @Nullable Integer modifierKeyTextColorOverride,
      @Nullable Integer enterKeyTextColorOverride,
      boolean themeOverlayActive,
      boolean drawKeyboardNameText,
      boolean drawHintText,
      boolean keyboardShifted,
      Locale keyboardLocale,
      ThemeResourcesHolder themeResourcesHolder,
      ColorStateList keyTextColor,
      DrawDecisions.ModifierStates modifierStates,
      int modifierActiveTextColor,
      int hintAlign,
      int hintVAlign,
      @Nullable Integer userKeyBackgroundTint,
      @Nullable Integer userSpecialKeyBackgroundTint,
      @Nullable Integer userSpacebarBackgroundTint,
      @Nullable Integer userModifierKeyBackgroundTint,
      @Nullable Integer userEnterKeyBackgroundTint,
      Drawable keyBackground,
      Keyboard.Key[] keys,
      Keyboard.Key invalidKey,
      boolean drawSingleKey,
      int kbdPaddingLeft,
      int kbdPaddingTop,
      int keyboardViewWidth,
      int keyboardViewHeight,
      float keyboardNameTextSize,
      float hintTextSize,
      float hintTextSizeMultiplier,
      boolean alwaysUseDrawText,
      int shadowRadius,
      int shadowOffsetX,
      int shadowOffsetY,
      int shadowColor,
      PerKeyTextShadowOverrides perKeyTextShadowOverrides,
      boolean keyBackgroundShadowEnabled,
      int keyBackgroundShadowOffsetX,
      int keyBackgroundShadowOffsetY,
      int keyBackgroundShadowSpread,
      int keyBackgroundShadowColor,
      PerKeyBackgroundShadowOverrides perKeyBackgroundShadowOverrides,
      int textCaseForceOverrideType,
      int textCaseType,
      KeyDetector keyDetector,
      float keyTextSize,
      boolean autoFitKeyLabels,
      float keyLabelAutoFitMinScale,
      boolean ellipsizeKeyLabels,
      boolean allowExpensiveWallpaperEffects,
      int keyFaceWallpaperOverlayMode,
      int keyFaceWallpaperOverlayBlendMode,
      @Nullable KeyboardWallpaperLayer[] keyFaceWallpaperOverlayLayers,
      boolean keyFaceWallpaperOverlayMatchKeyShape,
      Paint keyFaceWallpaperOverlayPaint,
      int keyFaceWallpaperOverlaySpecialKeyAlpha,
      int keyFaceWallpaperOverlaySpacebarAlpha,
      int keyFaceWallpaperOverlayModifierKeyAlpha,
      int keyFaceWallpaperOverlayEnterKeyAlpha,
      KeyDrawableStateProvider drawableStatesProvider) {
    this.keyboard = keyboard;
    this.keyboardName = keyboardName;
    this.spacebarVoiceBadgeText = spacebarVoiceBadgeText;
    this.specialKeyTextColorOverride = specialKeyTextColorOverride;
    this.modifierKeyTextColorOverride = modifierKeyTextColorOverride;
    this.enterKeyTextColorOverride = enterKeyTextColorOverride;
    this.themeOverlayActive = themeOverlayActive;
    this.drawKeyboardNameText = drawKeyboardNameText;
    this.drawHintText = drawHintText;
    this.keyboardShifted = keyboardShifted;
    this.keyboardLocale = keyboardLocale;
    this.themeResourcesHolder = themeResourcesHolder;
    this.keyTextColor = keyTextColor;
    this.modifierStates = modifierStates;
    this.modifierActiveTextColor = modifierActiveTextColor;
    this.hintAlign = hintAlign;
    this.hintVAlign = hintVAlign;
    this.userKeyBackgroundTint = userKeyBackgroundTint;
    this.userSpecialKeyBackgroundTint = userSpecialKeyBackgroundTint;
    this.userSpacebarBackgroundTint = userSpacebarBackgroundTint;
    this.userModifierKeyBackgroundTint = userModifierKeyBackgroundTint;
    this.userEnterKeyBackgroundTint = userEnterKeyBackgroundTint;
    this.keyBackground = keyBackground;
    this.keys = keys;
    this.invalidKey = invalidKey;
    this.drawSingleKey = drawSingleKey;
    this.kbdPaddingLeft = kbdPaddingLeft;
    this.kbdPaddingTop = kbdPaddingTop;
    this.keyboardViewWidth = keyboardViewWidth;
    this.keyboardViewHeight = keyboardViewHeight;
    this.keyboardNameTextSize = keyboardNameTextSize;
    this.hintTextSize = hintTextSize;
    this.hintTextSizeMultiplier = hintTextSizeMultiplier;
    this.alwaysUseDrawText = alwaysUseDrawText;
    this.shadowRadius = shadowRadius;
    this.shadowOffsetX = shadowOffsetX;
    this.shadowOffsetY = shadowOffsetY;
    this.shadowColor = shadowColor;
    this.perKeyTextShadowOverrides = perKeyTextShadowOverrides;
    this.keyBackgroundShadowEnabled = keyBackgroundShadowEnabled;
    this.keyBackgroundShadowOffsetX = keyBackgroundShadowOffsetX;
    this.keyBackgroundShadowOffsetY = keyBackgroundShadowOffsetY;
    this.keyBackgroundShadowSpread = keyBackgroundShadowSpread;
    this.keyBackgroundShadowColor = keyBackgroundShadowColor;
    this.perKeyBackgroundShadowOverrides = perKeyBackgroundShadowOverrides;
    this.textCaseForceOverrideType = textCaseForceOverrideType;
    this.textCaseType = textCaseType;
    this.keyDetector = keyDetector;
    this.keyTextSize = keyTextSize;
    this.autoFitKeyLabels = autoFitKeyLabels;
    this.keyLabelAutoFitMinScale = keyLabelAutoFitMinScale;
    this.ellipsizeKeyLabels = ellipsizeKeyLabels;
    this.allowExpensiveWallpaperEffects = allowExpensiveWallpaperEffects;
    this.keyFaceWallpaperOverlayMode = keyFaceWallpaperOverlayMode;
    this.keyFaceWallpaperOverlayBlendMode = keyFaceWallpaperOverlayBlendMode;
    this.keyFaceWallpaperOverlayLayers = keyFaceWallpaperOverlayLayers;
    this.keyFaceWallpaperOverlayMatchKeyShape = keyFaceWallpaperOverlayMatchKeyShape;
    this.keyFaceWallpaperOverlayPaint = keyFaceWallpaperOverlayPaint;
    this.keyFaceWallpaperOverlaySpecialKeyAlpha = keyFaceWallpaperOverlaySpecialKeyAlpha;
    this.keyFaceWallpaperOverlaySpacebarAlpha = keyFaceWallpaperOverlaySpacebarAlpha;
    this.keyFaceWallpaperOverlayModifierKeyAlpha = keyFaceWallpaperOverlayModifierKeyAlpha;
    this.keyFaceWallpaperOverlayEnterKeyAlpha = keyFaceWallpaperOverlayEnterKeyAlpha;
    this.drawableStatesProvider = drawableStatesProvider;
  }
}
