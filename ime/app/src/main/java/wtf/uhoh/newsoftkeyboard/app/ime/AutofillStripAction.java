package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

@RequiresApi(Build.VERSION_CODES.O)
final class AutofillStripAction implements KeyboardViewContainerView.StripActionProvider {

  private final Runnable onActionPressed;

  AutofillStripAction(@NonNull Runnable onActionPressed) {
    this.onActionPressed = onActionPressed;
  }

  @NonNull
  @Override
  public View inflateActionView(@NonNull ViewGroup parent) {
    final var root =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.autofill_strip_action, parent, false);
    root.setOnClickListener(v -> onActionPressed.run());
    return root;
  }

  @Override
  public void onRemoved() {}
}
