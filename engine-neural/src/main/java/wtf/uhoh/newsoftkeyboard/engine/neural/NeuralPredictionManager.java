package wtf.uhoh.newsoftkeyboard.engine.neural;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.ValueInfo;
import android.content.Context;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore.ActiveModel;

/** Handles ONNX Runtime backed next-word predictions. */
public final class NeuralPredictionManager {

  private static final String TAG = "NeuralPredictionManager";
  private static final boolean ENABLE_TEST_LOGS = resolveTestLogsEnabled();
  private static final int MAX_CONTEXT_TOKENS = 64;
  // A keyboard shares the device with the host app; keep ORT's intra-op pool small to avoid
  // oversubscribing cores during typing. XNNPACK, when enabled, owns its own pool of this size.
  private static final int SESSION_INTRA_OP_THREADS = 2;
  private static final int WORD_EXPANSION_MAX_EXTRA_TOKENS = 3;
  private static final int WORD_EXPANSION_MAX_CANDIDATES = 1;
  private static final int WORD_EXPANSION_SEARCH_WINDOW = 64;
  private static final int WORD_EXPANSION_MIN_LEN = 8;
  private static final int WORD_EXPANSION_NO_PAST_MAX_EXTRA_TOKENS = 2;
  private static final int WORD_EXPANSION_NO_PAST_MAX_CANDIDATES = 1;
  private static final int WORD_CANDIDATE_SEARCH_WINDOW_MIN = 256;
  private static final int WORD_CANDIDATE_SEARCH_WINDOW_MAX = 1024;
  private static final int WORD_CANDIDATE_SEARCH_WINDOW_MULTIPLIER = 40;
  private static final float WORD_CONTINUATION_LOGIT_DELTA_MAX = 0.75f;
  private static final java.util.Set<String> SOFT_CONTINUATION_DISALLOW_WORDS =
      java.util.Collections.unmodifiableSet(
          new java.util.HashSet<>(
              java.util.Arrays.asList(
                  "a", "an", "and", "the", "i", "to", "of", "in", "on", "at", "by", "as", "or",
                  "if", "is", "it", "be", "we", "me", "my", "he", "she", "us", "do", "go", "no",
                  "so", "up")));
  private static final java.util.Set<String> DOMAIN_LIKE_TOKENS =
      java.util.Collections.unmodifiableSet(
          new java.util.HashSet<>(java.util.Arrays.asList("com", "http", "https", "www")));
  private static final Pattern PAST_KEY_VALUE_PATTERN =
      Pattern.compile("past_key_values\\.(\\d+)\\.(key|value)");

  private static boolean resolveTestLogsEnabled() {
    final String javaProperty = System.getProperty("NSK_TEST_LOGS");
    if (javaProperty != null && !javaProperty.trim().isEmpty()) {
      return Boolean.parseBoolean(javaProperty);
    }

    final String androidProperty = readAndroidSystemProperty("NSK_TEST_LOGS");
    if (androidProperty == null) {
      return false;
    }

    final String value = androidProperty.trim();
    if (value.isEmpty()) {
      return false;
    }
    return "1".equals(value)
        || "true".equalsIgnoreCase(value)
        || "y".equalsIgnoreCase(value)
        || "yes".equalsIgnoreCase(value);
  }

  @Nullable
  private static String readAndroidSystemProperty(@NonNull String key) {
    try {
      final Class<?> systemProperties = Class.forName("android.os.SystemProperties");
      final java.lang.reflect.Method get =
          systemProperties.getMethod("get", String.class, String.class);
      final Object value = get.invoke(null, key, "");
      return value instanceof String ? (String) value : null;
    } catch (Throwable ignore) {
      return null;
    }
  }

  public static final class NextWordScoringContext {
    @NonNull public final float[] lastLogits;
    public final float logSumExp;

    private NextWordScoringContext(@NonNull float[] lastLogits, float logSumExp) {
      this.lastLogits = lastLogits;
      this.logSumExp = logSumExp;
    }
  }

  public static final class LastInferenceDebugState {
    public final long uptimeMs;
    public final long activationLatencyMs;
    public final int fullContextTokensCount;
    public final int stepTokensCount;
    public final int pastLength;
    public final boolean usedKvCache;
    public final long onnxLatencyMs;

    private LastInferenceDebugState(
        long uptimeMs,
        long activationLatencyMs,
        int fullContextTokensCount,
        int stepTokensCount,
        int pastLength,
        boolean usedKvCache,
        long onnxLatencyMs) {
      this.uptimeMs = uptimeMs;
      this.activationLatencyMs = activationLatencyMs;
      this.fullContextTokensCount = fullContextTokensCount;
      this.stepTokensCount = stepTokensCount;
      this.pastLength = pastLength;
      this.usedKvCache = usedKvCache;
      this.onnxLatencyMs = onnxLatencyMs;
    }
  }

  public static final class NextWordPredictions {
    @NonNull public final List<String> candidates;
    @Nullable public final NextWordScoringContext scoringContext;

    private NextWordPredictions(
        @NonNull List<String> candidates, @Nullable NextWordScoringContext scoringContext) {
      this.candidates = candidates;
      this.scoringContext = scoringContext;
    }

    public static NextWordPredictions empty() {
      return new NextWordPredictions(java.util.Collections.emptyList(), null);
    }
  }

  private final Context mContext;
  private final ModelStore mModelStore;
  private final ReentrantLock mSessionLock = new ReentrantLock();

  @Nullable private ActiveModel mActiveModel;
  @Nullable private OrtEnvironment mEnvironment;
  @Nullable private OrtSession.SessionOptions mSessionOptions;
  @Nullable private OrtSession mSession;
  @Nullable private Gpt2Tokenizer mTokenizer;
  @Nullable private String mLastActivationError;
  private long mLastActivationLatencyMs = -1L;
  private long mLastActivationUptimeMs = 0L;
  // Which execution provider the last activation actually used ("xnnpack" or "cpu"); for telemetry
  // and tests. XNNPACK is preferred but falls back to plain CPU when unavailable on the device.
  @NonNull private volatile String mLastSessionProvider = "cpu";
  @Nullable private java.util.Set<String> mSessionInputNames;
  @Nullable private java.util.Set<String> mSessionOutputNames;

  private final List<String> mPastKeyValueInputNames = new ArrayList<>();
  private final Map<String, long[]> mPastKeyValueInputShapes = new HashMap<>();
  private final Map<String, ai.onnxruntime.OnnxJavaType> mPastKeyValueInputTypes = new HashMap<>();
  private int mModelVocabSize = 0;

  private static final class KvCacheState {
    @NonNull final int[] encodedContext;
    @NonNull final OrtSession.Result result;
    @NonNull final Map<String, OnnxTensor> pastTensorsByInputName;

    KvCacheState(
        @NonNull int[] encodedContext,
        @NonNull OrtSession.Result result,
        @NonNull Map<String, OnnxTensor> pastTensorsByInputName) {
      this.encodedContext = encodedContext;
      this.result = result;
      this.pastTensorsByInputName = pastTensorsByInputName;
    }

    int pastLength() {
      return encodedContext.length;
    }
  }

  @Nullable private KvCacheState mKvCache;
  @Nullable private LastInferenceDebugState mLastInferenceDebugState;

  public NeuralPredictionManager(@NonNull Context context) {
    this(context, new ModelStore(context));
  }

  NeuralPredictionManager(@NonNull Context context, @NonNull ModelStore modelStore) {
    mContext = context.getApplicationContext();
    mModelStore = modelStore;
  }

