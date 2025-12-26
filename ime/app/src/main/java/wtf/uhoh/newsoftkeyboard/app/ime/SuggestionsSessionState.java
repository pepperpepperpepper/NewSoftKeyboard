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

  SuggestionsSessionState(long neverTimeStamp) {
    selectionExpectationTracker = new SelectionExpectationTracker(neverTimeStamp);
  }
}
