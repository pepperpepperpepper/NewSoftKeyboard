package wtf.uhoh.newsoftkeyboard.engine.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;

/** Handles discovery and installation of prediction models on-device. */
public class ModelStore {

  private static final String TAG = "ModelStore";
  private static final String DIGEST_PREFS = "presage_asset_versions";
  private static final String DIGEST_PREF_KEY_PREFIX = "sha_";
  private static final String MODEL_SELECTION_PREFS = "presage_model_selection";

  private final Context mContext;
  private final SharedPreferences mDigestPreferences;
  private final ModelSelection mSelection;
  private final ModelFiles mFiles;

  public ModelStore(@NonNull Context context) {
    mContext = context.getApplicationContext();
    mDigestPreferences = mContext.getSharedPreferences(DIGEST_PREFS, Context.MODE_PRIVATE);
    final SharedPreferences selectionPreferences =
        mContext.getSharedPreferences(MODEL_SELECTION_PREFS, Context.MODE_PRIVATE);
    mSelection = new ModelSelection(selectionPreferences);
    mFiles = new ModelFiles(mContext);
  }

  @Nullable
  public ActiveModel ensureActiveModel() {
    return ensureActiveModel(EngineType.NGRAM);
  }

  @Nullable
  public ActiveModel ensureActiveModel(@NonNull EngineType engineType) {
    if (engineType == EngineType.NONE) {
      return null;
    }

    final Map<String, ModelDefinition> availableDefinitions = discoverDefinitions();
    final List<ModelDefinition> engineDefinitions = new ArrayList<>();
    for (ModelDefinition definition : availableDefinitions.values()) {
      if (definition.getEngineType() == engineType) {
        engineDefinitions.add(definition);
      }
    }
    if (engineDefinitions.isEmpty()) {
      Logger.w(TAG, "No models discovered for engine " + engineType + "; predictions unavailable.");
      return null;
    }

    String selectedId = mSelection.getSelectedModelId(engineType);
    if (selectedId == null && engineType == EngineType.NGRAM) {
      for (ModelDefinition definition : engineDefinitions) {
        if (definition.getId().equals(ModelDefinition.DEFAULT_MODEL_ID)) {
          selectedId = definition.getId();
          break;
        }
      }
    }

    ModelDefinition definition = null;
    if (selectedId != null) {
      for (ModelDefinition candidate : engineDefinitions) {
        if (candidate.getId().equals(selectedId)) {
          definition = candidate;
          break;
        }
      }
    }

    if (definition == null) {
      definition =
          engineDefinitions.get(0); // fallback to first available when selected id is missing.
    }

    ActiveModel activeModel = ensureDefinitionInstalled(definition);
    if (activeModel != null) {
      mSelection.persistSelectedModelId(engineType, definition.getId());
      return activeModel;
    }

    Logger.w(
        TAG,
        "Failed to stage model "
            + definition.getId()
            + " for engine "
            + engineType
            + "; attempting fallback model.");
    for (ModelDefinition candidate : engineDefinitions) {
      if (candidate == definition) {
        continue;
      }
      activeModel = ensureDefinitionInstalled(candidate);
      if (activeModel != null) {
        mSelection.persistSelectedModelId(engineType, candidate.getId());
        return activeModel;
      }
    }

    Logger.w(TAG, "No models could be staged for engine " + engineType + "; predictions disabled.");
    return null;
  }

  @Nullable
  private ActiveModel ensureDefinitionInstalled(@NonNull ModelDefinition definition) {
    final File modelDirectory = new File(mFiles.getModelsRootDirectory(), definition.getId());
    mFiles.ensureDirectory(modelDirectory);

    final LinkedHashMap<String, File> installedFiles = new LinkedHashMap<>();
    for (Map.Entry<String, ModelDefinition.FileRequirement> entry :
        definition.getAllFileRequirements().entrySet()) {
      final File file = ensureRequirement(modelDirectory, definition, entry.getValue());
      if (file == null) {
        return null;
      }
      installedFiles.put(entry.getKey(), file);
    }

    mFiles.writeManifestIfNecessary(mFiles.manifestFile(modelDirectory), definition);
    return new ActiveModel(definition, modelDirectory, installedFiles);
  }

  @Nullable
  private File ensureRequirement(
      @NonNull File modelDirectory,
      @NonNull ModelDefinition definition,
      @NonNull ModelDefinition.FileRequirement requirement) {
    final File destination = new File(modelDirectory, requirement.getFilename());

    if (destination.exists() && destination.length() > 0L) {
      if (isDigestValid(definition, requirement, destination)) {
        return destination;
      }
      mFiles.removeFile(destination);
    }

    final String assetPath = requirement.getAssetPath();
    if (assetPath != null) {
      if (mFiles.stageFromAsset(destination, requirement)) {
        if (isDigestValid(definition, requirement, destination)) {
          return destination;
        }
        mFiles.removeFile(destination);
      }
    }

    if (!destination.exists()) {
      Logger.w(
          TAG,
          "Model "
              + definition.getId()
              + " missing "
              + requirement.getFilename()
              + "; download the model package and place it under "
              + modelDirectory.getAbsolutePath());
      return null;
    }

    if (isDigestValid(definition, requirement, destination)) {
      return destination;
    }

    Logger.w(
        TAG,
        "Model "
            + definition.getId()
            + " has unexpected checksum for "
            + requirement.getFilename()
            + "; delete and reinstall the model.");
    mFiles.removeFile(destination);
    return null;
  }

