package com.anysoftkeyboard.dictionaries.neural;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore.ActiveModel;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.ValueInfo;
import java.io.File;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles ONNX Runtime backed next-word predictions. */
public final class NeuralPredictionManager {

  private static final String TAG = "NeuralPredictionManager";
  private static final int MAX_CONTEXT_TOKENS = 64;
  private static final Pattern PAST_KEY_VALUE_PATTERN =
      Pattern.compile("past_key_values\\.(\\d+)\\.(key|value)");

  private final Context mContext;
  private final PresageModelStore mModelStore;
  private final ReentrantLock mSessionLock = new ReentrantLock();

  @Nullable private ActiveModel mActiveModel;
  @Nullable private OrtEnvironment mEnvironment;
  @Nullable private OrtSession.SessionOptions mSessionOptions;
  @Nullable private OrtSession mSession;
  @Nullable private Gpt2Tokenizer mTokenizer;
  @Nullable private String mLastActivationError;
  @Nullable private java.util.Set<String> mSessionInputNames;

  private final List<String> mPastKeyValueInputNames = new ArrayList<>();
  private final Map<String, long[]> mPastKeyValueInputShapes = new HashMap<>();
  private final Map<String, OnnxJavaType> mPastKeyValueInputTypes = new HashMap<>();
  private int mModelVocabSize = 0;

  public NeuralPredictionManager(@NonNull Context context) {
    this(context, new PresageModelStore(context));
  }

  NeuralPredictionManager(
      @NonNull Context context, @NonNull PresageModelStore presageModelStore) {
    mContext = context.getApplicationContext();
    mModelStore = presageModelStore;
  }

  public boolean activate() {
    mSessionLock.lock();
    try {
      if (isActive()) {
        return true;
      }

      mLastActivationError = null;
      final ActiveModel activeModel =
          mModelStore.ensureActiveModel(PresageModelDefinition.EngineType.NEURAL);
      if (activeModel == null) {
        mLastActivationError = "No neural language model installed.";
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
        mSession = mEnvironment.createSession(onnxFile.getAbsolutePath(), mSessionOptions);
        mSessionInputNames = mSession.getInputNames();
        Logger.i(TAG, "Neural model input names: " + mSessionInputNames);
        initializeSessionMetadata();
        mActiveModel = activeModel;
        Logger.i(
            TAG, "Neural predictor activated with model " + activeModel.getDefinition().getId());
        return true;
      } catch (IOException exception) {
        mLastActivationError = "Failed loading tokenizer assets: " + exception.getMessage();
        Logger.e(TAG, mLastActivationError, exception);
        deactivate();
        return false;
      } catch (OrtException exception) {
        mLastActivationError = "Failed initializing ONNX runtime: " + exception.getMessage();
        Logger.e(TAG, mLastActivationError, exception);
        deactivate();
        return false;
      }
    } finally {
      mSessionLock.unlock();
    }
  }

