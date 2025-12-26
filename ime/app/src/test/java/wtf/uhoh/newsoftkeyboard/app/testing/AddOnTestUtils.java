package wtf.uhoh.newsoftkeyboard.app.testing;

import androidx.test.core.app.ApplicationProvider;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.addons.AddOnsFactory;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

public class AddOnTestUtils {
  public static void ensureAddOnAtIndexEnabled(
      AddOnsFactory<? extends AddOn> factory, int index, boolean enabled) {
    final AddOn addOn = factory.getAllAddOns().get(index);
    factory.setAddOnEnabled(addOn.getId(), enabled);
  }

  public static void ensureKeyboardAtIndexEnabled(int index, boolean enabled) {
    ensureAddOnAtIndexEnabled(
        NskApplicationBase.getKeyboardFactory(ApplicationProvider.getApplicationContext()),
        index,
        enabled);
  }
}
