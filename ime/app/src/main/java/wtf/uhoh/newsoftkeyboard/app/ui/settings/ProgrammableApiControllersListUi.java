package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.InputType;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import java.util.ArrayList;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiControllerTokensStore;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPairingNotifier;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPairingStore;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPrefs;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiSignatureUtils;

final class ProgrammableApiControllersListUi {

  @NonNull private final PreferenceFragmentCompat mFragment;
  @NonNull private final KeyboardApiPrefs mPrefs;
  @NonNull private final KeyboardApiPairingStore mPairingStore;
  @NonNull private final ProgrammableApiSessionOverridesUi mSessionOverridesUi;
  @NonNull private final Runnable mRefreshAll;

  ProgrammableApiControllersListUi(
      @NonNull PreferenceFragmentCompat fragment,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull KeyboardApiPairingStore pairingStore,
      @NonNull ProgrammableApiSessionOverridesUi sessionOverridesUi,
      @NonNull Runnable refreshAll) {
    mFragment = fragment;
    mPrefs = prefs;
    mPairingStore = pairingStore;
    mSessionOverridesUi = sessionOverridesUi;
    mRefreshAll = refreshAll;
  }

  void bindPreferences() {
    final Preference addController =
        mFragment.findPreference(ProgrammableApiControllersUi.KEY_ADD_CONTROLLER);
    if (addController == null) return;
    addController.setOnPreferenceClickListener(
        p -> {
          showAddControllerDialog();
          return true;
        });
  }

  void refreshControllers() {
    final PreferenceCategory category =
        mFragment.findPreference(ProgrammableApiControllersUi.KEY_CONTROLLERS_CATEGORY);
    if (category == null) return;

    final Preference addController =
        mFragment.findPreference(ProgrammableApiControllersUi.KEY_ADD_CONTROLLER);
    category.removeAll();
    if (addController != null) category.addPreference(addController);

    final Set<String> controllers = mPrefs.getAllowListedPackages();
    final Context context = mFragment.requireContext();
    for (String pkg : controllers) {
      final Preference p = new Preference(context);
      final String label = mPrefs.getControllerLabel(pkg);
      p.setTitle(label == null ? pkg : label);
      p.setSummary(pkg);
      p.setOnPreferenceClickListener(
          pref -> {
            showControllerDialog(pkg);
            return true;
          });
      category.addPreference(p);
    }
  }

