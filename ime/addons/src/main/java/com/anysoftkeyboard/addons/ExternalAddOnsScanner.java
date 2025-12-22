package com.anysoftkeyboard.addons;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ExternalAddOnsScanner {

  interface AddOnLoader<E extends AddOn> {
    @NonNull
    List<E> load(@NonNull Context packContext, @NonNull ActivityInfo ai, @NonNull String metaData);
  }

  @NonNull
  static <E extends AddOn> List<E> getExternalAddOns(
      @NonNull Context context,
      @NonNull List<AddOnsFactory.ReceiverSpec> receiverSpecs,
      boolean readExternalPacksToo,
      @NonNull AddOnLoader<E> loader,
      @NonNull String logTag) {
    final PackageManager packageManager = context.getPackageManager();
    final List<E> externalAddOns = new ArrayList<>();
    final Set<String> handledReceivers = new HashSet<>();

    for (AddOnsFactory.ReceiverSpec spec : receiverSpecs) {
      final Intent queryIntent = new Intent(spec.action());
      final List<ResolveInfo> broadcastReceivers =
          packageManager.queryBroadcastReceivers(queryIntent, PackageManager.GET_META_DATA);

      for (final ResolveInfo receiver : broadcastReceivers) {
        if (receiver.activityInfo == null) {
          Logger.e(
              logTag,
              "BroadcastReceiver has null ActivityInfo. Receiver's label is "
                  + receiver.loadLabel(packageManager));
          Logger.e(logTag, "Is the external keyboard a service instead of BroadcastReceiver?");
          // Skip to next receiver
          continue;
        }

        if (!receiver.activityInfo.enabled || !receiver.activityInfo.applicationInfo.enabled) {
          continue;
        }

        if (!readExternalPacksToo
            && !context.getPackageName().equalsIgnoreCase(receiver.activityInfo.packageName)) {
          // Skipping external packages
          continue;
        }

        final String receiverKey =
            receiver.activityInfo.packageName
                + "/"
                + receiver.activityInfo.name
                + "#"
                + spec.metaData();
        if (!handledReceivers.add(receiverKey)) {
          continue;
        }

        try {
          final Context externalPackageContext =
              context.createPackageContext(
                  receiver.activityInfo.packageName, Context.CONTEXT_IGNORE_SECURITY);
          final List<E> packageAddOns =
              loader.load(externalPackageContext, receiver.activityInfo, spec.metaData());

          externalAddOns.addAll(packageAddOns);
        } catch (final PackageManager.NameNotFoundException e) {
          Logger.e(logTag, "Did not find package: " + receiver.activityInfo.packageName);
        }
      }
    }

    return externalAddOns;
  }

  private ExternalAddOnsScanner() {
    // no instances
  }
}
