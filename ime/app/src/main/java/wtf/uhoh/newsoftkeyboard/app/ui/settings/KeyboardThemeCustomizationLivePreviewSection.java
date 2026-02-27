package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;

final class KeyboardThemeCustomizationLivePreviewSection {

  interface Host {
    @Nullable
    Context getContext();

    @Nullable
    KeyboardTheme getCurrentThemeOrNull();

    @NonNull
    String resolvePresetId(@NonNull KeyboardTheme theme);

    void scrollToPreference(@NonNull String key);
  }

  @NonNull private final Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @Nullable private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardWallpaperResolver wallpaperPreviewResolver;
  @NonNull private final KeyboardThemeCustomizationLivePreviewModesUi modesUi;
  @NonNull private final KeyboardThemeCustomizationLivePreviewInspectController inspectController;

  @Nullable private DemoKeyboardView livePreviewKeyboardView;
  @Nullable private CandidateView livePreviewCandidateView;
  @Nullable private String livePreviewConfiguredBaseThemeId;
  @Nullable private String livePreviewLastAppliedPresetId;
  private int livePreviewLastAppliedUserOverridesToken = Integer.MIN_VALUE;
  private boolean livePreviewLastHadWallpaperOverride;
  private boolean livePreviewShowSuggestions = true;
  private boolean livePreviewShifted;
  private boolean livePreviewSimulateTyping;
  private boolean livePreviewShowOriginal;

  @Nullable private CompoundButton livePreviewSuggestionsToggle;

  enum PreviewFocusArea {
    AUTO,
    BACKGROUND,
    KEYS,
    TEXT,
    SUGGESTIONS,
    OVERLAYS
  }

