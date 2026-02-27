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
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;

final class KeyboardThemeCustomizationWallpaperLayerRowBasicEditorsUi {

  @NonNull private final Context context;
  @NonNull private final ArrayList<KeyboardWallpaperLayer> stack;
  @NonNull private final Runnable applyToStore;
  @NonNull private final Runnable rerender;

  KeyboardThemeCustomizationWallpaperLayerRowBasicEditorsUi(
      @NonNull Context context,
      @NonNull ArrayList<KeyboardWallpaperLayer> stack,
      @NonNull Runnable applyToStore,
      @NonNull Runnable rerender) {
    this.context = context;
    this.stack = stack;
    this.applyToStore = applyToStore;
    this.rerender = rerender;
  }

  void addTo(@NonNull LinearLayout card, int index, @NonNull KeyboardWallpaperLayer layer) {
    // Opacity
    final LinearLayout opacityRow = new LinearLayout(context);
    opacityRow.setOrientation(LinearLayout.HORIZONTAL);
    opacityRow.setPadding(
        0, KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4), 0, 0);
    card.addView(
        opacityRow,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final TextView opacityLabel = new TextView(context);
    opacityLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_opacity_title);
    opacityRow.addView(opacityLabel);

    final SeekBar opacity = new SeekBar(context);
    opacity.setMax(100);
    opacity.setProgress(Math.max(0, Math.min(100, layer.opacityPercent())));
    opacityRow.addView(
        opacity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    final TextView opacityValue = new TextView(context);
    opacityValue.setText(opacity.getProgress() + "%");
    opacityRow.addView(opacityValue);

    opacity.setEnabled(layer.enabled());
    opacity.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            opacityValue.setText(progress + "%");
            final KeyboardWallpaperLayer current = stack.get(index);
            stack.set(
                index,
                new KeyboardWallpaperLayer(
                    current.type(),
                    current.enabled(),
                    Math.max(0, Math.min(100, progress)),
                    current.blendMode(),
                    current.argb(),
                    current.argb2(),
                    current.direction(),
                    current.scalePercent(),
                    current.gradientStops()));
            applyToStore.run();
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    // Blend mode
    final LinearLayout blendRow = new LinearLayout(context);
    blendRow.setOrientation(LinearLayout.HORIZONTAL);
    blendRow.setPadding(
        0, KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4), 0, 0);
    card.addView(
        blendRow,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final TextView blendLabel = new TextView(context);
    blendLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_blend_mode_title);
    blendRow.addView(blendLabel);

    final Button blendButton = new Button(context);
    blendButton.setAllCaps(false);
    blendButton.setText(
        KeyboardThemeCustomizationWallpaperLayerEditors.blendModeLabel(context, layer.blendMode()));
    blendButton.setEnabled(layer.enabled());
    blendButton.setOnClickListener(
        ignored ->
            KeyboardThemeCustomizationWallpaperLayerEditors.showBlendModeDialog(
                context,
                mode -> {
                  final KeyboardWallpaperLayer current = stack.get(index);
                  stack.set(
                      index,
                      new KeyboardWallpaperLayer(
                          current.type(),
                          current.enabled(),
                          current.opacityPercent(),
                          mode,
                          current.argb(),
                          current.argb2(),
                          current.direction(),
                          current.scalePercent(),
                          current.gradientStops()));
                  applyToStore.run();
                  rerender.run();
                }));
    blendRow.addView(blendButton);

    if (layer.type() == KeyboardWallpaperLayer.TYPE_GRADIENT) {
      // Gradient start color
      final LinearLayout startRow = new LinearLayout(context);
      startRow.setOrientation(LinearLayout.HORIZONTAL);
      startRow.setPadding(
          0,
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
          0,
          0);
      card.addView(
          startRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      final TextView startLabel = new TextView(context);
      startLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_start_color_title);
      startRow.addView(startLabel);

      final View startSwatch = new View(context);
      final int startSwatchSize =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 20);
      final LinearLayout.LayoutParams startSwatchParams =
          new LinearLayout.LayoutParams(startSwatchSize, startSwatchSize);
      startSwatchParams.leftMargin =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
      startSwatchParams.rightMargin =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
      startSwatch.setLayoutParams(startSwatchParams);
      final Integer startColorForUi =
          KeyboardThemeCustomizationWallpaperLayerEditors.resolveGradientStartColorForUi(layer);
      startSwatch.setBackground(new android.graphics.drawable.ColorDrawable(startColorForUi));
      startRow.addView(startSwatch);

