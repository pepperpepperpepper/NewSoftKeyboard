package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import java.util.Objects;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetTransfer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;

public class KeyboardThemeCustomizationFragment extends PreferenceFragmentCompat {

  private KeyboardWallpaperOverrideStore wallpaperStore;
  private KeyboardThemeUserOverridesStore themeOverridesStore;
  private KeyboardWallpaperResolver wallpaperPreviewResolver;
  private KeyboardThemePresetStore presetStore;
  private ActivityResultLauncher<String[]> pickWallpaperLauncher;
  private ActivityResultLauncher<String[]> pickKeyFontLauncher;
  private ActivityResultLauncher<String> exportPresetLauncher;
  private ActivityResultLauncher<String[]> importPresetLauncher;

  @Nullable private KeyboardThemeCustomizationOverlaysSection overlaysSection;

  @Nullable private KeyboardThemeCustomizationOverlaysSection.Host overlaysSectionHost;

  @Nullable private KeyboardThemeCustomizationLivePreviewSection livePreviewSection;

  @Nullable private KeyboardThemeCustomizationLivePreviewSection.Host livePreviewSectionHost;

  @Nullable private KeyboardThemeCustomizationPresetsSection presetsSection;

  @Nullable private KeyboardThemeCustomizationPresetsSection.Host presetsSectionHost;

  @Nullable private KeyboardThemeCustomizationBackgroundSection backgroundSection;

  @Nullable private KeyboardThemeCustomizationBackgroundSection.Host backgroundSectionHost;

  @Nullable private KeyboardThemeCustomizationColorsSection colorsSection;

  @Nullable private KeyboardThemeCustomizationColorsSection.Host colorsSectionHost;

  @Nullable private KeyboardThemeCustomizationTypographySection typographySection;

  @Nullable private KeyboardThemeCustomizationTypographySection.Host typographySectionHost;

  @Nullable private KeyboardThemeCustomizationShadowsSection shadowsSection;

  @Nullable private KeyboardThemeCustomizationShadowsSection.Host shadowsSectionHost;

  @Nullable private KeyboardThemeCustomizationResetSection resetSection;

  @Nullable private KeyboardThemeCustomizationResetSection.Host resetSectionHost;

