package wtf.uhoh.newsoftkeyboard.app.dictionaries.prefsprovider;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ExternalDictionaryFactory;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.UserDictionary;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.prefs.backup.PrefItem;
import wtf.uhoh.newsoftkeyboard.prefs.backup.PrefsProvider;
import wtf.uhoh.newsoftkeyboard.prefs.backup.PrefsRoot;

public class UserDictionaryPrefsProvider implements PrefsProvider {
  private final Context mContext;
  private final Iterable<String> mLocaleToStore;

  public UserDictionaryPrefsProvider(Context context) {
    this(context, ExternalDictionaryFactory.getLocalesFromDictionaryAddOns(context));
  }

  @VisibleForTesting
  UserDictionaryPrefsProvider(Context context, Iterable<String> localeToStore) {
    mContext = context;
    mLocaleToStore = localeToStore;
  }

  @Override
  public String providerId() {
    return "UserDictionaryPrefsProvider";
  }

  @Override
  public PrefsRoot getPrefsRoot() {
    final PrefsRoot root = new PrefsRoot(1);

    for (String locale : mLocaleToStore) {
      final PrefItem localeChild = root.createChild();
      localeChild.addValue("locale", locale);

      TappingUserDictionary dictionary =
          new TappingUserDictionary(
              mContext,
              locale,
              (word, frequency) -> {
                localeChild
                    .createChild()
                    .addValue("word", word)
                    .addValue("freq", Integer.toString(frequency));

                return true;
              });

      dictionary.loadDictionary();

      dictionary.close();
    }

    return root;
  }

  @Override
  public void storePrefsRoot(PrefsRoot prefsRoot) {
    Observable.fromIterable(prefsRoot.getChildren())
        .blockingSubscribe(
            prefItem -> {
              final String locale = prefItem.getValue("locale");
              if (TextUtils.isEmpty(locale)) return;

              final UserDictionary userDictionary =
                  new TappingUserDictionary(
                      mContext, locale, (word, frequency) -> false /*don't read words*/);
              userDictionary.loadDictionary();

              Observable.fromIterable(prefItem.getChildren())
                  .map(
                      prefItem1 ->
                          Pair.create(
                              prefItem1.getValue("word"),
                              Integer.parseInt(prefItem1.getValue("freq"))))
                  .blockingSubscribe(
                      word -> {
                        if (!userDictionary.addWord(word.first, word.second)) {
                          throw new RuntimeException(
                              "Failed to add word to dictionary."
                                  + " Word: "
                                  + word.first
                                  + ", dictionary is closed? "
                                  + userDictionary.isClosed());
                        }
                      },
                      throwable -> {
                        Logger.w(
                            "UserDictionaryPrefsProvider",
                            throwable,
                            "Failed to add words to dictionary!");
                        throwable.printStackTrace();
                      });

              userDictionary.close();
            },
            throwable -> {
              Logger.w(
                  "UserDictionaryPrefsProvider", throwable, "Failed to load locale dictionary!");
              throwable.printStackTrace();
            });
  }
}
