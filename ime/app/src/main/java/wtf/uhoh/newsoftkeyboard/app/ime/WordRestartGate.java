package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

/** Encapsulates the logic that decides if we can restart word suggestions. */
final class WordRestartGate {

  boolean canRestartWordSuggestion(
      boolean predictionOn, boolean allowSuggestionsRestart, @Nullable InputViewBinder inputView) {
    if (!predictionOn || !allowSuggestionsRestart || inputView == null || !inputView.isShown()) {
      Logger.d(
          ImeSuggestionsController.TAG,
          "performRestartWordSuggestion: no need to restart: isPredictionOn=%s,"
              + " mAllowSuggestionsRestart=%s",
          predictionOn,
          allowSuggestionsRestart);
      return false;
    }
    return true;
  }
}
