package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

public class KeyboardThemeCustomizationFragmentTest
    extends RobolectricFragmentTestCase<KeyboardThemeCustomizationFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.keyboardThemeCustomizationFragment;
  }

  @Test
  public void testAnchorVisibilityTogglesWithScaleMode() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    writeSmallBitmap(store.getWallpaperFile(themeId), Color.RED);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final ListPreference scalePref =
        fragment.findPreference("keyboard_theme_wallpaper_customization_scale_mode");
    final ListPreference anchorPref =
        fragment.findPreference("keyboard_theme_wallpaper_customization_anchor");
    Assert.assertNotNull(scalePref);
    Assert.assertNotNull(anchorPref);

    Assert.assertTrue(scalePref.isVisible());
    Assert.assertTrue(anchorPref.isVisible());

    Assert.assertTrue(
        scalePref.callChangeListener(
            String.valueOf(KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE)));
    Assert.assertEquals(
        KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE,
        store.getWallpaperScaleMode(themeId));
    Assert.assertFalse(anchorPref.isEnabled());

    Assert.assertTrue(
        scalePref.callChangeListener(
            String.valueOf(KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT)));
    Assert.assertEquals(
        KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT,
        store.getWallpaperScaleMode(themeId));
    Assert.assertTrue(anchorPref.isEnabled());
  }

  @Test
  public void testAnchorPreferenceUpdatesStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    writeSmallBitmap(store.getWallpaperFile(themeId), Color.GREEN);

    final KeyboardThemeCustomizationFragment fragment = startFragment();
    final ListPreference anchorPref =
        fragment.findPreference("keyboard_theme_wallpaper_customization_anchor");
    Assert.assertNotNull(anchorPref);
    Assert.assertTrue(anchorPref.isVisible());

    Assert.assertTrue(
        anchorPref.callChangeListener(
            String.valueOf(KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT)));
    Assert.assertEquals(
        KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT,
        store.getWallpaperAnchor(themeId));
  }

  @Test
  public void testThemeColorOverridesUpdateStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final EditTextPreference tokenPrimaryText =
        fragment.findPreference("keyboard_theme_token_primary_text_color");
    final EditTextPreference tokenSecondaryText =
        fragment.findPreference("keyboard_theme_token_secondary_text_color");
    final EditTextPreference tokenAccent =
        fragment.findPreference("keyboard_theme_token_accent_color");
    final EditTextPreference tokenKeySurface =
        fragment.findPreference("keyboard_theme_token_key_surface_color");
    final EditTextPreference tokenBackground =
        fragment.findPreference("keyboard_theme_token_background_color");

    final EditTextPreference keyTextColor =
        fragment.findPreference("keyboard_theme_override_key_text_color");
    final EditTextPreference specialKeyTextColor =
        fragment.findPreference("keyboard_theme_override_special_key_text_color");
    final EditTextPreference spacebarTextColor =
        fragment.findPreference("keyboard_theme_override_spacebar_text_color");
    final EditTextPreference hintTextColor =
        fragment.findPreference("keyboard_theme_override_hint_text_color");
    final EditTextPreference keyBackgroundTint =
        fragment.findPreference("keyboard_theme_override_key_background_tint");
    final EditTextPreference specialKeyBackgroundTint =
        fragment.findPreference("keyboard_theme_override_special_key_background_tint");
    final EditTextPreference spacebarBackgroundTint =
        fragment.findPreference("keyboard_theme_override_spacebar_background_tint");
    final EditTextPreference keyboardBackgroundTint =
        fragment.findPreference("keyboard_theme_override_keyboard_background_tint");

    Assert.assertNotNull(keyTextColor);
    Assert.assertNotNull(specialKeyTextColor);
    Assert.assertNotNull(spacebarTextColor);
    Assert.assertNotNull(hintTextColor);
    Assert.assertNotNull(keyBackgroundTint);
    Assert.assertNotNull(specialKeyBackgroundTint);
    Assert.assertNotNull(spacebarBackgroundTint);
    Assert.assertNotNull(keyboardBackgroundTint);

    Assert.assertNotNull(tokenPrimaryText);
    Assert.assertNotNull(tokenSecondaryText);
    Assert.assertNotNull(tokenAccent);
    Assert.assertNotNull(tokenKeySurface);
    Assert.assertNotNull(tokenBackground);

    Assert.assertTrue(tokenPrimaryText.callChangeListener("#010101"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#010101"), store.getTokenPrimaryTextColor(themeId));

    Assert.assertTrue(tokenSecondaryText.callChangeListener("#020202"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#020202"), store.getTokenSecondaryTextColor(themeId));

    Assert.assertTrue(tokenAccent.callChangeListener("#030303"));
    Assert.assertEquals((Integer) Color.parseColor("#030303"), store.getTokenAccentColor(themeId));

    Assert.assertTrue(tokenKeySurface.callChangeListener("#040404"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#040404"), store.getTokenKeySurfaceColor(themeId));

    Assert.assertTrue(tokenBackground.callChangeListener("#050505"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#050505"), store.getTokenBackgroundColor(themeId));

    Assert.assertTrue(keyTextColor.callChangeListener("#FF0000"));
    Assert.assertEquals((Integer) Color.RED, store.getKeyTextColor(themeId));

    Assert.assertTrue(specialKeyTextColor.callChangeListener("#112233"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#112233"), store.getSpecialKeyTextColor(themeId));

    Assert.assertTrue(spacebarTextColor.callChangeListener("#445566"));
    Assert.assertEquals((Integer) Color.parseColor("#445566"), store.getSpacebarTextColor(themeId));

    Assert.assertTrue(hintTextColor.callChangeListener("#00FF00"));
    Assert.assertEquals((Integer) Color.GREEN, store.getHintTextColor(themeId));

    Assert.assertTrue(keyBackgroundTint.callChangeListener("#0000FF"));
    Assert.assertEquals((Integer) Color.BLUE, store.getKeyBackgroundTint(themeId));

    Assert.assertTrue(specialKeyBackgroundTint.callChangeListener("#102030"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#102030"), store.getSpecialKeyBackgroundTint(themeId));

    Assert.assertTrue(spacebarBackgroundTint.callChangeListener("#405060"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#405060"), store.getSpacebarBackgroundTint(themeId));

    Assert.assertTrue(keyboardBackgroundTint.callChangeListener("#123456"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#123456"), store.getKeyboardBackgroundTint(themeId));

    Assert.assertTrue(keyTextColor.callChangeListener(""));
    Assert.assertNull(store.getKeyTextColor(themeId));
    Assert.assertTrue(specialKeyTextColor.callChangeListener(""));
    Assert.assertNull(store.getSpecialKeyTextColor(themeId));
    Assert.assertTrue(spacebarTextColor.callChangeListener(""));
    Assert.assertNull(store.getSpacebarTextColor(themeId));
    Assert.assertTrue(specialKeyBackgroundTint.callChangeListener(""));
    Assert.assertNull(store.getSpecialKeyBackgroundTint(themeId));
    Assert.assertTrue(spacebarBackgroundTint.callChangeListener(""));
    Assert.assertNull(store.getSpacebarBackgroundTint(themeId));
  }

  @Test
  public void testAutoReadableColorsPrefEnabledOnlyWhenPhotoSet() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);

    final KeyboardThemeCustomizationFragment fragmentWithoutPhoto = startFragment();
    final CheckBoxPreference ensureReadable =
        fragmentWithoutPhoto.findPreference("keyboard_theme_appearance_ensure_readable_text");
    Assert.assertNotNull(ensureReadable);
    Assert.assertFalse(ensureReadable.isEnabled());

    final Preference autoReadable =
        fragmentWithoutPhoto.findPreference("keyboard_theme_appearance_auto_readable_colors");
    Assert.assertNotNull(autoReadable);
    Assert.assertFalse(autoReadable.isEnabled());

    final Preference autoPhotoColors =
        fragmentWithoutPhoto.findPreference("keyboard_theme_appearance_auto_photo_colors");
    Assert.assertNotNull(autoPhotoColors);
    Assert.assertFalse(autoPhotoColors.isEnabled());

    final Preference highContrast =
        fragmentWithoutPhoto.findPreference("keyboard_theme_appearance_high_contrast_dark");
    Assert.assertNotNull(highContrast);
    Assert.assertTrue(highContrast.isEnabled());
  }

  @Test
  public void testAutoReadableColorsPrefEnabledWithPhoto() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore store = new KeyboardWallpaperOverrideStore(context);
    store.clear(themeId);
    writeSmallBitmap(store.getWallpaperFile(themeId), Color.RED);

    final KeyboardThemeCustomizationFragment fragment = startFragment();
    final CheckBoxPreference ensureReadable =
        fragment.findPreference("keyboard_theme_appearance_ensure_readable_text");
    Assert.assertNotNull(ensureReadable);
    Assert.assertTrue(ensureReadable.isEnabled());

    final Preference autoReadable =
        fragment.findPreference("keyboard_theme_appearance_auto_readable_colors");
    Assert.assertNotNull(autoReadable);
    Assert.assertTrue(autoReadable.isEnabled());

    final Preference autoPhotoColors =
        fragment.findPreference("keyboard_theme_appearance_auto_photo_colors");
    Assert.assertNotNull(autoPhotoColors);
    Assert.assertTrue(autoPhotoColors.isEnabled());
  }

  @Test
  public void testEnsureReadableTextToggleUpdatesStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardWallpaperOverrideStore wallpaperStore =
        new KeyboardWallpaperOverrideStore(context);
    wallpaperStore.clear(themeId);
    writeSmallBitmap(wallpaperStore.getWallpaperFile(themeId), Color.BLUE);

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();
    final CheckBoxPreference ensureReadable =
        fragment.findPreference("keyboard_theme_appearance_ensure_readable_text");
    Assert.assertNotNull(ensureReadable);
    Assert.assertTrue(ensureReadable.isEnabled());
    Assert.assertFalse(Boolean.TRUE.equals(store.getEnsureReadableTextEnabled(themeId)));

    Assert.assertTrue(ensureReadable.callChangeListener(true));
    Assert.assertEquals(Boolean.TRUE, store.getEnsureReadableTextEnabled(themeId));

    Assert.assertTrue(ensureReadable.callChangeListener(false));
    Assert.assertNull(store.getEnsureReadableTextEnabled(themeId));
  }

  @Test
  public void testAutoFitKeyLabelsPreferenceUpdatesStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();
    final CheckBoxPreference autoFit =
        fragment.findPreference("keyboard_theme_override_key_label_auto_fit");
    Assert.assertNotNull(autoFit);
    Assert.assertTrue(autoFit.isChecked());

    Assert.assertTrue(autoFit.callChangeListener(false));
    Assert.assertEquals(Boolean.FALSE, store.getKeyLabelAutoFitEnabled(themeId));

    Assert.assertTrue(autoFit.callChangeListener(true));
    Assert.assertEquals(Boolean.TRUE, store.getKeyLabelAutoFitEnabled(themeId));
  }

  @Test
  public void testThemeTypographyOverridesUpdateStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final ListPreference familyPref =
        fragment.findPreference("keyboard_theme_override_key_font_family");
    final ListPreference stylePref =
        fragment.findPreference("keyboard_theme_override_key_font_style");
    final ListPreference hintFamilyPref =
        fragment.findPreference("keyboard_theme_override_hint_font_family");
    final ListPreference hintStylePref =
        fragment.findPreference("keyboard_theme_override_hint_font_style");
    final ListPreference suggestionFamilyPref =
        fragment.findPreference("keyboard_theme_override_suggestion_font_family");
    final ListPreference suggestionStylePref =
        fragment.findPreference("keyboard_theme_override_suggestion_font_style");
    final ListPreference keyboardNameFamilyPref =
        fragment.findPreference("keyboard_theme_override_keyboard_name_font_family");
    final ListPreference keyboardNameStylePref =
        fragment.findPreference("keyboard_theme_override_keyboard_name_font_style");
    final ListPreference keyLabelSizePref =
        fragment.findPreference("keyboard_theme_override_key_label_text_size");
    final ListPreference hintSizePref =
        fragment.findPreference("keyboard_theme_override_hint_text_size");
    final ListPreference suggestionSizePref =
        fragment.findPreference("keyboard_theme_override_suggestion_text_size");
    final ListPreference keyboardNameSizePref =
        fragment.findPreference("keyboard_theme_override_keyboard_name_text_size");

    Assert.assertNotNull(familyPref);
    Assert.assertNotNull(stylePref);
    Assert.assertNotNull(hintFamilyPref);
    Assert.assertNotNull(hintStylePref);
    Assert.assertNotNull(suggestionFamilyPref);
    Assert.assertNotNull(suggestionStylePref);
    Assert.assertNotNull(keyboardNameFamilyPref);
    Assert.assertNotNull(keyboardNameStylePref);
    Assert.assertNotNull(keyLabelSizePref);
    Assert.assertNotNull(hintSizePref);
    Assert.assertNotNull(suggestionSizePref);
    Assert.assertNotNull(keyboardNameSizePref);

    Assert.assertTrue(familyPref.callChangeListener("serif"));
    Assert.assertEquals("serif", store.getKeyFontFamily(themeId));

    Assert.assertTrue(stylePref.callChangeListener("1"));
    Assert.assertEquals((Integer) 1, store.getKeyFontStyle(themeId));

    Assert.assertTrue(hintFamilyPref.callChangeListener("monospace"));
    Assert.assertEquals("monospace", store.getHintFontFamily(themeId));

    Assert.assertTrue(hintStylePref.callChangeListener("2"));
    Assert.assertEquals((Integer) 2, store.getHintFontStyle(themeId));

    Assert.assertTrue(suggestionFamilyPref.callChangeListener("serif"));
    Assert.assertEquals("serif", store.getSuggestionFontFamily(themeId));

    Assert.assertTrue(suggestionStylePref.callChangeListener("3"));
    Assert.assertEquals((Integer) 3, store.getSuggestionFontStyle(themeId));

    Assert.assertTrue(keyboardNameFamilyPref.callChangeListener("monospace"));
    Assert.assertEquals("monospace", store.getKeyboardNameFontFamily(themeId));

    Assert.assertTrue(keyboardNameStylePref.callChangeListener("1"));
    Assert.assertEquals((Integer) 1, store.getKeyboardNameFontStyle(themeId));

    Assert.assertTrue(keyLabelSizePref.callChangeListener("110"));
    Assert.assertEquals((Integer) 110, store.getKeyLabelTextSizePercent(themeId));

    Assert.assertTrue(hintSizePref.callChangeListener("90"));
    Assert.assertEquals((Integer) 90, store.getHintTextSizePercent(themeId));

    Assert.assertTrue(suggestionSizePref.callChangeListener("110"));
    Assert.assertEquals((Integer) 110, store.getSuggestionTextSizePercent(themeId));

    Assert.assertTrue(keyboardNameSizePref.callChangeListener("90"));
    Assert.assertEquals((Integer) 90, store.getKeyboardNameTextSizePercent(themeId));

    Assert.assertTrue(familyPref.callChangeListener("theme"));
    Assert.assertNull(store.getKeyFontFamily(themeId));

    Assert.assertTrue(hintFamilyPref.callChangeListener("key"));
    Assert.assertNull(store.getHintFontFamily(themeId));

    Assert.assertTrue(suggestionFamilyPref.callChangeListener("key"));
    Assert.assertNull(store.getSuggestionFontFamily(themeId));

    Assert.assertTrue(keyboardNameFamilyPref.callChangeListener("label"));
    Assert.assertNull(store.getKeyboardNameFontFamily(themeId));

    Assert.assertTrue(keyLabelSizePref.callChangeListener("theme"));
    Assert.assertNull(store.getKeyLabelTextSizePercent(themeId));

    Assert.assertTrue(hintSizePref.callChangeListener("key"));
    Assert.assertNull(store.getHintTextSizePercent(themeId));

    Assert.assertTrue(suggestionSizePref.callChangeListener("key"));
    Assert.assertNull(store.getSuggestionTextSizePercent(themeId));

    Assert.assertTrue(keyboardNameSizePref.callChangeListener("key"));
    Assert.assertNull(store.getKeyboardNameTextSizePercent(themeId));
  }

  @Test
  public void testThemeTextShadowOverridesUpdateStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final EditTextPreference shadowColor =
        fragment.findPreference("keyboard_theme_override_key_text_shadow_color");
    final EditTextPreference shadowRadiusDp =
        fragment.findPreference("keyboard_theme_override_key_text_shadow_radius_dp");
    final EditTextPreference shadowOffsetXDp =
        fragment.findPreference("keyboard_theme_override_key_text_shadow_offset_x_dp");
    final EditTextPreference shadowOffsetYDp =
        fragment.findPreference("keyboard_theme_override_key_text_shadow_offset_y_dp");

    Assert.assertNotNull(shadowColor);
    Assert.assertNotNull(shadowRadiusDp);
    Assert.assertNotNull(shadowOffsetXDp);
    Assert.assertNotNull(shadowOffsetYDp);

    Assert.assertTrue(shadowColor.callChangeListener("#55000000"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#55000000"), store.getKeyTextShadowColor(themeId));

    Assert.assertTrue(shadowRadiusDp.callChangeListener("2"));
    Assert.assertEquals((Integer) 2, store.getKeyTextShadowRadiusDp(themeId));

    Assert.assertTrue(shadowOffsetXDp.callChangeListener("-1"));
    Assert.assertEquals((Integer) (-1), store.getKeyTextShadowOffsetXDp(themeId));

    Assert.assertTrue(shadowOffsetYDp.callChangeListener("3"));
    Assert.assertEquals((Integer) 3, store.getKeyTextShadowOffsetYDp(themeId));

    Assert.assertTrue(shadowColor.callChangeListener(""));
    Assert.assertNull(store.getKeyTextShadowColor(themeId));
  }

  @Test
  public void testThemeKeyShadowOverridesUpdateStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final EditTextPreference shadowColor =
        fragment.findPreference("keyboard_theme_override_key_background_shadow_color");
    final EditTextPreference shadowOffsetXDp =
        fragment.findPreference("keyboard_theme_override_key_background_shadow_offset_x_dp");
    final EditTextPreference shadowOffsetYDp =
        fragment.findPreference("keyboard_theme_override_key_background_shadow_offset_y_dp");

    Assert.assertNotNull(shadowColor);
    Assert.assertNotNull(shadowOffsetXDp);
    Assert.assertNotNull(shadowOffsetYDp);

    Assert.assertTrue(shadowOffsetXDp.isVisible());
    Assert.assertTrue(shadowOffsetYDp.isVisible());
    Assert.assertFalse(shadowOffsetXDp.isEnabled());
    Assert.assertFalse(shadowOffsetYDp.isEnabled());

    Assert.assertTrue(shadowColor.callChangeListener("#55000000"));
    Assert.assertEquals(
        (Integer) Color.parseColor("#55000000"), store.getKeyBackgroundShadowColor(themeId));

    Assert.assertTrue(shadowOffsetXDp.isEnabled());
    Assert.assertTrue(shadowOffsetYDp.isEnabled());

    Assert.assertTrue(shadowOffsetXDp.callChangeListener("-2"));
    Assert.assertEquals((Integer) (-2), store.getKeyBackgroundShadowOffsetXDp(themeId));

    Assert.assertTrue(shadowOffsetYDp.callChangeListener("3"));
    Assert.assertEquals((Integer) 3, store.getKeyBackgroundShadowOffsetYDp(themeId));

    Assert.assertTrue(shadowColor.callChangeListener(""));
    Assert.assertNull(store.getKeyBackgroundShadowColor(themeId));
    Assert.assertNull(store.getKeyBackgroundShadowOffsetXDp(themeId));
    Assert.assertNull(store.getKeyBackgroundShadowOffsetYDp(themeId));
  }

  @Test
  public void testKeyLabelFitPreferencesUpdateStore() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    Assert.assertNotNull(theme);
    final String themeId = theme.getId();

    final KeyboardThemeUserOverridesStore store = new KeyboardThemeUserOverridesStore(context);
    store.clearAllOverrides(themeId);

    final KeyboardThemeCustomizationFragment fragment = startFragment();

    final CheckBoxPreference autoFit =
        fragment.findPreference("keyboard_theme_override_key_label_auto_fit");
    final ListPreference minSize =
        fragment.findPreference("keyboard_theme_override_key_label_auto_fit_min_size_percent");
    final CheckBoxPreference ellipsize =
        fragment.findPreference("keyboard_theme_override_key_label_ellipsize");

    Assert.assertNotNull(autoFit);
    Assert.assertNotNull(minSize);
    Assert.assertNotNull(ellipsize);

    Assert.assertTrue(autoFit.isChecked());
    Assert.assertTrue(minSize.isEnabled());
    Assert.assertEquals("30", minSize.getValue());

    Assert.assertNull(store.getKeyLabelAutoFitMinSizePercent(themeId));
    Assert.assertTrue(minSize.callChangeListener("50"));
    Assert.assertEquals((Integer) 50, store.getKeyLabelAutoFitMinSizePercent(themeId));

    Assert.assertTrue(minSize.callChangeListener("30"));
    Assert.assertNull(store.getKeyLabelAutoFitMinSizePercent(themeId));

    Assert.assertTrue(ellipsize.isChecked());
    Assert.assertNull(store.getKeyLabelEllipsizeEnabled(themeId));
    Assert.assertTrue(ellipsize.callChangeListener(false));
    Assert.assertEquals(Boolean.FALSE, store.getKeyLabelEllipsizeEnabled(themeId));
    Assert.assertTrue(ellipsize.callChangeListener(true));
    Assert.assertNull(store.getKeyLabelEllipsizeEnabled(themeId));

    Assert.assertTrue(autoFit.callChangeListener(false));
    Assert.assertEquals(Boolean.FALSE, store.getKeyLabelAutoFitEnabled(themeId));
    Assert.assertFalse(minSize.isEnabled());
  }

  private static void writeSmallBitmap(java.io.File file, int color) throws Exception {
    final java.io.File parent = file.getParentFile();
    Assert.assertTrue(parent.isDirectory() || parent.mkdirs());
    final Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(color);
    try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
      Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }
}
