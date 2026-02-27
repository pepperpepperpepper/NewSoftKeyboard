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
    postToMainThread(
        () -> {
          final boolean recordingNow = trigger.isRecording();
          isRecording = recordingNow;

          VoiceInputState stateToRender = currentState;
          if (recordingNow) {
            stateToRender = VoiceInputState.RECORDING;
          } else if (stateToRender == VoiceInputState.RECORDING
              || stateToRender == VoiceInputState.ERROR) {
            stateToRender = VoiceInputState.IDLE;
          }

          currentState = stateToRender;
          host.updateVoiceInputStatus(stateToRender);
          host.updateVoiceKeyState();
          host.updateSpaceBarRecordingStatus(recordingNow);
        });
  }

  public void onFinishInputView() {
    trigger.onFinishInputView();
    postToMainThread(
        () -> {
          isRecording = false;
          currentState = VoiceInputState.IDLE;
          host.updateVoiceInputStatus(VoiceInputState.IDLE);
          host.updateVoiceKeyState();
          host.updateSpaceBarRecordingStatus(false);
        });
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
                    emitState(VoiceInputState.RECORDING);
                  } else if (currentState == VoiceInputState.RECORDING) {
                    // Avoid "RECORDING → IDLE → WAITING" flicker: recording stop implies we're
                    // about to start transcription, or immediately hit an error.
                    emitState(VoiceInputState.WAITING);
                  }
                  host.updateVoiceKeyState();
                  host.updateSpaceBarRecordingStatus(isRecording);
                }));

    trigger.setTranscriptionStateCallback(
        isTranscribing ->
            postToMainThread(
                () -> {
                  if (isTranscribing) {
                    emitState(VoiceInputState.WAITING);
                  } else {
                    // Do not emit IDLE here. Third-party triggers fire:
                    //   transcribing=false → (text written | error)
                    // and transitioning to IDLE would create UI flicker.
                  }
                }));

    trigger.setTranscriptionErrorCallback(
        error ->
            postToMainThread(
                () -> {
                  emitState(VoiceInputState.ERROR);
                  host.onVoiceError(error);
                }));

    trigger.setRecordingEndedCallback(
        () -> postToMainThread(() -> emitState(VoiceInputState.WAITING)));

    trigger.setTextWrittenCallback(text -> postToMainThread(() -> emitState(VoiceInputState.IDLE)));
  }

  private void postToMainThread(@NonNull Runnable action) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action.run();
    } else {
      mainHandler.post(action);
    }
  }

  @NonNull
  private VoiceInputState emitState(@NonNull VoiceInputState newState) {
    if (currentState == newState) return currentState;
    currentState = newState;
    host.updateVoiceInputStatus(newState);
    return newState;
  }
}
