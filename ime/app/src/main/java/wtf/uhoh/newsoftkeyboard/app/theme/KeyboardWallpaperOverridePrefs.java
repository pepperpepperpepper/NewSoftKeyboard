package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

final class KeyboardWallpaperOverridePrefs extends KeyboardWallpaperOverrideLayerStackPrefs {

  KeyboardWallpaperOverridePrefs(
      @NonNull SharedPreferences prefs, @NonNull KeyboardWallpaperFileStore fileStore) {
    super(prefs, fileStore);
  }
}
