package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Random;

final class GrainDrawable extends Drawable {
  private static final int NOISE_SIZE_PX = 64;
  private static final long NOISE_SEED = 0x4E534B475241494EL; // "NSKGRAIN"
  @Nullable private static Bitmap cachedNoiseBitmap;
  private final Paint paint = new Paint();
  private final android.graphics.BitmapShader shader;
  private final Matrix shaderMatrix = new Matrix();
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  private int strengthPercent = 0;
  private int scalePercent = KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT;

  GrainDrawable() {
    paint.setStyle(Paint.Style.FILL);
    paint.setFilterBitmap(false);
    paint.setDither(false);
    shader = createNoiseShader();
    paint.setShader(shader);
    updateShaderMatrix();
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paint, normalized);
    invalidateSelf();
  }

  void setStrengthPercent(int strengthPercent) {
    final int clamped = KeyboardWallpaperRenderMath.clampPercent(strengthPercent);
    if (this.strengthPercent == clamped) return;
    this.strengthPercent = clamped;
    paint.setAlpha(KeyboardWallpaperRenderMath.alphaForPercent(clamped));
    invalidateSelf();
  }

  void setScalePercent(int scalePercent) {
    final int clamped = Math.max(25, Math.min(400, scalePercent));
    if (this.scalePercent == clamped) return;
    this.scalePercent = clamped;
    updateShaderMatrix();
    invalidateSelf();
  }

  private void updateShaderMatrix() {
    final float scale = scalePercent / 100f;
    shaderMatrix.reset();
    shaderMatrix.setScale(scale, scale);
    shader.setLocalMatrix(shaderMatrix);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (strengthPercent <= 0) return;
    canvas.drawRect(getBounds(), paint);
  }

  @Override
  public void setAlpha(int alpha) {
    // no-op: strength controls alpha
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @NonNull
  private static android.graphics.BitmapShader createNoiseShader() {
    final Bitmap bitmap = getNoiseBitmap();
    return new android.graphics.BitmapShader(
        bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
  }

  @NonNull
  private static Bitmap getNoiseBitmap() {
    if (cachedNoiseBitmap != null && !cachedNoiseBitmap.isRecycled()) return cachedNoiseBitmap;
    synchronized (GrainDrawable.class) {
      if (cachedNoiseBitmap != null && !cachedNoiseBitmap.isRecycled()) return cachedNoiseBitmap;
      cachedNoiseBitmap = generateNoiseBitmap();
      return cachedNoiseBitmap;
    }
  }

  @NonNull
  private static Bitmap generateNoiseBitmap() {
    final int size = NOISE_SIZE_PX;
    final int[] pixels = new int[size * size];
    final Random random = new Random(NOISE_SEED);
    for (int i = 0; i < pixels.length; i++) {
      final int jitter = random.nextInt(65) - 32; // [-32..32]
      final int value = 128 + jitter;
      pixels[i] = 0xFF000000 | (value << 16) | (value << 8) | value;
    }
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
    return bitmap;
  }
}
