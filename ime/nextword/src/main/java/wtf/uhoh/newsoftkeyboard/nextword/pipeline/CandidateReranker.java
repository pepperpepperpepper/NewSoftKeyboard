package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Heuristic reranking for next-word candidates.
 *
 * <p>Goal: keep the strip feeling "alive" and avoid obvious repetition loops when a user keeps
 * tapping the top suggestion (greedy decode).
 */
public final class CandidateReranker {

  private static final char KEY_DELIMITER = '\u0001';
  private static final int RECENT_WORD_REPEAT_WINDOW = 6;
  private static final int RECENT_CONTENT_WORD_REPEAT_WINDOW = 12;
  private static final int RECENT_NGRAM_WINDOW = 24;
  private static final int DIVERSITY_SCAN_LIMIT = 16;
  private static final int CONTENT_WORD_MIN_LEN = 3;
  private static final java.util.Set<String> COMMON_DETERMINERS =
      java.util.Collections.unmodifiableSet(
          new java.util.HashSet<>(java.util.Arrays.asList("a", "an", "the")));
  private static final java.util.Set<String> COMMON_FUNCTION_WORDS =
      java.util.Collections.unmodifiableSet(
          new java.util.HashSet<>(
              java.util.Arrays.asList(
                  "a",
                  "an",
                  "the",
                  "of",
                  "to",
                  "and",
                  "or",
                  "but",
                  "if",
                  "in",
                  "on",
                  "at",
                  "for",
                  "from",
                  "with",
                  "as",
                  "by",
                  "that",
                  "this",
                  "these",
                  "those",
                  "is",
                  "it",
                  "be",
                  // Common pronouns + auxiliary verbs. This is only used for diversity heuristics
                  // and does not affect committed text.
                  "i",
                  "you",
                  "we",
                  "he",
                  "she",
                  "they",
                  "me",
                  "my",
                  "your",
                  "our",
                  "their",
                  "them",
                  "us",
                  "am",
                  "are",
                  "was",
                  "were",
                  "have",
                  "has",
                  "had",
                  "do",
                  "does",
                  "did",
                  "not")));

  private CandidateReranker() {}

