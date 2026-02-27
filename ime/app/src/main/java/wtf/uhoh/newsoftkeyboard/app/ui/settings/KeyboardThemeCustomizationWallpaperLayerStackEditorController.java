package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;

final class KeyboardThemeCustomizationWallpaperLayerStackEditorController {

  private final Context context;
  private final ArrayList<KeyboardWallpaperLayer> stack;
  private final Runnable applyToStore;
  private final boolean keyLayers;
  private final ArrayList<Boolean> advancedExpanded;

  private final LinearLayout root;
  private final LinearLayout list;
  @NonNull private final KeyboardThemeCustomizationWallpaperLayerRowUi rowUi;

  KeyboardThemeCustomizationWallpaperLayerStackEditorController(
      @NonNull Context context,
      @NonNull ArrayList<KeyboardWallpaperLayer> stack,
      @NonNull Runnable applyToStore,
      boolean keyLayers) {
    this.context = context;
    this.stack = stack;
    this.applyToStore = applyToStore;
    this.keyLayers = keyLayers;
    advancedExpanded = new ArrayList<>(stack.size());
    for (KeyboardWallpaperLayer layer : stack) {
      advancedExpanded.add(shouldAutoExpandAdvanced(layer));
    }

    rowUi =
        new KeyboardThemeCustomizationWallpaperLayerRowUi(
            context, stack, advancedExpanded, applyToStore, this::render);

    root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    final int padding = dpToPx(context, 16);
    root.setPadding(padding, padding, padding, padding);

    final LinearLayout buttonRow = new LinearLayout(context);
    buttonRow.setOrientation(LinearLayout.HORIZONTAL);
    root.addView(
        buttonRow,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final Button addLayer = new Button(context);
    addLayer.setAllCaps(false);
    addLayer.setText(R.string.keyboard_theme_wallpaper_layer_order_add_layer);
    buttonRow.addView(
        addLayer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    final Button clearAll = new Button(context);
    clearAll.setAllCaps(false);
    clearAll.setText(R.string.keyboard_theme_wallpaper_layer_stack_clear_all);
    buttonRow.addView(
        clearAll,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    list = new LinearLayout(context);
    list.setOrientation(LinearLayout.VERTICAL);
    root.addView(
        list,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    addLayer.setOnClickListener(
        ignored ->
            KeyboardThemeCustomizationWallpaperLayerEditors.showAddLayerDialog(
                context,
                keyLayers,
                type -> {
                  stack.add(
                      KeyboardThemeCustomizationWallpaperLayerEditors.defaultLayerForType(type));
                  advancedExpanded.add(true);
                  applyToStore.run();
                  render();
                }));

    clearAll.setOnClickListener(
        ignored -> {
          stack.clear();
          advancedExpanded.clear();
          applyToStore.run();
          render();
        });

    render();
  }

  @NonNull
  View getContentView() {
    return root;
  }

  void render() {
    list.removeAllViews();
    if (stack.isEmpty()) {
      final TextView none = new TextView(context);
      none.setText(R.string.keyboard_theme_wallpaper_layer_order_none);
      none.setPadding(0, dpToPx(context, 8), 0, dpToPx(context, 8));
      list.addView(
          none,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      return;
    }

    for (int i = 0; i < stack.size(); i++) {
      list.addView(
          rowUi.createRow(i),
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
  }

  private static boolean shouldAutoExpandAdvanced(@Nullable KeyboardWallpaperLayer layer) {
    if (layer == null) return false;

    final boolean hasDirection =
        layer.type() == KeyboardWallpaperLayer.TYPE_GRADIENT
            || layer.type() == KeyboardWallpaperLayer.TYPE_HIGHLIGHT
            || layer.type() == KeyboardWallpaperLayer.TYPE_STRIPES;
    if (hasDirection && layer.direction() != KeyboardWallpaperLayer.DIRECTION_VERTICAL) return true;

    final boolean hasScale =
        layer.type() == KeyboardWallpaperLayer.TYPE_GRAIN
            || layer.type() == KeyboardWallpaperLayer.TYPE_DOTS
            || layer.type() == KeyboardWallpaperLayer.TYPE_GRID
            || layer.type() == KeyboardWallpaperLayer.TYPE_STRIPES
            || layer.type() == KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES
            || layer.type() == KeyboardWallpaperLayer.TYPE_TRIANGLES
            || layer.type() == KeyboardWallpaperLayer.TYPE_HEX
            || layer.type() == KeyboardWallpaperLayer.TYPE_BLUR
            || layer.type() == KeyboardWallpaperLayer.TYPE_CHECKER;
    if (hasScale && layer.scalePercent() != KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT)
      return true;

    if (layer.type() == KeyboardWallpaperLayer.TYPE_GRADIENT
        && layer.gradientStops() != null
        && layer.gradientStops().size() >= 2) {
      return true;
    }

    if (layer.type() == KeyboardWallpaperLayer.TYPE_CHECKER && layer.argb2() != null) return true;

    return false;
  }

  static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
  }
}
