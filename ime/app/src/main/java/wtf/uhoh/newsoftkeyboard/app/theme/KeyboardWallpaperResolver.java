package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
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
import android.os.Trace;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.UserManagerCompat;
import java.io.File;
import java.util.WeakHashMap;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;

/** Resolves the IME container wallpaper for a given {@link KeyboardTheme}. */
public class KeyboardWallpaperResolver {

  private static final WallpaperBitmapLoader DEFAULT_WALLPAPER_BITMAP_LOADER =
      WallpaperBitmapRepository.getInstance();

  private final Context appContext;
  private final KeyboardWallpaperOverrideStore overrideStore;
  private final WallpaperBitmapLoader wallpaperBitmapLoader;

  private String cachedPhotoThemeId;
  private long cachedPhotoLastModified;
  private int cachedPhotoMaxDimBucketPx;
  @Nullable private Bitmap cachedPhotoBitmap;
  @Nullable private WallpaperBitmapDrawable cachedPhotoBaseDrawable;
  @Nullable private ColorDrawable cachedPhotoDimOverlayDrawable;
  @Nullable private LayerDrawable cachedPhotoDimmedDrawable;
  private int cachedPhotoDimPercent;

  private int cachedKeyFaceOverlayDimPercent;
  private int cachedKeyFaceOverlayAlpha;
  private int cachedKeyFaceOverlayRotationDegrees;
  private int cachedKeyFaceOverlayScaleMode;
  private int cachedKeyFaceOverlayAnchor;
  @Nullable private Paint cachedKeyFaceOverlayPaint;
  @Nullable private Shader cachedKeyFaceOverlayShader;
  private final Rect cachedKeyFaceOverlayBounds = new Rect();
  private final Matrix cachedKeyFaceOverlayMatrix = new Matrix();
  private final KeyFaceOverlay keyFaceOverlay = new KeyFaceOverlay();

  private final WeakHashMap<View, Integer> backgroundApplyTokens = new WeakHashMap<>();
  private int nextBackgroundApplyToken = 1;

  private String lastRequestedKeyFaceThemeId;
  private long lastRequestedKeyFaceLastModified;
  private int lastRequestedKeyFaceBucketPx;

  public KeyboardWallpaperResolver(@NonNull Context context) {
    this(context, DEFAULT_WALLPAPER_BITMAP_LOADER);
  }

  KeyboardWallpaperResolver(@NonNull Context context, @NonNull WallpaperBitmapLoader bitmapLoader) {
    appContext = context.getApplicationContext();
    overrideStore = new KeyboardWallpaperOverrideStore(appContext);
    wallpaperBitmapLoader = bitmapLoader;
    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    cachedKeyFaceOverlayScaleMode = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
    cachedPhotoMaxDimBucketPx = 0;
    lastRequestedKeyFaceLastModified = 0L;
    lastRequestedKeyFaceBucketPx = 0;
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

  /**
   * Applies the user photo override (if any) as this view's background without blocking the UI
   * thread.
   *
   * <p>The view background is updated only after it has non-zero bounds.
   */
  public void applyPhotoOverrideIfAnyAsync(@NonNull View view, @Nullable KeyboardTheme theme) {
    if (theme == null) return;

    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return;
    }

    final String themeId = theme.getId();
    if (overrideStore.isWallpaperInvalid(themeId)) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return;
    }