  private void showAddControllerDialog() {
    final Context context = mFragment.requireContext();
    final EditText input = new EditText(context);
    input.setHint("com.android.shell");
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_api_add_controller_title)
        .setMessage(R.string.keyboard_api_add_controller_message)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              final String pkg = String.valueOf(input.getText()).trim();
              if (pkg.isEmpty()) return;
              addControllerByPackage(pkg);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void addControllerByPackage(@NonNull String packageName) {
    final Context context = mFragment.requireContext();
    final PackageManager pm = context.getPackageManager();
    final ApplicationInfo applicationInfo;
    try {
      applicationInfo = pm.getApplicationInfo(packageName, 0);
    } catch (PackageManager.NameNotFoundException e) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_add_controller_failed_title),
          mFragment.getString(R.string.keyboard_api_add_controller_failed_message, packageName));
      return;
    }

    final String label = String.valueOf(pm.getApplicationLabel(applicationInfo));
    final Set<String> digests;
    try {
      digests = KeyboardApiSignatureUtils.getSigningCertDigestsSha256(context, packageName);
    } catch (PackageManager.NameNotFoundException e) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_add_controller_failed_title),
          mFragment.getString(R.string.keyboard_api_add_controller_failed_message, packageName));
      return;
    }

    if (digests.isEmpty()) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_add_controller_failed_title),
          mFragment.getString(
              R.string.keyboard_api_add_controller_failed_no_signatures, packageName));
      return;
    }

    ProgrammableApiScopesUi.showScopesDialog(
        context,
        mFragment.getString(R.string.keyboard_api_scopes_dialog_title, label),
        ProgrammableApiScopesUi.defaultScopes(),
        selectedScopes -> {
          mPrefs.addOrUpdateController(packageName, label, digests, selectedScopes);
          mRefreshAll.run();
        });
  }

  private void showControllerDialog(@NonNull String packageName) {
    final Context context = mFragment.requireContext();
    final String label = mPrefs.getControllerLabel(packageName);
    final Set<String> scopes = mPrefs.getControllerScopes(packageName);
    final Set<String> digests = mPrefs.getControllerCertDigestsSha256(packageName);
    final int allowedSessionPresets = mPrefs.getAllowedSessionPresetIds(packageName).size();
    final int allowedSessionThemePresets =
        mPrefs.getAllowedSessionThemePresetIds(packageName).size();
    final int allowedSessionKeyboards = mPrefs.getAllowedSessionKeyboardIds(packageName).size();
    final int allowedSessionTargetApps = mPrefs.getAllowedSessionTargetPackages(packageName).size();

    final StringBuilder message = new StringBuilder();
    message.append(
        mFragment.getString(R.string.keyboard_api_controller_details_package, packageName));
    message.append("\n\n");
    message.append(mFragment.getString(R.string.keyboard_api_controller_details_scopes));
    if (scopes.isEmpty()) {
      message
          .append("\n- ")
          .append(mFragment.getString(R.string.keyboard_api_controller_details_none));
    } else {
      final ArrayList<String> sortedScopes = new ArrayList<>(scopes);
      java.util.Collections.sort(sortedScopes);
      for (String s : sortedScopes) message.append("\n- ").append(s);
    }
    message.append("\n\n");
    message.append(mFragment.getString(R.string.keyboard_api_controller_details_signatures));
    if (digests.isEmpty()) {
      message
          .append("\n- ")
          .append(mFragment.getString(R.string.keyboard_api_controller_details_none));
    } else {
      final ArrayList<String> sortedDigests = new ArrayList<>(digests);
      java.util.Collections.sort(sortedDigests);
      for (String d : sortedDigests) message.append("\n- ").append(d);
    }
    message.append("\n\n");
    message.append(
        mFragment.getString(
            R.string.keyboard_api_controller_details_session_overrides_counts,
            allowedSessionPresets,
            allowedSessionThemePresets,
            allowedSessionKeyboards,
            allowedSessionTargetApps));

    new AlertDialog.Builder(context)
        .setTitle(label == null ? packageName : label)
        .setMessage(message.toString())
        .setPositiveButton(android.R.string.ok, null)
        .setNeutralButton(
            R.string.keyboard_api_manage_controller_title,
            (dialog, which) ->
                showControllerManageDialog(context, packageName, label, scopes, digests))
        .setNegativeButton(
            R.string.keyboard_api_remove_controller_title,
            (dialog, which) -> confirmRemoveController(packageName, label))
        .show();
  }

  private void showControllerManageDialog(
      @NonNull Context context,
      @NonNull String packageName,
      @Nullable String label,
      @NonNull Set<String> scopes,
      @NonNull Set<String> digests) {
    final String title = label == null ? packageName : label;
    final String[] items = {
      mFragment.getString(R.string.keyboard_api_edit_scopes_title),
      mFragment.getString(R.string.keyboard_api_edit_session_overrides_title)
    };

    new AlertDialog.Builder(context)
        .setTitle(title)
        .setItems(
            items,
            (d, which) -> {
              if (which == 0) {
                ProgrammableApiScopesUi.showScopesDialog(
                    context,
                    mFragment.getString(R.string.keyboard_api_scopes_dialog_title, title),
                    scopes,
                    selectedScopes -> {
                      mPrefs.addOrUpdateController(packageName, label, digests, selectedScopes);
                      mRefreshAll.run();
                    });
              } else if (which == 1) {
                mSessionOverridesUi.showSessionOverridesDialog(context, packageName, title);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmRemoveController(@NonNull String packageName, @Nullable String label) {
    final Context context = mFragment.requireContext();
    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_api_remove_controller_title)
        .setMessage(
            mFragment.getString(
                R.string.keyboard_api_remove_controller_message,
                label == null ? packageName : label))
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              mPrefs.removeController(packageName);
              KeyboardApiControllerTokensStore.clearToken(context, packageName);
              mPairingStore.clear(packageName);
              KeyboardApiPairingNotifier.refresh(context, mPairingStore);
              mRefreshAll.run();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showSimpleMessageDialog(@NonNull String title, @NonNull String message) {
    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
