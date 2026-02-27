package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayDataImpl;

final class KeyboardThemeCustomizationLivePreviewModesUi {

  @FunctionalInterface
  interface UpdateLivePreviewCallback {
    void updateLivePreview();
  }

  @FunctionalInterface
  interface SuggestionsEnsurer {
    void ensureSuggestionsVisible();
  }

  enum OverlayPreviewMode {
    AUTO,
    OFF,
    APP_COLORS,
    NIGHT_MODE,
    POWER_SAVING
  }

  enum PreviewLayoutMode {
    FULL,
    COMPACT,
    ONE_HANDED_LEFT,
    ONE_HANDED_RIGHT,
    LANDSCAPE_SCALED
  }

  @NonNull private final KeyboardThemeCustomizationLivePreviewSection.Host host;
  @NonNull private final UpdateLivePreviewCallback updateLivePreviewCallback;
  @NonNull private final SuggestionsEnsurer suggestionsEnsurer;

  @Nullable private TextView livePreviewOverlaysStatusView;
  @Nullable private Button livePreviewOverlaysButton;
  @Nullable private Button livePreviewLayoutButton;
  @Nullable private View livePreviewKeyboardArea;
  @Nullable private FrameLayout livePreviewKeyboardAreaContainer;
  @Nullable private FrameLayout livePreviewCandidatesContainer;
  @Nullable private FrameLayout livePreviewKeyboardContainer;

  @NonNull private OverlayPreviewMode overlayPreviewMode = OverlayPreviewMode.AUTO;
  @NonNull private PreviewLayoutMode previewLayoutMode = PreviewLayoutMode.FULL;
  @NonNull private PreviewFocusArea previewFocusArea = PreviewFocusArea.AUTO;
  @NonNull private PreviewFocusArea editorSuggestedFocusArea = PreviewFocusArea.AUTO;

  @Nullable private TextView focusAutoChip;
  @Nullable private TextView focusBackgroundChip;
  @Nullable private TextView focusKeysChip;
  @Nullable private TextView focusTextChip;
  @Nullable private TextView focusSuggestionsChip;
  @Nullable private TextView focusOverlaysChip;

  @Nullable private Drawable.ConstantState focusHighlightBorderState;

  KeyboardThemeCustomizationLivePreviewModesUi(
      @NonNull KeyboardThemeCustomizationLivePreviewSection.Host host,
      @NonNull UpdateLivePreviewCallback updateLivePreviewCallback,
      @NonNull SuggestionsEnsurer suggestionsEnsurer) {
    this.host = host;
    this.updateLivePreviewCallback = updateLivePreviewCallback;
    this.suggestionsEnsurer = suggestionsEnsurer;
  }

  void bindToRootView(@NonNull View root) {
    livePreviewOverlaysStatusView = root.findViewById(R.id.wallpaper_live_preview_overlays_status);
    livePreviewOverlaysButton = root.findViewById(R.id.wallpaper_live_preview_overlays_button);
    livePreviewLayoutButton = root.findViewById(R.id.wallpaper_live_preview_layout_button);
    livePreviewKeyboardArea = root.findViewById(R.id.wallpaper_live_preview_keyboard_area);
    livePreviewKeyboardAreaContainer = root.findViewById(R.id.wallpaper_live_preview_keyboard_area);
    livePreviewCandidatesContainer =
        root.findViewById(R.id.wallpaper_live_preview_candidates_container);
    livePreviewKeyboardContainer =
        root.findViewById(R.id.wallpaper_live_preview_keyboard_container);

    final Button layoutButton = livePreviewLayoutButton;
    if (layoutButton != null) {
      layoutButton.setOnClickListener(
          ignored -> {
            final Context c = host.getContext();
            if (c == null) return;
            showLayoutModeDialog(c);
          });
    }

    final Button overlaysButton = livePreviewOverlaysButton;
    if (overlaysButton != null) {
      overlaysButton.setOnClickListener(
          ignored -> {
            final Context c = host.getContext();
            if (c == null) return;
            showOverlayPreviewModeDialog(c);
          });
    }

    final TextView overlaysStatus = livePreviewOverlaysStatusView;
    if (overlaysStatus != null) {
      overlaysStatus.setOnClickListener(ignored -> host.scrollToPreference("section:overlays"));
    }

    bindFocusChips(root);
    updateOverlaysStatusUiIfPossible();
  }

