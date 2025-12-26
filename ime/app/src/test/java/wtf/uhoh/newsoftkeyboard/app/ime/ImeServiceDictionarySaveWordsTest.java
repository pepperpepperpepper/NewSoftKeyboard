package wtf.uhoh.newsoftkeyboard.app.ime;

import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceDictionarySaveWordsTest extends ImeServiceBaseTest {

  @Test
  public void testAsksToAddToDictionaryWhenTouchingTypedUnknownWordAndAdds() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.pickSuggestionManually(0, "hel");
    // at this point, the candidates view will show a hint
    Mockito.verify(getMockCandidateView()).showAddToDictionaryHint("hel");
    Assert.assertEquals("hel ", inputConnection.getCurrentTextInInputConnection());
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .addWordToUserDictionary(Mockito.anyString());
    Mockito.verify(getMockCandidateView(), Mockito.never())
        .notifyAboutWordAdded(Mockito.anyString());
    Assert.assertTrue(mImeServiceUnderTest.isAddToDictionaryHintShown());
    mImeServiceUnderTest.addWordToDictionary("hel");
    TestRxSchedulers.drainAllTasks();
    Mockito.verify(mImeServiceUnderTest.getSuggest()).addWordToUserDictionary("hel");
    Mockito.verify(getMockCandidateView()).notifyAboutWordAdded("hel");
    Assert.assertFalse(mImeServiceUnderTest.isAddToDictionaryHintShown());

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .removeWordFromUserDictionary(Mockito.anyString());
    mImeServiceUnderTest.removeFromUserDictionary("hel");
    TestRxSchedulers.drainAllTasks();
    Mockito.verify(mImeServiceUnderTest.getSuggest()).removeWordFromUserDictionary("hel");
  }

  @Test
  public void testAddToDictionaryHintDismissedWhenBackspace() {
    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.pickSuggestionManually(0, "hel");
    // at this point, the candidates view will show a hint
    Mockito.verify(getMockCandidateView()).showAddToDictionaryHint("hel");
    Assert.assertTrue(mImeServiceUnderTest.isAddToDictionaryHintShown());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertFalse(mImeServiceUnderTest.isAddToDictionaryHintShown());
  }

  @Test
  public void testAutoAddUnknownWordIfPickedFrequently() {
    final String typedWord = "blah";
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    StringBuilder expectedOutput = new StringBuilder();
    // it takes 3 picks to learn a new word
    for (int pickIndex = 0; pickIndex < 3; pickIndex++) {
      mImeServiceUnderTest.simulateTextTyping(typedWord);
      mImeServiceUnderTest.pickSuggestionManually(0, typedWord);
      TestRxSchedulers.drainAllTasks(); // allowing to write to database.
      expectedOutput.append(typedWord).append(" ");
      if (pickIndex != 2) {
        Mockito.verify(getMockCandidateView(), Mockito.times(1 + pickIndex))
            .showAddToDictionaryHint(typedWord);
        Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
            .addWordToUserDictionary(Mockito.anyString());
        Mockito.verify(getMockCandidateView(), Mockito.never())
            .notifyAboutWordAdded(Mockito.anyString());
      } else {
        // third time will auto-add
        Mockito.verify(getMockCandidateView(), Mockito.times(pickIndex + 1 /*3 times*/))
            .showAddToDictionaryHint(typedWord);
        Mockito.verify(mImeServiceUnderTest.getSuggest()).addWordToUserDictionary(typedWord);
        Mockito.verify(getMockCandidateView()).notifyAboutWordAdded(typedWord);
      }
      Assert.assertEquals(
          expectedOutput.toString(), inputConnection.getCurrentTextInInputConnection());
    }
  }

  @Test
  public void testAutoAddUnknownWordIfAutoPickedAfterUndoCommit() {
    // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/580
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    StringBuilder expectedOutput = new StringBuilder();
    // it takes 5 tries to lean from typing
    for (int pickIndex = 0; pickIndex < 5; pickIndex++) {
      mImeServiceUnderTest.simulateTextTyping("hel");
      mImeServiceUnderTest.simulateKeyPress(' ');
      Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
          .addWordToUserDictionary(Mockito.anyString());
      Mockito.verify(getMockCandidateView(), Mockito.never())
          .notifyAboutWordAdded(Mockito.anyString());
      expectedOutput.append("he'll ");
      Assert.assertEquals(
          expectedOutput.toString(), inputConnection.getCurrentTextInInputConnection());
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
      expectedOutput.setLength(expectedOutput.length() - 6); // undo commit
      expectedOutput.append("hel"); // undo commit
      Assert.assertEquals(
          expectedOutput.toString(), inputConnection.getCurrentTextInInputConnection());
      mImeServiceUnderTest.simulateKeyPress(' ');
      TestRxSchedulers.drainAllTasks();
      expectedOutput.append(" ");
      Assert.assertEquals(
          expectedOutput.toString(), inputConnection.getCurrentTextInInputConnection());

      if (pickIndex != 4) {
        Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
            .addWordToUserDictionary(Mockito.anyString());
        Mockito.verify(getMockCandidateView(), Mockito.never())
            .notifyAboutWordAdded(Mockito.anyString());
      } else {
        Mockito.verify(mImeServiceUnderTest.getSuggest()).addWordToUserDictionary("hel");
        Mockito.verify(getMockCandidateView()).notifyAboutWordAdded("hel");
      }
    }
  }
}
