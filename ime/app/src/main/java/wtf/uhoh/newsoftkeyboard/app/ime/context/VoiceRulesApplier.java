package wtf.uhoh.newsoftkeyboard.app.ime.context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class VoiceRulesApplier {

  @NonNull
  static VoiceTextPostProcessResult apply(
      @NonNull String formattedText,
      @NonNull List<ContextProfilesStore.VoiceRule> rules,
      boolean allowSuggestions,
      boolean allowAutoApply) {
    if (formattedText.isEmpty()) return new VoiceTextPostProcessResult(formattedText, null);

    int start = 0;
    while (start < formattedText.length() && Character.isWhitespace(formattedText.charAt(start))) {
      start++;
    }
    int end = formattedText.length();
    while (end > start && Character.isWhitespace(formattedText.charAt(end - 1))) {
      end--;
    }

    final String core = formattedText.substring(start, end);
    if (core.isEmpty()) return new VoiceTextPostProcessResult(formattedText, null);

    final String autoReplacement = allowAutoApply ? resolveAutoReplacement(core, rules) : null;
    if (autoReplacement != null) {
      return new VoiceTextPostProcessResult(
          formattedText.substring(0, start) + autoReplacement + formattedText.substring(end), null);
    }

    if (!allowSuggestions) {
      return new VoiceTextPostProcessResult(formattedText, null);
    }

    final String suggestionReplacement = resolveSuggestionReplacement(core, rules);
    if (suggestionReplacement == null) {
      return new VoiceTextPostProcessResult(formattedText, null);
    }

    final String replacementText =
        formattedText.substring(0, start) + suggestionReplacement + formattedText.substring(end);
    return new VoiceTextPostProcessResult(
        formattedText,
        new VoiceTextPostProcessResult.VoiceSuggestion(
            suggestionReplacement, formattedText, replacementText));
  }

  @Nullable
  private static String resolveAutoReplacement(
      @NonNull String core, @NonNull List<ContextProfilesStore.VoiceRule> rules) {
    for (ContextProfilesStore.VoiceRule rule : rules) {
      if (rule == null) continue;
      if (!rule.autoApply) continue;
      if (core.equalsIgnoreCase(rule.match)) return rule.replace;
    }
    return null;
  }

  @Nullable
  private static String resolveSuggestionReplacement(
      @NonNull String core, @NonNull List<ContextProfilesStore.VoiceRule> rules) {
    for (ContextProfilesStore.VoiceRule rule : rules) {
      if (rule == null) continue;
      if (rule.autoApply) continue;
      if (core.equalsIgnoreCase(rule.match)) return rule.replace;
    }
    return null;
  }

  private VoiceRulesApplier() {}
}
