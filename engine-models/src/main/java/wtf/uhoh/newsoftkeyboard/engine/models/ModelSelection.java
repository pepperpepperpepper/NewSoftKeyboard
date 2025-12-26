package wtf.uhoh.newsoftkeyboard.engine.models;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;

/** Persists per-engine model selection. */
final class ModelSelection {

  private static final String PREF_SELECTED_MODEL_ID = "selected_model_id";

  private final SharedPreferences mSelectionPreferences;

  ModelSelection(@NonNull SharedPreferences selectionPreferences) {
    mSelectionPreferences = selectionPreferences;
  }

  public void persistSelectedModelId(@NonNull EngineType engineType, @NonNull String modelId) {
    if (modelId.trim().isEmpty()) {
      clearSelectedModelId(engineType);
    } else {
      mSelectionPreferences.edit().putString(selectionPrefKey(engineType), modelId).apply();
    }
  }

  @Nullable
  public String getSelectedModelId(@NonNull EngineType engineType) {
    final String stored = mSelectionPreferences.getString(selectionPrefKey(engineType), "");
    if (stored == null || stored.trim().isEmpty()) {
      return null;
    }
    return stored;
  }

  public void clearSelectedModelId(@NonNull EngineType engineType) {
    mSelectionPreferences.edit().remove(selectionPrefKey(engineType)).apply();
  }

  private String selectionPrefKey(@NonNull EngineType engineType) {
    return PREF_SELECTED_MODEL_ID + "_" + ModelDefinition.serializeEngineType(engineType);
  }
}