  public boolean activate() {
    mSessionLock.lock();
    try {
      if (isActive()) {
        return true;
      }

      final long activationStart = SystemClock.elapsedRealtime();
      mLastActivationError = null;
      final ActiveModel activeModel = mModelStore.ensureActiveModel(EngineType.NEURAL);
      if (activeModel == null) {
        mLastActivationError = "No neural language model installed.";
        mLastActivationLatencyMs = SystemClock.elapsedRealtime() - activationStart;
        mLastActivationUptimeMs = SystemClock.uptimeMillis();
        return false;
      }

      try {
        final File onnxFile = activeModel.requireFile("onnx");
        final File vocabFile = activeModel.requireFile("tokenizer.vocab");
        final File mergesFile = activeModel.requireFile("tokenizer.merges");

        mTokenizer = new Gpt2Tokenizer(vocabFile, mergesFile);
        mModelVocabSize = mTokenizer.getVocabSize();
        mEnvironment = OrtEnvironment.getEnvironment();
        mSessionOptions = new OrtSession.SessionOptions();
        configureSessionOptions(mSessionOptions);
        mSession = mEnvironment.createSession(onnxFile.getAbsolutePath(), mSessionOptions);
        mSessionInputNames = mSession.getInputNames();
        mSessionOutputNames = mSession.getOutputNames();
        Logger.i(TAG, "Neural model input names: " + mSessionInputNames);
        Logger.i(TAG, "Neural model output names: " + mSessionOutputNames);
        initializeSessionMetadata();
        mActiveModel = activeModel;
        Logger.i(
            TAG, "Neural predictor activated with model " + activeModel.getDefinition().getId());
        mLastActivationLatencyMs = SystemClock.elapsedRealtime() - activationStart;
        mLastActivationUptimeMs = SystemClock.uptimeMillis();
        return true;
      } catch (IOException exception) {
        mLastActivationError = "Failed loading tokenizer assets: " + exception.getMessage();
        Logger.e(TAG, mLastActivationError, exception);
        mLastActivationLatencyMs = SystemClock.elapsedRealtime() - activationStart;
        mLastActivationUptimeMs = SystemClock.uptimeMillis();
        deactivate();
        return false;
      } catch (OrtException exception) {
        mLastActivationError = "Failed initializing ONNX runtime: " + exception.getMessage();
        Logger.e(TAG, mLastActivationError, exception);
        mLastActivationLatencyMs = SystemClock.elapsedRealtime() - activationStart;
        mLastActivationUptimeMs = SystemClock.uptimeMillis();
        deactivate();
        return false;
      }
    } finally {
      mSessionLock.unlock();
    }
  }

  /**
   * Configures the ONNX session for low-latency on-device inference: full graph optimization,
   * sequential execution, and the XNNPACK execution provider when available. Each step degrades
   * gracefully — a provider or option the device/runtime doesn't support is skipped (logged) rather
   * than failing activation, so we always end up with at least a plain-CPU session. Output parity is
   * preserved: these are speed/scheduling knobs, not numerical ones.
   */
  private void configureSessionOptions(@NonNull OrtSession.SessionOptions options) {
    try {
      options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
    } catch (Throwable t) {
      Logger.w(TAG, "Could not set ONNX graph optimization level: " + t.getMessage());
    }
    try {
      options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
    } catch (Throwable t) {
      Logger.w(TAG, "Could not set ONNX execution mode: " + t.getMessage());
    }

    // Prefer XNNPACK (optimized CPU kernels). When it owns the compute, give it the intra-op pool
    // and keep ORT's own intra-op threads at 1 to avoid two competing pools. If XNNPACK isn't in
    // this AAR / on this device, fall back to ORT's CPU provider with a small intra-op pool.
    boolean xnnpackEnabled = false;
    try {
      options.addXnnpack(
          java.util.Collections.singletonMap(
              "intra_op_num_threads", String.valueOf(SESSION_INTRA_OP_THREADS)));
      xnnpackEnabled = true;
    } catch (Throwable t) {
      Logger.i(TAG, "XNNPACK execution provider unavailable, using CPU: " + t.getMessage());
    }

    try {
      options.setIntraOpNumThreads(xnnpackEnabled ? 1 : SESSION_INTRA_OP_THREADS);
    } catch (Throwable t) {
      Logger.w(TAG, "Could not set ONNX intra-op thread count: " + t.getMessage());
    }

    mLastSessionProvider = xnnpackEnabled ? "xnnpack" : "cpu";
    Logger.i(TAG, "Neural ONNX session configured with provider=" + mLastSessionProvider);
  }

  /** The execution provider the last successful activation used ("xnnpack" or "cpu"). */
  @VisibleForTesting
  @NonNull
  public String getLastSessionProviderForTest() {
    return mLastSessionProvider;
  }

  public void deactivate() {
    mSessionLock.lock();
    try {
      clearKvCacheLocked();
      mLastInferenceDebugState = null;
      mActiveModel = null;
      if (mSession != null) {
        try {
          mSession.close();
        } catch (OrtException ignore) {
        }
      }
      mSession = null;
      if (mSessionOptions != null) {
        mSessionOptions.close();
      }
      mSessionOptions = null;
      mTokenizer = null;
      mEnvironment = null;
      mSessionInputNames = null;
      mSessionOutputNames = null;
      mPastKeyValueInputNames.clear();
      mPastKeyValueInputShapes.clear();
      mPastKeyValueInputTypes.clear();
      mModelVocabSize = 0;
    } finally {
      mSessionLock.unlock();
    }
  }

  public boolean isActive() {
    return mSession != null && mTokenizer != null;
  }

  @Nullable
  public String getLastActivationError() {
    return mLastActivationError;
  }

  @VisibleForTesting
  public long getLastActivationLatencyMsForTest() {
    mSessionLock.lock();
    try {
      return mLastActivationLatencyMs;
    } finally {
      mSessionLock.unlock();
    }
  }

  @VisibleForTesting
  public long getLastActivationUptimeMsForTest() {
    mSessionLock.lock();
    try {
      return mLastActivationUptimeMs;
    } finally {
      mSessionLock.unlock();
    }
  }

  @NonNull
  public java.util.List<String> predictNextWords(@NonNull String[] contextTokens, int maxResults) {
    return predictNextWordsWithScoringContext(contextTokens, maxResults).candidates;
  }

