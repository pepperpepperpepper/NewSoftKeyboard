package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;

final class KeyboardThemeCustomizationWallpaperLayerRowAdvancedEditorsUi {

  @NonNull private final Context context;
  @NonNull private final ArrayList<KeyboardWallpaperLayer> stack;
  @NonNull private final ArrayList<Boolean> advancedExpanded;
  @NonNull private final Runnable applyToStore;
  @NonNull private final Runnable rerender;

  KeyboardThemeCustomizationWallpaperLayerRowAdvancedEditorsUi(
      @NonNull Context context,
      @NonNull ArrayList<KeyboardWallpaperLayer> stack,
      @NonNull ArrayList<Boolean> advancedExpanded,
      @NonNull Runnable applyToStore,
      @NonNull Runnable rerender) {
    this.context = context;
    this.stack = stack;
    this.advancedExpanded = advancedExpanded;
    this.applyToStore = applyToStore;
    this.rerender = rerender;
  }

  void addTo(@NonNull LinearLayout card, int index, @NonNull KeyboardWallpaperLayer layer) {
    final boolean hasDirectionAdvanced =
        layer.type() == KeyboardWallpaperLayer.TYPE_GRADIENT
            || layer.type() == KeyboardWallpaperLayer.TYPE_HIGHLIGHT
            || layer.type() == KeyboardWallpaperLayer.TYPE_STRIPES;
    final boolean hasScaleAdvanced =
        layer.type() == KeyboardWallpaperLayer.TYPE_GRAIN
            || layer.type() == KeyboardWallpaperLayer.TYPE_DOTS
            || layer.type() == KeyboardWallpaperLayer.TYPE_GRID
            || layer.type() == KeyboardWallpaperLayer.TYPE_STRIPES
            || layer.type() == KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES
            || layer.type() == KeyboardWallpaperLayer.TYPE_TRIANGLES
            || layer.type() == KeyboardWallpaperLayer.TYPE_HEX
            || layer.type() == KeyboardWallpaperLayer.TYPE_BLUR
            || layer.type() == KeyboardWallpaperLayer.TYPE_CHECKER;
    final boolean hasGradientStopsAdvanced = layer.type() == KeyboardWallpaperLayer.TYPE_GRADIENT;
    final boolean hasCheckerAlternateColorAdvanced =
        layer.type() == KeyboardWallpaperLayer.TYPE_CHECKER;
    final boolean hasAdvanced =
        hasDirectionAdvanced
            || hasScaleAdvanced
            || hasGradientStopsAdvanced
            || hasCheckerAlternateColorAdvanced;

    if (hasAdvanced) {
      final boolean expanded =
          index >= 0 && index < advancedExpanded.size() && advancedExpanded.get(index);

      final Button advancedButton = new Button(context);
      advancedButton.setAllCaps(false);
      advancedButton.setText(
          expanded
              ? R.string.keyboard_theme_wallpaper_layer_stack_hide_advanced
              : R.string.keyboard_theme_wallpaper_layer_stack_show_advanced);
      advancedButton.setOnClickListener(
          ignored -> {
            if (index < 0 || index >= advancedExpanded.size()) return;
            advancedExpanded.set(index, !advancedExpanded.get(index));
            rerender.run();
          });
      card.addView(
          advancedButton,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      final LinearLayout advancedSection = new LinearLayout(context);
      advancedSection.setOrientation(LinearLayout.VERTICAL);
      advancedSection.setVisibility(expanded ? View.VISIBLE : View.GONE);

      // Direction (for directional layers)
      if (hasDirectionAdvanced) {
        final LinearLayout directionRow = new LinearLayout(context);
        directionRow.setOrientation(LinearLayout.HORIZONTAL);
        directionRow.setPadding(
            0,
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
            0,
            0);
        advancedSection.addView(
            directionRow,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView directionTitle = new TextView(context);
        directionTitle.setText(R.string.keyboard_theme_wallpaper_layer_stack_direction_title);
        directionRow.addView(directionTitle);

        final Button directionButton = new Button(context);
        directionButton.setAllCaps(false);
        directionButton.setText(
            KeyboardThemeCustomizationWallpaperLayerEditors.directionLabel(
                context, layer.direction()));
        directionButton.setEnabled(layer.enabled());
        directionButton.setOnClickListener(
            ignored ->
                KeyboardThemeCustomizationWallpaperLayerEditors.showDirectionDialog(
                    context,
                    layer.type(),
                    dir -> {
                      final KeyboardWallpaperLayer current = stack.get(index);
                      stack.set(
                          index,
                          new KeyboardWallpaperLayer(
                              current.type(),
                              current.enabled(),
                              current.opacityPercent(),
                              current.blendMode(),
                              current.argb(),
                              current.argb2(),
                              dir,
                              current.scalePercent(),
                              current.gradientStops()));
                      applyToStore.run();
                      rerender.run();
                    }));
        directionRow.addView(directionButton);
      }

      // Scale (for textured/photo-derived layers)
      if (hasScaleAdvanced) {
        final int minScalePercent = 25;
        final int maxScalePercent = 400;
        final int clampedScalePercent =
            Math.max(minScalePercent, Math.min(maxScalePercent, layer.scalePercent()));

        final LinearLayout scaleRow = new LinearLayout(context);
        scaleRow.setOrientation(LinearLayout.HORIZONTAL);
        scaleRow.setPadding(
            0,
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
            0,
            0);
        advancedSection.addView(
            scaleRow,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView scaleLabel = new TextView(context);
        final int scaleTitle =
            layer.type() == KeyboardWallpaperLayer.TYPE_GRAIN
                ? R.string.keyboard_theme_wallpaper_layer_stack_grain_scale_title
                : layer.type() == KeyboardWallpaperLayer.TYPE_BLUR
                    ? R.string.keyboard_theme_wallpaper_layer_stack_blur_strength_title
                    : R.string.keyboard_theme_wallpaper_layer_stack_pattern_scale_title;
        scaleLabel.setText(scaleTitle);
        scaleRow.addView(scaleLabel);

        final SeekBar scale = new SeekBar(context);
        scale.setMax(maxScalePercent - minScalePercent);
        scale.setProgress(clampedScalePercent - minScalePercent);
        scaleRow.addView(
            scale, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView scaleValue = new TextView(context);
        scaleValue.setText(clampedScalePercent + "%");
        scaleRow.addView(scaleValue);

        scale.setEnabled(layer.enabled());
        scale.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
              @Override
              public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final int updatedScalePercent =
                    Math.max(
                        minScalePercent, Math.min(maxScalePercent, minScalePercent + progress));
                scaleValue.setText(updatedScalePercent + "%");
                final KeyboardWallpaperLayer current = stack.get(index);
                stack.set(
                    index,
                    new KeyboardWallpaperLayer(
                        current.type(),
                        current.enabled(),
                        current.opacityPercent(),
                        current.blendMode(),
                        current.argb(),
                        current.argb2(),
                        current.direction(),
                        updatedScalePercent,
                        current.gradientStops()));
                applyToStore.run();
              }

              @Override
              public void onStartTrackingTouch(SeekBar seekBar) {}

              @Override
              public void onStopTrackingTouch(SeekBar seekBar) {}
            });
      }

      // Gradient stops (advanced editor)
      if (hasGradientStopsAdvanced) {
        final LinearLayout stopsRow = new LinearLayout(context);
        stopsRow.setOrientation(LinearLayout.HORIZONTAL);
        stopsRow.setPadding(
            0,
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
            0,
            0);
        advancedSection.addView(
            stopsRow,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView stopsLabel = new TextView(context);
        stopsLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_stops_title);
        stopsRow.addView(stopsLabel);

        final Button editStops = new Button(context);
        editStops.setAllCaps(false);
        final int stopsCount =
            layer.gradientStops() != null && layer.gradientStops().size() >= 2
                ? layer.gradientStops().size()
                : 2;
        editStops.setText(
            context.getString(
                R.string.keyboard_theme_wallpaper_layer_stack_stops_edit, stopsCount));
        editStops.setEnabled(layer.enabled());
        editStops.setOnClickListener(
            ignored ->
                KeyboardThemeCustomizationWallpaperLayerEditors.showGradientStopsEditorDialog(
                    context, stack, index, applyToStore, rerender));
        stopsRow.addView(editStops);
      }

      // Checker alternate color (advanced)
      if (hasCheckerAlternateColorAdvanced) {
        final LinearLayout altColorRow = new LinearLayout(context);
        altColorRow.setOrientation(LinearLayout.HORIZONTAL);
        altColorRow.setPadding(
            0,
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
            0,
            0);
        advancedSection.addView(
            altColorRow,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView altColorLabel = new TextView(context);
        altColorLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_alternate_color_title);
        altColorRow.addView(altColorLabel);

        final View altSwatch = new View(context);
        final int swatchSize3 =
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 20);
        final LinearLayout.LayoutParams swatchParams3 =
            new LinearLayout.LayoutParams(swatchSize3, swatchSize3);
        swatchParams3.leftMargin =
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
        swatchParams3.rightMargin =
            KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
        altSwatch.setLayoutParams(swatchParams3);
        final Integer altColorForUi = layer.argb2();
        if (altColorForUi != null) {
          altSwatch.setBackground(new android.graphics.drawable.ColorDrawable(altColorForUi));
        }
        altColorRow.addView(altSwatch);

        final Button pickAltColor = new Button(context);
        pickAltColor.setAllCaps(false);
        pickAltColor.setText(
            altColorForUi != null
                ? KeyboardThemeCustomizationWallpaperLayerEditors.formatColor(altColorForUi)
                : context.getString(
                    R.string.keyboard_theme_wallpaper_layer_stack_color_pick_button));
        pickAltColor.setEnabled(layer.enabled());
        pickAltColor.setOnClickListener(
            ignored ->
                KeyboardThemeCustomizationWallpaperLayerEditors.showArgbColorPicker(
                    context,
                    KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(
                            context, layer.type())
                        + " — "
                        + context.getString(
                            R.string.keyboard_theme_wallpaper_layer_stack_alternate_color_title),
                    altColorForUi,
                    parsed -> {
                      final KeyboardWallpaperLayer current = stack.get(index);
                      stack.set(
                          index,
                          new KeyboardWallpaperLayer(
                              current.type(),
                              current.enabled(),
                              current.opacityPercent(),
                              current.blendMode(),
                              current.argb(),
                              parsed,
                              current.direction(),
                              current.scalePercent(),
                              current.gradientStops()));
                      applyToStore.run();
                      rerender.run();
                      return true;
                    }));
        altColorRow.addView(pickAltColor);
      }

      card.addView(
          advancedSection,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
  }
}
