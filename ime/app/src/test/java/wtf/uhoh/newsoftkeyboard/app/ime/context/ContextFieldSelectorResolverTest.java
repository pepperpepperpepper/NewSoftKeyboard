package wtf.uhoh.newsoftkeyboard.app.ime.context;

import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ContextFieldSelectorResolverTest {

  @Test
  public void testResolvesTextSelectors() {
    final EditorInfo email = new EditorInfo();
    email.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    Assert.assertEquals(ContextFieldSelector.EMAIL, ContextFieldSelectorResolver.resolve(email));

    final EditorInfo url = new EditorInfo();
    url.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_URI;
    Assert.assertEquals(ContextFieldSelector.URL, ContextFieldSelectorResolver.resolve(url));

    final EditorInfo im = new EditorInfo();
    im.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
    Assert.assertEquals(ContextFieldSelector.IM, ContextFieldSelectorResolver.resolve(im));

    final EditorInfo search = new EditorInfo();
    search.inputType = EditorInfo.TYPE_CLASS_TEXT;
    search.imeOptions = EditorInfo.IME_ACTION_SEARCH;
    Assert.assertEquals(ContextFieldSelector.SEARCH, ContextFieldSelectorResolver.resolve(search));

    final EditorInfo plainText = new EditorInfo();
    plainText.inputType = EditorInfo.TYPE_CLASS_TEXT;
    Assert.assertEquals(ContextFieldSelector.TEXT, ContextFieldSelectorResolver.resolve(plainText));
  }

  @Test
  public void testReturnsNullForNonTextFields() {
    final EditorInfo number = new EditorInfo();
    number.inputType = EditorInfo.TYPE_CLASS_NUMBER;
    Assert.assertNull(ContextFieldSelectorResolver.resolve(number));
  }
}
