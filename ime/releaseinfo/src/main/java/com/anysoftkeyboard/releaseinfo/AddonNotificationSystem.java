/*
 * Copyright (c) 2024 AnySoftKeyboard Contributors
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

package com.anysoftkeyboard.releaseinfo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.base.utils.CompatUtils;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.notification.NotificationDriver;
import com.anysoftkeyboard.notification.NotificationIds;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/**
 * Generic notification system for AnySoftKeyboard addons. Follows the same pattern as
 * TesterNotification but allows addons to register their own notifications when they are first
 * loaded.
 */
public class AddonNotificationSystem {
  private static final String TAG = "AddonNotification";
  private static final String KEY_ADDON_NOTIFICATION_PREFIX = "addon_notification_shown_";
  private static NotificationDriver sNotificationDriver;
  private static Context sApplicationContext;

  /** Initialize the notification system. Should be called from AnyApplication. */
  public static void initialize(@NonNull Context context, @NonNull NotificationDriver driver) {
    sApplicationContext = context.getApplicationContext();
    sNotificationDriver = driver;
    Logger.i(TAG, "AddonNotificationSystem initialized");
  }

  /**
   * Shows a notification for an addon if this addon hasn't shown a notification before. This method
   * can be called directly from addons.
   *
   * @param packageName The package name of the addon (used as unique identifier)
   * @param intent The intent to open when notification is clicked
   * @param titleResId Resource ID for the notification title
   * @param messageResId Resource ID for the notification message
   * @return true if notification was shown, false otherwise
   */
  public static boolean showIfNeeded(
      @NonNull String packageName, @NonNull Intent intent, int titleResId, int messageResId) {

    if (sApplicationContext == null || sNotificationDriver == null) {
      Logger.w(TAG, "AddonNotificationSystem not initialized");
      return false;
    }

    final String key = KEY_ADDON_NOTIFICATION_PREFIX + packageName;
    SharedPreferences sp = DirectBootAwareSharedPreferences.create(sApplicationContext);

    if (!sp.getBoolean(key, false)) {
      Logger.i(TAG, "Showing notification for addon: " + packageName);

      PendingIntent contentIntent =
          PendingIntent.getActivity(
              sApplicationContext,
              0,
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
              CompatUtils.appendImmutableFlag(0));

      var builder =
          sNotificationDriver
              .buildNotification(
                  NotificationIds.Tester, // Reusing tester notification ID as it's appropriate
                  R.drawable.ic_notification_debug_version,
                  titleResId)
              .setContentText(sApplicationContext.getString(messageResId))
              .setContentIntent(contentIntent)
              .setColor(
                  ContextCompat.getColor(
                      sApplicationContext, R.color.notification_background_debug_version))
              .setAutoCancel(true);

      if (sNotificationDriver.notify(builder, true)) {
        sp.edit().putBoolean(key, true).apply();
        return true;
      }
    }
    return false;
  }

  /**
   * Shows a notification for an addon if this addon hasn't shown a notification before. Overloaded
   * method that accepts string parameters instead of resource IDs.
   *
   * @param packageName The package name of the addon (used as unique identifier)
   * @param intent The intent to open when notification is clicked
   * @param title The notification title string
   * @param message The notification message string
   * @return true if notification was shown, false otherwise
   */
  public static boolean showIfNeeded(
      @NonNull String packageName,
      @NonNull Intent intent,
      @NonNull String title,
      @NonNull String message) {

    if (sApplicationContext == null || sNotificationDriver == null) {
      Logger.w(TAG, "AddonNotificationSystem not initialized");
      return false;
    }

    final String key = KEY_ADDON_NOTIFICATION_PREFIX + packageName;
    SharedPreferences sp = DirectBootAwareSharedPreferences.create(sApplicationContext);

    if (!sp.getBoolean(key, false)) {
      Logger.i(TAG, "Showing notification for addon: " + packageName);

      PendingIntent contentIntent =
          PendingIntent.getActivity(
              sApplicationContext,
              0,
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
              CompatUtils.appendImmutableFlag(0));

      var builder =
          sNotificationDriver
              .buildNotification(
                  NotificationIds.Tester, // Reusing tester notification ID as it's appropriate
                  R.drawable.ic_notification_debug_version,
                  R.string.ime_name)
              .setContentTitle(title)
              .setContentText(message)
              .setContentIntent(contentIntent)
              .setColor(
                  ContextCompat.getColor(
                      sApplicationContext, R.color.notification_background_debug_version))
              .setAutoCancel(true);

      if (sNotificationDriver.notify(builder, true)) {
        sp.edit().putBoolean(key, true).apply();
        return true;
      }
    }
    return false;
  }

  /** Reset notification flag for an addon (useful for testing/debugging). */
  public static void resetNotificationFlag(@NonNull String packageName) {
    if (sApplicationContext != null) {
      final String key = KEY_ADDON_NOTIFICATION_PREFIX + packageName;
      DirectBootAwareSharedPreferences.create(sApplicationContext).edit().remove(key).apply();
    }
  }
}
