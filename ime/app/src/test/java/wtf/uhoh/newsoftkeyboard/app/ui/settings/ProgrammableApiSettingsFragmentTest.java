package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.preference.CheckBoxPreference;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowDialog;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

public class ProgrammableApiSettingsFragmentTest
    extends RobolectricFragmentTestCase<ProgrammableApiSettingsFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.programmableApiSettingsFragment;
  }

  @Before
  public void resetPrefs() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_keyboard_api_enabled, false);
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_keyboard_api_high_risk_actions_enabled, false);
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_keyboard_api_clipboard_copy_cut_enabled, false);
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_keyboard_api_automation_controllers_enabled, false);
    ShadowDialog.getShownDialogs().clear();
  }

  @Test
  public void testEnablingHighRiskRequiresConfirmation() {
    final ProgrammableApiSettingsFragment fragment = startFragment();

    final CheckBoxPreference enabledPref =
        fragment.findPreference(
            getApplicationContext().getString(R.string.settings_key_keyboard_api_enabled));
    final CheckBoxPreference highRiskPref =
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_keyboard_api_high_risk_actions_enabled));
    assertNotNull(enabledPref);
    assertNotNull(highRiskPref);

    ViewTestUtils.performClick(enabledPref);
    assertTrue(enabledPref.isChecked());
    assertTrue(highRiskPref.isEnabled());

    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(highRiskPref);
    assertFalse(highRiskPref.isChecked());

    AlertDialog dialog = getLatestShownAlertDialog();
    assertNotNull(dialog);
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    assertFalse(highRiskPref.isChecked());

    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(highRiskPref);
    dialog = getLatestShownAlertDialog();
    assertNotNull(dialog);
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    assertTrue(highRiskPref.isChecked());
  }

  @Test
  public void testEnablingClipboardCopyCutRequiresConfirmation() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_keyboard_api_enabled, true);
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_keyboard_api_high_risk_actions_enabled, true);

    final ProgrammableApiSettingsFragment fragment = startFragment();

    final CheckBoxPreference pref =
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_keyboard_api_clipboard_copy_cut_enabled));
    assertNotNull(pref);
    assertTrue(pref.isEnabled());
    assertFalse(pref.isChecked());

    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(pref);
    assertFalse(pref.isChecked());

    AlertDialog dialog = getLatestShownAlertDialog();
    assertNotNull(dialog);
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    assertFalse(pref.isChecked());

    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(pref);
    dialog = getLatestShownAlertDialog();
    assertNotNull(dialog);
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    assertTrue(pref.isChecked());
  }

  @Test
  public void testEnablingAutomationControllersRequiresConfirmation() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_keyboard_api_enabled, true);
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_keyboard_api_high_risk_actions_enabled, true);

    final ProgrammableApiSettingsFragment fragment = startFragment();

    final CheckBoxPreference pref =
        fragment.findPreference(
            getApplicationContext()
                .getString(R.string.settings_key_keyboard_api_automation_controllers_enabled));
    assertNotNull(pref);
    assertTrue(pref.isEnabled());
    assertFalse(pref.isChecked());

    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(pref);
    assertFalse(pref.isChecked());

    AlertDialog dialog = getLatestShownAlertDialog();
    assertNotNull(dialog);
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    assertFalse(pref.isChecked());

    ShadowDialog.getShownDialogs().clear();
    ViewTestUtils.performClick(pref);
    dialog = getLatestShownAlertDialog();
    assertNotNull(dialog);
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    assertTrue(pref.isChecked());
  }

  private static AlertDialog getLatestShownAlertDialog() {
    final List<Dialog> shownDialogs = ShadowDialog.getShownDialogs();
    for (int i = shownDialogs.size() - 1; i >= 0; i--) {
      final Dialog dialog = shownDialogs.get(i);
      if (dialog instanceof AlertDialog && dialog.isShowing()) {
        return (AlertDialog) dialog;
      }
    }
    return null;
  }
}