  private boolean isDigestValid(
      @NonNull ModelDefinition definition,
      @NonNull ModelDefinition.FileRequirement requirement,
      @NonNull File destination) {
    final String expectedSha = requirement.getSha256();
    if (expectedSha == null || expectedSha.isEmpty()) {
      return true;
    }

    final String digestKey = digestPreferenceKey(definition.getId(), requirement.getFilename());
    final String recordedSha = mDigestPreferences.getString(digestKey, "");
    if (expectedSha.equalsIgnoreCase(recordedSha)) {
      return true;
    }

    final String computed = mFiles.computeFileSha256(destination);
    if (computed == null) {
      return false;
    }
    if (!expectedSha.equalsIgnoreCase(computed)) {
      return false;
    }

    mDigestPreferences.edit().putString(digestKey, computed).apply();
    return true;
  }

  @NonNull
  private Map<String, ModelDefinition> discoverDefinitions() {
    final Map<String, ModelDefinition> definitions = new LinkedHashMap<>();

    final File[] modelDirs = mFiles.getModelsRootDirectory().listFiles();
    if (modelDirs == null) {
      maybeAddBundledDefault(definitions);
      return definitions;
    }

    for (File child : modelDirs) {
      if (!child.isDirectory()) {
        continue;
      }
      final File manifestFile = mFiles.manifestFile(child);
      if (!manifestFile.exists()) {
        continue;
      }
      try {
        final JSONObject manifestJson = mFiles.readJson(manifestFile);
        if (manifestJson == null) {
          continue;
        }
        final ModelDefinition manifestDefinition = ModelDefinition.fromJson(manifestJson);
        definitions.put(manifestDefinition.getId(), manifestDefinition);
      } catch (IOException | JSONException exception) {
        Log.w(TAG, "Failed parsing Presage manifest " + manifestFile.getAbsolutePath(), exception);
      }
    }

    maybeAddBundledDefault(definitions);
    return definitions;
  }

  private void maybeAddBundledDefault(@NonNull Map<String, ModelDefinition> definitions) {
    final ModelDefinition defaultDefinition = ModelDefinition.createDefaultKenlmDefinition();
    if (definitions.containsKey(defaultDefinition.getId())) {
      return;
    }
    if (hasBundledAssets(defaultDefinition)) {
      definitions.put(defaultDefinition.getId(), defaultDefinition);
    }
  }

  private boolean hasBundledAssets(@NonNull ModelDefinition definition) {
    if (definition.getEngineType() != EngineType.NGRAM) {
      return false;
    }
    return mFiles.assetExists(definition.getArpaRequirement().getAssetPath())
        && mFiles.assetExists(definition.getVocabRequirement().getAssetPath());
  }

  private String digestPreferenceKey(@NonNull String modelId, @NonNull String filename) {
    return DIGEST_PREF_KEY_PREFIX + modelId + "_" + filename;
  }

  public void persistSelectedModelId(@NonNull EngineType engineType, @NonNull String modelId) {
    mSelection.persistSelectedModelId(engineType, modelId);
  }

  @Nullable
  public String getSelectedModelId(@NonNull EngineType engineType) {
    return mSelection.getSelectedModelId(engineType);
  }

  public void clearSelectedModelId(@NonNull EngineType engineType) {
    mSelection.clearSelectedModelId(engineType);
  }

  // --- Compatibility helpers for settings/UI (used by ime/app) ---
  /** Returns all discovered Presage model definitions on device for both engines. */
  @NonNull
  public List<ModelDefinition> listAvailableModels() {
    return new ArrayList<>(discoverDefinitions().values());
  }

  /**
   * Removes a model directory by id. If the removed model was selected, clears the selection for
   * its engine type.
   */
  public void removeModel(@NonNull String modelId) {
    final Map<String, ModelDefinition> defs = discoverDefinitions();
    final ModelDefinition def = defs.get(modelId);
    final File modelDir = new File(mFiles.getModelsRootDirectory(), modelId);
    if (modelDir.exists()) {
      mFiles.deleteRecursively(modelDir);
    }
    if (def != null) {
      final String selected = mSelection.getSelectedModelId(def.getEngineType());
      if (modelId.equals(selected)) {
        mSelection.clearSelectedModelId(def.getEngineType());
      }
    }
  }

  public static final class ActiveModel {

    private final ModelDefinition mDefinition;
    private final File mModelDirectory;
    private final LinkedHashMap<String, File> mFiles;

    public ActiveModel(
        @NonNull ModelDefinition definition,
        @NonNull File modelDirectory,
        @NonNull LinkedHashMap<String, File> files) {
      mDefinition = definition;
      mModelDirectory = modelDirectory;
      mFiles = new LinkedHashMap<>(files);
    }

    @NonNull
    public ModelDefinition getDefinition() {
      return mDefinition;
    }

    @NonNull
    public File getModelDirectory() {
      return mModelDirectory;
    }

    @Nullable
    public File getFile(@NonNull String type) {
      return mFiles.get(type.toLowerCase(Locale.US));
    }

    @NonNull
    public File requireFile(@NonNull String type) {
      final File file = getFile(type);
      if (file == null) {
        throw new IllegalStateException(
            "Missing file type "
                + type
                + " for model "
                + mDefinition.getId()
                + " (engine "
                + mDefinition.getEngineType()
                + ")");
      }
      return file;
    }

    @NonNull
    public File getArpaFile() {
      return requireFile("arpa");
    }

    @NonNull
    public File getVocabFile() {
      return requireFile("vocab");
    }
  }
}
