package wtf.uhoh.newsoftkeyboard.nextword.prediction;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.engine.NeuralEngineAdapter;
import wtf.uhoh.newsoftkeyboard.engine.PredictionEngine;
import wtf.uhoh.newsoftkeyboard.engine.PresageEngineAdapter;
import wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPredictionManager;
import wtf.uhoh.newsoftkeyboard.engine.presage.PresagePredictionManager;
import wtf.uhoh.newsoftkeyboard.nextword.R;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.CandidateMerger;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.CandidateNormalizer;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.CandidateReranker;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.EngineOrchestrator;
import wtf.uhoh.newsoftkeyboard.prefs.RxSharedPrefs;

/**
 * Owns on-device prediction engines (Presage + Neural) and their next-word state.
 *
 * <p>This keeps {@code SuggestionsProvider} focused on dictionary plumbing.
 */
public final class NextWordPredictionEngines {

  private static final int DEFAULT_CONTEXT_WINDOW_WORDS = 20;
  private static final int CONTEXT_WINDOW_WORDS_MIN = 2;
  private static final int CONTEXT_WINDOW_WORDS_MAX = 40;
  private static final int NEURAL_MIN_CONTEXT_TOKENS = 1;
  private static final long NEURAL_LATENCY_BUDGET_MS = 25L;
  // Below this typed length, dictionary completion is plenty; a neural call isn't worth it.
  private static final int NEURAL_COMPLETION_MIN_PREFIX_LEN = 2;
  private static final int HYBRID_NEURAL_COOLDOWN_PASSES = 2;
  private static final int HYBRID_PRESAGE_MAX_CANDIDATES_WHEN_NEURAL_ACTIVE = 4;
  private static final char NEURAL_FAILURE_DELIMITER = '|';

  public static final class Outcome {
    public final int added;
    public final boolean shouldIncludeLegacyNextWords;

    Outcome(int added, boolean shouldIncludeLegacyNextWords) {
      this.added = added;
      this.shouldIncludeLegacyNextWords = shouldIncludeLegacyNextWords;
    }
  }

  private static final class NeuralCandidatesOutcome {
    @NonNull final List<String> candidates;
    final boolean hadRaw;
    final boolean hadNormalized;
    @Nullable final NeuralPredictionManager.NextWordScoringContext scoringContext;

    private NeuralCandidatesOutcome(
        @NonNull List<String> candidates,
        boolean hadRaw,
        boolean hadNormalized,
        @Nullable NeuralPredictionManager.NextWordScoringContext scoringContext) {
      this.candidates = candidates;
      this.hadRaw = hadRaw;
      this.hadNormalized = hadNormalized;
      this.scoringContext = scoringContext;
    }

    static NeuralCandidatesOutcome empty() {
      return new NeuralCandidatesOutcome(java.util.Collections.emptyList(), false, false, null);
    }
  }

  private enum Mode {
    NONE,
    NGRAM,
    NEURAL,
    HYBRID
  }

  private static final class NeuralInferenceSample {
    final long uptimeMs;
    @NonNull final String mode;
    @NonNull final String path;
    final int contextTokensCount;
    final int desiredLimit;
    final long activationLatencyMs;
    final long pipelineLatencyMs;
    final long onnxLatencyMs;
    final boolean usedKvCache;
    final int fullContextTokensCount;
    final int stepTokensCount;
    final int pastLength;

    NeuralInferenceSample(
        long uptimeMs,
        @NonNull String mode,
        @NonNull String path,
        int contextTokensCount,
        int desiredLimit,
        long activationLatencyMs,
        long pipelineLatencyMs,
        long onnxLatencyMs,
        boolean usedKvCache,
        int fullContextTokensCount,
        int stepTokensCount,
        int pastLength) {
      this.uptimeMs = uptimeMs;
      this.mode = mode;
      this.path = path;
      this.contextTokensCount = contextTokensCount;
      this.desiredLimit = desiredLimit;
      this.activationLatencyMs = activationLatencyMs;
      this.pipelineLatencyMs = pipelineLatencyMs;
      this.onnxLatencyMs = onnxLatencyMs;
      this.usedKvCache = usedKvCache;
      this.fullContextTokensCount = fullContextTokensCount;
      this.stepTokensCount = stepTokensCount;
      this.pastLength = pastLength;
    }
  }

  @NonNull private final Context mContext;
  @NonNull private final RxSharedPrefs mPrefs;
  @NonNull private final Handler mMainHandler = new Handler(Looper.getMainLooper());
  @NonNull private final String mLogTagBase;
  private final boolean mEnableDebugLogging;
  private final boolean mEnableTestLogging;

  @NonNull private final PresagePredictionManager mPresagePredictionManager;
  @NonNull private final NeuralPredictionManager mNeuralPredictionManager;
  @NonNull private final PredictionEngine mNgramEngine;
  @NonNull private final NeuralEngineAdapter mNeuralEngine;

