package com.anysoftkeyboard.addons.cts;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardFactory;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Verifies that add-on discovery works for both the NewSoftKeyboard and legacy ASK namespaces using
 * a test receiver declared in the androidTest manifest.
 */
@RunWith(AndroidJUnit4.class)
public class AddOnDiscoveryInstrumentedTest {

  @Test
  public void discoversTestKeyboardViaBothNamespaces() {
    Context context = getApplicationContext();
    KeyboardFactory factory = new KeyboardFactory(context);

    List<KeyboardAddOnAndBuilder> addOns = factory.getAllAddOns();

    assertTrue(
        "Expected test keyboard add-on to be discovered",
        addOns.stream()
            .map(AddOn::getId)
            .anyMatch(id -> id.equals("test_keyboard_ns")));
  }
}
