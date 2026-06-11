package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceMuteMainDictionaryContextProfileTest extends ImeServiceBaseTest {

  @Test
  public void testMutedMainDictionarySuppressesSuggestionsButKeepsValidity() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Muted");
    store.setSafeToggles(
        preset.id,
        new ContextProfilesStore.SafeToggles(false, false, false, false, true));
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());
      mImeServiceUnderTest.resetMockCandidateView();

      // language-dictionary completions (hell/hello) are muted by the profile; "he'll"
      // arrives via the quick-fix table, which has its own separate toggle
      mImeServiceUnderTest.simulateTextTyping("hel");
      verifySuggestions(true, "hel", "he'll");

      // but language words stay valid: completing to "hell" + space must not auto-correct
      mImeServiceUnderTest.simulateTextTyping("l ");
      Assert.assertEquals(
          "hell ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }

  @Test
  public void testUnmutedProfileKeepsMainDictionarySuggestions() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("NotMuted");
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());
      mImeServiceUnderTest.resetMockCandidateView();

      mImeServiceUnderTest.simulateTextTyping("hel");
      verifySuggestions(true, "hel", "he'll", "hello", "hell");
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }
}
