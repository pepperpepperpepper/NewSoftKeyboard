package wtf.uhoh.newsoftkeyboard.app.ime;

import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceSelectionModificationTest extends ImeServiceBaseTest {

  @Test
  public void testCapitalizeEntireInput() {
    final String initialText = "this should all he caps";
    final String upperCaseText = "THIS SHOULD ALL HE CAPS";
    final String capitalizedText = "This should all he caps";
    mImeServiceUnderTest.simulateTextTyping(initialText);
    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.setSelectedText(0, initialText.length(), true);
    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentSelectedText());
    // to capitalized
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals(capitalizedText, mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(capitalizedText, mImeServiceUnderTest.getCurrentInputConnectionText());

    // To uppercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals(upperCaseText, mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(upperCaseText, mImeServiceUnderTest.getCurrentInputConnectionText());

    // Back to lowercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentSelectedText());
  }

  @Test
  public void testNoChangeIfNotSelected() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    final String expectedText = "this is not selected";
    inputConnection.commitText(expectedText, 1);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals(expectedText, inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testCapitalizeSingleWord() {
    final String inputText = "this should not all he caps";
    final String capitalized = "this Should not all he caps";
    final String uppercase = "this SHOULD not all he caps";
    mImeServiceUnderTest.simulateTextTyping(inputText);
    mImeServiceUnderTest.setSelectedText("this ".length(), "this should".length(), true);
    // To capitalized
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("Should", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(capitalized, mImeServiceUnderTest.getCurrentInputConnectionText());
    // To uppercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("SHOULD", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(uppercase, mImeServiceUnderTest.getCurrentInputConnectionText());

    // Back to lowercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("should", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(inputText, mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testStartsCapitalized() {
    final String inputText = "this Should not all he caps";
    final String capitalized = "this Should not all he caps";
    final String lowercase = "this should not all he caps";
    final String uppercase = "this SHOULD not all he caps";
    mImeServiceUnderTest.simulateTextTyping(inputText);
    mImeServiceUnderTest.setSelectedText("this ".length(), "this should".length(), true);
    // To uppercase - instead of capitalized, it switches to uppercase (since it was already
    // capitalized)
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("SHOULD", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(uppercase, mImeServiceUnderTest.getCurrentInputConnectionText());
    // To lowercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("should", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(lowercase, mImeServiceUnderTest.getCurrentInputConnectionText());

    // Back to capitalized
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("Should", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(capitalized, mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testCapitalizeMixedCaseWord() {
    final String inputText = "this sHoUlD not all he caps";
    final String capitalized = "this Should not all he caps";
    final String uppercase = "this SHOULD not all he caps";
    final String lowercase = "this should not all he caps";
    mImeServiceUnderTest.simulateTextTyping(inputText.toLowerCase());
    mImeServiceUnderTest.setSelectedText("this ".length(), "this should".length(), true);
    // To capitalized
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("Should", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(capitalized, mImeServiceUnderTest.getCurrentInputConnectionText());
    // To uppercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("SHOULD", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(uppercase, mImeServiceUnderTest.getCurrentInputConnectionText());

    // Back to lowercase
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("should", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(lowercase, mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testWrapWithSpecials() {
    final String inputText = "not this but this he quoted not this";
    mImeServiceUnderTest.simulateTextTyping(inputText.toLowerCase());
    mImeServiceUnderTest.setSelectedText(
        "not this but ".length(), "not this but this he quoted".length(), true);
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress('\"');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"this he quoted\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'this he quoted'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('-');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-this he quoted-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('_');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_this he quoted_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('*');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*this he quoted*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('`');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`this he quoted`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('~');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~this he quoted~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());

    // special case () [] {}
    mImeServiceUnderTest.simulateKeyPress('(');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(this he quoted)~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(')');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~((this he quoted))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress('[');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(([this he quoted]))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(']');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(([[this he quoted]]))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress('{');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(([[{this he quoted}]]))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress('}');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(([[{{this he quoted}}]]))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress('<');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(([[{{<this he quoted>}}]]))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress('>');
    Assert.assertEquals("this he quoted", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "not this but \"'-_*`~(([[{{<<this he quoted>>}}]]))~`*_-'\" not this",
        mImeServiceUnderTest.getCurrentInputConnectionText());
  }
}
