package com.anysoftkeyboard.ime;

/** Holds suggestion/prediction related flags to reduce noise in the service class. */
final class PredictionState {
  boolean predictionOn;
  boolean autoSpace;
  boolean inputFieldSupportsAutoPick;
  boolean autoCorrectOn;
  boolean allowSuggestionsRestart = true;
  boolean showSuggestions;
  boolean autoComplete;

  boolean isPredictionOn() {
    return predictionOn && showSuggestions;
  }

  boolean isAutoCorrect() {
    return autoCorrectOn && inputFieldSupportsAutoPick && predictionOn;
  }
}
