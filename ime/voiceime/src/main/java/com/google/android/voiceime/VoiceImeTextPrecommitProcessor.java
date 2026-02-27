package com.google.android.voiceime;

import androidx.annotation.NonNull;

/**
 * Optional hook for an IME service to post-process voice transcription text right before it is
 * committed to the target app.
 *
 * <p>Implementations must be fast, must not log or persist the provided text, and should return
 * {@code formattedText} unchanged when post-processing is disabled or not applicable.
 */
public interface VoiceImeTextPrecommitProcessor {
  @NonNull
  String onVoiceTextPreCommit(@NonNull String formattedText);
}
