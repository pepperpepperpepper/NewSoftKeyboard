package wtf.uhoh.newsoftkeyboard.app.ime;

/**
 * Single source of truth for the active suggestions session state.
 *
 * <p>This intentionally groups the small, feature-owned state holders used by the suggestions
 * pipeline so the owning host does not have to maintain many independent fields.
 */
final class SuggestionsSessionState {

  final SentenceSeparators sentenceSeparators = new SentenceSeparators();
  final AutoCorrectState autoCorrectState = new AutoCorrectState();
  final WordComposerTracker wordComposerTracker = new WordComposerTracker();
  final SpaceTimeTracker spaceTimeTracker = new SpaceTimeTracker();
  final LastKeyTracker lastKeyTracker = new LastKeyTracker();
  final SelectionExpectationTracker selectionExpectationTracker;
  final ShiftStateTracker shiftStateTracker = new ShiftStateTracker();
  final PredictionState predictionState = new PredictionState();

  /**
   * Last committed word used for "next suggestions" requests when the user presses a separator
   * while not composing (e.g., manual space after a manual pick).
   *
   * <p>This is session-scoped and cleared on input start/end.
   */
  String lastCommittedWordForNextSuggestions = "";

  /**
   * When true, keep the currently displayed suggestions while the user is idle (not composing a
   * word). This is used to keep next-word suggestions visible after committing/picking a word, even
   * if a delayed suggestions refresh fires afterwards.
   */
  boolean keepSuggestionsStripWhileIdle = false;

  SuggestionsSessionState(long neverTimeStamp) {
    selectionExpectationTracker = new SelectionExpectationTracker(neverTimeStamp);
  }
}
