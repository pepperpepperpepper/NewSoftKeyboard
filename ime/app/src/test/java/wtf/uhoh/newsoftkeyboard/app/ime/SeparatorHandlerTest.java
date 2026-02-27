package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anysoftkeyboard.api.KeyCodes;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class SeparatorHandlerTest {

  @Test
  public void spaceWhileNotComposingDoesNotClobberNextWordSuggestions() {
    final SeparatorHandler handler = new SeparatorHandler();
    final SeparatorHandler.Host host = mock(SeparatorHandler.Host.class);
    final KeyboardDefinition keyboard = mock(KeyboardDefinition.class);
    when(keyboard.isLeftToRightLanguage()).thenReturn(true);
    when(host.currentAlphabetKeyboard()).thenReturn(keyboard);

    when(host.isCurrentlyPredicting()).thenReturn(false);
    when(host.isSentenceSeparator(anyInt())).thenReturn(false);
    when(host.isAutoCorrect()).thenReturn(false);
    when(host.isDoubleSpaceChangesToPeriod()).thenReturn(false);
    when(host.multiTapTimeout()).thenReturn(0);
    when(host.spaceTimeTracker()).thenReturn(new SpaceTimeTracker());
    when(host.separatorOutputHandler()).thenReturn(new SeparatorOutputHandler());
    when(host.inputConnectionRouter()).thenReturn(new InputConnectionRouter(() -> null));
    when(host.prepareWordComposerForNextWord()).thenReturn(new WordComposer());
    when(host.isSpaceSwapCharacter(anyInt())).thenReturn(false);
    when(host.isInAllUpperCaseState()).thenReturn(false);
    when(host.lastCommittedWordForNextSuggestions()).thenReturn("");

    handler.handleSeparator(KeyCodes.SPACE, host);

    verify(host, never()).performUpdateSuggestions();
    verify(host, never()).setSuggestions(any(), anyInt());
    verify(host, never()).suggest();
  }

  @Test
  public void spaceWhileNotComposingRequestsNextSuggestionsWhenLastCommittedWordKnown() {
    final SeparatorHandler handler = new SeparatorHandler();
    final SeparatorHandler.Host host = mock(SeparatorHandler.Host.class);
    final KeyboardDefinition keyboard = mock(KeyboardDefinition.class);
    when(keyboard.isLeftToRightLanguage()).thenReturn(true);
    when(host.currentAlphabetKeyboard()).thenReturn(keyboard);

    when(host.isCurrentlyPredicting()).thenReturn(false);
    when(host.isSentenceSeparator(anyInt())).thenReturn(false);
    when(host.isAutoCorrect()).thenReturn(false);
    when(host.isDoubleSpaceChangesToPeriod()).thenReturn(false);
    when(host.multiTapTimeout()).thenReturn(0);
    when(host.spaceTimeTracker()).thenReturn(new SpaceTimeTracker());
    when(host.separatorOutputHandler()).thenReturn(new SeparatorOutputHandler());
    when(host.inputConnectionRouter()).thenReturn(new InputConnectionRouter(() -> null));
    when(host.prepareWordComposerForNextWord()).thenReturn(new WordComposer());
    when(host.isSpaceSwapCharacter(anyInt())).thenReturn(false);
    when(host.isInAllUpperCaseState()).thenReturn(false);
    when(host.lastCommittedWordForNextSuggestions()).thenReturn("keep");

    final Suggest suggest = mock(Suggest.class);
    final List<CharSequence> next = List.of("me", "going");
    when(suggest.getNextSuggestions(eq("keep"), anyBoolean())).thenReturn(next);
    when(host.suggest()).thenReturn(suggest);

    handler.handleSeparator(KeyCodes.SPACE, host);

    verify(host, never()).performUpdateSuggestions();
    verify(host).setSuggestions(eq(next), eq(-1));
  }

  @Test
  public void spaceWhileComposingRequestsNextSuggestions() {
    final SeparatorHandler handler = new SeparatorHandler();
    final SeparatorHandler.Host host = mock(SeparatorHandler.Host.class);
    final KeyboardDefinition keyboard = mock(KeyboardDefinition.class);
    when(keyboard.isLeftToRightLanguage()).thenReturn(true);
    when(host.currentAlphabetKeyboard()).thenReturn(keyboard);

    when(host.isCurrentlyPredicting()).thenReturn(true);
    when(host.isSentenceSeparator(anyInt())).thenReturn(false);
    when(host.isAutoCorrect()).thenReturn(false);
    when(host.isDoubleSpaceChangesToPeriod()).thenReturn(false);
    when(host.multiTapTimeout()).thenReturn(0);
    when(host.spaceTimeTracker()).thenReturn(new SpaceTimeTracker());
    when(host.separatorOutputHandler()).thenReturn(new SeparatorOutputHandler());
    when(host.inputConnectionRouter()).thenReturn(new InputConnectionRouter(() -> null));
    when(host.isSpaceSwapCharacter(anyInt())).thenReturn(false);
    when(host.isInAllUpperCaseState()).thenReturn(false);
    when(host.lastCommittedWordForNextSuggestions()).thenReturn("");

    final WordComposer typedWord = new WordComposer();
    typedWord.simulateTypedWord("hello");
    when(host.prepareWordComposerForNextWord()).thenReturn(typedWord);

    final Suggest suggest = mock(Suggest.class);
    final List<CharSequence> next = List.of("world", "there");
    when(suggest.getNextSuggestions(eq("hello"), anyBoolean())).thenReturn(next);
    when(host.suggest()).thenReturn(suggest);

    handler.handleSeparator(KeyCodes.SPACE, host);

    verify(host).performUpdateSuggestions();
    verify(host).setSuggestions(eq(next), eq(-1));
  }
}
