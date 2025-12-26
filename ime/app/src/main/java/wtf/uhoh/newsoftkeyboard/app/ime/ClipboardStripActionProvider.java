package wtf.uhoh.newsoftkeyboard.app.ime;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

@VisibleForTesting
class ClipboardStripActionProvider implements KeyboardViewContainerView.StripActionProvider {

  interface ClipboardActionOwner {
    @NonNull
    Context getContext();

    void outputClipboardText();

    void showAllClipboardOptions();
  }

  private final ClipboardActionOwner owner;
  private View rootView;
  private ViewGroup parentView;
  private TextView clipboardText;
  private Animator hideClipboardTextAnimator;

  ClipboardStripActionProvider(@NonNull ClipboardActionOwner owner) {
    this.owner = owner;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    parentView = parent;
    rootView =
        LayoutInflater.from(owner.getContext())
            .inflate(R.layout.clipboard_suggestion_action, parentView, false);
    hideClipboardTextAnimator =
        AnimatorInflater.loadAnimator(parent.getContext(), R.animator.clipboard_text_to_gone);
    clipboardText = rootView.findViewById(R.id.clipboard_suggestion_text);
    hideClipboardTextAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            final TextView textView = clipboardText;
            if (textView != null) {
              textView.setVisibility(View.GONE);
            }
          }
        });
    rootView.setOnClickListener(view -> owner.outputClipboardText());
    rootView.setOnLongClickListener(
        v -> {
          owner.showAllClipboardOptions();
          return true;
        });

    return rootView;
  }

  @Override
  public void onRemoved() {
    if (hideClipboardTextAnimator != null) hideClipboardTextAnimator.cancel();
    clipboardText = null;
    rootView = null;
  }

  boolean isVisible() {
    return rootView != null;
  }

  boolean isFullyVisible() {
    return clipboardText != null && clipboardText.getVisibility() == View.VISIBLE;
  }

  void setAsHint(boolean now) {
    if (now) {
      clipboardText.setVisibility(View.GONE);
    } else if (clipboardText.getVisibility() != View.GONE
        && !hideClipboardTextAnimator.isStarted()) {
      clipboardText.setPivotX(clipboardText.getWidth());
      clipboardText.setPivotY(clipboardText.getHeight() / 2f);
      hideClipboardTextAnimator.setTarget(clipboardText);
      hideClipboardTextAnimator.start();
    }
    parentView.requestLayout();
  }

  void setClipboardText(CharSequence text, boolean isSecured) {
    hideClipboardTextAnimator.cancel();
    clipboardText.setVisibility(View.VISIBLE);
    clipboardText.setScaleX(1f);
    clipboardText.setScaleY(1f);
    clipboardText.setAlpha(1f);
    clipboardText.setSelected(true);
    if (isSecured) clipboardText.setText("**********");
    else clipboardText.setText(text);
    parentView.requestLayout();
  }
}
