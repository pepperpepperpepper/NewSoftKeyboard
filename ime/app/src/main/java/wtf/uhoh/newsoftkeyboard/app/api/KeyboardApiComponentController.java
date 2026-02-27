package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

public final class KeyboardApiComponentController {

  private KeyboardApiComponentController() {}

  public static void setProviderEnabled(@NonNull Context context, boolean enabled) {
    final ComponentName provider = new ComponentName(context, KeyboardApiProvider.class);
    context
        .getPackageManager()
        .setComponentEnabledSetting(
            provider,
            enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
  }
}
