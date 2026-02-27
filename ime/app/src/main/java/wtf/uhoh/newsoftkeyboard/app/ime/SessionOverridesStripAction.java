package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

final class SessionOverridesStripAction implements KeyboardViewContainerView.StripActionProvider {

  @NonNull private final Runnable clearSessionOverrides;
  private View rootView;

  SessionOverridesStripAction(@NonNull Runnable clearSessionOverrides) {
    this.clearSessionOverrides = clearSessionOverrides;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    final Context context = parent.getContext();
    rootView =
        LayoutInflater.from(context)
            .inflate(R.layout.session_overrides_strip_action, parent, false);
    rootView.setOnClickListener(view -> clearSessionOverrides.run());
    return rootView;
  }

  @Override
  public void onRemoved() {
    rootView = null;
  }
}
