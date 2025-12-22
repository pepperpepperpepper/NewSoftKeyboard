package com.anysoftkeyboard.compat;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.anysoftkeyboard.addons.AddOnsFactory.ReceiverSpec;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtensionFactory;
import com.anysoftkeyboard.keyboards.KeyboardFactory;
import com.anysoftkeyboard.quicktextkeys.QuickTextKeyFactory;
import com.anysoftkeyboard.theme.KeyboardThemeFactory;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.api.PluginActions;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class PluginActionsWiringTest {

  @SuppressWarnings("unchecked")
  private static List<ReceiverSpec> getReceiverSpecs(Object factory) throws Exception {
    Field f = AddOnsFactory.class.getDeclaredField("mReceiverSpecs");
    f.setAccessible(true);
    return (List<ReceiverSpec>) f.get(factory);
  }

  @Test
  public void keyboardFactoryContainsBothNamespaces() throws Exception {
    KeyboardFactory factory = new KeyboardFactory(getApplicationContext());
    List<ReceiverSpec> specs = getReceiverSpecs(factory);

    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_KEYBOARD_NEW, PluginActions.METADATA_KEYBOARDS_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_KEYBOARD_ASK, PluginActions.METADATA_KEYBOARDS_ASK)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_KEYBOARD_ASK_MENNY,
                PluginActions.METADATA_KEYBOARDS_ASK_MENNY)));
  }

  @Test
  public void dictionaryFactoryContainsBothNamespaces() throws Exception {
    ExternalDictionaryFactory factory = new ExternalDictionaryFactory(getApplicationContext());
    List<ReceiverSpec> specs = getReceiverSpecs(factory);

    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_DICTIONARY_NEW, PluginActions.METADATA_DICTIONARIES_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_DICTIONARY_ASK, PluginActions.METADATA_DICTIONARIES_ASK)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_DICTIONARY_ASK_MENNY,
                PluginActions.METADATA_DICTIONARIES_ASK_MENNY)));
  }

  @Test
  public void extensionKeyboardFactoryContainsBothNamespaces() throws Exception {
    KeyboardExtensionFactory factory =
        new KeyboardExtensionFactory(
            getApplicationContext(),
            com.menny.android.anysoftkeyboard.R.string.settings_default_ext_kbd_bottom_row_key,
            KeyboardExtensionFactory.BOTTOM_ROW_PREF_ID_PREFIX,
            com.anysoftkeyboard.keyboardextensions.KeyboardExtension.TYPE_BOTTOM);
    List<ReceiverSpec> specs = getReceiverSpecs(factory);
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_EXTENSION_KEYBOARD_NEW,
                PluginActions.METADATA_EXTENSION_KEYBOARD_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_EXTENSION_KEYBOARD_ASK,
                PluginActions.METADATA_EXTENSION_KEYBOARD_ASK)));
  }

  @Test
  public void quickTextFactoryContainsBothNamespaces() throws Exception {
    QuickTextKeyFactory factory = new QuickTextKeyFactory(getApplicationContext());
    List<ReceiverSpec> specs = getReceiverSpecs(factory);
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_QUICK_TEXT_NEW, PluginActions.METADATA_QUICK_TEXT_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_QUICK_TEXT_ASK, PluginActions.METADATA_QUICK_TEXT_ASK)));
  }

  @Test
  public void themeFactoryContainsBothNamespaces() throws Exception {
    KeyboardThemeFactory factory = new KeyboardThemeFactory(getApplicationContext());
    List<ReceiverSpec> specs = getReceiverSpecs(factory);
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_THEME_NEW, PluginActions.METADATA_KEYBOARD_THEME_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_THEME_ASK, PluginActions.METADATA_KEYBOARD_THEME_ASK)));
  }
}
