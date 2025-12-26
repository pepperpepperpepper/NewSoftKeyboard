package wtf.uhoh.newsoftkeyboard.app.ime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceExtendingTest extends ImeServiceBaseTest {

  @Test
  public void testImeServiceClassHierarchy() throws Exception {
    final Set<Class<?>> allPossibleClasses =
        new HashSet<>(
            Arrays.asList(
                wtf.uhoh.newsoftkeyboard.app.ime.ImeBase.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeClipboard.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeKeyboardTagsSearcher.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeMediaInsertion.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeNightMode.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImePowerSaving.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImePressEffects.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeColorizeNavBar.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeWithGestureTyping.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeSwipeListener.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeWithQuickText.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeSuggestionsController.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeInlineSuggestions.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeThemeOverlay.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeHardware.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeIncognito.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeDialogProvider.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImePopText.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeRxPrefs.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeKeyboardSwitchedListener.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTokenService.class,
                wtf.uhoh.newsoftkeyboard.app.notices.PublicNotices.class,
                wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase.class));

    Class<?> superclass = NewSoftKeyboardService.class.getSuperclass();
    Assert.assertNotNull(superclass);
    while (!superclass.equals(ImeBase.class)) {
      Assert.assertTrue(
          "Class " + superclass + " is not in the allPossibleClasses set! Was it removed?",
          allPossibleClasses.remove(superclass));
      superclass = superclass.getSuperclass();
      Assert.assertNotNull(superclass);
    }

    final String errorMessage =
        "Still have classes in set: "
            + String.join(
                ", ",
                allPossibleClasses.stream().map(Object::toString).collect(Collectors.toList()));

    Assert.assertEquals(errorMessage, 1, allPossibleClasses.size());
    Assert.assertTrue(allPossibleClasses.contains(ImeBase.class));
  }
}
