package wtf.uhoh.newsoftkeyboard.app.ime;

import java.util.List;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class AddToDictionaryHintControllerTest {

  @Test
  public void testDoesNotShowAddHintForNextWordSuggestions() {
    final CandidateView candidateView = Mockito.mock(CandidateView.class);
    final Suggest suggest = Mockito.mock(Suggest.class);
    Mockito.when(suggest.isIncognitoMode()).thenReturn(false);
    Mockito.when(suggest.isValidWord(Mockito.any())).thenReturn(false);
    Mockito.when(suggest.getNextSuggestions(Mockito.any(), Mockito.anyBoolean()))
        .thenReturn(List.of("next"));

    final KeyboardDefinition keyboardDefinition = Mockito.mock(KeyboardDefinition.class);
    Mockito.when(keyboardDefinition.getLocale()).thenReturn(Locale.US);

    final boolean[] setSuggestionsCalled = {false};
    final AddToDictionaryHintController controller =
        new AddToDictionaryHintController(
            new AddToDictionaryHintController.Host() {
              @Override
              public CandidateView candidateView() {
                return candidateView;
              }

              @Override
              public Suggest suggest() {
                return suggest;
              }

              @Override
              public KeyboardDefinition currentAlphabetKeyboard() {
                return keyboardDefinition;
              }

              @Override
              public void setSuggestions(List<CharSequence> suggestions, int highlightedIndex) {
                setSuggestionsCalled[0] = true;
                Assert.assertEquals(List.of("next"), suggestions);
                Assert.assertEquals(-1, highlightedIndex);
              }
            });

    // Empty typed word (between words) but picked an invalid suggestion at index 0.
    final WordComposer emptyTyped = new WordComposer();
    controller.handlePostPick(
        0,
        /* pickedIndex= */ true,
        /* showSuggestions= */ false,
        /* justAutoAddedWord= */ false,
        /* isTagsSearchState= */ false,
        /* inAllUpperCaseState= */ "wut",
        /* suggestion= */ emptyTyped
        /* typedWord= */ );

    Assert.assertTrue(setSuggestionsCalled[0]);
    Mockito.verify(candidateView, Mockito.never()).showAddToDictionaryHint(Mockito.any());
  }
}
