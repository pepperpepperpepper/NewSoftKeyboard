package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.Nullable;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;

/**
 * Handles visual updates for voice input state: voice-key state and a non-invasive spacebar badge.
 */
public final class VoiceStatusRenderer {

  private VoiceInputState voiceState = VoiceInputState.IDLE;

  public void updateVoiceKeyState(boolean isRecording, @Nullable InputViewBinder inputView) {
    if (inputView == null) return;
    inputView.setVoice(isRecording, false);
  }

  public void updateSpaceBarRecordingStatus(boolean isRecording, @Nullable InputViewBinder view) {
    if (view == null) return;
    // Prefer to show explicit recording state when we know it, but don't overwrite non-recording
    // states (e.g., waiting/error) when recording stops.
    final VoiceInputState stateToShow = isRecording ? VoiceInputState.RECORDING : voiceState;
    view.setVoiceInputState(stateToShow);
  }

  public void updateVoiceInputStatus(@Nullable InputViewBinder view, VoiceInputState newState) {
    if (voiceState == newState) return;
    voiceState = newState;
    if (view == null) return;
    view.setVoiceInputState(voiceState);
  }

  public VoiceInputState getCurrentState() {
    return voiceState;
  }
}
