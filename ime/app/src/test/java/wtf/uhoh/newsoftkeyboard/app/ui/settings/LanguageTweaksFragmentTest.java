package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import androidx.preference.ListPreference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardFactory;
import wtf.uhoh.newsoftkeyboard.app.testing.AddOnTestUtils;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class LanguageTweaksFragmentTest
    extends RobolectricFragmentTestCase<LanguageTweaksFragment> {
  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.languageTweaksFragment;
  }

  @Test
  public void testShowEnabledKeyboardsPlusNoneEntries() {
    final KeyboardFactory keyboardFactory =
        NskApplicationBase.getKeyboardFactory(getApplicationContext());

    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);

    LanguageTweaksFragment fragment = startFragment();
    ListPreference listPreference =
        (ListPreference)
            fragment.findPreference(
                fragment.getString(R.string.settings_key_layout_for_internet_fields));
    Assert.assertNotNull(listPreference);

    Assert.assertEquals(2, keyboardFactory.getEnabledIds().size());
    Assert.assertEquals(3, listPreference.getEntries().length);
    Assert.assertEquals(3, listPreference.getEntryValues().length);
    Assert.assertEquals(keyboardFactory.getEnabledAddOn().getId(), listPreference.getValue());

    Assert.assertEquals("None", listPreference.getEntries()[0]);
    Assert.assertEquals("none", listPreference.getEntryValues()[0]);

    for (int enabledKeyboardIndex = 0;
        enabledKeyboardIndex < keyboardFactory.getEnabledAddOns().size();
        enabledKeyboardIndex++) {
      final KeyboardAddOnAndBuilder builder =
          keyboardFactory.getEnabledAddOns().get(enabledKeyboardIndex);
      Assert.assertTrue(
          listPreference
              .getEntries()[enabledKeyboardIndex + 1]
              .toString()
              .contains(builder.getName()));
      Assert.assertTrue(
          listPreference
              .getEntries()[enabledKeyboardIndex + 1]
              .toString()
              .contains(builder.getDescription()));
      Assert.assertEquals(
          listPreference.getEntryValues()[enabledKeyboardIndex + 1], builder.getId());
    }
  }
}
