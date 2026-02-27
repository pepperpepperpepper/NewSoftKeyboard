package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import java.util.ArrayList;

final class KeyboardWallpaperLegacyLayerStackBuilder {

  private KeyboardWallpaperLegacyLayerStackBuilder() {}

  @NonNull
  static KeyboardWallpaperLayer[] buildLegacyBackgroundLayerStack(
      @NonNull KeyboardWallpaperOverrideBackgroundLayersPrefs prefs, @NonNull String themeId) {
    final int[] order = prefs.getBackgroundLayerOrder(themeId);
    final ArrayList<KeyboardWallpaperLayer> layers = new ArrayList<>();
    for (int layer : order) {
      switch (layer) {
        case KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_TINT:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_THEME_TINT,
                  true,
                  100,
                  prefs.getBackgroundTintBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_DIM:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_DIM,
                  prefs.getDimPercent(themeId) > 0,
                  prefs.getDimPercent(themeId),
                  prefs.getBackgroundDimBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_GRADIENT:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_GRADIENT,
                  prefs.getGradientPercent(themeId) > 0,
                  prefs.getGradientPercent(themeId),
                  prefs.getBackgroundGradientBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_VIGNETTE:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_VIGNETTE,
                  prefs.getVignettePercent(themeId) > 0,
                  prefs.getVignettePercent(themeId),
                  prefs.getBackgroundVignetteBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_GRAIN:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_GRAIN,
                  prefs.getGrainPercent(themeId) > 0,
                  prefs.getGrainPercent(themeId),
                  prefs.getBackgroundGrainBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        default:
          break;
      }
    }
    return layers.toArray(new KeyboardWallpaperLayer[0]);
  }

  @NonNull
  static KeyboardWallpaperLayer[] buildLegacyKeyLayerStack(
      @NonNull KeyboardWallpaperOverrideKeyLayersPrefs prefs, @NonNull String themeId) {
    final int[] order = prefs.getKeyLayerOrder(themeId);
    final ArrayList<KeyboardWallpaperLayer> layers = new ArrayList<>();
    for (int layer : order) {
      switch (layer) {
        case KeyboardWallpaperOverrideConstants.KEY_LAYER_COLOR_WASH:
          final Integer color = prefs.getKeyColorWashColor(themeId);
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_COLOR_WASH,
                  color != null && ((color >>> 24) != 0),
                  100,
                  prefs.getKeyColorWashBlendMode(themeId),
                  color,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.KEY_LAYER_HIGHLIGHT:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_HIGHLIGHT,
                  prefs.getKeyHighlightPercent(themeId) > 0,
                  prefs.getKeyHighlightPercent(themeId),
                  prefs.getKeyHighlightBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.KEY_LAYER_GRADIENT:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_GRADIENT,
                  prefs.getGradientPercent(themeId) > 0,
                  prefs.getGradientPercent(themeId),
                  prefs.getKeyGradientBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.KEY_LAYER_VIGNETTE:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_VIGNETTE,
                  prefs.getVignettePercent(themeId) > 0,
                  prefs.getVignettePercent(themeId),
                  prefs.getKeyVignetteBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        case KeyboardWallpaperOverrideConstants.KEY_LAYER_GRAIN:
          layers.add(
              new KeyboardWallpaperLayer(
                  KeyboardWallpaperLayer.TYPE_GRAIN,
                  prefs.getGrainPercent(themeId) > 0,
                  prefs.getGrainPercent(themeId),
                  prefs.getKeyGrainBlendMode(themeId),
                  null,
                  null,
                  KeyboardWallpaperLayer.DIRECTION_VERTICAL,
                  KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
                  null));
          break;
        default:
          break;
      }
    }
    return layers.toArray(new KeyboardWallpaperLayer[0]);
  }
}