  public void deactivate() {
    mSessionLock.lock();
    try {
      mActiveModel = null;
      if (mSession != null) {
        try {
          mSession.close();
        } catch (OrtException ignore) {
          // ignore close failures
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

  @NonNull
  public List<String> predictNextWords(@NonNull String[] contextTokens, int maxResults) {
    if (maxResults <= 0) {
      return new ArrayList<>();
    }
    mSessionLock.lock();
    try {
      if (!isActive() && !activate()) {
        return new ArrayList<>();
      }
      if (!isActive() || mTokenizer == null || mSession == null || mEnvironment == null) {
        return new ArrayList<>();
      }

      final String contextText = joinContext(contextTokens);
      if (contextText.trim().isEmpty()) {
        return new ArrayList<>();
      }

      int[] encoded = mTokenizer.encode(contextText);
      if (encoded.length == 0) {
        return new ArrayList<>();
      }
      if (encoded.length > MAX_CONTEXT_TOKENS) {
        final int[] trimmed = new int[MAX_CONTEXT_TOKENS];
        System.arraycopy(
            encoded, encoded.length - MAX_CONTEXT_TOKENS, trimmed, 0, MAX_CONTEXT_TOKENS);
        encoded = trimmed;
      }

      try (OnnxTensor inputTensor = createInputTensor(encoded);
          OnnxTensor maskTensor = needsAttentionMask() ? createAttentionMask(encoded.length) : null;
          OnnxTensor positionTensor =
              needsPositionIds() ? createPositionIds(encoded.length) : null) {
        final HashMap<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", inputTensor);
        if (maskTensor != null) {
          inputs.put("attention_mask", maskTensor);
        }
        if (positionTensor != null) {
          inputs.put("position_ids", positionTensor);
        }

        final List<OnnxTensor> pastTensors = new ArrayList<>();
        try {
          for (String pastName : mPastKeyValueInputNames) {
            final OnnxTensor tensor = createEmptyPastTensor(pastName);
            pastTensors.add(tensor);
            inputs.put(pastName, tensor);
          }

          final Result result = mSession.run(inputs);
          try {
            final Object value = result.get(0).getValue();
            final float[] lastLogits = extractLogits(value);
            if (lastLogits == null) {
              return new ArrayList<>();
            }
            return extractTopTokens(lastLogits, maxResults);
          } finally {
            result.close();
          }
        } finally {
          for (OnnxTensor tensor : pastTensors) {
            tensor.close();
          }
        }
      }
    } catch (OrtException exception) {
      Logger.e(TAG, "ONNX runtime failure: " + exception.getMessage(), exception);
      mLastActivationError = exception.getMessage();
      deactivate();
      return new ArrayList<>();
    } finally {
      mSessionLock.unlock();
    }
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
        final String debugMessage =
            "Past input "
                + name
                + " rawShape="
                + Arrays.toString(rawShape)
                + " shape="
                + Arrays.toString(shape)
                + " type="
                + tensorInfo.type;
        Logger.d(TAG, debugMessage);
      } else {
        mPastKeyValueInputShapes.put(name, new long[] {1, 1, 1, 1});
        mPastKeyValueInputTypes.put(name, OnnxJavaType.FLOAT);
      }
    }

    mPastKeyValueInputNames.sort(
        (left, right) -> {
          final int indexCompare = Integer.compare(extractPastLayer(left), extractPastLayer(right));
          if (indexCompare != 0) {
            return indexCompare;
          }
          return extractPastComponent(left) - extractPastComponent(right);
        });
    Logger.d(
        TAG,
        "Past key/value inputs discovered: %d",
        mPastKeyValueInputNames.size());
  }

  private long[] materializeShape(@Nullable long[] shape) {
    if (shape == null || shape.length == 0) {
      return new long[] {1, 1, 1, 1};
    }
    final long[] copy = shape.clone();
    for (int i = 0; i < copy.length; i++) {
      if (copy[i] < 0) {
        copy[i] = 0;
      }
    }
    return copy;
  }

  private int extractPastLayer(@NonNull String name) {
    final Matcher matcher = PAST_KEY_VALUE_PATTERN.matcher(name);
    if (matcher.matches()) {
      try {
        return Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException ignored) {
        // ignore malformed index
      }
    }
    return Integer.MAX_VALUE;
  }

  private int extractPastComponent(@NonNull String name) {
    return name.endsWith(".key") ? 0 : 1;
  }

  @Nullable
  private float[] extractLogits(@NonNull Object value) {
    if (value instanceof float[][][]) {
      final float[][][] logits = (float[][][]) value;
      if (logits.length == 0 || logits[0].length == 0) {
        return null;
      }
      return logits[0][logits[0].length - 1];
    } else if (value instanceof float[][]) {
      final float[][] logits = (float[][]) value;
      if (logits.length == 0) {
        return null;
      }
      return logits[logits.length - 1];
    } else if (value instanceof float[]) {
      return (float[]) value;
    } else {
      Logger.w(TAG, "Unexpected logits tensor type: " + value.getClass());
      return null;
    }
  }

  @NonNull
  private List<String> extractTopTokens(float[] logits, int maxResults) {
    final int vocabSize =
        Math.min(logits.length, mModelVocabSize > 0 ? mModelVocabSize : logits.length);
    final PriorityQueue<int[]> topCandidates =
        new PriorityQueue<>(Comparator.comparingDouble(candidate -> candidate[1]));
    for (int i = 0; i < vocabSize; i++) {
      if (mTokenizer != null && mTokenizer.isSpecialToken(i)) {
        continue;
      }
      final float logit = logits[i];
      if (Float.isNaN(logit) || Float.isInfinite(logit)) {
        continue;
      }
      if (topCandidates.size() < maxResults) {
        topCandidates.offer(new int[] {i, Float.floatToRawIntBits(logit)});
      } else {
        final int[] smallest = topCandidates.peek();
        if (smallest != null && Float.intBitsToFloat(smallest[1]) < logit) {
          topCandidates.poll();
          topCandidates.offer(new int[] {i, Float.floatToRawIntBits(logit)});
        }
      }
    }

    final List<int[]> sorted = new ArrayList<>(topCandidates);
    sorted.sort(
        (left, right) ->
            Float.compare(
                Float.intBitsToFloat(right[1]), Float.intBitsToFloat(left[1])));
    final List<String> suggestions = new ArrayList<>(sorted.size());
    for (int[] candidate : sorted) {
      final int tokenId = candidate[0];
      if (mTokenizer != null && mTokenizer.isSpecialToken(tokenId)) {
        continue;
      }
      final String decoded = mTokenizer == null ? "" : mTokenizer.decodeToken(tokenId);
      if (decoded.isEmpty()) {
        continue;
      }
      final String cleaned = decoded.replace("\u0120", " ").replace('▁', ' ').trim();
      if (!cleaned.isEmpty() && !suggestions.contains(cleaned)) {
        Logger.d(TAG, "Neural candidate token " + tokenId + " -> " + cleaned);
        suggestions.add(cleaned);
      }
    }
    return suggestions;
  }

  @NonNull
  private String joinContext(@NonNull String[] contextTokens) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < contextTokens.length; i++) {
      if (i > 0) {
        builder.append(' ');
      }
      builder.append(contextTokens[i]);
    }
    return builder.toString();
  }

