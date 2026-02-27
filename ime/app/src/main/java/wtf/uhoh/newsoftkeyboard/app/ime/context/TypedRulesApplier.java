package wtf.uhoh.newsoftkeyboard.app.ime.context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

/** Applies typed rules to recently committed text near the cursor. */
public final class TypedRulesApplier {

  public static final int MAX_MATCH_CHARS = 128;
  public static final int MAX_OUTPUT_CHARS = 256;
  private static final int MAX_PUNCTUATION_TAIL_CHARS = 8;

  @NonNull
  public static Result resolve(
      @NonNull String textBeforeCursor,
      @NonNull List<ContextProfilesStore.TypedRule> rules,
      boolean allowSuggestions) {
    if (textBeforeCursor.isEmpty() || rules.isEmpty()) return Result.noop();

    int coreEnd = textBeforeCursor.length();
    while (coreEnd > 0 && Character.isWhitespace(textBeforeCursor.charAt(coreEnd - 1))) {
      coreEnd--;
    }

    final String core = textBeforeCursor.substring(0, coreEnd);
    final String tail = textBeforeCursor.substring(coreEnd);
    if (core.isEmpty()) return Result.noop();

    final Replacement autoReplacement =
        resolveFirstMatchWithPunctuationFallback(core, tail, rules, true /*autoApply*/);
    if (autoReplacement != null) {
      return Result.auto(autoReplacement);
    }

    if (!allowSuggestions) return Result.noop();

    final Replacement suggestionReplacement =
        resolveFirstMatchWithPunctuationFallback(core, tail, rules, false /*autoApply*/);
    if (suggestionReplacement == null) return Result.noop();

    final String suggestionText = trimWhitespace(suggestionReplacement.replacementText());
    if (suggestionText.isEmpty()) return Result.noop();
    return Result.suggestion(
        new TypedSuggestion(
            suggestionText,
            suggestionReplacement.committedText(),
            suggestionReplacement.replacementText()));
  }

  @Nullable
  private static Replacement resolveFirstMatch(
      @NonNull String core,
      @NonNull String tail,
      @NonNull List<ContextProfilesStore.TypedRule> rules,
      boolean autoApply) {
    for (ContextProfilesStore.TypedRule rule : rules) {
      if (rule == null) continue;
      if (!rule.enabled) continue;
      if (rule.autoApply != autoApply) continue;

      final String match = rule.match.trim();
      if (match.isEmpty()) continue;
      if (match.length() > MAX_MATCH_CHARS) continue;
      if (core.length() < match.length()) continue;

      final int start = core.length() - match.length();
      if (!core.regionMatches(!rule.matchCaseSensitive, start, match, 0, match.length())) continue;
      if (rule.matchWholeWord && start > 0 && Character.isLetterOrDigit(core.charAt(start - 1)))
        continue;

      final String committedText = core.substring(start) + tail;
      final String replacementText =
          appendTailAvoidingDuplicateSentencePunctuation(rule.replace, tail);
      if (committedText.equals(replacementText)) continue;
      if (replacementText.length() > MAX_OUTPUT_CHARS) continue;

      return new Replacement(committedText, replacementText);
    }

    return null;
  }

  @Nullable
  private static Replacement resolveFirstMatchWithPunctuationFallback(
      @NonNull String core,
      @NonNull String tail,
      @NonNull List<ContextProfilesStore.TypedRule> rules,
      boolean autoApply) {
    final Replacement direct = resolveFirstMatch(core, tail, rules, autoApply);
    if (direct != null) return direct;

    final PunctuationSplit split = splitTrailingSentencePunctuation(core);
    if (split == null || split.coreWithoutPunctuation.isEmpty()) return null;
    return resolveFirstMatch(
        split.coreWithoutPunctuation, split.punctuation + tail, rules, autoApply);
  }

