package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.utils.EmojiUtils;

/** Handles drawing of key labels (main text) to keep {@link KeyboardViewBase#onDraw} smaller. */
final class KeyLabelRenderer {

  interface KeyTextPaintSetter {
    void setPaintToKeyText(Paint paint);
  }

  interface KeyboardNamePaintSetter {
    void setPaintForKeyboardNameText(Paint paint);
  }

  interface LabelTextPaintSetter {
    void setPaintForLabelText(Paint paint);
  }

  interface TextSizeAdjuster {
    float adjust(Paint paint, CharSequence label, int width);
  }

  void drawLabel(
      Canvas canvas,
      Paint paint,
      CharSequence label,
      KeyboardKey key,
      Rect keyBackgroundPadding,
      boolean keyIsSpace,
      float keyboardNameTextSize,
      boolean ellipsizeKeyLabels,
      boolean alwaysUseDrawText,
      KeyTextPaintSetter keyTextPaintSetter,
      KeyboardNamePaintSetter keyboardNamePaintSetter,
      LabelTextPaintSetter labelTextPaintSetter,
      TextSizeAdjuster textSizeAdjuster,
      float shadowRadius,
      float shadowOffsetX,
      float shadowOffsetY,
      int shadowColor) {
    if (keyIsSpace) {
      keyboardNamePaintSetter.setPaintForKeyboardNameText(paint);
      paint.setTextSize(keyboardNameTextSize);
    } else if (label.length() > 1 && key.getCodesCount() < 2) {
      labelTextPaintSetter.setPaintForLabelText(paint);
    } else {
      keyTextPaintSetter.setPaintToKeyText(paint);
    }

    if (EmojiUtils.isLabelOfEmoji(label)) {
      paint.setTextSize(1.35f * paint.getTextSize());
    }

    paint.setShadowLayer(shadowRadius, shadowOffsetX, shadowOffsetY, shadowColor);

    final int availableWidth =
        Math.max(1, key.width - keyBackgroundPadding.left - keyBackgroundPadding.right);
    final float textWidth = textSizeAdjuster.adjust(paint, label, availableWidth);
    CharSequence labelToDraw = label;
    float effectiveTextWidth = textWidth;
    if (ellipsizeKeyLabels && textWidth > availableWidth && label.length() > 1) {
      final TextPaint ellipsizePaint = new TextPaint(paint);
      labelToDraw =
          TextUtils.ellipsize(label, ellipsizePaint, availableWidth, TextUtils.TruncateAt.END);
      effectiveTextWidth =
          Math.min(availableWidth, paint.measureText(labelToDraw, 0, labelToDraw.length()));
    }
    final Paint.FontMetrics fm = paint.getFontMetrics();
    final float labelHeight = -fm.top;

    final float centerY =
        keyBackgroundPadding.top
            + ((float) (key.height - keyBackgroundPadding.top - keyBackgroundPadding.bottom)
                / (keyIsSpace ? 3 : 2));

    final float textX =
        keyBackgroundPadding.left
            + (key.width - keyBackgroundPadding.left - keyBackgroundPadding.right) / 2f;

    float textY;
    float translateX = textX;
    final boolean labelHasSpans =
        labelToDraw instanceof Spanned
            && ((Spanned) labelToDraw).getSpans(0, labelToDraw.length(), Object.class).length > 0;
    final boolean shouldUseStaticLayout =
        (labelToDraw.length() > 1 && !alwaysUseDrawText) || labelHasSpans;
    if (shouldUseStaticLayout) {
      final int layoutWidth =
          Math.max(1, Math.min((int) Math.ceil(effectiveTextWidth), availableWidth));
      textY = centerY - ((labelHeight - paint.descent()) / 2);
      translateX = textX - (layoutWidth / 2f);
      canvas.translate(translateX, textY);
      final TextPaint layoutPaint = new TextPaint(paint);
      layoutPaint.setTextAlign(Paint.Align.LEFT);
      StaticLayout labelText =
          new StaticLayout(
              labelToDraw, layoutPaint, layoutWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
      labelText.draw(canvas);
    } else {
      textY = centerY + ((labelHeight - paint.descent()) / 2);
      canvas.translate(translateX, textY);
      canvas.drawText(labelToDraw, 0, labelToDraw.length(), 0, 0, paint);
    }
    canvas.translate(-translateX, -textY);
    paint.setShadowLayer(0, 0, 0, 0);
  }
}
