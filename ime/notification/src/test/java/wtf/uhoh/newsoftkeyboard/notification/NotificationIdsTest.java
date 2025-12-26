package wtf.uhoh.newsoftkeyboard.notification;

import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskPlainTestRunner;

@RunWith(NskPlainTestRunner.class)
public class NotificationIdsTest {

  @Test
  public void testValuesHaveUniqueId() {
    Set<Integer> seenIds = new HashSet<>();
    for (NotificationIds id : NotificationIds.values()) {
      Assert.assertTrue(id.name(), seenIds.add(id.mNotificationId));
    }
  }

  @Test
  public void testValuesUseAllChannels() {
    Set<NotificationChannels> notUsed = new HashSet<>();
    for (NotificationChannels channel : NotificationChannels.values()) {
      notUsed.add(channel);
    }

    for (NotificationIds id : NotificationIds.values()) {
      notUsed.remove(id.mChannel);
    }

    Assert.assertTrue(notUsed.isEmpty());
  }
}