  KeyboardThemeCustomizationLivePreviewSection(
      @NonNull Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @Nullable KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardWallpaperResolver wallpaperPreviewResolver) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.wallpaperStore = wallpaperStore;
    this.wallpaperPreviewResolver = wallpaperPreviewResolver;
    this.modesUi =
        new KeyboardThemeCustomizationLivePreviewModesUi(
            host, this::updateLivePreview, this::ensureSuggestionsVisible);
    this.inspectController =
        new KeyboardThemeCustomizationLivePreviewInspectController(
            host, themeOverridesStore, wallpaperStore, modesUi::currentOverlayPreviewModeLabel);
  }

  void bindToRootView(@NonNull View root) {
    final DemoKeyboardView view = root.findViewById(R.id.wallpaper_live_preview_keyboard);
    if (view == null) return;
    final CandidateView candidates =
        (CandidateView) root.findViewById(R.id.wallpaper_live_preview_candidates);
    bindLivePreviewViews(root, view, candidates);
  }

  @Nullable
  DemoKeyboardView getLivePreviewKeyboardView() {
    return livePreviewKeyboardView;
  }

  void dispose() {
    livePreviewKeyboardView = null;
    livePreviewCandidateView = null;
    livePreviewSuggestionsToggle = null;
    livePreviewConfiguredBaseThemeId = null;
    livePreviewLastAppliedPresetId = null;
    livePreviewLastAppliedUserOverridesToken = Integer.MIN_VALUE;
    livePreviewLastHadWallpaperOverride = false;
    modesUi.dispose();
    inspectController.dispose();
  }

  private void bindLivePreviewViews(
      @NonNull View root, @NonNull DemoKeyboardView view, @Nullable CandidateView candidateView) {
    livePreviewKeyboardView = view;
    livePreviewCandidateView = candidateView;
    livePreviewConfiguredBaseThemeId = null;
    livePreviewLastAppliedPresetId = null;
    livePreviewLastAppliedUserOverridesToken = Integer.MIN_VALUE;
    livePreviewLastHadWallpaperOverride = false;

    final CompoundButton suggestionsToggle =
        root.findViewById(R.id.wallpaper_live_preview_toggle_suggestions);
    if (suggestionsToggle != null) {
      suggestionsToggle.setOnCheckedChangeListener(null);
      suggestionsToggle.setChecked(livePreviewShowSuggestions);
      suggestionsToggle.setOnCheckedChangeListener(
          (ignored, checked) -> {
            livePreviewShowSuggestions = checked;
            final CandidateView candidates = livePreviewCandidateView;
            if (candidates != null) {
              candidates.setVisibility(checked ? View.VISIBLE : View.GONE);
            }
          });
    }
    livePreviewSuggestionsToggle = suggestionsToggle;

    final CompoundButton shiftToggle = root.findViewById(R.id.wallpaper_live_preview_toggle_shift);
    if (shiftToggle != null) {
      shiftToggle.setOnCheckedChangeListener(null);
      shiftToggle.setChecked(livePreviewShifted);
      shiftToggle.setOnCheckedChangeListener(
          (ignored, checked) -> {
            livePreviewShifted = checked;
            final DemoKeyboardView keyboard = livePreviewKeyboardView;
            if (keyboard != null) {
              keyboard.setShifted(checked);
            }
          });
    }

    final CompoundButton typingToggle =
        root.findViewById(R.id.wallpaper_live_preview_toggle_typing);
    if (typingToggle != null) {
      typingToggle.setOnCheckedChangeListener(null);
      typingToggle.setChecked(livePreviewSimulateTyping);
      typingToggle.setOnCheckedChangeListener(
          (ignored, checked) -> {
            livePreviewSimulateTyping = checked;
            final DemoKeyboardView keyboard = livePreviewKeyboardView;
            if (keyboard != null) {
              keyboard.setSimulatedTypingText(checked ? "Theme preview" : null);
            }
          });
    }

    final CompoundButton originalToggle =
        root.findViewById(R.id.wallpaper_live_preview_toggle_original);
    if (originalToggle != null) {
      originalToggle.setOnCheckedChangeListener(null);
      originalToggle.setChecked(livePreviewShowOriginal);
      originalToggle.setOnCheckedChangeListener(
          (ignored, checked) -> {
            livePreviewShowOriginal = checked;
            updateLivePreview();
          });
    }

    modesUi.bindToRootView(root);
    inspectController.bindToRootView(root, view);

    final View openKeyboardButton = root.findViewById(R.id.wallpaper_live_preview_open_keyboard);
    if (openKeyboardButton != null) {
      openKeyboardButton.setOnClickListener(
          ignored -> {
            final Context c = host.getContext();
            if (c == null) return;
            c.startActivity(new Intent(c, TestInputActivity.class));
          });
    }

    if (candidateView != null && candidateView.getSuggestions().isEmpty()) {
      candidateView.setSuggestions(Arrays.asList("Theme preview", "Café", "naïve", "你好", "🙂"), 1);
    }

    applyLivePreviewState(view, candidateView);
    updateLivePreview();
    modesUi.applyPreviewLayoutMode();
    modesUi.applyPreviewFocusHighlight();
  }

  void setEditorSuggestedFocus(@NonNull PreviewFocusArea focusArea) {
    modesUi.setEditorSuggestedFocus(focusArea);
  }

  void updateLivePreview() {
    final DemoKeyboardView preview = livePreviewKeyboardView;
    if (preview == null) return;
    final CandidateView candidates = livePreviewCandidateView;
    final KeyboardTheme theme = host.getCurrentThemeOrNull();
    if (theme == null) return;
    final Context context = host.getContext();
    if (context == null) return;

    final boolean applyUserOverrides = !livePreviewShowOriginal;
    preview.setApplyUserThemeOverrides(applyUserOverrides);
    if (candidates != null) {
      candidates.setApplyUserThemeOverrides(applyUserOverrides);
    }

    final OverlayData overlayData = modesUi.resolvePreviewOverlayData(context);
    preview.setThemeOverlay(overlayData);
    if (candidates != null) {
      candidates.setThemeOverlay(overlayData);
    }
    modesUi.updateOverlaysStatusUi(context);

    preview.setAllowExpensiveWallpaperEffects(true);

    final String baseThemeId = theme.getId();
    final boolean themeChanged =
        livePreviewConfiguredBaseThemeId == null
            || !livePreviewConfiguredBaseThemeId.equals(baseThemeId);
    if (themeChanged) {
      livePreviewConfiguredBaseThemeId = baseThemeId;
      livePreviewLastAppliedPresetId = null;
      livePreviewLastAppliedUserOverridesToken = Integer.MIN_VALUE;
      livePreviewLastHadWallpaperOverride = false;

      preview.setKeyboardTheme(theme);

      final KeyboardDefinition defaultKeyboard =
          wtf.uhoh.newsoftkeyboard.app.NskApplicationBase.getKeyboardFactory(context)
              .getEnabledAddOn()
              .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      defaultKeyboard.loadKeyboard(preview.getThemedKeyboardDimens());
      preview.setKeyboard(defaultKeyboard, null, null);
    }

    final String presetId = host.resolvePresetId(theme);
    final int userOverrideToken =
        themeOverridesStore != null ? themeOverridesStore.getChangeToken(presetId) : 0;
    final boolean presetChanged =
        livePreviewLastAppliedPresetId == null || !livePreviewLastAppliedPresetId.equals(presetId);
    final boolean userOverridesChanged =
        userOverrideToken != livePreviewLastAppliedUserOverridesToken;
    final boolean hasWallpaperOverride =
        wallpaperStore != null
            && wallpaperStore.hasWallpaper(presetId)
            && !wallpaperStore.isWallpaperInvalid(presetId);
    final boolean wallpaperRemoved = livePreviewLastHadWallpaperOverride && !hasWallpaperOverride;

    if (candidates != null && (themeChanged || presetChanged || userOverridesChanged)) {
      candidates.setKeyboardTheme(theme);
    }

    if (themeChanged || presetChanged || userOverridesChanged || wallpaperRemoved) {
      livePreviewLastAppliedPresetId = presetId;
      livePreviewLastAppliedUserOverridesToken = userOverrideToken;
    }
    livePreviewLastHadWallpaperOverride = hasWallpaperOverride;

    if (applyUserOverrides && wallpaperPreviewResolver != null) {
      wallpaperPreviewResolver.applyPhotoOverrideIfAnyAsync(preview, theme);
    }
    applyLivePreviewState(preview, candidates);
    if (candidates != null) {
      candidates.invalidate();
    }
    preview.invalidate();
    modesUi.applyPreviewLayoutMode();
    modesUi.applyPreviewFocusHighlight();
  }

  private void applyLivePreviewState(
      @NonNull DemoKeyboardView preview, @Nullable CandidateView candidates) {
    if (candidates != null) {
      candidates.setVisibility(livePreviewShowSuggestions ? View.VISIBLE : View.GONE);
    }
    preview.setShifted(livePreviewShifted);
    preview.setSimulatedTypingText(livePreviewSimulateTyping ? "Theme preview" : null);
  }

  private void ensureSuggestionsVisible() {
    if (livePreviewShowSuggestions) return;
    livePreviewShowSuggestions = true;
    final CandidateView candidates = livePreviewCandidateView;
    if (candidates != null) {
      candidates.setVisibility(View.VISIBLE);
    }
    final CompoundButton toggle = livePreviewSuggestionsToggle;
    if (toggle != null) {
      toggle.setOnCheckedChangeListener(null);
      toggle.setChecked(true);
      toggle.setOnCheckedChangeListener(
          (ignored, checked) -> {
            livePreviewShowSuggestions = checked;
            final CandidateView cv = livePreviewCandidateView;
            if (cv != null) {
              cv.setVisibility(checked ? View.VISIBLE : View.GONE);
            }
          });
    }
  }
}
