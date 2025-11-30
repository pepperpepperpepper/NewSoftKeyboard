package com.anysoftkeyboard.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.nextword.NextWordDictionary;
import com.anysoftkeyboard.nextword.NextWordStatistics;
import com.anysoftkeyboard.rx.RxSchedulers;
import com.anysoftkeyboard.utils.Triple;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;

import net.evendanan.pixel.UiUtils;

public class NextWordSettingsFragment extends PreferenceFragmentCompat {

  @NonNull private final CompositeDisposable mDisposable = new CompositeDisposable();

  @Nullable private SharedPreferences mSharedPreferences;
  @Nullable private ListPreference mPredictionEnginePreference;
  @Nullable private Preference mManageModelsPreference;
  @Nullable private String mPredictionEnginePrefKey;
  @Nullable private String mNextWordModePrefKey;
  @Nullable private String mNeuralFailurePrefKey;
  @Nullable private PresageModelStore mPresageModelStore;

  private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener =
      (sharedPreferences, key) -> {
        if (mPredictionEnginePrefKey != null && mPredictionEnginePrefKey.equals(key)) {
          updatePredictionEnginePreferenceSummary();
        } else if (mNextWordModePrefKey != null && mNextWordModePrefKey.equals(key)) {
          updatePredictionEnginePreferenceEnabled();
        } else if (mNeuralFailurePrefKey != null && mNeuralFailurePrefKey.equals(key)) {
          updatePredictionEnginePreferenceSummary();
          updateManageModelsSummary();
        }
      };

  private final Preference.OnPreferenceClickListener mClearDataListener =
      preference -> {
        mDisposable.add(
            createDictionaryAddOnFragment(this)
                .subscribeOn(RxSchedulers.background())
                .map(
                    pair -> {
                      Context appContext = pair.second.requireContext().getApplicationContext();

                      NextWordDictionary nextWordDictionary =
                          new NextWordDictionary(appContext, pair.first.getLanguage());
                      nextWordDictionary.load();
                      nextWordDictionary.clearData();
                      nextWordDictionary.close();

                      return pair.second;
                    })
                .observeOn(RxSchedulers.mainThread())
                .last(NextWordSettingsFragment.this)
                .subscribe(
                    NextWordSettingsFragment::loadUsageStatistics, t -> loadUsageStatistics()));
        return true;
      };

