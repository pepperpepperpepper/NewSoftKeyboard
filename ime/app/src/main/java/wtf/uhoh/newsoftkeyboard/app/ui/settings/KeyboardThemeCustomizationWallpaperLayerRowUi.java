package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import java.util.ArrayList;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayerPreviewDrawableFactory;

final class KeyboardThemeCustomizationWallpaperLayerRowUi {

  @NonNull private final Context context;
  @NonNull private final ArrayList<KeyboardWallpaperLayer> stack;
  @NonNull private final ArrayList<Boolean> advancedExpanded;
  @NonNull private final Runnable applyToStore;
  @NonNull private final Runnable rerender;
  @NonNull private final KeyboardThemeCustomizationWallpaperLayerRowBasicEditorsUi basicEditorsUi;

  @NonNull
  private final KeyboardThemeCustomizationWallpaperLayerRowAdvancedEditorsUi advancedEditorsUi;

  KeyboardThemeCustomizationWallpaperLayerRowUi(
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
    this.basicEditorsUi =
        new KeyboardThemeCustomizationWallpaperLayerRowBasicEditorsUi(
            context, stack, applyToStore, rerender);
    this.advancedEditorsUi =
        new KeyboardThemeCustomizationWallpaperLayerRowAdvancedEditorsUi(
            context, stack, advancedExpanded, applyToStore, rerender);
  }

  @NonNull
  View createRow(int index) {
    final KeyboardWallpaperLayer layer = stack.get(index);

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
        0, 0, 0, KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 4));
    card.addView(
        header,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final View layerPreview = new View(context);
    final int previewSize =
        KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 28);
    final LinearLayout.LayoutParams previewParams =
        new LinearLayout.LayoutParams(previewSize, previewSize);
    previewParams.rightMargin =
        KeyboardThemeCustomizationWallpaperLayerStackEditorController.dpToPx(context, 8);
    layerPreview.setLayoutParams(previewParams);
    final KeyboardWallpaperLayer previewLayer =
        new KeyboardWallpaperLayer(
            layer.type(),
            layer.enabled(),
            100 /* preview */,
            layer.blendMode(),
            layer.argb(),
            layer.argb2(),
            layer.direction(),
            layer.scalePercent(),
            layer.gradientStops());
    final android.graphics.drawable.Drawable previewDrawable =
        KeyboardWallpaperLayerPreviewDrawableFactory.createPreviewDrawable(previewLayer);
    if (previewDrawable != null) {
      layerPreview.setBackground(previewDrawable);
    } else {
      layerPreview.setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    }
    header.addView(layerPreview);

    final SwitchCompat enabled = new SwitchCompat(context);
    enabled.setText(
        KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(context, layer.type()));
    enabled.setChecked(layer.enabled());
    header.addView(
        enabled, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    final Button up = new Button(context);
    up.setAllCaps(false);
    up.setText(R.string.keyboard_theme_wallpaper_layer_order_move_up);
    up.setEnabled(index > 0);
    up.setOnClickListener(
        ignored -> {
          if (index <= 0) return;
          final KeyboardWallpaperLayer tmp = stack.get(index - 1);
          stack.set(index - 1, stack.get(index));
          stack.set(index, tmp);
          final Boolean tmpExpanded = advancedExpanded.get(index - 1);
          advancedExpanded.set(index - 1, advancedExpanded.get(index));
          advancedExpanded.set(index, tmpExpanded);
          applyToStore.run();
          rerender.run();
        });
    header.addView(up);

    final Button down = new Button(context);
    down.setAllCaps(false);
    down.setText(R.string.keyboard_theme_wallpaper_layer_order_move_down);
    down.setEnabled(index < stack.size() - 1);
    down.setOnClickListener(
        ignored -> {
          if (index >= stack.size() - 1) return;
          final KeyboardWallpaperLayer tmp = stack.get(index + 1);
          stack.set(index + 1, stack.get(index));
          stack.set(index, tmp);
          final Boolean tmpExpanded = advancedExpanded.get(index + 1);
          advancedExpanded.set(index + 1, advancedExpanded.get(index));
          advancedExpanded.set(index, tmpExpanded);
          applyToStore.run();
          rerender.run();
        });
    header.addView(down);

    final Button duplicate = new Button(context);
    duplicate.setAllCaps(false);
    duplicate.setText(R.string.keyboard_theme_wallpaper_layer_stack_duplicate);
    duplicate.setOnClickListener(
        ignored -> {
          stack.add(index + 1, stack.get(index));
          advancedExpanded.add(index + 1, advancedExpanded.get(index));
          applyToStore.run();
          rerender.run();
        });
    header.addView(duplicate);

    final Button remove = new Button(context);
    remove.setAllCaps(false);
    remove.setText(R.string.keyboard_theme_wallpaper_layer_order_remove_layer);
    remove.setOnClickListener(
        ignored -> {
          if (index < 0 || index >= stack.size()) return;
          stack.remove(index);
          advancedExpanded.remove(index);
          applyToStore.run();
          rerender.run();
        });
    header.addView(remove);

    enabled.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          final KeyboardWallpaperLayer current = stack.get(index);
          stack.set(
              index,
              new KeyboardWallpaperLayer(
                  current.type(),
                  isChecked,
                  current.opacityPercent(),
                  current.blendMode(),
                  current.argb(),
                  current.argb2(),
                  current.direction(),
                  current.scalePercent(),
                  current.gradientStops()));
          applyToStore.run();
          rerender.run();
        });

    basicEditorsUi.addTo(card, index, layer);
    advancedEditorsUi.addTo(card, index, layer);

    return card;
  }
}
