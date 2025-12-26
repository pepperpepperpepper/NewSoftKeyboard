package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.DictionaryAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordDictionary;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

/**
 * Clears all next-word dictionaries for every installed language. Extracted to shrink {@link
 * NextWordSettingsFragment}.
 */
final class NextWordDataCleaner {

  private NextWordDataCleaner() {}

  static void clearAll(
      @NonNull Context context,
      @NonNull CompositeDisposable disposables,
      @NonNull Runnable onComplete) {
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
                    nextWordDictionary.clearData();
                  } finally {
                    nextWordDictionary.close();
                  }
                  return true;
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(ignore -> {}, throwable -> onComplete.run(), onComplete::run));
  }
}
