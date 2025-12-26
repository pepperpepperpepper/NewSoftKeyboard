package wtf.uhoh.newsoftkeyboard.compat.addons.cts;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.DictionaryAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ExternalDictionaryFactory;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardFactory;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeFactory;

/**
 * Smoke test for real external add-ons (from F-Droid, etc).
 *
 * <p>This test is gated: it only runs when the expected package(s) are installed on the device.
 *
 * <p>Run with:
 *
 * <pre>
 * adb shell am instrument -w -r \
 *   -e expected_language_pack com.anysoftkeyboard.languagepack.german \
 *   -e expected_theme_pack com.anysoftkeyboard.theme.three_d \
 *   -e class wtf.uhoh.newsoftkeyboard.compat.addons.cts.ExternalAddOnSmokeInstrumentedTest \
 *   wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
 * </pre>
 */
@RunWith(AndroidJUnit4.class)
public class ExternalAddOnSmokeInstrumentedTest {

  private static boolean isPackageInstalled(Context context, String packageName) {
    try {
      context.getPackageManager().getPackageInfo(packageName, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  @Test
  public void discoversExternalLanguagePackKeyboardsAndDictionaries() {
    final String expectedLanguagePack =
        InstrumentationRegistry.getArguments().getString("expected_language_pack");
    assumeTrue(
        "No expected_language_pack provided; skipping external add-on smoke test.",
        expectedLanguagePack != null && !expectedLanguagePack.trim().isEmpty());

    final Context context = getApplicationContext();
    assumeTrue(
        "Expected language pack is not installed: " + expectedLanguagePack,
        isPackageInstalled(context, expectedLanguagePack));

    KeyboardFactory keyboardFactory = new KeyboardFactory(context);
    List<KeyboardAddOnAndBuilder> keyboards = keyboardFactory.getAllAddOns();
    assertTrue(
        "Expected at least one keyboard from " + expectedLanguagePack,
        keyboards.stream().anyMatch(addOn -> expectedLanguagePack.equals(addOn.getPackageName())));

    ExternalDictionaryFactory dictionaryFactory = new ExternalDictionaryFactory(context);
    List<DictionaryAddOnAndBuilder> dictionaries = dictionaryFactory.getAllAddOns();
    assertTrue(
        "Expected at least one dictionary from " + expectedLanguagePack,
        dictionaries.stream()
            .map(AddOn::getPackageName)
            .anyMatch(pkg -> expectedLanguagePack.equals(pkg)));
  }

  @Test
  public void discoversExternalThemePackThemes() {
    final String expectedThemePack =
        InstrumentationRegistry.getArguments().getString("expected_theme_pack");
    assumeTrue(
        "No expected_theme_pack provided; skipping external add-on smoke test.",
        expectedThemePack != null && !expectedThemePack.trim().isEmpty());

    final Context context = getApplicationContext();
    assumeTrue(
        "Expected theme pack is not installed: " + expectedThemePack,
        isPackageInstalled(context, expectedThemePack));

    KeyboardThemeFactory themeFactory = new KeyboardThemeFactory(context);
    List<KeyboardTheme> themes = themeFactory.getAllAddOns();
    assertTrue(
        "Expected at least one theme from " + expectedThemePack,
        themes.stream().map(AddOn::getPackageName).anyMatch(pkg -> expectedThemePack.equals(pkg)));
  }
}