  @NonNull
  public static List<String> rerank(
      @NonNull Deque<String> contextTokens, @NonNull List<String> candidates) {
    if (candidates.size() <= 1 || contextTokens.isEmpty()) {
      return candidates;
    }

    final String lastLower = lower(contextTokens.peekLast());
    final String prevLower = lower(peekSecondLast(contextTokens));

    final Set<String> recentWords = new HashSet<>();
    final Set<String> recentContentWords = new HashSet<>();
    int recentCount = 0;
    final int recentScanLimit =
        Math.max(RECENT_WORD_REPEAT_WINDOW, RECENT_CONTENT_WORD_REPEAT_WINDOW);
    for (java.util.Iterator<String> iterator = contextTokens.descendingIterator();
        iterator.hasNext() && recentCount < recentScanLimit;
        recentCount++) {
      final String token = iterator.next();
      final String lower = lower(token);
      if (lower.isEmpty()) continue;
      if (recentCount < RECENT_WORD_REPEAT_WINDOW) recentWords.add(lower);
      if (recentCount < RECENT_CONTENT_WORD_REPEAT_WINDOW && isLikelyContentWord(lower)) {
        recentContentWords.add(lower);
      }
    }

    final List<String> ctxLowerTail =
        new ArrayList<>(Math.min(contextTokens.size(), RECENT_NGRAM_WINDOW));
    final int tailStart = Math.max(0, contextTokens.size() - RECENT_NGRAM_WINDOW);
    int index = 0;
    for (String token : contextTokens) {
      if (index++ < tailStart) continue;
      final String lower = lower(token);
      if (!lower.isEmpty()) {
        ctxLowerTail.add(lower);
      }
    }

    final Set<String> recentBigrams = new HashSet<>();
    for (int i = 0; i + 1 < ctxLowerTail.size(); i++) {
      recentBigrams.add(ctxLowerTail.get(i) + KEY_DELIMITER + ctxLowerTail.get(i + 1));
    }
    final Set<String> recentTrigrams = new HashSet<>();
    for (int i = 0; i + 2 < ctxLowerTail.size(); i++) {
      recentTrigrams.add(
          ctxLowerTail.get(i)
              + KEY_DELIMITER
              + ctxLowerTail.get(i + 1)
              + KEY_DELIMITER
              + ctxLowerTail.get(i + 2));
    }

    final List<String> primary = new ArrayList<>(candidates.size());
    final List<String> repeats = new ArrayList<>();
    final List<String> immediateRepeats = new ArrayList<>();

    for (String candidate : candidates) {
      if (candidate == null) continue;
      final String candidateLower = lower(candidate);
      if (candidateLower.isEmpty()) continue;

      final boolean isImmediateRepeat = !lastLower.isEmpty() && candidateLower.equals(lastLower);
      if (isImmediateRepeat) {
        immediateRepeats.add(candidate);
        continue;
      }

      final boolean violatesIndefiniteArticle =
          violatesIndefiniteArticleRule(lastLower, candidateLower);
      if (violatesIndefiniteArticle) {
        repeats.add(candidate);
        continue;
      }

      final boolean repeatsDeterminerSequence =
          !lastLower.isEmpty()
              && COMMON_DETERMINERS.contains(lastLower)
              && COMMON_DETERMINERS.contains(candidateLower);
      if (repeatsDeterminerSequence) {
        repeats.add(candidate);
        continue;
      }

      final boolean repeatsRecentWord =
          !recentWords.isEmpty() && recentWords.contains(candidateLower);
      final boolean repeatsRecentContentWord =
          !recentContentWords.isEmpty()
              && isLikelyContentWord(candidateLower)
              && recentContentWords.contains(candidateLower);
      final boolean repeatsRecentBigram =
          !lastLower.isEmpty()
              && recentBigrams.contains(lastLower + KEY_DELIMITER + candidateLower);
      final boolean repeatsRecentTrigram =
          !prevLower.isEmpty()
              && !lastLower.isEmpty()
              && recentTrigrams.contains(
                  prevLower + KEY_DELIMITER + lastLower + KEY_DELIMITER + candidateLower);

      if (repeatsRecentTrigram
          || repeatsRecentBigram
          || repeatsRecentWord
          || repeatsRecentContentWord) {
        repeats.add(candidate);
      } else {
        primary.add(candidate);
      }
    }

    // After a determiner ("a/an/the"), try to surface a content-ish option early, but keep the
    // most-probable candidate in the top slot. Users expect next-word suggestions to feel
    // probabilistic; swapping the top suggestion can make the strip feel "random".
    if (primary.size() > 1 && COMMON_DETERMINERS.contains(lastLower)) {
      final int scanLimit = Math.min(primary.size(), DIVERSITY_SCAN_LIMIT);
      final String firstLower = lower(primary.get(0));
      final String secondLower = lower(primary.get(1));
      if (looksLikeShortOrFunctionWord(firstLower) && !isLikelyContentWord(secondLower)) {
        promoteFirstContentWord(primary, /* targetIndex= */ 1, /* searchFrom= */ 2, scanLimit);
      }
    }

    // Diversity: if the top row is dominated by short/function tokens (common in greedy decoding),
    // try to surface a longer content-ish option early so users can "chain" suggestions into more
    // sentence-like output without immediately spiraling into stopword loops.
    diversifyPrimaryHead(primary);

    if (repeats.isEmpty() && immediateRepeats.isEmpty()) {
      return primary;
    }

    final List<String> out =
        new ArrayList<>(primary.size() + repeats.size() + immediateRepeats.size());
    out.addAll(primary);
    out.addAll(repeats);
    out.addAll(immediateRepeats);
    return out;
  }

  @NonNull
  private static String lower(String token) {
    return token == null ? "" : token.toLowerCase();
  }

