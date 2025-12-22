package com.anysoftkeyboard.crash;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.base.utils.NullLogProvider;
import com.anysoftkeyboard.chewbacca.ChewbaccaUncaughtExceptionHandler;
import com.anysoftkeyboard.notification.NotificationDriver;
import com.anysoftkeyboard.ui.SendBugReportUiActivity;
import com.anysoftkeyboard.ui.dev.DeveloperUtils;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class CrashHandlerInstaller {

  public static void install(
      @NonNull Context app,
      @NonNull SharedPreferences prefs,
      @NonNull Resources resources,
      @NonNull NotificationDriver notificationDriver,
      boolean testingBuild,
      boolean debugBuild) {
    JustPrintExceptionHandler globalErrorHandler = new JustPrintExceptionHandler();
    RxJavaPlugins.setErrorHandler(globalErrorHandler);
    Thread.setDefaultUncaughtExceptionHandler(globalErrorHandler);

    if (prefs.getBoolean(
        resources.getString(R.string.settings_key_show_chewbacca),
        resources.getBoolean(R.bool.settings_default_show_chewbacca))) {
      final boolean enableCrashNotifications = !testingBuild || !debugBuild;
      final ChewbaccaUncaughtExceptionHandler chewbaccaUncaughtExceptionHandler =
          new AnyChewbaccaUncaughtExceptionHandler(
              app, globalErrorHandler, notificationDriver, enableCrashNotifications);
      Thread.setDefaultUncaughtExceptionHandler(chewbaccaUncaughtExceptionHandler);
      RxJavaPlugins.setErrorHandler(
          e -> chewbaccaUncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e));

      if (chewbaccaUncaughtExceptionHandler.performCrashDetectingFlow()) {
        Logger.w("NSKApp", "Previous crash detected and reported!");
      }
    }

    Logger.setLogProvider(new NullLogProvider());
  }

  private static class JustPrintExceptionHandler
      implements Consumer<Throwable>, Thread.UncaughtExceptionHandler {
    @Override
    public void accept(Throwable throwable) {
      throwable.printStackTrace();
      Logger.e("NSK_FATAL", throwable, "Fatal RxJava error %s", throwable.getMessage());
    }

    @Override
    public void uncaughtException(Thread t, Throwable throwable) {
      throwable.printStackTrace();
      Logger.e(
          "NSK_FATAL",
          throwable,
          "Fatal Java error '%s' on thread '%s'",
          throwable.getMessage(),
          t.toString());
    }
  }

  private static class AnyChewbaccaUncaughtExceptionHandler
      extends ChewbaccaUncaughtExceptionHandler {

    public AnyChewbaccaUncaughtExceptionHandler(
        @NonNull Context app,
        @Nullable Thread.UncaughtExceptionHandler previous,
        @NonNull NotificationDriver notificationDriver,
        boolean notificationsEnabled) {
      super(app, previous, notificationDriver, notificationsEnabled);
    }

    @NonNull
    @Override
    protected Intent createBugReportingActivityIntent() {
      return new Intent(mApp, SendBugReportUiActivity.class);
    }

    @NonNull
    @Override
    protected String getAppDetails() {
      return DeveloperUtils.getAppDetails(mApp);
    }
  }
}
