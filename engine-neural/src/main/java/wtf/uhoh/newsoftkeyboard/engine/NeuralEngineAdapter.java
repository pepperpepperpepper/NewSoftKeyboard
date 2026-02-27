package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPredictionManager;

/** Adapter over NeuralPredictionManager to conform to PredictionEngine. */
public final class NeuralEngineAdapter implements PredictionEngine {

  private final NeuralPredictionManager manager;
  @Nullable private volatile NeuralPredictionManager.NextWordScoringContext lastScoringContext;

  public NeuralEngineAdapter(@NonNull NeuralPredictionManager manager) {
    this.manager = manager;
  }

  @NonNull
  @Override
  public EngineType getType() {
    return EngineType.NEURAL;
  }

  @Override
  public boolean isReady() {
    return manager.isActive();
  }

  @Override
  public boolean activate() {
    lastScoringContext = null;
    return manager.activate();
  }

  @Override
  public void deactivate() {
    lastScoringContext = null;
    manager.deactivate();
  }

  @Nullable
  @Override
  public String getLastError() {
    return manager.getLastActivationError();
  }

  @NonNull
  @Override
  public PredictionResult predict(@NonNull String[] contextTokens, int maxResults) {
    final NeuralPredictionManager.NextWordPredictions prediction =
        manager.predictNextWordsWithScoringContext(contextTokens, maxResults);
    lastScoringContext = prediction.scoringContext;
    return new PredictionResult(prediction.candidates);
  }

  @Nullable
  public NeuralPredictionManager.NextWordScoringContext getLastScoringContext() {
    return lastScoringContext;
  }
}
