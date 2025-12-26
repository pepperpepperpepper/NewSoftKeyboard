package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

final class WordComposerTracker {

  @NonNull private WordComposer currentWord = new WordComposer();
  @NonNull private WordComposer previousWord = new WordComposer();

  @NonNull
  WordComposer currentWord() {
    return currentWord;
  }

  @NonNull
  WordComposer previousWord() {
    return previousWord;
  }

  void setPreviousWord(@NonNull WordComposer word) {
    previousWord = word;
  }

  void setWords(@NonNull WordComposer currentWord, @NonNull WordComposer previousWord) {
    this.currentWord = currentWord;
    this.previousWord = previousWord;
  }

  @NonNull
  WordComposer prepareWordComposerForNextWord() {
    if (currentWord.isEmpty()) return currentWord;

    final WordComposer typedWord = currentWord;
    currentWord = previousWord;
    previousWord = typedWord;
    currentWord.reset(); // re-using
    return typedWord;
  }

  void resetCurrentWord() {
    currentWord.reset();
  }
}
