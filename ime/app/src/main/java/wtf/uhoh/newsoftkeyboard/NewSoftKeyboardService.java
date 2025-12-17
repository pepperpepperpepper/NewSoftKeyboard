/*
 * NewSoftKeyboardService
 *
 * Branding-friendly service entry point that delegates to the legacy
 * SoftKeyboard implementation while keeping full compatibility with
 * existing AnySoftKeyboard add-ons (they still reference the legacy
 * classes and action strings). This class is registered only in the
 * NSK flavor; the askCompat flavor continues to expose the original
 * SoftKeyboard service.
 */
package wtf.uhoh.newsoftkeyboard;

import android.content.ComponentName;
import com.menny.android.anysoftkeyboard.SoftKeyboard;

public class NewSoftKeyboardService extends SoftKeyboard {

  @Override
  protected String getSettingsInputMethodId() {
    // Ensure settings and IME enable/disable flows point at the NSK-branded
    // service component when this class is the manifest entry point.
    return new ComponentName(getApplication(), NewSoftKeyboardService.class).flattenToShortString();
  }
}
