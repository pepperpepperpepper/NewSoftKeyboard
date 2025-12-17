package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.CallSuper;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.SuggestImpl;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.views.CandidateView;

/**
 * Encapsulates manual pick handling from the suggestions strip.
 */
public final class SuggestionPicker {

  public interface Host {
    InputConnection currentInputConnection();

    WordComposer prepareWordComposerForNextWord();

    void checkAddToDictionaryWithAutoDictionary(CharSequence newWord, Suggest.AdditionType type);

    void setSuggestions(java.util.List<CharSequence> suggestions, int highlightedIndex);

    Suggest getSuggest();

    CandidateView getCandidateView();

    boolean tryCommitCompletion(int index, InputConnection ic, CandidateView candidateView);

    AnyKeyboard getCurrentAlphabetKeyboard();

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

    InputConnection ic = host.currentInputConnection();
    if (ic != null) {
      ic.beginBatchEdit();
    }

    try {
      if (tryCommitCompletion(index, ic, host.getCandidateView())) {
        return;
      }

      host.commitWordToInput(
          suggestion,
          suggestion /* manual pick; not a correction */);

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
      if (ic != null) {
        ic.endBatchEdit();
      }
    }
  }

  private boolean tryCommitCompletion(int index, InputConnection ic, CandidateView candidateView) {
    return host.tryCommitCompletion(index, ic, candidateView);
  }
}
