package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import wtf.uhoh.newsoftkeyboard.R;

/** Holds the current voice input state as a small badge to render on the keyboard. */
final class VoiceStatusBadgeState {

  @NonNull private VoiceInputState state = VoiceInputState.IDLE;
  @Nullable private CharSequence badgeText;

  boolean setState(@NonNull VoiceInputState state, @NonNull Context context) {
    if (this.state == state) return false;
    this.state = state;
    badgeText =
        switch (state) {
          case RECORDING -> context.getText(R.string.voice_spacebar_badge_recording);
          case WAITING -> context.getText(R.string.voice_spacebar_badge_waiting);
          case ERROR -> context.getText(R.string.voice_spacebar_badge_error);
          case IDLE -> null;
        };
    return true;
  }

  @NonNull
  VoiceInputState state() {
    return state;
  }

  @Nullable
  CharSequence badgeText() {
    return badgeText;
  }
}
