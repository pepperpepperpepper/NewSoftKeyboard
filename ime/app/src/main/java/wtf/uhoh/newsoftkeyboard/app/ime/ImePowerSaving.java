package wtf.uhoh.newsoftkeyboard.app.ime;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.android.PowerSaving;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayDataImpl;
import wtf.uhoh.newsoftkeyboard.overlay.OverlyDataCreator;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;

public abstract class ImePowerSaving extends ImeNightMode {
  private boolean mPowerState;
  private ToggleOverlayCreator mToggleOverlayCreator;

  @Override
  public void onCreate() {
    super.onCreate();

    addDisposable(
        PowerSaving.observePowerSavingState(getApplicationContext(), 0)
            .subscribe(
                powerState -> {
                  mPowerState = powerState;
                  setupInputViewWatermark();
                },
                GenericOnError.onError("Power-Saving icon")));

    addDisposable(
        PowerSaving.observePowerSavingState(
                getApplicationContext(),
                R.string.settings_key_power_save_mode_theme_control,
                R.bool.settings_default_true)
            .subscribe(
                mToggleOverlayCreator::setToggle, GenericOnError.onError("Power-Saving theme")));
  }

  @NonNull
  @Override
  protected List<Drawable> generateWatermark() {
    final List<Drawable> watermark = super.generateWatermark();
    if (mPowerState) {
      watermark.add(ContextCompat.getDrawable(this, R.drawable.ic_watermark_power_saving));
    }
    return watermark;
  }

  @Override
  protected OverlyDataCreator createOverlayDataCreator() {
    return mToggleOverlayCreator =
        new ToggleOverlayCreator(
            super.createOverlayDataCreator(),
            this,
            new OverlayDataImpl(Color.BLACK, Color.BLACK, Color.DKGRAY, Color.GRAY, Color.DKGRAY),
            "PowerSaving");
  }
}
