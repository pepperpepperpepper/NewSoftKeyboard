package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

/**
 * The persistent "open clipboard history" doorway in the action strip.
 *
 * <p>This is the stable, discoverable entrance to the clipboard picker: a small icon that is shown
 * whenever there is history to browse and whose single tap always does the same, non-destructive
 * thing — {@link ClipboardStripActionProvider.ClipboardActionOwner#showAllClipboardOptions()}. It
 * deliberately carries <em>no</em> preview text and never pastes; pasting the latest entry is the
 * job of the separate, transient {@link ClipboardStripActionProvider} hint. See {@code
 * clipboard_access_plan.md}.
 */
@VisibleForTesting
class ClipboardDoorwayActionProvider implements KeyboardViewContainerView.StripActionProvider {

  private final ClipboardStripActionProvider.ClipboardActionOwner owner;
  private View rootView;

  ClipboardDoorwayActionProvider(@NonNull ClipboardStripActionProvider.ClipboardActionOwner owner) {
    this.owner = owner;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    rootView =
        LayoutInflater.from(owner.getContext())
            .inflate(R.layout.clipboard_action_icon, parent, false);
    rootView.setOnClickListener(view -> owner.showAllClipboardOptions());
    return rootView;
  }

  @Override
  public void onRemoved() {
    rootView = null;
  }

  boolean isVisible() {
    return rootView != null;
  }
}
