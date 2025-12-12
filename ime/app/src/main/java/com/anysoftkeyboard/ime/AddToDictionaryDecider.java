package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.Suggest;
import java.util.Locale;

/**
 * Small helper that encapsulates the logic for showing the
 * "add to dictionary" hint after a manual suggestion pick.
 */
final class AddToDictionaryDecider {

  private AddToDictionaryDecider() {}

  static boolean shouldShowAddHint(
      int pickedIndex,
      boolean justAutoAddedWord,
      boolean showSuggestions,
      Suggest suggest,
      CharSequence suggestion,
      Locale locale) {
    if (justAutoAddedWord) return false;
    if (pickedIndex != 0) return false;
    if (!showSuggestions) return false;
    if (suggest.isValidWord(suggestion)) return false;
    return !suggest.isValidWord(suggestion.toString().toLowerCase(locale));
  }
}