  private final ExecutorService mHybridNeuralExecutor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            final Thread t = new Thread(runnable, "NextWordHybridNeural");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
          });

  private final Object mHybridNeuralAsyncLock = new Object();
  private final AtomicLong mHybridNeuralRequestGeneration = new AtomicLong();
  @Nullable private String mHybridNeuralInFlightContextKey;
  private int mHybridNeuralInFlightLimit;
  private long mHybridNeuralInFlightGeneration;
  @Nullable private String mHybridNeuralCachedContextKey;
  private int mHybridNeuralCachedLimit;
  @Nullable private NeuralCandidatesOutcome mHybridNeuralCachedOutcome;
  @Nullable private Runnable mAsyncHybridNeuralListener;

  // Async state for prefix-constrained word completion (#1+#2). Mirrors the hybrid next-word async
  // path and shares its executor + listener, but is keyed by (context, prefix, limit) since the
  // prefix changes every keystroke. Inference never runs on the suggestion thread: getOrSchedule
  // returns a ready cached result or empty, and readiness fires the shared listener to refresh.
  private final Object mCompletionAsyncLock = new Object();
  private final AtomicLong mCompletionRequestGeneration = new AtomicLong();
  @Nullable private String mCompletionInFlightKey;
  private long mCompletionInFlightGeneration;
  @Nullable private String mCompletionCachedKey;
  @Nullable private List<String> mCompletionCachedResult;

  // Best-effort telemetry for HYBRID async neural behavior (used by instrumentation reports).
  @Nullable private String mHybridNeuralLastRequestContextKey;
  private int mHybridNeuralLastRequestLimit;
  private long mHybridNeuralLastRequestGeneration;
  private long mHybridNeuralLastRequestUptimeMs;
  private boolean mHybridNeuralLastRequestCacheHit;
  private boolean mHybridNeuralLastRequestScheduled;
  @Nullable private String mHybridNeuralLastRequestNotScheduledReason;
  @Nullable private String mHybridNeuralLastComputeContextKey;
  private int mHybridNeuralLastComputeLimit;
  private long mHybridNeuralLastComputeGeneration;
  private long mHybridNeuralLastComputeUptimeMs;
  private long mHybridNeuralLastComputeLatencyMs;
  private boolean mHybridNeuralLastComputeHadRaw;
  private boolean mHybridNeuralLastComputeHadNormalized;
  private int mHybridNeuralLastComputeCandidatesCount;
  private int mHybridNeuralAsyncListenerInvocations;
  @Nullable private String mHybridNeuralLastListenerContextKey;
  private long mHybridNeuralLastListenerGeneration;
  private long mHybridNeuralLastListenerUptimeMs;
  @Nullable private String mHybridNeuralLastListenerSuppressedReason;

  // Best-effort per-inference samples for test reports (tap-chain latency / KV-cache visibility).
  private static final int NEURAL_INFERENCE_SAMPLES_MAX_FOR_TEST = 256;
  private final Object mNeuralInferenceSamplesLock = new Object();

  @NonNull
  private final ArrayDeque<NeuralInferenceSample> mNeuralInferenceSamplesForTest =
      new ArrayDeque<>();

  // Best-effort candidate source breakdown for instrumentation reports.
  private final Object mCandidateSourcesLock = new Object();
  @NonNull private String mLastCandidatesModeForTest = "none";
  @NonNull private List<String> mLastPresageCandidatesForTest = java.util.Collections.emptyList();

  @NonNull private List<String> mLastNeuralCandidatesForTest = java.util.Collections.emptyList();

  @NonNull private volatile Mode mMode = Mode.HYBRID;

  @NonNull
  private final ArrayDeque<String> mPresageContext = new ArrayDeque<>(DEFAULT_CONTEXT_WINDOW_WORDS);

  private int mContextWindowWords = DEFAULT_CONTEXT_WINDOW_WORDS;

  private long mLastNeuralLatencyMs;
  @NonNull private Mode mLastNeuralLatencyMode = Mode.NONE;
  private int mHybridNeuralCooldownRemaining;
  private boolean mIncognitoMode;
  @Nullable private NeuralPredictionManager.NextWordScoringContext mLastNeuralScoringContext;

  public NextWordPredictionEngines(
      @NonNull Context context,
      @NonNull RxSharedPrefs prefs,
      @NonNull String logTagBase,
      boolean enableDebugLogging,
      boolean enableTestLogging) {
    mContext = context.getApplicationContext();
    mPrefs = prefs;
    mLogTagBase = logTagBase;
    mEnableDebugLogging = enableDebugLogging;
    mEnableTestLogging = enableTestLogging;
    mPresagePredictionManager = new PresagePredictionManager(mContext);
    mNeuralPredictionManager = new NeuralPredictionManager(mContext);
    mNgramEngine = new PresageEngineAdapter(mPresagePredictionManager);
    mNeuralEngine = new NeuralEngineAdapter(mNeuralPredictionManager);
  }

  public void close() {
    mPresagePredictionManager.deactivate();
    scheduleNeuralDeactivate(/* force= */ true);
    mPresageContext.clear();
    mHybridNeuralCooldownRemaining = 0;
    mLastNeuralLatencyMs = 0L;
    mLastNeuralLatencyMode = Mode.NONE;
    mLastNeuralScoringContext = null;
    clearHybridNeuralAsyncState();
  }

  /**
   * Deactivates prediction engines but preserves the in-memory context window.
   *
   * <p>This is used when dictionaries are closed for resource management (for example, when the IME
   * is hidden) but we still want to resume next-word suggestions without losing context in editors
   * where text readback is blocked.
   */
  public void hibernate() {
    mPresagePredictionManager.deactivate();
    scheduleNeuralDeactivate(/* force= */ true);
    mHybridNeuralCooldownRemaining = 0;
    mLastNeuralLatencyMs = 0L;
    mLastNeuralLatencyMode = Mode.NONE;
    mLastNeuralScoringContext = null;
    clearHybridNeuralAsyncState();
  }

  public void resetSentence() {
    mPresageContext.clear();
    mHybridNeuralCooldownRemaining = 0;
    mLastNeuralScoringContext = null;
    clearHybridNeuralAsyncState();
  }

  public void setIncognitoMode(boolean incognitoMode) {
    if (mIncognitoMode != incognitoMode) {
      mPresageContext.clear();
      mHybridNeuralCooldownRemaining = 0;
    }
    mIncognitoMode = incognitoMode;
    clearHybridNeuralAsyncState();
  }

  public boolean isPresageEnabled() {
    return usesPresageEngine();
  }

  public void setContextWindowWords(int contextWindowWords) {
    final int clamped =
        Math.max(CONTEXT_WINDOW_WORDS_MIN, Math.min(CONTEXT_WINDOW_WORDS_MAX, contextWindowWords));
    if (mContextWindowWords == clamped) {
      return;
    }
    mContextWindowWords = clamped;
    trimContextToWindow();
  }

  /** Returns true when the neural predictor should be considered active in the pipeline. */
  public boolean isNeuralEnabled() {
    return mMode == Mode.NEURAL || mMode == Mode.HYBRID;
  }

  /**
   * Best-effort warm-up for the neural engine when the current mode requires it.
   *
   * <p>This is useful after dictionary close/hibernate cycles: the mode preference may not change,
   * but the neural engine could have been deactivated for resource management. Warming it up
   * eagerly helps avoid a user-visible "cold start" on the first next-word request.
   */
  public void warmUpNeuralIfNeeded() {
    activateNeuralIfNeeded();
  }

  public void updatePredictionEngine(@NonNull String modeValue) {
    switch (modeValue) {
      case "ngram":
        mMode = Mode.NGRAM;
        activatePresageIfNeeded();
        scheduleNeuralDeactivate(/* force= */ false);
        break;
      case "neural":
        mMode = Mode.NEURAL;
        mPresagePredictionManager.deactivate();
        activateNeuralIfNeeded();
        break;
      case "hybrid":
        mMode = Mode.HYBRID;
        activatePresageIfNeeded();
        activateNeuralIfNeeded();
        break;
      default:
        mMode = Mode.NONE;
        mPresagePredictionManager.deactivate();
        scheduleNeuralDeactivate(/* force= */ false);
        mPresageContext.clear();
        break;
    }
    if (mMode == Mode.NONE) {
      mPresageContext.clear();
    }
    if (mMode != Mode.HYBRID) {
      mHybridNeuralCooldownRemaining = 0;
    }
    if (mMode != Mode.HYBRID) {
      clearHybridNeuralAsyncState();
    }
    Logger.i(mLogTagBase, "Prediction engine set to " + mMode);
  }

  public void activatePresageIfNeeded() {
    if (usesPresageEngine()) {
      mNgramEngine.activate();
    }
  }

  public void notifyWordCommitted(@NonNull String committedWord) {
    recordPresageContext(committedWord);
    if (mMode == Mode.NONE) {
      mPresageContext.clear();
    }
    // The committed word becomes context; the prior per-word completion KV/cache is now stale.
    invalidateNeuralContextCache();
  }

  /**
   * Seeds the prediction context window from existing editor text.
   *
   * <p>This must <b>not</b> perform any learning or persist user data. It only initializes the
   * in-memory context window used by next-word prediction engines.
   */
  public void seedContextTokens(@NonNull Collection<String> contextTokens) {
    mPresageContext.clear();
    mHybridNeuralCooldownRemaining = 0;
    mLastNeuralScoringContext = null;
    clearHybridNeuralAsyncState();
    if (contextTokens.isEmpty()) {
      return;
    }
    for (String token : contextTokens) {
      if (token == null) continue;
      recordPresageContext(token.trim());
    }
    if (mMode == Mode.NONE) {
      mPresageContext.clear();
    }
  }

  public Outcome appendNextWords(
      @NonNull String currentWord,
      @NonNull Collection<CharSequence> suggestionsHolder,
      int maxSuggestions,
      boolean incognitoMode,
      int maxNextWordSuggestionsCount) {
    mLastNeuralScoringContext = null;
    resetCandidateSourcesForTest();
    if (mMode == Mode.HYBRID) {
      return appendHybridNextWords(suggestionsHolder, maxSuggestions, maxNextWordSuggestionsCount);
    }

    int remainingSuggestions = maxSuggestions;

    boolean presageHadNormalized = false;
    int presageAdded = 0;
    if (usesPresageEngine()) {
      final EngineOrchestrator.MergeOutcome outcome =
          appendPresageSuggestions(
              suggestionsHolder, remainingSuggestions, maxNextWordSuggestionsCount);
      presageHadNormalized = outcome.hadNormalized;
      presageAdded = outcome.added;
      remainingSuggestions -= outcome.added;
      if (remainingSuggestions <= 0) {
        return new Outcome(maxSuggestions, false);
      }
    }

    boolean neuralHadNormalized = false;
    int neuralAdded = 0;
    if (mMode == Mode.NEURAL) {
      if (mEnableDebugLogging) {
        final String[] ctx = mPresageContext.toArray(new String[0]);
        Logger.d(
            mLogTagBase,
            "Invoking neural next-word with context %s, limit %d",
            Arrays.toString(ctx),
            remainingSuggestions);
      }
      final NeuralCandidatesOutcome neuralOutcome =
          collectHybridNeuralCandidatesAsync(
              remainingSuggestions,
              maxNextWordSuggestionsCount,
              /* hasFallbackSuggestions= */ !suggestionsHolder.isEmpty());
      neuralHadNormalized = neuralOutcome.hadNormalized;
      neuralAdded =
          CandidateMerger.mergeUnique(
              suggestionsHolder, neuralOutcome.candidates, /* limit= */ remainingSuggestions);
      remainingSuggestions -= neuralAdded;
      if (remainingSuggestions <= 0) {
        return new Outcome(maxSuggestions, false);
      }
    }

    final boolean shouldIncludeLegacyNextWords;
    switch (mMode) {
      case NGRAM:
        shouldIncludeLegacyNextWords = !presageHadNormalized;
        break;
      case NEURAL:
        shouldIncludeLegacyNextWords = !neuralHadNormalized;
        break;
      case HYBRID:
        shouldIncludeLegacyNextWords = !(presageHadNormalized || neuralHadNormalized);
        break;
      case NONE:
      default:
        shouldIncludeLegacyNextWords = true;
        break;
    }

    return new Outcome(maxSuggestions - remainingSuggestions, shouldIncludeLegacyNextWords);
  }

  private Outcome appendHybridNextWords(
      @NonNull Collection<CharSequence> suggestionsHolder,
      int maxSuggestions,
      int maxNextWordSuggestionsCount) {
    if (maxSuggestions <= 0) {
      return new Outcome(0, true);
    }
    if (mPresageContext.isEmpty()) {
      return new Outcome(0, true);
    }

    final int perEngineLimit = Math.min(maxSuggestions, maxNextWordSuggestionsCount);

    final EngineOrchestrator.CandidatesOutcome presageOutcome =
        collectPresageCandidates(perEngineLimit, maxNextWordSuggestionsCount);
    final NeuralCandidatesOutcome neuralOutcome =
        collectHybridNeuralCandidatesAsync(
            perEngineLimit,
            maxNextWordSuggestionsCount,
            /* hasFallbackSuggestions= */ !presageOutcome.candidates.isEmpty());

    final List<String> presageCandidates;
    if (neuralOutcome.hadNormalized
        && neuralOutcome.scoringContext != null
        && presageOutcome.candidates.size() > HYBRID_PRESAGE_MAX_CANDIDATES_WHEN_NEURAL_ACTIVE) {
      final List<String> sortedPresage =
          mNeuralPredictionManager.sortCandidatesByFirstTokenLogProb(
              neuralOutcome.scoringContext, presageOutcome.candidates);
      presageCandidates =
          new ArrayList<>(
              sortedPresage.subList(0, HYBRID_PRESAGE_MAX_CANDIDATES_WHEN_NEURAL_ACTIVE));
    } else {
      presageCandidates = presageOutcome.candidates;
    }

    final List<String> union = interleaveNeuralFirst(neuralOutcome.candidates, presageCandidates);
    final List<String> rerankedUnion = CandidateReranker.rerank(mPresageContext, union);
    final int added = CandidateMerger.mergeUnique(suggestionsHolder, rerankedUnion, maxSuggestions);

    final boolean shouldIncludeLegacyNextWords =
        !(presageOutcome.hadNormalized || neuralOutcome.hadNormalized);
    return new Outcome(added, shouldIncludeLegacyNextWords);
  }

  private EngineOrchestrator.CandidatesOutcome collectPresageCandidates(
      int limit, int maxNextWordSuggestionsCount) {
    if (limit <= 0) return EngineOrchestrator.CandidatesOutcome.empty();
    if (!mNgramEngine.isReady() && !mNgramEngine.activate()) {
      return EngineOrchestrator.CandidatesOutcome.empty();
    }
    if (mPresageContext.isEmpty()) {
      return EngineOrchestrator.CandidatesOutcome.empty();
    }
    final EngineOrchestrator.CandidatesOutcome outcome =
        EngineOrchestrator.predictCandidates(
            mNgramEngine,
            mPresageContext,
            maxNextWordSuggestionsCount,
            limit,
            mEnableTestLogging,
            mLogTagBase + "-Presage");
    recordPresageCandidatesForTest(outcome.candidates);
    return outcome;
  }

  private NeuralCandidatesOutcome collectNeuralCandidates(
      int limit, int maxNextWordSuggestionsCount, boolean hasFallbackSuggestions) {
    if (limit <= 0) {
      return NeuralCandidatesOutcome.empty();
    }
    if (mPresageContext.size() < NEURAL_MIN_CONTEXT_TOKENS) {
      return NeuralCandidatesOutcome.empty();
    }
    if (mLastNeuralLatencyMode == Mode.HYBRID
        && mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS
        && mHybridNeuralCooldownRemaining > 0
        && hasFallbackSuggestions) {
      mHybridNeuralCooldownRemaining--;
      Logger.d(
          mLogTagBase,
          "Skipping neural cascade; last inference "
              + mLastNeuralLatencyMs
              + "ms exceeded budget. Cooldown remaining: "
              + mHybridNeuralCooldownRemaining);
      return NeuralCandidatesOutcome.empty();
    }
    if (!mNeuralEngine.isReady() && !mNeuralEngine.activate()) {
      handleNeuralActivationFailure();
      return NeuralCandidatesOutcome.empty();
    }
    final int desiredLimit = Math.min(limit, maxNextWordSuggestionsCount);
    final long start = SystemClock.elapsedRealtime();
    final NeuralPredictionManager.NextWordPredictions prediction =
        mNeuralPredictionManager.predictNextWordsWithScoringContext(
            mPresageContext.toArray(new String[0]), desiredLimit);
    mLastNeuralScoringContext = prediction.scoringContext;
    final List<String> raw = prediction.candidates;
    final boolean hadRaw = raw != null && !raw.isEmpty();

    final String lastContextToken = mPresageContext.peekLast();
    final List<String> normalized;
    if (lastContextToken == null || lastContextToken.isEmpty()) {
      normalized = CandidateNormalizer.normalizeForNextWordUx(mPresageContext, raw);
    } else {
      final String disallow = lastContextToken.toLowerCase(java.util.Locale.ROOT);
      normalized =
          CandidateNormalizer.normalizeForNextWordUx(
              mPresageContext, raw, java.util.Collections.singleton(disallow));
    }
    final List<String> candidates;
    final boolean hadNormalized;
    if (normalized.isEmpty()) {
      candidates = java.util.Collections.emptyList();
      hadNormalized = false;
    } else {
      candidates = CandidateReranker.rerank(mPresageContext, normalized);
      hadNormalized = !candidates.isEmpty();
    }

    mLastNeuralLatencyMs = SystemClock.elapsedRealtime() - start;
    mLastNeuralLatencyMode = mMode;
    recordNeuralInferenceSampleForTest(
        /* contextTokensCount= */ mPresageContext.size(),
        /* desiredLimit= */ desiredLimit,
        /* pipelineLatencyMs= */ mLastNeuralLatencyMs,
        /* path= */ "collectNeuralCandidates");
    if (mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS) {
      Logger.i(
          mLogTagBase,
          "Neural inference latency " + mLastNeuralLatencyMs + "ms for current context");
    }
    if (mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS) {
      mHybridNeuralCooldownRemaining = HYBRID_NEURAL_COOLDOWN_PASSES;
    }
    clearNeuralActivationFailureStatus();
    if (!hadRaw && !TextUtils.isEmpty(mNeuralPredictionManager.getLastActivationError())) {
      handleNeuralActivationFailure();
    }
    return new NeuralCandidatesOutcome(
        candidates, hadRaw, hadNormalized, prediction.scoringContext);
  }

  private NeuralCandidatesOutcome collectHybridNeuralCandidatesAsync(
      int limit, int maxNextWordSuggestionsCount, boolean hasFallbackSuggestions) {
    if (limit <= 0) {
      return NeuralCandidatesOutcome.empty();
    }
    if (mPresageContext.size() < NEURAL_MIN_CONTEXT_TOKENS) {
      return NeuralCandidatesOutcome.empty();
    }

    final int desiredLimit = Math.min(limit, maxNextWordSuggestionsCount);
    final String[] contextTokens = mPresageContext.toArray(new String[0]);
    final String contextKey = contextKey(contextTokens);

    final NeuralCandidatesOutcome cached;
    synchronized (mHybridNeuralAsyncLock) {
      final NeuralCandidatesOutcome cachedOutcome = mHybridNeuralCachedOutcome;
      if (cachedOutcome != null
          && contextKey.equals(mHybridNeuralCachedContextKey)
          && mHybridNeuralCachedLimit >= desiredLimit) {
        cached =
            cachedOutcome.candidates.size() <= desiredLimit
                ? cachedOutcome
                : new NeuralCandidatesOutcome(
                    cachedOutcome.candidates.subList(0, desiredLimit),
                    cachedOutcome.hadRaw,
                    cachedOutcome.hadNormalized,
                    cachedOutcome.scoringContext);
      } else {
        cached = null;
      }
    }
    if (cached != null) {
      if (mEnableTestLogging) {
        synchronized (mHybridNeuralAsyncLock) {
          mHybridNeuralLastRequestContextKey = contextKey;
          mHybridNeuralLastRequestLimit = desiredLimit;
          mHybridNeuralLastRequestGeneration = 0L;
          mHybridNeuralLastRequestUptimeMs = SystemClock.uptimeMillis();
          mHybridNeuralLastRequestCacheHit = true;
          mHybridNeuralLastRequestScheduled = false;
          mHybridNeuralLastRequestNotScheduledReason = "cache_hit";
        }
      }
      mLastNeuralScoringContext = cached.scoringContext;
      recordNeuralCandidatesForTest(cached.candidates);
      return cached;
    }

    if (mEnableTestLogging) {
      synchronized (mHybridNeuralAsyncLock) {
        mHybridNeuralLastRequestContextKey = contextKey;
        mHybridNeuralLastRequestLimit = desiredLimit;
        mHybridNeuralLastRequestGeneration = 0L;
        mHybridNeuralLastRequestUptimeMs = SystemClock.uptimeMillis();
        mHybridNeuralLastRequestCacheHit = false;
        mHybridNeuralLastRequestScheduled = false;
        mHybridNeuralLastRequestNotScheduledReason = "not_yet_scheduled";
      }
    }

    if (!hasFallbackSuggestions) {
      // Even when we have no fallback suggestions, avoid blocking UI on neural inference. We'll
      // return empty and rely on legacy sources + an async refresh.
      requestHybridNeuralAsync(contextTokens, contextKey, desiredLimit);
      recordNeuralCandidatesForTest(java.util.Collections.emptyList());
      return NeuralCandidatesOutcome.empty();
    }

    requestHybridNeuralAsync(contextTokens, contextKey, desiredLimit);
    recordNeuralCandidatesForTest(java.util.Collections.emptyList());
    return NeuralCandidatesOutcome.empty();
  }

  private void requestHybridNeuralAsync(
      @NonNull String[] contextTokens, @NonNull String contextKey, int desiredLimit) {
    if (mMode != Mode.HYBRID && mMode != Mode.NEURAL) {
      if (mEnableTestLogging) {
        synchronized (mHybridNeuralAsyncLock) {
          mHybridNeuralLastRequestScheduled = false;
          mHybridNeuralLastRequestNotScheduledReason = "mode_neural_disabled";
        }
      }
      return;
    }

    final long generation;
    synchronized (mHybridNeuralAsyncLock) {
      if (contextKey.equals(mHybridNeuralInFlightContextKey)
          && mHybridNeuralInFlightLimit >= desiredLimit) {
        if (mEnableTestLogging) {
          mHybridNeuralLastRequestScheduled = false;
          mHybridNeuralLastRequestNotScheduledReason = "already_in_flight";
        }
        return;
      }
      if (contextKey.equals(mHybridNeuralCachedContextKey)
          && mHybridNeuralCachedLimit >= desiredLimit
          && mHybridNeuralCachedOutcome != null) {
        if (mEnableTestLogging) {
          mHybridNeuralLastRequestScheduled = false;
          mHybridNeuralLastRequestNotScheduledReason = "already_cached";
        }
        return;
      }
      generation = mHybridNeuralRequestGeneration.incrementAndGet();
      mHybridNeuralInFlightContextKey = contextKey;
      mHybridNeuralInFlightLimit = desiredLimit;
      mHybridNeuralInFlightGeneration = generation;

      if (mEnableTestLogging) {
        mHybridNeuralLastRequestContextKey = contextKey;
        mHybridNeuralLastRequestLimit = desiredLimit;
        mHybridNeuralLastRequestGeneration = generation;
        mHybridNeuralLastRequestUptimeMs = SystemClock.uptimeMillis();
        mHybridNeuralLastRequestCacheHit = false;
        mHybridNeuralLastRequestScheduled = true;
        mHybridNeuralLastRequestNotScheduledReason = null;
      }
    }

    mHybridNeuralExecutor.execute(
        () -> {
          synchronized (mHybridNeuralAsyncLock) {
            // Coalesce bursty requests: if a newer request was scheduled before this runnable
            // started, skip the expensive inference work. We'll only compute for the current
            // in-flight generation/contextKey, and older queued tasks become cheap no-ops.
            if (mHybridNeuralInFlightGeneration != generation
                || !contextKey.equals(mHybridNeuralInFlightContextKey)) {
              return;
            }
          }

          final NeuralCandidatesOutcome outcome =
              computeNeuralCandidatesBlocking(contextTokens, contextKey, desiredLimit, generation);
          mMainHandler.post(
              () -> {
                synchronized (mHybridNeuralAsyncLock) {
                  if (mHybridNeuralInFlightGeneration != generation
                      || !contextKey.equals(mHybridNeuralInFlightContextKey)) {
                    if (mEnableTestLogging) {
                      mHybridNeuralLastListenerSuppressedReason = "stale_result";
                    }
                    return;
                  }
                  mHybridNeuralInFlightContextKey = null;
                  mHybridNeuralInFlightLimit = 0;
                  mHybridNeuralInFlightGeneration = 0L;
                  mHybridNeuralCachedContextKey = contextKey;
                  mHybridNeuralCachedLimit = desiredLimit;
                  mHybridNeuralCachedOutcome = outcome;
                }

                final Runnable listener = mAsyncHybridNeuralListener;
                if (listener == null) {
                  if (mEnableTestLogging) {
                    synchronized (mHybridNeuralAsyncLock) {
                      mHybridNeuralLastListenerSuppressedReason = "listener_null";
                    }
                  }
                  return;
                }
                if (!outcome.hadNormalized) {
                  if (mEnableTestLogging) {
                    synchronized (mHybridNeuralAsyncLock) {
                      mHybridNeuralLastListenerSuppressedReason = "no_normalized_candidates";
                    }
                  }
                  return;
                }
                if (mMode != Mode.HYBRID && mMode != Mode.NEURAL) {
                  if (mEnableTestLogging) {
                    synchronized (mHybridNeuralAsyncLock) {
                      mHybridNeuralLastListenerSuppressedReason = "mode_changed";
                    }
                  }
                  return;
                }
                if (!contextKey.equals(contextKey(mPresageContext.toArray(new String[0])))) {
                  if (mEnableTestLogging) {
                    synchronized (mHybridNeuralAsyncLock) {
                      mHybridNeuralLastListenerSuppressedReason = "context_changed";
                    }
                  }
                  return;
                }
                if (mEnableTestLogging) {
                  synchronized (mHybridNeuralAsyncLock) {
                    mHybridNeuralAsyncListenerInvocations++;
                    mHybridNeuralLastListenerContextKey = contextKey;
                    mHybridNeuralLastListenerGeneration = generation;
                    mHybridNeuralLastListenerUptimeMs = SystemClock.uptimeMillis();
                    mHybridNeuralLastListenerSuppressedReason = null;
                  }
                }
                listener.run();
              });
        });
  }

  private NeuralCandidatesOutcome computeNeuralCandidatesBlocking(
      @NonNull String[] contextTokens,
      @NonNull String contextKey,
      int desiredLimit,
      long generation) {
    if (desiredLimit <= 0) {
      return NeuralCandidatesOutcome.empty();
    }
    if (contextTokens.length < NEURAL_MIN_CONTEXT_TOKENS) {
      return NeuralCandidatesOutcome.empty();
    }
    if (!mNeuralEngine.isReady() && !mNeuralEngine.activate()) {
      handleNeuralActivationFailure();
      if (mEnableTestLogging) {
        synchronized (mHybridNeuralAsyncLock) {
          mHybridNeuralLastComputeContextKey = contextKey;
          mHybridNeuralLastComputeLimit = desiredLimit;
          mHybridNeuralLastComputeGeneration = generation;
          mHybridNeuralLastComputeUptimeMs = SystemClock.uptimeMillis();
          mHybridNeuralLastComputeLatencyMs = 0L;
          mHybridNeuralLastComputeHadRaw = false;
          mHybridNeuralLastComputeHadNormalized = false;
          mHybridNeuralLastComputeCandidatesCount = 0;
        }
      }
      return NeuralCandidatesOutcome.empty();
    }

    final long start = SystemClock.elapsedRealtime();
    final NeuralPredictionManager.NextWordPredictions prediction =
        mNeuralPredictionManager.predictNextWordsWithScoringContext(contextTokens, desiredLimit);
    mLastNeuralLatencyMs = SystemClock.elapsedRealtime() - start;
    mLastNeuralLatencyMode = mMode;
    recordNeuralInferenceSampleForTest(
        /* contextTokensCount= */ contextTokens.length,
        /* desiredLimit= */ desiredLimit,
        /* pipelineLatencyMs= */ mLastNeuralLatencyMs,
        /* path= */ "computeNeuralCandidatesBlocking");
    if (mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS) {
      Logger.i(
          mLogTagBase,
          "Neural inference latency " + mLastNeuralLatencyMs + "ms for current context");
    }

    final List<String> raw = prediction.candidates;
    final boolean hadRaw = raw != null && !raw.isEmpty();

    final java.util.ArrayDeque<String> context =
        new java.util.ArrayDeque<>(java.util.Arrays.asList(contextTokens));
    final String lastContextToken = context.peekLast();
    final List<String> normalized;
    if (lastContextToken == null || lastContextToken.isEmpty()) {
      normalized = CandidateNormalizer.normalizeForNextWordUx(context, raw);
    } else {
      final String disallow = lastContextToken.toLowerCase(java.util.Locale.ROOT);
      normalized =
          CandidateNormalizer.normalizeForNextWordUx(
              context, raw, java.util.Collections.singleton(disallow));
    }
    final List<String> candidates;
    final boolean hadNormalized;
    if (normalized.isEmpty()) {
      candidates = java.util.Collections.emptyList();
      hadNormalized = false;
    } else {
      candidates = CandidateReranker.rerank(context, normalized);
      hadNormalized = !candidates.isEmpty();
    }

    clearNeuralActivationFailureStatus();
    if (!hadRaw && !TextUtils.isEmpty(mNeuralPredictionManager.getLastActivationError())) {
      handleNeuralActivationFailure();
    }
    if (mEnableTestLogging) {
      synchronized (mHybridNeuralAsyncLock) {
        mHybridNeuralLastComputeContextKey = contextKey;
        mHybridNeuralLastComputeLimit = desiredLimit;
        mHybridNeuralLastComputeGeneration = generation;
        mHybridNeuralLastComputeUptimeMs = SystemClock.uptimeMillis();
        mHybridNeuralLastComputeLatencyMs = mLastNeuralLatencyMs;
        mHybridNeuralLastComputeHadRaw = hadRaw;
        mHybridNeuralLastComputeHadNormalized = hadNormalized;
        mHybridNeuralLastComputeCandidatesCount = candidates.size();
      }
    }
    return new NeuralCandidatesOutcome(
        candidates, hadRaw, hadNormalized, prediction.scoringContext);
  }

  private void clearHybridNeuralAsyncState() {
    mHybridNeuralRequestGeneration.incrementAndGet();
    synchronized (mHybridNeuralAsyncLock) {
      mHybridNeuralInFlightContextKey = null;
      mHybridNeuralInFlightLimit = 0;
      mHybridNeuralInFlightGeneration = 0L;
      mHybridNeuralCachedContextKey = null;
      mHybridNeuralCachedLimit = 0;
      mHybridNeuralCachedOutcome = null;
    }
    clearWordCompletionAsyncState();
  }

  private void clearWordCompletionAsyncState() {
    mCompletionRequestGeneration.incrementAndGet();
    synchronized (mCompletionAsyncLock) {
      mCompletionInFlightKey = null;
      mCompletionInFlightGeneration = 0L;
      mCompletionCachedKey = null;
      mCompletionCachedResult = null;
    }
  }

  /**
   * Returns ready prefix-completions for the current context, or schedules an async computation and
   * returns empty. Never runs inference on the calling (suggestion) thread. When a scheduled result
   * becomes ready it fires the shared async listener, prompting a suggestions refresh that picks it
   * up. Falls back to empty (dictionary handles it) when neural is off, context is cold, or the
   * prefix is too short to be worth a model call.
   */
  @NonNull
  public List<String> getOrScheduleWordCompletions(@NonNull String prefix, int maxResults) {
    if (maxResults <= 0 || (mMode != Mode.NEURAL && mMode != Mode.HYBRID)) {
      return java.util.Collections.emptyList();
    }
    final String trimmedPrefix = prefix.trim();
    if (trimmedPrefix.length() < NEURAL_COMPLETION_MIN_PREFIX_LEN) {
      return java.util.Collections.emptyList();
    }
    final String[] contextTokens = mPresageContext.toArray(new String[0]);
    if (contextTokens.length < NEURAL_MIN_CONTEXT_TOKENS) {
      return java.util.Collections.emptyList();
    }
    final String key =
        contextKey(contextTokens)
            + ' '
            + trimmedPrefix.toLowerCase(java.util.Locale.ROOT)
            + ' '
            + maxResults;

    synchronized (mCompletionAsyncLock) {
      if (key.equals(mCompletionCachedKey) && mCompletionCachedResult != null) {
        return new ArrayList<>(mCompletionCachedResult);
      }
    }
    requestWordCompletionAsync(contextTokens, trimmedPrefix, key, maxResults);
    return java.util.Collections.emptyList();
  }

  private void requestWordCompletionAsync(
      @NonNull String[] contextTokens,
      @NonNull String prefix,
      @NonNull String key,
      int maxResults) {
    final long generation;
    synchronized (mCompletionAsyncLock) {
      if (key.equals(mCompletionInFlightKey) || key.equals(mCompletionCachedKey)) {
        return;
      }
      generation = mCompletionRequestGeneration.incrementAndGet();
      mCompletionInFlightKey = key;
      mCompletionInFlightGeneration = generation;
    }

    mHybridNeuralExecutor.execute(
        () -> {
          synchronized (mCompletionAsyncLock) {
            if (mCompletionInFlightGeneration != generation
                || !key.equals(mCompletionInFlightKey)) {
              return; // coalesced away by a newer request
            }
          }
          if (mMode != Mode.NEURAL && mMode != Mode.HYBRID) {
            return;
          }
          if (!mNeuralEngine.isReady() && !mNeuralEngine.activate()) {
            handleNeuralActivationFailure();
            return;
          }
          final List<String> completions =
              mNeuralPredictionManager
                  .completeWordWithScoringContext(contextTokens, prefix, maxResults)
                  .candidates;
          mMainHandler.post(
              () -> {
                final Runnable listener;
                synchronized (mCompletionAsyncLock) {
                  if (mCompletionInFlightGeneration != generation
                      || !key.equals(mCompletionInFlightKey)) {
                    return; // stale
                  }
                  mCompletionInFlightKey = null;
                  mCompletionInFlightGeneration = 0L;
                  mCompletionCachedKey = key;
                  mCompletionCachedResult = completions;
                  listener = mAsyncHybridNeuralListener;
                }
                if (completions.isEmpty() || listener == null) {
                  return;
                }
                if (!key.startsWith(contextKey(mPresageContext.toArray(new String[0])) + ' ')) {
                  return; // context changed underneath us
                }
                listener.run();
              });
        });
  }

  /**
   * Invalidates the per-word neural context KV and any cached completions. Called on word / sentence
   * / field boundaries; the manager cache is content-keyed so this is for promptness, not
   * correctness.
   */
  public void invalidateNeuralContextCache() {
    mNeuralPredictionManager.invalidateContextCache();
    clearWordCompletionAsyncState();
  }

  private void scheduleNeuralDeactivate(boolean force) {
    mHybridNeuralExecutor.execute(
        () -> {
          if (!force && (mMode == Mode.NEURAL || mMode == Mode.HYBRID)) {
            return;
          }
          mNeuralPredictionManager.deactivate();
        });
  }

  static String contextKey(@NonNull String[] contextTokens) {
    if (contextTokens.length == 0) return "";
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < contextTokens.length; i++) {
      if (i > 0) sb.append('\u001F');
      sb.append(contextTokens[i]);
    }
    return sb.toString();
  }

  public void setAsyncHybridNeuralListener(@Nullable Runnable listener) {
    mAsyncHybridNeuralListener = listener;
  }

  @NonNull
  private static List<String> interleaveNeuralFirst(
      @NonNull List<String> neural, @NonNull List<String> presage) {
    if (neural.isEmpty() && presage.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    final List<String> combined = new ArrayList<>(neural.size() + presage.size());

    // Prefer a neural-dominant top row so HYBRID doesn't "feel like n-gram wins by default" when
    // the UI can only show a handful of suggestions.
    int neuralIndex = 0;
    int presageIndex = 0;
    final int neuralHead = Math.min(2, neural.size());
    while (neuralIndex < neuralHead) {
      combined.add(neural.get(neuralIndex++));
    }
    if (presageIndex < presage.size()) {
      combined.add(presage.get(presageIndex++));
    }

    while (combined.size() < neural.size() + presage.size()) {
      if (neuralIndex < neural.size()) combined.add(neural.get(neuralIndex++));
      if (presageIndex < presage.size()) combined.add(presage.get(presageIndex++));
    }
    return combined;
  }

  private boolean usesPresageEngine() {
    return mMode == Mode.NGRAM || mMode == Mode.HYBRID;
  }

  private void recordPresageContext(@NonNull String word) {
    if (TextUtils.isEmpty(word)) return;
    if (mPresageContext.size() == mContextWindowWords) {
      mPresageContext.removeFirst();
    }
    mPresageContext.addLast(word);
  }

  private void trimContextToWindow() {
    while (mPresageContext.size() > mContextWindowWords) {
      mPresageContext.removeFirst();
    }
  }

  private EngineOrchestrator.MergeOutcome appendPresageSuggestions(
      Collection<CharSequence> suggestionsHolder, int limit, int maxNextWordSuggestionsCount) {
    if (limit <= 0) return EngineOrchestrator.MergeOutcome.empty();
    if (!mNgramEngine.isReady() && !mNgramEngine.activate()) {
      return EngineOrchestrator.MergeOutcome.empty();
    }
    if (mPresageContext.isEmpty()) {
      return EngineOrchestrator.MergeOutcome.empty();
    }
    final EngineOrchestrator.CandidatesOutcome outcome =
        collectPresageCandidates(limit, maxNextWordSuggestionsCount);
    return EngineOrchestrator.mergeCandidatesOutcome(outcome, suggestionsHolder, limit);
  }

  private EngineOrchestrator.MergeOutcome appendNeuralSuggestions(
      Collection<CharSequence> suggestionsHolder, int limit, int maxNextWordSuggestionsCount) {
    if (limit <= 0) {
      return EngineOrchestrator.MergeOutcome.empty();
    }
    if (mPresageContext.size() < NEURAL_MIN_CONTEXT_TOKENS) {
      return EngineOrchestrator.MergeOutcome.empty();
    }
    if (mMode == Mode.HYBRID
        && mLastNeuralLatencyMode == Mode.HYBRID
        && mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS
        && mHybridNeuralCooldownRemaining > 0
        && !suggestionsHolder.isEmpty()) {
      mHybridNeuralCooldownRemaining--;
      Logger.d(
          mLogTagBase,
          "Skipping neural cascade; last inference "
              + mLastNeuralLatencyMs
              + "ms exceeded budget. Cooldown remaining: "
              + mHybridNeuralCooldownRemaining);
      return EngineOrchestrator.MergeOutcome.empty();
    }
    if (!mNeuralEngine.isReady() && !mNeuralEngine.activate()) {
      handleNeuralActivationFailure();
      return EngineOrchestrator.MergeOutcome.empty();
    }
    final long start = SystemClock.elapsedRealtime();
    final EngineOrchestrator.MergeOutcome outcome =
        EngineOrchestrator.predictAndMerge(
            mNeuralEngine,
            mPresageContext,
            maxNextWordSuggestionsCount,
            suggestionsHolder,
            limit,
            mEnableTestLogging,
            mLogTagBase + "-Neural");
    mLastNeuralScoringContext = mNeuralEngine.getLastScoringContext();
    mLastNeuralLatencyMs = SystemClock.elapsedRealtime() - start;
    mLastNeuralLatencyMode = mMode;
    recordNeuralInferenceSampleForTest(
        /* contextTokensCount= */ mPresageContext.size(),
        /* desiredLimit= */ limit,
        /* pipelineLatencyMs= */ mLastNeuralLatencyMs,
        /* path= */ "appendNeuralSuggestions");
    if (mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS) {
      Logger.i(
          mLogTagBase,
          "Neural inference latency " + mLastNeuralLatencyMs + "ms for current context");
    }
    if (mMode == Mode.HYBRID && mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS) {
      mHybridNeuralCooldownRemaining = HYBRID_NEURAL_COOLDOWN_PASSES;
    }
    clearNeuralActivationFailureStatus();
    if (outcome.added == 0
        && !outcome.hadRaw
        && !TextUtils.isEmpty(mNeuralPredictionManager.getLastActivationError())) {
      handleNeuralActivationFailure();
    }
    return outcome;
  }

  private void activateNeuralIfNeeded() {
    if (mMode == Mode.NEURAL || mMode == Mode.HYBRID) {
      mHybridNeuralExecutor.execute(
          () -> {
            if (mMode != Mode.NEURAL && mMode != Mode.HYBRID) return;
            if (!mNeuralEngine.isReady() && !mNeuralEngine.activate()) {
              handleNeuralActivationFailure();
            }
          });
    }
  }

  private void handleNeuralActivationFailure() {
    final String error = mNeuralPredictionManager.getLastActivationError();
    persistNeuralActivationFailure(error);
    Logger.w(mLogTagBase, "Neural prediction engine failed to activate: " + error);
    final String message =
        mContext.getString(
            R.string.prediction_engine_neural_activation_failed,
            TextUtils.isEmpty(error)
                ? mContext.getString(R.string.prediction_engine_error_unknown)
                : error);
    mMainHandler.post(() -> Toast.makeText(mContext, message, Toast.LENGTH_LONG).show());

    if (mMode == Mode.NEURAL || mMode == Mode.HYBRID) {
      final String currentModeValue = modeToPreferenceValue(mMode);
      if (!"ngram".equals(currentModeValue)) {
        updatePredictionEnginePreference("ngram");
      } else {
        updatePredictionEnginePreference("none");
      }
    }
  }

  private void updatePredictionEnginePreference(@NonNull String newMode) {
    mPrefs
        .getString(
            R.string.settings_key_prediction_engine_mode,
            R.string.settings_default_prediction_engine_mode)
        .set(newMode);
  }

  private void persistNeuralActivationFailure(@Nullable String rawError) {
    final String sanitized =
        TextUtils.isEmpty(rawError)
            ? mContext.getString(R.string.prediction_engine_error_unknown)
            : sanitizeNeuralFailureMessage(rawError);
    final String value;
    if (TextUtils.isEmpty(sanitized)) {
      value = "";
    } else {
      value = System.currentTimeMillis() + String.valueOf(NEURAL_FAILURE_DELIMITER) + sanitized;
    }
    mPrefs
        .getString(
            R.string.settings_key_prediction_engine_last_neural_error,
            R.string.settings_default_prediction_engine_last_neural_error)
        .set(value);
  }

  private void clearNeuralActivationFailureStatus() {
    mPrefs
        .getString(
            R.string.settings_key_prediction_engine_last_neural_error,
            R.string.settings_default_prediction_engine_last_neural_error)
        .set("");
  }

  private String sanitizeNeuralFailureMessage(@Nullable String rawError) {
    if (TextUtils.isEmpty(rawError)) {
      return "";
    }
    return rawError.replace(String.valueOf(NEURAL_FAILURE_DELIMITER), " ").trim();
  }

  private String modeToPreferenceValue(@NonNull Mode mode) {
    switch (mode) {
      case NGRAM:
        return "ngram";
      case NEURAL:
        return "neural";
      case HYBRID:
        return "hybrid";
      case NONE:
      default:
        return "none";
    }
  }

  @VisibleForTesting
  String[] getContextTokensForTests() {
    return mPresageContext.toArray(new String[0]);
  }

  @VisibleForTesting
  public int getHybridNeuralAsyncListenerInvocationCountForTest() {
    if (!mEnableTestLogging) return 0;
    synchronized (mHybridNeuralAsyncLock) {
      return mHybridNeuralAsyncListenerInvocations;
    }
  }

  @VisibleForTesting
  public void resetNeuralInferenceSamplesForTest() {
    if (!mEnableTestLogging) return;
    synchronized (mNeuralInferenceSamplesLock) {
      mNeuralInferenceSamplesForTest.clear();
    }
  }

  @VisibleForTesting
  public static final class EngineCandidateSourcesForTest {
    @NonNull public final String mode;
    @NonNull public final List<String> presageCandidates;
    @NonNull public final List<String> neuralCandidates;

    EngineCandidateSourcesForTest(
        @NonNull String mode,
        @NonNull List<String> presageCandidates,
        @NonNull List<String> neuralCandidates) {
      this.mode = mode;
      this.presageCandidates = presageCandidates;
      this.neuralCandidates = neuralCandidates;
    }
  }

  @VisibleForTesting
  @NonNull
  public EngineCandidateSourcesForTest snapshotEngineCandidateSourcesForTest() {
    if (!mEnableTestLogging) {
      return new EngineCandidateSourcesForTest(
          modeToPreferenceValue(mMode),
          java.util.Collections.emptyList(),
          java.util.Collections.emptyList());
    }
    synchronized (mCandidateSourcesLock) {
      return new EngineCandidateSourcesForTest(
          mLastCandidatesModeForTest,
          new java.util.ArrayList<>(mLastPresageCandidatesForTest),
          new java.util.ArrayList<>(mLastNeuralCandidatesForTest));
    }
  }

  private void resetCandidateSourcesForTest() {
    if (!mEnableTestLogging) return;
    synchronized (mCandidateSourcesLock) {
      mLastCandidatesModeForTest = modeToPreferenceValue(mMode);
      mLastPresageCandidatesForTest = java.util.Collections.emptyList();
      mLastNeuralCandidatesForTest = java.util.Collections.emptyList();
    }
  }

  private void recordPresageCandidatesForTest(@NonNull List<String> candidates) {
    if (!mEnableTestLogging) return;
    synchronized (mCandidateSourcesLock) {
      mLastPresageCandidatesForTest =
          candidates.isEmpty()
              ? java.util.Collections.emptyList()
              : new java.util.ArrayList<>(candidates);
    }
  }

  private void recordNeuralCandidatesForTest(@NonNull List<String> candidates) {
    if (!mEnableTestLogging) return;
    synchronized (mCandidateSourcesLock) {
      mLastNeuralCandidatesForTest =
          candidates.isEmpty()
              ? java.util.Collections.emptyList()
              : new java.util.ArrayList<>(candidates);
    }
  }

  @VisibleForTesting
  @NonNull
  public String dumpNeuralInferenceSamplesForTest() {
    if (!mEnableTestLogging) return "[]";
    try {
      final JSONArray out = new JSONArray();
      synchronized (mNeuralInferenceSamplesLock) {
        for (NeuralInferenceSample s : mNeuralInferenceSamplesForTest) {
          final JSONObject row = new JSONObject();
          row.put("uptimeMs", s.uptimeMs);
          row.put("mode", s.mode);
          row.put("path", s.path);
          row.put("contextTokensCount", s.contextTokensCount);
          row.put("desiredLimit", s.desiredLimit);
          row.put("activationLatencyMs", s.activationLatencyMs);
          row.put("pipelineLatencyMs", s.pipelineLatencyMs);
          row.put("onnxLatencyMs", s.onnxLatencyMs);
          row.put("usedKvCache", s.usedKvCache);
          row.put("fullContextTokensCount", s.fullContextTokensCount);
          row.put("stepTokensCount", s.stepTokensCount);
          row.put("pastLength", s.pastLength);
          out.put(row);
        }
      }
      return out.toString();
    } catch (Exception e) {
      return "[]";
    }
  }

  @VisibleForTesting
  @NonNull
  public String dumpHybridNeuralAsyncDebugStateForTest() {
    if (!mEnableTestLogging) return "{}";
    try {
      final JSONObject root = new JSONObject();
      root.put("mode", modeToPreferenceValue(mMode));

      final String[] ctx = mPresageContext.toArray(new String[0]);
      root.put("contextTokensCount", ctx.length);
      root.put("contextKey", contextKey(ctx));

      root.put("lastNeuralLatencyMs", mLastNeuralLatencyMs);
      root.put("lastNeuralLatencyMode", modeToPreferenceValue(mLastNeuralLatencyMode));

      synchronized (mHybridNeuralAsyncLock) {
        root.put("inFlightContextKey", mHybridNeuralInFlightContextKey);
        root.put("inFlightLimit", mHybridNeuralInFlightLimit);
        root.put("inFlightGeneration", mHybridNeuralInFlightGeneration);
        root.put("cachedContextKey", mHybridNeuralCachedContextKey);
        root.put("cachedLimit", mHybridNeuralCachedLimit);
        root.put(
            "cachedHadNormalized",
            mHybridNeuralCachedOutcome != null && mHybridNeuralCachedOutcome.hadNormalized);

        root.put("lastRequestContextKey", mHybridNeuralLastRequestContextKey);
        root.put("lastRequestLimit", mHybridNeuralLastRequestLimit);
        root.put("lastRequestGeneration", mHybridNeuralLastRequestGeneration);
        root.put("lastRequestUptimeMs", mHybridNeuralLastRequestUptimeMs);
        root.put("lastRequestCacheHit", mHybridNeuralLastRequestCacheHit);
        root.put("lastRequestScheduled", mHybridNeuralLastRequestScheduled);
        root.put("lastRequestNotScheduledReason", mHybridNeuralLastRequestNotScheduledReason);

        root.put("lastComputeContextKey", mHybridNeuralLastComputeContextKey);
        root.put("lastComputeLimit", mHybridNeuralLastComputeLimit);
        root.put("lastComputeGeneration", mHybridNeuralLastComputeGeneration);
        root.put("lastComputeUptimeMs", mHybridNeuralLastComputeUptimeMs);
        root.put("lastComputeLatencyMs", mHybridNeuralLastComputeLatencyMs);
        root.put("lastComputeHadRaw", mHybridNeuralLastComputeHadRaw);
        root.put("lastComputeHadNormalized", mHybridNeuralLastComputeHadNormalized);
        root.put("lastComputeCandidatesCount", mHybridNeuralLastComputeCandidatesCount);

        root.put("listenerInvocationCount", mHybridNeuralAsyncListenerInvocations);
        root.put("lastListenerContextKey", mHybridNeuralLastListenerContextKey);
        root.put("lastListenerGeneration", mHybridNeuralLastListenerGeneration);
        root.put("lastListenerUptimeMs", mHybridNeuralLastListenerUptimeMs);
        root.put("lastListenerSuppressedReason", mHybridNeuralLastListenerSuppressedReason);
      }

      final String activationError = mNeuralPredictionManager.getLastActivationError();
      if (!TextUtils.isEmpty(activationError)) {
        root.put("neuralActivationError", activationError);
      }
      root.put(
          "neuralLastActivationLatencyMs",
          mNeuralPredictionManager.getLastActivationLatencyMsForTest());
      root.put(
          "neuralLastActivationUptimeMs",
          mNeuralPredictionManager.getLastActivationUptimeMsForTest());
      return root.toString();
    } catch (Exception e) {
      return "{}";
    }
  }

  private void recordNeuralInferenceSampleForTest(
      int contextTokensCount, int desiredLimit, long pipelineLatencyMs, @NonNull String path) {
    if (!mEnableTestLogging) return;

    final NeuralPredictionManager.LastInferenceDebugState debug =
        mNeuralPredictionManager.getLastInferenceDebugStateForTest();

    final long activationLatencyMs = debug == null ? -1L : debug.activationLatencyMs;
    final long onnxLatencyMs = debug == null ? -1L : debug.onnxLatencyMs;
    final boolean usedKvCache = debug != null && debug.usedKvCache;
    final int fullContextTokensCount = debug == null ? 0 : debug.fullContextTokensCount;
    final int stepTokensCount = debug == null ? 0 : debug.stepTokensCount;
    final int pastLength = debug == null ? 0 : debug.pastLength;
    final long uptimeMs = debug == null ? SystemClock.uptimeMillis() : debug.uptimeMs;

    final NeuralInferenceSample sample =
        new NeuralInferenceSample(
            uptimeMs,
            modeToPreferenceValue(mMode),
            path,
            contextTokensCount,
            desiredLimit,
            activationLatencyMs,
            pipelineLatencyMs,
            onnxLatencyMs,
            usedKvCache,
            fullContextTokensCount,
            stepTokensCount,
            pastLength);

    synchronized (mNeuralInferenceSamplesLock) {
      mNeuralInferenceSamplesForTest.add(sample);
      while (mNeuralInferenceSamplesForTest.size() > NEURAL_INFERENCE_SAMPLES_MAX_FOR_TEST) {
        mNeuralInferenceSamplesForTest.pollFirst();
      }
    }
  }

  @NonNull
  public java.util.Deque<String> getContextSnapshot() {
    return new java.util.ArrayDeque<>(mPresageContext);
  }

  @NonNull
  public List<String> sortCandidatesByNeuralFirstTokenLogProbIfAvailable(
      @NonNull List<String> candidates) {
    final NeuralPredictionManager.NextWordScoringContext scoringContext = mLastNeuralScoringContext;
    if (scoringContext == null) {
      return candidates;
    }
    return mNeuralPredictionManager.sortCandidatesByFirstTokenLogProb(scoringContext, candidates);
  }

  @VisibleForTesting
  static void interleaveHybridSuggestions(
      @NonNull List<CharSequence> suggestions, int startIndex, int presageCount, int neuralCount) {
    if (presageCount <= 0 || neuralCount <= 0) return;
    if (startIndex < 0 || startIndex >= suggestions.size()) return;
    final int expectedEnd = startIndex + presageCount + neuralCount;
    if (expectedEnd > suggestions.size()) return;

    final List<CharSequence> presage = new ArrayList<>(presageCount);
    for (int i = 0; i < presageCount; i++) {
      presage.add(suggestions.get(startIndex + i));
    }
    final List<CharSequence> neural = new ArrayList<>(neuralCount);
    for (int i = 0; i < neuralCount; i++) {
      neural.add(suggestions.get(startIndex + presageCount + i));
    }

    final List<CharSequence> combined = new ArrayList<>(presageCount + neuralCount);
    for (int i = 0; combined.size() < presageCount + neuralCount; i++) {
      if (i < neural.size()) combined.add(neural.get(i));
      if (i < presage.size()) combined.add(presage.get(i));
    }
    for (int i = 0; i < combined.size(); i++) {
      suggestions.set(startIndex + i, combined.get(i));
    }
  }
}