  void setEditorSuggestedFocus(@NonNull PreviewFocusArea focusArea) {
    editorSuggestedFocusArea = focusArea;
    updateOverlaysStatusUiIfPossible();
    if (previewFocusArea == PreviewFocusArea.AUTO) {
      applyPreviewFocusHighlight();
    }
  }

  @NonNull
  OverlayData resolvePreviewOverlayData(@NonNull Context context) {
    return switch (overlayPreviewMode) {
      case OFF -> new OverlayDataImpl();
      case APP_COLORS ->
          new OverlayDataImpl(0xFF1E88E5, 0xFF1565C0, 0xFFFFC107, Color.WHITE, 0xFFBBDEFB);
      case NIGHT_MODE ->
          new OverlayDataImpl(0xFF222222, 0xFF000000, Color.DKGRAY, Color.GRAY, Color.DKGRAY);
      case POWER_SAVING ->
          new OverlayDataImpl(Color.BLACK, Color.BLACK, Color.DKGRAY, Color.GRAY, Color.DKGRAY);
      case AUTO -> {
        if (isPowerSavingOverlayActive(context)) {
          yield new OverlayDataImpl(
              Color.BLACK, Color.BLACK, Color.DKGRAY, Color.GRAY, Color.DKGRAY);
        }
        if (isNightModeOverlayActive(context)) {
          yield new OverlayDataImpl(0xFF222222, 0xFF000000, Color.DKGRAY, Color.GRAY, Color.DKGRAY);
        }
        yield new OverlayDataImpl();
      }
    };
  }

  void updateOverlaysStatusUi(@NonNull Context context) {
    final TextView statusView = livePreviewOverlaysStatusView;
    if (statusView == null) return;

    final boolean appColorsEnabled = isApplyRemoteAppColorsEnabled(context);
    final boolean nightActive = isNightModeOverlayActive(context);
    final boolean powerActive = isPowerSavingOverlayActive(context);

    final boolean hasAnyActiveOrEnabled = appColorsEnabled || nightActive || powerActive;
    final PreviewFocusArea effectiveFocus =
        previewFocusArea == PreviewFocusArea.AUTO ? editorSuggestedFocusArea : previewFocusArea;
    final boolean forceVisible = effectiveFocus == PreviewFocusArea.OVERLAYS;
    if (!hasAnyActiveOrEnabled && overlayPreviewMode == OverlayPreviewMode.AUTO && !forceVisible) {
      statusView.setVisibility(View.GONE);
    } else {
      final String previewLine =
          context.getString(
              R.string.keyboard_theme_live_preview_overlays_button_mode,
              overlayPreviewModeLabel(context, overlayPreviewMode));
      final StringBuilder sb = new StringBuilder();
      if (nightActive) sb.append(context.getString(R.string.night_mode_screen));
      if (powerActive) {
        if (sb.length() > 0) sb.append(" \u00b7 ");
        sb.append(context.getString(R.string.power_save_mode_screen));
      }
      if (appColorsEnabled) {
        if (sb.length() > 0) sb.append(" \u00b7 ");
        sb.append(context.getString(R.string.apply_remote_app_colors_to_theme));
      }
      if (sb.length() == 0) {
        if (forceVisible) {
          statusView.setText(
              previewLine
                  + "\n"
                  + context.getString(R.string.keyboard_theme_appearance_overlays_title)
                  + ": "
                  + context.getString(R.string.keyboard_theme_appearance_overlay_status_off));
        } else {
          statusView.setText(previewLine);
        }
      } else {
        statusView.setText(
            previewLine
                + "\n"
                + context.getString(R.string.keyboard_theme_appearance_overlays_title)
                + ": "
                + sb);
      }
      statusView.setVisibility(View.VISIBLE);
    }

    final Button overlaysButton = livePreviewOverlaysButton;
    if (overlaysButton != null) {
      overlaysButton.setText(
          context.getString(
              R.string.keyboard_theme_live_preview_overlays_button_mode,
              overlayPreviewModeLabel(context, overlayPreviewMode)));
    }
  }

  @NonNull
  String currentOverlayPreviewModeLabel(@NonNull Context context) {
    return overlayPreviewModeLabel(context, overlayPreviewMode);
  }

