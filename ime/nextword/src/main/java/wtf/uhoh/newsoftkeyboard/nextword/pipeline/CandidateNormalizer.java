package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight, engine-agnostic normalization for next-word candidates.
 *
 * <p>Current scope: trim whitespace, drop empty/punctuation-only tokens, and de-duplicate
 * case-insensitively while preserving original casing.
 *
 * <p>Note: Casing transformation (e.g., sentence-case) is intentionally not applied here yet to
 * avoid behavioral changes. Add that later once UX rules are finalized.
 */
public final class CandidateNormalizer {

  private static final Set<String> DOMAIN_LIKE_TOKENS =
      java.util.Collections.unmodifiableSet(
          new HashSet<>(java.util.Arrays.asList("com", "http", "https", "www")));

  /**
   * Normalizes a list of model predictions.
   *
   * <ul>
   *   <li>Trims whitespace
   *   <li>Drops empty strings
   *   <li>Drops punctuation-only tokens
   *   <li>De-duplicates case-insensitively (keeps first occurrence)
   * </ul>
   */
  @NonNull
  public static List<String> normalize(@NonNull List<String> raw) {
    return normalize(raw, java.util.Collections.emptySet());
  }

  /**
   * Normalizes next-word candidates specifically for suggestion-strip UX.
   *
   * <p>In addition to {@link #normalize(List)} it also filters out low-quality domain-like tokens
   * (e.g., {@code com/http/www}) unless the recent context looks URL-like.
   */
  @NonNull
  public static List<String> normalizeForNextWordUx(
      @NonNull Deque<String> contextTokens, @NonNull List<String> raw) {
    return normalizeForNextWordUx(contextTokens, raw, java.util.Collections.emptySet());
  }

  /**
   * Normalizes a list of model predictions, skipping any token whose lowercase form appears in
   * {@code disallowLower}. If everything is filtered out, returns an empty list (callers may choose
   * to fall back to legacy sources instead of repeating the same token).
   */
  @NonNull
  public static List<String> normalize(
      @NonNull List<String> raw, @NonNull Collection<String> disallowLower) {
    final List<String> out = new ArrayList<>(raw.size());
    final Set<String> seenLower = new HashSet<>();
    for (String token : raw) {
      if (token == null) continue;
      final String trimmed = token.trim();
      if (trimmed.isEmpty()) continue;
      if (isPunctuationOnly(trimmed)) continue;
      final String lower = trimmed.toLowerCase(Locale.ROOT);
      if (disallowLower.contains(lower)) continue;
      if (seenLower.add(lower)) {
        // preserve original casing for now
        out.add(trimmed);
      }
    }
    return out;
  }

  /**
   * Normalizes next-word candidates for suggestion-strip UX, skipping any token whose lowercase
   * form appears in {@code disallowLower} and filtering out domain-like tokens when the recent
   * context does not appear URL-like.
   *
   * <p>If everything is filtered out, returns an empty list (callers may choose to fall back to
   * other sources instead of surfacing low-quality artifacts).
   */
  @NonNull
  public static List<String> normalizeForNextWordUx(
      @NonNull Deque<String> contextTokens,
      @NonNull List<String> raw,
      @NonNull Collection<String> disallowLower) {
    final boolean allowDomainLike = looksLikeDomainContext(contextTokens);
    final List<String> out = new ArrayList<>(raw.size());
    final Set<String> seenLower = new HashSet<>();
    for (String token : raw) {
      if (token == null) continue;
      final String trimmed = token.trim();
      if (trimmed.isEmpty()) continue;
      if (isPunctuationOnly(trimmed)) continue;
      final String lower = trimmed.toLowerCase(Locale.ROOT);
      if (disallowLower.contains(lower)) continue;
      if (!allowDomainLike && DOMAIN_LIKE_TOKENS.contains(lower)) continue;
      if (!allowDomainLike && looksLikeDomainName(lower)) continue;
      if (seenLower.add(lower)) {
        // preserve original casing for now
        out.add(trimmed);
      }
    }
    return out;
  }

  private static boolean looksLikeDomainName(@NonNull String tokenLower) {
    // Best-effort, conservative heuristic:
    // - no whitespace
    // - contains a '.' not at the edges
    // - last segment is a 2+ letter TLD
    // - all chars are in a small "domain-ish" allow-list
    if (tokenLower.length() < 4) return false;
    if (tokenLower.indexOf('.') < 0) return false;
    if (tokenLower.startsWith(".") || tokenLower.endsWith(".")) return false;

    boolean hasLetter = false;
    for (int i = 0; i < tokenLower.length(); i++) {
      final char c = tokenLower.charAt(i);
      if (Character.isWhitespace(c)) return false;
      if (Character.isLetter(c)) {
        hasLetter = true;
        continue;
      }
      if (Character.isDigit(c) || c == '.' || c == '-' || c == '_') {
        continue;
      }
      return false;
    }
    if (!hasLetter) return false;

    final int lastDot = tokenLower.lastIndexOf('.');
    if (lastDot <= 0 || lastDot >= tokenLower.length() - 2) return false;
    final String tld = tokenLower.substring(lastDot + 1);
    if (tld.length() < 2 || tld.length() > 24) return false;
    for (int i = 0; i < tld.length(); i++) {
      if (!Character.isLetter(tld.charAt(i))) return false;
    }

    return true;
  }

  private static boolean looksLikeDomainContext(@NonNull Deque<String> contextTokens) {
    if (contextTokens.isEmpty()) return false;
    final Iterator<String> it = contextTokens.descendingIterator();
    for (int i = 0; it.hasNext() && i < 2; i++) {
      final String token = it.next();
      if (token == null) continue;
      final String lower = token.toLowerCase(Locale.ROOT);
      if (lower.contains(".")) return true;
      if (lower.startsWith("http")) return true;
      if (lower.startsWith("www")) return true;
    }
    return false;
  }

  private static boolean isPunctuationOnly(@NonNull String s) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (Character.isLetterOrDigit(c)) return false;
    }
    return true;
  }
}
