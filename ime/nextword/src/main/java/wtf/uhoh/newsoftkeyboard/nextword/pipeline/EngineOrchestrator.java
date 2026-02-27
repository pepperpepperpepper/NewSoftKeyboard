package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import androidx.annotation.NonNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.engine.PredictionEngine;
import wtf.uhoh.newsoftkeyboard.engine.PredictionResult;

/**
 * Shared helper that runs a prediction engine and merges normalized candidates into the holder.
 *
 * <p>This keeps SuggestionsProvider slimmer while preserving existing behavior.
 */
public final class EngineOrchestrator {

  private EngineOrchestrator() {}

  public static final class CandidatesOutcome {
    public final List<String> candidates;
    public final boolean hadRaw;
    public final boolean hadNormalized;

    CandidatesOutcome(List<String> candidates, boolean hadRaw, boolean hadNormalized) {
      this.candidates = candidates;
      this.hadRaw = hadRaw;
      this.hadNormalized = hadNormalized;
    }

    public static CandidatesOutcome empty() {
      return new CandidatesOutcome(java.util.Collections.emptyList(), false, false);
    }
  }

  public static CandidatesOutcome predictCandidates(
      PredictionEngine engine,
      Deque<String> contextTokens,
      int maxNextWordSuggestionsCount,
      int limit,
      boolean enableTestLogging,
      String logTag) {
    if (limit <= 0 || contextTokens.isEmpty()) {
      return CandidatesOutcome.empty();
    }

    final String[] contextArray = contextTokens.toArray(new String[0]);
    final PredictionResult result =
        engine.predict(contextArray, Math.min(limit, maxNextWordSuggestionsCount));

    final List<String> raw = result.getCandidates();
    final boolean hadRaw = raw != null && !raw.isEmpty();
    if (enableTestLogging) {
      Logger.d(
          logTag,
          "Engine "
              + engine.getType()
              + " raw candidates="
              + raw
              + " ctx="
              + Arrays.toString(contextArray));
    }

    final String lastContextToken = contextTokens.peekLast();
    final List<String> predictions;
    if (lastContextToken == null || lastContextToken.isEmpty()) {
      predictions = CandidateNormalizer.normalizeForNextWordUx(contextTokens, raw);
    } else {
      final String disallow = lastContextToken.toLowerCase(java.util.Locale.ROOT);
      final List<String> filtered =
          CandidateNormalizer.normalizeForNextWordUx(
              contextTokens, raw, java.util.Collections.singleton(disallow));
      if (filtered.isEmpty()) {
        // Prefer falling back to other sources (neural, legacy dictionaries, punctuation) over
        // repeating the last committed word in the strip.
        return new CandidatesOutcome(java.util.Collections.emptyList(), hadRaw, false);
      }
      predictions = filtered;
    }
    if (predictions.isEmpty()) {
      return new CandidatesOutcome(java.util.Collections.emptyList(), hadRaw, false);
    }

    final List<String> reranked = CandidateReranker.rerank(contextTokens, predictions);
    return new CandidatesOutcome(reranked, hadRaw, true);
  }

  public static MergeOutcome predictAndMerge(
      PredictionEngine engine,
      Deque<String> contextTokens,
      int maxNextWordSuggestionsCount,
      Collection<CharSequence> suggestionsHolder,
      int limit,
      boolean enableTestLogging,
      String logTag) {
    final CandidatesOutcome outcome =
        predictCandidates(
            engine, contextTokens, maxNextWordSuggestionsCount, limit, enableTestLogging, logTag);
    return mergeCandidatesOutcome(outcome, suggestionsHolder, limit);
  }

  public static MergeOutcome mergeCandidatesOutcome(
      @NonNull CandidatesOutcome outcome,
      @NonNull Collection<CharSequence> suggestionsHolder,
      int limit) {
    if (outcome.candidates.isEmpty()) {
      return new MergeOutcome(0, outcome.hadRaw, outcome.hadNormalized);
    }
    final int added = CandidateMerger.mergeUnique(suggestionsHolder, outcome.candidates, limit);
    return new MergeOutcome(added, outcome.hadRaw, outcome.hadNormalized);
  }

  public static final class MergeOutcome {
    public final int added;
    public final boolean hadRaw;
    public final boolean hadNormalized;

    MergeOutcome(int added, boolean hadRaw, boolean hadNormalized) {
      this.added = added;
      this.hadRaw = hadRaw;
      this.hadNormalized = hadNormalized;
    }

    public static MergeOutcome empty() {
      return new MergeOutcome(0, false, false);
    }
  }
}