  private static void diversifyPrimaryHead(@NonNull List<String> primary) {
    if (primary.size() < 3) return;

    final String firstLower = lower(primary.get(0));
    final String secondLower = lower(primary.get(1));
    if (!looksLikeShortOrFunctionWord(firstLower) || !looksLikeShortOrFunctionWord(secondLower)) {
      return;
    }

    final int scanLimit = Math.min(primary.size(), DIVERSITY_SCAN_LIMIT);
    promoteFirstContentWord(primary, /* targetIndex= */ 1, /* searchFrom= */ 2, scanLimit);

    // If we still have 2+ short/function items in the top row, try to surface one more content-ish
    // option so users don't see a strip that feels like a list of stopwords.
    if (countShortOrFunctionWords(primary, /* topN= */ 3) >= 2) {
      promoteFirstContentWord(primary, /* targetIndex= */ 2, /* searchFrom= */ 3, scanLimit);
    }

    // If the top row still has no content-ish option, promote the longest candidate available
    // (even if it's not a "content word") to avoid a strip of 1-2 letter tokens.
    if (!hasLikelyContentWord(primary, /* topN= */ 3)) {
      promoteLongestWord(primary, /* targetIndex= */ 1, /* searchFrom= */ 2, scanLimit);
    }
  }

  private static void promoteFirstContentWord(
      @NonNull List<String> primary, int targetIndex, int searchFrom, int scanLimit) {
    if (targetIndex < 0 || targetIndex >= primary.size()) return;
    int promoteIndex = -1;
    for (int i = Math.max(searchFrom, targetIndex + 1); i < scanLimit; i++) {
      final String candidateLower = lower(primary.get(i));
      if (isLikelyContentWord(candidateLower)) {
        promoteIndex = i;
        break;
      }
    }
    if (promoteIndex > targetIndex) {
      final String promote = primary.remove(promoteIndex);
      primary.add(targetIndex, promote);
    }
  }

  private static boolean hasLikelyContentWord(@NonNull List<String> primary, int topN) {
    final int limit = Math.min(primary.size(), topN);
    for (int i = 0; i < limit; i++) {
      if (isLikelyContentWord(lower(primary.get(i)))) return true;
    }
    return false;
  }

  private static int countShortOrFunctionWords(@NonNull List<String> primary, int topN) {
    final int limit = Math.min(primary.size(), topN);
    int count = 0;
    for (int i = 0; i < limit; i++) {
      if (looksLikeShortOrFunctionWord(lower(primary.get(i)))) count++;
    }
    return count;
  }

  private static void promoteLongestWord(
      @NonNull List<String> primary, int targetIndex, int searchFrom, int scanLimit) {
    if (targetIndex < 0 || targetIndex >= primary.size()) return;

    int bestIndex = -1;
    int bestLen = -1;
    for (int i = Math.max(searchFrom, targetIndex + 1); i < scanLimit; i++) {
      final String candidate = primary.get(i);
      if (candidate == null) continue;
      final int len = candidate.length();
      if (len > bestLen) {
        bestLen = len;
        bestIndex = i;
      }
    }
    if (bestIndex > targetIndex) {
      final String promote = primary.remove(bestIndex);
      primary.add(targetIndex, promote);
    }
  }

  private static boolean looksLikeShortOrFunctionWord(@NonNull String lower) {
    if (lower.isEmpty()) return true;
    if (lower.length() < CONTENT_WORD_MIN_LEN) return true;
    return COMMON_FUNCTION_WORDS.contains(lower);
  }

  private static boolean isLikelyContentWord(@NonNull String lower) {
    if (lower.length() < CONTENT_WORD_MIN_LEN) return false;
    return !COMMON_FUNCTION_WORDS.contains(lower);
  }

  private static boolean violatesIndefiniteArticleRule(
      @NonNull String lastLower, @NonNull String candidateLower) {
    if ("a".equals(lastLower)) {
      return startsWithVowel(candidateLower);
    } else if ("an".equals(lastLower)) {
      return !startsWithVowel(candidateLower);
    } else {
      return false;
    }
  }

  private static boolean startsWithVowel(@NonNull String lower) {
    for (int i = 0; i < lower.length(); i++) {
      final char c = lower.charAt(i);
      if (!Character.isLetter(c)) continue;
      switch (c) {
        case 'a':
        case 'e':
        case 'i':
        case 'o':
        case 'u':
          return true;
        default:
          return false;
      }
    }
    return false;
  }

  private static String peekSecondLast(@NonNull Deque<String> deque) {
    if (deque.size() < 2) return null;
    final java.util.Iterator<String> iterator = deque.descendingIterator();
    iterator.next(); // last
    return iterator.hasNext() ? iterator.next() : null;
  }
}
