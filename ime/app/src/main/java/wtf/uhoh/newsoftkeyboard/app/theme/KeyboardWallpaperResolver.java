package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
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
  private int cachedPhotoDimPercent;
  @Nullable private Drawable cachedPhotoDrawable;

  public KeyboardWallpaperResolver(@NonNull Context context) {
    appContext = context.getApplicationContext();
    overrideStore = new KeyboardWallpaperOverrideStore(appContext);
  }

  @NonNull
  public Drawable resolveImeWallpaper(@Nullable KeyboardTheme theme) {
    final Drawable fallback = ContextCompat.getDrawable(appContext, R.drawable.nsk_wallpaper);
    if (theme == null || fallback == null) return fallback;

    if (cachedPhotoThemeId != null && !cachedPhotoThemeId.equals(theme.getId())) {
      clearPhotoCache();
    }

    final Drawable photoOverride = resolvePhotoOverride(theme);
    if (photoOverride != null) return photoOverride;

    final Drawable themeWallpaper = resolveThemeWallpaper(theme);
    return themeWallpaper != null ? themeWallpaper : fallback;
  }

  @Nullable
  private Drawable resolvePhotoOverride(@NonNull KeyboardTheme theme) {
    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return null;
    }

    final String themeId = theme.getId();
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
    if (cachedPhotoDrawable != null
        && themeId.equals(cachedPhotoThemeId)
        && cachedPhotoLastModified == lastModified
        && cachedPhotoDimPercent == dimPercent) {
      return cachedPhotoDrawable;
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

    final Drawable photo = new CenterCropBitmapDrawable(bitmap);
    final Drawable resolved = applyDimOverlay(photo, dimPercent);

    cachedPhotoThemeId = themeId;
    cachedPhotoLastModified = lastModified;
    cachedPhotoDimPercent = dimPercent;
    cachedPhotoDrawable = resolved;

    return resolved;
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

  private static Drawable applyDimOverlay(@NonNull Drawable wallpaper, int dimPercent) {
    final int clamped = clampPercent(dimPercent);
    if (clamped <= 0) return wallpaper;

    final ColorDrawable dim = new ColorDrawable(Color.BLACK);
    dim.setAlpha(Math.round(255f * (clamped / 100f)));
    return new LayerDrawable(new Drawable[] {wallpaper, dim});
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
    cachedPhotoDrawable = null;
  }

  private static final class CenterCropBitmapDrawable extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint;
    private final Shader bitmapShader;
    private final Matrix shaderMatrix = new Matrix();
    private int alpha = 0xFF;

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

    private void updateShaderMatrix(@NonNull Rect bounds) {
      final int bw = bitmap.getWidth();
      final int bh = bitmap.getHeight();
      if (bw <= 0 || bh <= 0) return;

      final float scale;
      float dx = 0f;
      float dy = 0f;

      final float boundsW = bounds.width();
      final float boundsH = bounds.height();

      if (bw * boundsH > boundsW * bh) {
        // bitmap is wider (relative) than bounds, scale by height and crop width
        scale = boundsH / bh;
        dx = (boundsW - bw * scale) * 0.5f;
      } else {
        // bitmap is taller (relative) than bounds, scale by width and crop height
        scale = boundsW / bw;
        dy = (boundsH - bh * scale) * 0.5f;
      }

      shaderMatrix.reset();
      shaderMatrix.setScale(scale, scale);
      shaderMatrix.postTranslate(bounds.left + Math.round(dx), bounds.top + Math.round(dy));
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
