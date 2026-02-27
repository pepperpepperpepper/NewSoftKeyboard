package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashSet;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeResourcesHolder;

/** Applies theme attributes with overlay-aware host wiring. */
final class ThemeAttributeLoaderRunner {

  private final KeyboardWallpaperResolver keyboardWallpaperResolver;
  private final KeyboardThemeUserOverridesStore themeOverridesStore;
  private final KeyboardThemePresetStore presetStore;

  ThemeAttributeLoaderRunner(@NonNull Context context) {
    keyboardWallpaperResolver = new KeyboardWallpaperResolver(context);
    themeOverridesStore = new KeyboardThemeUserOverridesStore(context);
    presetStore = new KeyboardThemePresetStore(context);
  }

  void applyThemeAttributes(
      KeyboardViewBase host, ThemeOverlayCombiner overlayCombiner, KeyboardTheme theme) {
    final KeyboardViewThemeAndDrawController themeAndDrawController =
        host.getThemeAndDrawController();
    final boolean applyUserOverrides = themeAndDrawController.shouldApplyUserThemeOverrides();
    if (!applyUserOverrides) {
      overlayCombiner.setUserKeyBackgroundTint(null);
      overlayCombiner.setUserKeyboardBackgroundTint(null);
      themeAndDrawController.clearUserThemeOverridesForPreview();
    }

    HashSet<Integer> doneAttrs = new HashSet<>();
    int[] padding = new int[] {0, 0, 0, 0};
    ThemeAttributeLoader themeAttributeLoader =
        new ThemeAttributeLoader(new HostImpl(host, overlayCombiner));
    themeAttributeLoader.loadThemeAttributes(theme, doneAttrs, padding);
    if (applyUserOverrides) {
      applyUserOverridesIfAny(host, themeAndDrawController, overlayCombiner, theme);
    }
    final Drawable background = overlayCombiner.getThemeResources().getKeyboardBackground();
    if (background != null) host.setBackground(background);
    if (applyUserOverrides) {
      keyboardWallpaperResolver.applyPhotoOverrideIfAnyAsync(host, theme);
    }
  }

