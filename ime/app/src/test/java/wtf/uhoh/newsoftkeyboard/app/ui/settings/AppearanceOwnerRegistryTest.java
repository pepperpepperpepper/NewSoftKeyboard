package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import org.junit.Test;

public class AppearanceOwnerRegistryTest {

  @Test
  public void testRegistryHasNoDuplicateIds() {
    AppearanceOwnerRegistry.assertValidOrThrow();
  }

  @Test
  public void testThemeCustomizationSearchEntriesPresent() {
    // If this goes empty, Settings Search may silently fall back to an older hard-coded list.
    org.junit.Assert.assertFalse(
        AppearanceOwnerRegistry.themeCustomizationSearchEntries().isEmpty());
  }
}
