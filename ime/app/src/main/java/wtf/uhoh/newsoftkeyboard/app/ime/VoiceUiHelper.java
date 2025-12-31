package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.Nullable;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;

/** Small helper to centralize voice key/status UI updates. */
public final class VoiceUiHelper {

  private final VoiceStatusRenderer voiceStatusRenderer;
  private final VoiceImeController voiceImeController;

  public VoiceUiHelper(
      VoiceStatusRenderer voiceStatusRenderer, VoiceImeController voiceImeController) {
    this.voiceStatusRenderer = voiceStatusRenderer;
    this.voiceImeController = voiceImeController;
  }

  public void updateVoiceKeyState(
      @Nullable KeyboardDefinition currentKeyboard, @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateVoiceKeyState(voiceImeController.isRecording(), view);
  }

  public void updateSpaceBarRecordingStatus(
      boolean isRecording,
      @Nullable KeyboardDefinition currentKeyboard,
      @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateSpaceBarRecordingStatus(isRecording, view);
  }

  public void updateVoiceInputStatus(
      VoiceInputState newState,
      @Nullable KeyboardDefinition currentKeyboard,
      @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateVoiceInputStatus(view, newState);
  }
}
