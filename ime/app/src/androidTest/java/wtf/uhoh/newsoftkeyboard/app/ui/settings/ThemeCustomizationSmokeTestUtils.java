package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

final class ThemeCustomizationSmokeTestUtils {

  private ThemeCustomizationSmokeTestUtils() {}

  @NonNull
  static ActivityScenario<MainSettingsActivity> launchMainSettings() {
    final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final Intent intent =
        new Intent(targetContext, MainSettingsActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return ActivityScenario.launch(intent);
  }

  static void ensureKeyboardEnabled(@NonNull MainSettingsActivity activity) {
    ensureKeyboardEnabled((Context) activity);
  }

  static void ensureKeyboardEnabled(@NonNull Context context) {
    final var prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_extension_keyboard_enabled), true)
        .apply();
  }

  @NonNull
  static String ensureDefaultThemeEnabled(@NonNull Context context) {
    final String themeId = context.getString(R.string.settings_default_keyboard_theme_key);
    final var prefs = DirectBootAwareSharedPreferences.create(context);
    final var editor = prefs.edit();
    for (String key : prefs.getAll().keySet()) {
      if (key.startsWith("theme_")) editor.remove(key);
    }
    editor.putBoolean("theme_" + themeId, true).apply();
    return themeId;
  }

  @Nullable
  static <T extends Fragment> T findNavFragment(
      @NonNull MainSettingsActivity activity, @NonNull Class<T> clazz) {
    final Fragment navHost =
        activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    if (!(navHost instanceof NavHostFragment host)) return null;
    for (Fragment child : host.getChildFragmentManager().getFragments()) {
      if (clazz.isInstance(child)) return clazz.cast(child);
    }
    return null;
  }

  @Nullable
  static Preference findPreferenceByTitle(
      @NonNull PreferenceGroup group, @NonNull CharSequence title) {
    final int count = group.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      final Preference pref = group.getPreference(i);
      if (title.equals(pref.getTitle())) return pref;
      if (pref instanceof PreferenceGroup nested) {
        final Preference nestedFound = findPreferenceByTitle(nested, title);
        if (nestedFound != null) return nestedFound;
      }
    }
    return null;
  }

  @NonNull
  static void waitForLivePreviewBackgroundDrawable(
      @NonNull ActivityScenario<MainSettingsActivity> scenario, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
    final String[] lastState = new String[] {"unknown"};
    while (SystemClock.uptimeMillis() < deadline) {
      final boolean[] found = new boolean[] {false};
      scenario.onActivity(
          activity -> {
            final View preview = activity.findViewById(R.id.wallpaper_live_preview_keyboard);
            if (preview == null) {
              lastState[0] = "missing live preview view";
              return;
            }
            if (!(preview instanceof DemoKeyboardView)) {
              lastState[0] = "unexpected live preview view class: " + preview.getClass().getName();
              return;
            }
            final var bg = preview.getBackground();
            if (bg == null) {
              lastState[0] = "live preview background is null";
              return;
            }
            lastState[0] = "live preview background: " + bg.getClass().getName();
            found[0] = true;
          });
      if (found[0]) return;
      SystemClock.sleep(200L);
    }
    throw new AssertionError(
        "Timed out waiting for live preview background drawable. Last state: " + lastState[0]);
  }

  static int captureLivePreviewBackgroundLuma(
      @NonNull ActivityScenario<MainSettingsActivity> scenario) {
    final int[] luma = new int[] {-1};
    scenario.onActivity(
        activity -> {
          final View preview = activity.findViewById(R.id.wallpaper_live_preview_keyboard);
          if (!(preview instanceof DemoKeyboardView)) return;
          final Drawable bg = preview.getBackground();
          if (bg == null) return;

          final int size = 64;
          final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
          try {
            final Canvas canvas = new Canvas(bitmap);
            bg.setBounds(0, 0, size, size);
            bg.draw(canvas);
            final int color = bitmap.getPixel(size / 2, size / 2);
            luma[0] =
                (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color))
                    / 1000;
          } finally {
            bitmap.recycle();
          }
        });
    if (luma[0] < 0) throw new AssertionError("Failed capturing live preview background luma.");
    return luma[0];
  }

  static void waitForLivePreviewBackgroundLumaAtMost(
      @NonNull ActivityScenario<MainSettingsActivity> scenario, int maxLuma, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
    int last = -1;
    while (SystemClock.uptimeMillis() < deadline) {
      last = captureLivePreviewBackgroundLuma(scenario);
      if (last <= maxLuma) return;
      SystemClock.sleep(200L);
    }
    throw new AssertionError(
        "Timed out waiting for live preview background luma <= "
            + maxLuma
            + ". Last="
            + last
            + ".");
  }

  static boolean getAllowExpensiveWallpaperEffects(@NonNull DemoKeyboardView view) {
    try {
      final Field controllerField =
          KeyboardViewBase.class.getDeclaredField("themeAndDrawController");
      controllerField.setAccessible(true);
      final Object controller = controllerField.get(view);
      if (controller == null) {
        throw new AssertionError("themeAndDrawController should not be null.");
      }

      final Field allowField =
          controller.getClass().getDeclaredField("allowExpensiveWallpaperEffects");
      allowField.setAccessible(true);
      return Boolean.TRUE.equals(allowField.get(controller));
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed reading allowExpensiveWallpaperEffects via reflection.", e);
    }
  }

  static void executeShellCommand(@NonNull UiAutomation automation, @NonNull String cmd)
      throws IOException {
    try (ParcelFileDescriptor pfd = automation.executeShellCommand(cmd);
        FileInputStream in = new FileInputStream(pfd.getFileDescriptor())) {
      // Drain output to ensure the command completes.
      final byte[] buf = new byte[256];
      //noinspection StatementWithEmptyBody
      while (in.read(buf) > 0) {}
    }
  }

  @NonNull
  static String executeShellCommandToString(@NonNull UiAutomation automation, @NonNull String cmd)
      throws IOException {
    try (ParcelFileDescriptor pfd = automation.executeShellCommand(cmd);
        FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      final byte[] buf = new byte[8 * 1024];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }
}
