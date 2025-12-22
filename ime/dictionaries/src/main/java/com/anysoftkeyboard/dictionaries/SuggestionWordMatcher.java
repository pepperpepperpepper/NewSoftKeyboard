package com.anysoftkeyboard.dictionaries;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.utils.IMEUtil;

final class SuggestionWordMatcher {

  private SuggestionWordMatcher() {}

  static boolean compareCaseInsensitive(
      @NonNull final CharSequence lowerOriginalWord,
      final char[] word,
      final int offset,
      final int length) {
    final int originalLength = lowerOriginalWord.length();

    if (originalLength == length) {
      for (int i = 0; i < originalLength; i++) {
        if (lowerOriginalWord.charAt(i) != Character.toLowerCase(word[offset + i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  static boolean haveSufficientCommonality(
      final int maxLengthDiff,
      final int maxCommonDistance,
      @NonNull final CharSequence typedWord,
      @NonNull final char[] word,
      final int offset,
      final int length) {
    final int originalLength = typedWord.length();
    final int lengthDiff = length - originalLength;

    return lengthDiff <= maxLengthDiff
        && IMEUtil.editDistance(typedWord, word, offset, length) <= maxCommonDistance;
  }
}
