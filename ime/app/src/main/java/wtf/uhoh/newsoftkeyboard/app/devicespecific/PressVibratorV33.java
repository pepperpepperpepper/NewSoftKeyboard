package wtf.uhoh.newsoftkeyboard.app.devicespecific;

import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.RequiresApi;
import java.lang.reflect.Field;

@RequiresApi(33)
public class PressVibratorV33 extends PressVibratorV29 {
  // NOTE: Android 14+/15 split "keyboard vibration" from generic "touch feedback" and introduced
  // a dedicated vibration usage for IME haptics: VibrationAttributes.USAGE_IME_FEEDBACK.
  // That constant is currently hidden in the public SDK, but it exists on devices where the
  // system exposes the "Keyboard vibration" toggle (Settings.System.KEYBOARD_VIBRATION_ENABLED).
  //
  // In AOSP, it is defined as:
  //   public static final int USAGE_IME_FEEDBACK = 0x50 | USAGE_CLASS_FEEDBACK;
  // which evaluates to 0x52 (82).
  //
  // We prefer it when available so key-press vibration matches the system's keyboard-haptics
  // channel (and isn't accidentally gated by the generic touch-feedback intensity).
  private static final int USAGE_IME_FEEDBACK = resolveImeFeedbackUsage();
  private static final VibrationAttributes HAPTIC_IME_VIBRATION_ATTRIBUTES =
      createImeHapticVibrationAttributes();
  private static final VibrationAttributes HAPTIC_TOUCH_VIBRATION_ATTRIBUTES =
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH);

  private volatile boolean mSystemKeyboardVibrationEnabled = true;

  private static int resolveImeFeedbackUsage() {
    try {
      Field field = VibrationAttributes.class.getDeclaredField("USAGE_IME_FEEDBACK");
      field.setAccessible(true);
      Object value = field.get(null);
      if (value instanceof Integer) return (int) value;
    } catch (Throwable ignored) {
      // best-effort; fall back to the AOSP value
    }
    return 0x52;
  }

  private static VibrationAttributes createImeHapticVibrationAttributes() {
    try {
      return VibrationAttributes.createForUsage(USAGE_IME_FEEDBACK);
    } catch (Throwable t) {
      // Older builds (or OEM forks) may not recognize the usage constant. Fall back to the most
      // reasonable available usage.
      return VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH);
    }
  }

  public PressVibratorV33(Vibrator vibe) {
    super(vibe);
  }

  @Override
  public void setSystemKeyboardVibrationEnabled(boolean enabled) {
    mSystemKeyboardVibrationEnabled = enabled;
  }

  private void vibrateWithSystemAttributes(VibrationEffect ve) {
    // Prefer the touch-haptics usage when the system-wide touch feedback is enabled.
    //
    // When system-wide touch haptics are disabled (e.g., "Touch feedback" intensity is set to
    // none/0) but keyboard haptics are enabled, prefer the dedicated IME usage so key-press
    // vibration can still work on Android 14+/15+ builds that split those channels.
    //
    // If the platform rejects the usage, fall back to touch usage and then to legacy vibration.
    try {
      if (mSystemHapticEnabled) {
        mVibe.vibrate(ve, HAPTIC_TOUCH_VIBRATION_ATTRIBUTES);
        return;
      }

      if (mSystemKeyboardVibrationEnabled
          && HAPTIC_IME_VIBRATION_ATTRIBUTES.getUsage() != VibrationAttributes.USAGE_TOUCH) {
        mVibe.vibrate(ve, HAPTIC_IME_VIBRATION_ATTRIBUTES);
        return;
      }

      // System touch haptics are disabled and we either can't use the IME channel (usage not
      // recognized) or the system reports keyboard vibration disabled. In that case, fall back to
      // legacy vibration APIs to maximize the chance of producing physical haptics.
      vibrateWithLegacyAttributes(ve);
    } catch (Throwable t) {
      try {
        mVibe.vibrate(ve, HAPTIC_TOUCH_VIBRATION_ATTRIBUTES);
      } catch (Throwable ignored) {
        vibrateWithLegacyAttributes(ve);
      }
    }
  }

  @Override
  public void vibrate(boolean longPress) {
    final VibrationEffect ve = longPress ? mLongPressVibration : mVibration;
    if (mVibe == null || ve == null || checkSuppressed()) return;

    // In non-system mode, prefer the legacy vibration path so the app-controlled duration slider
    // remains effective even when the platform gates/normalizes keyboard vibration usages.
    if (!mSystemVibe) {
      vibrateWithLegacyAttributes(ve);
    } else {
      vibrateWithSystemAttributes(ve);
    }
  }

  @Override
  public void vibrateFallback(boolean longPress) {
    VibrationEffect ve = longPress ? mDurationLongPressVibration : mDurationVibration;
    if (ve == null) ve = longPress ? mLongPressVibration : mVibration;
    if (mVibe == null || ve == null || checkSuppressed()) return;

    // This is explicitly a fallback/compatibility path. Prefer legacy vibration APIs here to
    // maximize the chance of producing physical haptics on OEM builds.
    vibrateWithLegacyAttributes(ve);
  }

  @Override
  public void vibrateSystemVibrationFallback() {
    final VibrationEffect ve = mSystemVibrationFallbackVibration;
    if (mVibe == null || ve == null || checkSuppressed()) return;

    // The system-mode fallback duration exists specifically to work around OEM/system paths that
    // report "handled" but produce no physical haptics. Use the most broadly compatible vibration
    // API here.
    vibrateWithLegacyAttributes(ve);
  }
}
