package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.CallSuper;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.SuggestImpl;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.Locale;

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

        final boolean showAddHint =
            AddToDictionaryDecider.shouldShowAddHint(
                index,
                justAutoAddedWord,
                showSuggestions,
                host.getSuggest(),
                suggestion,
                host.getCurrentAlphabetKeyboard().getLocale());

        if (showAddHint) {
          final CandidateView cv = host.getCandidateView();
          if (cv != null) cv.showAddToDictionaryHint(suggestion);
        } else {
          host.setSuggestions(
              host.getSuggest().getNextSuggestions(suggestion, typedWord.isAllUpperCase()), -1);
        }
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
