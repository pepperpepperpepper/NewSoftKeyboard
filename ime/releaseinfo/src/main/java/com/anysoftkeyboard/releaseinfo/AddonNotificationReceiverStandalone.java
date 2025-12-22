package com.anysoftkeyboard.releaseinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.anysoftkeyboard.notification.NotificationDriver;
import com.anysoftkeyboard.notification.NotificationDriverImpl;

public class AddonNotificationReceiverStandalone extends BroadcastReceiver {
  private static final String TAG = "AddonNotificationReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(
        TAG,
        "AddonNotificationReceiverStandalone.onReceive called with action: " + intent.getAction());

    if ("com.anysoftkeyboard.action.REGISTER_ADDON_NOTIFICATION".equals(intent.getAction())) {
      try {
        String packageName = intent.getStringExtra("package_name");
        Intent setupIntent = intent.getParcelableExtra("setup_intent");

        Log.d(TAG, "Received addon notification request from: " + packageName);

        if (packageName != null && setupIntent != null) {
          // Initialize notification system
          NotificationDriver notificationDriver =
              new NotificationDriverImpl(context.getApplicationContext());
          notificationDriver.initializeChannels(true);

          // Get title and message from intent extras
          String title = intent.getStringExtra("title");
          String message = intent.getStringExtra("message");

          // Use provided strings or fallback to generic ones
          if (title == null) title = "Addon Installed";
          if (message == null) message = "Tap to configure addon settings";

          // Create a simple notification using the system
          boolean shown =
              AddonNotificationSystem.showIfNeeded(packageName, setupIntent, title, message);

          Log.d(TAG, "Addon notification shown for " + packageName + ": " + shown);
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to register addon notification", e);
      }
    } else if ("com.anysoftkeyboard.action.RESET_ADDON_NOTIFICATION".equals(intent.getAction())) {
      try {
        String packageName = intent.getStringExtra("package_name");
        Log.d(TAG, "Resetting notification flag for: " + packageName);

        if (packageName != null) {
          AddonNotificationSystem.resetNotificationFlag(packageName);
          Log.d(TAG, "Notification flag reset for " + packageName);
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to reset addon notification", e);
      }
    }
  }
}
