package wtf.uhoh.newsoftkeyboard.app.devicespecific;

import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(33)
public class DeviceSpecificV33 extends DeviceSpecificV29 {
  @Override
  public String getApiLevel() {
    return "DeviceSpecificV33";
  }

  @Override
  public PressVibrator createPressVibrator(@NonNull Vibrator vibe) {
    return new PressVibratorV33(vibe);
  }
}
