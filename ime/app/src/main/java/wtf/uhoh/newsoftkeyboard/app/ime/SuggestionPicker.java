package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.SuggestImpl;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Encapsulates manual pick handling from the suggestions strip. */
public final class SuggestionPicker {

  public interface Host {
    @NonNull
    InputConnectionRouter inputConnectionRouter();

    WordComposer prepareWordComposerForNextWord();

    void checkAddToDictionaryWithAutoDictionary(CharSequence newWord, Suggest.AdditionType type);

    void setSuggestions(java.util.List<CharSequence> suggestions, int highlightedIndex);

    Suggest getSuggest();

    CandidateView getCandidateView();

    boolean tryCommitCompletion(
        int index, InputConnectionRouter inputConnectionRouter, CandidateView candidateView);

    KeyboardDefinition getCurrentAlphabetKeyboard();

    void clearSuggestions();

    void commitWordToInput(CharSequence wordToCommit, CharSequence typedWord);

    void sendKeyChar(char c);

    void setSpaceTimeStamp(boolean isSpace);

    boolean isPredictionOn();

    boolean isAutoCompleteEnabled();

    AddToDictionaryHintController addToDictionaryHintController();
  }

  private final Host host;

  public SuggestionPicker(Host host) {
    this.host = host;
  }

  @CallSuper
  public void pickSuggestionManually(
      WordComposer typedWord,
      boolean autoSpaceEnabled,
      int index,
      CharSequence suggestion,
      boolean showSuggestions,
      boolean justAutoAddedWord,
      boolean isTagsSearchState) {

    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    inputConnectionRouter.beginBatchEdit();

    try {
      if (tryCommitCompletion(index, inputConnectionRouter, host.getCandidateView())) {
        return;
      }

      host.commitWordToInput(suggestion, suggestion /* manual pick; not a correction */);

      if (autoSpaceEnabled && (index == 0 || !typedWord.isAtTagsSearchState())) {
        host.sendKeyChar((char) com.anysoftkeyboard.api.KeyCodes.SPACE);
        host.setSpaceTimeStamp(true);
      }

      if (!typedWord.isAtTagsSearchState()) {
        if (index == 0) {
          host.checkAddToDictionaryWithAutoDictionary(
              typedWord.getTypedWord(), SuggestImpl.AdditionType.Picked);
        }

        host.addToDictionaryHintController()
            .handlePostPick(
                index,
                showSuggestions,
                justAutoAddedWord,
                typedWord.isAtTagsSearchState(),
                typedWord.isAllUpperCase(),
                suggestion,
                typedWord);
      }
    } finally {
      inputConnectionRouter.endBatchEdit();
    }
  }

  private boolean tryCommitCompletion(
      int index, InputConnectionRouter inputConnectionRouter, CandidateView candidateView) {
    return host.tryCommitCompletion(index, inputConnectionRouter, candidateView);
  }
}
