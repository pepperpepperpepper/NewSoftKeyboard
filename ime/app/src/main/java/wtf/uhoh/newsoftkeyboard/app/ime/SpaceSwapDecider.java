package wtf.uhoh.newsoftkeyboard.app.ime;

/**
 * Encapsulates space-swap decision logic (period/space swap and French punctuation rules) to keep
 * the service smaller.
 */
final class SpaceSwapDecider {

  boolean isSpaceSwapCharacter(
      int primaryCode, boolean frenchSpacePunctuationBehavior, SentenceSeparators separators) {
    // Treat closing parenthesis as a swap candidate even if the current keyboard does not mark
    // it as a sentence separator (historic ASK behavior and expected by unit tests).
    if (primaryCode == ')') {
      return true;
    }

    if (separators.isSeparator(primaryCode)) {
      if (frenchSpacePunctuationBehavior) {
        return switch (primaryCode) {
          case '!', '?', ':', ';' -> false;
          default -> true;
        };
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
}
