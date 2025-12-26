package wtf.uhoh.newsoftkeyboard.app.ime;

import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateViewHost;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

/** Small helper to encapsulate suggestion strip wiring and visibility toggles. */
final class SuggestionStripController {

  private final CancelSuggestionsAction cancelSuggestionsAction;
  private final CandidateView candidateView;

  SuggestionStripController(
      CancelSuggestionsAction cancelSuggestionsAction, CandidateView candidateView) {
    this.cancelSuggestionsAction = cancelSuggestionsAction;
    this.candidateView = candidateView;
    this.cancelSuggestionsAction.setOwningCandidateView(candidateView);
  }

  void setHost(CandidateViewHost host) {
    candidateView.setHost(host);
  }

  void attachToStrip(KeyboardViewContainerView container) {
    container.addStripAction(cancelSuggestionsAction, false);
  }

  void showStrip(boolean predictionOn, KeyboardViewContainerView container) {
    cancelSuggestionsAction.setCancelIconVisible(false);
    container.setActionsStripVisibility(predictionOn);
  }
}
