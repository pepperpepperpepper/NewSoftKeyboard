package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;

final class KeyboardThemeCustomizationFragmentHosts {

  static final class OverlaysSectionHost implements KeyboardThemeCustomizationOverlaysSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    OverlaysSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @NonNull
    @Override
    public View requireView() {
      return fragment.requireView();
    }
  }

  static final class LivePreviewSectionHost
      implements KeyboardThemeCustomizationLivePreviewSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    LivePreviewSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public Context getContext() {
      return fragment.getContext();
    }

    @Nullable
    @Override
    public KeyboardTheme getCurrentThemeOrNull() {
      return fragment.getCurrentTheme();
    }

    @NonNull
    @Override
    public String resolvePresetId(@NonNull KeyboardTheme theme) {
      return fragment.resolvePresetId(theme);
    }

    @Override
    public void scrollToPreference(@NonNull String key) {
      fragment.scrollToPreference(key);
    }
  }

  static final class PresetsSectionHost implements KeyboardThemeCustomizationPresetsSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    PresetsSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public String getBaseThemeIdOrNull() {
      final KeyboardTheme theme = fragment.getCurrentTheme();
      return theme != null ? theme.getId() : null;
    }

    @Override
    public boolean isAdded() {
      return fragment.isAdded();
    }

    @Override
    public void refreshState() {
      fragment.refreshState();
    }

    @Override
    public void updateLivePreview() {
      fragment.updateLivePreview();
    }

    @Override
    public void scrollToPreference(@NonNull String key) {
      fragment.scrollToPreference(key);
    }

    @Nullable
    @Override
    public DemoKeyboardView getLivePreviewKeyboardView() {
      return fragment.getLivePreviewKeyboardViewOrNull();
    }

    @Nullable
    @Override
    public ActivityResultLauncher<String> getExportPresetLauncher() {
      return fragment.getExportPresetLauncherOrNull();
    }

    @Nullable
    @Override
    public ActivityResultLauncher<String[]> getImportPresetLauncher() {
      return fragment.getImportPresetLauncherOrNull();
    }
  }

  static final class BackgroundSectionHost
      implements KeyboardThemeCustomizationBackgroundSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    BackgroundSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public String getActiveThemeIdOrNull() {
      final KeyboardTheme theme = fragment.getCurrentTheme();
      if (theme == null) return null;
      return fragment.resolvePresetId(theme);
    }

    @Override
    public boolean isAdded() {
      return fragment.isAdded();
    }

    @Override
    public void refreshState() {
      fragment.refreshState();
    }

    @Override
    public void updateLivePreview() {
      fragment.updateLivePreview();
    }

    @Override
    public void scheduleEnsureReadableUpdateIfEnabled(@NonNull String themeId) {
      fragment.scheduleEnsureReadableUpdateIfEnabled(themeId);
    }

    @Override
    public void startTryNow() {
      final Context context = fragment.getContext();
      if (context == null) return;
      fragment.startActivity(new Intent(context, TestInputActivity.class));
    }

    @Nullable
    @Override
    public ActivityResultLauncher<String[]> getPickWallpaperLauncher() {
      return fragment.getPickWallpaperLauncherOrNull();
    }

    @Override
    public void attachColorPickerDialog(@NonNull EditTextPreference preference) {
      fragment.attachColorPickerDialog(preference);
    }
  }

  static final class ColorsSectionHost implements KeyboardThemeCustomizationColorsSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    ColorsSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public String getActiveThemeIdOrNull() {
      final KeyboardTheme theme = fragment.getCurrentTheme();
      if (theme == null) return null;
      return fragment.resolvePresetId(theme);
    }

    @Override
    public boolean isAdded() {
      return fragment.isAdded();
    }

    @Override
    public void refreshState() {
      fragment.refreshState();
    }

    @Override
    public void updateLivePreview() {
      fragment.updateLivePreview();
    }

    @Override
    public void updateResetEnabledStates(@NonNull String themeId) {
      fragment.updateResetEnabledStates(themeId);
    }

    @Nullable
    @Override
    public DemoKeyboardView getLivePreviewKeyboardView() {
      return fragment.getLivePreviewKeyboardViewOrNull();
    }

    @Override
    public void attachColorPickerDialog(@NonNull EditTextPreference preference) {
      fragment.attachColorPickerDialog(preference);
    }
  }

  static final class TypographySectionHost
      implements KeyboardThemeCustomizationTypographySection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    TypographySectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public String getActiveThemeIdOrNull() {
      final KeyboardTheme theme = fragment.getCurrentTheme();
      if (theme == null) return null;
      return fragment.resolvePresetId(theme);
    }

    @Override
    public boolean isAdded() {
      return fragment.isAdded();
    }

    @Override
    public void refreshState() {
      fragment.refreshState();
    }

    @Nullable
    @Override
    public ActivityResultLauncher<String[]> getPickKeyFontLauncher() {
      return fragment.getPickKeyFontLauncherOrNull();
    }
  }

  static final class ShadowsSectionHost implements KeyboardThemeCustomizationShadowsSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    ShadowsSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public String getActiveThemeIdOrNull() {
      final KeyboardTheme theme = fragment.getCurrentTheme();
      if (theme == null) return null;
      return fragment.resolvePresetId(theme);
    }

    @Override
    public void refreshState() {
      fragment.refreshState();
    }

    @Override
    public void attachColorPickerDialog(@NonNull EditTextPreference preference) {
      fragment.attachColorPickerDialog(preference);
    }
  }

  static final class ResetSectionHost implements KeyboardThemeCustomizationResetSection.Host {

    @NonNull private final KeyboardThemeCustomizationFragment fragment;

    ResetSectionHost(@NonNull KeyboardThemeCustomizationFragment fragment) {
      this.fragment = fragment;
    }

    @Nullable
    @Override
    public String getActiveThemeIdOrNull() {
      final KeyboardTheme theme = fragment.getCurrentTheme();
      if (theme == null) return null;
      return fragment.resolvePresetId(theme);
    }

    @Override
    public void refreshState() {
      fragment.refreshState();
    }

    @Override
    public void updateLivePreview() {
      fragment.updateLivePreview();
    }
  }
}
