package wtf.uhoh.newsoftkeyboard.prefs.context;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ContextProfilesStoreTest {

  private static ContextProfilesStore createStore() {
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs =
        context.getSharedPreferences(
            "ContextProfilesStoreTest_" + System.nanoTime(), Context.MODE_PRIVATE);
    return new ContextProfilesStore(prefs);
  }

  @Test
  public void testCreateRenameAndVoiceRules() {
    final ContextProfilesStore store = createStore();

    final ContextProfilesStore.Preset created = store.createPreset("My preset");
    Assert.assertNotNull(created.id);
    Assert.assertEquals("My preset", created.name);
    Assert.assertTrue(created.createdAtMillis > 0L);
    Assert.assertTrue(created.voiceRules.isEmpty());
    Assert.assertTrue(created.typedRules.isEmpty());
    Assert.assertFalse(created.safeToggles.disableContactsDictionary);
    Assert.assertFalse(created.safeToggles.disableUserDictionary);
    Assert.assertFalse(created.safeToggles.disableQuickFixes);
    Assert.assertFalse(created.safeToggles.disableNextWordSuggestions);

    store.renamePreset(created.id, "Renamed");
    final ContextProfilesStore.Preset renamed = store.getPreset(created.id);
    Assert.assertNotNull(renamed);
    Assert.assertEquals("Renamed", renamed.name);

    store.setVoiceRules(
        created.id,
        Arrays.asList(
            new ContextProfilesStore.VoiceRule("how are you?", "hru?", true),
            new ContextProfilesStore.VoiceRule("what's up?", "whatsup", false)));
    final ContextProfilesStore.Preset withRules = store.getPreset(created.id);
    Assert.assertNotNull(withRules);
    Assert.assertEquals(2, withRules.voiceRules.size());
    Assert.assertEquals("how are you?", withRules.voiceRules.get(0).match);
    Assert.assertEquals("hru?", withRules.voiceRules.get(0).replace);
    Assert.assertTrue(withRules.voiceRules.get(0).autoApply);
    Assert.assertFalse(withRules.voiceRules.get(1).autoApply);

    store.setTypedRules(
        created.id,
        Arrays.asList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", false, true),
            new ContextProfilesStore.TypedRule(
                "brb",
                "be right back",
                true,
                false,
                true /*matchCaseSensitive*/,
                false /*matchWholeWord*/)));
    final ContextProfilesStore.Preset withTypedRules = store.getPreset(created.id);
    Assert.assertNotNull(withTypedRules);
    Assert.assertEquals(2, withTypedRules.typedRules.size());
    Assert.assertEquals("how are you", withTypedRules.typedRules.get(0).match);
    Assert.assertEquals("hru?", withTypedRules.typedRules.get(0).replace);
    Assert.assertFalse(withTypedRules.typedRules.get(0).autoApply);
    Assert.assertTrue(withTypedRules.typedRules.get(0).enabled);
    Assert.assertFalse(withTypedRules.typedRules.get(0).matchCaseSensitive);
    Assert.assertTrue(withTypedRules.typedRules.get(0).matchWholeWord);
    Assert.assertTrue(withTypedRules.typedRules.get(1).autoApply);
    Assert.assertFalse(withTypedRules.typedRules.get(1).enabled);
    Assert.assertTrue(withTypedRules.typedRules.get(1).matchCaseSensitive);
    Assert.assertFalse(withTypedRules.typedRules.get(1).matchWholeWord);
  }

  @Test
  public void testSafeTogglesCanBePersisted() {
    final ContextProfilesStore store = createStore();
    final ContextProfilesStore.Preset created = store.createPreset("Preset");
    store.setSafeToggles(created.id, new ContextProfilesStore.SafeToggles(true, true, true, true));

    final ContextProfilesStore.Preset updated = store.getPreset(created.id);
    Assert.assertNotNull(updated);
    Assert.assertTrue(updated.safeToggles.disableContactsDictionary);
    Assert.assertTrue(updated.safeToggles.disableUserDictionary);
    Assert.assertTrue(updated.safeToggles.disableQuickFixes);
    Assert.assertTrue(updated.safeToggles.disableNextWordSuggestions);
  }

  @Test
  public void testBindingsAreClearedWhenPresetIsDeleted() {
    final ContextProfilesStore store = createStore();

    final ContextProfilesStore.Preset preset = store.createPreset("Bound");
    final String packageName = "com.example.app";

    store.bindAppToPreset(packageName, ContextFieldSelector.TEXT, preset.id);
    Assert.assertEquals(preset.id, store.getBoundPresetId(packageName, ContextFieldSelector.TEXT));

    store.deletePreset(preset.id);
    Assert.assertNull(store.getBoundPresetId(packageName, ContextFieldSelector.TEXT));
  }

  @Test
  public void testEnabledToggleAndListBindings() {
    final ContextProfilesStore store = createStore();
    Assert.assertFalse(store.isEnabled());
    store.setEnabled(true);
    Assert.assertTrue(store.isEnabled());

    final ContextProfilesStore.Preset preset = store.createPreset("Preset");
    store.bindAppToPreset("com.example.app", ContextFieldSelector.TEXT, preset.id);
    store.bindAppToPreset("com.example.app", ContextFieldSelector.EMAIL, preset.id);

    final var bindings = store.listBindings();
    Assert.assertEquals(2, bindings.size());
    Assert.assertEquals("com.example.app", bindings.get(0).packageName);
    Assert.assertEquals(ContextFieldSelector.EMAIL, bindings.get(0).selector);
    Assert.assertEquals(preset.id, bindings.get(0).presetId);
    Assert.assertEquals(ContextFieldSelector.TEXT, bindings.get(1).selector);

    store.clearAllBindings();
    Assert.assertTrue(store.listBindings().isEmpty());
  }

  @Test
  public void testWordListGenerationBumpsAndClearsOnDelete() {
    final ContextProfilesStore store = createStore();
    final ContextProfilesStore.Preset preset = store.createPreset("Preset");
    Assert.assertEquals(0L, store.getWordListGeneration(preset.id));

    store.bumpWordListGeneration(preset.id);
    Assert.assertEquals(1L, store.getWordListGeneration(preset.id));

    store.bumpWordListGeneration(preset.id);
    Assert.assertEquals(2L, store.getWordListGeneration(preset.id));

    store.deletePreset(preset.id);
    Assert.assertEquals(0L, store.getWordListGeneration(preset.id));
  }

  @Test
  public void testPresetLimitIsEnforced() {
    final ContextProfilesStore store = createStore();
    for (int i = 0; i < ContextProfilesStore.MAX_PRESETS; i++) {
      store.createPreset("Preset " + i);
    }

    try {
      store.createPreset("Preset overflow");
      Assert.fail("Expected preset creation to fail at MAX_PRESETS");
    } catch (IllegalStateException expected) {
      // OK
    }
  }

  @Test
  public void testBindingLimitIsEnforced() {
    final ContextProfilesStore store = createStore();
    final ContextProfilesStore.Preset preset = store.createPreset("Preset");
    for (int i = 0; i < ContextProfilesStore.MAX_APP_BINDINGS; i++) {
      store.bindAppToPreset("com.example.app" + i, ContextFieldSelector.TEXT, preset.id);
    }

    try {
      store.bindAppToPreset("com.example.overflow", ContextFieldSelector.TEXT, preset.id);
      Assert.fail("Expected binding creation to fail at MAX_APP_BINDINGS");
    } catch (IllegalStateException expected) {
      // OK
    }
  }

  @Test
  public void testRulesAreCappedOnWrite() {
    final ContextProfilesStore store = createStore();
    final ContextProfilesStore.Preset preset = store.createPreset("Preset");

    final List<ContextProfilesStore.VoiceRule> voiceRules = new ArrayList<>();
    for (int i = 0; i < ContextProfilesStore.MAX_VOICE_RULES_PER_PRESET + 5; i++) {
      voiceRules.add(new ContextProfilesStore.VoiceRule("voice " + i, "r" + i, false));
    }
    store.setVoiceRules(preset.id, voiceRules);

    final List<ContextProfilesStore.TypedRule> typedRules = new ArrayList<>();
    for (int i = 0; i < ContextProfilesStore.MAX_TYPED_RULES_PER_PRESET + 5; i++) {
      typedRules.add(new ContextProfilesStore.TypedRule("typed " + i, "r" + i, false, true));
    }
    store.setTypedRules(preset.id, typedRules);

    final ContextProfilesStore.Preset updated = store.getPreset(preset.id);
    Assert.assertNotNull(updated);
    Assert.assertEquals(ContextProfilesStore.MAX_VOICE_RULES_PER_PRESET, updated.voiceRules.size());
    Assert.assertEquals(ContextProfilesStore.MAX_TYPED_RULES_PER_PRESET, updated.typedRules.size());
  }
}
