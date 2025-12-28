package wtf.uhoh.newsoftkeyboard.app.ime.hosts;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import java.util.function.BooleanSupplier;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;

public final class ImeVoiceInputCallbacks implements VoiceImeController.HostCallbacks {

  public static final class Callbacks {
    @NonNull private final Runnable updateVoiceKeyState;
    @NonNull private final java.util.function.Consumer<Boolean> updateSpaceBarRecordingStatus;
    @NonNull private final java.util.function.Consumer<VoiceInputState> updateVoiceInputStatus;

    public Callbacks(
        @NonNull Runnable updateVoiceKeyState,
        @NonNull java.util.function.Consumer<Boolean> updateSpaceBarRecordingStatus,
        @NonNull java.util.function.Consumer<VoiceInputState> updateVoiceInputStatus) {
      this.updateVoiceKeyState = updateVoiceKeyState;
      this.updateSpaceBarRecordingStatus = updateSpaceBarRecordingStatus;
      this.updateVoiceInputStatus = updateVoiceInputStatus;
    }

    void updateVoiceKeyState() {
      updateVoiceKeyState.run();
    }

    void updateSpaceBarRecordingStatus(boolean isRecording) {
      updateSpaceBarRecordingStatus.accept(isRecording);
    }

    void updateVoiceInputStatus(@NonNull VoiceInputState state) {
      updateVoiceInputStatus.accept(state);
    }
  }

  @NonNull private final ImeServiceBase service;
  @NonNull private final Callbacks callbacks;
  @NonNull private final BooleanSupplier retryLastTranscription;
  @NonNull private final Runnable discardPendingTranscription;
  @Nullable private AlertDialog currentErrorDialog;

  public ImeVoiceInputCallbacks(
      @NonNull ImeServiceBase service,
      @NonNull Callbacks callbacks,
      @NonNull BooleanSupplier retryLastTranscription,
      @NonNull Runnable discardPendingTranscription) {
    this.service = service;
    this.callbacks = callbacks;
    this.retryLastTranscription = retryLastTranscription;
    this.discardPendingTranscription = discardPendingTranscription;
  }

  @Override
  public void updateVoiceKeyState() {
    callbacks.updateVoiceKeyState();
  }

  @Override
  public void updateSpaceBarRecordingStatus(boolean isRecording) {
    callbacks.updateSpaceBarRecordingStatus(isRecording);
  }

  @Override
  public void updateVoiceInputStatus(VoiceInputState state) {
    callbacks.updateVoiceInputStatus(state);
  }

  @Override
  public void onVoiceError(@NonNull String error) {
    dismissErrorDialog();
    final AlertDialog.Builder builder =
        new AlertDialog.Builder(service, R.style.Theme_NskAlertDialog);
    builder.setTitle("Voice transcription failed");
    builder.setMessage(error + "\n\nTry again?");
    builder.setPositiveButton(
        "Try again",
        (dialog, which) -> {
          dialog.dismiss();
          if (!retryLastTranscription.getAsBoolean()) {
            android.widget.Toast.makeText(service, error, android.widget.Toast.LENGTH_LONG).show();
          }
        });
    builder.setNegativeButton(
        android.R.string.cancel,
        (dialog, which) -> {
          dialog.dismiss();
          discardPendingTranscription.run();
        });
    builder.setOnCancelListener(dialog -> discardPendingTranscription.run());

    final AlertDialog dialog = builder.create();
    dialog.setOnDismissListener(dialogInterface -> currentErrorDialog = null);
    if (attachDialogToImeWindow(dialog)) {
      dialog.show();
      currentErrorDialog = dialog;
    } else {
      android.widget.Toast.makeText(service, error, android.widget.Toast.LENGTH_LONG).show();
    }
  }

  private void dismissErrorDialog() {
    final AlertDialog dialog = currentErrorDialog;
    if (dialog != null) {
      currentErrorDialog = null;
      dialog.dismiss();
    }
  }

  private boolean attachDialogToImeWindow(@NonNull AlertDialog dialog) {
    final Window window = dialog.getWindow();
    if (window == null) {
      return false;
    }
    if (!(service.getInputView() instanceof View)) {
      return false;
    }
    final View inputView = (View) service.getInputView();
    final WindowManager.LayoutParams lp = window.getAttributes();
    lp.token = inputView.getWindowToken();
    lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    window.setAttributes(lp);
    window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    return true;
  }
}
