package wtf.uhoh.newsoftkeyboard.app.dictionaries.content;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.cash.copper.rx2.RxContentResolver;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import wtf.uhoh.newsoftkeyboard.dictionaries.BTreeDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public abstract class ContentObserverDictionary extends BTreeDictionary {

  @Nullable private final Uri mDictionaryChangedUri;
  @NonNull private Disposable mDictionaryReLoaderDisposable = Disposables.empty();
  @NonNull private Disposable mDictionaryChangedDisposable = Disposables.disposed();

  protected ContentObserverDictionary(
      String dictionaryName, Context context, @Nullable Uri dictionaryChangedUri) {
    super(dictionaryName, context);
    mDictionaryChangedUri = dictionaryChangedUri;
  }

  @Override
  protected void loadAllResources() {
    super.loadAllResources();

    if (mDictionaryChangedUri != null && mDictionaryChangedDisposable.isDisposed()) {
      mDictionaryChangedDisposable =
          RxContentResolver.observeQuery(
                  mContext.getContentResolver(),
                  mDictionaryChangedUri,
                  null,
                  null,
                  null,
                  null,
                  true,
                  RxSchedulers.background())
              .subscribeOn(RxSchedulers.background())
              .observeOn(RxSchedulers.mainThread())
              .forEach(query -> onStorageChanged());
    }
  }

  void onStorageChanged() {
    if (isClosed()) return;
    resetDictionary();
    mDictionaryReLoaderDisposable.dispose();
    mDictionaryReLoaderDisposable = DictionaryBackgroundLoader.reloadDictionaryInBackground(this);
  }

  @Override
  protected void closeAllResources() {
    super.closeAllResources();
    mDictionaryReLoaderDisposable.dispose();
    mDictionaryChangedDisposable.dispose();
  }
}
