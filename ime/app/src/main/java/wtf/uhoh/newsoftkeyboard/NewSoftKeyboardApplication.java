package wtf.uhoh.newsoftkeyboard;

import com.menny.android.anysoftkeyboard.AnyApplication;

/**
 * Branding-friendly Application entry point for the NSK flavor.
 *
 * <p>It intentionally inherits from the legacy {@link AnyApplication} to preserve existing runtime
 * wiring and AnySoftKeyboard compatibility while allowing a NewSoftKeyboard-qualified component
 * name in the manifest.
 */
public class NewSoftKeyboardApplication extends AnyApplication {}
