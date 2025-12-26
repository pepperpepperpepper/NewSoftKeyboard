package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.robolectric.annotation.Config.OLDEST_SDK;

import android.os.Build;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.TestUtils;

@RunWith(NskRobolectricTestRunner.class)
public abstract class ImeServiceStartUpAllSdkTest extends ImeServiceBaseTest {

  @Override
  public void setUpForImeServiceBase() throws Exception {
    // get around java.lang.IllegalStateException: The Window Context should have been attached to a
    // DisplayArea
    Assume.assumeTrue("Need to figure how to start it in 32", Build.VERSION.SDK_INT != 32);
    Assume.assumeTrue("Need to figure how to start it in 33", Build.VERSION.SDK_INT != 33);
    Assume.assumeTrue("Need to figure how to start it in 34", Build.VERSION.SDK_INT != 34);
    Assume.assumeTrue("Need to figure how to start it in 34", Build.VERSION.SDK_INT != 35);
    super.setUpForImeServiceBase();
  }

  void testBasicWorks_impl() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("h");
    mImeServiceUnderTest.simulateTextTyping("e");
    mImeServiceUnderTest.simulateTextTyping("l");
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    simulateFinishInputFlow();

    simulateOnStartInputFlow();
  }

  public static class ImeServiceStartUpAllSdkShard1Test extends ImeServiceStartUpAllSdkTest {
    @Test
    @Config(minSdk = OLDEST_SDK, maxSdk = 23)
    public void testBasicWorks() {
      testBasicWorks_impl();
    }
  }

  public static class ImeServiceStartUpAllSdkShard2Test extends ImeServiceStartUpAllSdkTest {
    @Test
    @Config(minSdk = 24, maxSdk = 28)
    public void testBasicWorks() {
      testBasicWorks_impl();
    }
  }

  public static class ImeServiceStartUpAllSdkShard3Test extends ImeServiceStartUpAllSdkTest {
    @Test
    @Config(minSdk = 29, maxSdk = TestUtils.LATEST_STABLE_API_LEVEL)
    public void testBasicWorks() {
      testBasicWorks_impl();
    }
  }
}
