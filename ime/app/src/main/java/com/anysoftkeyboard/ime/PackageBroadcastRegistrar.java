package com.anysoftkeyboard.ime;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.addons.PackagesChangedReceiver;
import com.anysoftkeyboard.addons.UserUnlockedReceiver;
import java.util.function.Consumer;

/** Handles registration and clean unregistration of package/user-unlock broadcasts. */
public final class PackageBroadcastRegistrar {

  private final Context context;
  private final Consumer<Intent> onCriticalPackageChanged;
  private final PackagesChangedReceiver packagesChangedReceiver;
  @Nullable private UserUnlockedReceiver userUnlockedReceiver;

  public PackageBroadcastRegistrar(Context context, Consumer<Intent> onCriticalPackageChanged) {
    this.context = context;
    this.onCriticalPackageChanged = onCriticalPackageChanged;
    this.packagesChangedReceiver = new PackagesChangedReceiver(this::handleCriticalPackageChanged);
  }

  public void register() {
    ContextCompat.registerReceiver(
        context,
        packagesChangedReceiver,
        PackagesChangedReceiver.createIntentFilter(),
        ContextCompat.RECEIVER_EXPORTED);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      userUnlockedReceiver = new UserUnlockedReceiver(this::handleUserUnlocked);
      ContextCompat.registerReceiver(
          context,
          userUnlockedReceiver,
          UserUnlockedReceiver.createIntentFilter(),
          ContextCompat.RECEIVER_EXPORTED);
    }
  }

  public void unregister() {
    safeUnregister(packagesChangedReceiver);
    safeUnregisterUserUnlocked();
  }

  private void handleCriticalPackageChanged(Intent intent) {
    onCriticalPackageChanged.accept(intent);
  }

  private void handleUserUnlocked(Intent intent) {
    safeUnregisterUserUnlocked();
    handleCriticalPackageChanged(intent);
  }

  private void safeUnregisterUserUnlocked() {
    if (userUnlockedReceiver != null) {
      safeUnregister(userUnlockedReceiver);
      userUnlockedReceiver = null;
    }
  }

  private void safeUnregister(android.content.BroadcastReceiver receiver) {
    try {
      context.unregisterReceiver(receiver);
    } catch (IllegalArgumentException ignored) {
      // already unregistered; ignore
    }
  }
}
