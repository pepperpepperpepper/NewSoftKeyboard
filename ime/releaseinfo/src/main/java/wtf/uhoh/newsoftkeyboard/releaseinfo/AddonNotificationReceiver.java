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

package wtf.uhoh.newsoftkeyboard.releaseinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.notification.NotificationDriver;

/**
 * Broadcast receiver that allows addons to request notifications. Addons can send a broadcast with
 * specific extras to register their notification.
 */
public class AddonNotificationReceiver extends BroadcastReceiver {
  private static final String TAG = "AddonNotification";

  // Action for addon notifications
  public static final String ACTION_SHOW_ADDON_NOTIFICATION =
      "com.anysoftkeyboard.action.SHOW_ADDON_NOTIFICATION";

  // Intent extras
  public static final String EXTRA_PACKAGE_NAME = "package_name";
  public static final String EXTRA_TITLE_RES_ID = "title_res_id";
  public static final String EXTRA_MESSAGE_RES_ID = "message_res_id";
  public static final String EXTRA_SETUP_INTENT = "setup_intent";

  private NotificationDriver mNotificationDriver;

  public void setNotificationDriver(@NonNull NotificationDriver driver) {
    mNotificationDriver = driver;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (ACTION_SHOW_ADDON_NOTIFICATION.equals(intent.getAction()) && mNotificationDriver != null) {
      handleAddonNotification(context, intent);
    }
  }

  private void handleAddonNotification(@NonNull Context context, @NonNull Intent intent) {
    try {
      String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
      int titleResId = intent.getIntExtra(EXTRA_TITLE_RES_ID, 0);
      int messageResId = intent.getIntExtra(EXTRA_MESSAGE_RES_ID, 0);
      Intent setupIntent = intent.getParcelableExtra(EXTRA_SETUP_INTENT);

      if (packageName == null || titleResId == 0 || messageResId == 0 || setupIntent == null) {
        Logger.w(TAG, "Invalid addon notification request - missing required extras");
        return;
      }

      Logger.i(TAG, "Processing addon notification request from: " + packageName);

      boolean shown =
          AddonNotificationSystem.showIfNeeded(packageName, setupIntent, titleResId, messageResId);

      if (shown) {
        Logger.i(TAG, "Addon notification shown for: " + packageName);
      } else {
        Logger.i(TAG, "Addon notification already shown or failed for: " + packageName);
      }

    } catch (Exception e) {
      Logger.e(TAG, e, "Failed to process addon notification request");
    }
  }
}
