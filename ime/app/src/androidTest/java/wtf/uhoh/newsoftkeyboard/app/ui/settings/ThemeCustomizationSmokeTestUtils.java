package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
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
    final var prefs = DirectBootAwareSharedPreferences.create(activity);
    prefs
        .edit()
        .putBoolean(activity.getString(R.string.settings_key_extension_keyboard_enabled), true)
        .apply();
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
  static LayerDrawable waitForLivePreviewLayerBackground(
      @NonNull ActivityScenario<MainSettingsActivity> scenario, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
    while (SystemClock.uptimeMillis() < deadline) {
      final LayerDrawable[] found = new LayerDrawable[1];
      scenario.onActivity(
          activity -> {
            final View preview = activity.findViewById(R.id.wallpaper_live_preview_keyboard);
            if (!(preview instanceof DemoKeyboardView)) return;
            final var bg = preview.getBackground();
            if (bg instanceof LayerDrawable layer) {
              found[0] = layer;
            }
          });
      if (found[0] != null) return found[0];
      SystemClock.sleep(200L);
    }
    throw new AssertionError("Timed out waiting for live preview LayerDrawable background.");
  }

  static void waitForDimOverlayAlpha(
      @NonNull ActivityScenario<MainSettingsActivity> scenario, int expectedAlpha, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
    final int[] lastAlpha = new int[] {-1};
    final int[] lastLayerCount = new int[] {-1};
    final String[] lastBgClass = new String[] {null};
    final String[] lastLayer0Class = new String[] {null};
    final String[] lastLayer1Class = new String[] {null};
    while (SystemClock.uptimeMillis() < deadline) {
      final int[] foundAlpha = new int[] {-1};
      scenario.onActivity(
          activity -> {
            final View preview = activity.findViewById(R.id.wallpaper_live_preview_keyboard);
            if (!(preview instanceof DemoKeyboardView)) return;
            final var bg = preview.getBackground();
            lastBgClass[0] = bg != null ? bg.getClass().getName() : null;
            if (!(bg instanceof LayerDrawable layer)) return;
            if (layer.getNumberOfLayers() < 2) return;
            foundAlpha[0] = layer.getDrawable(1).getAlpha();
            lastAlpha[0] = foundAlpha[0];
            lastLayerCount[0] = layer.getNumberOfLayers();
            lastLayer0Class[0] =
                layer.getDrawable(0) != null ? layer.getDrawable(0).getClass().getName() : null;
            lastLayer1Class[0] =
                layer.getDrawable(1) != null ? layer.getDrawable(1).getClass().getName() : null;
          });
      // Some Android builds appear to quantize background alpha; accept +/- 1 to avoid flakes.
      if (foundAlpha[0] >= 0 && Math.abs(foundAlpha[0] - expectedAlpha) <= 1) return;
      SystemClock.sleep(200L);
    }
    throw new AssertionError(
        "Timed out waiting for dim overlay alpha "
            + expectedAlpha
            + ". Last alpha="
            + lastAlpha[0]
            + ", bg="
            + lastBgClass[0]
            + ", layers="
            + lastLayerCount[0]
            + ", layer0="
            + lastLayer0Class[0]
            + ", layer1="
            + lastLayer1Class[0]
            + ".");
  }

  static boolean getAllowExpensiveWallpaperEffects(@NonNull DemoKeyboardView view) {
    try {
      final Field field = KeyboardViewBase.class.getDeclaredField("allowExpensiveWallpaperEffects");
      field.setAccessible(true);
      return Boolean.TRUE.equals(field.get(view));
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
