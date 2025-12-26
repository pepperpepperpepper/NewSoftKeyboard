package wtf.uhoh.newsoftkeyboard.nextword;

import androidx.annotation.NonNull;

public interface NextWordSuggestions {
  @NonNull
  Iterable<String> getNextWords(@NonNull String currentWord, int maxResults, int minWordUsage);

  void notifyNextTypedWord(@NonNull String currentWord);

  void resetSentence();
}
