package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesPrefs
    extends KeyboardThemeUserOverridesKeyBackgroundShadowsPrefs {
  KeyboardThemeUserOverridesPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  public void clearAllOverrides(@NonNull String themeId) {
    clearCustomKeyFontFileNoChange(themeId);
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(ensureReadableTextEnabledKey(themeId));
    editor.remove(tokenPrimaryTextColorKey(themeId));
    editor.remove(tokenSecondaryTextColorKey(themeId));
    editor.remove(tokenAccentColorKey(themeId));
    editor.remove(tokenKeySurfaceColorKey(themeId));
    editor.remove(tokenBackgroundColorKey(themeId));
    editor.remove(tokenSecondaryFontFamilyKey(themeId));
    editor.remove(tokenSecondaryFontStyleKey(themeId));
    editor.remove(tokenSecondaryTextSizePercentKey(themeId));
    removeAllTextShadowOverridesNoChange(themeId, editor);
    removeAllKeyBackgroundShadowOverridesNoChange(themeId, editor);
    editor.remove(keyTextColorKey(themeId));
    editor.remove(specialKeyTextColorKey(themeId));
    editor.remove(spacebarTextColorKey(themeId));
    editor.remove(modifierKeyTextColorKey(themeId));
    editor.remove(enterKeyTextColorKey(themeId));
    editor.remove(hintTextColorKey(themeId));
    editor.remove(keyBackgroundTintKey(themeId));
    editor.remove(specialKeyBackgroundTintKey(themeId));
    editor.remove(spacebarBackgroundTintKey(themeId));
    editor.remove(modifierKeyBackgroundTintKey(themeId));
    editor.remove(enterKeyBackgroundTintKey(themeId));
    editor.remove(keyboardBackgroundTintKey(themeId));
    editor.remove(keyBackgroundOpacityPercentKey(themeId));
    editor.remove(keyboardBackgroundOpacityPercentKey(themeId));
    editor.remove(keyFontFamilyKey(themeId));
    editor.remove(keyFontStyleKey(themeId));
    editor.remove(hintFontFamilyKey(themeId));
    editor.remove(hintFontStyleKey(themeId));
    editor.remove(suggestionFontFamilyKey(themeId));
    editor.remove(suggestionFontStyleKey(themeId));
    editor.remove(keyboardNameFontFamilyKey(themeId));
    editor.remove(keyboardNameFontStyleKey(themeId));
    editor.remove(keyCustomFontNameKey(themeId));
    editor.remove(keyLabelAutoFitEnabledKey(themeId));
    editor.remove(keyLabelAutoFitMinSizePercentKey(themeId));
    editor.remove(keyLabelEllipsizeEnabledKey(themeId));
    editor.remove(keyLabelTextSizePercentKey(themeId));
    editor.remove(hintTextSizePercentKey(themeId));
    editor.remove(suggestionTextSizePercentKey(themeId));
    editor.remove(keyboardNameTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void copyToTheme(@NonNull String sourceThemeId, @NonNull String targetThemeId) {
    if (sourceThemeId.equals(targetThemeId)) return;

    copyCustomKeyFontFile(sourceThemeId, targetThemeId);
    final SharedPreferences.Editor editor = prefs.edit();
    copyBooleanOrRemove(
        editor,
        ensureReadableTextEnabledKey(sourceThemeId),
        ensureReadableTextEnabledKey(targetThemeId));
    copyIntOrRemove(
        editor, tokenPrimaryTextColorKey(sourceThemeId), tokenPrimaryTextColorKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryTextColorKey(sourceThemeId),
        tokenSecondaryTextColorKey(targetThemeId));
    copyIntOrRemove(editor, tokenAccentColorKey(sourceThemeId), tokenAccentColorKey(targetThemeId));
    copyIntOrRemove(
        editor, tokenKeySurfaceColorKey(sourceThemeId), tokenKeySurfaceColorKey(targetThemeId));
    copyIntOrRemove(
        editor, tokenBackgroundColorKey(sourceThemeId), tokenBackgroundColorKey(targetThemeId));
    copyStringOrRemove(
        editor,
        tokenSecondaryFontFamilyKey(sourceThemeId),
        tokenSecondaryFontFamilyKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryFontStyleKey(sourceThemeId),
        tokenSecondaryFontStyleKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryTextSizePercentKey(sourceThemeId),
        tokenSecondaryTextSizePercentKey(targetThemeId));
    copyTextShadowOverridesNoChange(editor, sourceThemeId, targetThemeId);
    copyKeyBackgroundShadowOverridesNoChange(editor, sourceThemeId, targetThemeId);
    copyIntOrRemove(editor, keyTextColorKey(sourceThemeId), keyTextColorKey(targetThemeId));
    copyIntOrRemove(
        editor, specialKeyTextColorKey(sourceThemeId), specialKeyTextColorKey(targetThemeId));
    copyIntOrRemove(
        editor, spacebarTextColorKey(sourceThemeId), spacebarTextColorKey(targetThemeId));
    copyIntOrRemove(
        editor, modifierKeyTextColorKey(sourceThemeId), modifierKeyTextColorKey(targetThemeId));
    copyIntOrRemove(
        editor, enterKeyTextColorKey(sourceThemeId), enterKeyTextColorKey(targetThemeId));
    copyIntOrRemove(editor, hintTextColorKey(sourceThemeId), hintTextColorKey(targetThemeId));
    copyIntOrRemove(
        editor, keyBackgroundTintKey(sourceThemeId), keyBackgroundTintKey(targetThemeId));
    copyIntOrRemove(
        editor,
        specialKeyBackgroundTintKey(sourceThemeId),
        specialKeyBackgroundTintKey(targetThemeId));
    copyIntOrRemove(
        editor, spacebarBackgroundTintKey(sourceThemeId), spacebarBackgroundTintKey(targetThemeId));
    copyIntOrRemove(
        editor,
        modifierKeyBackgroundTintKey(sourceThemeId),
        modifierKeyBackgroundTintKey(targetThemeId));
    copyIntOrRemove(
        editor, enterKeyBackgroundTintKey(sourceThemeId), enterKeyBackgroundTintKey(targetThemeId));
    copyIntOrRemove(
        editor, keyboardBackgroundTintKey(sourceThemeId), keyboardBackgroundTintKey(targetThemeId));
    copyIntOrRemove(
        editor,
        keyBackgroundOpacityPercentKey(sourceThemeId),
        keyBackgroundOpacityPercentKey(targetThemeId));
    copyIntOrRemove(
        editor,
        keyboardBackgroundOpacityPercentKey(sourceThemeId),
        keyboardBackgroundOpacityPercentKey(targetThemeId));

    copyStringOrRemove(editor, keyFontFamilyKey(sourceThemeId), keyFontFamilyKey(targetThemeId));
    copyStringOrRemove(editor, hintFontFamilyKey(sourceThemeId), hintFontFamilyKey(targetThemeId));
    copyStringOrRemove(
        editor, suggestionFontFamilyKey(sourceThemeId), suggestionFontFamilyKey(targetThemeId));
    copyStringOrRemove(
        editor, keyboardNameFontFamilyKey(sourceThemeId), keyboardNameFontFamilyKey(targetThemeId));
    copyStringOrRemove(
        editor, keyCustomFontNameKey(sourceThemeId), keyCustomFontNameKey(targetThemeId));
    copyBooleanOrRemove(
        editor, keyLabelAutoFitEnabledKey(sourceThemeId), keyLabelAutoFitEnabledKey(targetThemeId));
    copyIntOrRemove(
        editor,
        keyLabelAutoFitMinSizePercentKey(sourceThemeId),
        keyLabelAutoFitMinSizePercentKey(targetThemeId));
    copyBooleanOrRemove(
        editor,
        keyLabelEllipsizeEnabledKey(sourceThemeId),
        keyLabelEllipsizeEnabledKey(targetThemeId));
    copyIntOrRemove(
        editor,
        keyLabelTextSizePercentKey(sourceThemeId),
        keyLabelTextSizePercentKey(targetThemeId));
    copyIntOrRemove(
        editor, hintTextSizePercentKey(sourceThemeId), hintTextSizePercentKey(targetThemeId));
    copyIntOrRemove(
        editor,
        suggestionTextSizePercentKey(sourceThemeId),
        suggestionTextSizePercentKey(targetThemeId));
    copyIntOrRemove(
        editor,
        keyboardNameTextSizePercentKey(sourceThemeId),
        keyboardNameTextSizePercentKey(targetThemeId));
    copyIntOrRemove(editor, keyFontStyleKey(sourceThemeId), keyFontStyleKey(targetThemeId));
    copyIntOrRemove(editor, hintFontStyleKey(sourceThemeId), hintFontStyleKey(targetThemeId));
    copyIntOrRemove(
        editor, suggestionFontStyleKey(sourceThemeId), suggestionFontStyleKey(targetThemeId));
    copyIntOrRemove(
        editor, keyboardNameFontStyleKey(sourceThemeId), keyboardNameFontStyleKey(targetThemeId));

    markChanged(targetThemeId, editor);
    editor.apply();
  }
}
