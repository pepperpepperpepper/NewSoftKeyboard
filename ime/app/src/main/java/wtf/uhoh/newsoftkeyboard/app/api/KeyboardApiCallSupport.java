package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class KeyboardApiCallSupport {

  private static final long MAIN_THREAD_TIMEOUT_MS = 750;

  private KeyboardApiCallSupport() {}

  @NonNull
  static Bundle runOnMainThreadBlocking(@NonNull Callable<Bundle> action) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      try {
        final Bundle result = action.call();
        return result == null ? error(KeyboardApiContract.ERR_INTERNAL, "Internal error") : result;
      } catch (Throwable t) {
        return error(KeyboardApiContract.ERR_INTERNAL, "Internal error");
      }
    }

    final AtomicReference<Bundle> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                resultRef.set(action.call());
              } catch (Throwable t) {
                errorRef.set(t);
              } finally {
                latch.countDown();
              }
            });

    final boolean completed;
    try {
      completed = latch.await(MAIN_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return error(KeyboardApiContract.ERR_INTERNAL, "Interrupted");
    }

    if (!completed) {
      return error(KeyboardApiContract.ERR_INTERNAL, "Timed out waiting for IME");
    }
    if (errorRef.get() != null) {
      return error(KeyboardApiContract.ERR_INTERNAL, "Internal error");
    }

    final Bundle result = resultRef.get();
    return result == null ? error(KeyboardApiContract.ERR_INTERNAL, "Internal error") : result;
  }

  @NonNull
  static Bundle ok() {
    final Bundle out = new Bundle();
    out.putBoolean(KeyboardApiContract.EXTRA_OK, true);
    out.putInt(KeyboardApiContract.EXTRA_API_VERSION, KeyboardApiContract.API_VERSION);
    return out;
  }

  @NonNull
  static Bundle error(int code, @NonNull String message) {
    final Bundle out = new Bundle();
    out.putBoolean(KeyboardApiContract.EXTRA_OK, false);
    out.putInt(KeyboardApiContract.EXTRA_API_VERSION, KeyboardApiContract.API_VERSION);
    out.putInt(KeyboardApiContract.EXTRA_ERROR_CODE, code);
    out.putString(KeyboardApiContract.EXTRA_ERROR_MESSAGE, message);
    return out;
  }
}