  private void initHostsIfNeeded() {
    if (overlaysSectionHost != null) return;
    overlaysSectionHost = new KeyboardThemeCustomizationFragmentHosts.OverlaysSectionHost(this);
    livePreviewSectionHost =
        new KeyboardThemeCustomizationFragmentHosts.LivePreviewSectionHost(this);
    presetsSectionHost = new KeyboardThemeCustomizationFragmentHosts.PresetsSectionHost(this);
    backgroundSectionHost = new KeyboardThemeCustomizationFragmentHosts.BackgroundSectionHost(this);
    colorsSectionHost = new KeyboardThemeCustomizationFragmentHosts.ColorsSectionHost(this);
    typographySectionHost = new KeyboardThemeCustomizationFragmentHosts.TypographySectionHost(this);
    shadowsSectionHost = new KeyboardThemeCustomizationFragmentHosts.ShadowsSectionHost(this);
    resetSectionHost = new KeyboardThemeCustomizationFragmentHosts.ResetSectionHost(this);
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    wallpaperStore = new KeyboardWallpaperOverrideStore(context);
    themeOverridesStore = new KeyboardThemeUserOverridesStore(context);
    wallpaperPreviewResolver = new KeyboardWallpaperResolver(context);
    presetStore = new KeyboardThemePresetStore(context);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    initHostsIfNeeded();
    pickWallpaperLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
              final Context context = getContext();
              final KeyboardThemeCustomizationBackgroundSection background = backgroundSection;
              if (context == null || background == null) return;
              background.onPhotoPicked(context, uri);
            });
    pickKeyFontLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onCustomKeyFontPicked);
    exportPresetLauncher =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument(KeyboardThemePresetTransfer.MIME_TYPE_ZIP),
            this::onPresetExportUri);
    importPresetLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onPresetImportUri);
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View preferencesView = super.onCreateView(inflater, container, savedInstanceState);
    final View root =
        inflater.inflate(R.layout.keyboard_theme_customization_fragment, container, false);
    final ViewGroup prefsContainer =
        root.findViewById(R.id.theme_customization_preferences_container);
    if (preferencesView != null && prefsContainer != null) {
      prefsContainer.addView(
          preferencesView,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
    return root;
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    initHostsIfNeeded();
    final var context = requireContext();
    if (wallpaperStore == null) {
      wallpaperStore = new KeyboardWallpaperOverrideStore(context);
    }
    if (themeOverridesStore == null) {
      themeOverridesStore = new KeyboardThemeUserOverridesStore(context);
    }
    if (wallpaperPreviewResolver == null) {
      wallpaperPreviewResolver = new KeyboardWallpaperResolver(context);
    }
    if (presetStore == null) {
      presetStore = new KeyboardThemePresetStore(context);
    }
    final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    setPreferenceScreen(screen);

    if (livePreviewSection == null) {
      livePreviewSection =
          new KeyboardThemeCustomizationLivePreviewSection(
              Objects.requireNonNull(livePreviewSectionHost),
              themeOverridesStore,
              wallpaperStore,
              wallpaperPreviewResolver);
    }

    if (presetsSection == null) {
      presetsSection =
          new KeyboardThemeCustomizationPresetsSection(
              Objects.requireNonNull(presetsSectionHost),
              presetStore,
              wallpaperStore,
              themeOverridesStore);
    }
    presetsSection.addPreferences(context, screen);

    if (backgroundSection == null) {
      backgroundSection =
          new KeyboardThemeCustomizationBackgroundSection(
              Objects.requireNonNull(backgroundSectionHost), wallpaperStore, themeOverridesStore);
    }
    backgroundSection.addPreferences(context, screen);

    if (colorsSection == null) {
      colorsSection =
          new KeyboardThemeCustomizationColorsSection(
              Objects.requireNonNull(colorsSectionHost), wallpaperStore, themeOverridesStore);
    }
    colorsSection.addPreferences(context, screen);

    if (typographySection == null) {
      typographySection =
          new KeyboardThemeCustomizationTypographySection(
              Objects.requireNonNull(typographySectionHost), themeOverridesStore);
    }
    typographySection.addPreferences(context, screen);

    if (shadowsSection == null) {
      shadowsSection =
          new KeyboardThemeCustomizationShadowsSection(
              Objects.requireNonNull(shadowsSectionHost), themeOverridesStore);
    }
    shadowsSection.addPreferences(context, screen);

    if (overlaysSection == null) {
      overlaysSection =
          new KeyboardThemeCustomizationOverlaysSection(
              Objects.requireNonNull(overlaysSectionHost));
    }
    overlaysSection.addPreferences(context, screen);

    if (resetSection == null) {
      resetSection =
          new KeyboardThemeCustomizationResetSection(
              Objects.requireNonNull(resetSectionHost), wallpaperStore, themeOverridesStore);
    }
    resetSection.addPreferences(context, screen);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.keyboard_theme_wallpaper_customization_title);
    refreshState();
  }

  @Override
  public void onDestroy() {
    final KeyboardThemeCustomizationPresetsSection presets = presetsSection;
    if (presets != null) {
      presets.dispose();
    }
    final KeyboardThemeCustomizationColorsSection colors = colorsSection;
    if (colors != null) {
      colors.dispose();
    }
    final KeyboardThemeCustomizationTypographySection typography = typographySection;
    if (typography != null) {
      typography.dispose();
    }
    final KeyboardThemeCustomizationShadowsSection shadows = shadowsSection;
    if (shadows != null) {
      shadows.dispose();
    }
    final KeyboardThemeCustomizationBackgroundSection background = backgroundSection;
    if (background != null) {
      background.dispose();
    }
    final KeyboardThemeCustomizationLivePreviewSection preview = livePreviewSection;
    if (preview != null) {
      preview.dispose();
    }
    final KeyboardThemeCustomizationOverlaysSection overlays = overlaysSection;
    if (overlays != null) {
      overlays.dispose();
    }
    final KeyboardThemeCustomizationResetSection reset = resetSection;
    if (reset != null) {
      reset.dispose();
    }
    super.onDestroy();
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshState();
    scrollToRequestedPreferenceIfNeeded();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final KeyboardThemeCustomizationLivePreviewSection preview = livePreviewSection;
    if (preview != null) {
      preview.bindToRootView(view);
    }
    bindSectionNav(view);
    refreshState();
  }

  private void bindSectionNav(@NonNull View root) {
    bindScrollNavButton(root, R.id.theme_customization_nav_presets, "section:presets");
    bindScrollNavButton(root, R.id.theme_customization_nav_background, "section:background");
    bindScrollNavButton(root, R.id.theme_customization_nav_colors, "section:colors");
    bindScrollNavButton(root, R.id.theme_customization_nav_typography, "section:typography");
    bindScrollNavButton(root, R.id.theme_customization_nav_shadows, "section:shadows");
    bindScrollNavButton(root, R.id.theme_customization_nav_overlays, "section:overlays");
    bindScrollNavButton(root, R.id.theme_customization_nav_reset, "section:reset");
  }

  private void bindScrollNavButton(
      @NonNull View root, int buttonId, @NonNull String preferenceKey) {
    final View button = root.findViewById(buttonId);
    if (button == null) return;
    button.setOnClickListener(ignored -> scrollToPreference(preferenceKey));
  }

  private void scrollToRequestedPreferenceIfNeeded() {
    final Bundle args = getArguments();
    if (args == null) return;
    final String key = args.getString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
    if (TextUtils.isEmpty(key)) return;
    scrollToPreference(key);
    args.remove(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
  }

  @Nullable
  DemoKeyboardView getLivePreviewKeyboardViewOrNull() {
    final KeyboardThemeCustomizationLivePreviewSection preview = livePreviewSection;
    return preview != null ? preview.getLivePreviewKeyboardView() : null;
  }

  @Nullable
  ActivityResultLauncher<String[]> getPickWallpaperLauncherOrNull() {
    return pickWallpaperLauncher;
  }

  @Nullable
  ActivityResultLauncher<String[]> getPickKeyFontLauncherOrNull() {
    return pickKeyFontLauncher;
  }

  @Nullable
  ActivityResultLauncher<String> getExportPresetLauncherOrNull() {
    return exportPresetLauncher;
  }

  @Nullable
  ActivityResultLauncher<String[]> getImportPresetLauncherOrNull() {
    return importPresetLauncher;
  }

  @Nullable
  KeyboardTheme getCurrentTheme() {
    final var context = getContext();
    if (context == null) return null;
    return NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
  }

  @NonNull
  String resolvePresetId(@NonNull KeyboardTheme theme) {
    final String baseThemeId = theme.getId();
    final KeyboardThemePresetStore store = presetStore;
    return store != null ? store.getActivePresetId(baseThemeId) : baseThemeId;
  }

  void refreshState() {
    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    final KeyboardThemeCustomizationOverlaysSection overlays = overlaysSection;
    if (overlays != null) {
      overlays.refreshState();
    }

    final String baseThemeId = theme.getId();
    final String themeId = resolvePresetId(theme);
    final KeyboardThemeCustomizationPresetsSection presets = presetsSection;
    final KeyboardThemeCustomizationTypographySection typography = typographySection;
    final KeyboardThemeCustomizationBackgroundSection background = backgroundSection;
    final boolean importInProgress =
        (background != null && background.isWallpaperImportInProgress())
            || (typography != null && typography.isFontImportInProgress())
            || (presets != null && presets.isPresetTransferInProgress());
    if (presets != null) {
      presets.refreshState(baseThemeId, themeId, importInProgress);
    }
    if (background != null) {
      background.refreshState(themeId, importInProgress);
    }

    final KeyboardThemeCustomizationColorsSection colors = colorsSection;
    if (colors != null) {
      colors.refreshState(themeId, importInProgress);
    }

    if (typography != null) {
      typography.refreshState(themeId, importInProgress);
    }
    final KeyboardThemeCustomizationShadowsSection shadows = shadowsSection;
    if (shadows != null) {
      shadows.refreshState(themeId);
    }

    final boolean hasAnyWallpaperOverride =
        background != null && background.hasAnyWallpaperOverride(themeId);
    final boolean hasAnyColorOverride = colors != null && colors.hasAnyColorOverride(themeId);
    final boolean hasAnyTypographyOverride =
        typography != null && typography.hasAnyTypographyOverride(themeId);
    final boolean hasAnyShadowsOverride = shadows != null && shadows.hasAnyShadowsOverride(themeId);
    final boolean hasAnyAppearanceOverride =
        hasAnyColorOverride || hasAnyTypographyOverride || hasAnyShadowsOverride;

    if (presets != null) {
      presets.refreshOverridesSummary(
          themeId,
          importInProgress,
          hasAnyWallpaperOverride,
          hasAnyColorOverride,
          hasAnyTypographyOverride,
          hasAnyShadowsOverride);
    }

    final KeyboardThemeCustomizationResetSection reset = resetSection;
    if (reset != null) {
      reset.refreshState(importInProgress, hasAnyWallpaperOverride, hasAnyAppearanceOverride);
    }

    updateLivePreview();
  }

  void updateResetEnabledStates(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore overridesStore = themeOverridesStore;
    final KeyboardThemeCustomizationTypographySection typography = typographySection;
    final KeyboardThemeCustomizationPresetsSection presets = presetsSection;
    final KeyboardThemeCustomizationBackgroundSection background = backgroundSection;

    final boolean importInProgress =
        (background != null && background.isWallpaperImportInProgress())
            || (typography != null && typography.isFontImportInProgress())
            || (presets != null && presets.isPresetTransferInProgress());
    final boolean hasAnyWallpaperOverride =
        background != null && background.hasAnyWallpaperOverride(themeId);

    final KeyboardThemeCustomizationColorsSection colors = colorsSection;
    final boolean hasAnyColorOverride = colors != null && colors.hasAnyColorOverride(themeId);
    final boolean hasAnyTypographyOverride =
        typography != null && typography.hasAnyTypographyOverride(themeId);
    final KeyboardThemeCustomizationShadowsSection shadows = shadowsSection;
    final boolean hasAnyShadowsOverride =
        shadows != null
            ? shadows.hasAnyShadowsOverride(themeId)
            : overridesStore != null
                && (overridesStore.getKeyTextShadowColor(themeId) != null
                    || overridesStore.getTokenSecondaryTextShadowColor(themeId) != null
                    || overridesStore.getTokenSecondaryTextShadowRadiusDp(themeId) != null
                    || overridesStore.getTokenSecondaryTextShadowOffsetXDp(themeId) != null
                    || overridesStore.getTokenSecondaryTextShadowOffsetYDp(themeId) != null
                    || overridesStore.getTokenSecondaryKeyBackgroundShadowColor(themeId) != null
                    || overridesStore.getTokenSecondaryKeyBackgroundShadowOffsetXDp(themeId) != null
                    || overridesStore.getTokenSecondaryKeyBackgroundShadowOffsetYDp(themeId) != null
                    || overridesStore.getTokenSecondaryKeyBackgroundShadowSpreadDp(themeId) != null
                    || overridesStore.isKeyTextShadowUseTokenSecondary(themeId)
                    || overridesStore.isSpecialKeyTextShadowUseTokenSecondary(themeId)
                    || overridesStore.isSpacebarKeyTextShadowUseTokenSecondary(themeId)
                    || overridesStore.isModifierKeyTextShadowUseTokenSecondary(themeId)
                    || overridesStore.isEnterKeyTextShadowUseTokenSecondary(themeId)
                    || overridesStore.isKeyBackgroundShadowUseTokenSecondary(themeId)
                    || overridesStore.isSpecialKeyBackgroundShadowUseTokenSecondary(themeId)
                    || overridesStore.isSpacebarKeyBackgroundShadowUseTokenSecondary(themeId)
                    || overridesStore.isModifierKeyBackgroundShadowUseTokenSecondary(themeId)
                    || overridesStore.isEnterKeyBackgroundShadowUseTokenSecondary(themeId)
                    || overridesStore.getKeyTextShadowRadiusDp(themeId) != null
                    || overridesStore.getKeyTextShadowOffsetXDp(themeId) != null
                    || overridesStore.getKeyTextShadowOffsetYDp(themeId) != null
                    || overridesStore.getSpecialKeyTextShadowColor(themeId) != null
                    || overridesStore.getSpecialKeyTextShadowRadiusDp(themeId) != null
                    || overridesStore.getSpecialKeyTextShadowOffsetXDp(themeId) != null
                    || overridesStore.getSpecialKeyTextShadowOffsetYDp(themeId) != null
                    || overridesStore.getSpacebarKeyTextShadowColor(themeId) != null
                    || overridesStore.getSpacebarKeyTextShadowRadiusDp(themeId) != null
                    || overridesStore.getSpacebarKeyTextShadowOffsetXDp(themeId) != null
                    || overridesStore.getSpacebarKeyTextShadowOffsetYDp(themeId) != null
                    || overridesStore.getModifierKeyTextShadowColor(themeId) != null
                    || overridesStore.getModifierKeyTextShadowRadiusDp(themeId) != null
                    || overridesStore.getModifierKeyTextShadowOffsetXDp(themeId) != null
                    || overridesStore.getModifierKeyTextShadowOffsetYDp(themeId) != null
                    || overridesStore.getEnterKeyTextShadowColor(themeId) != null
                    || overridesStore.getEnterKeyTextShadowRadiusDp(themeId) != null
                    || overridesStore.getEnterKeyTextShadowOffsetXDp(themeId) != null
                    || overridesStore.getEnterKeyTextShadowOffsetYDp(themeId) != null
                    || overridesStore.getKeyBackgroundShadowColor(themeId) != null
                    || overridesStore.getKeyBackgroundShadowOffsetXDp(themeId) != null
                    || overridesStore.getKeyBackgroundShadowOffsetYDp(themeId) != null
                    || overridesStore.getKeyBackgroundShadowSpreadDp(themeId) != null
                    || overridesStore.getSpecialKeyBackgroundShadowColor(themeId) != null
                    || overridesStore.getSpecialKeyBackgroundShadowOffsetXDp(themeId) != null
                    || overridesStore.getSpecialKeyBackgroundShadowOffsetYDp(themeId) != null
                    || overridesStore.getSpacebarKeyBackgroundShadowColor(themeId) != null
                    || overridesStore.getSpacebarKeyBackgroundShadowOffsetXDp(themeId) != null
                    || overridesStore.getSpacebarKeyBackgroundShadowOffsetYDp(themeId) != null
                    || overridesStore.getModifierKeyBackgroundShadowColor(themeId) != null
                    || overridesStore.getModifierKeyBackgroundShadowOffsetXDp(themeId) != null
                    || overridesStore.getModifierKeyBackgroundShadowOffsetYDp(themeId) != null
                    || overridesStore.getEnterKeyBackgroundShadowColor(themeId) != null
                    || overridesStore.getEnterKeyBackgroundShadowOffsetXDp(themeId) != null
                    || overridesStore.getEnterKeyBackgroundShadowOffsetYDp(themeId) != null);

    final boolean hasAnyAppearanceOverride =
        hasAnyColorOverride || hasAnyTypographyOverride || hasAnyShadowsOverride;

    final KeyboardThemeCustomizationResetSection reset = resetSection;
    if (reset != null) {
      reset.refreshState(importInProgress, hasAnyWallpaperOverride, hasAnyAppearanceOverride);
    }
  }

  void updateLivePreview() {
    final KeyboardThemeCustomizationLivePreviewSection preview = livePreviewSection;
    if (preview == null) return;
    preview.updateLivePreview();
  }

  private void onPresetExportUri(@Nullable Uri uri) {
    final KeyboardThemeCustomizationPresetsSection presets = presetsSection;
    if (presets == null) return;
    presets.onPresetExportUri(uri);
  }

  private void onPresetImportUri(@Nullable Uri uri) {
    final KeyboardThemeCustomizationPresetsSection presets = presetsSection;
    if (presets == null) return;
    presets.onPresetImportUri(uri);
  }

  private void onCustomKeyFontPicked(@Nullable Uri uri) {
    final Context context = getContext();
    final KeyboardThemeCustomizationTypographySection typography = typographySection;
    if (context == null || typography == null) return;
    typography.onCustomKeyFontPicked(context, uri);
  }

  void scheduleEnsureReadableUpdateIfEnabled(@NonNull String themeId) {
    final KeyboardThemeCustomizationColorsSection colors = colorsSection;
    if (colors == null) return;
    colors.scheduleEnsureReadableUpdateIfEnabled(themeId);
  }

  void attachColorPickerDialog(@NonNull EditTextPreference preference) {
    preference.setOnPreferenceClickListener(
        ignored -> {
          suggestLivePreviewFocusForScrollTarget(preference.getKey());
          KeyboardThemeCustomizationArgbColorPickerDialog.show(preference.getContext(), preference);
          return true;
        });
  }

  @Override
  public void scrollToPreference(@NonNull String key) {
    suggestLivePreviewFocusForScrollTarget(key);
    super.scrollToPreference(key);
  }

  private void suggestLivePreviewFocusForScrollTarget(@Nullable String key) {
    if (key == null) return;
    final KeyboardThemeCustomizationLivePreviewSection preview = livePreviewSection;
    if (preview == null) return;
    preview.setEditorSuggestedFocus(focusForScrollTarget(key));
  }

  @NonNull
  private static KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea focusForScrollTarget(
      @NonNull String key) {
    if (key.startsWith("section:overlays")
        || key.contains("night_mode")
        || key.contains("power_save")
        || key.contains("apply_remote_app_colors")) {
      return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.OVERLAYS;
    }

    if (key.startsWith("section:background")
        || key.startsWith("keyboard_theme_wallpaper_customization")) {
      return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.BACKGROUND;
    }

    if (key.startsWith("section:typography")
        || key.contains("font")
        || key.contains("text_size")
        || key.contains("auto_fit")
        || key.contains("ellipsize")) {
      return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.TEXT;
    }

    if (key.startsWith("section:shadows") || key.contains("shadow")) {
      return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.TEXT;
    }

    if (key.contains("suggestion")) {
      return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.SUGGESTIONS;
    }

    if (key.startsWith("section:colors")
        || key.startsWith("keyboard_theme_override_")
        || key.startsWith("keyboard_theme_appearance_")) {
      return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.KEYS;
    }

    return KeyboardThemeCustomizationLivePreviewSection.PreviewFocusArea.KEYS;
  }
}
