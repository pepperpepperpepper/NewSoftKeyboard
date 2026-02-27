package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationLivePreviewInspectController {

  @FunctionalInterface
  interface OverlayPreviewModeLabelProvider {
    @NonNull
    String getOverlayPreviewModeLabel(@NonNull Context context);
  }

  @NonNull private final KeyboardThemeCustomizationLivePreviewSection.Host host;

  @NonNull
  private final KeyboardThemeCustomizationLivePreviewInspectKeyDetailsBuilder keyDetailsBuilder;

  @NonNull private final OverlayPreviewModeLabelProvider overlayPreviewModeLabelProvider;

  @Nullable private DemoKeyboardView livePreviewKeyboardView;

  private boolean livePreviewInspectKeys;
  private float touchDownX;
  private float touchDownY;
  private boolean touchMoved;
  private boolean longPressTriggered;
  private int touchDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
  private int touchDownPrimaryCode;
  @Nullable private Runnable longPressRunnable;
  private int touchSlopPx;

  KeyboardThemeCustomizationLivePreviewInspectController(
      @NonNull KeyboardThemeCustomizationLivePreviewSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @Nullable KeyboardWallpaperOverrideStore wallpaperStore,
      @NonNull OverlayPreviewModeLabelProvider overlayPreviewModeLabelProvider) {
    this.host = host;
    keyDetailsBuilder =
        new KeyboardThemeCustomizationLivePreviewInspectKeyDetailsBuilder(
            themeOverridesStore, wallpaperStore);
    this.overlayPreviewModeLabelProvider = overlayPreviewModeLabelProvider;
  }

  void bindToRootView(@NonNull View root, @NonNull DemoKeyboardView view) {
    livePreviewKeyboardView = view;

    final Context context = host.getContext();
    touchSlopPx = context != null ? ViewConfiguration.get(context).getScaledTouchSlop() : 0;

    final CompoundButton inspectToggle =
        root.findViewById(R.id.wallpaper_live_preview_toggle_inspect);
    if (inspectToggle != null) {
      inspectToggle.setOnCheckedChangeListener(null);
      inspectToggle.setChecked(livePreviewInspectKeys);
      inspectToggle.setOnCheckedChangeListener(
          (ignored, checked) -> {
            livePreviewInspectKeys = checked;
            touchMoved = false;
            longPressTriggered = false;
          });
    }

    view.setOnTouchListener((ignored, event) -> handlePreviewTouch(view, event));
  }

  void dispose() {
    livePreviewKeyboardView = null;
    longPressRunnable = null;
  }

  private boolean handlePreviewTouch(@NonNull DemoKeyboardView view, @NonNull MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        touchMoved = false;
        longPressTriggered = false;
        touchDownX = event.getX();
        touchDownY = event.getY();
        touchDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        touchDownPrimaryCode = 0;

        final int downX = (int) event.getX();
        final int downY = (int) event.getY();
        final int keyIndex = view.getKeyDetector().getKeyIndexAndNearbyCodes(downX, downY, null);
        if (keyIndex == KeyboardViewBase.NOT_A_KEY) return false;

        final KeyboardDefinition keyboard = view.getKeyboard();
        if (keyboard == null) return false;

        final java.util.List<Keyboard.Key> keys = keyboard.getKeys();
        if (keyIndex < 0 || keyIndex >= keys.size()) return false;

        touchDownKeyIndex = keyIndex;
        touchDownPrimaryCode = keys.get(keyIndex).getPrimaryCode();
        view.simulateKeyDown(touchDownPrimaryCode);

        if (longPressRunnable != null) {
          view.removeCallbacks(longPressRunnable);
          longPressRunnable = null;
        }
        longPressRunnable =
            () -> {
              if (touchMoved) return;
              if (touchDownKeyIndex == KeyboardViewBase.NOT_A_KEY) return;
              longPressTriggered = true;
              showInspectDialogForTouch(view, (int) touchDownX, (int) touchDownY);
            };
        view.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
        return true;

      case MotionEvent.ACTION_MOVE:
        if (touchDownKeyIndex == KeyboardViewBase.NOT_A_KEY) return false;
        if (!touchMoved) {
          final float dx = Math.abs(event.getX() - touchDownX);
          final float dy = Math.abs(event.getY() - touchDownY);
          if (dx > touchSlopPx || dy > touchSlopPx) {
            touchMoved = true;
            cancelLongPress(view);
          }
        }
        return true;

      case MotionEvent.ACTION_UP:
        if (touchDownKeyIndex == KeyboardViewBase.NOT_A_KEY) return false;
        cancelLongPress(view);
        view.simulateKeyUp(touchDownPrimaryCode);
        if (!touchMoved && !longPressTriggered && livePreviewInspectKeys) {
          showInspectDialogForTouch(view, (int) event.getX(), (int) event.getY());
        }
        touchDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        touchDownPrimaryCode = 0;
        return true;

      case MotionEvent.ACTION_CANCEL:
        cancelLongPress(view);
        view.simulateCancel();
        touchDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        touchDownPrimaryCode = 0;
        return false;

      default:
        return false;
    }
  }

  private void cancelLongPress(@NonNull View view) {
    if (longPressRunnable == null) return;
    view.removeCallbacks(longPressRunnable);
    longPressRunnable = null;
  }

  private void showInspectDialogForTouch(@NonNull DemoKeyboardView view, int x, int y) {
    final Context context = host.getContext();
    if (context == null) return;

    final int keyIndex = view.getKeyDetector().getKeyIndexAndNearbyCodes(x, y, null);
    if (keyIndex == KeyboardViewBase.NOT_A_KEY) return;

    final KeyboardDefinition keyboard = view.getKeyboard();
    if (keyboard == null) return;

    final java.util.List<Keyboard.Key> keys = keyboard.getKeys();
    if (keyIndex < 0 || keyIndex >= keys.size()) return;

    final Keyboard.Key key = keys.get(keyIndex);
    final CharSequence label = key.label;
    final int primaryCode = key.getPrimaryCode();

    view.highlightKey(key);

    final String typeLabel;
    if (primaryCode == KeyCodes.SPACE) {
      typeLabel = context.getString(R.string.keyboard_theme_live_preview_inspect_key_type_spacebar);
    } else if (primaryCode == KeyCodes.ENTER) {
      typeLabel = context.getString(R.string.keyboard_theme_live_preview_inspect_key_type_enter);
    } else if (key.modifier) {
      typeLabel = context.getString(R.string.keyboard_theme_live_preview_inspect_key_type_modifier);
    } else {
      typeLabel = context.getString(R.string.keyboard_theme_live_preview_inspect_key_type_key);
    }

    final String message =
        context.getString(
            R.string.keyboard_theme_live_preview_inspect_key_message, typeLabel, primaryCode);
    final StringBuilder messageBuilder = new StringBuilder(message);

    // Help with “why did it change?” confusion by surfacing preview mode right here.
    messageBuilder
        .append("\n")
        .append(
            context.getString(
                R.string.keyboard_theme_live_preview_overlays_button_mode,
                overlayPreviewModeLabelProvider.getOverlayPreviewModeLabel(context)));

    final KeyboardTheme theme = host.getCurrentThemeOrNull();
    final String presetId = theme != null ? host.resolvePresetId(theme) : null;
    final String styleDetails = keyDetailsBuilder.buildStyleDetails(context, presetId, key);
    if (!styleDetails.isEmpty()) {
      messageBuilder.append("\n\n").append(styleDetails);
    }

    final CharSequence[] items =
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_live_preview_inspect_edit_background),
          context.getString(R.string.keyboard_theme_live_preview_inspect_edit_colors),
          context.getString(R.string.keyboard_theme_live_preview_inspect_edit_typography),
          context.getString(R.string.keyboard_theme_live_preview_inspect_edit_shadows)
        };
    final String[] targets =
        new String[] {
          "section:background", "section:colors", "section:typography", "section:shadows"
        };

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog);
    if (label != null && label.length() > 0) {
      builder.setTitle(label);
    } else {
      builder.setTitle(R.string.keyboard_theme_live_preview_inspect_key_title);
    }
    builder.setMessage(messageBuilder.toString());
    builder.setItems(
        items,
        (dialog, which) -> {
          if (which < 0 || which >= targets.length) return;
          host.scrollToPreference(targets[which]);
        });
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
    builder.show();
  }
}
