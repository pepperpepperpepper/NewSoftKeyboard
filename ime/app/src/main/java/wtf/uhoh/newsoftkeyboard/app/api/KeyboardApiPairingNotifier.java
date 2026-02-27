package wtf.uhoh.newsoftkeyboard.app.api;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;

public final class KeyboardApiPairingNotifier {

  private static final String CHANNEL_ID = "keyboard_api_pairing";
  private static final int NOTIFICATION_ID = 0x4B415049; // "KAPI"

  private KeyboardApiPairingNotifier() {}

  public static void maybeNotifyNewRequest(
      @NonNull Context context,
      @NonNull KeyboardApiPairingStore pairingStore,
      @NonNull String packageName) {
    if (pairingStore.isNotified(packageName)) return;
    if (!updateOrCancel(context, pairingStore)) return;
    pairingStore.markNotified(packageName, System.currentTimeMillis());
  }

  public static void refresh(
      @NonNull Context context, @NonNull KeyboardApiPairingStore pairingStore) {
    updateOrCancel(context, pairingStore);
  }

  private static boolean updateOrCancel(
      @NonNull Context context, @NonNull KeyboardApiPairingStore pairingStore) {
    final Set<String> pending = pairingStore.getPendingPackages();
    if (pending.isEmpty()) {
      cancel(context);
      return true;
    }
    return notify(context, pending);
  }

  private static void cancel(@NonNull Context context) {
    final Object svc = context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (!(svc instanceof NotificationManager)) return;
    ((NotificationManager) svc).cancel(NOTIFICATION_ID);
  }

  private static boolean notify(@NonNull Context context, @NonNull Set<String> pendingPackages) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;

    final Object svc = context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (!(svc instanceof NotificationManager)) return false;
    final NotificationManager notificationManager = (NotificationManager) svc;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      final String channelName =
          context.getString(R.string.keyboard_api_pairing_notification_channel_name);
      final NotificationChannel channel =
          new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
      channel.enableVibration(false);
      channel.enableLights(false);
      notificationManager.createNotificationChannel(channel);
    }

    final int pendingCount = pendingPackages.size();
    final String title = context.getString(R.string.keyboard_api_pairing_notification_title);
    final String text;
    if (pendingCount == 1) {
      final String pkg = pendingPackages.iterator().next();
      text =
          context.getString(
              R.string.keyboard_api_pairing_notification_text_one, resolveAppLabel(context, pkg));
    } else {
      text = context.getString(R.string.keyboard_api_pairing_notification_text_many, pendingCount);
    }

    final Intent openSettingsIntent = createOpenProgrammableApiSettingsIntent(context);
    final PendingIntent contentIntent = createActivityPendingIntent(context, openSettingsIntent);

    final Notification.Builder builder =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(context, CHANNEL_ID)
            : new Notification.Builder(context);
    builder
        .setSmallIcon(R.drawable.ic_notification_perm_required)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(new Notification.BigTextStyle().bigText(text))
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setNumber(pendingCount)
        .setContentIntent(contentIntent);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.setCategory(Notification.CATEGORY_STATUS);
    }

    try {
      notificationManager.notify(NOTIFICATION_ID, builder.build());
      return true;
    } catch (SecurityException e) {
      // Android 13+ may require runtime notification permission.
      return false;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @NonNull
  private static Intent createOpenProgrammableApiSettingsIntent(@NonNull Context context) {
    final String deeplinkUri =
        KeyboardApiSettingsDeepLinks.toDeeplinkUri(
            context, KeyboardApiSettingsDeepLinks.DESTINATION_PROGRAMMABLE_API_SETTINGS);
    final Intent intent;
    if (deeplinkUri == null) {
      intent = new Intent(context, MainSettingsActivity.class);
    } else {
      intent =
          new Intent(
              Intent.ACTION_VIEW, Uri.parse(deeplinkUri), context, MainSettingsActivity.class);
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }

  @NonNull
  private static PendingIntent createActivityPendingIntent(
      @NonNull Context context, @NonNull Intent intent) {
    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
    }
    return PendingIntent.getActivity(context, 0, intent, flags);
  }

  @NonNull
  private static String resolveAppLabel(@NonNull Context context, @NonNull String packageName) {
    final PackageManager pm = context.getPackageManager();
    try {
      final ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
      final CharSequence label = pm.getApplicationLabel(info);
      if (label != null) {
        final String s = label.toString().trim();
        if (!s.isEmpty()) return s;
      }
    } catch (PackageManager.NameNotFoundException ignored) {
      // ignored
    } catch (RuntimeException ignored) {
      // ignored
    }
    return packageName;
  }
}
