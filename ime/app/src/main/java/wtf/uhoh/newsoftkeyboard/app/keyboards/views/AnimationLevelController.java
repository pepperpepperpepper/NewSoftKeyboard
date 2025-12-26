package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.function.Consumer;
import wtf.uhoh.newsoftkeyboard.app.prefs.AnimationsLevel;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;

/** Owns animation level subject to keep view classes slimmer. */
final class AnimationLevelController {
  private final Subject<AnimationsLevel> subject =
      BehaviorSubject.createDefault(AnimationsLevel.Some);

  Subject<AnimationsLevel> subject() {
    return subject;
  }

  void setLevel(AnimationsLevel level) {
    subject.onNext(level);
  }

  Disposable subscribeWithLogging(String tag, Consumer<AnimationsLevel> onNext) {
    return subject.subscribe(onNext::accept, GenericOnError.onError(tag));
  }
}
