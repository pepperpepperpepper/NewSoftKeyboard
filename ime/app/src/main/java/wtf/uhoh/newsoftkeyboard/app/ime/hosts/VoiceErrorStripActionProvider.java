package wtf.uhoh.newsoftkeyboard.app.ime.hosts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.function.BooleanSupplier;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

/** Shows third-party voice transcription failures as an IME-attached strip action. */
final class VoiceErrorStripActionProvider implements KeyboardViewContainerView.StripActionProvider {

  @NonNull private final Context context;
  @NonNull private final String errorMessage;
  @NonNull private final BooleanSupplier retryLastTranscription;
  @NonNull private final BooleanSupplier savePendingRecording;
  @NonNull private final Runnable discardPendingTranscription;
  @NonNull private final Runnable onDismissRequested;

  @Nullable private View rootView;
  @Nullable private TextView messageView;
  @Nullable private TextView retryButton;
  @Nullable private TextView saveButton;
  @Nullable private TextView discardButton;

  VoiceErrorStripActionProvider(
      @NonNull Context context,
      @NonNull String errorMessage,
      @NonNull BooleanSupplier retryLastTranscription,
      @NonNull BooleanSupplier savePendingRecording,
      @NonNull Runnable discardPendingTranscription,
      @NonNull Runnable onDismissRequested) {
    this.context = context;
    this.errorMessage = errorMessage;
    this.retryLastTranscription = retryLastTranscription;
    this.savePendingRecording = savePendingRecording;
    this.discardPendingTranscription = discardPendingTranscription;
    this.onDismissRequested = onDismissRequested;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    final View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.voice_error_strip_action, parent, false);
    rootView = view;
    messageView = view.findViewById(R.id.voice_error_message);
    retryButton = view.findViewById(R.id.voice_error_retry);
    saveButton = view.findViewById(R.id.voice_error_save);
    discardButton = view.findViewById(R.id.voice_error_discard);

    updateMessageText(errorMessage);

    if (retryButton != null) {
      retryButton.setOnClickListener(
          v -> {
            if (retryLastTranscription.getAsBoolean()) {
              onDismissRequested.run();
            } else {
              updateMessageText(context.getString(R.string.voice_error_retry_failed));
            }
          });
    }

    if (saveButton != null) {
      saveButton.setOnClickListener(
          v -> {
            if (savePendingRecording.getAsBoolean()) {
              saveButton.setEnabled(false);
              saveButton.setAlpha(0.6f);
              saveButton.setText(R.string.voice_error_saved);
            } else {
              updateMessageText(context.getString(R.string.voice_error_no_recording_to_save));
            }
          });
    }

    if (discardButton != null) {
      discardButton.setOnClickListener(
          v -> {
            discardPendingTranscription.run();
            onDismissRequested.run();
          });
    }

    // Tapping the banner body dismisses it, but does not delete the pending audio.
    view.setOnClickListener(v -> onDismissRequested.run());

    return view;
  }

  @Override
  public void onRemoved() {
    rootView = null;
    messageView = null;
    retryButton = null;
    saveButton = null;
    discardButton = null;
  }

  private void updateMessageText(@NonNull String message) {
    final TextView view = messageView;
    if (view == null) return;
    view.setText(message);
  }
}
