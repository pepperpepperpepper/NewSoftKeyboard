package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Source-of-truth mapping for appearance controls to their single owning surface.
 *
 * <p>NOTE: this is intentionally a small, explicit registry (no reflection, no extra deps). It is
 * primarily used for Settings Search deep-links and for preventing appearance controls from
 * accidentally gaining a second "owner" UI.
 */
final class AppearanceOwnerRegistry {

  enum Scope {
    GLOBAL,
    PRESET,
    SYSTEM_OVERLAY
  }

  static final class Owner {
    @NonNull final String canonicalId;
    @NonNull final Scope scope;
    @IdRes final int ownerDestinationId;
    @Nullable final String scrollToKey;

    Owner(
        @NonNull String canonicalId,
        @NonNull Scope scope,
        @IdRes int ownerDestinationId,
        @Nullable String scrollToKey) {
      this.canonicalId = canonicalId;
      this.scope = scope;
      this.ownerDestinationId = ownerDestinationId;
      this.scrollToKey = scrollToKey;
    }
  }

  static final class ThemeCustomizationSearchEntry {
    @StringRes final int titleResId;
    @StringRes final int summaryResId;
    @NonNull final String scrollToKey;

    ThemeCustomizationSearchEntry(
        @StringRes int titleResId, @StringRes int summaryResId, @NonNull String scrollToKey) {
      this.titleResId = titleResId;
      this.summaryResId = summaryResId;
      this.scrollToKey = scrollToKey;
    }
  }

  private static final List<Owner> OWNERS = AppearanceOwnerRegistryOwners.buildOwners();
  private static final List<ThemeCustomizationSearchEntry> THEME_CUSTOMIZATION_SEARCH_ENTRIES =
      AppearanceOwnerRegistryThemeCustomizationSearchEntries.buildThemeCustomizationSearchEntries();

  private AppearanceOwnerRegistry() {}

  @NonNull
  static List<Owner> owners() {
    return OWNERS;
  }

  @Nullable
  static Owner ownerForId(@NonNull String canonicalId) {
    for (Owner owner : OWNERS) {
      if (owner.canonicalId.equals(canonicalId)) return owner;
    }
    return null;
  }

  @NonNull
  static List<ThemeCustomizationSearchEntry> themeCustomizationSearchEntries() {
    return THEME_CUSTOMIZATION_SEARCH_ENTRIES;
  }

  static void assertValidOrThrow() {
    final Set<String> ids = new HashSet<>();
    for (Owner owner : OWNERS) {
      if (!ids.add(owner.canonicalId)) {
        throw new IllegalStateException("Duplicate appearance owner id: " + owner.canonicalId);
      }
    }
  }
}
