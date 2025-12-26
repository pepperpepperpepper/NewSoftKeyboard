package wtf.uhoh.newsoftkeyboard.prefs.backup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskPlainTestRunner;

@RunWith(NskPlainTestRunner.class)
public class PrefsRootTest {

  @Test
  public void testProperties() {
    PrefsRoot root = new PrefsRoot(3);
    Assert.assertEquals(3, root.getVersion());
  }
}
