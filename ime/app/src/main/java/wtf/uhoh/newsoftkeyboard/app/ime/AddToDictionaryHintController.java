package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Encapsulates add-to-dictionary hint/show/next-suggestions flow after a manual pick. */
final class AddToDictionaryHintController {

  private static final String TAG = "NextWordFlow";

  interface Host {
    @Nullable
    CandidateView candidateView();

    Suggest suggest();

    KeyboardDefinition currentAlphabetKeyboard();

    void setSuggestions(List<CharSequence> suggestions, int highlightedIndex);
  }

  private final Host host;

  AddToDictionaryHintController(Host host) {
    this.host = host;
  }

  void handlePostPick(
      int pickedIndex,
      boolean showSuggestions,
      boolean justAutoAddedWord,
      boolean isTagsSearchState,
      boolean inAllUpperCaseState,
      CharSequence suggestion,
      WordComposer typedWord) {

    if (isTagsSearchState) {
      return; // no add-to-dictionary hint in tags search
    }

    final boolean pickedTypedWord =
        typedWord != null
            && !TextUtils.isEmpty(typedWord.getTypedWord())
            && TextUtils.equals(suggestion, typedWord.getTypedWord());
    if (pickedTypedWord
        && shouldShowAddHint(
            pickedIndex,
            showSuggestions,
            justAutoAddedWord,
            host.suggest(),
            suggestion,
            host.currentAlphabetKeyboard().getLocale())) {
      final CandidateView cv = host.candidateView();
      if (cv != null) {
        cv.showAddToDictionaryHint(suggestion);
      }
    } else {
      Logger.d(
          TAG,
          "Next-word request (manual pick): index=%d word='%s'",
          pickedIndex,
          suggestion == null ? "" : suggestion.toString());
      host.setSuggestions(host.suggest().getNextSuggestions(suggestion, inAllUpperCaseState), -1);
    }
  }

  private static boolean shouldShowAddHint(
      int index,
      boolean showSuggestions,
      boolean justAutoAddedWord,
      Suggest suggest,
      CharSequence suggestion,
      Locale locale) {
    return AddToDictionaryDecider.shouldShowAddHint(
        index, justAutoAddedWord, showSuggestions, suggest, suggestion, locale);
  }
}
