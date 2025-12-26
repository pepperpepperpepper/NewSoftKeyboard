package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import io.reactivex.disposables.CompositeDisposable;
import java.util.function.Consumer;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.prefs.RxSharedPrefs;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;

/** Wires SuggestionsProvider prefs into their targets. */
final class SuggestionsProviderPrefsBinder {

  private SuggestionsProviderPrefsBinder() {}

  static void wire(
      RxSharedPrefs prefs,
      CompositeDisposable disposables,
      Consumer<Boolean> quickFixesEnabledConsumer,
      Consumer<Boolean> quickFixesSecondDisabledConsumer,
      Consumer<Boolean> contactsDictionaryEnabledConsumer,
      Consumer<Boolean> userDictionaryEnabledConsumer,
      Consumer<String> predictionEngineModeConsumer,
      Consumer<String> nextWordAggressivenessConsumer,
      Consumer<String> nextWordDictionaryTypeConsumer) {

    disposables.add(
        prefs
            .getBoolean(R.string.settings_key_quick_fix, R.bool.settings_default_quick_fix)
            .asObservable()
            .subscribe(
                quickFixesEnabledConsumer::accept,
                GenericOnError.onError("settings_key_quick_fix")));

    disposables.add(
        prefs
            .getBoolean(
                R.string.settings_key_quick_fix_second_disabled,
                R.bool.settings_default_key_quick_fix_second_disabled)
            .asObservable()
            .subscribe(
                quickFixesSecondDisabledConsumer::accept,
                GenericOnError.onError("settings_key_quick_fix_second_disable")));

    disposables.add(
        prefs
            .getBoolean(
                R.string.settings_key_use_contacts_dictionary,
                R.bool.settings_default_contacts_dictionary)
            .asObservable()
            .subscribe(
                contactsDictionaryEnabledConsumer::accept,
                GenericOnError.onError("settings_key_use_contacts_dictionary")));

    disposables.add(
        prefs
            .getBoolean(
                R.string.settings_key_use_user_dictionary, R.bool.settings_default_user_dictionary)
            .asObservable()
            .subscribe(
                userDictionaryEnabledConsumer::accept,
                GenericOnError.onError("settings_key_use_user_dictionary")));

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_prediction_engine_mode,
                R.string.settings_default_prediction_engine_mode)
            .asObservable()
            .subscribe(
                predictionEngineModeConsumer::accept,
                GenericOnError.onError("settings_key_prediction_engine_mode")));

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_next_word_suggestion_aggressiveness,
                R.string.settings_default_next_word_suggestion_aggressiveness)
            .asObservable()
            .subscribe(
                nextWordAggressivenessConsumer::accept,
                GenericOnError.onError("settings_key_next_word_suggestion_aggressiveness")));

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_next_word_dictionary_type,
                R.string.settings_default_next_words_dictionary_type)
            .asObservable()
            .subscribe(
                nextWordDictionaryTypeConsumer::accept,
                GenericOnError.onError("settings_key_next_word_dictionary_type")));
  }
}
