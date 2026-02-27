package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.view.View;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class AppearanceOwnerRegistryCodeDuplicationTest {

  @Test
  public void testOverlayStatusPreferencesAreNotEditableControls() {
    final Context context = ApplicationProvider.getApplicationContext();

    final KeyboardThemeCustomizationOverlaysSection.Host host = () -> new View(context);
    final KeyboardThemeCustomizationOverlaysSection section =
        new KeyboardThemeCustomizationOverlaysSection(host);

    final PreferenceManager preferenceManager = new PreferenceManager(context);
    final PreferenceScreen screen = preferenceManager.createPreferenceScreen(context);
    section.addPreferences(context, screen);

    final Preference overlaysCategoryPref = screen.findPreference("section:overlays");
    Assert.assertNotNull(overlaysCategoryPref);
    Assert.assertTrue(overlaysCategoryPref instanceof PreferenceCategory);

    final PreferenceCategory overlays = (PreferenceCategory) overlaysCategoryPref;
    Assert.assertEquals(3, overlays.getPreferenceCount());
    for (int i = 0; i < overlays.getPreferenceCount(); i++) {
      final Preference pref = overlays.getPreference(i);
      Assert.assertFalse(pref instanceof TwoStatePreference);
      Assert.assertFalse(pref instanceof DialogPreference);
    }
  }
}
