package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/** Stores user-selected theme appearance overrides per theme id. */
public class KeyboardThemeUserOverridesStore extends KeyboardThemeUserOverridesPrefs {

  private static final String PREF_CHANGE_PREFIX = "theme_user_overrides_change::";

  public static final String KEY_FONT_FAMILY_CUSTOM = "custom";
  public static final String KEY_FONT_FAMILY_TOKEN_SECONDARY = "token_secondary";
  public static final int TOKEN_SECONDARY_INT = Integer.MIN_VALUE + 7;

  private final KeyboardThemeUserCustomFontStore customFontStore;

  public KeyboardThemeUserOverridesStore(@NonNull Context context) {
    super(DirectBootAwareSharedPreferences.create(context.getApplicationContext()));
    customFontStore = new KeyboardThemeUserCustomFontStore(context, prefs);
  }

  @NonNull
  public static String changeKey(@NonNull String themeId) {
    return PREF_CHANGE_PREFIX + themeId;
  }

  @NonNull
  public File getCustomKeyFontFile(@NonNull String themeId) {
    return customFontStore.getCustomKeyFontFile(themeId);
  }

  public boolean hasCustomKeyFont(@NonNull String themeId) {
    return customFontStore.hasCustomKeyFont(themeId);
  }

  @Nullable
  public Typeface getCustomKeyFontTypefaceIfAny(@NonNull String themeId) {
    return customFontStore.getCustomKeyFontTypefaceIfAny(themeId);
  }

  @Nullable
  public String getCustomKeyFontDisplayName(@NonNull String themeId) {
    return customFontStore.getCustomKeyFontDisplayName(themeId);
  }

  public void importCustomKeyFontFromUri(@NonNull String themeId, @NonNull Uri sourceUri)
      throws IOException {
    customFontStore.importCustomKeyFontFromUri(themeId, sourceUri);
  }

  public void importCustomKeyFontFromFile(
      @NonNull String themeId, @NonNull File sourceFile, @Nullable String displayName)
      throws IOException {
    customFontStore.importCustomKeyFontFromFile(themeId, sourceFile, displayName);
  }

  public void clearCustomKeyFont(@NonNull String themeId) {
    customFontStore.clearCustomKeyFont(themeId);
  }

  public boolean canLoadCustomKeyFont(@NonNull String themeId) {
    return customFontStore.canLoadCustomKeyFont(themeId);
  }

  @Override
  protected void clearCustomKeyFontFileNoChange(@NonNull String themeId) {
    customFontStore.clearCustomKeyFontFileNoChange(themeId);
  }

  @Override
  protected void copyCustomKeyFontFile(
      @NonNull String sourceThemeId, @NonNull String targetThemeId) {
    customFontStore.copyCustomKeyFontFile(sourceThemeId, targetThemeId);
  }
}
