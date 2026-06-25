package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import java.util.LinkedHashMap;
import java.util.Map;

final class KeyboardApiRateLimiter {

  // Default: allow bursts, but stop sustained spam.
  private static final long WINDOW_MS = 1_000L;
  private static final int MAX_CALLS_PER_WINDOW = 25;
  private static final int MAX_TRACKED_CALLERS = 100;

  // UI actions are much more disruptive than simple reads. Limit to reduce UI spam even for
  // allow-listed controllers.
  private static final long UI_ACTION_COOLDOWN_MS = 2_000L;

  private static final Object LOCK = new Object();
  private static final LinkedHashMap<String, Window> WINDOWS =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Window> eldest) {
          return size() > MAX_TRACKED_CALLERS;
        }
      };

  private static final Object UI_LOCK = new Object();
  private static final LinkedHashMap<String, Long> LAST_UI_ACTION_MS =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
          return size() > MAX_TRACKED_CALLERS;
        }
      };

  static final class Decision {
    final boolean allowed;
    final long retryAfterMs;

    private Decision(boolean allowed, long retryAfterMs) {
      this.allowed = allowed;
      this.retryAfterMs = retryAfterMs;
    }

    static Decision allowed() {
      return new Decision(true, 0L);
    }

    static Decision limited(long retryAfterMs) {
      return new Decision(false, Math.max(0L, retryAfterMs));
    }
  }

  private static final class Window {
    long windowStartMs;
    int calls;
  }

  @NonNull
  static Decision check(@NonNull String callerId) {
    return check(callerId, 1);
  }

  /**
   * Charges {@code cost} calls against the caller's window at once. Used by {@code runMacro} to bill
   * the whole batch up front, so a macro can't bypass the per-window cap one step at a time.
   */
  @NonNull
  static Decision check(@NonNull String callerId, int cost) {
    final int charge = Math.max(1, cost);
    final long now = SystemClock.elapsedRealtime();
    synchronized (LOCK) {
      Window w = WINDOWS.get(callerId);
      if (w == null) {
        w = new Window();
        WINDOWS.put(callerId, w);
      }
      if (w.windowStartMs == 0L) {
        w.windowStartMs = now;
        w.calls = 0;
      }
      final long elapsed = now - w.windowStartMs;
      if (elapsed >= WINDOW_MS) {
        w.windowStartMs = now;
        w.calls = 0;
      }

      w.calls += charge;
      if (w.calls <= MAX_CALLS_PER_WINDOW) return Decision.allowed();

      return Decision.limited(WINDOW_MS - (now - w.windowStartMs));
    }
  }

  @NonNull
  static Decision checkUiAction(@NonNull String callerId) {
    final long now = SystemClock.elapsedRealtime();
    synchronized (UI_LOCK) {
      final Long last = LAST_UI_ACTION_MS.get(callerId);
      if (last == null) {
        LAST_UI_ACTION_MS.put(callerId, now);
        return Decision.allowed();
      }

      final long elapsed = now - last;
      if (elapsed >= UI_ACTION_COOLDOWN_MS) {
        LAST_UI_ACTION_MS.put(callerId, now);
        return Decision.allowed();
      }

      return Decision.limited(UI_ACTION_COOLDOWN_MS - Math.max(0L, elapsed));
    }
  }

  @VisibleForTesting
  static void resetForTests() {
    synchronized (LOCK) {
      WINDOWS.clear();
    }
    synchronized (UI_LOCK) {
      LAST_UI_ACTION_MS.clear();
    }
  }
}
