package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import java.util.HashMap;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;

/**
 * Handles visual updates for voice input state: space-bar labels, flashing error indicator, and
 * voice-key state on the current keyboard.
 */
public final class VoiceStatusRenderer {

  private VoiceInputState voiceState = VoiceInputState.IDLE;
  private boolean errorFlashState;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Runnable errorFlashRunnable;
  private final Map<String, CharSequence> originalSpaceKeyLabelsByKeyboardId = new HashMap<>();

  public void updateVoiceKeyState(
      @Nullable KeyboardDefinition keyboard, boolean isRecording, @Nullable View inputView) {
    if (keyboard == null) return;
    boolean stateChanged = keyboard.setVoice(isRecording, false);
    if (stateChanged && inputView != null) {
      inputView.invalidate();
    }
  }

  public void updateSpaceBarRecordingStatus(
      @Nullable KeyboardDefinition keyboard, boolean isRecording, @Nullable View inputView) {
    if (keyboard == null) return;
    // Prefer to show explicit recording state when we know it, but don't overwrite non-recording
    // states (e.g., waiting/error) when recording stops.
    applySpaceLabel(
        keyboard,
        inputView,
        isRecording
            ? getStatusTextForState(VoiceInputState.RECORDING)
            : getStatusTextForState(voiceState));
  }

  public void updateVoiceInputStatus(
      @Nullable KeyboardDefinition keyboard, @Nullable View inputView, VoiceInputState newState) {
    if (voiceState == newState) return;
    voiceState = newState;

    if (voiceState == VoiceInputState.ERROR) {
      startErrorFlashing(keyboard, inputView);
    } else {
      stopErrorFlashing();
    }

    if (keyboard == null) return;
    applySpaceLabel(keyboard, inputView, getStatusTextForState(voiceState));
  }

  public VoiceInputState getCurrentState() {
    return voiceState;
  }

  private void startErrorFlashing(@Nullable KeyboardDefinition keyboard, @Nullable View inputView) {
    stopErrorFlashing();
    errorFlashRunnable =
        new Runnable() {
          @Override
          public void run() {
            if (voiceState == VoiceInputState.ERROR) {
              errorFlashState = !errorFlashState;
              applySpaceLabel(keyboard, inputView);
              handler.postDelayed(this, 500);
            }
          }
        };
    handler.post(errorFlashRunnable);
  }

  private void stopErrorFlashing() {
    if (errorFlashRunnable != null) {
      handler.removeCallbacks(errorFlashRunnable);
      errorFlashRunnable = null;
    }
    errorFlashState = false;
  }

  private void applySpaceLabel(@Nullable KeyboardDefinition keyboard, @Nullable View inputView) {
    applySpaceLabel(keyboard, inputView, getStatusTextForState(voiceState));
  }

  private void applySpaceLabel(
      @Nullable KeyboardDefinition keyboard,
      @Nullable View inputView,
      @Nullable CharSequence label) {
    if (keyboard == null) return;
    final String keyboardId = keyboard.getKeyboardId();
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() != KeyCodes.SPACE) continue;

      if (label == null) {
        // Restore the original space-label if we previously overrode it (for compatibility with
        // keyboards that set their own space label).
        if (originalSpaceKeyLabelsByKeyboardId.containsKey(keyboardId)) {
          key.label = originalSpaceKeyLabelsByKeyboardId.remove(keyboardId);
        }
      } else {
        originalSpaceKeyLabelsByKeyboardId.putIfAbsent(keyboardId, key.label);
        key.label = label;
      }

      if (inputView != null) inputView.invalidate();
      break;
    }
  }

  private CharSequence getStatusTextForState(VoiceInputState state) {
    switch (state) {
      case RECORDING:
        return "Recording";
      case WAITING:
        return "Waiting";
      case ERROR:
        // Flash by alternating between showing the status and restoring the original space-label.
        return errorFlashState ? "Error" : null;
      case IDLE:
      default:
        return null;
    }
  }
}
