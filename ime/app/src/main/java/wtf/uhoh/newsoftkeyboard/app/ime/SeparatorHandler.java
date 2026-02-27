package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Handles separator key logic (space/punctuation/newline) and next-word suggestions. */
final class SeparatorHandler {

  private static final String TAG = "NextWordFlow";

  void handleSeparator(int primaryCode, @NonNull Host host) {
    if (!host.currentAlphabetKeyboard().isLeftToRightLanguage()) {
      if (primaryCode == (int) ')') primaryCode = (int) '(';
      else if (primaryCode == (int) '(') primaryCode = (int) ')';
    }

    final boolean wasPredicting = host.isCurrentlyPredicting();
    if (wasPredicting) {
      host.performUpdateSuggestions();
    }
    final boolean newLine = primaryCode == KeyCodes.ENTER;
    final boolean isSpace = primaryCode == KeyCodes.SPACE;
    final boolean isEndOfSentence = newLine || host.isSentenceSeparator(primaryCode);

    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    inputConnectionRouter.beginBatchEdit();

    final WordComposer typedWord = host.prepareWordComposerForNextWord();
    final boolean separatorInsideWord = typedWord.cursorPosition() < typedWord.charCount();

    SeparatorActionHelper.Result result =
        SeparatorActionHelper.handleSeparator(
            primaryCode,
            isSpace,
            newLine,
            isEndOfSentence,
            wasPredicting,
            separatorInsideWord,
            host.isAutoCorrect(),
            host.isDoubleSpaceChangesToPeriod(),
            host.multiTapTimeout(),
            typedWord,
            inputConnectionRouter,
            host.separatorOutputHandler(),
            host.spaceTimeTracker(),
            host::isSpaceSwapCharacter,
            host::commitWordToInput,
            host::checkAddToDictionaryWithAutoDictionary,
            () -> host.abortCorrectionAndResetPredictionState(false),
            host::setWordRevertLength,
            code -> host.sendKeyChar((char) code));

    host.markExpectingSelectionUpdate();
    inputConnectionRouter.endBatchEdit();

    if (result.endOfSentence) {
      host.suggest().resetNextWordSentence();
      host.clearLastCommittedWordForNextSuggestions();
      host.clearSuggestions();
    } else {
      final boolean usedFallback =
          result.wordForNextSuggestions.length() == 0
              && host.lastCommittedWordForNextSuggestions().length() > 0;
      final CharSequence nextSuggestionsWord =
          result.wordForNextSuggestions.length() == 0
              ? host.lastCommittedWordForNextSuggestions()
              : result.wordForNextSuggestions;

      if (nextSuggestionsWord.length() == 0) {
        // A user-pressed separator while not composing a word (for example: a leading space).
        // Do not clobber existing next-word suggestions with an empty request.
        Logger.d(TAG, "Next-word request (separator): empty (code=%d). Skipping.", primaryCode);
      } else {
        Logger.d(
            TAG,
            "Next-word request (separator): code=%d word='%s'%s",
            primaryCode,
            nextSuggestionsWord,
            usedFallback ? " (fallback)" : "");
        host.setSuggestions(
            host.suggest().getNextSuggestions(nextSuggestionsWord, host.isInAllUpperCaseState()),
            -1);
      }
    }
  }

  interface Host {
    void performUpdateSuggestions();

    @NonNull
    KeyboardDefinition currentAlphabetKeyboard();

    boolean isCurrentlyPredicting();

    boolean isSentenceSeparator(int code);

    boolean isAutoCorrect();

    boolean isDoubleSpaceChangesToPeriod();

    int multiTapTimeout();

    @NonNull
    SpaceTimeTracker spaceTimeTracker();

    @NonNull
    SeparatorOutputHandler separatorOutputHandler();

    boolean isSpaceSwapCharacter(int primaryCode);

    void commitWordToInput(@NonNull CharSequence wordToCommit, @NonNull CharSequence typedWord);

    void checkAddToDictionaryWithAutoDictionary(
        @NonNull CharSequence newWord, @NonNull Suggest.AdditionType type);

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    void setWordRevertLength(int length);

    void sendKeyChar(char c);

    void markExpectingSelectionUpdate();

    InputConnectionRouter inputConnectionRouter();

    @NonNull
    WordComposer prepareWordComposerForNextWord();

    /** Returns true when the keyboard is in an ALL-CAPS mode (caps-lock). */
    boolean isInAllUpperCaseState();

    @NonNull
    CharSequence lastCommittedWordForNextSuggestions();

    void clearLastCommittedWordForNextSuggestions();

    @NonNull
    Suggest suggest();

    void clearSuggestions();

    void setSuggestions(@NonNull List<? extends CharSequence> suggestions, int highlightedIndex);
  }
}