  private static Observable<Pair<DictionaryAddOnAndBuilder, NextWordSettingsFragment>>
      createDictionaryAddOnFragment(NextWordSettingsFragment fragment) {
    return Observable.fromIterable(
            AnyApplication.getExternalDictionaryFactory(fragment.requireContext()).getAllAddOns())
        .filter(addOn -> !TextUtils.isEmpty(addOn.getLanguage()))
        .distinct(DictionaryAddOnAndBuilder::getLanguage)
        .map(addOn -> Pair.create(addOn, fragment));
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_next_word);
    mPresageModelStore = new PresageModelStore(requireContext());
    mPredictionEnginePrefKey = getString(R.string.settings_key_prediction_engine_mode);
    mNextWordModePrefKey = getString(R.string.settings_key_next_word_dictionary_type);
    mNeuralFailurePrefKey = getString(R.string.settings_key_prediction_engine_last_neural_error);
    Preference enginePreference = findPreference(mPredictionEnginePrefKey);
    if (enginePreference instanceof ListPreference) {
      mPredictionEnginePreference = (ListPreference) enginePreference;
    }
    mManageModelsPreference = findPreference(getString(R.string.settings_key_manage_presage_models));
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);
    findPreference("clear_next_word_data").setOnPreferenceClickListener(mClearDataListener);

    if (mManageModelsPreference != null) {
      mManageModelsPreference.setOnPreferenceClickListener(
          preference -> {
            navigateToPresageModels();
            return true;
          });
    }

    if (mPredictionEnginePreference != null) {
      mPredictionEnginePreference.setOnPreferenceChangeListener(
          this::onPredictionEnginePreferenceChange);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    mSharedPreferences = getPreferenceManager().getSharedPreferences();
    if (mSharedPreferences != null) {
      mSharedPreferences.registerOnSharedPreferenceChangeListener(
          mSharedPreferenceChangeListener);
    }
    updatePredictionEnginePreferenceSummary();
    updateManageModelsSummary();
    updatePredictionEnginePreferenceEnabled();
  }

  @Override
  public void onPause() {
    if (mSharedPreferences != null) {
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(
          mSharedPreferenceChangeListener);
      mSharedPreferences = null;
    }
    super.onPause();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.next_word_dict_settings));
    loadUsageStatistics();
  }

  private boolean onPredictionEnginePreferenceChange(
      Preference preference, Object newValue) {
    final String modeValue = String.valueOf(newValue);
    if (TextUtils.isEmpty(modeValue)) {
      return true;
    }

    if (!isNextWordSuggestionsEnabled() && requiresPresageEngine(modeValue)) {
      showSuggestionsDisabledDialog();
      return false;
    }

    if (("ngram".equals(modeValue) || "hybrid".equals(modeValue))
        && !hasModelsForEngine(PresageModelDefinition.EngineType.NGRAM)) {
      showMissingModelDialog(PresageModelDefinition.EngineType.NGRAM);
      return false;
    }

    if ("neural".equals(modeValue)) {
      if (!hasModelsForEngine(PresageModelDefinition.EngineType.NEURAL)) {
        showMissingModelDialog(PresageModelDefinition.EngineType.NEURAL);
        return false;
      }
      updatePredictionEnginePreferenceSummary(modeValue);
      return true;
    }

    updatePredictionEnginePreferenceSummary(modeValue);
    return true;
  }

  private void updatePredictionEnginePreferenceSummary() {
    updatePredictionEnginePreferenceSummary(null);
  }

  private void updatePredictionEnginePreferenceSummary(@Nullable String overrideMode) {
    if (mPredictionEnginePreference == null || !isAdded()) {
      return;
    }

    String mode = overrideMode;
    if (TextUtils.isEmpty(mode)) {
      mode = mPredictionEnginePreference.getValue();
      if (TextUtils.isEmpty(mode)
          && mSharedPreferences != null
          && mPredictionEnginePrefKey != null) {
        mode =
            mSharedPreferences.getString(
                mPredictionEnginePrefKey,
                getString(R.string.settings_default_prediction_engine_mode));
      }
    }

    if (TextUtils.isEmpty(mode)) {
      mode = getString(R.string.settings_default_prediction_engine_mode);
    }

    final boolean suggestionsEnabled = isNextWordSuggestionsEnabled();
    if (!suggestionsEnabled) {
      mPredictionEnginePreference.setSummary(
          getString(R.string.prediction_engine_summary_requires_suggestions));
      return;
    }

    final NeuralFailureStatus failureStatus = getLastNeuralFailureStatus();
    if (failureStatus != null) {
      mPredictionEnginePreference.setSummary(buildNeuralFailureSummary(failureStatus, mode));
      return;
    }

    final String summary;
    switch (mode) {
      case "ngram":
        final String ngramLabel =
            resolveActiveModelLabel(PresageModelDefinition.EngineType.NGRAM);
        summary =
            ngramLabel != null
                ? getString(R.string.prediction_engine_mode_summary, ngramLabel)
                : getString(R.string.prediction_engine_summary_missing_model);
        break;
      case "hybrid":
        final String hybridLabel =
            resolveActiveModelLabel(PresageModelDefinition.EngineType.NGRAM);
        final String neuralHybridLabel =
            resolveActiveModelLabel(PresageModelDefinition.EngineType.NEURAL);
        summary =
            hybridLabel != null && neuralHybridLabel != null
                ? getString(
                    R.string.prediction_engine_summary_hybrid_dual,
                    hybridLabel,
                    neuralHybridLabel)
                : getString(R.string.prediction_engine_summary_missing_model);
        break;
      case "neural":
        final String neuralLabel =
            resolveActiveModelLabel(PresageModelDefinition.EngineType.NEURAL);
        summary =
            neuralLabel != null
                ? getString(R.string.prediction_engine_summary_neural, neuralLabel)
                : getString(R.string.prediction_engine_summary_missing_model);
        break;
      case "none":
      default:
        summary = getString(R.string.prediction_engine_summary_disabled);
        break;
    }
    mPredictionEnginePreference.setSummary(summary);
  }

  private void updatePredictionEnginePreferenceEnabled() {
    if (mPredictionEnginePreference == null) {
      return;
    }
    final boolean suggestionsEnabled = isNextWordSuggestionsEnabled();
    mPredictionEnginePreference.setEnabled(suggestionsEnabled);
    if (!suggestionsEnabled) {
      // ensure summary reflects disabled state
      updatePredictionEnginePreferenceSummary("none");
    } else {
      updatePredictionEnginePreferenceSummary();
    }
  }

  private CharSequence buildNeuralFailureSummary(
      @NonNull NeuralFailureStatus status, @NonNull String currentMode) {
    final CharSequence reason = buildNeuralFailureReason(status);
    if ("neural".equals(currentMode) || "hybrid".equals(currentMode)) {
      return getString(R.string.prediction_engine_summary_neural_failure, reason);
    }
    return getString(
        R.string.prediction_engine_summary_neural_failure_fallback,
        reason,
        resolveModeLabel(currentMode));
  }

  private CharSequence buildNeuralFailureReason(@NonNull NeuralFailureStatus status) {
    if (status.timestamp > 0L) {
      final CharSequence relativeTime =
          DateUtils.getRelativeTimeSpanString(
              status.timestamp,
              System.currentTimeMillis(),
              DateUtils.MINUTE_IN_MILLIS,
              DateUtils.FORMAT_ABBREV_RELATIVE);
      return getString(
          R.string.prediction_engine_neural_failure_reason_relative, relativeTime, status.message);
    }
    return status.message;
  }

  @Nullable
  private NeuralFailureStatus getLastNeuralFailureStatus() {
    if (!isAdded()) {
      return null;
    }
    final String storedValue =
        AnyApplication.prefs(requireContext())
            .getString(
                R.string.settings_key_prediction_engine_last_neural_error,
                R.string.settings_default_prediction_engine_last_neural_error)
            .get();
    if (TextUtils.isEmpty(storedValue)) {
      return null;
    }
    long timestamp = 0L;
    String message = storedValue;
    final int delimiterIndex = storedValue.indexOf('|');
    if (delimiterIndex >= 0) {
      final String timestampPart = storedValue.substring(0, delimiterIndex);
      if (!TextUtils.isEmpty(timestampPart)) {
        try {
          timestamp = Long.parseLong(timestampPart);
        } catch (NumberFormatException ignored) {
          timestamp = 0L;
        }
      }
      message = storedValue.substring(Math.min(delimiterIndex + 1, storedValue.length()));
    }
    if (TextUtils.isEmpty(message)) {
      message = getString(R.string.prediction_engine_error_unknown);
    }
    return new NeuralFailureStatus(timestamp, message);
  }

  @NonNull
  private CharSequence resolveModeLabel(@NonNull String mode) {
    switch (mode) {
      case "ngram":
        return getString(R.string.prediction_engine_mode_ngram);
      case "neural":
        return getString(R.string.prediction_engine_mode_neural);
      case "hybrid":
        return getString(R.string.prediction_engine_mode_hybrid);
      case "none":
      default:
        return getString(R.string.prediction_engine_mode_none);
    }
  }

  private static final class NeuralFailureStatus {
    final long timestamp;
    @NonNull final String message;

    NeuralFailureStatus(long timestamp, @NonNull String message) {
      this.timestamp = timestamp;
      this.message = message;
    }
  }

  private void updateManageModelsSummary() {
    if (mManageModelsPreference == null || !isAdded()) {
      return;
    }

    final String ngramLabel = resolveActiveModelLabel(PresageModelDefinition.EngineType.NGRAM);
    final String neuralLabel = resolveActiveModelLabel(PresageModelDefinition.EngineType.NEURAL);
    final String baseSummary;
    if (TextUtils.isEmpty(ngramLabel) && TextUtils.isEmpty(neuralLabel)) {
      baseSummary = getString(R.string.presage_models_manage_summary_empty);
    } else {
      final String displayNgram =
          TextUtils.isEmpty(ngramLabel)
              ? getString(R.string.presage_models_manage_summary_unavailable)
              : ngramLabel;
      final String displayNeural =
          TextUtils.isEmpty(neuralLabel)
              ? getString(R.string.presage_models_manage_summary_unavailable)
              : neuralLabel;
      baseSummary =
          getString(
              R.string.presage_models_manage_summary_active_multi, displayNgram, displayNeural);
    }
    final NeuralFailureStatus failureStatus = getLastNeuralFailureStatus();
    if (failureStatus != null) {
      final CharSequence reason = buildNeuralFailureReason(failureStatus);
      mManageModelsPreference.setSummary(
          getString(R.string.presage_models_manage_summary_with_error, baseSummary, reason));
    } else {
      mManageModelsPreference.setSummary(baseSummary);
    }
  }

  private boolean hasModelsForEngine(@NonNull PresageModelDefinition.EngineType engineType) {
    if (mPresageModelStore == null) {
      return false;
    }
    final List<PresageModelDefinition> definitions = mPresageModelStore.listAvailableModels();
    for (PresageModelDefinition definition : definitions) {
      if (definition.getEngineType() == engineType) {
        return true;
      }
    }
    return false;
  }

  private boolean isNextWordSuggestionsEnabled() {
    if (mSharedPreferences == null) {
      mSharedPreferences = getPreferenceManager().getSharedPreferences();
    }
    if (mSharedPreferences == null || mNextWordModePrefKey == null) {
      return true;
    }
    final String currentValue =
        mSharedPreferences.getString(
            mNextWordModePrefKey,
            getString(R.string.settings_default_next_words_dictionary_type));
    return currentValue == null || !"off".equals(currentValue);
  }

  private boolean requiresPresageEngine(@NonNull String modeValue) {
    return "ngram".equals(modeValue) || "hybrid".equals(modeValue);
  }

  @Nullable
  private String resolveActiveModelLabel(
      @NonNull PresageModelDefinition.EngineType engineType) {
    if (mPresageModelStore == null) {
      return null;
    }
    final List<PresageModelDefinition> definitions = mPresageModelStore.listAvailableModels();
    if (definitions.isEmpty()) {
      return null;
    }

    final String selectedId = mPresageModelStore.getSelectedModelId(engineType);
    String fallback = null;
    for (PresageModelDefinition definition : definitions) {
      if (definition.getEngineType() != engineType) {
        continue;
      }
      if (fallback == null) {
        fallback = definition.getLabel();
      }
      if (!TextUtils.isEmpty(selectedId) && selectedId.equals(definition.getId())) {
        return definition.getLabel();
      }
    }

    return fallback;
  }

  @Nullable
  private String resolveActiveModelLabel() {
    return resolveActiveModelLabel(PresageModelDefinition.EngineType.NGRAM);
  }

  private void showSuggestionsDisabledDialog() {
    if (!isAdded()) {
      return;
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.prediction_engine_suggestions_disabled_title)
        .setMessage(R.string.prediction_engine_suggestions_disabled_message)
        .setPositiveButton(R.string.prediction_engine_suggestions_disabled_enable_action, null)
        .show();
  }

  private void showMissingModelDialog(
      @NonNull PresageModelDefinition.EngineType missingEngine) {
    if (!isAdded()) {
      return;
    }
    final int titleRes =
        missingEngine == PresageModelDefinition.EngineType.NEURAL
            ? R.string.prediction_engine_missing_model_dialog_title_neural
            : R.string.prediction_engine_missing_model_dialog_title;
    final int messageRes =
        missingEngine == PresageModelDefinition.EngineType.NEURAL
            ? R.string.prediction_engine_missing_model_dialog_message_neural
            : R.string.prediction_engine_missing_model_dialog_message;
    new AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(
            R.string.prediction_engine_missing_model_dialog_action,
            (dialog, which) -> navigateToPresageModels())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void navigateToPresageModels() {
    if (!isAdded()) {
      return;
    }
    final View view = getView();
    if (view == null) {
      return;
    }
    Navigation.findNavController(view)
        .navigate(
            NextWordSettingsFragmentDirections
                .actionNextWordSettingsFragmentToPresageModelsFragment());
  }

  private void loadUsageStatistics() {
    findPreference("clear_next_word_data").setEnabled(false);
    ((PreferenceCategory) findPreference("next_word_stats")).removeAll();

    mDisposable.add(
        createDictionaryAddOnFragment(this)
            .subscribeOn(RxSchedulers.background())
            .map(
                pair -> {
                  NextWordDictionary nextWordDictionary =
                      new NextWordDictionary(pair.second.getContext(), pair.first.getLanguage());
                  nextWordDictionary.load();
                  return Triple.create(
                      pair.second, pair.first, nextWordDictionary.dumpDictionaryStatistics());
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                triple -> {
                  final FragmentActivity activity = triple.getFirst().requireActivity();
                  Preference localeData = new Preference(activity);
                  final DictionaryAddOnAndBuilder addOn = triple.getSecond();
                  localeData.setKey(addOn.getLanguage() + "_stats");
                  localeData.setTitle(addOn.getLanguage() + " - " + addOn.getName());
                  final NextWordStatistics statistics = triple.getThird();
                  if (statistics.firstWordCount == 0) {
                    localeData.setSummary(R.string.next_words_statistics_no_usage);
                  } else {
                    localeData.setSummary(
                        activity.getString(
                            R.string.next_words_statistics_count,
                            statistics.firstWordCount,
                            statistics.secondWordCount / statistics.firstWordCount));
                  }
                  localeData.setPersistent(false);

                  ((PreferenceCategory) triple.getFirst().findPreference("next_word_stats"))
                      .addPreference(localeData);
                },
                t -> {},
                () -> findPreference("clear_next_word_data").setEnabled(true)));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mDisposable.dispose();
  }
}
