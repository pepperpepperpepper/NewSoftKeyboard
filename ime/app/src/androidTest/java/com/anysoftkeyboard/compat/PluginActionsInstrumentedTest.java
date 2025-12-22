package com.anysoftkeyboard.compat;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.api.PluginActions;

@RunWith(AndroidJUnit4.class)
public class PluginActionsInstrumentedTest {

  @Test
  public void keyboardsRecognizeBothNamespaces() {
    assertTrue(PluginActions.isKeyboardAction(PluginActions.ACTION_KEYBOARD_NEW));
    assertTrue(PluginActions.isKeyboardAction(PluginActions.ACTION_KEYBOARD_ASK));
    assertTrue(PluginActions.isKeyboardAction(PluginActions.ACTION_KEYBOARD_ASK_MENNY));
  }

  @Test
  public void dictionariesRecognizeBothNamespaces() {
    assertTrue(PluginActions.isDictionaryAction(PluginActions.ACTION_DICTIONARY_NEW));
    assertTrue(PluginActions.isDictionaryAction(PluginActions.ACTION_DICTIONARY_ASK));
    assertTrue(PluginActions.isDictionaryAction(PluginActions.ACTION_DICTIONARY_ASK_MENNY));
  }
}