  void applyPreviewFocusHighlight() {
    final Context context = host.getContext();
    if (context == null) return;

    if (focusHighlightBorderState == null) {
      final Drawable d = context.getDrawable(R.drawable.preview_focus_highlight_border);
      focusHighlightBorderState = d != null ? d.getConstantState() : null;
    }

    final PreviewFocusArea effectiveFocus =
        previewFocusArea == PreviewFocusArea.AUTO ? editorSuggestedFocusArea : previewFocusArea;

    applyForeground(
        livePreviewKeyboardAreaContainer, effectiveFocus == PreviewFocusArea.BACKGROUND);
    applyForeground(livePreviewCandidatesContainer, effectiveFocus == PreviewFocusArea.SUGGESTIONS);
    applyForeground(
        livePreviewKeyboardContainer,
        effectiveFocus == PreviewFocusArea.KEYS || effectiveFocus == PreviewFocusArea.TEXT);

    final TextView overlaysStatus = livePreviewOverlaysStatusView;
    if (overlaysStatus != null) {
      if (effectiveFocus == PreviewFocusArea.OVERLAYS) {
        overlaysStatus.setBackgroundColor(
            ContextCompat.getColor(context, R.color.generic_clicked_background_color));
      } else {
        overlaysStatus.setBackgroundColor(Color.TRANSPARENT);
      }
    }
  }

  void applyPreviewLayoutMode() {
    final View keyboardArea = livePreviewKeyboardArea;
    final Context context = host.getContext();
    if (keyboardArea == null || context == null) return;

    final Button layoutButton = livePreviewLayoutButton;
    if (layoutButton != null) {
      layoutButton.setText(
          context.getString(
              R.string.keyboard_theme_live_preview_layout_button_mode,
              previewLayoutModeLabel(context, previewLayoutMode)));
    }

    keyboardArea.post(
        () -> {
          final float w = keyboardArea.getWidth();
          if (w <= 0f) return;

          final float scale =
              switch (previewLayoutMode) {
                case FULL -> 1f;
                case COMPACT -> 0.9f;
                case ONE_HANDED_LEFT, ONE_HANDED_RIGHT -> 0.85f;
                case LANDSCAPE_SCALED -> 0.75f;
              };
          final float pivotX =
              switch (previewLayoutMode) {
                case ONE_HANDED_LEFT -> 0f;
                case ONE_HANDED_RIGHT -> w;
                default -> w / 2f;
              };

          keyboardArea.setPivotX(pivotX);
          keyboardArea.setPivotY(0f);
          keyboardArea.setScaleX(scale);
          keyboardArea.setScaleY(scale);
        });
  }

  void dispose() {
    livePreviewOverlaysStatusView = null;
    livePreviewOverlaysButton = null;
    livePreviewLayoutButton = null;
    livePreviewKeyboardArea = null;
    livePreviewKeyboardAreaContainer = null;
    livePreviewCandidatesContainer = null;
    livePreviewKeyboardContainer = null;
    focusAutoChip = null;
    focusBackgroundChip = null;
    focusKeysChip = null;
    focusTextChip = null;
    focusSuggestionsChip = null;
    focusOverlaysChip = null;
    focusHighlightBorderState = null;
  }

  private boolean isApplyRemoteAppColorsEnabled(@NonNull Context context) {
    return NskApplicationBase.prefs(context)
        .getBoolean(
            R.string.settings_key_apply_remote_app_colors,
            R.bool.settings_default_apply_remote_app_colors)
        .get();
  }

  private boolean isNightModeOverlayActive(@NonNull Context context) {
    final var prefs = NskApplicationBase.prefs(context);
    final boolean enabled =
        prefs
            .getBoolean(
                R.string.settings_key_night_mode_theme_control, R.bool.settings_default_false)
            .get();
    if (!enabled) return false;

    final String mode =
        prefs
            .getString(R.string.settings_key_night_mode, R.string.settings_default_night_mode_value)
            .get();
    if ("never".equals(mode)) {
      return false;
    } else if ("always".equals(mode)) {
      return true;
    } else {
      return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
          == Configuration.UI_MODE_NIGHT_YES;
    }
  }

