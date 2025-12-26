package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.function.Consumer;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

/** Handles async add/remove operations against the user dictionary. */
final class UserDictionaryWorker {

  interface Host {
    Suggest suggest();

    @Nullable
    CandidateView candidateView();
  }

  private final Host host;

  UserDictionaryWorker(Host host) {
    this.host = host;
  }

  void addWordToDictionary(String word, Consumer<Disposable> disposableAdder) {
    disposableAdder.accept(
        Observable.just(word)
            .subscribeOn(wtf.uhoh.newsoftkeyboard.rx.RxSchedulers.background())
            .map(host.suggest()::addWordToUserDictionary)
            .filter(added -> added)
            .observeOn(wtf.uhoh.newsoftkeyboard.rx.RxSchedulers.mainThread())
            .subscribe(
                added -> {
                  CandidateView candidateView = host.candidateView();
                  if (candidateView != null) {
                    candidateView.notifyAboutWordAdded(word);
                  }
                },
                e -> Logger.w("NSKUDict", e, "Failed to add word '%s' to user-dictionary!", word)));
  }

  void removeFromUserDictionary(String wordToRemove, Consumer<Disposable> disposableAdder) {
    disposableAdder.accept(
        Observable.just(wordToRemove)
            .subscribeOn(wtf.uhoh.newsoftkeyboard.rx.RxSchedulers.background())
            .map(
                word -> {
                  host.suggest().removeWordFromUserDictionary(word);
                  return word;
                })
            .observeOn(wtf.uhoh.newsoftkeyboard.rx.RxSchedulers.mainThread())
            .subscribe(
                word -> {
                  CandidateView candidateView = host.candidateView();
                  if (candidateView != null) {
                    candidateView.notifyAboutRemovedWord(word);
                  }
                },
                e ->
                    Logger.w(
                        "NSKUDict",
                        e,
                        "Failed to remove word '%s' from user-dictionary!",
                        wordToRemove)));
  }
}
