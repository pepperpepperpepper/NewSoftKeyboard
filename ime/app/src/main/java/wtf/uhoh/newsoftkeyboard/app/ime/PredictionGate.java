package wtf.uhoh.newsoftkeyboard.app.ime;

/** Centralizes simple prediction gating logic to keep the service slimmer. */
final class PredictionGate {

  boolean shouldRunPrediction(boolean predictionOn, boolean showSuggestions) {
    return predictionOn && showSuggestions;
  }
}
