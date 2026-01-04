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
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;

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
  @NonNull private final BooleanSupplier savePendingRecording;
  @NonNull private final Runnable discardPendingTranscription;
  @Nullable private AlertDialog currentErrorDialog;
  @Nullable private VoiceErrorStripActionProvider currentErrorStripAction;
  private boolean restoreStripVisibilityAfterError = false;
  @Nullable private VoiceStatusStripActionProvider currentStatusStripAction;
  private boolean restoreStripVisibilityAfterStatus = false;
  @NonNull private VoiceInputState currentVoiceState = VoiceInputState.IDLE;

  public ImeVoiceInputCallbacks(
      @NonNull ImeServiceBase service,
      @NonNull Callbacks callbacks,
      @NonNull BooleanSupplier retryLastTranscription,
      @NonNull BooleanSupplier savePendingRecording,
      @NonNull Runnable discardPendingTranscription) {
    this.service = service;
    this.callbacks = callbacks;
    this.retryLastTranscription = retryLastTranscription;
    this.savePendingRecording = savePendingRecording;
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
    currentVoiceState = state;
    callbacks.updateVoiceInputStatus(state);
    if (state != VoiceInputState.ERROR) {
      dismissErrorUi();
    }
    if (state == VoiceInputState.RECORDING || state == VoiceInputState.WAITING) {
      showStatusUi(state);
    } else {
      dismissStatusStripAction();
    }
  }

  @Override
  public void onVoiceError(@NonNull String error) {
    dismissStatusStripAction();
    dismissErrorUi();

    final String displayMessage = resolveUserVisibleErrorMessage(error);
    final String messageToShow =
        BuildConfig.DEBUG ? appendRawErrorIfUseful(displayMessage, error) : displayMessage;

    final KeyboardViewContainerView container = service.getInputViewContainer();
    if (container != null) {
      restoreStripVisibilityAfterError =
          container.getCandidateView() != null
              && container.getCandidateView().getVisibility() != View.VISIBLE;
      container.setActionsStripVisibility(true);

      currentErrorStripAction =
          new VoiceErrorStripActionProvider(
              service,
              messageToShow,
              retryLastTranscription,
              savePendingRecording,
              () -> {
                discardPendingTranscription.run();
                callbacks.updateVoiceInputStatus(VoiceInputState.IDLE);
              },
              () -> {
                dismissErrorStripAction();
                clearVoiceErrorVisualStateIfNeeded();
              });
      container.addStripAction(currentErrorStripAction, true);
      return;
    }

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(service, R.style.Theme_NskAlertDialog);
    builder.setTitle(R.string.voice_error_default_message);
    builder.setMessage(messageToShow);
    builder.setPositiveButton(R.string.voice_error_retry, null);
    builder.setNeutralButton(R.string.voice_error_save, null);
    builder.setNegativeButton(R.string.voice_error_discard, null);
    builder.setOnCancelListener(
        dialog -> {
          discardPendingTranscription.run();
          clearVoiceErrorVisualStateIfNeeded();
        });

    final AlertDialog dialog = builder.create();
    dialog.setOnShowListener(
        dialogInterface -> {
          final android.widget.Button retryButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
          if (retryButton != null) {
            retryButton.setOnClickListener(
                v -> {
                  if (retryLastTranscription.getAsBoolean()) {
                    clearVoiceErrorVisualStateIfNeeded();
                    dialog.dismiss();
                  } else {
                    // keep the dialog open; caller will see the original error message
                  }
                });
          }

          final android.widget.Button saveButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
          if (saveButton != null) {
            saveButton.setOnClickListener(
                v -> {
                  if (!savePendingRecording.getAsBoolean()) {
                    // keep the dialog open; caller can choose discard
                  }
                });
          }

          final android.widget.Button discardButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
          if (discardButton != null) {
            discardButton.setOnClickListener(
                v -> {
                  discardPendingTranscription.run();
                  clearVoiceErrorVisualStateIfNeeded();
                  dialog.dismiss();
                });
          }
        });
    dialog.setOnDismissListener(dialogInterface -> currentErrorDialog = null);
    if (attachDialogToImeWindow(dialog)) {
      dialog.show();
      currentErrorDialog = dialog;
    } else {
      // If we can't attach to the IME window, avoid showing a toast from the IME service.
    }
  }

  @NonNull
  private String resolveUserVisibleErrorMessage(@NonNull String rawError) {
    final String trimmed = rawError.trim();
    if (trimmed.isEmpty()) {
      return service.getString(R.string.voice_error_default_message);
    }

    // Allow-list known error strings that already come from our own resources (localized).
    // For everything else, show a short localized message and keep details in logs.
    if (trimmed.equals(service.getString(R.string.openai_error_api_key_unset))
        || trimmed.equals(service.getString(R.string.openai_error_microphone_permission))
        || trimmed.equals(service.getString(R.string.openai_error_endpoint_unset))
        || trimmed.equals(service.getString(R.string.openai_error_network))
        || trimmed.equals(service.getString(R.string.openai_error_recording_failed))
        || trimmed.equals(service.getString(R.string.openai_error_transcription_failed))
        || trimmed.equals(service.getString(R.string.speech_to_text_error_save_recording_failed))
        || trimmed.equals(service.getString(R.string.speech_to_text_error_insert_failed))) {
      return trimmed;
    }

    return service.getString(R.string.voice_error_default_message);
  }

  @NonNull
  private static String appendRawErrorIfUseful(
      @NonNull String userMessage, @NonNull String rawError) {
    final String trimmed = rawError.trim();
    if (trimmed.isEmpty() || userMessage.equals(trimmed)) return userMessage;
    // Keep it short to avoid destroying the IME strip layout.
    final String singleLine = trimmed.replace('\n', ' ').replace('\r', ' ').trim();
    final int maxLen = 140;
    final String compact =
        singleLine.length() <= maxLen ? singleLine : singleLine.substring(0, maxLen) + "…";
    return userMessage + "\n" + compact;
  }

  private void dismissErrorUi() {
    dismissErrorDialog();
    dismissErrorStripAction();
  }

  private void showStatusUi(@NonNull VoiceInputState state) {
    final KeyboardViewContainerView container = service.getInputViewContainer();
    if (container == null) return;

    final int messageId =
        state == VoiceInputState.RECORDING
            ? R.string.voice_spacebar_badge_recording
            : R.string.voice_spacebar_badge_waiting;
    final String message = service.getString(messageId);

    final VoiceStatusStripActionProvider provider = currentStatusStripAction;
    if (provider != null) {
      provider.updateMessage(message);
      return;
    }

    restoreStripVisibilityAfterStatus =
        container.getCandidateView() != null
            && container.getCandidateView().getVisibility() != View.VISIBLE;
    container.setActionsStripVisibility(true);

    currentStatusStripAction = new VoiceStatusStripActionProvider(service, message);
    container.addStripAction(currentStatusStripAction, true);
  }

  private void dismissStatusStripAction() {
    final KeyboardViewContainerView container = service.getInputViewContainer();
    final VoiceStatusStripActionProvider provider = currentStatusStripAction;
    currentStatusStripAction = null;
    if (container != null && provider != null) {
      container.removeStripAction(provider);
      if (restoreStripVisibilityAfterStatus) {
        container.setActionsStripVisibility(false);
      }
    }
    restoreStripVisibilityAfterStatus = false;
  }

  private void dismissErrorDialog() {
    final AlertDialog dialog = currentErrorDialog;
    if (dialog != null) {
      currentErrorDialog = null;
      dialog.dismiss();
    }
  }

  private void dismissErrorStripAction() {
    final KeyboardViewContainerView container = service.getInputViewContainer();
    final VoiceErrorStripActionProvider provider = currentErrorStripAction;
    currentErrorStripAction = null;
    if (container != null && provider != null) {
      container.removeStripAction(provider);
      if (restoreStripVisibilityAfterError) {
        container.setActionsStripVisibility(false);
      }
    }
    restoreStripVisibilityAfterError = false;
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

  private void clearVoiceErrorVisualStateIfNeeded() {
    if (currentVoiceState != VoiceInputState.ERROR) return;
    currentVoiceState = VoiceInputState.IDLE;
    callbacks.updateVoiceInputStatus(VoiceInputState.IDLE);
  }
}
