package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationWallpaperLayerStackEditorDialog {

  private KeyboardThemeCustomizationWallpaperLayerStackEditorDialog() {}

  static void show(
      @NonNull Context context,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @NonNull KeyboardThemeCustomizationBackgroundSection.Host host,
      @NonNull String themeId,
      boolean keyLayers) {
    final boolean hadOverride =
        keyLayers
            ? wallpaperStore.hasKeyLayerStackOverride(themeId)
            : wallpaperStore.hasBackgroundLayerStackOverride(themeId);
    final KeyboardWallpaperLayer[] initial =
        keyLayers
            ? wallpaperStore.getKeyLayerStack(themeId)
            : wallpaperStore.getBackgroundLayerStack(themeId);
    final ArrayList<KeyboardWallpaperLayer> stack = new ArrayList<>(initial.length);
    for (KeyboardWallpaperLayer layer : initial) {
      if (layer != null) stack.add(layer);
    }

    final boolean[] changed = new boolean[] {false};
    final boolean[] accepted = new boolean[] {false};

    final Runnable applyToStore =
        () -> {
          changed[0] = true;
          final KeyboardWallpaperLayer[] out = stack.toArray(new KeyboardWallpaperLayer[0]);
          if (keyLayers) {
            wallpaperStore.setKeyLayerStack(themeId, out);
          } else {
            wallpaperStore.setBackgroundLayerStack(themeId, out);
          }
          host.updateLivePreview();
        };

    final KeyboardThemeCustomizationWallpaperLayerStackEditorController controller =
        new KeyboardThemeCustomizationWallpaperLayerStackEditorController(
            context, stack, applyToStore, keyLayers);

    final android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
    scrollView.addView(
        controller.getContentView(),
        new android.widget.ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(
                keyLayers
                    ? R.string.keyboard_theme_wallpaper_customization_key_layer_stack_title
                    : R.string.keyboard_theme_wallpaper_customization_background_layer_stack_title)
            .setView(scrollView)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setPositiveButton(android.R.string.ok, (d, w) -> accepted[0] = true)
            .create();

    dialog.setOnDismissListener(
        ignored -> {
          if (accepted[0]) {
            host.refreshState();
            return;
          }
          if (!changed[0]) return;

          if (keyLayers) {
            if (hadOverride) {
              wallpaperStore.setKeyLayerStack(themeId, initial);
            } else {
              wallpaperStore.clearKeyLayerStack(themeId);
            }
          } else {
            if (hadOverride) {
              wallpaperStore.setBackgroundLayerStack(themeId, initial);
            } else {
              wallpaperStore.clearBackgroundLayerStack(themeId);
            }
          }
          host.refreshState();
          host.updateLivePreview();
        });
    dialog.show();
  }

  @NonNull
  static String describeLayerStack(
      @NonNull Context context, @NonNull KeyboardWallpaperLayer[] layers) {
    if (layers.length == 0) {
      return context.getString(R.string.keyboard_theme_wallpaper_layer_order_none);
    }
    final StringBuilder sb = new StringBuilder();
    for (KeyboardWallpaperLayer layer : layers) {
      if (layer == null) continue;
      if (sb.length() > 0) sb.append(" \u2192 ");
      sb.append(KeyboardThemeCustomizationWallpaperLayerEditors.layerLabel(context, layer.type()));
    }
    if (sb.length() == 0) {
      return context.getString(R.string.keyboard_theme_wallpaper_layer_order_none);
    }
    return sb.toString();
  }
}
