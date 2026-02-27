package wtf.uhoh.newsoftkeyboard.nextword;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class NextWordsFileParserV2Test {

  @Test
  public void testFlow() throws Exception {
    Map<String, NextWordsContainer> words = new HashMap<>();
    NextWordsContainer hello = new NextWordsContainer("hello");
    hello.setNextWordUsedCount("is", 10);
    words.put("hello", hello);

    NextWordsContainer is = new NextWordsContainer("is");
    is.setNextWordUsedCount("it", 7);
    words.put("is", is);

    NextWordsContainer forWord = new NextWordsContainer("for");
    forWord.setNextWordUsedCount("hello", 2);
    forWord.setNextWordUsedCount("me", 9);
    forWord.setNextWordUsedCount("you", 1);
    words.put("for", forWord);

    OutputStream outputStream =
        getApplicationContext().openFileOutput("next_words_test_v2.txt", Context.MODE_PRIVATE);
    NextWordsFileParserV2 parserV2 = new NextWordsFileParserV2();
    parserV2.storeNextWords(words.values(), outputStream);

    outputStream.flush();
    outputStream.close();

    InputStream inputStream = getApplicationContext().openFileInput("next_words_test_v2.txt");
    // reading VERSION
    Assert.assertEquals(2, inputStream.read());
    // reading the rest of the dictionary
    Iterable<NextWordsContainer> loadedWords = parserV2.loadStoredNextWords(inputStream);
    for (NextWordsContainer loadedWord : loadedWords) {
      Assert.assertTrue(words.containsKey(loadedWord.word));
      NextWordsContainer word = words.get(loadedWord.word);
      Assert.assertEquals(
          word.getNextWordSuggestions().size(), loadedWord.getNextWordSuggestions().size());
      for (int nextWordIndex = 0;
          nextWordIndex < word.getNextWordSuggestions().size();
          nextWordIndex++) {
        NextWord nextWord = word.getNextWordSuggestions().get(nextWordIndex);
        NextWord loadedNextWord = loadedWord.getNextWordSuggestions().get(nextWordIndex);
        Assert.assertEquals(nextWord.nextWord, loadedNextWord.nextWord);
        Assert.assertEquals(nextWord.getUsedCount(), loadedNextWord.getUsedCount());
      }
      words.remove(loadedWord.word);
    }

    Assert.assertEquals(0, words.size());
  }

  @Test
  public void testWritesSortedByUsedCount() throws Exception {
    NextWordsContainer container = new NextWordsContainer("hello");
    container.setNextWordUsedCount("a", 2);
    container.setNextWordUsedCount("b", 5);
    container.setNextWordUsedCount("c", 1);

    Assert.assertEquals(
        Arrays.asList("b", "a", "c"),
        container.getNextWordSuggestions().stream()
            .map(nw -> nw.nextWord)
            .collect(Collectors.toList()));
  }
}