  private OnnxTensor createInputTensor(int[] tokens) throws OrtException {
    final long[] shape = new long[] {1, tokens.length};
    final LongBuffer buffer = LongBuffer.allocate(tokens.length);
    for (int token : tokens) {
      buffer.put(token);
    }
    buffer.rewind();
    return OnnxTensor.createTensor(Objects.requireNonNull(mEnvironment), buffer, shape);
  }

  private OnnxTensor createAttentionMask(int sequenceLength) throws OrtException {
    final long[] shape = new long[] {1, sequenceLength};
    final long[] mask = new long[sequenceLength];
    java.util.Arrays.fill(mask, 1L);
    return OnnxTensor.createTensor(Objects.requireNonNull(mEnvironment), new long[][] {mask});
  }

  private OnnxTensor createPositionIds(int sequenceLength) throws OrtException {
    final long[][] positions = new long[1][sequenceLength];
    for (int i = 0; i < sequenceLength; i++) {
      positions[0][i] = i;
    }
    return OnnxTensor.createTensor(Objects.requireNonNull(mEnvironment), positions);
  }

  private OnnxTensor createEmptyPastTensor(@Nullable String name) throws OrtException {
    final long[] shape =
        name != null
            ? mPastKeyValueInputShapes.getOrDefault(name, new long[] {1, 1, 1, 1})
            : new long[] {1, 1, 1, 1};
    final long[] actualShape = shape.clone();
    long elementCount = 1L;
    boolean hasZeroDim = false;
    for (int i = 0; i < actualShape.length; i++) {
      if (actualShape[i] <= 0) {
        actualShape[i] = 0;
      }
      if (actualShape[i] == 0) {
        hasZeroDim = true;
      } else {
        elementCount *= actualShape[i];
      }
    }
    final int elementCountInt = hasZeroDim ? 0 : (int) Math.max(1L, elementCount);
    final OnnxJavaType type =
        name != null
            ? mPastKeyValueInputTypes.getOrDefault(name, OnnxJavaType.FLOAT)
            : OnnxJavaType.FLOAT;
    switch (type) {
      case FLOAT16:
        final short[] shortZeros = new short[elementCountInt];
        return OnnxTensor.createTensor(
            Objects.requireNonNull(mEnvironment),
            ShortBuffer.wrap(shortZeros),
            actualShape);
      case DOUBLE:
        final double[] doubleZeros = new double[elementCountInt];
        return OnnxTensor.createTensor(
            Objects.requireNonNull(mEnvironment),
            DoubleBuffer.wrap(doubleZeros),
            actualShape);
      case FLOAT:
      default:
        final float[] floatZeros = new float[elementCountInt];
        return OnnxTensor.createTensor(
            Objects.requireNonNull(mEnvironment),
            FloatBuffer.wrap(floatZeros),
            actualShape);
    }
  }

  private boolean needsPositionIds() {
    return mSessionInputNames != null && mSessionInputNames.contains("position_ids");
  }

  private boolean needsAttentionMask() {
    return mSessionInputNames != null && mSessionInputNames.contains("attention_mask");
  }
}
