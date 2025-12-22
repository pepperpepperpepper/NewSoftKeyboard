package com.google.android.voiceime;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

/**
 * Owns wiring of {@link VoiceRecognitionTrigger} callbacks so the IME service remains focused on
 * state updates and rendering.
 *
 * <p>This controller is intentionally UI-agnostic: it routes state and errors to the host on the
 * main thread, but it does not show toasts or dialogs itself.
 */
public final class VoiceImeController {

  public enum VoiceInputState {
    IDLE,
    RECORDING,
    WAITING,
    ERROR
  }

  public interface HostCallbacks {
    void updateVoiceKeyState();

    void updateSpaceBarRecordingStatus(boolean isRecording);

    void updateVoiceInputStatus(@NonNull VoiceInputState state);

    void onVoiceError(@NonNull String error);
  }

  @NonNull private final VoiceRecognitionTrigger trigger;
  @NonNull private final HostCallbacks host;
  @NonNull private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private volatile boolean isRecording;
  @NonNull private volatile VoiceInputState currentState = VoiceInputState.IDLE;

  public VoiceImeController(
      @NonNull VoiceRecognitionTrigger trigger, @NonNull HostCallbacks hostCallbacks) {
    this.trigger = trigger;
    this.host = hostCallbacks;
  }

  public boolean isInstalled() {
    return trigger.isInstalled();
  }

  public boolean isRecording() {
    return isRecording;
  }

  @NonNull
  public VoiceInputState getCurrentState() {
    return currentState;
  }

  public void onStartInputView() {
    trigger.onStartInputView();
  }

  public void startVoiceRecognition(@NonNull String language) {
    trigger.startVoiceRecognition(language);
  }

  public void attachCallbacks() {
    trigger.setRecordingStateCallback(
        isRecording ->
            postToMainThread(
                () -> {
                  this.isRecording = isRecording;
                  if (isRecording) {
                    updateState(VoiceInputState.RECORDING);
                  } else if (currentState == VoiceInputState.RECORDING) {
                    updateState(VoiceInputState.IDLE);
                  }
                  host.updateVoiceKeyState();
                  host.updateSpaceBarRecordingStatus(isRecording);
                }));

    trigger.setTranscriptionStateCallback(
        isTranscribing ->
            postToMainThread(
                () ->
                    host.updateVoiceInputStatus(
                        updateState(
                            isTranscribing ? VoiceInputState.WAITING : VoiceInputState.IDLE))));

    trigger.setTranscriptionErrorCallback(
        error ->
            postToMainThread(
                () -> {
                  host.updateVoiceInputStatus(updateState(VoiceInputState.ERROR));
                  host.onVoiceError(error);
                }));

    trigger.setRecordingEndedCallback(
        () ->
            postToMainThread(
                () -> host.updateVoiceInputStatus(updateState(VoiceInputState.WAITING))));

    trigger.setTextWrittenCallback(
        text -> {
          // No-op by default; keep callback connected so triggers don't hold stale references.
        });
  }

  private void postToMainThread(@NonNull Runnable action) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action.run();
    } else {
      mainHandler.post(action);
    }
  }

  @NonNull
  private VoiceInputState updateState(@NonNull VoiceInputState newState) {
    currentState = newState;
    return newState;
  }
}
