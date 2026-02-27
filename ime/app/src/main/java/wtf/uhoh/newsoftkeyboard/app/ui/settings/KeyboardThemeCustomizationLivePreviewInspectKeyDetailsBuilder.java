package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationLivePreviewInspectKeyDetailsBuilder {

  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @Nullable private final KeyboardWallpaperOverrideStore wallpaperStore;

  KeyboardThemeCustomizationLivePreviewInspectKeyDetailsBuilder(
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @Nullable KeyboardWallpaperOverrideStore wallpaperStore) {
    this.themeOverridesStore = themeOverridesStore;
    this.wallpaperStore = wallpaperStore;
  }

  @NonNull
  String buildStyleDetails(
      @NonNull Context context, @Nullable String presetId, @NonNull Keyboard.Key key) {
    if (presetId == null) return "";

    final int primaryCode = key.getPrimaryCode();
    final boolean isSpace = primaryCode == KeyCodes.SPACE;
    final boolean isEnter = primaryCode == KeyCodes.ENTER;
    final boolean isModifier = isModifierKey(primaryCode, key);
    final boolean isFunctional = (key instanceof KeyboardKey kk) && kk.isFunctional();
    final boolean isSpecial = !isSpace && !isEnter && !isModifier && isFunctional;

    final StringBuilder sb = new StringBuilder();

    final KeyboardThemeUserOverridesStore overrides = themeOverridesStore;
    if (overrides != null) {
      final Integer tokenPrimaryTextColor = overrides.getTokenPrimaryTextColor(presetId);
      final Integer tokenSecondaryTextColor = overrides.getTokenSecondaryTextColor(presetId);
      final Integer tokenAccentColor = overrides.getTokenAccentColor(presetId);
      final Integer tokenKeySurfaceColor = overrides.getTokenKeySurfaceColor(presetId);

      final Integer keyTextColor =
          overrides.getKeyTextColor(presetId) != null
              ? overrides.getKeyTextColor(presetId)
              : tokenPrimaryTextColor;
      final Integer specialKeyTextColor =
          overrides.getSpecialKeyTextColor(presetId) != null
              ? overrides.getSpecialKeyTextColor(presetId)
              : tokenAccentColor;

      final Integer keyBackgroundTint =
          overrides.getKeyBackgroundTint(presetId) != null
              ? overrides.getKeyBackgroundTint(presetId)
              : tokenKeySurfaceColor;
      final Integer specialKeyBackgroundTint =
          overrides.getSpecialKeyBackgroundTint(presetId) != null
              ? overrides.getSpecialKeyBackgroundTint(presetId)
              : tokenAccentColor;

      final Integer textColor =
          isSpace
              ? overrides.getSpacebarTextColor(presetId) != null
                  ? overrides.getSpacebarTextColor(presetId)
                  : tokenPrimaryTextColor
              : isEnter
                  ? overrides.getEnterKeyTextColor(presetId) != null
                      ? overrides.getEnterKeyTextColor(presetId)
                      : specialKeyTextColor
                  : isModifier
                      ? overrides.getModifierKeyTextColor(presetId) != null
                          ? overrides.getModifierKeyTextColor(presetId)
                          : specialKeyTextColor
                      : isSpecial ? specialKeyTextColor : keyTextColor;
      final Integer bgTint =
          isSpace
              ? overrides.getSpacebarBackgroundTint(presetId) != null
                  ? overrides.getSpacebarBackgroundTint(presetId)
                  : keyBackgroundTint
              : isEnter
                  ? overrides.getEnterKeyBackgroundTint(presetId) != null
                      ? overrides.getEnterKeyBackgroundTint(presetId)
                      : specialKeyBackgroundTint
                  : isModifier
                      ? overrides.getModifierKeyBackgroundTint(presetId) != null
                          ? overrides.getModifierKeyBackgroundTint(presetId)
                          : specialKeyBackgroundTint
                      : isSpecial ? specialKeyBackgroundTint : keyBackgroundTint;

      final int textColorTitleRes =
          isSpace
              ? R.string.keyboard_theme_appearance_spacebar_text_color_title
              : isEnter
                  ? R.string.keyboard_theme_appearance_enter_key_text_color_title
                  : isModifier
                      ? R.string.keyboard_theme_appearance_modifier_key_text_color_title
                      : isSpecial
                          ? R.string.keyboard_theme_appearance_special_key_text_color_title
                          : R.string.keyboard_theme_appearance_key_text_color_title;
      final int bgTintTitleRes =
          isSpace
              ? R.string.keyboard_theme_appearance_spacebar_background_tint_title
              : isEnter
                  ? R.string.keyboard_theme_appearance_enter_key_background_tint_title
                  : isModifier
                      ? R.string.keyboard_theme_appearance_modifier_key_background_tint_title
                      : isSpecial
                          ? R.string.keyboard_theme_appearance_special_key_background_tint_title
                          : R.string.keyboard_theme_appearance_key_background_tint_title;

      appendInspectLine(
          sb,
          context.getString(textColorTitleRes),
          textColor != null ? KeyboardThemeCustomizationColorUiUtil.formatColor(textColor) : null);
      appendInspectLine(
          sb,
          context.getString(bgTintTitleRes),
          bgTint != null ? KeyboardThemeCustomizationColorUiUtil.formatColor(bgTint) : null);

      final Integer hintTextColor =
          overrides.getHintTextColor(presetId) != null
              ? overrides.getHintTextColor(presetId)
              : tokenSecondaryTextColor;
      appendInspectLine(
          sb,
          context.getString(R.string.keyboard_theme_appearance_hint_text_color_title),
          hintTextColor != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(hintTextColor)
              : null);

      final Integer keyBgOpacity = overrides.getKeyBackgroundOpacityPercent(presetId);
      if (keyBgOpacity != null) {
        appendInspectLine(
            sb,
            context.getString(R.string.keyboard_theme_appearance_key_background_opacity_title),
            keyBgOpacity + "%");
      }

      final Integer keyboardBgOpacity = overrides.getKeyboardBackgroundOpacityPercent(presetId);
      if (keyboardBgOpacity != null) {
        appendInspectLine(
            sb,
            context.getString(R.string.keyboard_theme_appearance_keyboard_background_opacity_title),
            keyboardBgOpacity + "%");
      }

      final String keyFontFamily = overrides.getKeyFontFamily(presetId);
      if (keyFontFamily != null) {
        appendInspectLine(
            sb,
            context.getString(R.string.keyboard_theme_appearance_key_font_family_title),
            fontFamilyLabel(context, keyFontFamily));
      }
      final Integer keyFontStyle = overrides.getKeyFontStyle(presetId);
      if (keyFontStyle != null) {
        appendInspectLine(
            sb,
            context.getString(R.string.keyboard_theme_appearance_key_font_style_title),
            fontStyleLabel(context, keyFontStyle));
      }

      final Integer keyLabelSize = overrides.getKeyLabelTextSizePercent(presetId);
      if (keyLabelSize != null) {
        appendInspectLine(
            sb,
            context.getString(R.string.keyboard_theme_appearance_key_label_text_size_title),
            keyLabelSize + "%");
      }

      final var textShadow =
          resolveTextShadowForKeyType(overrides, presetId, isSpace, isEnter, isModifier, isSpecial);
      if (!textShadow.isEmpty()) {
        appendInspectLine(
            sb,
            context.getString(R.string.keyboard_theme_appearance_text_shadow_title),
            textShadow);
      }

      final var keyShadow =
          resolveKeyShadowForKeyType(overrides, presetId, isSpace, isEnter, isModifier, isSpecial);
      if (!keyShadow.isEmpty()) {
        appendInspectLine(
            sb, context.getString(R.string.keyboard_theme_appearance_key_shadow_title), keyShadow);
      }
    }

    final KeyboardWallpaperOverrideStore wallpaper = wallpaperStore;
    if (wallpaper != null
        && wallpaper.hasWallpaper(presetId)
        && !wallpaper.isWallpaperInvalid(presetId)
        && wallpaper.getWallpaperMode(presetId)
            != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY) {
      final int baseAlpha = wallpaper.getKeyAlphaPercent(presetId);
      final int alpha;
      final int titleRes;
      if (isSpace && wallpaper.hasSpacebarAlphaPercentOverride(presetId)) {
        alpha = wallpaper.getSpacebarAlphaPercent(presetId);
        titleRes = R.string.keyboard_theme_wallpaper_customization_spacebar_opacity_title;
      } else if (isEnter && wallpaper.hasEnterKeyAlphaPercentOverride(presetId)) {
        alpha = wallpaper.getEnterKeyAlphaPercent(presetId);
        titleRes = R.string.keyboard_theme_wallpaper_customization_enter_key_opacity_title;
      } else if (isModifier && wallpaper.hasModifierKeyAlphaPercentOverride(presetId)) {
        alpha = wallpaper.getModifierKeyAlphaPercent(presetId);
        titleRes = R.string.keyboard_theme_wallpaper_customization_modifier_key_opacity_title;
      } else if (isSpecial && wallpaper.hasSpecialKeyAlphaPercentOverride(presetId)) {
        alpha = wallpaper.getSpecialKeyAlphaPercent(presetId);
        titleRes = R.string.keyboard_theme_wallpaper_customization_special_key_opacity_title;
      } else {
        alpha = baseAlpha;
        titleRes = R.string.keyboard_theme_wallpaper_customization_key_opacity_title;
      }
      appendInspectLine(sb, context.getString(titleRes), alpha + "%");
    }

    return sb.toString();
  }

  private static void appendInspectLine(
      @NonNull StringBuilder sb, @NonNull String title, @Nullable String value) {
    if (value == null || value.isEmpty()) return;
    if (sb.length() > 0) sb.append('\n');
    sb.append(title).append(": ").append(value);
  }

  private static boolean isModifierKey(int primaryCode, @NonNull Keyboard.Key key) {
    if (key.modifier) return true;
    return switch (primaryCode) {
      case KeyCodes.SHIFT,
          KeyCodes.SHIFT_LOCK,
          KeyCodes.ALT,
          KeyCodes.ALT_MODIFIER,
          KeyCodes.CTRL,
          KeyCodes.CTRL_LOCK,
          KeyCodes.FUNCTION ->
          true;
      default -> false;
    };
  }

  @NonNull
  private static String resolveTextShadowForKeyType(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String presetId,
      boolean isSpace,
      boolean isEnter,
      boolean isModifier,
      boolean isSpecial) {
    final boolean baseUsesToken = store.isKeyTextShadowUseTokenSecondary(presetId);
    final Integer baseColor =
        baseUsesToken
            ? store.getTokenSecondaryTextShadowColor(presetId)
            : store.getKeyTextShadowColor(presetId);
    final Integer baseRadius =
        baseUsesToken
            ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
            : store.getKeyTextShadowRadiusDp(presetId);
    final Integer baseDx =
        baseUsesToken
            ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
            : store.getKeyTextShadowOffsetXDp(presetId);
    final Integer baseDy =
        baseUsesToken
            ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
            : store.getKeyTextShadowOffsetYDp(presetId);

    Integer overrideColor = null;
    Integer overrideRadius = null;
    Integer overrideDx = null;
    Integer overrideDy = null;

    if (isSpace) {
      final boolean usesToken = store.isSpacebarKeyTextShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryTextShadowColor(presetId)
              : store.getSpacebarKeyTextShadowColor(presetId);
      overrideRadius =
          usesToken
              ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
              : store.getSpacebarKeyTextShadowRadiusDp(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
              : store.getSpacebarKeyTextShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
              : store.getSpacebarKeyTextShadowOffsetYDp(presetId);
    } else if (isEnter) {
      final boolean usesToken = store.isEnterKeyTextShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryTextShadowColor(presetId)
              : store.getEnterKeyTextShadowColor(presetId);
      overrideRadius =
          usesToken
              ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
              : store.getEnterKeyTextShadowRadiusDp(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
              : store.getEnterKeyTextShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
              : store.getEnterKeyTextShadowOffsetYDp(presetId);
      if (overrideColor == null
          && overrideRadius == null
          && overrideDx == null
          && overrideDy == null) {
        final boolean specialUsesToken = store.isSpecialKeyTextShadowUseTokenSecondary(presetId);
        overrideColor =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowColor(presetId)
                : store.getSpecialKeyTextShadowColor(presetId);
        overrideRadius =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
                : store.getSpecialKeyTextShadowRadiusDp(presetId);
        overrideDx =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
                : store.getSpecialKeyTextShadowOffsetXDp(presetId);
        overrideDy =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
                : store.getSpecialKeyTextShadowOffsetYDp(presetId);
      }
    } else if (isModifier) {
      final boolean usesToken = store.isModifierKeyTextShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryTextShadowColor(presetId)
              : store.getModifierKeyTextShadowColor(presetId);
      overrideRadius =
          usesToken
              ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
              : store.getModifierKeyTextShadowRadiusDp(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
              : store.getModifierKeyTextShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
              : store.getModifierKeyTextShadowOffsetYDp(presetId);
      if (overrideColor == null
          && overrideRadius == null
          && overrideDx == null
          && overrideDy == null) {
        final boolean specialUsesToken = store.isSpecialKeyTextShadowUseTokenSecondary(presetId);
        overrideColor =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowColor(presetId)
                : store.getSpecialKeyTextShadowColor(presetId);
        overrideRadius =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
                : store.getSpecialKeyTextShadowRadiusDp(presetId);
        overrideDx =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
                : store.getSpecialKeyTextShadowOffsetXDp(presetId);
        overrideDy =
            specialUsesToken
                ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
                : store.getSpecialKeyTextShadowOffsetYDp(presetId);
      }
    } else if (isSpecial) {
      final boolean usesToken = store.isSpecialKeyTextShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryTextShadowColor(presetId)
              : store.getSpecialKeyTextShadowColor(presetId);
      overrideRadius =
          usesToken
              ? store.getTokenSecondaryTextShadowRadiusDp(presetId)
              : store.getSpecialKeyTextShadowRadiusDp(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetXDp(presetId)
              : store.getSpecialKeyTextShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryTextShadowOffsetYDp(presetId)
              : store.getSpecialKeyTextShadowOffsetYDp(presetId);
    }

    final Integer color = overrideColor != null ? overrideColor : baseColor;
    final Integer radius = overrideRadius != null ? overrideRadius : baseRadius;
    final Integer dx = overrideDx != null ? overrideDx : baseDx;
    final Integer dy = overrideDy != null ? overrideDy : baseDy;

    if (color == null && radius == null && dx == null && dy == null) return "";

    final StringBuilder sb = new StringBuilder();
    if (color != null) sb.append(KeyboardThemeCustomizationColorUiUtil.formatColor(color));
    if (radius != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("r ").append(radius).append("dp");
    }
    if (dx != null || dy != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("d(")
          .append(dx != null ? dx : 0)
          .append(", ")
          .append(dy != null ? dy : 0)
          .append(")dp");
    }
    return sb.toString();
  }

  @NonNull
  private static String resolveKeyShadowForKeyType(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String presetId,
      boolean isSpace,
      boolean isEnter,
      boolean isModifier,
      boolean isSpecial) {
    final boolean baseUsesToken = store.isKeyBackgroundShadowUseTokenSecondary(presetId);
    final Integer baseColor =
        baseUsesToken
            ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
            : store.getKeyBackgroundShadowColor(presetId);
    final Integer baseDx =
        baseUsesToken
            ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
            : store.getKeyBackgroundShadowOffsetXDp(presetId);
    final Integer baseDy =
        baseUsesToken
            ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
            : store.getKeyBackgroundShadowOffsetYDp(presetId);
    final Integer baseSpread =
        baseUsesToken
            ? store.getTokenSecondaryKeyBackgroundShadowSpreadDp(presetId)
            : store.getKeyBackgroundShadowSpreadDp(presetId);

    Integer overrideColor = null;
    Integer overrideDx = null;
    Integer overrideDy = null;

    if (isSpace) {
      final boolean usesToken = store.isSpacebarKeyBackgroundShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
              : store.getSpacebarKeyBackgroundShadowColor(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
              : store.getSpacebarKeyBackgroundShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
              : store.getSpacebarKeyBackgroundShadowOffsetYDp(presetId);
    } else if (isEnter) {
      final boolean usesToken = store.isEnterKeyBackgroundShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
              : store.getEnterKeyBackgroundShadowColor(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
              : store.getEnterKeyBackgroundShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
              : store.getEnterKeyBackgroundShadowOffsetYDp(presetId);
      if (overrideColor == null && overrideDx == null && overrideDy == null) {
        final boolean specialUsesToken =
            store.isSpecialKeyBackgroundShadowUseTokenSecondary(presetId);
        overrideColor =
            specialUsesToken
                ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
                : store.getSpecialKeyBackgroundShadowColor(presetId);
        overrideDx =
            specialUsesToken
                ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
                : store.getSpecialKeyBackgroundShadowOffsetXDp(presetId);
        overrideDy =
            specialUsesToken
                ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
                : store.getSpecialKeyBackgroundShadowOffsetYDp(presetId);
      }
    } else if (isModifier) {
      final boolean usesToken = store.isModifierKeyBackgroundShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
              : store.getModifierKeyBackgroundShadowColor(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
              : store.getModifierKeyBackgroundShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
              : store.getModifierKeyBackgroundShadowOffsetYDp(presetId);
      if (overrideColor == null && overrideDx == null && overrideDy == null) {
        final boolean specialUsesToken =
            store.isSpecialKeyBackgroundShadowUseTokenSecondary(presetId);
        overrideColor =
            specialUsesToken
                ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
                : store.getSpecialKeyBackgroundShadowColor(presetId);
        overrideDx =
            specialUsesToken
                ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
                : store.getSpecialKeyBackgroundShadowOffsetXDp(presetId);
        overrideDy =
            specialUsesToken
                ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
                : store.getSpecialKeyBackgroundShadowOffsetYDp(presetId);
      }
    } else if (isSpecial) {
      final boolean usesToken = store.isSpecialKeyBackgroundShadowUseTokenSecondary(presetId);
      overrideColor =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowColor(presetId)
              : store.getSpecialKeyBackgroundShadowColor(presetId);
      overrideDx =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId)
              : store.getSpecialKeyBackgroundShadowOffsetXDp(presetId);
      overrideDy =
          usesToken
              ? store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId)
              : store.getSpecialKeyBackgroundShadowOffsetYDp(presetId);
    }

    final Integer color = overrideColor != null ? overrideColor : baseColor;
    final Integer dx = overrideDx != null ? overrideDx : baseDx;
    final Integer dy = overrideDy != null ? overrideDy : baseDy;
    final Integer spread = baseSpread;

    if (color == null && dx == null && dy == null && spread == null) return "";

    final StringBuilder sb = new StringBuilder();
    if (color != null) sb.append(KeyboardThemeCustomizationColorUiUtil.formatColor(color));
    if (dx != null || dy != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("d(")
          .append(dx != null ? dx : 0)
          .append(", ")
          .append(dy != null ? dy : 0)
          .append(")dp");
    }
    if (spread != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("s ").append(spread).append("dp");
    }
    return sb.toString();
  }

  @NonNull
  private static String fontFamilyLabel(@NonNull Context context, @NonNull String id) {
    return switch (id) {
      case "default" ->
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry);
      case "sans" -> context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry);
      case "serif" -> context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry);
      case "monospace" ->
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry);
      case KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM ->
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry);
      default -> id;
    };
  }

  @NonNull
  private static String fontStyleLabel(@NonNull Context context, int style) {
    return switch (style) {
      case 0 -> context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry);
      case 1 -> context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry);
      case 2 -> context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry);
      case 3 -> context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry);
      default -> String.valueOf(style);
    };
  }
}
