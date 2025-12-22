package com.anysoftkeyboard.addons;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import java.util.Collection;
import java.util.List;

final class AddOnPackageChangeDecider {

  static boolean shouldClearAddOnCache(
      @NonNull Intent eventIntent,
      @NonNull Context context,
      @NonNull String logTag,
      @NonNull Collection<? extends AddOn> managedAddOns,
      @NonNull List<AddOnsFactory.ReceiverSpec> receiverSpecs)
      throws PackageManager.NameNotFoundException {
    String action = eventIntent.getAction();
    if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
      Logger.d(logTag, "After device unlock!");
      return true;
    }
    String packageNameSchemePart = eventIntent.getData().getSchemeSpecificPart();
    if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
      // will reset only if the new package has my addons
      boolean hasAddon = isPackageContainAnAddon(context, receiverSpecs, packageNameSchemePart);
      if (hasAddon) {
        Logger.d(
            logTag,
            "It seems that an addon exists in a newly installed package "
                + packageNameSchemePart
                + ". I need to reload stuff.");
        return true;
      }
    } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)
        || Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
      // If I'm managing OR it contains an addon (could be new feature in the package), I want
      // to reset.
      boolean isPackagedManaged = isPackageManaged(managedAddOns, packageNameSchemePart);
      if (isPackagedManaged) {
        Logger.d(
            logTag,
            "It seems that an addon I use (in package "
                + packageNameSchemePart
                + ") has been changed. I need to reload stuff.");
        return true;
      } else {
        boolean hasAddon = isPackageContainAnAddon(context, receiverSpecs, packageNameSchemePart);
        if (hasAddon) {
          Logger.d(
              logTag,
              "It seems that an addon exists in an updated package "
                  + packageNameSchemePart
                  + ". I need to reload stuff.");
          return true;
        }
      }
    } else // removed
    {
      // so only if I manage this package, I want to reset
      boolean isPackagedManaged = isPackageManaged(managedAddOns, packageNameSchemePart);
      if (isPackagedManaged) {
        Logger.d(
            logTag,
            "It seems that an addon I use (in package "
                + packageNameSchemePart
                + ") has been removed. I need to reload stuff.");
        return true;
      }
    }
    return false;
  }

  private static boolean isPackageManaged(
      @NonNull Collection<? extends AddOn> managedAddOns, @NonNull String packageName) {
    for (AddOn addOn : managedAddOns) {
      if (addOn.getPackageName().equals(packageName)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isPackageContainAnAddon(
      @NonNull Context context,
      @NonNull List<AddOnsFactory.ReceiverSpec> receiverSpecs,
      @NonNull String packageName)
      throws PackageManager.NameNotFoundException {
    PackageInfo newPackage =
        context
            .getPackageManager()
            .getPackageInfo(
                packageName, PackageManager.GET_RECEIVERS + PackageManager.GET_META_DATA);
    if (newPackage.receivers != null) {
      ActivityInfo[] receivers = newPackage.receivers;
      for (ActivityInfo aReceiver : receivers) {
        // issue 904
        if (aReceiver == null
            || aReceiver.applicationInfo == null
            || !aReceiver.enabled
            || !aReceiver.applicationInfo.enabled) {
          continue;
        }
        for (AddOnsFactory.ReceiverSpec spec : receiverSpecs) {
          try (final XmlResourceParser xml =
              aReceiver.loadXmlMetaData(context.getPackageManager(), spec.metaData())) {
            if (xml != null) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private AddOnPackageChangeDecider() {
    // no instances
  }
}