  private boolean isPowerSavingOverlayActive(@NonNull Context context) {
    final var prefs = NskApplicationBase.prefs(context);
    final boolean enabled =
        prefs
            .getBoolean(
                R.string.settings_key_power_save_mode_theme_control, R.bool.settings_default_true)
            .get();
    if (!enabled) return false;

    final android.os.PowerManager powerManager =
        (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
    return powerManager != null && powerManager.isPowerSaveMode();
  }

  private void showOverlayPreviewModeDialog(@NonNull Context context) {
    final CharSequence[] items =
        new CharSequence[] {
          overlayPreviewModeLabel(context, OverlayPreviewMode.AUTO),
          overlayPreviewModeLabel(context, OverlayPreviewMode.OFF),
          overlayPreviewModeLabel(context, OverlayPreviewMode.APP_COLORS),
          overlayPreviewModeLabel(context, OverlayPreviewMode.NIGHT_MODE),
          overlayPreviewModeLabel(context, OverlayPreviewMode.POWER_SAVING)
        };

    final int checkedItem =
        switch (overlayPreviewMode) {
          case AUTO -> 0;
          case OFF -> 1;
          case APP_COLORS -> 2;
          case NIGHT_MODE -> 3;
          case POWER_SAVING -> 4;
        };

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog);
    builder.setTitle(R.string.keyboard_theme_live_preview_overlays_dialog_title);
    builder.setMessage(R.string.keyboard_theme_live_preview_overlays_dialog_message);
    builder.setSingleChoiceItems(
        items,
        checkedItem,
        (dialog, which) -> {
          overlayPreviewMode =
              switch (which) {
                case 0 -> OverlayPreviewMode.AUTO;
                case 1 -> OverlayPreviewMode.OFF;
                case 2 -> OverlayPreviewMode.APP_COLORS;
                case 3 -> OverlayPreviewMode.NIGHT_MODE;
                case 4 -> OverlayPreviewMode.POWER_SAVING;
                default -> OverlayPreviewMode.AUTO;
              };
          updateLivePreviewCallback.updateLivePreview();
          dialog.dismiss();
        });
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
    builder.show();
  }

  @NonNull
  private static String overlayPreviewModeLabel(
      @NonNull Context context, @NonNull OverlayPreviewMode mode) {
    return switch (mode) {
      case AUTO -> context.getString(R.string.keyboard_theme_live_preview_overlay_mode_auto);
      case OFF -> context.getString(R.string.keyboard_theme_live_preview_overlay_mode_off);
      case APP_COLORS ->
          context.getString(R.string.keyboard_theme_live_preview_overlay_mode_app_colors);
      case NIGHT_MODE -> context.getString(R.string.keyboard_theme_live_preview_overlay_mode_night);
      case POWER_SAVING ->
          context.getString(R.string.keyboard_theme_live_preview_overlay_mode_power_saving);
    };
  }

  private void bindFocusChips(@NonNull View root) {
    focusAutoChip = root.findViewById(R.id.wallpaper_live_preview_focus_auto);
    focusBackgroundChip = root.findViewById(R.id.wallpaper_live_preview_focus_background);
    focusKeysChip = root.findViewById(R.id.wallpaper_live_preview_focus_keys);
    focusTextChip = root.findViewById(R.id.wallpaper_live_preview_focus_text);
    focusSuggestionsChip = root.findViewById(R.id.wallpaper_live_preview_focus_suggestions);
    focusOverlaysChip = root.findViewById(R.id.wallpaper_live_preview_focus_overlays);

    bindFocusChip(focusAutoChip, PreviewFocusArea.AUTO);
    bindFocusChip(focusBackgroundChip, PreviewFocusArea.BACKGROUND);
    bindFocusChip(focusKeysChip, PreviewFocusArea.KEYS);
    bindFocusChip(focusTextChip, PreviewFocusArea.TEXT);
    bindFocusChip(focusSuggestionsChip, PreviewFocusArea.SUGGESTIONS);
    bindFocusChip(focusOverlaysChip, PreviewFocusArea.OVERLAYS);

    updateFocusChipsUi();
  }

