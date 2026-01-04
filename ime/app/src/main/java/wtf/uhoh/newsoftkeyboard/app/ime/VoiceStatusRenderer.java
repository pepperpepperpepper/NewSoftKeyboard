package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.Nullable;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;

/**
 * Handles visual updates for voice input state: voice-key state. Status messages are rendered above
 * the keyboard (strip actions), not on the spacebar.
 */
public final class VoiceStatusRenderer {

  private VoiceInputState voiceState = VoiceInputState.IDLE;

  public void updateVoiceKeyState(boolean isRecording, @Nullable InputViewBinder inputView) {
    if (inputView == null) return;
    inputView.setVoice(isRecording, false);
  }

  public void updateSpaceBarRecordingStatus(boolean isRecording, @Nullable InputViewBinder view) {
    if (isRecording) {
      voiceState = VoiceInputState.RECORDING;
    }
  }

  public void updateVoiceInputStatus(@Nullable InputViewBinder view, VoiceInputState newState) {
    if (voiceState == newState) return;
    voiceState = newState;
  }

  public VoiceInputState getCurrentState() {
    return voiceState;
  }
}
