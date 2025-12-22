package com.anysoftkeyboard.nextword.prediction;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.PresagePredictionManager;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import com.anysoftkeyboard.nextword.R;
import com.anysoftkeyboard.nextword.pipeline.EngineOrchestrator;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import wtf.uhoh.newsoftkeyboard.engine.NeuralEngineAdapter;
import wtf.uhoh.newsoftkeyboard.engine.PredictionEngine;
import wtf.uhoh.newsoftkeyboard.engine.PresageEngineAdapter;

/**
 * Owns on-device prediction engines (Presage + Neural) and their next-word state.
 *
 * <p>This keeps {@code SuggestionsProvider} focused on dictionary plumbing.
 */
public final class NextWordPredictionEngines {

  private static final int PRESAGE_CONTEXT_WINDOW = 16;
  private static final int NEURAL_MIN_CONTEXT_TOKENS = 2;
  private static final long NEURAL_LATENCY_BUDGET_MS = 25L;
  private static final char NEURAL_FAILURE_DELIMITER = '|';

  public static final class Outcome {
    public final int added;
    public final boolean shouldIncludeLegacyNextWords;

    Outcome(int added, boolean shouldIncludeLegacyNextWords) {
      this.added = added;
      this.shouldIncludeLegacyNextWords = shouldIncludeLegacyNextWords;
    }
  }

  private enum Mode {
    NONE,
    NGRAM,
    NEURAL,
    HYBRID
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
  @NonNull private final PredictionEngine mNeuralEngine;

  @NonNull private Mode mMode = Mode.HYBRID;

  @NonNull
  private final ArrayDeque<String> mPresageContext = new ArrayDeque<>(PRESAGE_CONTEXT_WINDOW);

  private long mLastNeuralLatencyMs;

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
    mNeuralPredictionManager.deactivate();
    mPresageContext.clear();
  }

  public void resetSentence() {
    mPresageContext.clear();
  }

  public void setIncognitoMode(boolean incognitoMode) {
    if (incognitoMode) {
      mPresageContext.clear();
    }
  }

  public boolean isPresageEnabled() {
    return usesPresageEngine();
  }

  /** Returns true when the neural predictor should be considered active in the pipeline. */
  public boolean isNeuralEnabled() {
    return mMode == Mode.NEURAL || mMode == Mode.HYBRID;
  }

  public void updatePredictionEngine(@NonNull String modeValue) {
    switch (modeValue) {
      case "ngram":
        mMode = Mode.NGRAM;
        activatePresageIfNeeded();
        mNeuralPredictionManager.deactivate();
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
        mNeuralPredictionManager.deactivate();
        mPresageContext.clear();
        break;
    }
    if (mMode == Mode.NONE) {
      mPresageContext.clear();
    }
    Logger.i(mLogTagBase, "Prediction engine set to " + mMode);
  }

  public void activatePresageIfNeeded() {
    if (usesPresageEngine()) {
      mNgramEngine.activate();
    }
  }

  public Outcome appendNextWords(
      @NonNull String currentWord,
      @NonNull Collection<CharSequence> suggestionsHolder,
      int maxSuggestions,
      boolean incognitoMode,
      int maxNextWordSuggestionsCount) {
    if (!incognitoMode) {
      recordPresageContext(currentWord);
    }
    if (mMode == Mode.NONE || incognitoMode) {
      mPresageContext.clear();
    }

    int remainingSuggestions = maxSuggestions;

    boolean presageHadRaw = false;
    if (usesPresageEngine()) {
      final EngineOrchestrator.MergeOutcome outcome =
          appendPresageSuggestions(
              suggestionsHolder, remainingSuggestions, maxNextWordSuggestionsCount);
      presageHadRaw = outcome.hadRaw;
      remainingSuggestions -= outcome.added;
      if (remainingSuggestions <= 0) {
        return new Outcome(maxSuggestions, false);
      }
    }

    boolean neuralHadRaw = false;
    if (mMode == Mode.NEURAL || mMode == Mode.HYBRID) {
      if (mEnableDebugLogging) {
        final String[] ctx = mPresageContext.toArray(new String[0]);
        Logger.d(
            mLogTagBase,
            "Invoking neural next-word with context %s, limit %d",
            Arrays.toString(ctx),
            remainingSuggestions);
      }
      final EngineOrchestrator.MergeOutcome outcome =
          appendNeuralSuggestions(
              suggestionsHolder, remainingSuggestions, maxNextWordSuggestionsCount);
      neuralHadRaw = outcome.hadRaw;
      remainingSuggestions -= outcome.added;
      if (remainingSuggestions <= 0) {
        return new Outcome(maxSuggestions, false);
      }
    }

    final boolean shouldIncludeLegacyNextWords;
    switch (mMode) {
      case NGRAM:
        shouldIncludeLegacyNextWords = !presageHadRaw;
        break;
      case NEURAL:
        shouldIncludeLegacyNextWords = !neuralHadRaw;
        break;
      case HYBRID:
        shouldIncludeLegacyNextWords = !(presageHadRaw || neuralHadRaw);
        break;
      case NONE:
      default:
        shouldIncludeLegacyNextWords = true;
        break;
    }

    return new Outcome(maxSuggestions - remainingSuggestions, shouldIncludeLegacyNextWords);
  }

  private boolean usesPresageEngine() {
    return mMode == Mode.NGRAM || mMode == Mode.HYBRID;
  }

  private void recordPresageContext(@NonNull String word) {
    if (TextUtils.isEmpty(word)) return;
    if (mPresageContext.size() == PRESAGE_CONTEXT_WINDOW) {
      mPresageContext.removeFirst();
    }
    mPresageContext.addLast(word);
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
    return EngineOrchestrator.predictAndMerge(
        mNgramEngine,
        mPresageContext,
        maxNextWordSuggestionsCount,
        suggestionsHolder,
        limit,
        mEnableTestLogging,
        mLogTagBase + "-Presage");
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
        && mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS
        && !suggestionsHolder.isEmpty()) {
      Logger.d(
          mLogTagBase,
          "Skipping neural cascade; last inference "
              + mLastNeuralLatencyMs
              + "ms exceeded budget.");
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
    mLastNeuralLatencyMs = SystemClock.elapsedRealtime() - start;
    if (mLastNeuralLatencyMs > NEURAL_LATENCY_BUDGET_MS) {
      Logger.i(
          mLogTagBase,
          "Neural inference latency " + mLastNeuralLatencyMs + "ms for current context");
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
      if (!mNeuralEngine.activate()) {
        handleNeuralActivationFailure();
      }
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
}
