package wtf.uhoh.newsoftkeyboard.nextword;

import androidx.collection.ArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NextWordsContainer {

  private static final NextWord.NextWordComparator msNextWordComparator =
      new NextWord.NextWordComparator();

  public final String word;
  private final List<NextWord> mOrderedNextWord = new ArrayList<>();
  private final Map<String, NextWord> mNextWordLookup = new ArrayMap<>();

  public NextWordsContainer(String word) {
    this.word = word;
  }

  public NextWordsContainer(String word, List<String> nextWords) {
    this.word = word;
    int frequency = nextWords.size();
    for (String nextWordText : nextWords) {
      NextWord nextWord = new NextWord(nextWordText, frequency);
      mNextWordLookup.put(nextWordText, nextWord);
      mOrderedNextWord.add(nextWord);
    }
  }

  public void markWordAsUsed(String word) {
    NextWord nextWord = mNextWordLookup.get(word);
    if (nextWord == null) {
      nextWord = new NextWord(word);
      mNextWordLookup.put(word, nextWord);
      mOrderedNextWord.add(nextWord);
    } else {
      nextWord.markAsUsed();
    }
  }

  void setNextWordUsedCount(String word, int usedCount) {
    if (word == null || word.isEmpty()) return;
    final int normalizedUsedCount = Math.max(1, usedCount);
    NextWord nextWord = mNextWordLookup.get(word);
    if (nextWord == null) {
      nextWord = new NextWord(word, normalizedUsedCount);
      mNextWordLookup.put(word, nextWord);
      mOrderedNextWord.add(nextWord);
    } else if (nextWord.getUsedCount() < normalizedUsedCount) {
      // NextWord used-count is immutable; swapping the instance is cheaper than incrementing.
      final NextWord replacement = new NextWord(word, normalizedUsedCount);
      mNextWordLookup.put(word, replacement);
      final int orderedIndex = mOrderedNextWord.indexOf(nextWord);
      if (orderedIndex >= 0) mOrderedNextWord.set(orderedIndex, replacement);
    }
  }

  public List<NextWord> getNextWordSuggestions() {
    Collections.sort(mOrderedNextWord, msNextWordComparator);

    return mOrderedNextWord;
  }

  @Override
  public String toString() {
    return "(" + word + ") -> [" + mOrderedNextWord.toString() + "]";
  }
}
