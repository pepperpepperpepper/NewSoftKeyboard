package com.anysoftkeyboard.ime;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InlineSuggestion;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class InlineSuggestionsAction implements KeyboardViewContainerView.StripActionProvider {
  private static final String TAG = "NSKInlineSuggestionsAction";

  private final Function<List<InlineSuggestion>, Void> showSuggestionsFunction;
  private final Runnable removeStripAction;
  private final List<InlineSuggestion> currentSuggestions;

  @Nullable private TextView suggestionsCount;
  @Nullable private ImageView suggestionTypeIcon;

  InlineSuggestionsAction(
      @NonNull Function<List<InlineSuggestion>, Void> showSuggestionsFunction,
      @NonNull Runnable removeStripAction) {
    this.showSuggestionsFunction = showSuggestionsFunction;
    this.removeStripAction = removeStripAction;
    currentSuggestions = new ArrayList<>();
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    View root =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.inline_suggestions_available_action, parent, false);

    root.setOnClickListener(
        view -> {
          Logger.d(TAG, "auto-fill action icon clicked");
          showSuggestionsFunction.apply(currentSuggestions);
          removeStripAction.run();
        });

    suggestionsCount = root.findViewById(R.id.inline_suggestions_strip_text);
    suggestionTypeIcon = root.findViewById(R.id.inline_suggestions_strip_icon);
    updateSuggestionsCountView();
    return root;
  }

  @Override
  public void onRemoved() {
    currentSuggestions.clear();
    suggestionsCount = null;
    suggestionTypeIcon = null;
  }

  void onNewSuggestions(@NonNull List<InlineSuggestion> suggestions) {
    currentSuggestions.clear();
    currentSuggestions.addAll(suggestions);
    updateSuggestionsCountView();
  }

  private void updateSuggestionsCountView() {
    if (suggestionsCount == null) return;

    suggestionsCount.setText(String.format(Locale.getDefault(), "%d", currentSuggestions.size()));
    if (suggestionTypeIcon == null) return;

    if (currentSuggestions.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;

    // taking the type for the icon
    final var hints = currentSuggestions.get(0).getInfo().getAutofillHints();
    var icon = IconType.GENERIC;
    if (hints != null) {
      for (String hint : hints) {
        switch (hint) {
          case "aiai" -> {
            if (icon.priority < IconType.AI.priority) {
              icon = IconType.AI;
            }
          }
          case "smartReply" -> {
            if (icon.priority < IconType.SMART_REPLY.priority) {
              icon = IconType.SMART_REPLY;
            }
          }
        }
      }
      // setting the highest priority icon
      suggestionTypeIcon.setImageResource(icon.drawable);
    }
  }

  private enum IconType {
    GENERIC(0, R.drawable.ic_inline_suggestions),
    AI(1, R.drawable.ic_inline_suggestions_ai),
    SMART_REPLY(2, R.drawable.ic_inline_suggestions_ai_reply);

    final int priority;
    final int drawable;

    IconType(int priority, @DrawableRes int drawable) {
      this.priority = priority;
      this.drawable = drawable;
    }
  }
}
