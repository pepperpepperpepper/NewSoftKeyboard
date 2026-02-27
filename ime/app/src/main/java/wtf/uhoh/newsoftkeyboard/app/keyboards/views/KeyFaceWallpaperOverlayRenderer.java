package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;

/** Draws the key-face wallpaper overlay and its optional effect layers. */
final class KeyFaceWallpaperOverlayRenderer {

  private static final int LAYER_ANALYSIS_HAS_EFFECTS = 1;
  private static final int LAYER_ANALYSIS_HAS_BLEND_MODES = 1 << 1;

  private final KeyFaceOverlayMaskRenderer overlayMaskRenderer;
  private final KeyFaceWallpaperEffectsRenderer wallpaperEffectsRenderer =
      new KeyFaceWallpaperEffectsRenderer();

  KeyFaceWallpaperOverlayRenderer(@NonNull KeyFaceOverlayMaskRenderer overlayMaskRenderer) {
    this.overlayMaskRenderer = overlayMaskRenderer;
  }

  int analyzeOverlayLayers(@Nullable KeyboardWallpaperLayer[] layers) {
    int analysis = 0;
    if (layers == null) return analysis;
    for (KeyboardWallpaperLayer layer : layers) {
      if (layer == null || !layer.enabled()) continue;
      if (layer.opacityPercent() <= 0) continue;

      final boolean draws;
      switch (layer.type()) {
        case KeyboardWallpaperLayer.TYPE_COLOR_WASH,
            KeyboardWallpaperLayer.TYPE_SOLID_COLOR,
            KeyboardWallpaperLayer.TYPE_DOTS,
            KeyboardWallpaperLayer.TYPE_GRID,
            KeyboardWallpaperLayer.TYPE_STRIPES,
            KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES,
            KeyboardWallpaperLayer.TYPE_TRIANGLES,
            KeyboardWallpaperLayer.TYPE_HEX -> {
          final Integer argb = layer.argb();
          draws = argb != null && ((argb >>> 24) != 0);
        }
        case KeyboardWallpaperLayer.TYPE_CHECKER -> {
          final Integer argb = layer.argb();
          final Integer argb2 = layer.argb2();
          draws =
              (argb != null && ((argb >>> 24) != 0)) || (argb2 != null && ((argb2 >>> 24) != 0));
        }
        case KeyboardWallpaperLayer.TYPE_HIGHLIGHT,
            KeyboardWallpaperLayer.TYPE_DIM,
            KeyboardWallpaperLayer.TYPE_GRADIENT,
            KeyboardWallpaperLayer.TYPE_VIGNETTE,
            KeyboardWallpaperLayer.TYPE_GRAIN ->
            draws = true;
        default -> draws = false;
      }

      if (!draws) continue;
      analysis |= LAYER_ANALYSIS_HAS_EFFECTS;
      if (layer.blendMode() != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
        analysis |= LAYER_ANALYSIS_HAS_BLEND_MODES;
      }
    }
    return analysis;
  }

  boolean hasKeyOverlayEffects(int analysis) {
    return (analysis & LAYER_ANALYSIS_HAS_EFFECTS) != 0;
  }

  boolean hasKeyOverlayBlendMode(@NonNull DrawInputs inputs, int analysis) {
    return inputs.keyFaceWallpaperOverlayBlendMode
            != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL
        || (analysis & LAYER_ANALYSIS_HAS_BLEND_MODES) != 0;
  }

  boolean canUseKeyboardMaskOverlay(@NonNull DrawInputs inputs, int overlayLayerAnalysis) {
    final boolean hasKeyOverlayEffects = hasKeyOverlayEffects(overlayLayerAnalysis);
    final boolean hasKeyOverlayBlendMode = hasKeyOverlayBlendMode(inputs, overlayLayerAnalysis);
    return inputs.keyFaceWallpaperOverlayPaint != null
        && !inputs.drawSingleKey
        && (inputs.keyFaceWallpaperOverlayMatchKeyShape
            || hasKeyOverlayEffects
            || hasKeyOverlayBlendMode);
  }

  boolean needsKeyOverlayLayer(@NonNull DrawInputs inputs, boolean hasKeyOverlayBlendMode) {
    return inputs.keyFaceWallpaperOverlayPaint != null && hasKeyOverlayBlendMode;
  }

