package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import io.reactivex.disposables.Disposable;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.android.PowerSaving;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class VibrationPowerSavingHint {

  private VibrationPowerSavingHint() {}

  /**
   * Shows a hint on the given vibration slider whenever Power-Saving is currently muting key-press
   * vibrations, so users don't think the slider is broken. Caller owns the returned Disposable.
   */
  @NonNull
  static Disposable bind(@NonNull Context context, @NonNull Preference slider) {
    final Context appContext = context.getApplicationContext();
    return PowerSaving.observePowerSavingState(
            appContext,
            R.string.settings_key_power_save_mode_vibration_control,
            R.bool.settings_default_false)
        .observeOn(RxSchedulers.mainThread())
        .subscribe(
            suppressed ->
                slider.setSummary(
                    suppressed
                        ? appContext.getString(
                            R.string.vibrate_on_key_press_summary_power_save_suppressed)
                        : null));
  }
}
