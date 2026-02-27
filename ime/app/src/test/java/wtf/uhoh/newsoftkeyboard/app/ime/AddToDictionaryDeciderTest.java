package wtf.uhoh.newsoftkeyboard.app.ime;

import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class AddToDictionaryDeciderTest {

  @Test
  public void testDoesNotShowAddHintInIncognito() {
    final Suggest suggest = Mockito.mock(Suggest.class);
    Mockito.when(suggest.isIncognitoMode()).thenReturn(true);
    Mockito.when(suggest.isValidWord(Mockito.any())).thenReturn(false);

    final boolean shouldShow =
        AddToDictionaryDecider.shouldShowAddHint(
            0,
            /* justAutoAddedWord= */ false,
            /* showSuggestions= */ true,
            suggest,
            "wut",
            Locale.US);
    Assert.assertFalse(shouldShow);
  }
}
