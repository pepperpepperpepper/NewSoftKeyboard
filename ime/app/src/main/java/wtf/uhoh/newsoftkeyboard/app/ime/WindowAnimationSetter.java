package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import android.view.Window;
import io.reactivex.disposables.Disposable;
import wtf.uhoh.newsoftkeyboard.app.prefs.AnimationsLevel;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;

/** Applies window animation style based on user preference. */
public final class WindowAnimationSetter {

  private WindowAnimationSetter() {}

  public static Disposable subscribe(Context context, Window window) {
    return AnimationsLevel.createPrefsObservable(context)
        .subscribe(
            animationsLevel -> {
              final int fancyAnimation =
                  context
                      .getResources()
                      .getIdentifier("Animation_InputMethodFancy", "style", "android");
              if (window == null) return;

              if (fancyAnimation != 0) {
                wtf.uhoh.newsoftkeyboard.base.utils.Logger.i(
                    "NSK-WINDOW-ANIM",
                    "Found Animation_InputMethodFancy as %d, using it",
                    fancyAnimation);
                window.setWindowAnimations(fancyAnimation);
              } else {
                wtf.uhoh.newsoftkeyboard.base.utils.Logger.w(
                    "NSK-WINDOW-ANIM",
                    "Could not find Animation_InputMethodFancy, using default animation");
                window.setWindowAnimations(android.R.style.Animation_InputMethod);
              }
            },
            GenericOnError.onError("AnimationsLevel"));
  }
}