  private void applyUserOverridesIfAny(
      @NonNull KeyboardViewBase host,
      @NonNull KeyboardViewThemeAndDrawController themeAndDrawController,
      @NonNull ThemeOverlayCombiner overlayCombiner,
      @NonNull KeyboardTheme theme) {
    final String themeId = presetStore.getActivePresetId(theme.getId());

    final Integer tokenPrimaryTextColor = themeOverridesStore.getTokenPrimaryTextColor(themeId);
    final Integer tokenSecondaryTextColor = themeOverridesStore.getTokenSecondaryTextColor(themeId);
    final Integer tokenAccentColor = themeOverridesStore.getTokenAccentColor(themeId);
    final Integer tokenKeySurfaceColor = themeOverridesStore.getTokenKeySurfaceColor(themeId);
    final Integer tokenBackgroundColor = themeOverridesStore.getTokenBackgroundColor(themeId);

    final Integer keyTextColor =
        themeOverridesStore.getKeyTextColor(themeId) != null
            ? themeOverridesStore.getKeyTextColor(themeId)
            : tokenPrimaryTextColor;
    if (keyTextColor != null) {
      overlayCombiner.setThemeTextColor(ColorStateList.valueOf(keyTextColor));
    }

    final Integer spacebarTextColor =
        themeOverridesStore.getSpacebarTextColor(themeId) != null
            ? themeOverridesStore.getSpacebarTextColor(themeId)
            : tokenPrimaryTextColor;
    if (spacebarTextColor != null) {
      overlayCombiner.setThemeNameTextColor(spacebarTextColor);
    }

    final Integer hintTextColor =
        themeOverridesStore.getHintTextColor(themeId) != null
            ? themeOverridesStore.getHintTextColor(themeId)
            : tokenSecondaryTextColor;
    if (hintTextColor != null) {
      overlayCombiner.setThemeHintTextColor(hintTextColor);
    }

    final Integer specialKeyTextColor =
        themeOverridesStore.getSpecialKeyTextColor(themeId) != null
            ? themeOverridesStore.getSpecialKeyTextColor(themeId)
            : tokenAccentColor;
    themeAndDrawController.applyUserSpecialKeyTextColorOverride(specialKeyTextColor);
    themeAndDrawController.applyUserModifierKeyTextColorOverride(
        themeOverridesStore.getModifierKeyTextColor(themeId));
    themeAndDrawController.applyUserEnterKeyTextColorOverride(
        themeOverridesStore.getEnterKeyTextColor(themeId));

    final Integer keyBackgroundTint =
        themeOverridesStore.getKeyBackgroundTint(themeId) != null
            ? themeOverridesStore.getKeyBackgroundTint(themeId)
            : tokenKeySurfaceColor;
    final Integer specialKeyBackgroundTint =
        themeOverridesStore.getSpecialKeyBackgroundTint(themeId) != null
            ? themeOverridesStore.getSpecialKeyBackgroundTint(themeId)
            : tokenAccentColor;
    overlayCombiner.setUserKeyBackgroundTint(keyBackgroundTint);
    themeAndDrawController.applyUserKeyBackgroundTintOverrides(
        keyBackgroundTint,
        specialKeyBackgroundTint,
        themeOverridesStore.getSpacebarBackgroundTint(themeId),
        themeOverridesStore.getModifierKeyBackgroundTint(themeId),
        themeOverridesStore.getEnterKeyBackgroundTint(themeId));
    overlayCombiner.setUserKeyboardBackgroundTint(
        themeOverridesStore.getKeyboardBackgroundTint(themeId) != null
            ? themeOverridesStore.getKeyboardBackgroundTint(themeId)
            : tokenBackgroundColor);

    applyOpacityOverridesIfAny(overlayCombiner, themeId);

    final String tokenSecondaryFontFamily =
        themeOverridesStore.getTokenSecondaryFontFamily(themeId);
    final Integer tokenSecondaryFontStyle = themeOverridesStore.getTokenSecondaryFontStyle(themeId);
    final Integer tokenSecondaryTextSizePercent =
        themeOverridesStore.getTokenSecondaryTextSizePercent(themeId);

    final Typeface fontFamilyOverride =
        resolveKeyFontFamilyOverride(themeOverridesStore.getKeyFontFamily(themeId), themeId);
    themeAndDrawController.applyUserKeyTextTypefaceOverrides(
        fontFamilyOverride, themeOverridesStore.getKeyFontStyle(themeId));

    final String resolvedHintFontFamilyId;
    final String hintFontFamilyId = themeOverridesStore.getHintFontFamily(themeId);
    if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(hintFontFamilyId)) {
      resolvedHintFontFamilyId = tokenSecondaryFontFamily;
    } else {
      resolvedHintFontFamilyId = hintFontFamilyId;
    }
    final Integer resolvedHintFontStyle;
    final Integer hintFontStyle = themeOverridesStore.getHintFontStyle(themeId);
    if (hintFontStyle != null
        && hintFontStyle == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT) {
      resolvedHintFontStyle = tokenSecondaryFontStyle;
    } else {
      resolvedHintFontStyle = hintFontStyle;
    }
    final Typeface hintFontFamilyOverride =
        resolveKeyFontFamilyOverride(resolvedHintFontFamilyId, themeId);
    themeAndDrawController.applyUserHintTextTypefaceOverrides(
        hintFontFamilyOverride, resolvedHintFontStyle);

