package wtf.uhoh.newsoftkeyboard.nextword;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import java.util.Iterator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class NextWordDictionaryTest {
  private NextWordDictionary mNextWordDictionaryUnderTest;

  private static void assertHasNextWordsForWord(
      NextWordDictionary nextWordDictionaryUnderTest, String word, String... expectedNextWords)
      throws Exception {
    assertHasNextWordsForWord(true, nextWordDictionaryUnderTest, word, expectedNextWords);
  }

  private static void assertHasNextWordsForWord(
      boolean withNotify,
      NextWordDictionary nextWordDictionaryUnderTest,
      String word,
      String... expectedNextWords)
      throws Exception {
    if (withNotify) nextWordDictionaryUnderTest.notifyNextTypedWord(word);

    Iterator<String> nextWordsIterator =
        nextWordDictionaryUnderTest.getNextWords(word, 8, 0).iterator();
    for (String expectedNextWord : expectedNextWords) {
      Assert.assertTrue(nextWordsIterator.hasNext());
      Assert.assertEquals(expectedNextWord, nextWordsIterator.next());
    }
    Assert.assertFalse(nextWordsIterator.hasNext());
  }

  @Before
  public void setup() {
    mNextWordDictionaryUnderTest = new NextWordDictionary(getApplicationContext(), "en");
  }

  @Test
  public void testLoadEmpty() throws Exception {
    mNextWordDictionaryUnderTest.load();

    Assert.assertEquals(0, mNextWordDictionaryUnderTest.dumpDictionaryStatistics().firstWordCount);
    Assert.assertEquals(0, mNextWordDictionaryUnderTest.dumpDictionaryStatistics().secondWordCount);

    mNextWordDictionaryUnderTest.close();
  }

  @Test
  public void testLyrics() throws Exception {
    mNextWordDictionaryUnderTest.load();

    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "is");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "it");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "me");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "you");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "looking");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "for");

    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello", "is");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "is", "it");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "it", "me");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "me", "you");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "you", "looking");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "looking", "for");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "for", "hello");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "bye");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "for", "hello", "bye");

    Assert.assertEquals(8, mNextWordDictionaryUnderTest.dumpDictionaryStatistics().firstWordCount);
    Assert.assertEquals(9, mNextWordDictionaryUnderTest.dumpDictionaryStatistics().secondWordCount);

    mNextWordDictionaryUnderTest.close();
  }

  @Test
  public void testResetSentence() throws Exception {
    mNextWordDictionaryUnderTest.load();

    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny");
    mNextWordDictionaryUnderTest.resetSentence();
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello", "menny");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny");

    mNextWordDictionaryUnderTest.close();
  }

  @Test
  public void testLoadAgain() throws Exception {
    mNextWordDictionaryUnderTest.load();

    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello", "menny");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny", "hello");

    Assert.assertEquals(2, mNextWordDictionaryUnderTest.dumpDictionaryStatistics().firstWordCount);
    Assert.assertEquals(2, mNextWordDictionaryUnderTest.dumpDictionaryStatistics().secondWordCount);

    mNextWordDictionaryUnderTest.close();
    mNextWordDictionaryUnderTest = null;

    NextWordDictionary loadedDictionary = new NextWordDictionary(getApplicationContext(), "en");
    loadedDictionary.load();

    assertHasNextWordsForWord(loadedDictionary, "hello", "menny");
    assertHasNextWordsForWord(loadedDictionary, "menny", "hello");

    Assert.assertEquals(2, loadedDictionary.dumpDictionaryStatistics().firstWordCount);
    Assert.assertEquals(2, loadedDictionary.dumpDictionaryStatistics().secondWordCount);

    loadedDictionary.close();
  }

  @Test
  public void testClearData() throws Exception {
    mNextWordDictionaryUnderTest.load();

    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello", "menny");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny", "hello");
    mNextWordDictionaryUnderTest.clearData();
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(mNextWordDictionaryUnderTest, "menny");

    mNextWordDictionaryUnderTest.close();
  }

  @Test
  public void testDoesNotLearnIfNotNotifying() throws Exception {
    mNextWordDictionaryUnderTest.load();

    assertHasNextWordsForWord(false, mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(false, mNextWordDictionaryUnderTest, "menny");
    assertHasNextWordsForWord(false, mNextWordDictionaryUnderTest, "hello");
    assertHasNextWordsForWord(false, mNextWordDictionaryUnderTest, "menny");
  }

  @Test
  public void testRanksByDescendingUsedCount() throws Exception {
    final String locale = "test_desc_used_count";
    getApplicationContext().deleteFile("next_words_" + locale + ".txt");
    NextWordDictionary dictionary = new NextWordDictionary(getApplicationContext(), locale);
    dictionary.load();

    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("a");
    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("b");
    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("b");
    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("b");

    Iterator<String> nextWordsIterator = dictionary.getNextWords("hello", 8, 0).iterator();
    Assert.assertTrue(nextWordsIterator.hasNext());
    Assert.assertEquals("b", nextWordsIterator.next());
    Assert.assertTrue(nextWordsIterator.hasNext());
    Assert.assertEquals("a", nextWordsIterator.next());
    Assert.assertFalse(nextWordsIterator.hasNext());

    dictionary.close();
  }

  @Test
  public void testUsedCountPersistsAndAffectsMinWordUsage() throws Exception {
    final String locale = "test_min_usage_persist";
    getApplicationContext().deleteFile("next_words_" + locale + ".txt");
    NextWordDictionary dictionary = new NextWordDictionary(getApplicationContext(), locale);
    dictionary.load();

    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("a");
    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("b");
    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("b");
    dictionary.notifyNextTypedWord("hello");
    dictionary.notifyNextTypedWord("b");

    Iterator<String> nextWordsIterator = dictionary.getNextWords("hello", 8, 2).iterator();
    Assert.assertTrue(nextWordsIterator.hasNext());
    Assert.assertEquals("b", nextWordsIterator.next());
    Assert.assertFalse(nextWordsIterator.hasNext());

    dictionary.close();

    NextWordDictionary loadedDictionary = new NextWordDictionary(getApplicationContext(), locale);
    loadedDictionary.load();

    Iterator<String> loadedNextWordsIterator =
        loadedDictionary.getNextWords("hello", 8, 2).iterator();
    Assert.assertTrue(loadedNextWordsIterator.hasNext());
    Assert.assertEquals("b", loadedNextWordsIterator.next());
    Assert.assertFalse(loadedNextWordsIterator.hasNext());

    loadedDictionary.close();
  }
}
