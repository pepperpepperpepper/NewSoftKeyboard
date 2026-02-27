package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;

final class KeyboardThemeCustomizationWallpaperGradientStopsEditorDialog {

  private KeyboardThemeCustomizationWallpaperGradientStopsEditorDialog() {}

  static void show(
      @NonNull Context context,
      @NonNull ArrayList<KeyboardWallpaperLayer> stack,
      int layerIndex,
      @NonNull Runnable applyToStore,
      @NonNull Runnable renderOuter) {
    if (layerIndex < 0 || layerIndex >= stack.size()) return;
    final KeyboardWallpaperLayer initialLayer = stack.get(layerIndex);
    if (initialLayer == null || initialLayer.type() != KeyboardWallpaperLayer.TYPE_GRADIENT) return;

    final ArrayList<KeyboardWallpaperLayer.GradientStop> stops = new ArrayList<>();
    final List<KeyboardWallpaperLayer.GradientStop> existingStops = initialLayer.gradientStops();
    if (existingStops != null && existingStops.size() >= 2) {
      stops.addAll(existingStops);
    } else {
      final int start =
          KeyboardThemeCustomizationWallpaperLayerEditors.resolveGradientStartColorForUi(
              initialLayer);
      final Integer endOrNull =
          KeyboardThemeCustomizationWallpaperLayerEditors.resolveLayerColorForUi(initialLayer);
      final int end = endOrNull != null ? endOrNull : Color.BLACK;
      stops.add(new KeyboardWallpaperLayer.GradientStop(0, start));
      stops.add(new KeyboardWallpaperLayer.GradientStop(100, end));
    }

    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    final int padding =
        KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 16);
    root.setPadding(padding, padding, padding, padding);

    final LinearLayout buttons = new LinearLayout(context);
    buttons.setOrientation(LinearLayout.HORIZONTAL);
    root.addView(
        buttons,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final Button addStop = new Button(context);
    addStop.setAllCaps(false);
    addStop.setText(R.string.keyboard_theme_wallpaper_layer_stack_add_stop);
    addStop.setEnabled(initialLayer.enabled());
    buttons.addView(
        addStop, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    final LinearLayout list = new LinearLayout(context);
    list.setOrientation(LinearLayout.VERTICAL);
    root.addView(
        list,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final Runnable applyStopsToLayer =
        () -> {
          stops.removeIf(Objects::isNull);
          if (stops.size() < 2) return;
          stops.sort(Comparator.comparingInt(KeyboardWallpaperLayer.GradientStop::positionPercent));

          final KeyboardWallpaperLayer.GradientStop first = stops.get(0);
          final KeyboardWallpaperLayer.GradientStop last = stops.get(stops.size() - 1);
          final KeyboardWallpaperLayer current = stack.get(layerIndex);
          stack.set(
              layerIndex,
              new KeyboardWallpaperLayer(
                  current.type(),
                  current.enabled(),
                  current.opacityPercent(),
                  current.blendMode(),
                  last.argb(),
                  first.argb(),
                  current.direction(),
                  current.scalePercent(),
                  List.copyOf(stops)));
          applyToStore.run();
        };

    final Runnable[] renderStops = new Runnable[1];
    renderStops[0] =
        () -> {
          list.removeAllViews();
          for (int i = 0; i < stops.size(); i++) {
            final int stopIndex = i;
            final KeyboardWallpaperLayer.GradientStop stop = stops.get(i);

            final LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(
                0,
                KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8),
                0,
                KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8));

            final LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setPadding(
                0,
                0,
                0,
                KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4));
            card.addView(
                header,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final TextView title = new TextView(context);
            title.setText(
                context.getString(
                    R.string.keyboard_theme_wallpaper_layer_stack_stop_title, stopIndex + 1));
            header.addView(
                title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            final Button removeStop = new Button(context);
            removeStop.setAllCaps(false);
            removeStop.setText(R.string.keyboard_theme_wallpaper_layer_stack_remove_stop);
            removeStop.setEnabled(initialLayer.enabled() && stops.size() > 2);
            removeStop.setOnClickListener(
                ignored -> {
                  if (stopIndex < 0 || stopIndex >= stops.size()) return;
                  if (stops.size() <= 2) return;
                  stops.remove(stopIndex);
                  applyStopsToLayer.run();
                  renderStops[0].run();
                });
            header.addView(removeStop);

            // Color
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
            colorLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_color_title);
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
            swatch.setBackground(new android.graphics.drawable.ColorDrawable(stop.argb()));
            colorRow.addView(swatch);

            final Button pickColor = new Button(context);
            pickColor.setAllCaps(false);
            pickColor.setText(
                KeyboardThemeCustomizationWallpaperLayerEditors.formatColor(stop.argb()));
            pickColor.setEnabled(initialLayer.enabled());
            pickColor.setOnClickListener(
                ignored -> {
                  final EditTextPreference fakePref = new EditTextPreference(context);
                  fakePref.setTitle(
                      KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(
                              context, KeyboardWallpaperLayer.TYPE_GRADIENT)
                          + " — "
                          + context.getString(
                              R.string.keyboard_theme_wallpaper_layer_stack_stop_title,
                              stopIndex + 1));
                  fakePref.setText(
                      KeyboardThemeCustomizationWallpaperLayerEditors.formatColor(stop.argb()));
                  fakePref.setOnPreferenceChangeListener(
                      (pref, newValue) -> {
                        final String raw = newValue == null ? "" : String.valueOf(newValue).trim();
                        final int parsed;
                        if (raw.isEmpty()) {
                          parsed = Color.TRANSPARENT;
                        } else {
                          try {
                            parsed = Color.parseColor(raw);
                          } catch (IllegalArgumentException e) {
                            return false;
                          }
                        }

                        if (stopIndex < 0 || stopIndex >= stops.size()) return false;
                        final KeyboardWallpaperLayer.GradientStop currentStop =
                            stops.get(stopIndex);
                        stops.set(
                            stopIndex,
                            new KeyboardWallpaperLayer.GradientStop(
                                currentStop.positionPercent(), parsed));
                        applyStopsToLayer.run();
                        renderStops[0].run();
                        return true;
                      });
                  KeyboardThemeCustomizationArgbColorPickerDialog.show(context, fakePref);
                });
            colorRow.addView(pickColor);

            // Position
            final LinearLayout positionRow = new LinearLayout(context);
            positionRow.setOrientation(LinearLayout.HORIZONTAL);
            positionRow.setPadding(
                0,
                KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4),
                0,
                0);
            card.addView(
                positionRow,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final TextView posLabel = new TextView(context);
            posLabel.setText(R.string.keyboard_theme_wallpaper_layer_stack_stop_position_title);
            positionRow.addView(posLabel);

            final SeekBar pos = new SeekBar(context);
            pos.setMax(100);
            pos.setProgress(Math.max(0, Math.min(100, stop.positionPercent())));
            pos.setEnabled(initialLayer.enabled());
            positionRow.addView(
                pos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            final TextView posValue = new TextView(context);
            posValue.setText(pos.getProgress() + "%");
            positionRow.addView(posValue);

            pos.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                  @Override
                  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    posValue.setText(progress + "%");
                    if (!fromUser) return;
                    if (stopIndex < 0 || stopIndex >= stops.size()) return;
                    final KeyboardWallpaperLayer.GradientStop currentStop = stops.get(stopIndex);
                    stops.set(
                        stopIndex,
                        new KeyboardWallpaperLayer.GradientStop(progress, currentStop.argb()));
                  }

                  @Override
                  public void onStartTrackingTouch(SeekBar seekBar) {}

                  @Override
                  public void onStopTrackingTouch(SeekBar seekBar) {
                    applyStopsToLayer.run();
                    renderStops[0].run();
                  }
                });

            list.addView(
                card,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          }
        };

