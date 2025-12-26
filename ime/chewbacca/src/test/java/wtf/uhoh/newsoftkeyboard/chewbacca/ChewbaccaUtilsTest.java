package wtf.uhoh.newsoftkeyboard.chewbacca;

import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ChewbaccaUtilsTest {

  @Test
  public void testGetSysInfo() {
    String info = ChewbaccaUtils.getSysInfo(ApplicationProvider.getApplicationContext());
    Assert.assertTrue(info.contains("BRAND:" + Build.BRAND));
    Assert.assertTrue(info.contains("VERSION.SDK_INT:" + Build.VERSION.SDK_INT));
    Assert.assertTrue(
        info.contains(
            "Locale:"
                + ApplicationProvider.getApplicationContext()
                    .getResources()
                    .getConfiguration()
                    .locale));
  }
}
