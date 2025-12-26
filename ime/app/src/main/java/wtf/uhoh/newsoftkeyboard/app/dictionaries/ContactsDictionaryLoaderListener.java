package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import static wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader.NO_OP_LISTENER;

import androidx.annotation.NonNull;
import java.util.function.Supplier;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;

final class ContactsDictionaryLoaderListener implements DictionaryBackgroundLoader.Listener {

  @NonNull private final Supplier<Dictionary> mContactsDictionarySupplier;
  @NonNull private final Runnable mOnContactsDictionaryLoadFailed;
  @NonNull private DictionaryBackgroundLoader.Listener mDelegate = NO_OP_LISTENER;

  ContactsDictionaryLoaderListener(
      @NonNull Supplier<Dictionary> contactsDictionarySupplier,
      @NonNull Runnable onContactsDictionaryLoadFailed) {
    mContactsDictionarySupplier = contactsDictionarySupplier;
    mOnContactsDictionaryLoadFailed = onContactsDictionaryLoadFailed;
  }

  void setDelegate(@NonNull DictionaryBackgroundLoader.Listener delegate) {
    mDelegate = delegate;
  }

  @Override
  public void onDictionaryLoadingStarted(Dictionary dictionary) {
    mDelegate.onDictionaryLoadingStarted(dictionary);
  }

  @Override
  public void onDictionaryLoadingDone(Dictionary dictionary) {
    mDelegate.onDictionaryLoadingDone(dictionary);
  }

  @Override
  public void onDictionaryLoadingFailed(Dictionary dictionary, Throwable exception) {
    mDelegate.onDictionaryLoadingFailed(dictionary, exception);
    if (dictionary == mContactsDictionarySupplier.get()) {
      mOnContactsDictionaryLoadFailed.run();
    }
  }
}