    final String resolvedKeyboardNameFontFamilyId;
    final String keyboardNameFontFamilyId = themeOverridesStore.getKeyboardNameFontFamily(themeId);
    if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(
        keyboardNameFontFamilyId)) {
      resolvedKeyboardNameFontFamilyId = tokenSecondaryFontFamily;
    } else {
      resolvedKeyboardNameFontFamilyId = keyboardNameFontFamilyId;
    }
    final Integer resolvedKeyboardNameFontStyle;
    final Integer keyboardNameFontStyle = themeOverridesStore.getKeyboardNameFontStyle(themeId);
    if (keyboardNameFontStyle != null
        && keyboardNameFontStyle == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT) {
      resolvedKeyboardNameFontStyle = tokenSecondaryFontStyle;
    } else {
      resolvedKeyboardNameFontStyle = keyboardNameFontStyle;
    }
    final Typeface keyboardNameFontFamilyOverride =
        resolveKeyFontFamilyOverride(resolvedKeyboardNameFontFamilyId, themeId);
    themeAndDrawController.applyUserKeyboardNameTextTypefaceOverrides(
        keyboardNameFontFamilyOverride, resolvedKeyboardNameFontStyle);

    themeAndDrawController.applyUserKeyLabelAutoFitOverrides(
        themeOverridesStore.getKeyLabelAutoFitEnabled(themeId));
    themeAndDrawController.applyUserKeyLabelAutoFitMinSizePercentOverrides(
        themeOverridesStore.getKeyLabelAutoFitMinSizePercent(themeId));
    themeAndDrawController.applyUserKeyLabelEllipsizeOverrides(
        themeOverridesStore.getKeyLabelEllipsizeEnabled(themeId));

    final Integer keyLabelTextSizePercent = themeOverridesStore.getKeyLabelTextSizePercent(themeId);
    final Integer hintTextSizePercentRaw = themeOverridesStore.getHintTextSizePercent(themeId);
    final Integer hintTextSizePercent =
        hintTextSizePercentRaw != null
                && hintTextSizePercentRaw == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT
            ? tokenSecondaryTextSizePercent
            : hintTextSizePercentRaw;
    final Integer keyboardNameTextSizePercentRaw =
        themeOverridesStore.getKeyboardNameTextSizePercent(themeId);
    final Integer keyboardNameTextSizePercent =
        keyboardNameTextSizePercentRaw != null
                && keyboardNameTextSizePercentRaw
                    == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT
            ? tokenSecondaryTextSizePercent
            : keyboardNameTextSizePercentRaw;
    themeAndDrawController.applyUserTextSizeScaleOverrides(
        keyLabelTextSizePercent,
        hintTextSizePercent != null ? hintTextSizePercent : keyLabelTextSizePercent,
        keyboardNameTextSizePercent != null
            ? keyboardNameTextSizePercent
            : keyLabelTextSizePercent);

    final float density = host.getResources().getDisplayMetrics().density;
    final Integer tokenSecondaryTextShadowColor =
        themeOverridesStore.getTokenSecondaryTextShadowColor(themeId);
    final Integer tokenSecondaryTextShadowRadiusDp =
        themeOverridesStore.getTokenSecondaryTextShadowRadiusDp(themeId);
    final Integer tokenSecondaryTextShadowOffsetXDp =
        themeOverridesStore.getTokenSecondaryTextShadowOffsetXDp(themeId);
    final Integer tokenSecondaryTextShadowOffsetYDp =
        themeOverridesStore.getTokenSecondaryTextShadowOffsetYDp(themeId);

    final boolean keyTextShadowUsesTokenSecondary =
        themeOverridesStore.isKeyTextShadowUseTokenSecondary(themeId);
    final Integer keyTextShadowColor =
        keyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowColor
            : themeOverridesStore.getKeyTextShadowColor(themeId);
    final Integer shadowRadiusDp =
        keyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowRadiusDp
            : themeOverridesStore.getKeyTextShadowRadiusDp(themeId);
    final Integer shadowOffsetXDp =
        keyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetXDp
            : themeOverridesStore.getKeyTextShadowOffsetXDp(themeId);
    final Integer shadowOffsetYDp =
        keyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetYDp
            : themeOverridesStore.getKeyTextShadowOffsetYDp(themeId);

    themeAndDrawController.applyUserKeyTextShadowOverrides(
        keyTextShadowColor,
        shadowRadiusDp != null ? Math.round(shadowRadiusDp * density) : null,
        shadowOffsetXDp != null ? Math.round(shadowOffsetXDp * density) : null,
        shadowOffsetYDp != null ? Math.round(shadowOffsetYDp * density) : null);

    final boolean specialKeyTextShadowUsesTokenSecondary =
        themeOverridesStore.isSpecialKeyTextShadowUseTokenSecondary(themeId);
    final Integer specialKeyShadowColor =
        specialKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowColor
            : themeOverridesStore.getSpecialKeyTextShadowColor(themeId);
    final Integer specialKeyShadowRadiusDp =
        specialKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowRadiusDp
            : themeOverridesStore.getSpecialKeyTextShadowRadiusDp(themeId);
    final Integer specialKeyShadowOffsetXDp =
        specialKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetXDp
            : themeOverridesStore.getSpecialKeyTextShadowOffsetXDp(themeId);
    final Integer specialKeyShadowOffsetYDp =
        specialKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetYDp
            : themeOverridesStore.getSpecialKeyTextShadowOffsetYDp(themeId);

    final boolean spacebarTextShadowUsesTokenSecondary =
        themeOverridesStore.isSpacebarKeyTextShadowUseTokenSecondary(themeId);
    final Integer spacebarShadowColor =
        spacebarTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowColor
            : themeOverridesStore.getSpacebarKeyTextShadowColor(themeId);
    final Integer spacebarShadowRadiusDp =
        spacebarTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowRadiusDp
            : themeOverridesStore.getSpacebarKeyTextShadowRadiusDp(themeId);
    final Integer spacebarShadowOffsetXDp =
        spacebarTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetXDp
            : themeOverridesStore.getSpacebarKeyTextShadowOffsetXDp(themeId);
    final Integer spacebarShadowOffsetYDp =
        spacebarTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetYDp
            : themeOverridesStore.getSpacebarKeyTextShadowOffsetYDp(themeId);

    final boolean modifierKeyTextShadowUsesTokenSecondary =
        themeOverridesStore.isModifierKeyTextShadowUseTokenSecondary(themeId);
    final Integer modifierKeyShadowColor =
        modifierKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowColor
            : themeOverridesStore.getModifierKeyTextShadowColor(themeId);
    final Integer modifierKeyShadowRadiusDp =
        modifierKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowRadiusDp
            : themeOverridesStore.getModifierKeyTextShadowRadiusDp(themeId);
    final Integer modifierKeyShadowOffsetXDp =
        modifierKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetXDp
            : themeOverridesStore.getModifierKeyTextShadowOffsetXDp(themeId);
    final Integer modifierKeyShadowOffsetYDp =
        modifierKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetYDp
            : themeOverridesStore.getModifierKeyTextShadowOffsetYDp(themeId);

    final boolean enterKeyTextShadowUsesTokenSecondary =
        themeOverridesStore.isEnterKeyTextShadowUseTokenSecondary(themeId);
    final Integer enterKeyShadowColor =
        enterKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowColor
            : themeOverridesStore.getEnterKeyTextShadowColor(themeId);
    final Integer enterKeyShadowRadiusDp =
        enterKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowRadiusDp
            : themeOverridesStore.getEnterKeyTextShadowRadiusDp(themeId);
    final Integer enterKeyShadowOffsetXDp =
        enterKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetXDp
            : themeOverridesStore.getEnterKeyTextShadowOffsetXDp(themeId);
    final Integer enterKeyShadowOffsetYDp =
        enterKeyTextShadowUsesTokenSecondary
            ? tokenSecondaryTextShadowOffsetYDp
            : themeOverridesStore.getEnterKeyTextShadowOffsetYDp(themeId);

    themeAndDrawController.applyUserPerKeyTextShadowOverrides(
        specialKeyShadowColor,
        specialKeyShadowRadiusDp != null ? Math.round(specialKeyShadowRadiusDp * density) : null,
        specialKeyShadowOffsetXDp != null ? Math.round(specialKeyShadowOffsetXDp * density) : null,
        specialKeyShadowOffsetYDp != null ? Math.round(specialKeyShadowOffsetYDp * density) : null,
        spacebarShadowColor,
        spacebarShadowRadiusDp != null ? Math.round(spacebarShadowRadiusDp * density) : null,
        spacebarShadowOffsetXDp != null ? Math.round(spacebarShadowOffsetXDp * density) : null,
        spacebarShadowOffsetYDp != null ? Math.round(spacebarShadowOffsetYDp * density) : null,
        modifierKeyShadowColor,
        modifierKeyShadowRadiusDp != null ? Math.round(modifierKeyShadowRadiusDp * density) : null,
        modifierKeyShadowOffsetXDp != null
            ? Math.round(modifierKeyShadowOffsetXDp * density)
            : null,
        modifierKeyShadowOffsetYDp != null
            ? Math.round(modifierKeyShadowOffsetYDp * density)
            : null,
        enterKeyShadowColor,
        enterKeyShadowRadiusDp != null ? Math.round(enterKeyShadowRadiusDp * density) : null,
        enterKeyShadowOffsetXDp != null ? Math.round(enterKeyShadowOffsetXDp * density) : null,
        enterKeyShadowOffsetYDp != null ? Math.round(enterKeyShadowOffsetYDp * density) : null);

    final Integer tokenSecondaryKeyBackgroundShadowColor =
        themeOverridesStore.getTokenSecondaryKeyBackgroundShadowColor(themeId);
    final Integer tokenSecondaryKeyBackgroundShadowOffsetXDp =
        themeOverridesStore.getTokenSecondaryKeyBackgroundShadowOffsetXDp(themeId);
    final Integer tokenSecondaryKeyBackgroundShadowOffsetYDp =
        themeOverridesStore.getTokenSecondaryKeyBackgroundShadowOffsetYDp(themeId);
    final Integer tokenSecondaryKeyBackgroundShadowSpreadDp =
        themeOverridesStore.getTokenSecondaryKeyBackgroundShadowSpreadDp(themeId);

    final boolean keyBackgroundShadowUsesTokenSecondary =
        themeOverridesStore.isKeyBackgroundShadowUseTokenSecondary(themeId);
    final Integer keyBackgroundShadowColor =
        keyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowColor
            : themeOverridesStore.getKeyBackgroundShadowColor(themeId);
    final Integer keyBackgroundShadowOffsetXDp =
        keyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetXDp
            : themeOverridesStore.getKeyBackgroundShadowOffsetXDp(themeId);
    final Integer keyBackgroundShadowOffsetYDp =
        keyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetYDp
            : themeOverridesStore.getKeyBackgroundShadowOffsetYDp(themeId);
    final Integer keyBackgroundShadowSpreadDp =
        keyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowSpreadDp
            : themeOverridesStore.getKeyBackgroundShadowSpreadDp(themeId);
    themeAndDrawController.applyUserKeyBackgroundShadowOverrides(
        keyBackgroundShadowColor,
        keyBackgroundShadowOffsetXDp != null
            ? Math.round(keyBackgroundShadowOffsetXDp * density)
            : null,
        keyBackgroundShadowOffsetYDp != null
            ? Math.round(keyBackgroundShadowOffsetYDp * density)
            : null,
        keyBackgroundShadowSpreadDp != null
            ? Math.round(keyBackgroundShadowSpreadDp * density)
            : null);

    final boolean specialKeyBackgroundShadowUsesTokenSecondary =
        themeOverridesStore.isSpecialKeyBackgroundShadowUseTokenSecondary(themeId);
    final Integer specialKeyBackgroundShadowColor =
        specialKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowColor
            : themeOverridesStore.getSpecialKeyBackgroundShadowColor(themeId);
    final Integer specialKeyBackgroundShadowOffsetXDp =
        specialKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetXDp
            : themeOverridesStore.getSpecialKeyBackgroundShadowOffsetXDp(themeId);
    final Integer specialKeyBackgroundShadowOffsetYDp =
        specialKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetYDp
            : themeOverridesStore.getSpecialKeyBackgroundShadowOffsetYDp(themeId);

    final boolean spacebarKeyBackgroundShadowUsesTokenSecondary =
        themeOverridesStore.isSpacebarKeyBackgroundShadowUseTokenSecondary(themeId);
    final Integer spacebarKeyBackgroundShadowColor =
        spacebarKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowColor
            : themeOverridesStore.getSpacebarKeyBackgroundShadowColor(themeId);
    final Integer spacebarKeyBackgroundShadowOffsetXDp =
        spacebarKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetXDp
            : themeOverridesStore.getSpacebarKeyBackgroundShadowOffsetXDp(themeId);
    final Integer spacebarKeyBackgroundShadowOffsetYDp =
        spacebarKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetYDp
            : themeOverridesStore.getSpacebarKeyBackgroundShadowOffsetYDp(themeId);

    final boolean modifierKeyBackgroundShadowUsesTokenSecondary =
        themeOverridesStore.isModifierKeyBackgroundShadowUseTokenSecondary(themeId);
    final Integer modifierKeyBackgroundShadowColor =
        modifierKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowColor
            : themeOverridesStore.getModifierKeyBackgroundShadowColor(themeId);
    final Integer modifierKeyBackgroundShadowOffsetXDp =
        modifierKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetXDp
            : themeOverridesStore.getModifierKeyBackgroundShadowOffsetXDp(themeId);
    final Integer modifierKeyBackgroundShadowOffsetYDp =
        modifierKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetYDp
            : themeOverridesStore.getModifierKeyBackgroundShadowOffsetYDp(themeId);

    final boolean enterKeyBackgroundShadowUsesTokenSecondary =
        themeOverridesStore.isEnterKeyBackgroundShadowUseTokenSecondary(themeId);
    final Integer enterKeyBackgroundShadowColor =
        enterKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowColor
            : themeOverridesStore.getEnterKeyBackgroundShadowColor(themeId);
    final Integer enterKeyBackgroundShadowOffsetXDp =
        enterKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetXDp
            : themeOverridesStore.getEnterKeyBackgroundShadowOffsetXDp(themeId);
    final Integer enterKeyBackgroundShadowOffsetYDp =
        enterKeyBackgroundShadowUsesTokenSecondary
            ? tokenSecondaryKeyBackgroundShadowOffsetYDp
            : themeOverridesStore.getEnterKeyBackgroundShadowOffsetYDp(themeId);

    themeAndDrawController.applyUserPerKeyBackgroundShadowOverrides(
        specialKeyBackgroundShadowColor,
        specialKeyBackgroundShadowOffsetXDp != null
            ? Math.round(specialKeyBackgroundShadowOffsetXDp * density)
            : null,
        specialKeyBackgroundShadowOffsetYDp != null
            ? Math.round(specialKeyBackgroundShadowOffsetYDp * density)
            : null,
        spacebarKeyBackgroundShadowColor,
        spacebarKeyBackgroundShadowOffsetXDp != null
            ? Math.round(spacebarKeyBackgroundShadowOffsetXDp * density)
            : null,
        spacebarKeyBackgroundShadowOffsetYDp != null
            ? Math.round(spacebarKeyBackgroundShadowOffsetYDp * density)
            : null,
        modifierKeyBackgroundShadowColor,
        modifierKeyBackgroundShadowOffsetXDp != null
            ? Math.round(modifierKeyBackgroundShadowOffsetXDp * density)
            : null,
        modifierKeyBackgroundShadowOffsetYDp != null
            ? Math.round(modifierKeyBackgroundShadowOffsetYDp * density)
            : null,
        enterKeyBackgroundShadowColor,
        enterKeyBackgroundShadowOffsetXDp != null
            ? Math.round(enterKeyBackgroundShadowOffsetXDp * density)
            : null,
        enterKeyBackgroundShadowOffsetYDp != null
            ? Math.round(enterKeyBackgroundShadowOffsetYDp * density)
            : null);
  }

  @Nullable
  private Typeface resolveKeyFontFamilyOverride(
      @Nullable String fontFamilyId, @NonNull String themeId) {
    if (fontFamilyId == null) return null;
    return switch (fontFamilyId) {
      case "default" -> Typeface.DEFAULT;
      case "sans" -> Typeface.SANS_SERIF;
      case "serif" -> Typeface.SERIF;
      case "monospace" -> Typeface.MONOSPACE;
      case KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM ->
          themeOverridesStore.getCustomKeyFontTypefaceIfAny(themeId);
      default -> null;
    };
  }

  private void applyOpacityOverridesIfAny(
      @NonNull ThemeOverlayCombiner overlayCombiner, @NonNull String themeId) {
    final int keyBackgroundOpacityPercent =
        resolveOpacityPercentOrDefault(themeOverridesStore.getKeyBackgroundOpacityPercent(themeId));
    final int keyboardBackgroundOpacityPercent =
        resolveOpacityPercentOrDefault(
            themeOverridesStore.getKeyboardBackgroundOpacityPercent(themeId));

    final int keyAlpha = percentToAlpha(keyBackgroundOpacityPercent);
    final int keyboardAlpha = percentToAlpha(keyboardBackgroundOpacityPercent);

    final Drawable keyBackground = overlayCombiner.getThemeResources().getKeyBackground();
    if (keyBackground != null) keyBackground.setAlpha(keyAlpha);

    final Drawable keyboardBackground = overlayCombiner.getThemeResources().getKeyboardBackground();
    if (keyboardBackground != null) keyboardBackground.setAlpha(keyboardAlpha);
  }

  private static int resolveOpacityPercentOrDefault(@Nullable Integer value) {
    if (value == null) return 100;
    return Math.max(0, Math.min(100, value));
  }

  private static int percentToAlpha(int percent) {
    return Math.round(Math.max(0, Math.min(100, percent)) * 255f / 100f);
  }

  private static final class HostImpl implements ThemeAttributeLoader.Host {
    private final KeyboardViewBase host;
    private final ThemeOverlayCombiner themeOverlayCombiner;

    private HostImpl(KeyboardViewBase host, ThemeOverlayCombiner themeOverlayCombiner) {
      this.host = host;
      this.themeOverlayCombiner = themeOverlayCombiner;
    }

    @NonNull
    @Override
    public ThemeResourcesHolder getThemeOverlayResources() {
      return themeOverlayCombiner.getThemeResources();
    }

    @Override
    public int getKeyboardStyleResId(@NonNull KeyboardTheme theme) {
      return host.getKeyboardStyleResId(theme);
    }

    @Override
    public int getKeyboardIconsStyleResId(@NonNull KeyboardTheme theme) {
      return host.getKeyboardIconsStyleResId(theme);
    }

    @NonNull
    @Override
    public KeyboardTheme getFallbackTheme() {
      return host.getFallbackKeyboardTheme();
    }

    @NonNull
    @Override
    public int[] getActionKeyTypes() {
      return KeyTypeAttributes.ACTION_KEY_TYPES;
    }

    @Override
    public boolean setValueFromTheme(
        TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
      return host.setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
    }

    @Override
    public boolean setKeyIconValueFromTheme(
        KeyboardTheme theme,
        TypedArray remoteTypedArray,
        int localAttrId,
        int remoteTypedArrayIndex) {
      return host.setKeyIconValueFromTheme(
          theme, remoteTypedArray, localAttrId, remoteTypedArrayIndex);
    }

    @Override
    public void setBackground(Drawable background) {
      host.setBackground(background);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
      host.setPadding(left, top, right, bottom);
    }

    @Override
    public int getWidth() {
      return host.getWidth();
    }

    @NonNull
    @Override
    public Resources getResources() {
      return host.getResources();
    }

    @Override
    public void onKeyDrawableProviderReady(
        int keyTypeFunctionAttrId,
        int keyActionAttrId,
        int keyActionTypeDoneAttrId,
        int keyActionTypeSearchAttrId,
        int keyActionTypeGoAttrId) {
      host.onKeyDrawableProviderReady(
          keyTypeFunctionAttrId,
          keyActionAttrId,
          keyActionTypeDoneAttrId,
          keyActionTypeSearchAttrId,
          keyActionTypeGoAttrId);
    }

    @Override
    public void onKeyboardDimensSet(int availableWidth) {
      host.setKeyboardMaxWidth(availableWidth);
    }
  }
}
