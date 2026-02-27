package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;

/** Draws a key's background (tint + shadow + base drawable). */
final class KeyBackgroundRenderer {

  private static final int KEY_BACKGROUND_TINT_MUL = 0xFF333333;
  private static final PerKeyBackgroundShadowOverrides.Overrides EMPTY_BACKGROUND_SHADOW_OVERRIDES =
      new PerKeyBackgroundShadowOverrides.Overrides();

  private final Paint keyBackgroundShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private boolean hasPerKeyBackgroundTintOverride;
  @Nullable private Integer keyBackgroundTint;
  @Nullable private Integer specialKeyBackgroundTint;
  @Nullable private Integer spacebarBackgroundTint;
  @Nullable private Integer modifierKeyBackgroundTint;
  @Nullable private Integer enterKeyBackgroundTint;
  @Nullable private Integer lastAppliedBackgroundTint;

  KeyBackgroundRenderer() {
    keyBackgroundShadowPaint.setStyle(Paint.Style.FILL);
  }

  void startFrame(@NonNull DrawInputs inputs) {
    keyBackgroundTint = inputs.userKeyBackgroundTint;
    specialKeyBackgroundTint = inputs.userSpecialKeyBackgroundTint;
    spacebarBackgroundTint = inputs.userSpacebarBackgroundTint;
    modifierKeyBackgroundTint = inputs.userModifierKeyBackgroundTint;
    enterKeyBackgroundTint = inputs.userEnterKeyBackgroundTint;
    hasPerKeyBackgroundTintOverride =
        !inputs.themeOverlayActive
            && (specialKeyBackgroundTint != null
                || spacebarBackgroundTint != null
                || modifierKeyBackgroundTint != null
                || enterKeyBackgroundTint != null);
    lastAppliedBackgroundTint = keyBackgroundTint;
  }

  void finishFrame(@NonNull DrawInputs inputs) {
    if (hasPerKeyBackgroundTintOverride) {
      applyKeyBackgroundTintIfNeeded(inputs, keyBackgroundTint);
    }
  }

  void maybeApplyBackgroundTintForKey(
      @NonNull DrawInputs inputs,
      @NonNull KeyboardKey key,
      boolean keyIsSpace,
      int primaryCode,
      @NonNull int[] drawableState) {
    if (!hasPerKeyBackgroundTintOverride) return;
    final boolean modifierKeyActive =
        (primaryCode == KeyCodes.FUNCTION && inputs.modifierStates.functionModeActive)
            || (primaryCode == KeyCodes.CTRL && inputs.modifierStates.controlModeActive)
            || (primaryCode == KeyCodes.ALT_MODIFIER && inputs.modifierStates.altModeActive);
    final Integer desiredTint;
    if (keyIsSpace) {
      desiredTint = spacebarBackgroundTint != null ? spacebarBackgroundTint : keyBackgroundTint;
    } else if (enterKeyBackgroundTint != null && KeyDrawHelper.isEnterKey(primaryCode)) {
      desiredTint = enterKeyBackgroundTint;
    } else if (!modifierKeyActive
        && modifierKeyBackgroundTint != null
        && KeyDrawHelper.isModifierKey(key)) {
      desiredTint = modifierKeyBackgroundTint;
    } else if (!modifierKeyActive
        && specialKeyBackgroundTint != null
        && KeyDrawHelper.isSpecialKey(primaryCode, drawableState, inputs)) {
      desiredTint = specialKeyBackgroundTint;
    } else {
      desiredTint = keyBackgroundTint;
    }

    if ((desiredTint == null && lastAppliedBackgroundTint != null)
        || (desiredTint != null && !desiredTint.equals(lastAppliedBackgroundTint))) {
      applyKeyBackgroundTintIfNeeded(inputs, desiredTint);
      lastAppliedBackgroundTint = desiredTint;
    }
  }

  void drawKeyBackground(
      @NonNull Canvas canvas,
      @NonNull KeyboardKey key,
      boolean keyIsSpace,
      int primaryCode,
      @NonNull int[] drawableState,
      @NonNull DrawInputs inputs) {
    final PerKeyBackgroundShadowOverrides.Overrides backgroundShadowOverrides =
        resolveBackgroundShadowOverridesForKey(key, keyIsSpace, primaryCode, drawableState, inputs);
    boolean drawKeyBackgroundShadow = inputs.keyBackgroundShadowEnabled;
    int keyBackgroundShadowColor = inputs.keyBackgroundShadowColor;
    int keyBackgroundShadowOffsetX = inputs.keyBackgroundShadowOffsetX;
    int keyBackgroundShadowOffsetY = inputs.keyBackgroundShadowOffsetY;
    if (!backgroundShadowOverrides.isEmpty()) {
      final Integer overrideColor = backgroundShadowOverrides.color();
      if (overrideColor != null) {
        drawKeyBackgroundShadow = true;
        keyBackgroundShadowColor = overrideColor;
      }
      final Integer overrideOffsetX = backgroundShadowOverrides.offsetX();
      if (overrideOffsetX != null) {
        keyBackgroundShadowOffsetX = overrideOffsetX;
      }
      final Integer overrideOffsetY = backgroundShadowOverrides.offsetY();
      if (overrideOffsetY != null) {
        keyBackgroundShadowOffsetY = overrideOffsetY;
      }
    }

    if (drawKeyBackgroundShadow && ((keyBackgroundShadowColor >>> 24) != 0)) {
      keyBackgroundShadowPaint.setColor(keyBackgroundShadowColor);
      final float radius =
          KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
              inputs.keyBackground, key.width, key.height);
      final float spread = inputs.keyBackgroundShadowSpread;
      final float left = keyBackgroundShadowOffsetX - spread;
      final float top = keyBackgroundShadowOffsetY - spread;
      canvas.drawRoundRect(
          left,
          top,
          left + key.width + (2f * spread),
          top + key.height + (2f * spread),
          radius + spread,
          radius + spread,
          keyBackgroundShadowPaint);
    }
    inputs.keyBackground.draw(canvas);
  }

  @NonNull
  private static PerKeyBackgroundShadowOverrides.Overrides resolveBackgroundShadowOverridesForKey(
      @NonNull KeyboardKey key,
      boolean keyIsSpace,
      int primaryCode,
      @NonNull int[] drawableState,
      @NonNull DrawInputs inputs) {
    if (keyIsSpace) return inputs.perKeyBackgroundShadowOverrides.spacebar();

    if (KeyDrawHelper.isEnterKey(primaryCode)) {
      final var override = inputs.perKeyBackgroundShadowOverrides.enter();
      return override.isEmpty() ? inputs.perKeyBackgroundShadowOverrides.special() : override;
    }

    if (KeyDrawHelper.isModifierKey(key)) {
      final var override = inputs.perKeyBackgroundShadowOverrides.modifier();
      return override.isEmpty() ? inputs.perKeyBackgroundShadowOverrides.special() : override;
    }

    if (KeyDrawHelper.isSpecialKey(primaryCode, drawableState, inputs)) {
      return inputs.perKeyBackgroundShadowOverrides.special();
    }

    return EMPTY_BACKGROUND_SHADOW_OVERRIDES;
  }

  private static void applyKeyBackgroundTintIfNeeded(
      @NonNull DrawInputs inputs, @Nullable Integer tintColor) {
    if (inputs.themeOverlayActive) return;
    if (tintColor == null) {
      inputs.keyBackground.clearColorFilter();
    } else {
      inputs.keyBackground.setColorFilter(
          new LightingColorFilter(KEY_BACKGROUND_TINT_MUL, tintColor));
    }
  }
}
