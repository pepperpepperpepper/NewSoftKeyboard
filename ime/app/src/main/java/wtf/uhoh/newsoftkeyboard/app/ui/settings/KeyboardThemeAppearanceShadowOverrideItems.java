package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeAppearanceShadowOverrideItems {

  @NonNull
  static List<KeyboardThemeAppearanceOverrideItem> build(
      @NonNull Context context,
      @NonNull String presetId,
      @Nullable KeyboardThemeUserOverridesStore store) {
    if (store == null) return Collections.emptyList();
    final List<KeyboardThemeAppearanceOverrideItem> out = new ArrayList<>();

    final Integer tokenSecondaryTextShadowColor = store.getTokenSecondaryTextShadowColor(presetId);
    final Integer tokenSecondaryTextShadowRadiusDp =
        store.getTokenSecondaryTextShadowRadiusDp(presetId);
    final Integer tokenSecondaryTextShadowOffsetXDp =
        store.getTokenSecondaryTextShadowOffsetXDp(presetId);
    final Integer tokenSecondaryTextShadowOffsetYDp =
        store.getTokenSecondaryTextShadowOffsetYDp(presetId);
    if (tokenSecondaryTextShadowColor != null
        || tokenSecondaryTextShadowRadiusDp != null
        || tokenSecondaryTextShadowOffsetXDp != null
        || tokenSecondaryTextShadowOffsetYDp != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_text_shadow_title)
                  + " ("
                  + context.getString(
                      R.string.keyboard_theme_appearance_typography_secondary_token_entry)
                  + ")",
              describeTextShadow(
                  tokenSecondaryTextShadowColor,
                  tokenSecondaryTextShadowRadiusDp,
                  tokenSecondaryTextShadowOffsetXDp,
                  tokenSecondaryTextShadowOffsetYDp),
              "keyboard_theme_token_secondary_text_shadow_color",
              () -> {
                store.clearTokenSecondaryTextShadowColor(presetId);
                store.clearTokenSecondaryTextShadowRadiusDp(presetId);
                store.clearTokenSecondaryTextShadowOffsetXDp(presetId);
                store.clearTokenSecondaryTextShadowOffsetYDp(presetId);
              }));
    }

    addTextShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(R.string.keyboard_theme_appearance_shadow_target_all_keys_entry),
        store.isKeyTextShadowUseTokenSecondary(presetId),
        store.getKeyTextShadowColor(presetId),
        store.getKeyTextShadowRadiusDp(presetId),
        store.getKeyTextShadowOffsetXDp(presetId),
        store.getKeyTextShadowOffsetYDp(presetId),
        () -> store.clearTextShadowOverrides(presetId));
    addTextShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_special_keys_label),
        store.isSpecialKeyTextShadowUseTokenSecondary(presetId),
        store.getSpecialKeyTextShadowColor(presetId),
        store.getSpecialKeyTextShadowRadiusDp(presetId),
        store.getSpecialKeyTextShadowOffsetXDp(presetId),
        store.getSpecialKeyTextShadowOffsetYDp(presetId),
        () -> store.clearSpecialKeyTextShadowOverrides(presetId));
    addTextShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_spacebar_label),
        store.isSpacebarKeyTextShadowUseTokenSecondary(presetId),
        store.getSpacebarKeyTextShadowColor(presetId),
        store.getSpacebarKeyTextShadowRadiusDp(presetId),
        store.getSpacebarKeyTextShadowOffsetXDp(presetId),
        store.getSpacebarKeyTextShadowOffsetYDp(presetId),
        () -> store.clearSpacebarKeyTextShadowOverrides(presetId));
    addTextShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_modifier_keys_label),
        store.isModifierKeyTextShadowUseTokenSecondary(presetId),
        store.getModifierKeyTextShadowColor(presetId),
        store.getModifierKeyTextShadowRadiusDp(presetId),
        store.getModifierKeyTextShadowOffsetXDp(presetId),
        store.getModifierKeyTextShadowOffsetYDp(presetId),
        () -> store.clearModifierKeyTextShadowOverrides(presetId));
    addTextShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_enter_key_label),
        store.isEnterKeyTextShadowUseTokenSecondary(presetId),
        store.getEnterKeyTextShadowColor(presetId),
        store.getEnterKeyTextShadowRadiusDp(presetId),
        store.getEnterKeyTextShadowOffsetXDp(presetId),
        store.getEnterKeyTextShadowOffsetYDp(presetId),
        () -> store.clearEnterKeyTextShadowOverrides(presetId));

    final Integer tokenSecondaryKeyShadowColor =
        store.getTokenSecondaryKeyBackgroundShadowColor(presetId);
    final Integer tokenSecondaryKeyShadowOffsetXDp =
        store.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId);
    final Integer tokenSecondaryKeyShadowOffsetYDp =
        store.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId);
    final Integer tokenSecondaryKeyShadowSpreadDp =
        store.getTokenSecondaryKeyBackgroundShadowSpreadDp(presetId);
    if (tokenSecondaryKeyShadowColor != null
        || tokenSecondaryKeyShadowOffsetXDp != null
        || tokenSecondaryKeyShadowOffsetYDp != null
        || tokenSecondaryKeyShadowSpreadDp != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_key_shadow_title)
                  + " ("
                  + context.getString(
                      R.string.keyboard_theme_appearance_typography_secondary_token_entry)
                  + ")",
              describeKeyShadow(
                  tokenSecondaryKeyShadowColor,
                  tokenSecondaryKeyShadowOffsetXDp,
                  tokenSecondaryKeyShadowOffsetYDp,
                  tokenSecondaryKeyShadowSpreadDp),
              "keyboard_theme_token_secondary_key_shadow_color",
              () -> {
                store.clearTokenSecondaryKeyBackgroundShadowColor(presetId);
                store.clearTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId);
                store.clearTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId);
                store.clearTokenSecondaryKeyBackgroundShadowSpreadDp(presetId);
              }));
    }

    addKeyShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(R.string.keyboard_theme_appearance_shadow_target_all_keys_entry),
        store.isKeyBackgroundShadowUseTokenSecondary(presetId),
        store.getKeyBackgroundShadowColor(presetId),
        store.getKeyBackgroundShadowOffsetXDp(presetId),
        store.getKeyBackgroundShadowOffsetYDp(presetId),
        store.getKeyBackgroundShadowSpreadDp(presetId),
        () -> store.clearKeyBackgroundShadowOverrides(presetId));
    addKeyShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_special_keys_label),
        store.isSpecialKeyBackgroundShadowUseTokenSecondary(presetId),
        store.getSpecialKeyBackgroundShadowColor(presetId),
        store.getSpecialKeyBackgroundShadowOffsetXDp(presetId),
        store.getSpecialKeyBackgroundShadowOffsetYDp(presetId),
        null,
        () -> store.clearSpecialKeyBackgroundShadowOverrides(presetId));
    addKeyShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_spacebar_label),
        store.isSpacebarKeyBackgroundShadowUseTokenSecondary(presetId),
        store.getSpacebarKeyBackgroundShadowColor(presetId),
        store.getSpacebarKeyBackgroundShadowOffsetXDp(presetId),
        store.getSpacebarKeyBackgroundShadowOffsetYDp(presetId),
        null,
        () -> store.clearSpacebarKeyBackgroundShadowOverrides(presetId));
    addKeyShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_modifier_keys_label),
        store.isModifierKeyBackgroundShadowUseTokenSecondary(presetId),
        store.getModifierKeyBackgroundShadowColor(presetId),
        store.getModifierKeyBackgroundShadowOffsetXDp(presetId),
        store.getModifierKeyBackgroundShadowOffsetYDp(presetId),
        null,
        () -> store.clearModifierKeyBackgroundShadowOverrides(presetId));
    addKeyShadowOverrideGroup(
        context,
        out,
        presetId,
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_enter_key_label),
        store.isEnterKeyBackgroundShadowUseTokenSecondary(presetId),
        store.getEnterKeyBackgroundShadowColor(presetId),
        store.getEnterKeyBackgroundShadowOffsetXDp(presetId),
        store.getEnterKeyBackgroundShadowOffsetYDp(presetId),
        null,
        () -> store.clearEnterKeyBackgroundShadowOverrides(presetId));

    return out;
  }

  private static void addTextShadowOverrideGroup(
      @NonNull Context context,
      @NonNull List<KeyboardThemeAppearanceOverrideItem> out,
      @NonNull String presetId,
      @NonNull String targetLabel,
      boolean usesSecondaryToken,
      @Nullable Integer color,
      @Nullable Integer radiusDp,
      @Nullable Integer offsetXDp,
      @Nullable Integer offsetYDp,
      @NonNull Runnable clearAction) {
    if (!usesSecondaryToken
        && color == null
        && radiusDp == null
        && offsetXDp == null
        && offsetYDp == null) return;
    final String description =
        usesSecondaryToken
            ? context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry)
            : describeTextShadow(color, radiusDp, offsetXDp, offsetYDp);
    out.add(
        new KeyboardThemeAppearanceOverrideItem(
            context.getString(R.string.keyboard_theme_appearance_text_shadow_title)
                + " ("
                + targetLabel
                + ")",
            description,
            "section:shadows",
            clearAction));
  }

  private static void addKeyShadowOverrideGroup(
      @NonNull Context context,
      @NonNull List<KeyboardThemeAppearanceOverrideItem> out,
      @NonNull String presetId,
      @NonNull String targetLabel,
      boolean usesSecondaryToken,
      @Nullable Integer color,
      @Nullable Integer offsetXDp,
      @Nullable Integer offsetYDp,
      @Nullable Integer spreadDp,
      @NonNull Runnable clearAction) {
    if (!usesSecondaryToken
        && color == null
        && offsetXDp == null
        && offsetYDp == null
        && spreadDp == null) return;
    final String description =
        usesSecondaryToken
            ? context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry)
            : describeKeyShadow(color, offsetXDp, offsetYDp, spreadDp);
    out.add(
        new KeyboardThemeAppearanceOverrideItem(
            context.getString(R.string.keyboard_theme_appearance_key_shadow_title)
                + " ("
                + targetLabel
                + ")",
            description,
            "section:shadows",
            clearAction));
  }

  @NonNull
  private static String describeTextShadow(
      @Nullable Integer color,
      @Nullable Integer radiusDp,
      @Nullable Integer offsetXDp,
      @Nullable Integer offsetYDp) {
    final StringBuilder sb = new StringBuilder();
    if (color != null) sb.append(KeyboardThemeCustomizationColorUiUtil.formatColor(color));
    if (radiusDp != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("r ").append(radiusDp).append("dp");
    }
    if (offsetXDp != null || offsetYDp != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("d(")
          .append(offsetXDp != null ? offsetXDp : 0)
          .append(", ")
          .append(offsetYDp != null ? offsetYDp : 0)
          .append(")dp");
    }
    return sb.toString();
  }

  @NonNull
  private static String describeKeyShadow(
      @Nullable Integer color,
      @Nullable Integer offsetXDp,
      @Nullable Integer offsetYDp,
      @Nullable Integer spreadDp) {
    final StringBuilder sb = new StringBuilder();
    if (color != null) sb.append(KeyboardThemeCustomizationColorUiUtil.formatColor(color));
    if (offsetXDp != null || offsetYDp != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("d(")
          .append(offsetXDp != null ? offsetXDp : 0)
          .append(", ")
          .append(offsetYDp != null ? offsetYDp : 0)
          .append(")dp");
    }
    if (spreadDp != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("s ").append(spreadDp).append("dp");
    }
    return sb.toString();
  }
}