  private void bindFocusChip(@Nullable View chip, @NonNull PreviewFocusArea focusArea) {
    if (chip == null) return;
    chip.setOnClickListener(
        ignored -> {
          previewFocusArea = focusArea;
          if (focusArea == PreviewFocusArea.SUGGESTIONS) {
            suggestionsEnsurer.ensureSuggestionsVisible();
          }
          updateOverlaysStatusUiIfPossible();
          applyPreviewFocusHighlight();
          updateFocusChipsUi();
        });
  }

  private void updateOverlaysStatusUiIfPossible() {
    final Context context = host.getContext();
    if (context != null) {
      updateOverlaysStatusUi(context);
    }
  }

  private void updateFocusChipsUi() {
    setSelectedOrFalse(focusAutoChip, previewFocusArea == PreviewFocusArea.AUTO);
    setSelectedOrFalse(focusBackgroundChip, previewFocusArea == PreviewFocusArea.BACKGROUND);
    setSelectedOrFalse(focusKeysChip, previewFocusArea == PreviewFocusArea.KEYS);
    setSelectedOrFalse(focusTextChip, previewFocusArea == PreviewFocusArea.TEXT);
    setSelectedOrFalse(focusSuggestionsChip, previewFocusArea == PreviewFocusArea.SUGGESTIONS);
    setSelectedOrFalse(focusOverlaysChip, previewFocusArea == PreviewFocusArea.OVERLAYS);
  }

  private static void setSelectedOrFalse(@Nullable View view, boolean selected) {
    if (view == null) return;
    view.setSelected(selected);
  }

  private void applyForeground(@Nullable FrameLayout frame, boolean highlight) {
    if (frame == null) return;
    if (!highlight) {
      frame.setForeground(null);
      return;
    }

    final Drawable.ConstantState state = focusHighlightBorderState;
    final Drawable drawable =
        state != null
            ? state.newDrawable(frame.getResources())
            : frame.getContext().getDrawable(R.drawable.preview_focus_highlight_border);
    frame.setForeground(drawable);
  }

  private void showLayoutModeDialog(@NonNull Context context) {
    final CharSequence[] items =
        new CharSequence[] {
          previewLayoutModeLabel(context, PreviewLayoutMode.FULL),
          previewLayoutModeLabel(context, PreviewLayoutMode.COMPACT),
          previewLayoutModeLabel(context, PreviewLayoutMode.ONE_HANDED_LEFT),
          previewLayoutModeLabel(context, PreviewLayoutMode.ONE_HANDED_RIGHT),
          previewLayoutModeLabel(context, PreviewLayoutMode.LANDSCAPE_SCALED)
        };

    final int checkedItem =
        switch (previewLayoutMode) {
          case FULL -> 0;
          case COMPACT -> 1;
          case ONE_HANDED_LEFT -> 2;
          case ONE_HANDED_RIGHT -> 3;
          case LANDSCAPE_SCALED -> 4;
        };

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog);
    builder.setTitle(R.string.keyboard_theme_live_preview_layout_dialog_title);
    builder.setSingleChoiceItems(
        items,
        checkedItem,
        (dialog, which) -> {
          previewLayoutMode =
              switch (which) {
                case 0 -> PreviewLayoutMode.FULL;
                case 1 -> PreviewLayoutMode.COMPACT;
                case 2 -> PreviewLayoutMode.ONE_HANDED_LEFT;
                case 3 -> PreviewLayoutMode.ONE_HANDED_RIGHT;
                case 4 -> PreviewLayoutMode.LANDSCAPE_SCALED;
                default -> PreviewLayoutMode.FULL;
              };
          applyPreviewLayoutMode();
          dialog.dismiss();
        });
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
    builder.show();
  }

  @NonNull
  private static String previewLayoutModeLabel(
      @NonNull Context context, @NonNull PreviewLayoutMode mode) {
    return switch (mode) {
      case FULL -> context.getString(R.string.keyboard_theme_live_preview_layout_mode_full);
      case COMPACT -> context.getString(R.string.keyboard_theme_live_preview_layout_mode_compact);
      case ONE_HANDED_LEFT ->
          context.getString(R.string.keyboard_theme_live_preview_layout_mode_one_handed_left);
      case ONE_HANDED_RIGHT ->
          context.getString(R.string.keyboard_theme_live_preview_layout_mode_one_handed_right);
      case LANDSCAPE_SCALED ->
          context.getString(R.string.keyboard_theme_live_preview_layout_mode_landscape_scaled);
    };
  }
}
