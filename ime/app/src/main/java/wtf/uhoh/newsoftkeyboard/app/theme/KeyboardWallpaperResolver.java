package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.UserManagerCompat;
import java.io.File;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;

/** Resolves the IME container wallpaper for a given {@link KeyboardTheme}. */
public class KeyboardWallpaperResolver {

  private final Context appContext;
  private final KeyboardWallpaperOverrideStore overrideStore;

  private String cachedPhotoThemeId;
  private long cachedPhotoLastModified;
  @Nullable private Bitmap cachedPhotoBitmap;
  @Nullable private CenterCropBitmapDrawable cachedPhotoBaseDrawable;
  @Nullable private ColorDrawable cachedPhotoDimOverlayDrawable;
  @Nullable private LayerDrawable cachedPhotoDimmedDrawable;
  private int cachedPhotoDimPercent;

  private int cachedKeyFaceOverlayDimPercent;
  private int cachedKeyFaceOverlayAlpha;
  private int cachedKeyFaceOverlayRotationDegrees;
  @Nullable private Paint cachedKeyFaceOverlayPaint;
  @Nullable private Shader cachedKeyFaceOverlayShader;
  private final Rect cachedKeyFaceOverlayBounds = new Rect();
  private final Matrix cachedKeyFaceOverlayMatrix = new Matrix();
  private final KeyFaceOverlay keyFaceOverlay = new KeyFaceOverlay();

  public KeyboardWallpaperResolver(@NonNull Context context) {
    appContext = context.getApplicationContext();
    overrideStore = new KeyboardWallpaperOverrideStore(appContext);
    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
  }

  @NonNull
  public Drawable resolveImeWallpaper(@Nullable KeyboardTheme theme) {
    final Drawable fallback = ContextCompat.getDrawable(appContext, R.drawable.nsk_wallpaper);
    if (theme == null || fallback == null) return fallback;

    final Drawable photoOverride = resolvePhotoOverrideInternal(theme);
    if (photoOverride != null) return photoOverride;

    final Drawable themeWallpaper = resolveThemeWallpaper(theme);
    return themeWallpaper != null ? themeWallpaper : fallback;
  }

  /** Returns the theme-provided wallpaper (if any), without applying user photo overrides. */
  @NonNull
  public Drawable resolveThemeWallpaperOrFallback(@Nullable KeyboardTheme theme) {
    final Drawable fallback = ContextCompat.getDrawable(appContext, R.drawable.nsk_wallpaper);
    if (theme == null || fallback == null) return fallback;

    final Drawable themeWallpaper = resolveThemeWallpaper(theme);
    return themeWallpaper != null ? themeWallpaper : fallback;
  }

  @Nullable
  public Drawable resolvePhotoOverrideIfAny(@Nullable KeyboardTheme theme) {
    if (theme == null) return null;
    return resolvePhotoOverrideInternal(theme);
  }

  @Nullable
  private Drawable resolvePhotoOverrideInternal(@NonNull KeyboardTheme theme) {
    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return null;
    }

    final String themeId = theme.getId();
    if (cachedPhotoThemeId != null && !cachedPhotoThemeId.equals(themeId)) {
      clearPhotoCache();
    }

