package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import androidx.annotation.NonNull;
import java.util.Collections;
import wtf.uhoh.newsoftkeyboard.dictionaries.EditableDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.GetWordsCallback;
import wtf.uhoh.newsoftkeyboard.dictionaries.KeyCodesProvider;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordSuggestions;

final class SuggestionsProviderNulls {

  static final EditableDictionary NULL_DICTIONARY =
      new EditableDictionary("NULL") {
        @Override
        public boolean addWord(String word, int frequency) {
          return false;
        }

        @Override
        public void deleteWord(String word) {}

        @Override
        public void getLoadedWords(@NonNull GetWordsCallback callback) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void getSuggestions(KeyCodesProvider composer, WordCallback callback) {}

        @Override
        public boolean isValidWord(CharSequence word) {
          return false;
        }

        @Override
        protected void closeAllResources() {}

        @Override
        protected void loadAllResources() {}
      };

  static final NextWordSuggestions NULL_NEXT_WORD_SUGGESTIONS =
      new NextWordSuggestions() {
        @Override
        @NonNull
        public Iterable<String> getNextWords(
            @NonNull String currentWord, int maxResults, int minWordUsage) {
          return Collections.emptyList();
        }

        @Override
        public void notifyNextTypedWord(@NonNull String currentWord) {}

        @Override
        public void resetSentence() {}
      };

  private SuggestionsProviderNulls() {}
}