  @NonNull
  public NextWordPredictions predictNextWordsWithScoringContext(
      @NonNull String[] contextTokens, int maxResults) {
    if (maxResults <= 0) {
      return NextWordPredictions.empty();
    }
    mSessionLock.lock();
    final java.util.ArrayList<OnnxTensor> owned = new java.util.ArrayList<>();
    OrtSession.Result result = null;
    boolean cachedResult = false;
    try {
      long activationLatencyMs = 0L;
      if (!isActive()) {
        final long start = SystemClock.elapsedRealtime();
        if (!activate()) {
          return NextWordPredictions.empty();
        }
        activationLatencyMs = SystemClock.elapsedRealtime() - start;
      }
      if (!isActive() || mTokenizer == null || mSession == null || mEnvironment == null) {
        return NextWordPredictions.empty();
      }

      // Prefix with a space so GPT-2 uses its "word start" tokens (which typically include a
      // leading-space marker) for next-word prediction. Avoid adding a trailing space: doing so
      // can cause the model to emit continuation fragments that don't include the word-start
      // marker.
      final String contextText = " " + String.join(" ", contextTokens);
      if (contextText.trim().isEmpty()) {
        return NextWordPredictions.empty();
      }

      int[] encoded = mTokenizer.encode(contextText);
      if (encoded.length == 0) {
        return NextWordPredictions.empty();
      }
      if (ENABLE_TEST_LOGS) {
        Logger.d(
            TAG, "predictNextWords ctx='" + contextText + "' tokens=" + Arrays.toString(encoded));
      }
      if (encoded.length > MAX_CONTEXT_TOKENS) {
        final int[] trimmed = new int[MAX_CONTEXT_TOKENS];
        System.arraycopy(
            encoded, encoded.length - MAX_CONTEXT_TOKENS, trimmed, 0, MAX_CONTEXT_TOKENS);
        encoded = trimmed;
      }

      int[] stepTokens = encoded;
      int pastLength = 0;
      @Nullable Map<String, OnnxTensor> pastTensors = null;
      boolean usedKvCache = false;
      final KvCacheState kvCache = mKvCache;
      if (kvCache != null) {
        final int[] cachedEncoded = kvCache.encodedContext;
        if (cachedEncoded.length > 0
            && encoded.length > cachedEncoded.length
            && startsWith(encoded, cachedEncoded)) {
          final int extraCount = encoded.length - cachedEncoded.length;
          final int[] delta = new int[extraCount];
          System.arraycopy(encoded, cachedEncoded.length, delta, 0, extraCount);
          stepTokens = delta;
          pastLength = kvCache.pastLength();
          pastTensors = kvCache.pastTensorsByInputName;
          usedKvCache = true;
        }
      }

      if (ENABLE_TEST_LOGS) {
        Logger.d(
            TAG,
            "predictNextWords inference tokens="
                + stepTokens.length
                + " pastLength="
                + pastLength
                + " kvHit="
                + usedKvCache);
      }

      final long inferenceStart = SystemClock.elapsedRealtime();
      result = runSequenceStep(stepTokens, pastLength, pastTensors, owned);
      final long onnxLatencyMs = SystemClock.elapsedRealtime() - inferenceStart;
      mLastInferenceDebugState =
          new LastInferenceDebugState(
              SystemClock.uptimeMillis(),
              activationLatencyMs,
              encoded.length,
              stepTokens.length,
              pastLength,
              usedKvCache,
              onnxLatencyMs);
      final float[] lastLogitsRaw = extractLogits(getLogitsValue(result));
      final float[] lastLogits =
          lastLogitsRaw == null
              ? null
              : java.util.Arrays.copyOf(lastLogitsRaw, lastLogitsRaw.length);
      if (lastLogits == null) {
        return NextWordPredictions.empty();
      }
      final NextWordScoringContext scoringContext =
          new NextWordScoringContext(lastLogits, logSumExp(lastLogits));
      if (ENABLE_TEST_LOGS && mTokenizer != null) {
        Logger.d(TAG, "top5 logits " + formatTopLogits(lastLogits, 5, mTokenizer));
        Logger.d(TAG, "top5 decoded " + formatTopDecoded(lastLogits, 5, mTokenizer));
      }

      List<String> rawCandidates;
      try {
        rawCandidates = extractTopWords(result, lastLogits, encoded, maxResults);
      } catch (Throwable t) {
        Logger.w(TAG, "Failed expanding next-word candidates: " + t.getMessage());
        rawCandidates = extractTopTokens(lastLogits, maxResults);
      }
      final List<String> filteredCandidates =
          filterCandidatesForKeyboardUx(contextTokens, rawCandidates);

      cachedResult = maybeUpdateKvCacheLocked(encoded, result);
      return new NextWordPredictions(filteredCandidates, scoringContext);
    } catch (OrtException exception) {
      Logger.e(TAG, "ONNX runtime failure: " + exception.getMessage(), exception);
      mLastActivationError = exception.getMessage();
      deactivate();
      return NextWordPredictions.empty();
    } finally {
      if (result != null && !cachedResult) {
        result.close();
      }
      // Close any past_key_values tensors created outside the try-with-resources scope.
      for (OnnxTensor tensor : owned) {
        if (tensor != null) {
          tensor.close();
        }
      }
      mSessionLock.unlock();
    }
  }

  @VisibleForTesting
  @Nullable
  public LastInferenceDebugState getLastInferenceDebugStateForTest() {
    mSessionLock.lock();
    try {
      return mLastInferenceDebugState;
    } finally {
      mSessionLock.unlock();
    }
  }

  private OrtSession.Result runSequenceStep(
      @NonNull int[] tokenIds,
      int pastLength,
      @Nullable Map<String, OnnxTensor> past,
      @NonNull java.util.List<OnnxTensor> owned)
      throws OrtException {
    if (mEnvironment == null || mSession == null) {
      throw new OrtException("Neural engine not active");
    }
    try (OnnxTensor inputTensor = createInputTensor(tokenIds);
        OnnxTensor attentionMask = maybeCreateAttentionMask(tokenIds.length, pastLength);
        OnnxTensor positionIds = maybeCreatePositionIds(tokenIds.length, pastLength)) {
      final java.util.HashMap<String, OnnxTensor> inputs = new java.util.HashMap<>();
      inputs.put("input_ids", inputTensor);
      if (attentionMask != null) {
        inputs.put("attention_mask", attentionMask);
      }
      if (positionIds != null) {
        inputs.put("position_ids", positionIds);
      }
      if (past != null) {
        inputs.putAll(past);
      } else {
        inputs.putAll(createPastKeyValueInputs(tokenIds.length, pastLength, owned));
      }
      return mSession.run(inputs);
    }
  }

  private boolean maybeUpdateKvCacheLocked(@NonNull int[] encodedContext, @NonNull Result result)
      throws OrtException {
    if (mPastKeyValueInputNames.isEmpty()) {
      return false;
    }

    final java.util.HashMap<String, OnnxTensor> past = new java.util.HashMap<>();
    if (!tryPopulatePastTensors(result, past)) {
      return false;
    }

    final int[] copy = java.util.Arrays.copyOf(encodedContext, encodedContext.length);
    final KvCacheState next = new KvCacheState(copy, result, past);
    if (mKvCache != null) {
      mKvCache.result.close();
    }
    mKvCache = next;
    return true;
  }

  private void clearKvCacheLocked() {
    if (mKvCache != null) {
      mKvCache.result.close();
      mKvCache = null;
    }
  }

