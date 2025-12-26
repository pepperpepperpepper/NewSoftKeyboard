package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.Build;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickKeyHistoryRecords;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickTextKeyFactory;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.TagsExtractorImpl;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1 /*the first API level to have support for those*/)
public class ImeServiceKeyboardTagsSearcherTest extends ImeServiceBaseTest {

  @Before
  public void setUpTagsLoad() {
    wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers.backgroundFlushAllJobs();
    TestRxSchedulers.foregroundFlushAllJobs();
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  public void testDefaultFalseBeforeAPI22() {
    Assert.assertSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());
    Assert.assertFalse(mImeServiceUnderTest.getQuickTextTagsSearcher().isEnabled());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
  public void testDefaultTrueAtAPI22() {
    Assert.assertNotNull(mImeServiceUnderTest.getQuickTextTagsSearcher());
    Assert.assertNotSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());
    Assert.assertTrue(mImeServiceUnderTest.getQuickTextTagsSearcher().isEnabled());
  }

  @Test
  public void testOnSharedPreferenceChangedCauseLoading() throws Exception {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
    Assert.assertSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, true);
    Object searcher = mImeServiceUnderTest.getQuickTextTagsSearcher();
    Assert.assertNotSame(TagsExtractorImpl.NO_OP, searcher);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, true);
    Assert.assertSame(searcher, mImeServiceUnderTest.getQuickTextTagsSearcher());
  }

  @Test
  public void testUnrelatedOnSharedPreferenceChangedDoesNotCreateSearcher() throws Exception {
    Object searcher = mImeServiceUnderTest.getQuickTextTagsSearcher();
    Assert.assertNotNull(searcher);
    // unrelated pref change, should not create a new searcher
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, false);
    Assert.assertSame(searcher, mImeServiceUnderTest.getQuickTextTagsSearcher());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
    Assert.assertSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    Assert.assertSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());
  }

  @Test
  public void testEnabledTypingTagProvidesSuggestionsFromTagsOnly() throws Exception {
    mImeServiceUnderTest.simulateKeyPress(':');
    verifySuggestions(
        true,
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER,
        QuickKeyHistoryRecords.DEFAULT_EMOJI);
    mImeServiceUnderTest.simulateTextTyping("fa");
    List suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertEquals(134, suggestions.size());
    Assert.assertEquals(
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "fa", suggestions.get(0));

    // now checking that suggestions will work without colon
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateTextTyping("fa");
    verifySuggestions(true, "fa", "face");
  }

  @Test
  public void testDeleteLetters() throws Exception {
    mImeServiceUnderTest.simulateKeyPress(':');
    verifySuggestions(
        true,
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER,
        QuickKeyHistoryRecords.DEFAULT_EMOJI);
    mImeServiceUnderTest.simulateTextTyping("fa");
    List suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertEquals(134, suggestions.size());
    Assert.assertEquals(
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "fa", suggestions.get(0));
    Assert.assertEquals("⏩", suggestions.get(1));

    mImeServiceUnderTest.simulateKeyPress('c');
    suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertEquals(132, suggestions.size());
    Assert.assertEquals(
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "fac", suggestions.get(0));
    Assert.assertEquals("☠", suggestions.get(1));

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertEquals(134, suggestions.size());
    Assert.assertEquals(
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "fa", suggestions.get(0));
    Assert.assertEquals("⏩", suggestions.get(1));

    mImeServiceUnderTest.simulateKeyPress('c');
    suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertEquals(132, suggestions.size());
    Assert.assertEquals(
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "fac", suggestions.get(0));
    Assert.assertEquals("☠", suggestions.get(1));
  }

  @Test
  public void testOnlyTagsAreSuggestedWhenTypingColon() throws Exception {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateKeyPress(':');
    verifySuggestions(
        true,
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER,
        QuickKeyHistoryRecords.DEFAULT_EMOJI);
    mImeServiceUnderTest.simulateTextTyping("face");
    List suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertNotNull(suggestions);
    Assert.assertEquals(131, suggestions.size());
    Assert.assertEquals(
        ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "face", suggestions.get(0));
    Assert.assertEquals("☠", suggestions.get(1));
  }

  @Test
  public void testTagsSearchDoesNotAutoPick() throws Exception {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(":face");

    mImeServiceUnderTest.simulateKeyPress(' ');

    Assert.assertEquals(":face ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testTagsSearchThrice() throws Exception {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertNotNull(suggestions);
    Assert.assertEquals(131, suggestions.size());

    mImeServiceUnderTest.simulateKeyPress(' ');

    mImeServiceUnderTest.simulateTextTyping(":face");
    suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertNotNull(suggestions);
    Assert.assertEquals(131, suggestions.size());

    mImeServiceUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

    mImeServiceUnderTest.simulateTextTyping(":face");
    suggestions = verifyAndCaptureSuggestion(true);
    Assert.assertNotNull(suggestions);
    Assert.assertEquals(131, suggestions.size());
  }

  @Test
  public void testPickingEmojiOutputsToInput() throws Exception {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(":face");

    mImeServiceUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

    verifySuggestions(true);
    Assert.assertEquals("\uD83D\uDE00", mImeServiceUnderTest.getCurrentInputConnectionText());

    // deleting

    // correctly, this is a bug with TestInputConnection: it reports that there is one character
    // in the input
    // but that's because it does not support deleting multi-character emojis.
    Assert.assertEquals(2, mImeServiceUnderTest.getCurrentInputConnectionText().length());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    // so, it was two characters, and now it's one
    Assert.assertEquals(1, mImeServiceUnderTest.getCurrentInputConnectionText().length());
  }

  @Test
  public void testPickingEmojiStoresInHistory() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    mImeServiceUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

    List<QuickKeyHistoryRecords.HistoryKey> keys =
        mImeServiceUnderTest.getQuickKeyHistoryRecords().getCurrentHistory();
    Assert.assertEquals(2, keys.size());
    // added last (this will be shown in reverse on the history tab)
    Assert.assertEquals("\uD83D\uDE00", keys.get(1).name);
    Assert.assertEquals("\uD83D\uDE00", keys.get(1).value);
  }

  @Test
  public void testPickingEmojiDoesNotTryToGetNextWords() throws Exception {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(":face");

    Mockito.reset(mImeServiceUnderTest.getSuggest());
    mImeServiceUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .getNextSuggestions(Mockito.any(CharSequence.class), Mockito.anyBoolean());
  }

  @Test
  public void testPickingTypedTagDoesNotTryToAddToAutoDictionary() throws Exception {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(":face");

    Mockito.reset(mImeServiceUnderTest.getSuggest());
    mImeServiceUnderTest.pickSuggestionManually(0, ":face");

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .isValidWord(Mockito.any(CharSequence.class));
  }

  @Test
  public void testPickingSearchCellInSuggestionsOutputTypedWord() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");

    mImeServiceUnderTest.pickSuggestionManually(
        0, ImeKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER + "face");

    // outputs the typed word
    Assert.assertEquals(":face ", mImeServiceUnderTest.getCurrentInputConnectionText());
    // clears suggestions
    verifySuggestions(true);
  }

  @Test
  public void testDisabledTypingTagDoesNotProvidesSuggestions() throws Exception {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
    mImeServiceUnderTest.simulateKeyPress(':');
    verifySuggestions(true);
    mImeServiceUnderTest.simulateTextTyping("fa");
    verifySuggestions(true, "fa", "face");
  }

  @Test
  public void testQuickTextEnabledPluginsPrefsChangedCauseReload() throws Exception {
    Object searcher = mImeServiceUnderTest.getQuickTextTagsSearcher();
    SharedPrefsHelper.setPrefsValue(QuickTextKeyFactory.PREF_ID_PREFIX + "jksdbc", "sdfsdfsd");
    Assert.assertNotSame(searcher, mImeServiceUnderTest.getQuickTextTagsSearcher());
  }

  @Test
  public void testQuickTextEnabledPluginsPrefsChangedDoesNotCauseReloadIfTagsSearchIsDisabled()
      throws Exception {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
    Assert.assertSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());
    SharedPrefsHelper.setPrefsValue(QuickTextKeyFactory.PREF_ID_PREFIX + "ddddd", "sdfsdfsd");

    Assert.assertSame(TagsExtractorImpl.NO_OP, mImeServiceUnderTest.getQuickTextTagsSearcher());
  }

  @Test
  public void testEnsureSuggestionsAreIterable() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    int suggestionsCount = suggestions.size();
    for (Object suggestion : suggestions) {
      Assert.assertNotNull(suggestion);
      Assert.assertTrue(suggestion instanceof CharSequence);
      suggestionsCount--;
    }
    Assert.assertEquals(0, suggestionsCount);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoveIteratorUnSupported() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    suggestions.iterator().remove();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testAddingAtIndexToSuggestionsUnSupported() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    suggestions.add(0, "demo");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testAddingToSuggestionsUnSupported() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    suggestions.add("demo");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testListIteratorUnSupported() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    suggestions.listIterator();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoteAtIndexUnSupported() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    suggestions.remove(0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoteObjectUnSupported() throws Exception {
    mImeServiceUnderTest.simulateTextTyping(":face");
    List suggestions = verifyAndCaptureSuggestion(true);
    suggestions.remove("DEMO");
  }
}
