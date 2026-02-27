package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import org.junit.Test;
import org.robolectric.shadows.ShadowDialog;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;

public class TroubleshootingAndBackupSettingsFragmentTest
    extends RobolectricFragmentTestCase<TroubleshootingAndBackupSettingsFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.troubleshootingAndBackupSettingsFragment;
  }

  @Test
  public void testResetLookAndFeelSlidersResetsKnownPreferences() {
    final var context = getApplicationContext();
    final var res = context.getResources();
    final var prefs = NskApplicationBase.prefs(context);

    prefs
        .getInteger(
            R.string.settings_key_zoom_percent_in_portrait,
            R.integer.settings_default_zoom_percent_in_portrait)
        .set(123);
    prefs
        .getInteger(
            R.string.settings_key_zoom_percent_in_landscape,
            R.integer.settings_default_zoom_percent_in_landscape)
        .set(124);
    prefs
        .getInteger(
            R.string.settings_key_bottom_extra_padding_in_portrait,
            R.integer.settings_default_bottom_extra_padding_in_portrait)
        .set(125);
    prefs
        .getInteger(
            R.string.settings_key_vibrate_on_key_press_duration_int,
            R.integer.settings_default_vibrate_on_key_press_duration_int)
        .set(12);
    prefs
        .getInteger(
            R.string.settings_key_system_vibration_fallback_duration_int,
            R.integer.settings_default_system_vibration_fallback_duration_int)
        .set(7);
    prefs
        .getInteger(
            R.string.settings_key_custom_sound_volume,
            R.integer.settings_default_custom_volume_level)
        .set(77);

    final var fragment = startFragment();
    final var resetPref = fragment.findPreference("reset_look_and_feel_sliders");
    assertNotNull(resetPref);
    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(resetPref);
    TestRxSchedulers.drainAllTasks();

    final AlertDialog confirmDialog = getLatestShownAlertDialog();
    assertNotNull(confirmDialog);
    confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
    TestRxSchedulers.drainAllTasks();

    assertEquals(
        res.getInteger(R.integer.settings_default_zoom_percent_in_portrait),
        (int)
            prefs
                .getInteger(
                    R.string.settings_key_zoom_percent_in_portrait,
                    R.integer.settings_default_zoom_percent_in_portrait)
                .get());
    assertEquals(
        res.getInteger(R.integer.settings_default_zoom_percent_in_landscape),
        (int)
            prefs
                .getInteger(
                    R.string.settings_key_zoom_percent_in_landscape,
                    R.integer.settings_default_zoom_percent_in_landscape)
                .get());
    assertEquals(
        res.getInteger(R.integer.settings_default_bottom_extra_padding_in_portrait),
        (int)
            prefs
                .getInteger(
                    R.string.settings_key_bottom_extra_padding_in_portrait,
                    R.integer.settings_default_bottom_extra_padding_in_portrait)
                .get());
    assertEquals(
        res.getInteger(R.integer.settings_default_vibrate_on_key_press_duration_int),
        (int)
            prefs
                .getInteger(
                    R.string.settings_key_vibrate_on_key_press_duration_int,
                    R.integer.settings_default_vibrate_on_key_press_duration_int)
                .get());
    assertEquals(
        res.getInteger(R.integer.settings_default_system_vibration_fallback_duration_int),
        (int)
            prefs
                .getInteger(
                    R.string.settings_key_system_vibration_fallback_duration_int,
                    R.integer.settings_default_system_vibration_fallback_duration_int)
                .get());
    assertEquals(
        res.getInteger(R.integer.settings_default_custom_volume_level),
        (int)
            prefs
                .getInteger(
                    R.string.settings_key_custom_sound_volume,
                    R.integer.settings_default_custom_volume_level)
                .get());
  }

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
}
