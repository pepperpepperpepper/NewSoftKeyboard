package wtf.uhoh.newsoftkeyboard.compat.addons.cts;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.DictionaryAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ExternalDictionaryFactory;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardFactory;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickTextKey;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickTextKeyFactory;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeFactory;

/**
 * Verifies that add-on discovery works for both the NewSoftKeyboard and legacy ASK namespaces using
 * a test receiver declared in the androidTest manifest.
 */
@RunWith(AndroidJUnit4.class)
public class AddOnDiscoveryInstrumentedTest {

  @Test
  public void discoversTestKeyboardsViaAllNamespaces() {
    Context context = getApplicationContext();
    KeyboardFactory factory = new KeyboardFactory(context);

    List<KeyboardAddOnAndBuilder> addOns = factory.getAllAddOns();

    assertTrue(
        "Expected NewSoftKeyboard-namespace test keyboard add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_keyboard_new")));

    assertTrue(
        "Expected plugin-legacy test keyboard add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_keyboard_plugin")));

    assertTrue(
        "Expected menny-legacy test keyboard add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_keyboard_menny")));
  }

  @Test
  public void discoversTestDictionariesViaAllNamespaces() {
    Context context = getApplicationContext();
    ExternalDictionaryFactory factory = new ExternalDictionaryFactory(context);

    List<DictionaryAddOnAndBuilder> addOns = factory.getAllAddOns();

    assertTrue(
        "Expected NewSoftKeyboard-namespace test dictionary add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_dictionary_new")));

    assertTrue(
        "Expected plugin-legacy test dictionary add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_dictionary_plugin")));

    assertTrue(
        "Expected menny-legacy test dictionary add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_dictionary_menny")));
  }

  @Test
  public void discoversTestThemesViaNewAndLegacyNamespaces() {
    Context context = getApplicationContext();
    KeyboardThemeFactory factory = new KeyboardThemeFactory(context);

    List<KeyboardTheme> addOns = factory.getAllAddOns();

    assertTrue(
        "Expected NewSoftKeyboard-namespace test theme add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_theme_new")));

    assertTrue(
        "Expected plugin-legacy test theme add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_theme_plugin")));
  }

  @Test
  public void discoversTestQuickTextKeysViaNewAndLegacyNamespaces() {
    Context context = getApplicationContext();
    QuickTextKeyFactory factory = new QuickTextKeyFactory(context);

    List<QuickTextKey> addOns = factory.getAllAddOns();

    assertTrue(
        "Expected NewSoftKeyboard-namespace test quick-text add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_quick_text_new")));

    assertTrue(
        "Expected plugin-legacy test quick-text add-on to be discovered",
        addOns.stream().map(AddOn::getId).anyMatch(id -> id.equals("test_quick_text_plugin")));
  }
}
