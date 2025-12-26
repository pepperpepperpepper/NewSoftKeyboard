package wtf.uhoh.newsoftkeyboard.addons;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

final class AddOnsFactoryEvents {

  private static final String TAG = "AddOnsFactory";

  private AddOnsFactoryEvents() {}

  static boolean onExternalPackChanged(Intent eventIntent, AddOnsFactory<?>... factories) {
    boolean cleared = false;
    for (AddOnsFactory<?> factory : factories) {
      try {
        if (AddOnPackageChangeDecider.shouldClearAddOnCache(
            eventIntent,
            factory.mContext,
            factory.mTag,
            factory.mAddOnsById.values(),
            factory.mReceiverSpecs)) {
          cleared = true;
          Logger.d(TAG, factory.getClass().getName() + " will handle this package-changed event.");
          factory.clearAddOnList();
        }
      } catch (PackageManager.NameNotFoundException e) {
        Logger.w(TAG, e, "Failed to notify onExternalPackChanged on %s", factory);
      }
    }
    return cleared;
  }

  static void onConfigurationChanged(
      @NonNull Configuration newConfig, AddOnsFactory<?>... factories) {
    for (AddOnsFactory<?> factory : factories) {
      for (AddOn addOn : factory.mAddOns) {
        if (addOn instanceof AddOnImpl) {
          ((AddOnImpl) addOn).setNewConfiguration(newConfig);
        }
      }
    }
  }
}
