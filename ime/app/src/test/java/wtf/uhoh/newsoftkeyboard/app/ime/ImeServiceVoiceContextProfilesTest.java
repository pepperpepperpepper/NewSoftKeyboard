package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceVoiceContextProfilesTest extends ImeServiceBaseTest {

  @Test
  public void testShowsAndAppliesVoiceRuleSuggestion() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Voice");
    store.setVoiceRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.VoiceRule("how are you?", "hru?", false)));
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());
      mImeServiceUnderTest.resetMockCandidateView();

      final String formattedText = " how are you? ";
      final String toCommit = mImeServiceUnderTest.onVoiceTextPreCommit(formattedText);
      getCurrentTestInputConnection().commitText(toCommit, 1);
      TestRxSchedulers.drainAllTasks();

      verifySuggestions(true, "hru?");

      mImeServiceUnderTest.pickSuggestionManually(0, "hru?");
      TestRxSchedulers.drainAllTasks();

      Assert.assertEquals(
          " hru? ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }
}
