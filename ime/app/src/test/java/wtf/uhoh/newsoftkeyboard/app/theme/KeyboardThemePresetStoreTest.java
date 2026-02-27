package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardThemePresetStoreTest {

  @Test
  public void testDefaultPresetIsReturned() {
    final var context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore store = new KeyboardThemePresetStore(context);

    final String baseThemeId = "theme_test_default";
    Assert.assertEquals(baseThemeId, store.getActivePresetId(baseThemeId));
    Assert.assertEquals(1, store.listPresets(baseThemeId).size());
    Assert.assertEquals(baseThemeId, store.listPresets(baseThemeId).get(0).id());
  }

  @Test
  public void testCreateRenameDeletePreset() {
    final var context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore store = new KeyboardThemePresetStore(context);

    final String baseThemeId = "theme_test_crd";

    final KeyboardThemePresetStore.Preset created = store.createPreset(baseThemeId, "My preset");
    Assert.assertNotNull(created.id());
    Assert.assertNotEquals(baseThemeId, created.id());
    Assert.assertEquals(2, store.listPresets(baseThemeId).size());

    store.setActivePresetId(baseThemeId, created.id());
    Assert.assertEquals(created.id(), store.getActivePresetId(baseThemeId));

    store.renamePreset(created.id(), "Renamed");
    Assert.assertEquals("Renamed", store.getPresetName(created.id()));

    store.deletePreset(created.id());
    Assert.assertEquals(baseThemeId, store.getActivePresetId(baseThemeId));
    Assert.assertEquals(1, store.listPresets(baseThemeId).size());
  }

  @Test
  public void testPerAppBindingResolvesOnlyWhileImePackageIsSet() {
    final var context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore store = new KeyboardThemePresetStore(context);
    final KeyboardThemePresetAppBindingStore appBindings =
        new KeyboardThemePresetAppBindingStore(context);

    final String baseThemeId = "theme_test_per_app";
    final KeyboardThemePresetStore.Preset globalPreset = store.createPreset(baseThemeId, "Global");
    final KeyboardThemePresetStore.Preset appPreset = store.createPreset(baseThemeId, "Per-app");

    store.setActivePresetId(baseThemeId, globalPreset.id());

    final String appPackageName = "com.example.app";
    appBindings.bindAppToPreset(baseThemeId, appPackageName, appPreset.id());
    appBindings.setLastImePackageName(appPackageName);

    // Without an active IME package, use the global preset.
    appBindings.setCurrentImePackageName(null);
    Assert.assertEquals(globalPreset.id(), store.getActivePresetId(baseThemeId));

    // While IME package is set, use the per-app preset.
    appBindings.setCurrentImePackageName(appPackageName);
    Assert.assertEquals(appPreset.id(), store.getActivePresetId(baseThemeId));

    // Deleting a preset clears bindings that reference it and falls back to global.
    store.deletePreset(appPreset.id());
    Assert.assertEquals(globalPreset.id(), store.getActivePresetId(baseThemeId));

    // Clean up.
    appBindings.setCurrentImePackageName(null);
  }

  @Test
  public void testSessionOverrideTakesPrecedenceWhileImePackageIsSet() {
    final var context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetStore store = new KeyboardThemePresetStore(context);
    final KeyboardThemePresetAppBindingStore appBindings =
        new KeyboardThemePresetAppBindingStore(context);

    final String baseThemeId = "theme_test_session_override";
    final KeyboardThemePresetStore.Preset globalPreset = store.createPreset(baseThemeId, "Global");
    final KeyboardThemePresetStore.Preset sessionPreset =
        store.createPreset(baseThemeId, "Session");
    store.setActivePresetId(baseThemeId, globalPreset.id());

    final String appPackageName = "com.example.app";
    appBindings.setCurrentImePackageName(appPackageName);

    Assert.assertEquals(globalPreset.id(), store.getActivePresetId(baseThemeId));

    KeyboardThemeSessionOverrideStore.setOverride(appPackageName, baseThemeId, sessionPreset.id());
    Assert.assertEquals(sessionPreset.id(), store.getActivePresetId(baseThemeId));

    KeyboardThemeSessionOverrideStore.clearForPackage(appPackageName);
    Assert.assertEquals(globalPreset.id(), store.getActivePresetId(baseThemeId));

    KeyboardThemeSessionOverrideStore.clearAll();
    appBindings.setCurrentImePackageName(null);
  }
}
