package com.google.android.voiceime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class VoiceImeControllerTest {

  @Test
  public void stopRecordingSuccessDoesNotEmitIdleBeforeWaiting() {
    final CapturedTriggerCallbacks callbacks = new CapturedTriggerCallbacks();
    final VoiceRecognitionTrigger trigger = callbacks.createTriggerMock();

    final List<VoiceInputState> states = new ArrayList<>();
    final VoiceImeController.HostCallbacks host = createHost(states);
    final VoiceImeController controller = new VoiceImeController(trigger, host);
    controller.attachCallbacks();
    callbacks.assertAllCaptured();

    callbacks.recordingState.get().onRecordingStateChanged(true);
    idleMainThread();
    callbacks.recordingState.get().onRecordingStateChanged(false);
    idleMainThread();
    callbacks.recordingEnded.get().onRecordingEnded();
    idleMainThread();
    callbacks.transcriptionState.get().onTranscriptionStateChanged(true);
    idleMainThread();
    callbacks.transcriptionState.get().onTranscriptionStateChanged(false);
    idleMainThread();
    callbacks.textWritten.get().onTextWritten("hello");
    idleMainThread();

    assertEquals(
        List.of(VoiceInputState.RECORDING, VoiceInputState.WAITING, VoiceInputState.IDLE), states);
  }

  @Test
  public void transcriptionErrorDoesNotEmitIdleBeforeError() {
    final CapturedTriggerCallbacks callbacks = new CapturedTriggerCallbacks();
    final VoiceRecognitionTrigger trigger = callbacks.createTriggerMock();

    final List<VoiceInputState> states = new ArrayList<>();
    final VoiceImeController.HostCallbacks host = createHost(states);
    final VoiceImeController controller = new VoiceImeController(trigger, host);
    controller.attachCallbacks();
    callbacks.assertAllCaptured();

    callbacks.recordingState.get().onRecordingStateChanged(true);
    idleMainThread();
    callbacks.recordingState.get().onRecordingStateChanged(false);
    idleMainThread();
    callbacks.recordingEnded.get().onRecordingEnded();
    idleMainThread();
    callbacks.transcriptionState.get().onTranscriptionStateChanged(true);
    idleMainThread();
    callbacks.transcriptionState.get().onTranscriptionStateChanged(false);
    idleMainThread();
    callbacks.transcriptionError.get().onTranscriptionError("boom");
    idleMainThread();

    assertEquals(
        List.of(VoiceInputState.RECORDING, VoiceInputState.WAITING, VoiceInputState.ERROR), states);
  }

  private static VoiceImeController.HostCallbacks createHost(List<VoiceInputState> states) {
    return new VoiceImeController.HostCallbacks() {
      @Override
      public void updateVoiceKeyState() {}

      @Override
      public void updateSpaceBarRecordingStatus(boolean isRecording) {}

      @Override
      public void updateVoiceInputStatus(VoiceInputState state) {
        states.add(state);
      }

      @Override
      public void onVoiceError(String error) {}
    };
  }

  private static void idleMainThread() {
    shadowOf(Looper.getMainLooper()).idle();
  }

  private static final class CapturedTriggerCallbacks {
    final AtomicReference<VoiceRecognitionTrigger.RecordingStateCallback> recordingState =
        new AtomicReference<>();
    final AtomicReference<VoiceRecognitionTrigger.TranscriptionStateCallback> transcriptionState =
        new AtomicReference<>();
    final AtomicReference<VoiceRecognitionTrigger.TranscriptionErrorCallback> transcriptionError =
        new AtomicReference<>();
    final AtomicReference<VoiceRecognitionTrigger.RecordingEndedCallback> recordingEnded =
        new AtomicReference<>();
    final AtomicReference<VoiceRecognitionTrigger.TextWrittenCallback> textWritten =
        new AtomicReference<>();

    VoiceRecognitionTrigger createTriggerMock() {
      final VoiceRecognitionTrigger trigger = Mockito.mock(VoiceRecognitionTrigger.class);
      Mockito.doAnswer(
              invocation -> {
                recordingState.set(invocation.getArgument(0));
                return null;
              })
          .when(trigger)
          .setRecordingStateCallback(Mockito.any());
      Mockito.doAnswer(
              invocation -> {
                transcriptionState.set(invocation.getArgument(0));
                return null;
              })
          .when(trigger)
          .setTranscriptionStateCallback(Mockito.any());
      Mockito.doAnswer(
              invocation -> {
                transcriptionError.set(invocation.getArgument(0));
                return null;
              })
          .when(trigger)
          .setTranscriptionErrorCallback(Mockito.any());
      Mockito.doAnswer(
              invocation -> {
                recordingEnded.set(invocation.getArgument(0));
                return null;
              })
          .when(trigger)
          .setRecordingEndedCallback(Mockito.any());
      Mockito.doAnswer(
              invocation -> {
                textWritten.set(invocation.getArgument(0));
                return null;
              })
          .when(trigger)
          .setTextWrittenCallback(Mockito.any());
      return trigger;
    }

    void assertAllCaptured() {
      assertNotNull(recordingState.get());
      assertNotNull(transcriptionState.get());
      assertNotNull(transcriptionError.get());
      assertNotNull(recordingEnded.get());
      assertNotNull(textWritten.get());
    }
  }
}
