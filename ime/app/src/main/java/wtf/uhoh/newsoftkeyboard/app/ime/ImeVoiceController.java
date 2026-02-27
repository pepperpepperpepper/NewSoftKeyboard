package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;

final class ImeVoiceController {

  @NonNull private final ImeServiceBase service;

  @Nullable private VoiceRecognitionTrigger voiceRecognitionTrigger;
  @Nullable private VoiceImeController voiceImeController;
  @NonNull private final VoiceStatusRenderer voiceStatusRenderer = new VoiceStatusRenderer();
  @Nullable private VoiceUiHelper voiceUiHelper;
  private boolean keepScreenOnForVoiceInput;

  ImeVoiceController(@NonNull ImeServiceBase service) {
    this.service = service;
  }

  void onVoiceInitialized(
      @NonNull VoiceRecognitionTrigger trigger, @NonNull VoiceImeController controller) {
    voiceRecognitionTrigger = trigger;
    voiceImeController = controller;
    voiceUiHelper = new VoiceUiHelper(voiceStatusRenderer, controller);
    controller.attachCallbacks();
  }

  void onDestroy() {
    updateKeepScreenOnForVoiceInput(VoiceInputState.IDLE);
  }

  void updateVoiceKeyState() {
    final VoiceUiHelper helper = voiceUiHelper;
    if (helper == null) return;
    final KeyboardDefinition alphabetKeyboard = service.getCurrentAlphabetKeyboard();
    final InputViewBinder inputView = service.getInputView();
    helper.updateVoiceKeyState(alphabetKeyboard, inputView);
  }

  void updateSpaceBarRecordingStatus(boolean isRecording) {
    final VoiceUiHelper helper = voiceUiHelper;
    if (helper == null) return;
    final KeyboardDefinition alphabetKeyboard = service.getCurrentAlphabetKeyboard();
    final InputViewBinder inputView = service.getInputView();
    helper.updateSpaceBarRecordingStatus(isRecording, alphabetKeyboard, inputView);

    final VoiceImeController controller = voiceImeController;
    updateKeepScreenOnForVoiceInput(
        controller != null ? controller.getCurrentState() : VoiceInputState.IDLE);
  }

  void updateVoiceInputStatus(@NonNull VoiceInputState newState) {
    final VoiceUiHelper helper = voiceUiHelper;
    if (helper == null) return;
    final KeyboardDefinition alphabetKeyboard = service.getCurrentAlphabetKeyboard();
    final InputViewBinder inputView = service.getInputView();
    helper.updateVoiceInputStatus(newState, alphabetKeyboard, inputView);
    updateKeepScreenOnForVoiceInput(newState);
  }

  private void updateKeepScreenOnForVoiceInput(@NonNull VoiceInputState voiceInputState) {
    final boolean keepScreenOn =
        voiceInputState == VoiceInputState.RECORDING || voiceInputState == VoiceInputState.WAITING;
    if (keepScreenOn == keepScreenOnForVoiceInput) {
      return;
    }
    keepScreenOnForVoiceInput = keepScreenOn;
    final Window window = service.getWindow().getWindow();
    if (window == null) {
      return;
    }
    if (keepScreenOn) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }
}
