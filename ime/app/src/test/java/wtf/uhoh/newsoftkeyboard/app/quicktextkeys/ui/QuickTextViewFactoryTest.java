package wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.mockito.Mockito.mock;
import static wtf.uhoh.newsoftkeyboard.app.NskApplicationBase.prefs;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickKeyHistoryRecords;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class QuickTextViewFactoryTest {
  @Test
  public void testCreateQuickTextView() throws Exception {
    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
    QuickTextPagerView view =
        QuickTextViewFactory.createQuickTextView(
            getApplicationContext(),
            linearLayout,
            new QuickKeyHistoryRecords(prefs(getApplicationContext())),
            mock(DefaultSkinTonePrefTracker.class),
            mock(DefaultGenderPrefTracker.class));

    Assert.assertNotNull(view);

    Assert.assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, view.getLayoutParams().width);
  }
}
