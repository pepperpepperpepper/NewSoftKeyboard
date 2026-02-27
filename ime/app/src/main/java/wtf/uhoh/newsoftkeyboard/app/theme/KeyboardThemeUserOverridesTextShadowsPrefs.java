package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesTextShadowsPrefs
    extends KeyboardThemeUserOverridesTextShadowTargetsPrefs {
  KeyboardThemeUserOverridesTextShadowsPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }
}
