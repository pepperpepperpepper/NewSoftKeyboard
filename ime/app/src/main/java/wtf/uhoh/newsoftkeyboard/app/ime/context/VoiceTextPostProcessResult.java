package wtf.uhoh.newsoftkeyboard.app.ime.context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Result of post-processing voice transcription text for context profiles. */
public record VoiceTextPostProcessResult(
    @NonNull String textToCommit, @Nullable VoiceSuggestion suggestion) {

  /** Suggestion-first replacement for a just-committed voice transcription. */
  public record VoiceSuggestion(
      @NonNull String suggestionText,
      @NonNull String committedText,
      @NonNull String replacementText) {}
}