    addStop.setOnClickListener(
        ignored -> {
          if (!initialLayer.enabled()) return;
          if (stops.size() < 2) return;

          // Insert into the largest gap.
          final ArrayList<KeyboardWallpaperLayer.GradientStop> sorted = new ArrayList<>(stops);
          sorted.sort(
              Comparator.comparingInt(KeyboardWallpaperLayer.GradientStop::positionPercent));

          int bestGap = -1;
          int insertPos = 50;
          for (int i = 0; i < sorted.size() - 1; i++) {
            final int a = sorted.get(i).positionPercent();
            final int b = sorted.get(i + 1).positionPercent();
            final int gap = b - a;
            if (gap > bestGap) {
              bestGap = gap;
              insertPos = a + (gap / 2);
            }
          }
          insertPos = Math.max(0, Math.min(100, insertPos));

          int insertColor = 0x80FFFFFF;
          for (int i = 0; i < sorted.size() - 1; i++) {
            final KeyboardWallpaperLayer.GradientStop a = sorted.get(i);
            final KeyboardWallpaperLayer.GradientStop b = sorted.get(i + 1);
            if (insertPos >= a.positionPercent() && insertPos <= b.positionPercent()) {
              final float denom = Math.max(1f, (b.positionPercent() - a.positionPercent()));
              final float t = (insertPos - a.positionPercent()) / denom;
              insertColor = lerpArgb(a.argb(), b.argb(), t);
              break;
            }
          }

          stops.add(new KeyboardWallpaperLayer.GradientStop(insertPos, insertColor));
          applyStopsToLayer.run();
          renderStops[0].run();
        });

    final android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
    scrollView.addView(
        root,
        new android.widget.ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.keyboard_theme_wallpaper_layer_stack_stops_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
            .create();
    dialog.setOnDismissListener(ignored -> renderOuter.run());
    renderStops[0].run();
    dialog.show();
  }

  private static int lerpArgb(int argbA, int argbB, float t) {
    final float clamped = Math.max(0f, Math.min(1f, t));
    final int aA = (argbA >>> 24) & 0xFF;
    final int aR = (argbA >>> 16) & 0xFF;
    final int aG = (argbA >>> 8) & 0xFF;
    final int aB = argbA & 0xFF;
    final int bA = (argbB >>> 24) & 0xFF;
    final int bR = (argbB >>> 16) & 0xFF;
    final int bG = (argbB >>> 8) & 0xFF;
    final int bB = argbB & 0xFF;
    final int oA = Math.round(aA + (bA - aA) * clamped);
    final int oR = Math.round(aR + (bR - aR) * clamped);
    final int oG = Math.round(aG + (bG - aG) * clamped);
    final int oB = Math.round(aB + (bB - aB) * clamped);
    return (oA << 24) | (oR << 16) | (oG << 8) | oB;
  }
}
