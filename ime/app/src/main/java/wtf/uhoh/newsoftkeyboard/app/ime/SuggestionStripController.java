package wtf.uhoh.newsoftkeyboard.app.ime;

import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateViewHost;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

/** Small helper to encapsulate suggestion strip wiring and visibility toggles. */
final class SuggestionStripController {

  private final CancelSuggestionsAction cancelSuggestionsAction;
  private final NoSuggestionsStripAction noSuggestionsStripAction;
  private final CandidateView candidateView;

  SuggestionStripController(
      CancelSuggestionsAction cancelSuggestionsAction, CandidateView candidateView) {
    this.cancelSuggestionsAction = cancelSuggestionsAction;
    this.noSuggestionsStripAction = new NoSuggestionsStripAction();
    this.candidateView = candidateView;
    this.cancelSuggestionsAction.setOwningCandidateView(candidateView);
  }

  void setHost(CandidateViewHost host) {
    candidateView.setHost(host);
  }

  void attachToStrip(KeyboardViewContainerView container) {
    container.addStripAction(cancelSuggestionsAction, false);
    container.addStripAction(noSuggestionsStripAction, true);
  }

  void showStrip(
      boolean predictionOn, boolean showNoSuggestionsAction, KeyboardViewContainerView container) {
    cancelSuggestionsAction.setCancelIconVisible(false);
    noSuggestionsStripAction.setVisible(showNoSuggestionsAction);
    container.setActionsStripVisibility(predictionOn || showNoSuggestionsAction);
  }
}
