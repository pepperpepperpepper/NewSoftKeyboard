package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.DictionaryAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordDictionary;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordStatistics;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

/**
 * Loads next-word usage statistics into a preferences category. Extracted to trim {@link
 * NextWordSettingsFragment} while keeping behavior identical.
 */
final class NextWordUsageStatsLoader {

  private NextWordUsageStatsLoader() {}

  static void load(
      @NonNull Context context,
      @NonNull Preference clearDataPreference,
      @NonNull PreferenceCategory statsCategory,
      @NonNull CompositeDisposable disposables) {
    clearDataPreference.setEnabled(false);
    statsCategory.removeAll();

    disposables.add(
        Observable.fromIterable(
                NskApplicationBase.getExternalDictionaryFactory(context).getAllAddOns())
            .filter(addOn -> addOn.getLanguage() != null && !addOn.getLanguage().isEmpty())
            .distinct(DictionaryAddOnAndBuilder::getLanguage)
            .subscribeOn(RxSchedulers.background())
            .map(
                addOn -> {
                  NextWordDictionary nextWordDictionary =
                      new NextWordDictionary(context.getApplicationContext(), addOn.getLanguage());
                  nextWordDictionary.load();
                  try {
                    return new LoadedStats(addOn, nextWordDictionary.dumpDictionaryStatistics());
                  } finally {
                    nextWordDictionary.close();
                  }
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                loaded -> addStatsPreference(context, statsCategory, loaded),
                throwable -> {},
                () -> clearDataPreference.setEnabled(true)));
  }

  private static void addStatsPreference(
      @NonNull Context context,
      @NonNull PreferenceCategory statsCategory,
      @NonNull LoadedStats loaded) {
    Preference localeData = new Preference(context);
    localeData.setKey(loaded.addOn.getLanguage() + "_stats");
    localeData.setTitle(loaded.addOn.getLanguage() + " - " + loaded.addOn.getName());

    if (loaded.statistics.firstWordCount == 0) {
      localeData.setSummary(R.string.next_words_statistics_no_usage);
    } else {
      localeData.setSummary(
          context.getString(
              R.string.next_words_statistics_count,
              loaded.statistics.firstWordCount,
              loaded.statistics.secondWordCount / loaded.statistics.firstWordCount));
    }

    localeData.setPersistent(false);
    statsCategory.addPreference(localeData);
  }

  private static final class LoadedStats {
    final DictionaryAddOnAndBuilder addOn;
    final NextWordStatistics statistics;

    LoadedStats(DictionaryAddOnAndBuilder addOn, NextWordStatistics statistics) {
      this.addOn = addOn;
      this.statistics = statistics;
    }
  }
}