      final Button pickStartColor = new Button(context);
      pickStartColor.setAllCaps(false);
      pickStartColor.setText(
          KeyboardThemeCustomizationWallpaperLayerEditors.formatColor(startColorForUi));
      pickStartColor.setEnabled(layer.enabled());
      pickStartColor.setOnClickListener(
          ignored ->
              KeyboardThemeCustomizationWallpaperLayerEditors.showArgbColorPicker(
                  context,
                  KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(context, layer.type())
                      + " — "
                      + context.getString(
                          R.string.keyboard_theme_wallpaper_layer_stack_start_color_title),
                  startColorForUi,
                  parsed -> {
                    final KeyboardWallpaperLayer current = stack.get(index);
                    final List<KeyboardWallpaperLayer.GradientStop> updatedStops =
                        KeyboardThemeCustomizationWallpaperLayerEditors
                            .updateGradientStopsStartColor(current, parsed);
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
                            updatedStops));
                    applyToStore.run();
                    rerender.run();
                    return true;
                  }));
      startRow.addView(pickStartColor);

      // Gradient end color
      final LinearLayout colorRow = new LinearLayout(context);
      colorRow.setOrientation(LinearLayout.HORIZONTAL);
      colorRow.setPadding(
          0,
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
          0,
          0);
      card.addView(
          colorRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      final TextView colorLabel = new TextView(context);
      colorLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_end_color_title);
      colorRow.addView(colorLabel);

      final View swatch = new View(context);
      final int swatchSize =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 20);
      final LinearLayout.LayoutParams swatchParams =
          new LinearLayout.LayoutParams(swatchSize, swatchSize);
      swatchParams.leftMargin =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
      swatchParams.rightMargin =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
      swatch.setLayoutParams(swatchParams);
      final Integer colorForUi =
          KeyboardThemeCustomizationWallpaperLayerEditors.resolveLayerColorForUi(layer);
      if (colorForUi != null) {
        swatch.setBackground(new android.graphics.drawable.ColorDrawable(colorForUi));
      }
      colorRow.addView(swatch);

      final Button pickColor = new Button(context);
      pickColor.setAllCaps(false);
      pickColor.setText(
          colorForUi != null
              ? KeyboardThemeCustomizationWallpaperLayerEditors.formatColor(colorForUi)
              : context.getString(R.string.keyboard_theme_wallpaper_layer_stack_color_pick_button));
      pickColor.setEnabled(layer.enabled());
      pickColor.setOnClickListener(
          ignored ->
              KeyboardThemeCustomizationWallpaperLayerEditors.showArgbColorPicker(
                  context,
                  KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(context, layer.type())
                      + " — "
                      + context.getString(
                          R.string.keyboard_theme_wallpaper_layer_stack_end_color_title),
                  colorForUi,
                  parsed -> {
                    final KeyboardWallpaperLayer current = stack.get(index);
                    final List<KeyboardWallpaperLayer.GradientStop> updatedStops =
                        KeyboardThemeCustomizationWallpaperLayerEditors.updateGradientStopsEndColor(
                            current, parsed);
                    stack.set(
                        index,
                        new KeyboardWallpaperLayer(
                            current.type(),
                            current.enabled(),
                            current.opacityPercent(),
                            current.blendMode(),
                            parsed,
                            current.argb2(),
                            current.direction(),
                            current.scalePercent(),
                            updatedStops));
                    applyToStore.run();
                    rerender.run();
                    return true;
                  }));
      colorRow.addView(pickColor);
    } else if (layer.type() == KeyboardWallpaperLayer.TYPE_SOLID_COLOR
        || layer.type() == KeyboardWallpaperLayer.TYPE_COLOR_WASH
        || layer.type() == KeyboardWallpaperLayer.TYPE_VIGNETTE
        || layer.type() == KeyboardWallpaperLayer.TYPE_HIGHLIGHT
        || layer.type() == KeyboardWallpaperLayer.TYPE_DOTS
        || layer.type() == KeyboardWallpaperLayer.TYPE_GRID
        || layer.type() == KeyboardWallpaperLayer.TYPE_STRIPES
        || layer.type() == KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES
        || layer.type() == KeyboardWallpaperLayer.TYPE_TRIANGLES
        || layer.type() == KeyboardWallpaperLayer.TYPE_HEX
        || layer.type() == KeyboardWallpaperLayer.TYPE_CHECKER) {
      final LinearLayout colorRow = new LinearLayout(context);
      colorRow.setOrientation(LinearLayout.HORIZONTAL);
      colorRow.setPadding(
          0,
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
          0,
          0);
      card.addView(
          colorRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      final TextView colorLabel2 = new TextView(context);
      colorLabel2.setText(R.string.keyboard_theme_wallpaper_layer_stack_color_title);
      colorRow.addView(colorLabel2);

      final View swatch2 = new View(context);
      final int swatchSize2 =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 20);
      final LinearLayout.LayoutParams swatchParams2 =
          new LinearLayout.LayoutParams(swatchSize2, swatchSize2);
      swatchParams2.leftMargin =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
      swatchParams2.rightMargin =
          KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
      swatch2.setLayoutParams(swatchParams2);
      final Integer colorForUi2 =
          KeyboardThemeCustomizationWallpaperLayerEditors.resolveLayerColorForUi(layer);
      if (colorForUi2 != null) {
        swatch2.setBackground(new android.graphics.drawable.ColorDrawable(colorForUi2));
      }
      colorRow.addView(swatch2);

      final Button pickColor2 = new Button(context);
      pickColor2.setAllCaps(false);
      pickColor2.setText(
          colorForUi2 != null
              ? KeyboardThemeCustomizationWallpaperLayerEditors.formatColor(colorForUi2)
              : context.getString(R.string.keyboard_theme_wallpaper_layer_stack_color_pick_button));
      pickColor2.setEnabled(layer.enabled());
      pickColor2.setOnClickListener(
          ignored ->
              KeyboardThemeCustomizationWallpaperLayerEditors.showArgbColorPicker(
                  context,
                  KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(context, layer.type())
                      + " — "
                      + context.getString(
                          R.string.keyboard_theme_wallpaper_layer_stack_color_title),
                  colorForUi2,
                  parsed -> {
                    final KeyboardWallpaperLayer current = stack.get(index);
                    stack.set(
                        index,
                        new KeyboardWallpaperLayer(
                            current.type(),
                            current.enabled(),
                            current.opacityPercent(),
                            current.blendMode(),
                            parsed,
                            current.argb2(),
                            current.direction(),
                            current.scalePercent(),
                            current.gradientStops()));
                    applyToStore.run();
                    rerender.run();
                    return true;
                  }));
      colorRow.addView(pickColor2);
    }
  }
}