    if (overrideStore.isWallpaperInvalid(themeId)) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return null;
    }

    final File file = overrideStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return null;
    }

    final long lastModified = file.lastModified();
    final int dimPercent = overrideStore.getDimPercent(themeId);
    final CenterCropBitmapDrawable baseDrawable = ensurePhotoLoaded(themeId, file, lastModified);
    if (baseDrawable == null) return null;

    baseDrawable.setRotationDegrees(overrideStore.getWallpaperRotationDegrees(themeId));
    return applyDimOverlayCached(baseDrawable, dimPercent);
  }

  @Nullable
  protected Bitmap decodePhotoFile(@NonNull File file) {
    try {
      return BitmapFactory.decodeFile(file.getAbsolutePath());
    } catch (OutOfMemoryError oom) {
      return null;
    }
  }

  @Nullable
  private Drawable resolveThemeWallpaper(@NonNull KeyboardTheme theme) {
    final Context packageContext = theme.getPackageContext();
    if (packageContext == null) return null;

    final AddOn.AddOnResourceMapping resourceMapping = theme.getResourceMapping();
    final int[] remoteKeyboardThemeStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewTheme);
    if (remoteKeyboardThemeStyleable.length == 0) return null;

    final TypedArray a =
        packageContext.obtainStyledAttributes(theme.getThemeResId(), remoteKeyboardThemeStyleable);
    try {
      final int attrCount = a.getIndexCount();
      for (int i = 0; i < attrCount; i++) {
        final int remoteIndex = a.getIndex(i);
        final int localAttrId =
            resourceMapping.getLocalAttrId(remoteKeyboardThemeStyleable[remoteIndex]);
        if (localAttrId == R.attr.keyboardWallpaper) {
          return a.getDrawable(remoteIndex);
        }
      }
      return null;
    } finally {
      a.recycle();
    }
  }

  private static int clampPercent(int value) {
    if (value < 0) return 0;
    if (value > 100) return 100;
    return value;
  }

  private void clearPhotoCache() {
    cachedPhotoThemeId = null;
    cachedPhotoLastModified = 0L;
    cachedPhotoDimPercent = 0;
    cachedPhotoBitmap = null;
    cachedPhotoBaseDrawable = null;
    cachedPhotoDimOverlayDrawable = null;
    cachedPhotoDimmedDrawable = null;

    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    cachedKeyFaceOverlayPaint = null;
    cachedKeyFaceOverlayShader = null;
    cachedKeyFaceOverlayBounds.setEmpty();
    keyFaceOverlay.reset();
  }

  @Nullable
  private CenterCropBitmapDrawable ensurePhotoLoaded(
      @NonNull String themeId, @NonNull File file, long lastModified) {
    if (cachedPhotoBaseDrawable != null
        && themeId.equals(cachedPhotoThemeId)
        && cachedPhotoLastModified == lastModified) {
      return cachedPhotoBaseDrawable;
    }

    final Bitmap bitmap = decodePhotoFile(file);
    if (bitmap == null) {
      // treat as invalid/corrupt and fall back to theme wallpaper
      //noinspection ResultOfMethodCallIgnored
      file.delete();
      overrideStore.markWallpaperInvalid(themeId);
      clearPhotoCache();
      return null;
    }

    cachedPhotoThemeId = themeId;
    cachedPhotoLastModified = lastModified;
    cachedPhotoBitmap = bitmap;
    cachedPhotoBaseDrawable = new CenterCropBitmapDrawable(bitmap);
    cachedPhotoDimOverlayDrawable = null;
    cachedPhotoDimmedDrawable = null;

    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    cachedKeyFaceOverlayPaint = null;
    cachedKeyFaceOverlayShader = null;
    cachedKeyFaceOverlayBounds.setEmpty();
    keyFaceOverlay.reset();

    return cachedPhotoBaseDrawable;
  }

  @NonNull
  private Drawable applyDimOverlayCached(
      @NonNull CenterCropBitmapDrawable baseDrawable, int dimPercent) {
    final int clamped = clampPercent(dimPercent);
    if (clamped <= 0) {
      cachedPhotoDimPercent = 0;
      return baseDrawable;
    }

    if (cachedPhotoDimmedDrawable == null) {
      cachedPhotoDimOverlayDrawable = new ColorDrawable(Color.BLACK);
      cachedPhotoDimmedDrawable =
          new LayerDrawable(new Drawable[] {baseDrawable, cachedPhotoDimOverlayDrawable});
      cachedPhotoDimPercent = Integer.MIN_VALUE;
    }

    if (cachedPhotoDimPercent != clamped && cachedPhotoDimOverlayDrawable != null) {
      cachedPhotoDimOverlayDrawable.setAlpha(Math.round(255f * (clamped / 100f)));
      cachedPhotoDimPercent = clamped;
    }

    return cachedPhotoDimmedDrawable;
  }

  /**
   * Resolves the user photo overlay to be drawn on top of key backgrounds (key faces).
   *
   * <p>This is intentionally separate from the view background drawable so the overlay can be
   * anchored to the keyboard view bounds and appear continuous across keys.
   */
  @NonNull
  public KeyFaceOverlay resolveKeyFaceOverlay(
      @Nullable KeyboardTheme theme, @NonNull Rect keyboardViewBounds) {
    keyFaceOverlay.reset();
    if (theme == null) return keyFaceOverlay;

    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return keyFaceOverlay;
    }

    final String themeId = theme.getId();
    if (cachedPhotoThemeId != null && !cachedPhotoThemeId.equals(themeId)) {
      clearPhotoCache();
    }

    final int mode = overrideStore.getWallpaperMode(themeId);
    if (mode == KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY)
      return keyFaceOverlay;

    final int alphaPercent = overrideStore.getKeyAlphaPercent(themeId);
    if (alphaPercent <= 0) return keyFaceOverlay;

    if (overrideStore.isWallpaperInvalid(themeId)) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return keyFaceOverlay;
    }

    final File file = overrideStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return keyFaceOverlay;
    }

    final CenterCropBitmapDrawable baseDrawable =
        ensurePhotoLoaded(themeId, file, file.lastModified());
    if (baseDrawable == null || cachedPhotoBitmap == null) return keyFaceOverlay;

    if (cachedKeyFaceOverlayPaint == null || cachedKeyFaceOverlayShader == null) {
      cachedKeyFaceOverlayShader =
          new android.graphics.BitmapShader(
              cachedPhotoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      cachedKeyFaceOverlayPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
      cachedKeyFaceOverlayPaint.setShader(cachedKeyFaceOverlayShader);
      cachedKeyFaceOverlayBounds.setEmpty();
      cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
      cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
      cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    }

    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    // Ensure the shader mapping is anchored to the full keyboard view bounds, not per-key bounds.
    if (!cachedKeyFaceOverlayBounds.equals(keyboardViewBounds)
        || cachedKeyFaceOverlayRotationDegrees != rotationDegrees) {
      cachedKeyFaceOverlayBounds.set(keyboardViewBounds);
      updateCenterCropMatrix(
          cachedKeyFaceOverlayMatrix,
          cachedPhotoBitmap.getWidth(),
          cachedPhotoBitmap.getHeight(),
          cachedKeyFaceOverlayBounds,
          rotationDegrees);
      if (cachedKeyFaceOverlayShader instanceof android.graphics.BitmapShader bitmapShader) {
        bitmapShader.setLocalMatrix(cachedKeyFaceOverlayMatrix);
      }
      cachedKeyFaceOverlayRotationDegrees = rotationDegrees;
    }

    if (cachedKeyFaceOverlayAlpha != alphaPercent) {
      cachedKeyFaceOverlayPaint.setAlpha(Math.round(255f * (clampPercent(alphaPercent) / 100f)));
      cachedKeyFaceOverlayAlpha = alphaPercent;
    }

    final int dimPercent = overrideStore.getDimPercent(themeId);
    if (cachedKeyFaceOverlayDimPercent != dimPercent) {
      cachedKeyFaceOverlayPaint.setColorFilter(createDimColorFilter(dimPercent));
      cachedKeyFaceOverlayDimPercent = dimPercent;
    }

    keyFaceOverlay.mode = mode;
    keyFaceOverlay.paint = cachedKeyFaceOverlayPaint;
    keyFaceOverlay.matchKeyShape =
        mode == KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE
            && overrideStore.isMatchKeyShapeEnabled(themeId);
    return keyFaceOverlay;
  }

  @Nullable
  private static ColorFilter createDimColorFilter(int dimPercent) {
    final int clamped = clampPercent(dimPercent);
    if (clamped <= 0) return null;

    final float factor = 1f - (clamped / 100f);
    final ColorMatrix matrix =
        new ColorMatrix(
            new float[] {
              factor, 0f, 0f, 0f, 0f, 0f, factor, 0f, 0f, 0f, 0f, 0f, factor, 0f, 0f, 0f, 0f, 0f,
              1f, 0f
            });
    return new ColorMatrixColorFilter(matrix);
  }

  private static void updateCenterCropMatrix(
      @NonNull Matrix outMatrix,
      int bitmapWidth,
      int bitmapHeight,
      @NonNull Rect bounds,
      int rotationDegrees) {
    if (bitmapWidth <= 0 || bitmapHeight <= 0) return;

    final int rotation = normalizeRotationDegrees(rotationDegrees);

    final float boundsW = bounds.width();
    final float boundsH = bounds.height();
    if (boundsW <= 0f || boundsH <= 0f) return;

    final float effectiveW = (rotation == 90 || rotation == 270) ? bitmapHeight : bitmapWidth;
    final float effectiveH = (rotation == 90 || rotation == 270) ? bitmapWidth : bitmapHeight;

    // Scale so the (possibly rotated) bitmap fully covers bounds.
    final float scale = Math.max(boundsW / effectiveW, boundsH / effectiveH);

    outMatrix.reset();
    outMatrix.postTranslate(-bitmapWidth / 2f, -bitmapHeight / 2f);
    if (rotation != 0) {
      outMatrix.postRotate(rotation);
    }
    outMatrix.postScale(scale, scale);
    outMatrix.postTranslate(bounds.exactCenterX(), bounds.exactCenterY());
  }

  private static int normalizeRotationDegrees(int rotationDegrees) {
    final int normalized = ((rotationDegrees % 360) + 360) % 360;
    switch (normalized) {
      case 0:
      case 90:
      case 180:
      case 270:
        return normalized;
      default:
        return 0;
    }
  }

  public static final class KeyFaceOverlay {
    private int mode = KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY;
    @Nullable private Paint paint;
    private boolean matchKeyShape = false;

    public int mode() {
      return mode;
    }

    @Nullable
    public Paint paint() {
      return paint;
    }

    public boolean matchKeyShape() {
      return matchKeyShape;
    }

    void reset() {
      mode = KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY;
      paint = null;
      matchKeyShape = false;
    }
  }

  private static final class CenterCropBitmapDrawable extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint;
    private final Shader bitmapShader;
    private final Matrix shaderMatrix = new Matrix();
    private int alpha = 0xFF;
    private int rotationDegrees = 0;

    CenterCropBitmapDrawable(@NonNull Bitmap bitmap) {
      this.bitmap = bitmap;
      this.paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
      this.bitmapShader =
          new android.graphics.BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      this.paint.setShader(bitmapShader);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      updateShaderMatrix(bounds);
    }

    void setRotationDegrees(int rotationDegrees) {
      final int normalized = normalizeRotationDegrees(rotationDegrees);
      if (this.rotationDegrees == normalized) return;
      this.rotationDegrees = normalized;
      updateShaderMatrix(getBounds());
    }

    private void updateShaderMatrix(@NonNull Rect bounds) {
      updateCenterCropMatrix(
          shaderMatrix, bitmap.getWidth(), bitmap.getHeight(), bounds, rotationDegrees);
      ((android.graphics.BitmapShader) bitmapShader).setLocalMatrix(shaderMatrix);
      invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
      paint.setAlpha(alpha);
      canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int alpha) {
      this.alpha = alpha;
      invalidateSelf();
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
  }
}