  @Nullable
  private static PunctuationSplit splitTrailingSentencePunctuation(@NonNull String core) {
    int start = core.length();
    while (start > 0 && isSentencePunctuation(core.charAt(start - 1))) {
      start--;
      if (core.length() - start >= MAX_PUNCTUATION_TAIL_CHARS) {
        break;
      }
    }
    if (start == core.length()) return null;
    return new PunctuationSplit(core.substring(0, start), core.substring(start));
  }

  private static boolean isSentencePunctuation(char c) {
    switch (c) {
      case '.':
      case '!':
      case '?':
      case ',':
      case ';':
      case ':':
      case '…':
      case '。':
      case '！':
      case '？':
      case '，':
      case '；':
      case '：':
      case '、':
        return true;
      default:
        return false;
    }
  }

  @NonNull
  private static String appendTailAvoidingDuplicateSentencePunctuation(
      @NonNull String replacement, @NonNull String tail) {
    if (tail.isEmpty()) return replacement;

    int punctuationPrefixLength = 0;
    while (punctuationPrefixLength < tail.length()
        && isSentencePunctuation(tail.charAt(punctuationPrefixLength))) {
      punctuationPrefixLength++;
      if (punctuationPrefixLength >= MAX_PUNCTUATION_TAIL_CHARS) {
        break;
      }
    }
    if (punctuationPrefixLength == 0) return replacement + tail;

    final int maxOverlap = Math.min(replacement.length(), punctuationPrefixLength);
    for (int overlap = maxOverlap; overlap > 0; overlap--) {
      if (replacement.regionMatches(replacement.length() - overlap, tail, 0, overlap)) {
        return replacement + tail.substring(overlap);
      }
    }
    return replacement + tail;
  }

  @NonNull
  private static String trimWhitespace(@NonNull String text) {
    int start = 0;
    while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
    int end = text.length();
    while (end > start && Character.isWhitespace(text.charAt(end - 1))) end--;
    return text.substring(start, end);
  }

  private static final class PunctuationSplit {
    @NonNull final String coreWithoutPunctuation;
    @NonNull final String punctuation;

    private PunctuationSplit(@NonNull String coreWithoutPunctuation, @NonNull String punctuation) {
      this.coreWithoutPunctuation = coreWithoutPunctuation;
      this.punctuation = punctuation;
    }
  }

  public static final class Replacement {
    @NonNull private final String committedText;
    @NonNull private final String replacementText;

    Replacement(@NonNull String committedText, @NonNull String replacementText) {
      this.committedText = committedText;
      this.replacementText = replacementText;
    }

    @NonNull
    public String committedText() {
      return committedText;
    }

    @NonNull
    public String replacementText() {
      return replacementText;
    }
  }

  public static final class TypedSuggestion {
    @NonNull private final String suggestionText;
    @NonNull private final String committedText;
    @NonNull private final String replacementText;

    TypedSuggestion(
        @NonNull String suggestionText,
        @NonNull String committedText,
        @NonNull String replacementText) {
      this.suggestionText = suggestionText;
      this.committedText = committedText;
      this.replacementText = replacementText;
    }

    @NonNull
    public String suggestionText() {
      return suggestionText;
    }

    @NonNull
    public String committedText() {
      return committedText;
    }

    @NonNull
    public String replacementText() {
      return replacementText;
    }
  }

  public static final class Result {
    @Nullable private final Replacement autoReplacement;
    @Nullable private final TypedSuggestion suggestion;

    private Result(@Nullable Replacement autoReplacement, @Nullable TypedSuggestion suggestion) {
      this.autoReplacement = autoReplacement;
      this.suggestion = suggestion;
    }

    @NonNull
    static Result noop() {
      return new Result(null, null);
    }

    @NonNull
    static Result auto(@NonNull Replacement replacement) {
      return new Result(replacement, null);
    }

    @NonNull
    static Result suggestion(@NonNull TypedSuggestion suggestion) {
      return new Result(null, suggestion);
    }

    @Nullable
    public Replacement autoReplacement() {
      return autoReplacement;
    }

    @Nullable
    public TypedSuggestion suggestion() {
      return suggestion;
    }
  }

  private TypedRulesApplier() {}
}
