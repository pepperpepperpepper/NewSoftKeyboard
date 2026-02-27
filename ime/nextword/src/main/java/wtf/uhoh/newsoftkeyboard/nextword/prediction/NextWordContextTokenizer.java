package wtf.uhoh.newsoftkeyboard.nextword.prediction;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tokenizes existing editor text into a small "context window" suitable for next-word prediction.
 *
 * <p>Design goals:
 *
 * <ul>
 *   <li>Best-effort and allocation-light (backward scan, bounded output).
 *   <li>Stop at sentence boundaries (so a trailing '.' produces an empty context, matching the
 *       runtime reset behavior).
 *   <li>Preserve domain-like tokens (e.g., "example.com") so downstream filtering can make better
 *       decisions about "com/http/www" suggestions.
 * </ul>
 */
public final class NextWordContextTokenizer {

  private NextWordContextTokenizer() {}

  @NonNull
  public static List<String> tokenizeTextBeforeCursor(
      @NonNull CharSequence textBeforeCursor, int maxTokens) {
    if (maxTokens <= 0) return Collections.emptyList();
    final int length = textBeforeCursor.length();
    if (length == 0) return Collections.emptyList();

    int i = length - 1;
    while (i >= 0 && Character.isWhitespace(textBeforeCursor.charAt(i))) i--;
    if (i < 0) return Collections.emptyList();

    final ArrayList<String> reversed = new ArrayList<>(Math.min(maxTokens, 8));
    while (i >= 0 && reversed.size() < maxTokens) {
      if (isSentenceBoundary(textBeforeCursor, i)) {
        break;
      }
      if (isTokenChar(textBeforeCursor, i)) {
        final int tokenEndExclusive = i + 1;
        int start = i;
        while (start >= 0 && isTokenChar(textBeforeCursor, start)) start--;
        final String raw = textBeforeCursor.subSequence(start + 1, tokenEndExclusive).toString();
        final String token = trimToLettersOrDigits(raw);
        if (!token.isEmpty()) {
          reversed.add(token);
        }
        i = start;
      } else {
        i--;
      }
    }

    if (reversed.isEmpty()) return Collections.emptyList();
    Collections.reverse(reversed);
    return reversed;
  }

  private static boolean isSentenceBoundary(@NonNull CharSequence text, int index) {
    final char c = text.charAt(index);
    switch (c) {
      case '\n':
      case '\r':
      case '!':
      case '?':
      case '\u3002': // IDEOGRAPHIC FULL STOP
      case '\uff01': // FULLWIDTH EXCLAMATION MARK
      case '\uff1f': // FULLWIDTH QUESTION MARK
        return true;
      case '.':
        // A '.' within a token is treated as domain-like (example.com). Sentence boundaries
        // usually appear at token boundaries (hello. <space>).
        return !isTokenChar(text, index);
      default:
        return false;
    }
  }

  private static boolean isTokenChar(@NonNull CharSequence text, int index) {
    final char c = text.charAt(index);
    if (Character.isLetterOrDigit(c)) return true;
    // allow common intra-token joiners; we'll trim them at edges later
    if (c == '\'' || c == '\u2019' || c == '-' || c == '_') return true;
    if (c == '.') {
      if (index <= 0 || index >= text.length() - 1) return false;
      final char before = text.charAt(index - 1);
      final char after = text.charAt(index + 1);
      return Character.isLetterOrDigit(before) && Character.isLetterOrDigit(after);
    }
    return false;
  }

  @NonNull
  private static String trimToLettersOrDigits(@NonNull String raw) {
    int start = 0;
    int endExclusive = raw.length();
    while (start < endExclusive && !Character.isLetterOrDigit(raw.charAt(start))) start++;
    while (endExclusive > start && !Character.isLetterOrDigit(raw.charAt(endExclusive - 1)))
      endExclusive--;
    if (start == 0 && endExclusive == raw.length()) return raw;
    if (start >= endExclusive) return "";
    return raw.substring(start, endExclusive);
  }
}
