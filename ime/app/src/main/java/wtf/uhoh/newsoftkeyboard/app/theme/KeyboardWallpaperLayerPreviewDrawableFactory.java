package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Creates cheap preview drawables for wallpaper layer-stack rows in Settings.
 *
 * <p>These previews are intentionally approximate: they are meant to make layer types obvious and
 * keep the editor responsive.
 */
public final class KeyboardWallpaperLayerPreviewDrawableFactory {

  private KeyboardWallpaperLayerPreviewDrawableFactory() {}

  @Nullable
  public static Drawable createPreviewDrawable(@NonNull KeyboardWallpaperLayer layer) {
    final int strengthPercent = KeyboardWallpaperRenderMath.clampPercent(layer.opacityPercent());
    final int alpha = KeyboardWallpaperRenderMath.alphaForPercent(strengthPercent);
    final int blendMode = KeyboardWallpaperOverrideConstants.normalizeBlendMode(layer.blendMode());

    return switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_THEME_TINT -> {
        final SolidOverlayDrawable tint = new SolidOverlayDrawable(Color.WHITE);
        tint.setBlendMode(blendMode);
        tint.setAlpha(layer.enabled() ? alpha : 0);
        yield tint;
      }
      case KeyboardWallpaperLayer.TYPE_DIM -> {
        final SolidOverlayDrawable dim = new SolidOverlayDrawable(Color.BLACK);
        dim.setBlendMode(blendMode);
        dim.setAlpha(layer.enabled() ? alpha : 0);
        yield dim;
      }
      case KeyboardWallpaperLayer.TYPE_SOLID_COLOR, KeyboardWallpaperLayer.TYPE_COLOR_WASH -> {
        final Integer argb = layer.argb();
        final SolidOverlayDrawable solid =
            new SolidOverlayDrawable(argb != null ? argb : Color.TRANSPARENT);
        solid.setBlendMode(blendMode);
        solid.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield solid;
      }
      case KeyboardWallpaperLayer.TYPE_GRADIENT -> {
        final GradientOverlayDrawable gradient = new GradientOverlayDrawable();
        gradient.setBlendMode(blendMode);
        gradient.setDirection(layer.direction());
        gradient.setStops(layer.gradientStops());
        gradient.setColors(layer.argb2(), layer.argb());
        gradient.setStrengthPercent(layer.enabled() ? strengthPercent : 0);
        yield gradient;
      }
      case KeyboardWallpaperLayer.TYPE_HIGHLIGHT -> {
        final Integer argb = layer.argb();
        final int endColor = argb != null ? argb : Color.WHITE;
        final int startColor = endColor & 0x00FF_FFFF;
        final GradientOverlayDrawable highlight = new GradientOverlayDrawable();
        highlight.setBlendMode(blendMode);
        highlight.setDirection(layer.direction());
        highlight.setColors(startColor, endColor);
        highlight.setStrengthPercent(layer.enabled() ? strengthPercent : 0);
        yield highlight;
      }
      case KeyboardWallpaperLayer.TYPE_VIGNETTE -> {
        final VignetteDrawable vignette = new VignetteDrawable();
        vignette.setBlendMode(blendMode);
        vignette.setColor(layer.argb());
        vignette.setStrengthPercent(layer.enabled() ? strengthPercent : 0);
        yield vignette;
      }
      case KeyboardWallpaperLayer.TYPE_GRAIN -> {
        final GrainDrawable grain = new GrainDrawable();
        grain.setBlendMode(blendMode);
        grain.setScalePercent(layer.scalePercent());
        grain.setStrengthPercent(layer.enabled() ? strengthPercent : 0);
        yield grain;
      }
      case KeyboardWallpaperLayer.TYPE_DOTS -> {
        final Integer argb = layer.argb();
        final DotsDrawable dots = new DotsDrawable();
        dots.setBlendMode(blendMode);
        dots.setScalePercent(layer.scalePercent());
        dots.setColor(argb);
        dots.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield dots;
      }
      case KeyboardWallpaperLayer.TYPE_GRID -> {
        final Integer argb = layer.argb();
        final GridDrawable grid = new GridDrawable();
        grid.setBlendMode(blendMode);
        grid.setScalePercent(layer.scalePercent());
        grid.setColor(argb);
        grid.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield grid;
      }
      case KeyboardWallpaperLayer.TYPE_STRIPES -> {
        final Integer argb = layer.argb();
        final StripesDrawable stripes = new StripesDrawable();
        stripes.setBlendMode(blendMode);
        stripes.setScalePercent(layer.scalePercent());
        stripes.setDirection(layer.direction());
        stripes.setColor(argb);
        stripes.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield stripes;
      }
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES -> {
        final Integer argb = layer.argb();
        final DiagonalStripesDrawable stripes = new DiagonalStripesDrawable();
        stripes.setBlendMode(blendMode);
        stripes.setScalePercent(layer.scalePercent());
        stripes.setColor(argb);
        stripes.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield stripes;
      }
      case KeyboardWallpaperLayer.TYPE_TRIANGLES -> {
        final Integer argb = layer.argb();
        final TrianglesDrawable triangles = new TrianglesDrawable();
        triangles.setBlendMode(blendMode);
        triangles.setScalePercent(layer.scalePercent());
        triangles.setColor(argb);
        triangles.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield triangles;
      }
      case KeyboardWallpaperLayer.TYPE_HEX -> {
        final Integer argb = layer.argb();
        final HexDrawable hex = new HexDrawable();
        hex.setBlendMode(blendMode);
        hex.setScalePercent(layer.scalePercent());
        hex.setColor(argb);
        hex.setAlpha(layer.enabled() && argb != null ? alpha : 0);
        yield hex;
      }
      case KeyboardWallpaperLayer.TYPE_CHECKER -> {
        final Integer argb = layer.argb();
        final Integer argb2 = layer.argb2();
        final CheckerDrawable checker = new CheckerDrawable();
        checker.setBlendMode(blendMode);
        checker.setScalePercent(layer.scalePercent());
        checker.setColors(argb, argb2);
        checker.setAlpha(layer.enabled() && (argb != null || argb2 != null) ? alpha : 0);
        yield checker;
      }
      case KeyboardWallpaperLayer.TYPE_BLUR -> {
        // Blur depends on the photo, so a solid placeholder keeps the UI cheap and predictable.
        final ColorDrawable blur = new ColorDrawable(0xFF909090);
        blur.setAlpha(layer.enabled() ? alpha : 0);
        yield blur;
      }
      default -> null;
    };
  }
}
