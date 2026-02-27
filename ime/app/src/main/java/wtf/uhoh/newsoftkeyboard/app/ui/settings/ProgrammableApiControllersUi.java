package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPairingStore;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPrefs;

final class ProgrammableApiControllersUi {

  static final String KEY_PAIRING_CATEGORY = "keyboard_api_pairing_category";
  static final String KEY_CONTROLLERS_CATEGORY = "keyboard_api_controllers_category";
  static final String KEY_ADD_CONTROLLER = "keyboard_api_add_controller";

  @NonNull private final ProgrammableApiControllersListUi mControllersListUi;
  @NonNull private final ProgrammableApiPairingRequestsUi mPairingRequestsUi;

  ProgrammableApiControllersUi(
      @NonNull PreferenceFragmentCompat fragment,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull KeyboardApiPairingStore pairingStore) {
    final ProgrammableApiSessionOverridesUi sessionOverridesUi =
        new ProgrammableApiSessionOverridesUi(fragment, prefs);
    final Runnable refreshAll =
        () -> {
          refreshControllers();
          refreshPairingRequests();
        };
    mControllersListUi =
        new ProgrammableApiControllersListUi(
            fragment, prefs, pairingStore, sessionOverridesUi, refreshAll);
    mPairingRequestsUi =
        new ProgrammableApiPairingRequestsUi(fragment, prefs, pairingStore, refreshAll);
  }

  void bindPreferences() {
    mControllersListUi.bindPreferences();
  }

  void refreshControllers() {
    mControllersListUi.refreshControllers();
  }

  void refreshPairingRequests() {
    mPairingRequestsUi.refreshPairingRequests();
  }
}
