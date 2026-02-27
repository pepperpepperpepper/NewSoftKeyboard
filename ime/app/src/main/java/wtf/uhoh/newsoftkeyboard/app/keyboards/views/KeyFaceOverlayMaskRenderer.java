package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;

final class KeyFaceOverlayMaskRenderer {

  private final Paint keyFaceUnionMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF keyFaceUnionMaskRect = new RectF();
  private final Canvas keyFaceUnionMaskCanvas = new Canvas();
  private final Canvas keyFaceUnionMaskSpecialCanvas = new Canvas();
  private final Canvas keyFaceUnionMaskSpacebarCanvas = new Canvas();
  private final Canvas keyFaceUnionMaskModifierCanvas = new Canvas();
  private final Canvas keyFaceUnionMaskEnterCanvas = new Canvas();

  @Nullable private Bitmap cachedKeyFaceUnionMask;
  @Nullable private Bitmap cachedKeyFaceUnionMaskSpecial;
  @Nullable private Bitmap cachedKeyFaceUnionMaskSpacebar;
  @Nullable private Bitmap cachedKeyFaceUnionMaskModifier;
  @Nullable private Bitmap cachedKeyFaceUnionMaskEnter;
  private int cachedKeyFaceUnionMaskWidth;
  private int cachedKeyFaceUnionMaskHeight;
  private int cachedKeyFaceUnionMaskPaddingLeft;
  private int cachedKeyFaceUnionMaskPaddingTop;
  @Nullable private Object cachedKeyFaceUnionMaskDrawableKey;
  @Nullable private Object cachedKeyFaceUnionMaskKeyboardKey;
  private int cachedKeyFaceUnionMaskKeyCount;
  private boolean cachedKeyFaceUnionMaskSplitSpecial;
  private boolean cachedKeyFaceUnionMaskSplitSpacebar;
  private boolean cachedKeyFaceUnionMaskSplitModifier;
  private boolean cachedKeyFaceUnionMaskSplitEnter;
  private boolean cachedKeyFaceUnionMaskUsesAlphaMasks;

  KeyFaceOverlayMaskRenderer() {
    keyFaceUnionMaskPaint.setColor(Color.BLACK);
    keyFaceUnionMaskPaint.setStyle(Paint.Style.FILL);
  }

  @Nullable
  Bitmap getCachedKeyFaceUnionMaskSpecial() {
    return cachedKeyFaceUnionMaskSpecial;
  }

  @Nullable
  Bitmap getCachedKeyFaceUnionMaskSpacebar() {
    return cachedKeyFaceUnionMaskSpacebar;
  }

  @Nullable
  Bitmap getCachedKeyFaceUnionMaskModifier() {
    return cachedKeyFaceUnionMaskModifier;
  }

  @Nullable
  Bitmap getCachedKeyFaceUnionMaskEnter() {
    return cachedKeyFaceUnionMaskEnter;
  }

