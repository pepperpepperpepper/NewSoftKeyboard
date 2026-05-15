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

import android.os.Vibrator;
import androidx.annotation.VisibleForTesting;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class PressVibrator {
  // Active instance, registered in the constructor so static suppress-callers can find it.
  private static volatile PressVibrator sActive;

  private final AtomicBoolean mSkip = new AtomicBoolean(false);
  protected Vibrator mVibe;

  public PressVibrator(Vibrator vibe) {
    this.mVibe = vibe;
    sActive = this;
  }

  public abstract void setDuration(int duration);

  public abstract void setLongPressDuration(int duration);

  public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {
    // empty; not supported if not overridden
  }

  public void setSystemKeyboardVibrationEnabled(boolean enabled) {
    // empty; not supported if not overridden
  }

  public void setSystemVibrationFallbackDuration(int duration) {
    // empty; not supported if not overridden
  }

  public abstract void vibrate(boolean longPress);

  public void vibrateFallback(boolean longPress) {
    vibrate(longPress);
  }

  public void vibrateSystemVibrationFallback() {
    // empty; not supported if not overridden
  }

  public boolean hasVibrator() {
    return mVibe != null && mVibe.hasVibrator();
  }

  public static void suppressNextVibration() {
    PressVibrator active = sActive;
    if (active != null) active.mSkip.set(true);
  }

  protected boolean checkSuppressed() {
    return mSkip.getAndSet(false);
  }

  @VisibleForTesting
  public Vibrator getVibrator() {
    return mVibe;
  }
}
