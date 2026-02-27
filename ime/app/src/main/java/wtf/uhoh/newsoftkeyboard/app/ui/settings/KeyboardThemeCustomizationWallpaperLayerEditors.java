package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;

final class KeyboardThemeCustomizationWallpaperLayerEditors {

  @FunctionalInterface
  interface IntCallback {
    void onValue(int value);
  }

  @FunctionalInterface
  interface ColorPickedCallback {
    boolean onColorPicked(@Nullable Integer argbOrNull);
  }

  static void showAddLayerDialog(
      @NonNull Context context, boolean keyLayers, @NonNull IntCallback onLayerTypeSelected) {
    final int[] types =
        keyLayers
            ? new int[] {
              KeyboardWallpaperLayer.TYPE_COLOR_WASH,
              KeyboardWallpaperLayer.TYPE_HIGHLIGHT,
              KeyboardWallpaperLayer.TYPE_DIM,
              KeyboardWallpaperLayer.TYPE_SOLID_COLOR,
              KeyboardWallpaperLayer.TYPE_GRADIENT,
              KeyboardWallpaperLayer.TYPE_VIGNETTE,
              KeyboardWallpaperLayer.TYPE_GRAIN,
              KeyboardWallpaperLayer.TYPE_DOTS,
              KeyboardWallpaperLayer.TYPE_GRID,
              KeyboardWallpaperLayer.TYPE_STRIPES,
              KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES,
              KeyboardWallpaperLayer.TYPE_TRIANGLES,
              KeyboardWallpaperLayer.TYPE_HEX,
              KeyboardWallpaperLayer.TYPE_CHECKER
            }
            : new int[] {
              KeyboardWallpaperLayer.TYPE_THEME_TINT,
              KeyboardWallpaperLayer.TYPE_BLUR,
              KeyboardWallpaperLayer.TYPE_DIM,
              KeyboardWallpaperLayer.TYPE_SOLID_COLOR,
              KeyboardWallpaperLayer.TYPE_GRADIENT,
              KeyboardWallpaperLayer.TYPE_VIGNETTE,
              KeyboardWallpaperLayer.TYPE_GRAIN,
              KeyboardWallpaperLayer.TYPE_DOTS,
              KeyboardWallpaperLayer.TYPE_GRID,
              KeyboardWallpaperLayer.TYPE_STRIPES,
              KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES,
              KeyboardWallpaperLayer.TYPE_TRIANGLES,
              KeyboardWallpaperLayer.TYPE_HEX,
              KeyboardWallpaperLayer.TYPE_CHECKER
            };

    final CharSequence[] labels = new CharSequence[types.length];
    for (int i = 0; i < types.length; i++) {
      labels[i] = layerLabel(context, types[i]);
    }
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.keyboard_theme_wallpaper_layer_order_add_layer)
        .setItems(
            labels,
            (d, which) -> {
              if (which < 0 || which >= types.length) return;
              onLayerTypeSelected.onValue(types[which]);
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  static void showBlendModeDialog(
      @NonNull Context context, @NonNull IntCallback onBlendModeSelected) {
    final int[] modes =
        new int[] {
          KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
          KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_MULTIPLY,
          KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SCREEN,
          KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_OVERLAY,
          KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SOFT_LIGHT
        };
    final CharSequence[] labels = new CharSequence[modes.length];
    for (int j = 0; j < modes.length; j++) {
      labels[j] = blendModeLabel(context, modes[j]);
    }
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.keyboard_theme_wallpaper_layer_stack_blend_mode_title)
        .setItems(
            labels,
            (d, which) -> {
              if (which < 0 || which >= modes.length) return;
              onBlendModeSelected.onValue(modes[which]);
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  static void showDirectionDialog(
      @NonNull Context context, int layerType, @NonNull IntCallback onDirectionSelected) {
    final int[] dirs =
        layerType == KeyboardWallpaperLayer.TYPE_STRIPES
            ? new int[] {
              KeyboardWallpaperLayer.DIRECTION_VERTICAL, KeyboardWallpaperLayer.DIRECTION_HORIZONTAL
            }
            : new int[] {
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE
            };
    final CharSequence[] labels = new CharSequence[dirs.length];
    for (int j = 0; j < dirs.length; j++) {
      labels[j] = directionLabel(context, dirs[j]);
    }
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.keyboard_theme_wallpaper_layer_stack_direction_title)
        .setItems(
            labels,
            (d, which) -> {
              if (which < 0 || which >= dirs.length) return;
              onDirectionSelected.onValue(dirs[which]);
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  static void showGradientStopsEditorDialog(
      @NonNull Context context,
      @NonNull ArrayList<KeyboardWallpaperLayer> stack,
      int index,
      @NonNull Runnable applyToStore,
      @NonNull Runnable rerender) {
    KeyboardThemeCustomizationWallpaperGradientStopsEditorDialog.show(
        context, stack, index, applyToStore, rerender);
  }

  static void showArgbColorPicker(
      @NonNull Context context,
      @NonNull String title,
      @Nullable Integer initialArgbOrNull,
      @NonNull ColorPickedCallback callback) {
    final EditTextPreference fakePref = new EditTextPreference(context);
    fakePref.setTitle(title);
    fakePref.setText(initialArgbOrNull != null ? formatColor(initialArgbOrNull) : "");
    fakePref.setOnPreferenceChangeListener(
        (pref, newValue) -> {
          final String raw = newValue == null ? "" : String.valueOf(newValue).trim();
          final Integer parsed;
          if (raw.isEmpty()) {
            parsed = null;
          } else {
            try {
              parsed = Color.parseColor(raw);
            } catch (IllegalArgumentException e) {
              return false;
            }
          }
          return callback.onColorPicked(parsed);
        });
    KeyboardThemeCustomizationArgbColorPickerDialog.show(context, fakePref);
  }

  @NonNull
  static KeyboardWallpaperLayer defaultLayerForType(int type) {
    final int clampedType = type;
    return switch (clampedType) {
      case KeyboardWallpaperLayer.TYPE_THEME_TINT ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_THEME_TINT,
              true,
              100,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_DIM ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DIM,
              true,
              20,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_SOLID_COLOR ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_SOLID_COLOR,
              true,
              20,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.BLACK,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_COLOR_WASH ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_COLOR_WASH,
              true,
              15,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.BLACK,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_HIGHLIGHT ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_HIGHLIGHT,
              true,
              20,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_VIGNETTE ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_VIGNETTE,
              true,
              15,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_GRAIN ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRAIN,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_DOTS ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DOTS,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_GRID ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRID,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_STRIPES ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_STRIPES,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_TRIANGLES ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_TRIANGLES,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_HEX ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_HEX,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_CHECKER ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_CHECKER,
              true,
              10,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              Color.WHITE,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_BLUR ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_BLUR,
              true,
              80,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      case KeyboardWallpaperLayer.TYPE_GRADIENT ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRADIENT,
              true,
              20,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
      default ->
          new KeyboardWallpaperLayer(
              KeyboardWallpaperLayer.TYPE_GRADIENT,
              true,
              20,
              KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL,
              null,
              null,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL,
              KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT,
              null);
    };
  }

  @NonNull
  static String blendModeLabel(@NonNull Context context, int blendMode) {
    return switch (KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode)) {
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
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_normal);
      default ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_normal);
    };
  }

  @NonNull
  static String directionLabel(@NonNull Context context, int direction) {
    return switch (direction) {
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_stack_direction_horizontal);
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE ->
          context.getString(
              R.string.keyboard_theme_wallpaper_layer_stack_direction_horizontal_reverse);
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_stack_direction_vertical);
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
          context.getString(
              R.string.keyboard_theme_wallpaper_layer_stack_direction_vertical_reverse);
      default ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_stack_direction_vertical);
    };
  }