    final File file = overrideStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return;
    }

    final long lastModified = file.lastModified();
    final int dimPercent = overrideStore.getDimPercent(themeId);
    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    final int scaleMode = overrideStore.getWallpaperScaleMode(themeId);
    final int anchor = overrideStore.getWallpaperAnchor(themeId);

    final int token = nextBackgroundApplyToken++;
    backgroundApplyTokens.put(view, token);

    final Runnable start =
        () -> {
          final Integer currentToken = backgroundApplyTokens.get(view);
          if (currentToken == null || currentToken != token) return;

          final int width = view.getWidth();
          final int height = view.getHeight();
          if (width <= 0 || height <= 0) return;

          final int maxDimBucket =
              WallpaperBitmapRepository.bucketMaxDimPx(Math.max(width, height));
          final Bitmap cached = wallpaperBitmapLoader.getCached(file, lastModified, maxDimBucket);
          if (cached != null) {
            applyCachedPhotoToView(
                view,
                themeId,
                lastModified,
                maxDimBucket,
                cached,
                dimPercent,
                rotationDegrees,
                scaleMode,
                anchor,
                token);
            return;
          }

          wallpaperBitmapLoader.loadAsync(
              file,
              lastModified,
              maxDimBucket,
              bitmap -> {
                final Integer stillCurrent = backgroundApplyTokens.get(view);
                if (stillCurrent == null || stillCurrent != token) return;

                if (bitmap == null) {
                  // treat as invalid/corrupt and fall back to theme wallpaper
                  //noinspection ResultOfMethodCallIgnored
                  file.delete();
                  overrideStore.markWallpaperInvalid(themeId);
                  if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
                  return;
                }

                applyCachedPhotoToView(
                    view,
                    themeId,
                    lastModified,
                    maxDimBucket,
                    bitmap,
                    dimPercent,
                    rotationDegrees,
                    scaleMode,
                    anchor,
                    token);
              });
        };

    if (view.getWidth() > 0 && view.getHeight() > 0) {
      start.run();
    } else {
      view.addOnLayoutChangeListener(
          new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                View v,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
              v.removeOnLayoutChangeListener(this);
              start.run();
            }
          });
    }
  }

  private void applyCachedPhotoToView(
      @NonNull View view,
      @NonNull String themeId,
      long lastModified,
      int maxDimBucketPx,
      @NonNull Bitmap bitmap,
      int dimPercent,
      int rotationDegrees,
      int scaleMode,
      int anchor,
      int expectedToken) {
    Trace.beginSection("NSK.WallpaperApply");
    try {
      final Integer token = backgroundApplyTokens.get(view);
      if (token == null || token != expectedToken) return;

      final WallpaperBitmapDrawable baseDrawable =
          cacheLoadedPhoto(themeId, lastModified, maxDimBucketPx, bitmap);
      baseDrawable.setTransform(rotationDegrees, scaleMode, anchor);
      view.setBackground(applyDimOverlayCached(baseDrawable, dimPercent));
    } finally {
      Trace.endSection();
    }
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
    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    final int scaleMode = overrideStore.getWallpaperScaleMode(themeId);
    final int anchor = overrideStore.getWallpaperAnchor(themeId);
    final int maxDim =
        Math.max(
            appContext.getResources().getDisplayMetrics().widthPixels,
            appContext.getResources().getDisplayMetrics().heightPixels);
    final WallpaperBitmapDrawable baseDrawable =
        ensurePhotoLoaded(themeId, file, lastModified, maxDim);
    if (baseDrawable == null) return null;

    baseDrawable.setTransform(rotationDegrees, scaleMode, anchor);
    return applyDimOverlayCached(baseDrawable, dimPercent);
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

  private static int alphaForPercent(int percent) {
    final int clamped = clampPercent(percent);
    // Integer rounding: (255 * p / 100) with proper half-up rounding.
    return (255 * clamped + 50) / 100;
  }

  private void clearPhotoCache() {
    cachedPhotoThemeId = null;
    cachedPhotoLastModified = 0L;
    cachedPhotoMaxDimBucketPx = 0;
    cachedPhotoDimPercent = 0;
    cachedPhotoBitmap = null;
    cachedPhotoBaseDrawable = null;
    cachedPhotoDimOverlayDrawable = null;
    cachedPhotoDimmedDrawable = null;

    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    cachedKeyFaceOverlayScaleMode = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
    cachedKeyFaceOverlayPaint = null;
    cachedKeyFaceOverlayShader = null;
    cachedKeyFaceOverlayBounds.setEmpty();
    keyFaceOverlay.reset();

    lastRequestedKeyFaceThemeId = null;
    lastRequestedKeyFaceLastModified = 0L;
    lastRequestedKeyFaceBucketPx = 0;
  }

  @Nullable
  private WallpaperBitmapDrawable ensurePhotoLoaded(
      @NonNull String themeId, @NonNull File file, long lastModified, int maxDimPx) {
    final int bucket = WallpaperBitmapRepository.bucketMaxDimPx(maxDimPx);
    if (cachedPhotoBaseDrawable != null
        && themeId.equals(cachedPhotoThemeId)
        && cachedPhotoLastModified == lastModified
        && cachedPhotoMaxDimBucketPx == bucket) {
      return cachedPhotoBaseDrawable;
    }

    final Bitmap bitmap = wallpaperBitmapLoader.getCached(file, lastModified, bucket);
    if (bitmap == null) return null;

    return cacheLoadedPhoto(themeId, lastModified, bucket, bitmap);
  }

  @NonNull
  private WallpaperBitmapDrawable cacheLoadedPhoto(
      @NonNull String themeId, long lastModified, int maxDimBucketPx, @NonNull Bitmap bitmap) {
    if (cachedPhotoBaseDrawable != null
        && themeId.equals(cachedPhotoThemeId)
        && cachedPhotoLastModified == lastModified
        && cachedPhotoMaxDimBucketPx == maxDimBucketPx
        && cachedPhotoBitmap == bitmap) {
      return cachedPhotoBaseDrawable;
    }

    cachedPhotoThemeId = themeId;
    cachedPhotoLastModified = lastModified;
    cachedPhotoMaxDimBucketPx = maxDimBucketPx;
    cachedPhotoBitmap = bitmap;
    cachedPhotoBaseDrawable = new WallpaperBitmapDrawable(bitmap);
    cachedPhotoDimOverlayDrawable = null;
    cachedPhotoDimmedDrawable = null;

    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    cachedKeyFaceOverlayScaleMode = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
    cachedKeyFaceOverlayPaint = null;
    cachedKeyFaceOverlayShader = null;
    cachedKeyFaceOverlayBounds.setEmpty();
    keyFaceOverlay.reset();

    return cachedPhotoBaseDrawable;
  }

  @NonNull
  private Drawable applyDimOverlayCached(
      @NonNull WallpaperBitmapDrawable baseDrawable, int dimPercent) {
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
      cachedPhotoDimOverlayDrawable.setAlpha(alphaForPercent(clamped));
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
      @Nullable KeyboardTheme theme,
      @NonNull Rect keyboardViewBounds,
      @NonNull Runnable requestInvalidate) {
    return resolveKeyFaceOverlay(theme, keyboardViewBounds, true, requestInvalidate);
  }

  @NonNull
  public KeyFaceOverlay resolveKeyFaceOverlay(
      @Nullable KeyboardTheme theme,
      @NonNull Rect keyboardViewBounds,
      boolean allowMatchKeyShape,
      @NonNull Runnable requestInvalidate) {
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

    final int maxDimPx = Math.max(keyboardViewBounds.width(), keyboardViewBounds.height());
    final WallpaperBitmapDrawable baseDrawable =
        ensurePhotoLoaded(themeId, file, file.lastModified(), maxDimPx);
    if (baseDrawable == null || cachedPhotoBitmap == null) {
      requestKeyFaceBitmapLoadIfNeeded(
          themeId, file, file.lastModified(), maxDimPx, requestInvalidate);
      return keyFaceOverlay;
    }

    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    final int scaleMode = overrideStore.getWallpaperScaleMode(themeId);
    final int anchor = overrideStore.getWallpaperAnchor(themeId);

    if (cachedKeyFaceOverlayPaint == null
        || cachedKeyFaceOverlayShader == null
        || cachedKeyFaceOverlayScaleMode != scaleMode) {
      final Shader.TileMode tileMode = KeyboardWallpaperTransform.tileModeForScaleMode(scaleMode);
      cachedKeyFaceOverlayShader =
          new android.graphics.BitmapShader(cachedPhotoBitmap, tileMode, tileMode);
      cachedKeyFaceOverlayPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
      cachedKeyFaceOverlayPaint.setShader(cachedKeyFaceOverlayShader);
      cachedKeyFaceOverlayBounds.setEmpty();
      cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
      cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
      cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
      cachedKeyFaceOverlayScaleMode = scaleMode;
      cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
    }

    // Ensure the shader mapping is anchored to the full keyboard view bounds, not per-key bounds.
    if (!cachedKeyFaceOverlayBounds.equals(keyboardViewBounds)
        || cachedKeyFaceOverlayRotationDegrees != rotationDegrees
        || cachedKeyFaceOverlayScaleMode != scaleMode
        || cachedKeyFaceOverlayAnchor != anchor) {
      cachedKeyFaceOverlayBounds.set(keyboardViewBounds);
      KeyboardWallpaperTransform.updateShaderMatrix(
          cachedKeyFaceOverlayMatrix,
          cachedPhotoBitmap.getWidth(),
          cachedPhotoBitmap.getHeight(),
          cachedKeyFaceOverlayBounds,
          rotationDegrees,
          scaleMode,
          anchor);
      if (cachedKeyFaceOverlayShader instanceof android.graphics.BitmapShader bitmapShader) {
        bitmapShader.setLocalMatrix(cachedKeyFaceOverlayMatrix);
      }
      cachedKeyFaceOverlayRotationDegrees = rotationDegrees;
      cachedKeyFaceOverlayScaleMode = scaleMode;
      cachedKeyFaceOverlayAnchor = anchor;
    }

    if (cachedKeyFaceOverlayAlpha != alphaPercent) {
      cachedKeyFaceOverlayPaint.setAlpha(alphaForPercent(alphaPercent));
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
        allowMatchKeyShape
            && mode == KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE
            && overrideStore.isMatchKeyShapeEnabled(themeId);
    return keyFaceOverlay;
  }

  private void requestKeyFaceBitmapLoadIfNeeded(
      @NonNull String themeId,
      @NonNull File file,
      long lastModified,
      int maxDimPx,
      @NonNull Runnable requestInvalidate) {
    final int bucket = WallpaperBitmapRepository.bucketMaxDimPx(maxDimPx);
    if (bucket <= 0) return;
    if (themeId.equals(lastRequestedKeyFaceThemeId)
        && lastRequestedKeyFaceLastModified == lastModified
        && lastRequestedKeyFaceBucketPx == bucket) {
      return;
    }
    lastRequestedKeyFaceThemeId = themeId;
    lastRequestedKeyFaceLastModified = lastModified;
    lastRequestedKeyFaceBucketPx = bucket;

    wallpaperBitmapLoader.loadAsync(
        file,
        lastModified,
        bucket,
        bitmap -> {
          if (bitmap == null) {
            // treat as invalid/corrupt and fall back to theme wallpaper
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            overrideStore.markWallpaperInvalid(themeId);
            if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
            return;
          }
          requestInvalidate.run();
        });
  }

  @Nullable
  private static ColorFilter createDimColorFilter(int dimPercent) {
    final int clamped = clampPercent(dimPercent);
    if (clamped <= 0) return null;

    final float factor = (100f - clamped) / 100f;
    final ColorMatrix matrix =
        new ColorMatrix(
            new float[] {
              factor, 0f, 0f, 0f, 0f, 0f, factor, 0f, 0f, 0f, 0f, 0f, factor, 0f, 0f, 0f, 0f, 0f,
              1f, 0f
            });
    return new ColorMatrixColorFilter(matrix);
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

  private static final class WallpaperBitmapDrawable extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint;
    private android.graphics.BitmapShader bitmapShader;
    private final Matrix shaderMatrix = new Matrix();
    private int alpha = 0xFF;
    private int rotationDegrees = 0;
    private int scaleMode = KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_CROP;
    private int anchor = KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_CENTER;

    WallpaperBitmapDrawable(@NonNull Bitmap bitmap) {
      this.bitmap = bitmap;
      this.paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
      this.bitmapShader = createShader(bitmap, scaleMode);
      this.paint.setShader(this.bitmapShader);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      updateShaderMatrix(bounds);
    }

    void setTransform(int rotationDegrees, int scaleMode, int anchor) {
      final int normalizedRotation =
          KeyboardWallpaperOverrideStore.normalizeRotationDegrees(rotationDegrees);
      final int normalizedScaleMode = normalizeScaleMode(scaleMode);
      final int normalizedAnchor = normalizeAnchor(anchor);

      boolean changed = false;
      if (this.rotationDegrees != normalizedRotation) {
        this.rotationDegrees = normalizedRotation;
        changed = true;
      }
      if (this.scaleMode != normalizedScaleMode) {
        this.scaleMode = normalizedScaleMode;
        this.bitmapShader = createShader(bitmap, this.scaleMode);
        this.paint.setShader(this.bitmapShader);
        changed = true;
      }
      if (this.anchor != normalizedAnchor) {
        this.anchor = normalizedAnchor;
        changed = true;
      }
      if (changed) updateShaderMatrix(getBounds());
    }

    private void updateShaderMatrix(@NonNull Rect bounds) {
      KeyboardWallpaperTransform.updateShaderMatrix(
          shaderMatrix,
          bitmap.getWidth(),
          bitmap.getHeight(),
          bounds,
          rotationDegrees,
          scaleMode,
          anchor);
      bitmapShader.setLocalMatrix(shaderMatrix);
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

    private static android.graphics.BitmapShader createShader(
        @NonNull Bitmap bitmap, int scaleMode) {
      final Shader.TileMode tileMode = KeyboardWallpaperTransform.tileModeForScaleMode(scaleMode);
      return new android.graphics.BitmapShader(bitmap, tileMode, tileMode);
    }

    private static int normalizeScaleMode(int scaleMode) {
      switch (scaleMode) {
        case KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_CROP:
        case KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_FIT:
        case KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_STRETCH:
        case KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_TILE:
        case KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_MIRROR:
          return scaleMode;
        default:
          return KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_CROP;
      }
    }

    private static int normalizeAnchor(int anchor) {
      return anchor >= KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_TOP_LEFT
              && anchor <= KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_BOTTOM_RIGHT
          ? anchor
          : KeyboardWallpaperOverrideStore.WALLPAPER_ANCHOR_CENTER;
    }
  }
}