  @Nullable
  Paint resolveKeyOverlayBlendPaintIfNeeded(
      @NonNull DrawInputs inputs, boolean needsKeyOverlayLayer) {
    if (!needsKeyOverlayLayer) return null;
    return wallpaperEffectsRenderer.resolveKeyOverlayBlendPaint(
        inputs.keyFaceWallpaperOverlayBlendMode);
  }

  void drawKeyFaceOverlayIfAny(
      @NonNull Canvas canvas,
      @NonNull KeyboardKey key,
      @NonNull int[] drawableState,
      @NonNull DrawInputs inputs,
      boolean hasKeyOverlayEffects,
      boolean needsKeyOverlayLayer,
      @Nullable Paint keyOverlayBlendPaint) {
    if (inputs.keyFaceWallpaperOverlayPaint == null) return;

    final int baseAlpha = inputs.keyFaceWallpaperOverlayPaint.getAlpha();
    final int desiredAlpha =
        resolveKeyFaceOverlayAlphaForKey(key, drawableState, inputs, baseAlpha);
    final boolean alphaChanged = desiredAlpha != baseAlpha;
    if (alphaChanged) inputs.keyFaceWallpaperOverlayPaint.setAlpha(desiredAlpha);
    int blendSaveCount = Integer.MIN_VALUE;
    try {
      if (needsKeyOverlayLayer) {
        blendSaveCount = canvas.saveLayer(0f, 0f, key.width, key.height, keyOverlayBlendPaint);
      }
      switch (inputs.keyFaceWallpaperOverlayMode) {
        case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TINT:
          final float tintRadius =
              KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
                  inputs.keyBackground, key.width, key.height);
          canvas.drawRoundRect(
              0f,
              0f,
              key.width,
              key.height,
              tintRadius,
              tintRadius,
              inputs.keyFaceWallpaperOverlayPaint);
          break;
        case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE:
          if (inputs.keyFaceWallpaperOverlayMatchKeyShape) {
            overlayMaskRenderer.drawKeyTextureOverlayWithMask(
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
                0f, 0f, key.width, key.height, radius, radius, inputs.keyFaceWallpaperOverlayPaint);
          }
          break;
        case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY:
        default:
          break;
      }
      if (hasKeyOverlayEffects) {
        wallpaperEffectsRenderer.drawKeyFacePhotoEffectOverlaysIfAny(
            canvas,
            inputs,
            desiredAlpha,
            inputs.keyFaceWallpaperOverlayMatchKeyShape,
            key,
            overlayMaskRenderer);
      }
    } catch (RuntimeException | OutOfMemoryError ignored) {
      // Key wallpaper overlays should never crash the IME. If a renderer or device cannot
      // handle the shader/layer/mask combination, skip the overlay for this draw pass.
    } finally {
      if (blendSaveCount != Integer.MIN_VALUE) {
        try {
          canvas.restoreToCount(blendSaveCount);
        } catch (RuntimeException ignored) {
          // ignore
        }
      }
      if (alphaChanged) inputs.keyFaceWallpaperOverlayPaint.setAlpha(baseAlpha);
    }
  }

  void drawFallbackKeyFaceOverlayIfAny(
      @NonNull Canvas canvas,
      @NonNull KeyboardKey key,
      @NonNull int[] drawableState,
      @NonNull DrawInputs inputs) {
    if (inputs.keyFaceWallpaperOverlayPaint == null) return;

    final int baseAlpha = inputs.keyFaceWallpaperOverlayPaint.getAlpha();
    final int desiredAlpha =
        resolveKeyFaceOverlayAlphaForKey(key, drawableState, inputs, baseAlpha);
    final boolean alphaChanged = desiredAlpha != baseAlpha;
    if (alphaChanged) inputs.keyFaceWallpaperOverlayPaint.setAlpha(desiredAlpha);
    try {
      switch (inputs.keyFaceWallpaperOverlayMode) {
        case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TINT:
          final float tintRadius =
              KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
                  inputs.keyBackground, key.width, key.height);
          canvas.drawRoundRect(
              0f,
              0f,
              key.width,
              key.height,
              tintRadius,
              tintRadius,
              inputs.keyFaceWallpaperOverlayPaint);
          break;
        case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE:
          if (inputs.keyFaceWallpaperOverlayMatchKeyShape) {
            overlayMaskRenderer.drawKeyTextureOverlayWithMask(
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
                0f, 0f, key.width, key.height, radius, radius, inputs.keyFaceWallpaperOverlayPaint);
          }
          break;
        case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY:
        default:
          break;
      }

      wallpaperEffectsRenderer.drawKeyFacePhotoEffectOverlaysIfAny(
          canvas,
          inputs,
          desiredAlpha,
          inputs.keyFaceWallpaperOverlayMatchKeyShape,
          key,
          overlayMaskRenderer);
    } catch (RuntimeException | OutOfMemoryError ignored) {
      // If the fallback path also fails, keep going without overlays.
    } finally {
      if (alphaChanged) inputs.keyFaceWallpaperOverlayPaint.setAlpha(baseAlpha);
    }
  }

  boolean tryDrawKeyboardTextureOverlayStackWithMask(
      @NonNull Canvas canvas, @NonNull Rect dirtyRect, @NonNull DrawInputs inputs) {
    boolean overlayApplied = false;
    Trace.beginSection("NSK.WallpaperKeyOverlay");
    try {
      if (inputs.allowExpensiveWallpaperEffects) {
        try {
          Trace.beginSection("NSK.WallpaperKeyOverlayMask");
          final Bitmap normalMask;
          try {
            final Paint overlayPaint = inputs.keyFaceWallpaperOverlayPaint;
            final int baseAlpha =
                overlayPaint != null ? overlayPaint.getAlpha() : Integer.MIN_VALUE;
            final int specialAlpha = inputs.keyFaceWallpaperOverlaySpecialKeyAlpha;
            final int spacebarAlpha = inputs.keyFaceWallpaperOverlaySpacebarAlpha;
            final int modifierAlpha = inputs.keyFaceWallpaperOverlayModifierKeyAlpha;
            final int enterAlpha = inputs.keyFaceWallpaperOverlayEnterKeyAlpha;

            final boolean splitSpecialKeys =
                overlayPaint != null
                    && specialAlpha != Integer.MIN_VALUE
                    && specialAlpha != baseAlpha;
            final boolean splitSpacebar =
                overlayPaint != null
                    && spacebarAlpha != Integer.MIN_VALUE
                    && spacebarAlpha != baseAlpha;
            final int effectiveSpecialBaseAlpha = splitSpecialKeys ? specialAlpha : baseAlpha;
            final boolean splitModifierKeys =
                overlayPaint != null
                    && modifierAlpha != Integer.MIN_VALUE
                    && modifierAlpha != effectiveSpecialBaseAlpha;
            final boolean splitEnterKey =
                overlayPaint != null
                    && enterAlpha != Integer.MIN_VALUE
                    && enterAlpha != effectiveSpecialBaseAlpha;

            normalMask =
                overlayMaskRenderer.ensureKeyFaceUnionMask(
                    inputs,
                    splitSpecialKeys,
                    splitSpacebar,
                    splitModifierKeys,
                    splitEnterKey,
                    inputs.keyFaceWallpaperOverlayMatchKeyShape);
          } finally {
            Trace.endSection();
          }
          final Paint overlayPaint = inputs.keyFaceWallpaperOverlayPaint;
          if (normalMask != null && !normalMask.isRecycled() && overlayPaint != null) {
            Trace.beginSection("NSK.WallpaperKeyOverlayDraw");
            final int baseAlpha = overlayPaint.getAlpha();
            try {
              wallpaperEffectsRenderer.ensureUpdated(inputs);

              final int specialAlpha = inputs.keyFaceWallpaperOverlaySpecialKeyAlpha;
              final int spacebarAlpha = inputs.keyFaceWallpaperOverlaySpacebarAlpha;
              final int modifierAlpha = inputs.keyFaceWallpaperOverlayModifierKeyAlpha;
              final int enterAlpha = inputs.keyFaceWallpaperOverlayEnterKeyAlpha;
              final boolean splitSpecialKeys =
                  specialAlpha != Integer.MIN_VALUE && specialAlpha != baseAlpha;
              final boolean splitSpacebar =
                  spacebarAlpha != Integer.MIN_VALUE && spacebarAlpha != baseAlpha;
              final int effectiveSpecialBaseAlpha = splitSpecialKeys ? specialAlpha : baseAlpha;
              final boolean splitModifierKeys =
                  modifierAlpha != Integer.MIN_VALUE && modifierAlpha != effectiveSpecialBaseAlpha;
              final boolean splitEnterKey =
                  enterAlpha != Integer.MIN_VALUE && enterAlpha != effectiveSpecialBaseAlpha;

              final Bitmap specialMask = overlayMaskRenderer.getCachedKeyFaceUnionMaskSpecial();
              final Bitmap spacebarMask = overlayMaskRenderer.getCachedKeyFaceUnionMaskSpacebar();
              final Bitmap modifierMask = overlayMaskRenderer.getCachedKeyFaceUnionMaskModifier();
              final Bitmap enterMask = overlayMaskRenderer.getCachedKeyFaceUnionMaskEnter();
              if ((splitSpecialKeys && (specialMask == null || specialMask.isRecycled()))
                  || (splitSpacebar && (spacebarMask == null || spacebarMask.isRecycled()))
                  || (splitModifierKeys && (modifierMask == null || modifierMask.isRecycled()))
                  || (splitEnterKey && (enterMask == null || enterMask.isRecycled()))) {
                overlayApplied = false;
              } else {
                overlayApplied =
                    overlayMaskRenderer.drawKeyboardTextureOverlayStackWithMask(
                        canvas,
                        dirtyRect,
                        normalMask,
                        overlayPaint,
                        inputs,
                        wallpaperEffectsRenderer);
                if (overlayApplied && splitSpecialKeys) {
                  overlayPaint.setAlpha(specialAlpha);
                  overlayApplied =
                      overlayMaskRenderer.drawKeyboardTextureOverlayStackWithMask(
                          canvas,
                          dirtyRect,
                          specialMask,
                          overlayPaint,
                          inputs,
                          wallpaperEffectsRenderer);
                }
                if (overlayApplied && splitModifierKeys) {
                  overlayPaint.setAlpha(modifierAlpha);
                  overlayApplied =
                      overlayMaskRenderer.drawKeyboardTextureOverlayStackWithMask(
                          canvas,
                          dirtyRect,
                          modifierMask,
                          overlayPaint,
                          inputs,
                          wallpaperEffectsRenderer);
                }
                if (overlayApplied && splitEnterKey) {
                  overlayPaint.setAlpha(enterAlpha);
                  overlayApplied =
                      overlayMaskRenderer.drawKeyboardTextureOverlayStackWithMask(
                          canvas,
                          dirtyRect,
                          enterMask,
                          overlayPaint,
                          inputs,
                          wallpaperEffectsRenderer);
                }
                if (overlayApplied && splitSpacebar) {
                  overlayPaint.setAlpha(spacebarAlpha);
                  overlayApplied =
                      overlayMaskRenderer.drawKeyboardTextureOverlayStackWithMask(
                          canvas,
                          dirtyRect,
                          spacebarMask,
                          overlayPaint,
                          inputs,
                          wallpaperEffectsRenderer);
                }
              }
            } finally {
              if (overlayPaint.getAlpha() != baseAlpha) overlayPaint.setAlpha(baseAlpha);
              Trace.endSection();
            }
          }
        } catch (RuntimeException | OutOfMemoryError ignored) {
          overlayApplied = false;
        }
      }
    } finally {
      Trace.endSection();
    }
    return overlayApplied;
  }

  private static int resolveKeyFaceOverlayAlphaForKey(
      @NonNull KeyboardKey key,
      @NonNull int[] drawableState,
      @NonNull DrawInputs inputs,
      int baseAlpha) {
    final int primaryCode = key.getPrimaryCode();
    if (primaryCode == KeyCodes.SPACE) {
      return inputs.keyFaceWallpaperOverlaySpacebarAlpha != Integer.MIN_VALUE
          ? inputs.keyFaceWallpaperOverlaySpacebarAlpha
          : baseAlpha;
    }
    if (KeyDrawHelper.isEnterKey(primaryCode)) {
      return inputs.keyFaceWallpaperOverlayEnterKeyAlpha != Integer.MIN_VALUE
          ? inputs.keyFaceWallpaperOverlayEnterKeyAlpha
          : baseAlpha;
    }
    if (KeyDrawHelper.isModifierKey(key)
        && inputs.keyFaceWallpaperOverlayModifierKeyAlpha != Integer.MIN_VALUE) {
      return inputs.keyFaceWallpaperOverlayModifierKeyAlpha;
    }
    if (KeyDrawHelper.isSpecialKey(primaryCode, drawableState, inputs)
        && inputs.keyFaceWallpaperOverlaySpecialKeyAlpha != Integer.MIN_VALUE) {
      return inputs.keyFaceWallpaperOverlaySpecialKeyAlpha;
    }
    return baseAlpha;
  }
}
