package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.util.LruCache;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import wtf.uhoh.newsoftkeyboard.BuildConfig;

/**
 * A process-wide, size-aware cache for decoded wallpaper bitmaps.
 *
 * <p>Decoding is always performed off the main thread. Callers may synchronously query the cache,
 * and if needed request an async load that is de-duped across concurrent callers.
 */
final class WallpaperBitmapRepository implements WallpaperBitmapLoader {

  interface Callback {
    void onBitmapReady(@Nullable Bitmap bitmap);
  }

  private static final String TAG = "WallpaperBitmapRepo";

  private static final int MAX_DIM_BUCKET_PX = 4096;
  private static final int BUCKET_SIZE_PX = 256;

  private static final WallpaperBitmapRepository INSTANCE = new WallpaperBitmapRepository();

  static WallpaperBitmapRepository getInstance() {
    return INSTANCE;
  }

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final ExecutorService decodeExecutor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            final Thread t = new Thread(runnable, "KeyboardWallpaperDecode");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
          });

  private final Object cacheLock = new Object();
  private final LruCache<Key, Bitmap> cache;

  // Guards both inFlight and its callback lists.
  private final Object inFlightLock = new Object();
  private final Map<Key, List<Callback>> inFlight = new HashMap<>();

  private WallpaperBitmapRepository() {
    cache =
        new LruCache<>(computeCacheSizeKb()) {
          @Override
          protected int sizeOf(@NonNull Key key, @NonNull Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
          }
        };
  }

  @Nullable
  @Override
  public Bitmap getCached(@NonNull File file, long lastModified, int requestedMaxDimPx) {
    final int bucket = bucketMaxDimPx(requestedMaxDimPx);
    if (bucket <= 0) return null;
    final Key key = new Key(file, lastModified, bucket);
    final Bitmap cached;
    synchronized (cacheLock) {
      cached = cache.get(key);
      if (cached != null && cached.isRecycled()) {
        cache.remove(key);
        return null;
      }
    }
    return cached;
  }

  @Override
  public void loadAsync(
      @NonNull File file, long lastModified, int requestedMaxDimPx, @NonNull Callback cb) {
    final int bucket = bucketMaxDimPx(requestedMaxDimPx);
    if (bucket <= 0) {
      mainHandler.post(() -> cb.onBitmapReady(null));
      return;
    }

    final Key key = new Key(file, lastModified, bucket);
    {
      final Bitmap cached;
      synchronized (cacheLock) {
        cached = cache.get(key);
        if (cached != null && cached.isRecycled()) {
          cache.remove(key);
        }
      }
      if (cached != null && !cached.isRecycled()) {
        mainHandler.post(() -> cb.onBitmapReady(cached));
        return;
      }
    }

    boolean shouldStartDecode = false;
    synchronized (inFlightLock) {
      final List<Callback> existing = inFlight.get(key);
      if (existing != null) {
        existing.add(cb);
      } else {
        final ArrayList<Callback> callbacks = new ArrayList<>(2);
        callbacks.add(cb);
        inFlight.put(key, callbacks);
        shouldStartDecode = true;
      }
    }

    if (!shouldStartDecode) return;

    decodeExecutor.execute(
        () -> {
          final Bitmap decoded = decodeDownsampled(file, bucket);
          if (decoded != null && !decoded.isRecycled()) {
            synchronized (cacheLock) {
              cache.put(key, decoded);
            }
          }

          final List<Callback> callbacks;
          synchronized (inFlightLock) {
            callbacks = inFlight.remove(key);
          }
          if (callbacks == null || callbacks.isEmpty()) return;

          mainHandler.post(
              () -> {
                for (Callback callback : callbacks) {
                  callback.onBitmapReady(decoded);
                }
              });
        });
  }

  static int bucketMaxDimPx(int maxDimPx) {
    if (maxDimPx <= 0) return 0;
    final int clamped = Math.min(MAX_DIM_BUCKET_PX, maxDimPx);
    return ((clamped + BUCKET_SIZE_PX - 1) / BUCKET_SIZE_PX) * BUCKET_SIZE_PX;
  }

  private static int computeCacheSizeKb() {
    final int maxMemoryKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
    final int targetKb = maxMemoryKb / 16;
    return Math.max(4 * 1024, Math.min(64 * 1024, targetKb));
  }

  @Nullable
  private static Bitmap decodeDownsampled(@NonNull File file, int maxDimPx) {
    if (BuildConfig.DEBUG && Looper.getMainLooper() == Looper.myLooper()) {
      Log.w(TAG, "Wallpaper decode running on main thread for " + file.getAbsolutePath());
    }

    Trace.beginSection("NSK.WallpaperDecode");
    try {
      final BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

      final int outMax = Math.max(bounds.outWidth, bounds.outHeight);
      final int requestedMax = Math.max(1, maxDimPx);
      // Keep the decoded bitmap close to the target size for quality, but always bounded.
      //
      // Note: BitmapFactory may round down non-power-of-two sample sizes on some Android versions,
      // so we intentionally choose a power-of-two value and then scale down if still needed.
      final int ratioCeil = (outMax + requestedMax - 1) / requestedMax;
      final int inSampleSize = Math.max(1, Integer.highestOneBit(Math.max(1, ratioCeil)));

      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = inSampleSize;
      options.inPreferredConfig = Bitmap.Config.ARGB_8888;
      try {
        final Bitmap decoded = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (decoded == null) return null;

        final int width = decoded.getWidth();
        final int height = decoded.getHeight();
        final int decodedMax = Math.max(width, height);
        if (decodedMax <= requestedMax) return decoded;

        final float scale = requestedMax / (float) decodedMax;
        final int scaledWidth = Math.max(1, Math.round(width * scale));
        final int scaledHeight = Math.max(1, Math.round(height * scale));
        final Bitmap scaled = Bitmap.createScaledBitmap(decoded, scaledWidth, scaledHeight, true);
        if (scaled != decoded) decoded.recycle();
        return scaled;
      } catch (OutOfMemoryError oom) {
        return null;
      }
    } finally {
      Trace.endSection();
    }
  }

  private record Key(@NonNull String absolutePath, long lastModified, int maxDimBucketPx) {
    Key(@NonNull File file, long lastModified, int maxDimBucketPx) {
      this(file.getAbsolutePath(), lastModified, maxDimBucketPx);
    }
  }
}
