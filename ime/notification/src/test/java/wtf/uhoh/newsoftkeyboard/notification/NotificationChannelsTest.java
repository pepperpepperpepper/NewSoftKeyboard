package wtf.uhoh.newsoftkeyboard.notification;

import android.text.TextUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskPlainTestRunner;

@RunWith(NskPlainTestRunner.class)
public class NotificationChannelsTest {
  @Test
  public void testValuesAreClear() {
    for (NotificationChannels v : NotificationChannels.values()) {
      Assert.assertEquals(v.name(), v.mChannelId, v.name());
      Assert.assertFalse(v.name(), TextUtils.isEmpty(v.mDescription));
    }
  }
}
