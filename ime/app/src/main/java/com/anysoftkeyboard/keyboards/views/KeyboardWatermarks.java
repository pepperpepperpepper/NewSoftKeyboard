package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

final class KeyboardWatermarks {

  private final int watermarkDimen;
  private final int watermarkMargin;
  private final List<Drawable> watermarks = new ArrayList<>();
  private int watermarkEdgeX = 0;

  KeyboardWatermarks(int watermarkDimen, int watermarkMargin) {
    this.watermarkDimen = watermarkDimen;
    this.watermarkMargin = watermarkMargin;
  }

  int minimumKeyboardBottomPadding() {
    return watermarkDimen + watermarkMargin;
  }

  void setWatermarkEdgeX(int watermarkEdgeX) {
    this.watermarkEdgeX = watermarkEdgeX;
  }

  void setWatermarks(@NonNull List<Drawable> watermarks) {
    this.watermarks.clear();
    this.watermarks.addAll(watermarks);
    for (Drawable watermark : this.watermarks) {
      watermark.setBounds(0, 0, watermarkDimen, watermarkDimen);
    }
  }

  void draw(@NonNull Canvas canvas, int viewHeight) {
    float watermarkX = watermarkEdgeX;
    final float watermarkY = viewHeight - watermarkDimen - watermarkMargin;
    for (Drawable watermark : watermarks) {
      watermarkX -= (watermarkDimen + watermarkMargin);
      canvas.translate(watermarkX, watermarkY);
      watermark.draw(canvas);
      canvas.translate(-watermarkX, -watermarkY);
    }
  }
}