  @NonNull
  static String layerLabel(@NonNull Context context, int layerType) {
    return switch (layerType) {
      case KeyboardWallpaperLayer.TYPE_THEME_TINT ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_background_tint_title);
      case KeyboardWallpaperLayer.TYPE_SOLID_COLOR ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_solid_color_title);
      case KeyboardWallpaperLayer.TYPE_DIM ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_dim_title);
      case KeyboardWallpaperLayer.TYPE_COLOR_WASH ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_key_color_wash_title);
      case KeyboardWallpaperLayer.TYPE_VIGNETTE ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_vignette_title);
      case KeyboardWallpaperLayer.TYPE_HIGHLIGHT ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_key_highlight_title);
      case KeyboardWallpaperLayer.TYPE_GRAIN ->
          context.getString(R.string.keyboard_theme_wallpaper_customization_grain_title);
      case KeyboardWallpaperLayer.TYPE_DOTS ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_dots_title);
      case KeyboardWallpaperLayer.TYPE_GRID ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_grid_title);
      case KeyboardWallpaperLayer.TYPE_STRIPES ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_stripes_title);
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_diagonal_stripes_title);
      case KeyboardWallpaperLayer.TYPE_TRIANGLES ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_triangles_title);
      case KeyboardWallpaperLayer.TYPE_HEX ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_hex_title);
      case KeyboardWallpaperLayer.TYPE_BLUR ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_blur_title);
      case KeyboardWallpaperLayer.TYPE_CHECKER ->
          context.getString(R.string.keyboard_theme_wallpaper_layer_checker_title);
      default -> context.getString(R.string.keyboard_theme_wallpaper_customization_gradient_title);
    };
  }

  @Nullable
  static Integer resolveLayerColorForUi(@NonNull KeyboardWallpaperLayer layer) {
    final Integer argb = layer.argb();
    return switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_GRADIENT -> {
        final List<KeyboardWallpaperLayer.GradientStop> stops = layer.gradientStops();
        if (stops != null && stops.size() >= 2) {
          final KeyboardWallpaperLayer.GradientStop last = stops.get(stops.size() - 1);
          if (last != null) yield last.argb();
        }
        yield argb != null ? argb : Color.BLACK;
      }
      case KeyboardWallpaperLayer.TYPE_VIGNETTE -> argb != null ? argb : Color.BLACK;
      case KeyboardWallpaperLayer.TYPE_HIGHLIGHT -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_DOTS -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_GRID -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_STRIPES -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_TRIANGLES -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_HEX -> argb != null ? argb : Color.WHITE;
      case KeyboardWallpaperLayer.TYPE_CHECKER -> {
        final Integer argb2 = layer.argb2();
        yield argb != null ? argb : (argb2 != null ? argb2 : Color.WHITE);
      }
      case KeyboardWallpaperLayer.TYPE_SOLID_COLOR, KeyboardWallpaperLayer.TYPE_COLOR_WASH -> argb;
      default -> argb;
    };
  }

  @NonNull
  static Integer resolveGradientStartColorForUi(@NonNull KeyboardWallpaperLayer layer) {
    final List<KeyboardWallpaperLayer.GradientStop> stops = layer.gradientStops();
    if (stops != null && stops.size() >= 2) {
      final KeyboardWallpaperLayer.GradientStop first = stops.get(0);
      if (first != null) return first.argb();
    }
    final Integer start = layer.argb2();
    if (start != null) return start;
    final Integer end = layer.argb();
    if (end != null) return end & 0x00FF_FFFF;
    return Color.TRANSPARENT;
  }

  @Nullable
  static List<KeyboardWallpaperLayer.GradientStop> updateGradientStopsStartColor(
      @NonNull KeyboardWallpaperLayer layer, @Nullable Integer startArgbOrNull) {
    final List<KeyboardWallpaperLayer.GradientStop> stops = layer.gradientStops();
    if (stops == null || stops.size() < 2) return stops;

    final Integer resolvedEndOrNull = resolveLayerColorForUi(layer);
    final int endColor = resolvedEndOrNull != null ? resolvedEndOrNull : Color.BLACK;
    final int resolvedStart = startArgbOrNull != null ? startArgbOrNull : (endColor & 0x00FF_FFFF);
    final ArrayList<KeyboardWallpaperLayer.GradientStop> out = new ArrayList<>(stops.size());
    for (int i = 0; i < stops.size(); i++) {
      final KeyboardWallpaperLayer.GradientStop stop = stops.get(i);
      if (stop == null) continue;
      if (i == 0) {
        out.add(new KeyboardWallpaperLayer.GradientStop(stop.positionPercent(), resolvedStart));
      } else {
        out.add(stop);
      }
    }
    if (out.size() < 2) return null;
    return List.copyOf(out);
  }

  @Nullable
  static List<KeyboardWallpaperLayer.GradientStop> updateGradientStopsEndColor(
      @NonNull KeyboardWallpaperLayer layer, @Nullable Integer endArgbOrNull) {
    final List<KeyboardWallpaperLayer.GradientStop> stops = layer.gradientStops();
    if (stops == null || stops.size() < 2) return stops;

    final int resolvedEnd = endArgbOrNull != null ? endArgbOrNull : Color.BLACK;
    final ArrayList<KeyboardWallpaperLayer.GradientStop> out = new ArrayList<>(stops.size());
    for (int i = 0; i < stops.size(); i++) {
      final KeyboardWallpaperLayer.GradientStop stop = stops.get(i);
      if (stop == null) continue;
      if (i == stops.size() - 1) {
        out.add(new KeyboardWallpaperLayer.GradientStop(stop.positionPercent(), resolvedEnd));
      } else {
        out.add(stop);
      }
    }
    if (out.size() < 2) return null;
    return List.copyOf(out);
  }

  @NonNull
  static String formatColor(int argb) {
    final int alpha = (argb >>> 24) & 0xFF;
    if (alpha == 0xFF) {
      return String.format("#%06X", argb & 0x00FF_FFFF);
    }
    return String.format("#%08X", argb);
  }
}
