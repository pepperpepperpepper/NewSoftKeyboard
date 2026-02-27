package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.KeyEvent;
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

    void commitManuallyPickedWordToInput(CharSequence wordToCommit, CharSequence typedWordInEditor);

    void setSpaceTimeStamp(boolean isSpace);

    boolean isPredictionOn();

    boolean isAutoCompleteEnabled();

    /** Returns true when the keyboard is in an ALL-CAPS mode (caps-lock). */
    boolean isInAllUpperCaseState();

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

      host.commitManuallyPickedWordToInput(suggestion, typedWord.getTypedWord());

      if (autoSpaceEnabled && (index == 0 || !typedWord.isAtTagsSearchState())) {
        boolean needsSpaceFallback = !inputConnectionRouter.commitText(" ", 1);
        if (!needsSpaceFallback) {
          final CharSequence beforeCursor = inputConnectionRouter.getTextBeforeCursor(1, 0);
          if (beforeCursor != null && !java.util.Objects.equals(beforeCursor, " ")) {
            needsSpaceFallback = true;
          }
        }
        if (needsSpaceFallback) {
          // Some editors (e.g., some rich text wrappers) may ignore commitText(" ") but still
          // respond to key events for SPACE. Fall back to key events so users can chain
          // suggestions into sentences.
          final boolean downOk =
              inputConnectionRouter.sendKeyEvent(
                  new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE));
          final boolean upOk =
              inputConnectionRouter.sendKeyEvent(
                  new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE));
          if (!(downOk && upOk)) {
            inputConnectionRouter.commitText(" ", 1);
          }
        }
        host.setSpaceTimeStamp(true);
      }

      if (!typedWord.isAtTagsSearchState()) {
        if (TextUtils.equals(suggestion, typedWord.getTypedWord())) {
          host.checkAddToDictionaryWithAutoDictionary(
              typedWord.getTypedWord(), SuggestImpl.AdditionType.Picked);
        }

        host.addToDictionaryHintController()
            .handlePostPick(
                index,
                showSuggestions,
                justAutoAddedWord,
                typedWord.isAtTagsSearchState(),
                host.isInAllUpperCaseState(),
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