  @Nullable
  Bitmap ensureKeyFaceUnionMask(
      @NonNull DrawInputs inputs,
      boolean splitSpecialKeys,
      boolean splitSpacebar,
      boolean splitModifierKeys,
      boolean splitEnterKey,
      boolean useAlphaMasks) {
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
        && cachedKeyFaceUnionMaskKeyCount == keyCount
        && cachedKeyFaceUnionMaskSplitSpecial == splitSpecialKeys
        && cachedKeyFaceUnionMaskSplitSpacebar == splitSpacebar
        && cachedKeyFaceUnionMaskSplitModifier == splitModifierKeys
        && cachedKeyFaceUnionMaskSplitEnter == splitEnterKey
        && cachedKeyFaceUnionMaskUsesAlphaMasks == useAlphaMasks
        && (!splitSpecialKeys
            || (cachedKeyFaceUnionMaskSpecial != null
                && !cachedKeyFaceUnionMaskSpecial.isRecycled()))
        && (!splitSpacebar
            || (cachedKeyFaceUnionMaskSpacebar != null
                && !cachedKeyFaceUnionMaskSpacebar.isRecycled()))
        && (!splitModifierKeys
            || (cachedKeyFaceUnionMaskModifier != null
                && !cachedKeyFaceUnionMaskModifier.isRecycled()))
        && (!splitEnterKey
            || (cachedKeyFaceUnionMaskEnter != null
                && !cachedKeyFaceUnionMaskEnter.isRecycled()))) {
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

      final Bitmap specialMask;
      final Bitmap spacebarMask;
      final Bitmap modifierMask;
      final Bitmap enterMask;
      if (splitSpecialKeys) {
        final Bitmap cachedSpecial = cachedKeyFaceUnionMaskSpecial;
        if (cachedSpecial != null
            && !cachedSpecial.isRecycled()
            && cachedSpecial.getWidth() == width
            && cachedSpecial.getHeight() == height) {
          specialMask = cachedSpecial;
        } else {
          if (cachedSpecial != null && !cachedSpecial.isRecycled()) cachedSpecial.recycle();
          try {
            specialMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
          } catch (OutOfMemoryError oom) {
            cachedKeyFaceUnionMask = null;
            return null;
          }
        }
        specialMask.eraseColor(Color.TRANSPARENT);
        keyFaceUnionMaskSpecialCanvas.setBitmap(specialMask);
      } else {
        final Bitmap cachedSpecial = cachedKeyFaceUnionMaskSpecial;
        if (cachedSpecial != null && !cachedSpecial.isRecycled()) cachedSpecial.recycle();
        cachedKeyFaceUnionMaskSpecial = null;
        specialMask = null;
      }

      if (splitSpacebar) {
        final Bitmap cachedSpace = cachedKeyFaceUnionMaskSpacebar;
        if (cachedSpace != null
            && !cachedSpace.isRecycled()
            && cachedSpace.getWidth() == width
            && cachedSpace.getHeight() == height) {
          spacebarMask = cachedSpace;
        } else {
          if (cachedSpace != null && !cachedSpace.isRecycled()) cachedSpace.recycle();
          try {
            spacebarMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
          } catch (OutOfMemoryError oom) {
            cachedKeyFaceUnionMask = null;
            return null;
          }
        }
        spacebarMask.eraseColor(Color.TRANSPARENT);
        keyFaceUnionMaskSpacebarCanvas.setBitmap(spacebarMask);
      } else {
        final Bitmap cachedSpace = cachedKeyFaceUnionMaskSpacebar;
        if (cachedSpace != null && !cachedSpace.isRecycled()) cachedSpace.recycle();
        cachedKeyFaceUnionMaskSpacebar = null;
        spacebarMask = null;
      }

      if (splitModifierKeys) {
        final Bitmap cachedModifier = cachedKeyFaceUnionMaskModifier;
        if (cachedModifier != null
            && !cachedModifier.isRecycled()
            && cachedModifier.getWidth() == width
            && cachedModifier.getHeight() == height) {
          modifierMask = cachedModifier;
        } else {
          if (cachedModifier != null && !cachedModifier.isRecycled()) cachedModifier.recycle();
          try {
            modifierMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
          } catch (OutOfMemoryError oom) {
            cachedKeyFaceUnionMask = null;
            return null;
          }
        }
        modifierMask.eraseColor(Color.TRANSPARENT);
        keyFaceUnionMaskModifierCanvas.setBitmap(modifierMask);
      } else {
        final Bitmap cachedModifier = cachedKeyFaceUnionMaskModifier;
        if (cachedModifier != null && !cachedModifier.isRecycled()) cachedModifier.recycle();
        cachedKeyFaceUnionMaskModifier = null;
        modifierMask = null;
      }

      if (splitEnterKey) {
        final Bitmap cachedEnter = cachedKeyFaceUnionMaskEnter;
        if (cachedEnter != null
            && !cachedEnter.isRecycled()
            && cachedEnter.getWidth() == width
            && cachedEnter.getHeight() == height) {
          enterMask = cachedEnter;
        } else {
          if (cachedEnter != null && !cachedEnter.isRecycled()) cachedEnter.recycle();
          try {
            enterMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
          } catch (OutOfMemoryError oom) {
            cachedKeyFaceUnionMask = null;
            return null;
          }
        }
        enterMask.eraseColor(Color.TRANSPARENT);
        keyFaceUnionMaskEnterCanvas.setBitmap(enterMask);
      } else {
        final Bitmap cachedEnter = cachedKeyFaceUnionMaskEnter;
        if (cachedEnter != null && !cachedEnter.isRecycled()) cachedEnter.recycle();
        cachedKeyFaceUnionMaskEnter = null;
        enterMask = null;
      }

      try {
        for (Keyboard.Key keyBase : inputs.keys) {
          final KeyboardKey key = (KeyboardKey) keyBase;
          if (key.width <= 0 || key.height <= 0) continue;

          final int left = key.x + inputs.kbdPaddingLeft;
          final int top = key.y + inputs.kbdPaddingTop;

          final int primaryCode = key.getPrimaryCode();
          final boolean keyIsSpace = primaryCode == KeyCodes.SPACE;
          final int[] drawableState = key.getCurrentDrawableState(inputs.drawableStatesProvider);

          final Canvas targetCanvas;
          if (keyIsSpace && splitSpacebar) {
            targetCanvas = keyFaceUnionMaskSpacebarCanvas;
          } else if (splitEnterKey && KeyDrawHelper.isEnterKey(primaryCode)) {
            targetCanvas = keyFaceUnionMaskEnterCanvas;
          } else if (splitModifierKeys && KeyDrawHelper.isModifierKey(key)) {
            targetCanvas = keyFaceUnionMaskModifierCanvas;
          } else if (!keyIsSpace
              && splitSpecialKeys
              && KeyDrawHelper.isSpecialKey(primaryCode, drawableState, inputs)) {
            targetCanvas = keyFaceUnionMaskSpecialCanvas;
          } else {
            targetCanvas = keyFaceUnionMaskCanvas;
          }

          final Bitmap keyMask =
              useAlphaMasks
                  ? KeyBackgroundAlphaMaskCache.resolveAlphaMask(
                      inputs.keyBackground, key.width, key.height)
                  : null;
          if (keyMask != null && !keyMask.isRecycled()) {
            targetCanvas.drawBitmap(keyMask, left, top, null);
          } else {
            final float radius =
                KeyBackgroundCornerRadiusResolver.resolveCornerRadiusOrFallback(
                    inputs.keyBackground, key.width, key.height);
            keyFaceUnionMaskRect.set(left, top, left + key.width, top + key.height);
            targetCanvas.drawRoundRect(keyFaceUnionMaskRect, radius, radius, keyFaceUnionMaskPaint);
          }
        }
      } catch (RuntimeException | OutOfMemoryError e) {
        return null;
      }

      cachedKeyFaceUnionMask = mask;
      cachedKeyFaceUnionMaskSpecial = specialMask;
      cachedKeyFaceUnionMaskSpacebar = spacebarMask;
      cachedKeyFaceUnionMaskModifier = modifierMask;
      cachedKeyFaceUnionMaskEnter = enterMask;
      cachedKeyFaceUnionMaskWidth = width;
      cachedKeyFaceUnionMaskHeight = height;
      cachedKeyFaceUnionMaskPaddingLeft = inputs.kbdPaddingLeft;
      cachedKeyFaceUnionMaskPaddingTop = inputs.kbdPaddingTop;
      cachedKeyFaceUnionMaskDrawableKey = drawableKey;
      cachedKeyFaceUnionMaskKeyboardKey = keyboardKey;
      cachedKeyFaceUnionMaskKeyCount = keyCount;
      cachedKeyFaceUnionMaskSplitSpecial = splitSpecialKeys;
      cachedKeyFaceUnionMaskSplitSpacebar = splitSpacebar;
      cachedKeyFaceUnionMaskSplitModifier = splitModifierKeys;
      cachedKeyFaceUnionMaskSplitEnter = splitEnterKey;
      cachedKeyFaceUnionMaskUsesAlphaMasks = useAlphaMasks;

      return mask;
    } finally {
      Trace.endSection();
    }
  }

  boolean drawKeyboardTextureOverlayWithMask(
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

  boolean drawKeyboardTextureOverlayStackWithMask(
      @NonNull Canvas canvas,
      @NonNull Rect dirtyRect,
      @NonNull Bitmap unionMask,
      @NonNull Paint overlayPaint,
      @NonNull DrawInputs inputs,
      @NonNull KeyFaceWallpaperEffectsRenderer effectsRenderer) {
    if (dirtyRect.isEmpty()) return false;
    final Paint blendPaint =
        effectsRenderer.resolveKeyOverlayBlendPaint(inputs.keyFaceWallpaperOverlayBlendMode);
    final int saveCount =
        canvas.saveLayer(
            dirtyRect.left, dirtyRect.top, dirtyRect.right, dirtyRect.bottom, blendPaint);

    effectsRenderer.drawKeyboardOverlayStack(canvas, dirtyRect, overlayPaint, inputs);
    canvas.drawBitmap(unionMask, 0f, 0f, KeyBackgroundAlphaMaskCache.dstInPaint());
    canvas.restoreToCount(saveCount);
    return true;
  }

  void drawKeyTextureOverlayWithMask(
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
