package wtf.uhoh.newsoftkeyboard.overlay;

import android.graphics.Color;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskPlainTestRunner;

@RunWith(NskPlainTestRunner.class)
public class OverlayDataTest {

  @Test
  public void isValidIfTextColorIsDifferentThanBackground() {
    Assert.assertTrue(overlay(Color.GRAY, Color.GRAY, Color.BLACK).isValid());
    Assert.assertTrue(overlay(Color.GRAY, Color.BLACK, Color.BLUE).isValid());
  }

  @Test
  public void isNotValidIfTextIsSame() {
    Assert.assertFalse(overlay(Color.GRAY, Color.GRAY, Color.GRAY).isValid());
    Assert.assertFalse(overlay(Color.BLACK, Color.BLUE, Color.BLACK).isValid());
    Assert.assertFalse(overlay(Color.MAGENTA, Color.WHITE, Color.WHITE).isValid());
  }

  private OverlayData overlay(int primaryColor, int darkPrimaryColor, int textColor) {
    return new OverlayDataImpl(primaryColor, darkPrimaryColor, 0, textColor, 0);
  }
}
