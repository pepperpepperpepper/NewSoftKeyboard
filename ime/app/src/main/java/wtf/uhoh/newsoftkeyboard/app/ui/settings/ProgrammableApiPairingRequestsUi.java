package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
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

final class ProgrammableApiPairingRequestsUi {

  @NonNull private final PreferenceFragmentCompat mFragment;
  @NonNull private final KeyboardApiPrefs mPrefs;
  @NonNull private final KeyboardApiPairingStore mPairingStore;
  @NonNull private final Runnable mRefreshAll;

  ProgrammableApiPairingRequestsUi(
      @NonNull PreferenceFragmentCompat fragment,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull KeyboardApiPairingStore pairingStore,
      @NonNull Runnable refreshAll) {
    mFragment = fragment;
    mPrefs = prefs;
    mPairingStore = pairingStore;
    mRefreshAll = refreshAll;
  }

  void refreshPairingRequests() {
    final PreferenceCategory category =
        mFragment.findPreference(ProgrammableApiControllersUi.KEY_PAIRING_CATEGORY);
    if (category == null) return;

    category.removeAll();

    final Set<String> pending = mPairingStore.getPendingPackages();
    if (pending.isEmpty()) {
      final Preference none = new Preference(mFragment.requireContext());
      none.setSelectable(false);
      none.setSummary(R.string.keyboard_api_pairing_requests_none_summary);
      category.addPreference(none);
      return;
    }

    final Context context = mFragment.requireContext();
    for (String pkg : pending) {
      final Preference p = new Preference(context);
      p.setTitle(toAppLabel(context, pkg));
      p.setSummary(pkg);
      p.setOnPreferenceClickListener(
          pref -> {
            showPairingRequestDialog(pkg);
            return true;
          });
      category.addPreference(p);
    }
  }

  private void showPairingRequestDialog(@NonNull String packageName) {
    final Context context = mFragment.requireContext();
    final String label = toAppLabel(context, packageName);
    final Set<String> requestedScopes = mPairingStore.getRequestedScopes(packageName);

    final StringBuilder message = new StringBuilder();
    message.append(
        mFragment.getString(R.string.keyboard_api_controller_details_package, packageName));
    message.append("\n\n");
    message.append(mFragment.getString(R.string.keyboard_api_pairing_request_requested_scopes));
    if (requestedScopes.isEmpty()) {
      message
          .append("\n- ")
          .append(mFragment.getString(R.string.keyboard_api_controller_details_none));
    } else {
      final ArrayList<String> sortedScopes = new ArrayList<>(requestedScopes);
      java.util.Collections.sort(sortedScopes);
      for (String s : sortedScopes) message.append("\n- ").append(s);
    }

    new AlertDialog.Builder(context)
        .setTitle(mFragment.getString(R.string.keyboard_api_pairing_request_title, label))
        .setMessage(message.toString())
        .setPositiveButton(
            R.string.keyboard_api_pairing_request_approve,
            (d, which) ->
                ProgrammableApiScopesUi.showScopesDialog(
                    context,
                    mFragment.getString(R.string.keyboard_api_scopes_dialog_title, label),
                    requestedScopes,
                    selectedScopes -> approvePairingRequest(packageName, selectedScopes)))
        .setNegativeButton(
            R.string.keyboard_api_pairing_request_deny,
            (d, which) -> {
              mPairingStore.deny(packageName, System.currentTimeMillis());
              KeyboardApiPairingNotifier.refresh(context, mPairingStore);
              mRefreshAll.run();
            })
        .setNeutralButton(android.R.string.cancel, null)
        .show();
  }

  private void approvePairingRequest(
      @NonNull String packageName, @NonNull Set<String> selectedScopes) {
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

    final String token = KeyboardApiControllerTokensStore.generateNewToken();
    KeyboardApiControllerTokensStore.setToken(context, packageName, token);

    mPrefs.addOrUpdateController(packageName, label, digests, selectedScopes);
    mPairingStore.approve(packageName, selectedScopes, System.currentTimeMillis());
    KeyboardApiPairingNotifier.refresh(context, mPairingStore);
    mRefreshAll.run();
  }

  @NonNull
  private static String toAppLabel(@NonNull Context context, @NonNull String packageName) {
    final PackageManager pm = context.getPackageManager();
    try {
      final ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
      return String.valueOf(pm.getApplicationLabel(ai));
    } catch (PackageManager.NameNotFoundException e) {
      return packageName;
    }
  }

  private void showSimpleMessageDialog(@NonNull String title, @NonNull String message) {
    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
