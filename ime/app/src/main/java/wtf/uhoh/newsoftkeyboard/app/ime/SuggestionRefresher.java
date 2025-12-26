package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.NonNull;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Keeps suggestion refresh logic out of {@link ImeSuggestionsController}. */
final class SuggestionRefresher {

  interface Host {
    void clearSuggestions();

    void setSuggestions(
        @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex);
  }

  void performUpdateSuggestions(
      @NonNull PredictionState predictionState,
      @NonNull WordComposer wordComposer,
      @NonNull Suggest suggest,
      @NonNull Host host) {
    if (!predictionState.isPredictionOn() || !predictionState.showSuggestions) {
      host.clearSuggestions();
      return;
    }

    final List<CharSequence> suggestionsList = suggest.getSuggestions(wordComposer);
    int highlightedSuggestionIndex =
        predictionState.isAutoCorrect() ? suggest.getLastValidSuggestionIndex() : -1;

    // Don't auto-correct words with multiple capital letters
    if (highlightedSuggestionIndex == 1 && wordComposer.isMostlyCaps()) {
      highlightedSuggestionIndex = -1;
    }

    host.setSuggestions(suggestionsList, highlightedSuggestionIndex);
    if (highlightedSuggestionIndex >= 0) {
      wordComposer.setPreferredWord(suggestionsList.get(highlightedSuggestionIndex));
    } else {
      wordComposer.setPreferredWord(null);
    }
  }
}
