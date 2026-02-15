/*
 * Copyright (c) 2021 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.devicespecific;

import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.RequiresApi;

@RequiresApi(29)
public class PressVibratorV29 extends PressVibratorV26 {
  protected boolean mSystemVibe;
  protected boolean mSystemHapticEnabled;
  protected VibrationEffect mDurationVibration;
  protected VibrationEffect mDurationLongPressVibration;
  private static final int[] PRESS_PREDEFINED_CANDIDATES =
      // Prefer CLICK over TICK: some devices make EFFECT_TICK extremely subtle (or effectively
      // silent) even when reported as supported.
      new int[] {VibrationEffect.EFFECT_CLICK, VibrationEffect.EFFECT_TICK};
  private static final int[] LONG_PRESS_PREDEFINED_CANDIDATES =
      new int[] {VibrationEffect.EFFECT_HEAVY_CLICK, VibrationEffect.EFFECT_CLICK};
  private static final int SYSTEM_PRESS_FALLBACK_DURATION_MS = 10;
  private static final int SYSTEM_LONG_PRESS_FALLBACK_DURATION_MS = 30;

  public PressVibratorV29(Vibrator vibe) {
    super(vibe);
  }

  @Override
  public void setDuration(int duration) {
    this.mDuration = duration;
    mDurationVibration =
        this.mDuration > 0 ? VibrationEffect.createOneShot(this.mDuration, AMPLITUDE) : null;
    if (mSystemVibe) {
      mVibration =
          createSystemVibration(
              PRESS_PREDEFINED_CANDIDATES, mDurationVibration, SYSTEM_PRESS_FALLBACK_DURATION_MS);
    } else {
      mVibration = mDurationVibration;
    }
  }

  @Override
  public void setLongPressDuration(int duration) {
    mLongPressDuration = duration;
    mDurationLongPressVibration =
        mLongPressDuration > 0
            ? VibrationEffect.createOneShot(mLongPressDuration, AMPLITUDE)
            : null;
    if (mSystemVibe) {
      if (mLongPressDuration <= 0) {
        mLongPressVibration = null;
      } else {
        mLongPressVibration =
            createSystemVibration(
                LONG_PRESS_PREDEFINED_CANDIDATES,
                mDurationLongPressVibration,
                SYSTEM_LONG_PRESS_FALLBACK_DURATION_MS);
      }
    } else {
      mLongPressVibration = mDurationLongPressVibration;
    }
  }

  @Override
  public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {
    mSystemVibe = system;
    mSystemHapticEnabled = systemWideHapticEnabled;
    setDuration(mDuration);
    setLongPressDuration(mLongPressDuration);
  }

  private VibrationEffect createSystemVibration(
      int[] candidates, VibrationEffect durationFallback, int fallbackDurationMs) {
    if (mVibe == null) {
      if (durationFallback != null) return durationFallback;
      return VibrationEffect.createOneShot(fallbackDurationMs, AMPLITUDE);
    }

    try {
      final int[] supported = mVibe.areEffectsSupported(candidates);
      for (int index = 0; index < supported.length && index < candidates.length; index++) {
        final int supportLevel = supported[index];
        // Some devices report VIBRATION_EFFECT_SUPPORT_UNKNOWN for predefined effects, and then
        // silently do not vibrate when that effect is used. Prefer only effects that are
        // explicitly supported and otherwise fall back to a duration-based vibration.
        if (supportLevel == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
          return VibrationEffect.createPredefined(candidates[index]);
        }
      }
    } catch (Throwable ignored) {
      // best-effort; fall back to a duration-based click
    }

    if (durationFallback != null) return durationFallback;
    return VibrationEffect.createOneShot(fallbackDurationMs, AMPLITUDE);
  }

  @Override
  public void vibrate(boolean longPress) {
    final VibrationEffect ve = longPress ? mLongPressVibration : mVibration;
    if (mVibe == null || ve == null || checkSuppressed()) return;

    if (mSystemVibe) {
      // When emulating system vibration on pre-API 33, keep using haptic audio attributes so
      // predefined effects behave similarly to framework keyboard haptics.
      try {
        mVibe.vibrate(ve, HAPTIC_AUDIO_ATTRIBUTES);
      } catch (Throwable ignored) {
        vibrateWithLegacyAttributes(ve);
      }
    } else {
      // For app-controlled/custom durations, prefer the legacy vibration path to avoid OEM
      // suppression tied to usage/attributes.
      vibrateWithLegacyAttributes(ve);
    }
  }

  @Override
  public void vibrateFallback(boolean longPress) {
    VibrationEffect ve = longPress ? mDurationLongPressVibration : mDurationVibration;
    if (ve == null) ve = longPress ? mLongPressVibration : mVibration;
    if (mVibe != null && ve != null && !checkSuppressed()) {
      vibrateWithLegacyAttributes(ve);
    }
  }

  @Override
  public void vibrateSystemVibrationFallback() {
    final VibrationEffect ve = mSystemVibrationFallbackVibration;
    if (mVibe != null && ve != null && !checkSuppressed()) {
      vibrateWithLegacyAttributes(ve);
    }
  }
}
