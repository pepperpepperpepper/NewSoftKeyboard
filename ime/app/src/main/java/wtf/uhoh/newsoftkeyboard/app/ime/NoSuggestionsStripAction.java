package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

/**
 * Action shown when the current input field requests {@code TYPE_TEXT_FLAG_NO_SUGGESTIONS} and the
 * user chose to respect it.
 *
 * <p>This keeps the strip visible so users understand why suggestions are missing and have a quick
 * path to the setting.
 */
final class NoSuggestionsStripAction implements KeyboardViewContainerView.StripActionProvider {

  private View rootView;
  private boolean requestedVisible;

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    final Context context = parent.getContext();
    rootView =
        LayoutInflater.from(context).inflate(R.layout.no_suggestions_strip_action, parent, false);
    rootView.setOnClickListener(
        view ->
            SettingsLauncher.launchTypingSettings(
                context, context.getString(R.string.settings_key_respect_app_no_suggestions_flag)));
    setVisible(requestedVisible);
    return rootView;
  }

  @Override
  public void onRemoved() {
    rootView = null;
  }

  void setVisible(boolean visible) {
    requestedVisible = visible;
    if (rootView != null) {
      rootView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
  }
}
