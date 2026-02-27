package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import com.anysoftkeyboard.api.KeyCodes;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceTypedContextProfilesTest extends ImeServiceBaseTest {

  @Test
  public void testShowsAndAppliesTypedRuleSuggestionOnSeparator() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Typed");
    store.setTypedRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", false, true)));
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());
      mImeServiceUnderTest.resetMockCandidateView();
      Mockito.doReturn(Arrays.asList("nextOne", "nextTwo"))
          .when(mImeServiceUnderTest.suggest())
          .getNextSuggestions(Mockito.any(CharSequence.class), Mockito.anyBoolean());

      mImeServiceUnderTest.simulateTextTyping("how are you ");
      TestRxSchedulers.drainAllTasks();

      verifySuggestions(true, "hru?", "nextOne", "nextTwo");

      mImeServiceUnderTest.pickSuggestionManually(0, "hru?");
      TestRxSchedulers.drainAllTasks();

      Assert.assertEquals(
          "hru? ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }

  @Test
  public void testAutoAppliesTypedRulesBeforeEditorAction() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Typed");
    store.setTypedRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", true, true)));
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(
          true,
          TestableImeService.createEditorInfo(
              android.view.inputmethod.EditorInfo.IME_ACTION_SEND,
              android.view.inputmethod.EditorInfo.TYPE_CLASS_TEXT));
      mImeServiceUnderTest.resetMockCandidateView();

      mImeServiceUnderTest.simulateTextTyping("how are you");
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
      TestRxSchedulers.drainAllTasks();

      Assert.assertEquals(
          "hru?", getCurrentTestInputConnection().getCurrentTextInInputConnection());
      Assert.assertEquals(
          android.view.inputmethod.EditorInfo.IME_ACTION_SEND,
          getCurrentTestInputConnection().getLastEditorAction());
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }

  @Test
  public void testDoesNotAutoApplyTypedRulesInNoSuggestionsFieldsByDefault() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Typed");
    store.setTypedRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", true, true)));
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(
          true,
          TestableImeService.createEditorInfo(
              android.view.inputmethod.EditorInfo.IME_ACTION_SEND,
              android.view.inputmethod.EditorInfo.TYPE_CLASS_TEXT
                  | android.view.inputmethod.EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS));

      mImeServiceUnderTest.simulateTextTyping("how are you");
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
      TestRxSchedulers.drainAllTasks();

      Assert.assertEquals(
          "how are you", getCurrentTestInputConnection().getCurrentTextInInputConnection());
      Assert.assertEquals(
          android.view.inputmethod.EditorInfo.IME_ACTION_SEND,
          getCurrentTestInputConnection().getLastEditorAction());
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }

  @Test
  public void testAutoAppliesTypedRulesInNoSuggestionsFieldsWhenEnabledInPreset() {
    final ContextProfilesStore store = new ContextProfilesStore(getApplicationContext());
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Typed");
    store.setSecurityOptions(preset.id, preset.containsPersonalContent, true);
    store.setTypedRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", true, true)));
    store.bindAppToPreset(BuildConfig.APPLICATION_ID, ContextFieldSelector.TEXT, preset.id);

    try {
      simulateOnStartInputFlow(
          true,
          TestableImeService.createEditorInfo(
              android.view.inputmethod.EditorInfo.IME_ACTION_SEND,
              android.view.inputmethod.EditorInfo.TYPE_CLASS_TEXT
                  | android.view.inputmethod.EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS));

      mImeServiceUnderTest.simulateTextTyping("how are you");
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
      TestRxSchedulers.drainAllTasks();

      Assert.assertEquals(
          "hru?", getCurrentTestInputConnection().getCurrentTextInInputConnection());
      Assert.assertEquals(
          android.view.inputmethod.EditorInfo.IME_ACTION_SEND,
          getCurrentTestInputConnection().getLastEditorAction());
    } finally {
      store.deletePreset(preset.id);
      store.setEnabled(false);
    }
  }
}
