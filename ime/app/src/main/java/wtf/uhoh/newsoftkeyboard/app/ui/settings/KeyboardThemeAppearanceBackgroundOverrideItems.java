package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeAppearanceBackgroundOverrideItems {

  @NonNull
  static List<KeyboardThemeAppearanceOverrideItem> build(
      @NonNull Context context,
      @NonNull String presetId,
      @Nullable KeyboardWallpaperOverrideStore store) {
    if (store == null) return Collections.emptyList();

    final List<KeyboardThemeAppearanceOverrideItem> out = new ArrayList<>();

    if (store.hasWallpaper(presetId) || store.isWallpaperInvalid(presetId)) {
      final boolean invalid = store.isWallpaperInvalid(presetId);
      final CharSequence summary =
          invalid
              ? context.getString(
                  R.string.keyboard_theme_appearance_readability_status_invalid_photo_summary)
              : null;
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_pick_title),
              summary,
              "keyboard_theme_wallpaper_customization_pick",
              () -> store.clearWallpaperFile(presetId)));
    }

    final int rotation = store.getWallpaperRotationDegrees(presetId);
    if (rotation != 0) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_rotate_title),
              rotation + "\u00b0",
              "keyboard_theme_wallpaper_customization_rotate",
              () -> store.setWallpaperRotationDegrees(presetId, 0)));
    }

    final int scaleMode = store.getWallpaperScaleMode(presetId);
    if (scaleMode != KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_scale_title),
              wallpaperScaleModeLabel(context, scaleMode),
              "keyboard_theme_wallpaper_customization_scale_mode",
              () ->
                  store.setWallpaperScaleMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP)));
    }

    final int anchor = store.getWallpaperAnchor(presetId);
    if (anchor != KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_title),
              wallpaperAnchorLabel(context, anchor),
              "keyboard_theme_wallpaper_customization_anchor",
              () ->
                  store.setWallpaperAnchor(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER)));
    }

    final int saturation = store.getSaturationPercent(presetId);
    if (saturation != 100) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_saturation_title),
              saturation + "%",
              "keyboard_theme_wallpaper_customization_saturation",
              () -> store.setSaturationPercent(presetId, 100)));
    }

    final int contrast = store.getContrastPercent(presetId);
    if (contrast != 100) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_contrast_title),
              contrast + "%",
              "keyboard_theme_wallpaper_customization_contrast",
              () -> store.setContrastPercent(presetId, 100)));
    }

    final int brightness = store.getBrightnessPercent(presetId);
    if (brightness != 100) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_brightness_title),
              brightness + "%",
              "keyboard_theme_wallpaper_customization_brightness",
              () -> store.setBrightnessPercent(presetId, 100)));
    }

    final int temperature = store.getTemperaturePercent(presetId);
    if (temperature != 100) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_temperature_title),
              temperature + "%",
              "keyboard_theme_wallpaper_customization_temperature",
              () -> store.setTemperaturePercent(presetId, 100)));
    }

    if (store.getDimPercent(presetId) > 0) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_dim_title),
              store.getDimPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_background_layer_stack",
              () -> store.setDimPercent(presetId, 0)));
    }

    if (store.getGradientPercent(presetId) > 0) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_gradient_title),
              store.getGradientPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_background_layer_stack",
              () -> store.setGradientPercent(presetId, 0)));
    }

    if (store.getVignettePercent(presetId) > 0) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_vignette_title),
              store.getVignettePercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_background_layer_stack",
              () -> store.setVignettePercent(presetId, 0)));
    }

    if (store.getGrainPercent(presetId) > 0) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_grain_title),
              store.getGrainPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_background_layer_stack",
              () -> store.setGrainPercent(presetId, 0)));
    }

    if (store.hasBackgroundLayerStackOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_background_layer_stack_title),
              KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.describeLayerStack(
                  context, store.getBackgroundLayerStack(presetId)),
              "keyboard_theme_wallpaper_customization_background_layer_stack",
              () -> store.clearBackgroundLayerStack(presetId)));
    }

    if (store.hasBackgroundLayerOrderOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_background_layer_order_title),
              null,
              "keyboard_theme_wallpaper_customization_background_layer_stack",
              () -> store.clearBackgroundLayerOrder(presetId)));
    }

    final int mode = store.getWallpaperMode(presetId);
    if (mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_mode_title),
              wallpaperModeLabel(context, mode),
              "keyboard_theme_wallpaper_customization_mode",
              () ->
                  store.setWallpaperMode(
                      presetId,
                      KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY)));
    }

    final int baseKeyAlpha = store.getKeyAlphaPercent(presetId);
    if (baseKeyAlpha != KeyboardWallpaperOverrideConstants.DEFAULT_KEY_ALPHA_PERCENT) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_key_opacity_title),
              baseKeyAlpha + "%",
              "keyboard_theme_wallpaper_customization_key_opacity",
              () ->
                  store.setKeyAlphaPercent(
                      presetId, KeyboardWallpaperOverrideConstants.DEFAULT_KEY_ALPHA_PERCENT)));
    }

    if (store.hasSpecialKeyAlphaPercentOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_special_key_opacity_title),
              store.getSpecialKeyAlphaPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_special_key_opacity",
              () -> store.clearSpecialKeyAlphaPercent(presetId)));
    }

    if (store.hasModifierKeyAlphaPercentOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_modifier_key_opacity_title),
              store.getModifierKeyAlphaPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_modifier_key_opacity",
              () -> store.clearModifierKeyAlphaPercent(presetId)));
    }

    if (store.hasEnterKeyAlphaPercentOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_enter_key_opacity_title),
              store.getEnterKeyAlphaPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_enter_key_opacity",
              () -> store.clearEnterKeyAlphaPercent(presetId)));
    }

    if (store.hasSpacebarAlphaPercentOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_spacebar_opacity_title),
              store.getSpacebarAlphaPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_spacebar_opacity",
              () -> store.clearSpacebarAlphaPercent(presetId)));
    }

    final int keyBlendMode = store.getKeyBlendMode(presetId);
    if (keyBlendMode != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_blend_mode_title),
              blendModeLabel(context, keyBlendMode),
              "keyboard_theme_wallpaper_customization_key_blend_mode",
              () ->
                  store.setKeyBlendMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL)));
    }

    if (store.hasKeyColorWashColorOverride(presetId)) {
      final Integer color = store.getKeyColorWashColor(presetId);
      if (color != null) {
        out.add(
            new KeyboardThemeAppearanceOverrideItem(
                context.getString(
                    R.string.keyboard_theme_wallpaper_customization_key_color_wash_color_title),
                KeyboardThemeCustomizationColorUiUtil.formatColor(color),
                "keyboard_theme_wallpaper_customization_key_layer_stack",
                () -> store.clearKeyColorWashColor(presetId)));
      }
    }

    final int colorWashBlend = store.getKeyColorWashBlendMode(presetId);
    if (colorWashBlend != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_color_wash_blend_mode_title),
              blendModeLabel(context, colorWashBlend),
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () ->
                  store.setKeyColorWashBlendMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL)));
    }

    if (store.getKeyHighlightPercent(presetId) > 0) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_highlight_title),
              store.getKeyHighlightPercent(presetId) + "%",
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () -> store.setKeyHighlightPercent(presetId, 0)));
    }

    final int highlightBlend = store.getKeyHighlightBlendMode(presetId);
    if (highlightBlend != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_highlight_blend_mode_title),
              blendModeLabel(context, highlightBlend),
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () ->
                  store.setKeyHighlightBlendMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL)));
    }

    final int keyGradientBlend = store.getKeyGradientBlendMode(presetId);
    if (keyGradientBlend != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_gradient_blend_mode_title),
              blendModeLabel(context, keyGradientBlend),
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () ->
                  store.setKeyGradientBlendMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL)));
    }

    final int keyVignetteBlend = store.getKeyVignetteBlendMode(presetId);
    if (keyVignetteBlend != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_vignette_blend_mode_title),
              blendModeLabel(context, keyVignetteBlend),
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () ->
                  store.setKeyVignetteBlendMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL)));
    }

    final int keyGrainBlend = store.getKeyGrainBlendMode(presetId);
    if (keyGrainBlend != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_grain_blend_mode_title),
              blendModeLabel(context, keyGrainBlend),
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () ->
                  store.setKeyGrainBlendMode(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL)));
    }

    if (store.hasKeyLayerStackOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_layer_stack_title),
              KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.describeLayerStack(
                  context, store.getKeyLayerStack(presetId)),
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () -> store.clearKeyLayerStack(presetId)));
    }

    if (store.hasKeyLayerOrderOverride(presetId)) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_key_layer_order_title),
              null,
              "keyboard_theme_wallpaper_customization_key_layer_stack",
              () -> store.clearKeyLayerOrder(presetId)));
    }

    final boolean matchKeyShape = store.isMatchKeyShapeEnabled(presetId);
    if (matchKeyShape) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_wallpaper_customization_match_key_shape_title),
              context.getString(android.R.string.yes),
              "keyboard_theme_wallpaper_customization_match_key_shape",
              () -> store.setMatchKeyShapeEnabled(presetId, false)));
    }

    final int quality = store.getWallpaperQuality(presetId);
    if (quality != KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_wallpaper_customization_quality_title),
              wallpaperQualityLabel(context, quality),
              "keyboard_theme_wallpaper_customization_quality",
              () ->
                  store.setWallpaperQuality(
                      presetId, KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED)));
    }

    return out;
  }

  @NonNull
  private static String wallpaperScaleModeLabel(@NonNull Context context, int value) {
    return switch (value) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_crop);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_fit);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_STRETCH ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_stretch);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_tile);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_MIRROR ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_mirror);
      default -> String.valueOf(value);
    };
  }

  @NonNull
  private static String wallpaperAnchorLabel(@NonNull Context context, int value) {
    return switch (value) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_LEFT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_top_left);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_top);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_RIGHT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_top_right);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_LEFT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_left);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_center);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_RIGHT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_right);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_LEFT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom_left);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom_right);
      default -> String.valueOf(value);
    };
  }

  @NonNull
  private static String wallpaperModeLabel(@NonNull Context context, int value) {
    return switch (value) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_mode_background_only);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TINT ->
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_mode_background_key_tint);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE ->
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_mode_background_key_texture);
      default -> String.valueOf(value);
    };
  }

  @NonNull
  private static String wallpaperQualityLabel(@NonNull Context context, int value) {
    return switch (value) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_LOW ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_quality_low);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_quality_balanced);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_quality_high);
      default -> String.valueOf(value);
    };
  }

  @NonNull
  private static String blendModeLabel(@NonNull Context context, int value) {
    return switch (value) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_normal);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_MULTIPLY ->
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_key_blend_mode_multiply);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SCREEN ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_screen);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_OVERLAY ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_overlay);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SOFT_LIGHT ->
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_key_blend_mode_soft_light);
      default -> String.valueOf(value);
    };
  }
}