  private static boolean startsWith(@NonNull int[] encoded, @NonNull int[] prefix) {
    if (prefix.length > encoded.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (encoded[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  @NonNull
  public List<String> sortCandidatesByFirstTokenLogProb(
      @NonNull NextWordScoringContext scoringContext, @NonNull List<String> candidates) {
    if (candidates.size() <= 1) {
      return candidates;
    }

    mSessionLock.lock();
    try {
      if (mTokenizer == null) {
        return candidates;
      }
      final class ScoredIndex {
        final int index;
        final String token;
        final float score;

        ScoredIndex(int index, @NonNull String token, float score) {
          this.index = index;
          this.token = token;
          this.score = score;
        }
      }

      final ArrayList<ScoredIndex> scored = new ArrayList<>(candidates.size());
      for (int i = 0; i < candidates.size(); i++) {
        final String candidate = candidates.get(i);
        if (candidate == null) {
          scored.add(new ScoredIndex(i, "", Float.NEGATIVE_INFINITY));
          continue;
        }
        final String trimmed = candidate.trim();
        final int tokenId;
        if (trimmed.isEmpty()) {
          tokenId = -1;
        } else {
          final int[] encoded = mTokenizer.encode(" " + trimmed);
          tokenId = encoded.length == 0 ? -1 : encoded[0];
        }
        final float score =
            tokenId < 0
                ? Float.NEGATIVE_INFINITY
                : logProbForTokenId(scoringContext.lastLogits, scoringContext.logSumExp, tokenId);
        scored.add(new ScoredIndex(i, candidate, score));
      }

      if (scored.size() <= 1) {
        return candidates;
      }

      scored.sort(
          (a, b) -> {
            final int cmp = Float.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            return Integer.compare(a.index, b.index);
          });

      final ArrayList<String> out = new ArrayList<>(scored.size());
      for (ScoredIndex s : scored) {
        out.add(s.token);
      }
      return out;
    } finally {
      mSessionLock.unlock();
    }
  }

  @Nullable
  private Object getLogitsValue(@NonNull Result result) throws OrtException {
    if (mSessionOutputNames != null && mSessionOutputNames.contains("logits")) {
      final java.util.Optional<ai.onnxruntime.OnnxValue> maybe = result.get("logits");
      if (maybe.isPresent()) {
        return maybe.get().getValue();
      }
    }
    return result.get(0).getValue();
  }

  private void initializeSessionMetadata() throws OrtException {
    mPastKeyValueInputNames.clear();
    mPastKeyValueInputShapes.clear();
    mPastKeyValueInputTypes.clear();
    if (mSession == null || mSessionInputNames == null) {
      return;
    }

    final Map<String, NodeInfo> inputInfo = mSession.getInputInfo();
    for (String name : mSessionInputNames) {
      final Matcher matcher = PAST_KEY_VALUE_PATTERN.matcher(name);
      if (!matcher.matches()) {
        continue;
      }
      mPastKeyValueInputNames.add(name);
      final NodeInfo nodeInfo = inputInfo.get(name);
      final ValueInfo valueInfo = nodeInfo != null ? nodeInfo.getInfo() : null;
      if (valueInfo instanceof TensorInfo) {
        final TensorInfo tensorInfo = (TensorInfo) valueInfo;
        final long[] rawShape = tensorInfo.getShape();
        final long[] shape = materializeShape(rawShape);
        mPastKeyValueInputShapes.put(name, shape);
        mPastKeyValueInputTypes.put(name, tensorInfo.type);
        if (ENABLE_TEST_LOGS) {
          Logger.d(
              TAG,
              "past_key_values input "
                  + name
                  + " shape="
                  + java.util.Arrays.toString(shape)
                  + " type="
                  + tensorInfo.type);
        }
      }
    }
  }

  @Nullable
  private OnnxTensor maybeCreateAttentionMask(int tokenCount, int pastLength) throws OrtException {
    if (mEnvironment == null
        || mSessionInputNames == null
        || !mSessionInputNames.contains("attention_mask")) {
      return null;
    }
    final int length = pastLength + tokenCount;
    final long[] shape = new long[] {1L, length};
    final long[] data = new long[length];
    java.util.Arrays.fill(data, 1L);
    return OnnxTensor.createTensor(mEnvironment, LongBuffer.wrap(data), shape);
  }

  @Nullable
  private OnnxTensor maybeCreatePositionIds(int tokenCount, int pastLength) throws OrtException {
    if (mEnvironment == null
        || mSessionInputNames == null
        || !mSessionInputNames.contains("position_ids")) {
      return null;
    }
    final long[] shape = new long[] {1L, tokenCount};
    final long[] data = new long[tokenCount];
    for (int i = 0; i < tokenCount; i++) {
      data[i] = pastLength + i;
    }
    return OnnxTensor.createTensor(mEnvironment, LongBuffer.wrap(data), shape);
  }

  private OnnxTensor createInputTensor(int[] tokenIds) throws OrtException {
    final long[] shape = new long[] {1L, tokenIds.length};
    final long[] data = new long[tokenIds.length];
    for (int i = 0; i < tokenIds.length; i++) data[i] = tokenIds[i];
    final LongBuffer buf = LongBuffer.wrap(data);
    return OnnxTensor.createTensor(mEnvironment, buf, shape);
  }

  private java.util.Map<String, OnnxTensor> createPastKeyValueInputs(
      int tokenCount, int pastLength, java.util.List<OnnxTensor> owned) throws OrtException {
    final java.util.HashMap<String, OnnxTensor> out = new java.util.HashMap<>();
    if (mEnvironment == null || mPastKeyValueInputNames.isEmpty()) {
      return out;
    }
    for (String name : mPastKeyValueInputNames) {
      final long[] recordedShape = mPastKeyValueInputShapes.get(name);
      final ai.onnxruntime.OnnxJavaType type = mPastKeyValueInputTypes.get(name);
      if (recordedShape == null || type == null) continue;
      final long[] shape = recordedShape.clone();
      if (shape.length >= 3) {
        final int seqIndex = shape.length - 2; // for [batch, heads, seq, dim] this is seq
        shape[seqIndex] = pastLength;
      }
      final long size = elementCount(shape);
      if (size < 0 || size > Integer.MAX_VALUE) continue;
      switch (type) {
        case FLOAT:
          final float[] fData = new float[(int) size];
          final OnnxTensor fTensor =
              OnnxTensor.createTensor(mEnvironment, FloatBuffer.wrap(fData), shape);
          owned.add(fTensor);
          out.put(name, fTensor);
          break;
        case INT64:
          final long[] lData = new long[(int) size];
          final OnnxTensor lTensor =
              OnnxTensor.createTensor(mEnvironment, LongBuffer.wrap(lData), shape);
          owned.add(lTensor);
          out.put(name, lTensor);
          break;
        default:
          // unsupported type; skip
          break;
      }
    }
    return out;
  }

  private long elementCount(long[] shape) {
    long n = 1L;
    for (long dim : shape) {
      n *= dim;
    }
    return n;
  }

  private float[] extractLogits(Object value) {
    if (value instanceof float[][][]) {
      final float[][][] logits = (float[][][]) value;
      return logits[0][logits[0].length - 1];
    }
    if (value instanceof float[][]) {
      final float[][] logits = (float[][]) value;
      return logits[logits.length - 1];
    }
    return null;
  }

  private java.util.List<String> extractTopTokens(float[] lastLogits, int k) {
    return selectTopTokens(lastLogits, k, mTokenizer);
  }

  private java.util.List<String> extractTopWords(
      @NonNull Result baseResult, float[] lastLogits, @NonNull int[] baseEncoded, int k)
      throws OrtException {
    if (k <= 0 || lastLogits == null || lastLogits.length == 0 || mTokenizer == null) {
      return new java.util.ArrayList<>();
    }

    final int desired =
        Math.max(WORD_CANDIDATE_SEARCH_WINDOW_MIN, k * WORD_CANDIDATE_SEARCH_WINDOW_MULTIPLIER);
    final int target =
        Math.min(lastLogits.length, Math.min(WORD_CANDIDATE_SEARCH_WINDOW_MAX, desired));
    final java.util.List<Integer> tokenIds = selectTopTokenIds(lastLogits, target);

    final int basePastLength = baseEncoded.length;
    final java.util.HashMap<String, OnnxTensor> basePast = new java.util.HashMap<>();
    final boolean canExpand = tryPopulatePastTensors(baseResult, basePast);

    final float baseLogSumExp = logSumExp(lastLogits);
    final int maxSeedCandidates = Math.max(64, k * 16);
    final java.util.ArrayList<ScoredWordCandidate> candidates =
        new java.util.ArrayList<>(Math.min(tokenIds.size(), maxSeedCandidates));

    final long start = SystemClock.elapsedRealtime();
    for (int tokenId : tokenIds) {
      final String decodedRaw = mTokenizer.decodeId(tokenId);
      if (!startsWithWhitespace(decodedRaw)) continue;
      final String decoded = normalizeDecodedToken(decodedRaw);
      if (decoded.isEmpty()) continue;
      if (isPunctuationOnly(decoded)) continue;
      if (!isSimpleWord(decoded)) continue;

      final float score = logProbForTokenId(lastLogits, baseLogSumExp, tokenId);
      candidates.add(new ScoredWordCandidate(tokenId, decoded, score, 1));
      if (candidates.size() >= maxSeedCandidates) break;
    }

    candidates.sort((a, b) -> Float.compare(b.score, a.score));

    // Expand only the highest-scoring short candidates. Expanding the first few decoded tokens in
    // logit order can miss "word fragment" candidates that end up in the top-K after scoring.
    final int expansionLimit =
        canExpand ? WORD_EXPANSION_MAX_CANDIDATES : WORD_EXPANSION_NO_PAST_MAX_CANDIDATES;
    int expanded = 0;
    for (ScoredWordCandidate candidate : candidates) {
      if (expanded >= expansionLimit) break;
      if (candidate.word.length() > WORD_EXPANSION_MIN_LEN) continue;
      if (SOFT_CONTINUATION_DISALLOW_WORDS.contains(candidate.word.toLowerCase(Locale.ROOT))) {
        continue;
      }

      expanded++;
      if (canExpand) {
        final ExpansionResult expansion =
            greedyExpandWordScored(
                candidate.firstTokenId,
                candidate.word,
                basePast,
                basePastLength,
                WORD_EXPANSION_MAX_EXTRA_TOKENS,
                WORD_EXPANSION_SEARCH_WINDOW);
        candidate.word = expansion.word;
        candidate.score += expansion.scoreDelta;
        candidate.tokens += expansion.extraTokens;
      } else {
        final ExpansionResult expansion =
            greedyExpandWordFullSequenceScored(
                baseEncoded,
                candidate.firstTokenId,
                candidate.word,
                WORD_EXPANSION_NO_PAST_MAX_EXTRA_TOKENS,
                WORD_EXPANSION_SEARCH_WINDOW);
        candidate.word = expansion.word;
        candidate.score += expansion.scoreDelta;
        candidate.tokens += expansion.extraTokens;
      }

      if (candidate.word.isEmpty()
          || isPunctuationOnly(candidate.word)
          || !isSimpleWord(candidate.word)) {
        candidate.word = "";
        candidate.score = Float.NEGATIVE_INFINITY;
      }
    }

    candidates.sort((a, b) -> Float.compare(b.score, a.score));
    final java.util.ArrayList<String> out = new java.util.ArrayList<>(k);
    final java.util.HashSet<String> seenLower = new java.util.HashSet<>();
    for (ScoredWordCandidate candidate : candidates) {
      if (candidate.word.isEmpty()) continue;
      final String lower = candidate.word.toLowerCase();
      if (seenLower.add(lower)) {
        out.add(candidate.word);
        if (out.size() == k) break;
      }
    }

    if (ENABLE_TEST_LOGS) {
      Logger.d(
          TAG,
          "extractTopWords produced "
              + out.size()
              + " candidates in "
              + (SystemClock.elapsedRealtime() - start)
              + "ms");
    }
    return out;
  }

  @VisibleForTesting
  static java.util.List<String> extractTopWordsFromLogitsForTests(
      @NonNull float[] lastLogits, int k, @NonNull Gpt2Tokenizer tokenizer) {
    if (k <= 0 || lastLogits.length == 0) {
      return java.util.Collections.emptyList();
    }
    final int desired =
        Math.max(WORD_CANDIDATE_SEARCH_WINDOW_MIN, k * WORD_CANDIDATE_SEARCH_WINDOW_MULTIPLIER);
    final int target =
        Math.min(lastLogits.length, Math.min(WORD_CANDIDATE_SEARCH_WINDOW_MAX, desired));
    final java.util.List<Integer> tokenIds = selectTopTokenIds(lastLogits, target);

    final java.util.ArrayList<String> out = new java.util.ArrayList<>(k);
    final java.util.HashSet<String> seenLower = new java.util.HashSet<>();
    for (int tokenId : tokenIds) {
      final String decodedRaw = tokenizer.decodeId(tokenId);
      if (!startsWithWhitespace(decodedRaw)) continue;
      final String decoded = normalizeDecodedToken(decodedRaw);
      if (decoded.isEmpty()) continue;
      if (isPunctuationOnly(decoded)) continue;
      if (!isSimpleWord(decoded)) continue;
      final String lower = decoded.toLowerCase();
      if (seenLower.add(lower)) {
        out.add(decoded);
        if (out.size() == k) break;
      }
    }
    return out;
  }

  @NonNull
  private static List<String> filterCandidatesForKeyboardUx(
      @NonNull String[] contextTokens, @NonNull List<String> candidates) {
    if (candidates.isEmpty()) {
      return candidates;
    }

    final boolean allowDomainLike = looksLikeDomainContext(contextTokens);
    final ArrayList<String> out = new ArrayList<>(candidates.size());
    for (String candidate : candidates) {
      if (candidate == null) continue;
      final String trimmed = candidate.trim();
      if (trimmed.isEmpty()) continue;
      final String lower = trimmed.toLowerCase(Locale.ROOT);

      // Filter out GPT-style artifacts that are poor "next word" UX in normal prose. Keep them
      // available when the context looks like URL/domain typing.
      if (!allowDomainLike && DOMAIN_LIKE_TOKENS.contains(lower)) continue;
      if (isLikelyStopwordTheFusion(lower)) continue;

      out.add(trimmed);
    }

    // If the model produces both a likely subword fragment and a longer candidate that starts with
    // it (e.g., "pizz" and "pizza"), drop the shorter prefix. This helps avoid "token fragments"
    // in the suggestion strip without needing a dictionary lookup.
    final List<String> withoutPrefixes = dropShortPrefixCandidates(out);
    final List<String> filtered = withoutPrefixes.isEmpty() ? out : withoutPrefixes;

    if (ENABLE_TEST_LOGS && (!filtered.equals(candidates) || filtered.isEmpty())) {
      Logger.d(
          TAG,
          "filterCandidatesForKeyboardUx allowDomainLike="
              + allowDomainLike
              + " in="
              + candidates
              + " out="
              + filtered);
    }

    // Allow returning an empty list: callers can fall back to non-neural sources instead of showing
    // low-quality / domain-like artifacts in normal prose.
    return filtered;
  }

  @NonNull
  private static List<String> dropShortPrefixCandidates(@NonNull List<String> candidates) {
    if (candidates.size() <= 1) {
      return candidates;
    }
    final int n = candidates.size();
    final boolean[] drop = new boolean[n];
    for (int i = 0; i < n; i++) {
      final String a = candidates.get(i);
      if (a == null) continue;
      final String aLower = a.toLowerCase(Locale.ROOT);
      if (aLower.length() < 4) continue;
      for (int j = 0; j < n; j++) {
        if (i == j) continue;
        final String b = candidates.get(j);
        if (b == null) continue;
        final String bLower = b.toLowerCase(Locale.ROOT);
        if (bLower.length() <= aLower.length()) continue;
        if (!bLower.startsWith(aLower)) continue;
        if (bLower.length() - aLower.length() > 4) continue;
        drop[i] = true;
        break;
      }
    }

    final ArrayList<String> out = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      if (!drop[i]) {
        out.add(candidates.get(i));
      }
    }
    return out;
  }

  private static boolean looksLikeDomainContext(@NonNull String[] contextTokens) {
    // Heuristic: if the user recently typed something URL/domain-ish, allow "com"/"www"/etc.
    final int start = Math.max(0, contextTokens.length - 2);
    for (int i = contextTokens.length - 1; i >= start; i--) {
      final String token = contextTokens[i];
      if (token == null) continue;
      final String lower = token.toLowerCase(Locale.ROOT);
      if (lower.contains(".")) return true;
      if (lower.startsWith("http")) return true;
      if (lower.startsWith("www")) return true;
    }
    return false;
  }

  private static boolean isLikelyStopwordTheFusion(@NonNull String lowerWord) {
    if (lowerWord.length() <= 3) return false;
    if (!lowerWord.endsWith("the")) return false;
    final String prefix = lowerWord.substring(0, lowerWord.length() - 3);
    return !prefix.isEmpty() && SOFT_CONTINUATION_DISALLOW_WORDS.contains(prefix);
  }

  private boolean tryPopulatePastTensors(
      @NonNull Result result, @NonNull Map<String, OnnxTensor> out) throws OrtException {
    if (mPastKeyValueInputNames.isEmpty() || mSessionOutputNames == null) {
      return false;
    }
    for (String inputName : mPastKeyValueInputNames) {
      final String outputName = mapPastInputToOutputName(inputName);
      if (outputName == null) {
        return false;
      }
      final java.util.Optional<ai.onnxruntime.OnnxValue> maybeValue = result.get(outputName);
      if (maybeValue.isEmpty()) {
        return false;
      }
      final ai.onnxruntime.OnnxValue value = maybeValue.get();
      if (!(value instanceof OnnxTensor)) {
        return false;
      }
      out.put(inputName, (OnnxTensor) value);
    }
    return out.size() == mPastKeyValueInputNames.size();
  }

  @Nullable
  private String mapPastInputToOutputName(@NonNull String inputName) {
    if (mSessionOutputNames == null) {
      return null;
    }
    if (mSessionOutputNames.contains(inputName)) {
      return inputName;
    }
    if (inputName.startsWith("past_key_values.")) {
      final String suffix = inputName.substring("past_key_values".length());
      final String present = "present" + suffix;
      if (mSessionOutputNames.contains(present)) {
        return present;
      }
      final String presentKeyValues = "present_key_values" + suffix;
      if (mSessionOutputNames.contains(presentKeyValues)) {
        return presentKeyValues;
      }
    }
    return null;
  }

  @NonNull
  private String greedyExpandWord(
      int firstTokenId,
      @NonNull String firstToken,
      @NonNull Map<String, OnnxTensor> basePast,
      int basePastLength,
      int maxExtraTokens,
      int searchWindow)
      throws OrtException {
    if (mTokenizer == null || maxExtraTokens <= 0) {
      return firstToken;
    }
    final StringBuilder word = new StringBuilder(Math.min(32, firstToken.length() + 12));
    word.append(firstToken);

    int currentTokenId = firstTokenId;
    int pastLength = basePastLength;
    Map<String, OnnxTensor> pastTensors = basePast;

    OrtSession.Result currentResult = null;
    try {
      for (int step = 0; step < maxExtraTokens; step++) {
        if (word.length() >= 24) break;

        final OrtSession.Result result =
            runSingleTokenStep(currentTokenId, pastTensors, pastLength);
        final float[] logits = extractLogits(getLogitsValue(result));
        if (logits == null) {
          result.close();
          break;
        }

        final int nextTokenId =
            chooseBestWordContinuationTokenId(logits, searchWindow, word.toString());
        if (nextTokenId < 0) {
          result.close();
          break;
        }

        final String nextRaw = mTokenizer.decodeId(nextTokenId);
        if (!isWordContinuationSegment(nextRaw)) {
          result.close();
          break;
        }
        final String next = normalizeDecodedToken(nextRaw);
        if (next.isEmpty()) {
          result.close();
          break;
        }
        word.append(next);

        final java.util.HashMap<String, OnnxTensor> newPast = new java.util.HashMap<>();
        if (!tryPopulatePastTensors(result, newPast)) {
          result.close();
          break;
        }

        pastLength += 1;
        currentTokenId = nextTokenId;
        pastTensors = newPast;

        if (currentResult != null) {
          currentResult.close();
        }
        currentResult = result;
      }
    } finally {
      if (currentResult != null) {
        currentResult.close();
      }
    }
    return word.toString();
  }

  @NonNull
  private ExpansionResult greedyExpandWordScored(
      int firstTokenId,
      @NonNull String firstToken,
      @NonNull Map<String, OnnxTensor> basePast,
      int basePastLength,
      int maxExtraTokens,
      int searchWindow)
      throws OrtException {
    if (mTokenizer == null || maxExtraTokens <= 0) {
      return new ExpansionResult(firstToken, 0f, 0);
    }
    final StringBuilder word = new StringBuilder(Math.min(32, firstToken.length() + 12));
    word.append(firstToken);

    float scoreDelta = 0f;
    int extraTokens = 0;
    int currentTokenId = firstTokenId;
    int pastLength = basePastLength;
    Map<String, OnnxTensor> pastTensors = basePast;

    OrtSession.Result currentResult = null;
    try {
      for (int step = 0; step < maxExtraTokens; step++) {
        if (word.length() >= 24) break;

        final OrtSession.Result result =
            runSingleTokenStep(currentTokenId, pastTensors, pastLength);
        final float[] logits = extractLogits(getLogitsValue(result));
        if (logits == null) {
          result.close();
          break;
        }

        final int nextTokenId =
            chooseBestWordContinuationTokenId(logits, searchWindow, word.toString());
        if (nextTokenId < 0) {
          result.close();
          break;
        }

        final String nextRaw = mTokenizer.decodeId(nextTokenId);
        if (!isWordContinuationSegment(nextRaw)) {
          result.close();
          break;
        }
        final String next = normalizeDecodedToken(nextRaw);
        if (next.isEmpty()) {
          result.close();
          break;
        }

        scoreDelta += logProbForTokenId(logits, nextTokenId);
        extraTokens += 1;
        word.append(next);

        final java.util.HashMap<String, OnnxTensor> newPast = new java.util.HashMap<>();
        if (!tryPopulatePastTensors(result, newPast)) {
          result.close();
          break;
        }

        pastLength += 1;
        currentTokenId = nextTokenId;
        pastTensors = newPast;

        if (currentResult != null) {
          currentResult.close();
        }
        currentResult = result;
      }
    } finally {
      if (currentResult != null) {
        currentResult.close();
      }
    }
    return new ExpansionResult(word.toString(), scoreDelta, extraTokens);
  }

  @NonNull
  private String greedyExpandWordFullSequence(
      @NonNull int[] baseEncoded,
      int firstTokenId,
      @NonNull String firstToken,
      int maxExtraTokens,
      int searchWindow)
      throws OrtException {
    if (mTokenizer == null || maxExtraTokens <= 0) {
      return firstToken;
    }

    final StringBuilder word = new StringBuilder(Math.min(32, firstToken.length() + 12));
    word.append(firstToken);

    int[] seq = new int[baseEncoded.length + 1];
    System.arraycopy(baseEncoded, 0, seq, 0, baseEncoded.length);
    seq[baseEncoded.length] = firstTokenId;

    for (int step = 0; step < maxExtraTokens; step++) {
      if (word.length() >= 24) break;

      final OrtSession.Result result = runFullSequenceStep(seq);
      try {
        final float[] logits = extractLogits(getLogitsValue(result));
        if (logits == null) {
          break;
        }

        final int nextTokenId =
            chooseBestWordContinuationTokenId(logits, searchWindow, word.toString());
        if (nextTokenId < 0) {
          break;
        }

        final String nextRaw = mTokenizer.decodeId(nextTokenId);
        if (!isWordContinuationSegment(nextRaw)) {
          break;
        }
        final String next = normalizeDecodedToken(nextRaw);
        if (next.isEmpty()) {
          break;
        }

        word.append(next);
        final int[] nextSeq = new int[seq.length + 1];
        System.arraycopy(seq, 0, nextSeq, 0, seq.length);
        nextSeq[seq.length] = nextTokenId;
        seq = nextSeq;
      } finally {
        result.close();
      }
    }

    return word.toString();
  }

  @NonNull
  private ExpansionResult greedyExpandWordFullSequenceScored(
      @NonNull int[] baseEncoded,
      int firstTokenId,
      @NonNull String firstToken,
      int maxExtraTokens,
      int searchWindow)
      throws OrtException {
    if (mTokenizer == null || maxExtraTokens <= 0) {
      return new ExpansionResult(firstToken, 0f, 0);
    }

    final StringBuilder word = new StringBuilder(Math.min(32, firstToken.length() + 12));
    word.append(firstToken);

    float scoreDelta = 0f;
    int extraTokens = 0;
    int[] seq = new int[baseEncoded.length + 1];
    System.arraycopy(baseEncoded, 0, seq, 0, baseEncoded.length);
    seq[baseEncoded.length] = firstTokenId;

    for (int step = 0; step < maxExtraTokens; step++) {
      if (word.length() >= 24) break;

      final OrtSession.Result result = runFullSequenceStep(seq);
      try {
        final float[] logits = extractLogits(getLogitsValue(result));
        if (logits == null) {
          break;
        }

        final int nextTokenId =
            chooseBestWordContinuationTokenId(logits, searchWindow, word.toString());
        if (nextTokenId < 0) {
          break;
        }

        final String nextRaw = mTokenizer.decodeId(nextTokenId);
        if (!isWordContinuationSegment(nextRaw)) {
          break;
        }
        final String next = normalizeDecodedToken(nextRaw);
        if (next.isEmpty()) {
          break;
        }

        scoreDelta += logProbForTokenId(logits, nextTokenId);
        extraTokens += 1;
        word.append(next);

        final int[] nextSeq = new int[seq.length + 1];
        System.arraycopy(seq, 0, nextSeq, 0, seq.length);
        nextSeq[seq.length] = nextTokenId;
        seq = nextSeq;
      } finally {
        result.close();
      }
    }

    return new ExpansionResult(word.toString(), scoreDelta, extraTokens);
  }

  private static final class ScoredWordCandidate {
    final int firstTokenId;
    @NonNull String word;
    float score;
    int tokens;

    private ScoredWordCandidate(int firstTokenId, @NonNull String word, float score, int tokens) {
      this.firstTokenId = firstTokenId;
      this.word = word;
      this.score = score;
      this.tokens = tokens;
    }
  }

  private static final class ExpansionResult {
    @NonNull final String word;
    final float scoreDelta;
    final int extraTokens;

    private ExpansionResult(@NonNull String word, float scoreDelta, int extraTokens) {
      this.word = word;
      this.scoreDelta = scoreDelta;
      this.extraTokens = extraTokens;
    }
  }

  @VisibleForTesting
  static float logProbForTokenId(@NonNull float[] logits, int tokenId) {
    return logProbForTokenId(logits, logSumExp(logits), tokenId);
  }

  @VisibleForTesting
  static float logProbForTokenId(@NonNull float[] logits, float logSumExp, int tokenId) {
    if (tokenId < 0 || tokenId >= logits.length) return Float.NEGATIVE_INFINITY;
    final float raw = logits[tokenId];
    if (!Float.isFinite(raw) || !Float.isFinite(logSumExp)) return Float.NEGATIVE_INFINITY;
    return raw - logSumExp;
  }

  @VisibleForTesting
  static float logSumExp(@NonNull float[] logits) {
    if (logits.length == 0) return Float.NEGATIVE_INFINITY;
    float max = Float.NEGATIVE_INFINITY;
    for (float v : logits) {
      if (Float.isFinite(v) && v > max) {
        max = v;
      }
    }
    if (!Float.isFinite(max)) return Float.NEGATIVE_INFINITY;
    double sum = 0d;
    for (float v : logits) {
      if (!Float.isFinite(v)) continue;
      sum += Math.exp(v - max);
    }
    if (sum <= 0d) return Float.NEGATIVE_INFINITY;
    return (float) (max + Math.log(sum));
  }

  private OrtSession.Result runSingleTokenStep(
      int tokenId, @NonNull Map<String, OnnxTensor> past, int pastLength) throws OrtException {
    if (mEnvironment == null || mSession == null) {
      throw new OrtException("Neural engine not active");
    }
    try (OnnxTensor inputTensor = createInputTensor(new int[] {tokenId});
        OnnxTensor attentionMask = maybeCreateAttentionMask(1, pastLength);
        OnnxTensor positionIds = maybeCreatePositionIds(1, pastLength)) {
      final java.util.HashMap<String, OnnxTensor> inputs = new java.util.HashMap<>();
      inputs.put("input_ids", inputTensor);
      if (attentionMask != null) {
        inputs.put("attention_mask", attentionMask);
      }
      if (positionIds != null) {
        inputs.put("position_ids", positionIds);
      }
      inputs.putAll(past);
      return mSession.run(inputs);
    }
  }

  private OrtSession.Result runFullSequenceStep(@NonNull int[] tokenIds) throws OrtException {
    if (mEnvironment == null || mSession == null) {
      throw new OrtException("Neural engine not active");
    }
    final java.util.ArrayList<OnnxTensor> owned = new java.util.ArrayList<>();
    try (OnnxTensor inputTensor = createInputTensor(tokenIds);
        OnnxTensor attentionMask = maybeCreateAttentionMask(tokenIds.length, 0);
        OnnxTensor positionIds = maybeCreatePositionIds(tokenIds.length, 0)) {
      final java.util.HashMap<String, OnnxTensor> inputs = new java.util.HashMap<>();
      inputs.put("input_ids", inputTensor);
      if (attentionMask != null) {
        inputs.put("attention_mask", attentionMask);
      }
      if (positionIds != null) {
        inputs.put("position_ids", positionIds);
      }
      inputs.putAll(createPastKeyValueInputs(tokenIds.length, 0, owned));
      return mSession.run(inputs);
    } finally {
      for (OnnxTensor tensor : owned) {
        if (tensor != null) {
          tensor.close();
        }
      }
    }
  }

  private int chooseBestWordContinuationTokenId(float[] logits, int searchWindow) {
    if (mTokenizer == null) {
      return -1;
    }
    return chooseBestWordContinuationTokenId(logits, searchWindow, mTokenizer);
  }

  private int chooseBestWordContinuationTokenId(
      float[] logits, int searchWindow, @NonNull String wordSoFar) {
    if (mTokenizer == null) {
      return -1;
    }
    return chooseBestWordContinuationTokenId(logits, searchWindow, mTokenizer, wordSoFar);
  }

  @VisibleForTesting
  static int chooseBestWordContinuationTokenId(
      float[] logits, int searchWindow, @NonNull Gpt2Tokenizer tokenizer) {
    return chooseBestWordContinuationTokenId(logits, searchWindow, tokenizer, null);
  }

  @VisibleForTesting
  static int chooseBestWordContinuationTokenId(
      float[] logits,
      int searchWindow,
      @NonNull Gpt2Tokenizer tokenizer,
      @Nullable String wordSoFar) {
    if (logits == null || logits.length == 0) {
      return -1;
    }
    final int target = Math.min(logits.length, Math.max(searchWindow, 8));
    final java.util.List<Integer> tokenIds = selectTopTokenIds(logits, target);
    if (tokenIds.isEmpty()) return -1;

    final int bestId = tokenIds.get(0);
    final String bestRaw = tokenizer.decodeId(bestId);
    if (isWordContinuationSegment(bestRaw)) {
      return bestId;
    }

    if (!shouldTrySoftContinuation(wordSoFar)) {
      // If the best next token introduces whitespace/punctuation, treat the word as complete.
      // Do not force a continuation token for short words: that can fuse "of"+"the" into "ofthe".
      return -1;
    }

    final int softLen = wordSoFar == null ? 0 : wordSoFar.trim().length();
    final float continuationLogitDeltaMax =
        softLen >= 4
            ? WORD_CONTINUATION_LOGIT_DELTA_MAX + 0.75f
            : WORD_CONTINUATION_LOGIT_DELTA_MAX;

    final float bestLogit = logits[bestId];
    for (int id : tokenIds) {
      if (id == bestId) continue;
      final String raw = tokenizer.decodeId(id);
      if (!isWordContinuationSegment(raw)) continue;
      final float logit = logits[id];
      if (bestLogit - logit > continuationLogitDeltaMax) {
        break;
      }
      return id;
    }
    return -1;
  }

  private static boolean isWordContinuationSegment(@NonNull String rawSegment) {
    if (rawSegment.isEmpty()) return false;
    boolean hasLetter = false;
    for (int i = 0; i < rawSegment.length(); i++) {
      final char c = rawSegment.charAt(i);
      if (Character.isWhitespace(c)) return false;
      if (Character.isLetter(c)) {
        hasLetter = true;
        continue;
      }
      if (c == '\'' || c == '.') continue;
      return false;
    }
    return hasLetter;
  }

  private static boolean shouldTrySoftContinuation(@Nullable String wordSoFar) {
    if (wordSoFar == null) return false;
    final String trimmed = wordSoFar.trim();
    if (trimmed.length() < 2 || trimmed.length() > 4) return false;
    final String lower = trimmed.toLowerCase();
    if (SOFT_CONTINUATION_DISALLOW_WORDS.contains(lower)) return false;
    for (int i = 0; i < lower.length(); i++) {
      if (!Character.isLetter(lower.charAt(i))) return false;
    }
    return true;
  }

  private static java.util.List<Integer> selectTopTokenIds(float[] lastLogits, int target) {
    if (target <= 0 || lastLogits == null || lastLogits.length == 0) {
      return java.util.Collections.emptyList();
    }
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(
            java.util.Comparator.comparingDouble(a -> Float.intBitsToFloat(a[1])));
    for (int i = 0; i < lastLogits.length; i++) {
      final float score = lastLogits[i];
      final int packed = Float.floatToRawIntBits(score);
      if (heap.size() < target) {
        heap.add(new int[] {i, packed});
      } else if (score > Float.intBitsToFloat(heap.peek()[1])) {
        heap.poll();
        heap.add(new int[] {i, packed});
      }
    }
    final java.util.ArrayList<int[]> sorted = new java.util.ArrayList<>(heap.size());
    while (!heap.isEmpty()) {
      sorted.add(heap.poll());
    }
    java.util.Collections.reverse(sorted);
    final java.util.ArrayList<Integer> out = new java.util.ArrayList<>(sorted.size());
    for (int[] pair : sorted) {
      out.add(pair[0]);
    }
    return out;
  }

  private static String formatTopLogits(
      float[] lastLogits, int k, @NonNull Gpt2Tokenizer tokenizer) {
    if (lastLogits == null || lastLogits.length == 0 || k <= 0) {
      return "[]";
    }
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(
            java.util.Comparator.comparingDouble(a -> Float.intBitsToFloat(a[1])));
    for (int i = 0; i < lastLogits.length; i++) {
      final float score = lastLogits[i];
      final int packed = Float.floatToRawIntBits(score);
      if (heap.size() < k) {
        heap.add(new int[] {i, packed});
      } else if (score > Float.intBitsToFloat(heap.peek()[1])) {
        heap.poll();
        heap.add(new int[] {i, packed});
      }
    }
    final java.util.ArrayList<String> out = new java.util.ArrayList<>(heap.size());
    while (!heap.isEmpty()) {
      final int[] pair = heap.poll();
      out.add(
          pair[0]
              + ":"
              + tokenizer.decodeId(pair[0]).replace("\n", "\\n").replace("\r", "")
              + "="
              + Float.intBitsToFloat(pair[1]));
    }
    java.util.Collections.reverse(out);
    return out.toString();
  }

  private static String formatTopDecoded(
      float[] lastLogits, int k, @NonNull Gpt2Tokenizer tokenizer) {
    if (lastLogits == null || lastLogits.length == 0 || k <= 0) {
      return "[]";
    }
    final int target = Math.min(lastLogits.length, Math.max(k * 6, k + 8));
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(
            java.util.Comparator.comparingDouble(a -> Float.intBitsToFloat(a[1])));
    for (int i = 0; i < lastLogits.length; i++) {
      final float score = lastLogits[i];
      final int packed = Float.floatToRawIntBits(score);
      if (heap.size() < target) {
        heap.add(new int[] {i, packed});
      } else if (score > Float.intBitsToFloat(heap.peek()[1])) {
        heap.poll();
        heap.add(new int[] {i, packed});
      }
    }
    final java.util.ArrayList<int[]> sorted = new java.util.ArrayList<>(heap.size());
    while (!heap.isEmpty()) {
      sorted.add(heap.poll());
    }
    java.util.Collections.reverse(sorted);

    final java.util.ArrayList<String> out = new java.util.ArrayList<>(Math.min(k, sorted.size()));
    for (int idx = 0; idx < sorted.size() && out.size() < k; idx++) {
      final int tokenId = sorted.get(idx)[0];
      final float score = Float.intBitsToFloat(sorted.get(idx)[1]);
      final String decoded = tokenizer.decodeId(tokenId).replace("\n", "\\n").replace("\r", "");
      out.add(tokenId + ":" + decoded + "=" + score);
    }
    return out.toString();
  }

  /** Visible for testing: selects the top-k token strings by logit value. */
  static java.util.List<String> selectTopTokens(
      float[] lastLogits, int k, @Nullable Gpt2Tokenizer tokenizer) {
    if (k <= 0 || lastLogits == null || lastLogits.length == 0) {
      return java.util.Collections.emptyList();
    }
    final int target = Math.min(lastLogits.length, Math.max(k * 6, k + 8));
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(
            java.util.Comparator.comparingDouble(a -> Float.intBitsToFloat(a[1])));
    for (int i = 0; i < lastLogits.length; i++) {
      final float score = lastLogits[i];
      final int packed = Float.floatToRawIntBits(score);
      if (heap.size() < target) {
        heap.add(new int[] {i, packed});
      } else if (score > Float.intBitsToFloat(heap.peek()[1])) {
        heap.poll();
        heap.add(new int[] {i, packed});
      }
    }
    final java.util.ArrayList<int[]> sorted = new java.util.ArrayList<>(heap.size());
    while (!heap.isEmpty()) {
      final int[] pair = heap.poll();
      sorted.add(pair);
    }
    java.util.Collections.reverse(sorted);

    final java.util.ArrayList<String> out = new java.util.ArrayList<>(k);
    for (int[] pair : sorted) {
      final int tokenId = pair[0];
      final String decodedRaw =
          tokenizer != null ? tokenizer.decodeId(tokenId) : String.valueOf(tokenId);
      final String decoded = normalizeDecodedToken(decodedRaw);
      if (decoded.isEmpty()) continue;
      if (tokenizer != null) {
        if (isPunctuationOnly(decoded)) continue;
        if (!isSimpleWord(decoded)) continue;
      }
      out.add(decoded);
      if (out.size() == k) break;
    }
    if (out.isEmpty()) {
      for (int[] pair : sorted) {
        final String decodedRaw =
            tokenizer != null ? tokenizer.decodeId(pair[0]) : String.valueOf(pair[0]);
        final String decoded = normalizeDecodedToken(decodedRaw);
        if (decoded.isEmpty()) continue;
        if (tokenizer != null) {
          if (isPunctuationOnly(decoded)) continue;
          if (!isSimpleWord(decoded)) continue;
        }
        out.add(decoded);
        if (out.size() == k) break;
      }
    }
    return out;
  }

  private long[] materializeShape(long[] raw) {
    if (raw == null) return new long[] {};
    final long[] shape = raw.clone();
    for (int i = 0; i < shape.length; i++) if (shape[i] < 0) shape[i] = 1L;
    return shape;
  }

  private static boolean isPunctuationOnly(@NonNull String token) {
    if (token.isEmpty()) return true;
    for (int i = 0; i < token.length(); i++) {
      final char c = token.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        return false;
      }
    }
    return true;
  }

  private static boolean startsWithWhitespace(@NonNull String token) {
    return !token.isEmpty() && Character.isWhitespace(token.charAt(0));
  }

  @NonNull
  private static String normalizeDecodedToken(@NonNull String decodedRaw) {
    return decodedRaw
        .trim()
        // Normalize smart quotes so tests/filters treat them consistently.
        .replace('\u2018', '\'')
        .replace('\u2019', '\'');
  }

  private static boolean isSimpleWord(@NonNull String token) {
    // Keep this tight: next-word suggestions should be word-like, not arbitrary punctuation/digits.
    if (token.isEmpty() || token.length() > 24) return false;
    if (!Character.isLetter(token.charAt(0))) return false;
    for (int i = 0; i < token.length(); i++) {
      final char c = token.charAt(i);
      if (Character.isLetter(c)) continue;
      if (c == '\'' || c == '.') continue;
      return false;
    }
    return true;
  }
}
