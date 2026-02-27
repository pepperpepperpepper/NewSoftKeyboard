package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import org.junit.Test;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowPackageManager;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;

public class ContextProfilesSettingsFragmentTest
    extends RobolectricFragmentTestCase<ContextProfilesSettingsFragment> {

  private static final String TEST_APP_ALPHA_PACKAGE = "com.example.nsktest.alpha";
  private static final String TEST_APP_BETA_PACKAGE = "com.example.nsktest.beta";
  private static final String TEST_PRESET_NAME = "NSK Test Preset";

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.contextProfilesSettingsFragment;
  }

  @Test
  public void testAddBindingFlowShowsPickerAndSavesBinding() {
    final var context = getApplicationContext();
    final var store = new ContextProfilesStore(context);
    final ContextProfilesStore.Preset preset = store.createPreset(TEST_PRESET_NAME);

    try {
      store.unbindApp(TEST_APP_BETA_PACKAGE, ContextFieldSelector.ALL_FIELDS);

      final ShadowPackageManager shadowPm = Shadows.shadowOf(context.getPackageManager());
      addLaunchableApp(shadowPm, TEST_APP_ALPHA_PACKAGE, "NSK Test Alpha");
      addLaunchableApp(shadowPm, TEST_APP_BETA_PACKAGE, "NSK Test Beta");

      final ContextProfilesSettingsFragment fragment = startFragment();
      ShadowDialog.getShownDialogs().clear();

      ViewTestUtils.performClick(fragment.findPreference("context_profiles_manage_bindings"));
      TestRxSchedulers.drainAllTasks();

      final AlertDialog manageBindingsDialog = getLatestShownAlertDialog();
      assertNotNull(manageBindingsDialog);
      manageBindingsDialog
          .getButton(android.content.DialogInterface.BUTTON_POSITIVE)
          .performClick();
      TestRxSchedulers.drainAllTasks();

      final AlertDialog appPickerDialog = getLatestShownAlertDialog();
      assertNotNull(appPickerDialog);
      final ListView appsList = findFirstViewByType(appPickerDialog, ListView.class);
      assertNotNull(appsList);

      final int betaIndex = findIndexContaining(appsList.getAdapter(), TEST_APP_BETA_PACKAGE);
      assertTrue(betaIndex >= 0);
      Shadows.shadowOf(appsList).performItemClick(betaIndex);
      TestRxSchedulers.drainAllTasks();

      final AlertDialog fieldSelectorDialog = getLatestShownAlertDialog();
      assertNotNull(fieldSelectorDialog);
      final ListView fieldList = fieldSelectorDialog.getListView();
      assertNotNull(fieldList);
      Shadows.shadowOf(fieldList).performItemClick(0 /*ALL_FIELDS*/);
      TestRxSchedulers.drainAllTasks();

      final AlertDialog presetPickerDialog = getLatestShownAlertDialog();
      assertNotNull(presetPickerDialog);
      final ListView presetList = presetPickerDialog.getListView();
      assertNotNull(presetList);
      final int presetIndex = findIndexContaining(presetList.getAdapter(), TEST_PRESET_NAME);
      assertTrue(presetIndex >= 0);
      Shadows.shadowOf(presetList).performItemClick(presetIndex);
      TestRxSchedulers.drainAllTasks();

      final String boundPresetId =
          store.getBoundPresetId(TEST_APP_BETA_PACKAGE, ContextFieldSelector.ALL_FIELDS);
      assertTrue(preset.id.equals(boundPresetId));
    } finally {
      store.unbindApp(TEST_APP_BETA_PACKAGE, ContextFieldSelector.ALL_FIELDS);
      store.deletePreset(preset.id);
    }
  }

  private static void addLaunchableApp(
      @NonNull ShadowPackageManager shadowPm, @NonNull String packageName, @NonNull String label) {
    final ResolveInfo resolveInfo = new ResolveInfo();
    resolveInfo.activityInfo = new ActivityInfo();
    resolveInfo.activityInfo.packageName = packageName;
    resolveInfo.activityInfo.name = packageName + ".MainActivity";
    resolveInfo.activityInfo.enabled = true;
    resolveInfo.activityInfo.nonLocalizedLabel = label;

    final Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
    launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    shadowPm.addResolveInfoForIntent(launcherIntent, resolveInfo);
  }

  @Nullable
  private static AlertDialog getLatestShownAlertDialog() {
    final var shownDialogs = ShadowDialog.getShownDialogs();
    for (int i = shownDialogs.size() - 1; i >= 0; i--) {
      final var dialog = shownDialogs.get(i);
      if (dialog instanceof AlertDialog && dialog.isShowing()) {
        return (AlertDialog) dialog;
      }
    }
    return null;
  }

  @Nullable
  private static <T extends View> T findFirstViewByType(
      @NonNull AlertDialog dialog, @NonNull Class<T> type) {
    final var window = dialog.getWindow();
    if (window == null) return null;
    final var decorView = window.getDecorView();
    return findFirstViewByType(decorView, type);
  }

  @Nullable
  private static <T extends View> T findFirstViewByType(
      @NonNull View root, @NonNull Class<T> type) {
    if (type.isInstance(root)) return type.cast(root);
    if (root instanceof ViewGroup) {
      final ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        final T found = findFirstViewByType(group.getChildAt(i), type);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static int findIndexContaining(@NonNull ListAdapter adapter, @NonNull String needle) {
    for (int i = 0; i < adapter.getCount(); i++) {
      final Object item = adapter.getItem(i);
      if (item != null && item.toString().contains(needle)) return i;
    }
    return -1;
  }
}
