package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.preference.Preference;

final class AppearanceOwnerNavigation {

  private AppearanceOwnerNavigation() {}

  static boolean navigateToOwner(@NonNull View view, @NonNull String canonicalId) {
    final AppearanceOwnerRegistry.Owner owner = AppearanceOwnerRegistry.ownerForId(canonicalId);
    if (owner == null) return false;

    final Bundle args = new Bundle();
    if (owner.scrollToKey != null) {
      args.putString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY, owner.scrollToKey);
    }
    Navigation.findNavController(view).navigate(owner.ownerDestinationId, args);
    return true;
  }

  static void bindPreference(
      @NonNull Preference preference, @NonNull View view, @NonNull String canonicalId) {
    preference.setOnPreferenceClickListener(
        ignored -> {
          navigateToOwner(view, canonicalId);
          return true;
        });
  }
}
