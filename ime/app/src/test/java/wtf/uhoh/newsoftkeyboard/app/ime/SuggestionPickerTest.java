package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class SuggestionPickerTest {

  @Test
  public void pickingNextWordSuggestionDoesNotLearnPartialTypedWord() {
    final WordComposer typed = new WordComposer();
    typed.simulateTypedWord("m");

    final FakeHost host = new FakeHost();
    final SuggestionPicker picker = new SuggestionPicker(host);

    picker.pickSuggestionManually(
        typed,
        false /* autoSpaceEnabled */,
        0 /* index */,
        "me" /* suggestion */,
        true /* showSuggestions */,
        false /* justAutoAddedWord */,
        false /* isTagsSearchState */);

    assertFalse(host.checkAddCalled);
  }

  @Test
  public void pickingTypedWordLearnsIt() {
    final WordComposer typed = new WordComposer();
    typed.simulateTypedWord("keep");

    final FakeHost host = new FakeHost();
    final SuggestionPicker picker = new SuggestionPicker(host);

    picker.pickSuggestionManually(
        typed,
        false /* autoSpaceEnabled */,
        0 /* index */,
        "keep" /* suggestion */,
        true /* showSuggestions */,
        false /* justAutoAddedWord */,
        false /* isTagsSearchState */);

    assertTrue(host.checkAddCalled);
  }

  @Test
  public void autoSpaceFallsBackToKeyEventsWhenCommitTextSpaceFails() {
    final InputConnection inputConnection = mock(InputConnection.class);
    when(inputConnection.beginBatchEdit()).thenReturn(true);
    when(inputConnection.endBatchEdit()).thenReturn(true);
    when(inputConnection.getTextBeforeCursor(eq(1), eq(0))).thenReturn("e");
    when(inputConnection.commitText(eq(" "), eq(1))).thenReturn(false);
    when(inputConnection.sendKeyEvent(any())).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    final WordComposer typed = new WordComposer();
    final FakeHost host = new FakeHost(router);
    final SuggestionPicker picker = new SuggestionPicker(host);

    picker.pickSuggestionManually(
        typed,
        true /* autoSpaceEnabled */,
        0 /* index */,
        "me" /* suggestion */,
        true /* showSuggestions */,
        false /* justAutoAddedWord */,
        false /* isTagsSearchState */);

    verify(inputConnection).commitText(eq(" "), eq(1));

    final ArgumentCaptor<KeyEvent> events = ArgumentCaptor.forClass(KeyEvent.class);
    verify(inputConnection, times(2)).sendKeyEvent(events.capture());
    final List<KeyEvent> sent = events.getAllValues();
    assertTrue(sent.get(0).getAction() == KeyEvent.ACTION_DOWN);
    assertTrue(sent.get(0).getKeyCode() == KeyEvent.KEYCODE_SPACE);
    assertTrue(sent.get(1).getAction() == KeyEvent.ACTION_UP);
    assertTrue(sent.get(1).getKeyCode() == KeyEvent.KEYCODE_SPACE);
  }

  @Test
  public void autoSpaceFallsBackToKeyEventsWhenCommitTextSpaceReturnsTrueButNoSpaceVisible() {
    final InputConnection inputConnection = mock(InputConnection.class);
    when(inputConnection.beginBatchEdit()).thenReturn(true);
    when(inputConnection.endBatchEdit()).thenReturn(true);
    when(inputConnection.commitText(eq(" "), eq(1))).thenReturn(true);
    when(inputConnection.getTextBeforeCursor(eq(1), eq(0))).thenReturn("x");
    when(inputConnection.sendKeyEvent(any())).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    final WordComposer typed = new WordComposer();
    final FakeHost host = new FakeHost(router);
    final SuggestionPicker picker = new SuggestionPicker(host);

    picker.pickSuggestionManually(
        typed,
        true /* autoSpaceEnabled */,
        0 /* index */,
        "me" /* suggestion */,
        true /* showSuggestions */,
        false /* justAutoAddedWord */,
        false /* isTagsSearchState */);

    verify(inputConnection).commitText(eq(" "), eq(1));

    final ArgumentCaptor<KeyEvent> events = ArgumentCaptor.forClass(KeyEvent.class);
    verify(inputConnection, times(2)).sendKeyEvent(events.capture());
    final List<KeyEvent> sent = events.getAllValues();
    assertTrue(sent.get(0).getAction() == KeyEvent.ACTION_DOWN);
    assertTrue(sent.get(0).getKeyCode() == KeyEvent.KEYCODE_SPACE);
    assertTrue(sent.get(1).getAction() == KeyEvent.ACTION_UP);
    assertTrue(sent.get(1).getKeyCode() == KeyEvent.KEYCODE_SPACE);
  }

  @Test
  public void autoSpaceDoesNotSendKeyEventsWhenReadbackNotSupportedButCommitTextSucceeds() {
    final InputConnection inputConnection = mock(InputConnection.class);
    when(inputConnection.beginBatchEdit()).thenReturn(true);
    when(inputConnection.endBatchEdit()).thenReturn(true);
    when(inputConnection.commitText(eq(" "), eq(1))).thenReturn(true);
    when(inputConnection.getTextBeforeCursor(eq(1), eq(0))).thenReturn(null);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    final WordComposer typed = new WordComposer();
    final FakeHost host = new FakeHost(router);
    final SuggestionPicker picker = new SuggestionPicker(host);

    picker.pickSuggestionManually(
        typed,
        true /* autoSpaceEnabled */,
        0 /* index */,
        "me" /* suggestion */,
        true /* showSuggestions */,
        false /* justAutoAddedWord */,
        false /* isTagsSearchState */);

    verify(inputConnection).commitText(eq(" "), eq(1));
    verify(inputConnection, times(0)).sendKeyEvent(any());
  }

  private static final class FakeHost implements SuggestionPicker.Host {
    boolean checkAddCalled;
    final Suggest suggest;
    final KeyboardDefinition keyboard;
    final AddToDictionaryHintController controller;
    final InputConnectionRouter router;

    FakeHost() {
      this(new InputConnectionRouter(() -> null));
    }

    FakeHost(InputConnectionRouter router) {
      this.router = router;
      suggest = mock(Suggest.class);
      when(suggest.isValidWord(any())).thenReturn(true);
      when(suggest.getNextSuggestions(any(), anyBoolean())).thenReturn(List.of());

      keyboard = mock(KeyboardDefinition.class);
      when(keyboard.getLocale()).thenReturn(Locale.ENGLISH);

      controller =
          new AddToDictionaryHintController(
              new AddToDictionaryHintController.Host() {
                @Override
                public CandidateView candidateView() {
                  return null;
                }

                @Override
                public Suggest suggest() {
                  return FakeHost.this.suggest;
                }

                @Override
                public KeyboardDefinition currentAlphabetKeyboard() {
                  return FakeHost.this.keyboard;
                }

                @Override
                public void setSuggestions(List<CharSequence> suggestions, int highlightedIndex) {}
              });
    }

    @Override
    public InputConnectionRouter inputConnectionRouter() {
      return router;
    }

    @Override
    public WordComposer prepareWordComposerForNextWord() {
      return new WordComposer();
    }

    @Override
    public void checkAddToDictionaryWithAutoDictionary(
        CharSequence newWord, Suggest.AdditionType type) {
      checkAddCalled = true;
    }

    @Override
    public void setSuggestions(java.util.List<CharSequence> suggestions, int highlightedIndex) {}

    @Override
    public Suggest getSuggest() {
      return suggest;
    }

    @Override
    public CandidateView getCandidateView() {
      return null;
    }

    @Override
    public boolean tryCommitCompletion(
        int index, InputConnectionRouter inputConnectionRouter, CandidateView candidateView) {
      return false;
    }

    @Override
    public KeyboardDefinition getCurrentAlphabetKeyboard() {
      return keyboard;
    }

    @Override
    public void clearSuggestions() {}

    @Override
    public void commitWordToInput(CharSequence wordToCommit, CharSequence typedWord) {}

    @Override
    public void commitManuallyPickedWordToInput(
        CharSequence wordToCommit, CharSequence typedWordInEditor) {}

    @Override
    public void setSpaceTimeStamp(boolean isSpace) {}

    @Override
    public boolean isPredictionOn() {
      return true;
    }

    @Override
    public boolean isAutoCompleteEnabled() {
      return false;
    }

    @Override
    public boolean isInAllUpperCaseState() {
      return false;
    }

    @Override
    public AddToDictionaryHintController addToDictionaryHintController() {
      return controller;
    }
  }
}
