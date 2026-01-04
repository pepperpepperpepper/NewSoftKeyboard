package wtf.uhoh.newsoftkeyboard.app.ime.hosts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

/** Shows third-party voice recording/transcription status as an IME-attached strip action. */
final class VoiceStatusStripActionProvider
    implements KeyboardViewContainerView.StripActionProvider {

  @NonNull private final Context context;
  @NonNull private String statusMessage;

  @Nullable private TextView messageView;

  VoiceStatusStripActionProvider(@NonNull Context context, @NonNull String statusMessage) {
    this.context = context;
    this.statusMessage = statusMessage;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    final View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.voice_status_strip_action, parent, false);
    messageView = view.findViewById(R.id.voice_status_message);
    updateMessageText(statusMessage);
    return view;
  }

  void updateMessage(@NonNull String statusMessage) {
    this.statusMessage = statusMessage;
    updateMessageText(statusMessage);
  }

  @Override
  public void onRemoved() {
    messageView = null;
  }

  private void updateMessageText(@NonNull String message) {
    final TextView view = messageView;
    if (view == null) return;
    view.setText(message);
  }
}
