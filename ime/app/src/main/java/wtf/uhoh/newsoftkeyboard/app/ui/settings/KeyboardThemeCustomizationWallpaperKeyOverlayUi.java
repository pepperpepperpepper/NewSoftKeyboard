package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SeekBarPreference;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationWallpaperKeyOverlayUi {

  @NonNull private final KeyboardThemeCustomizationBackgroundSection.Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private ListPreference wallpaperModePref;
  @Nullable private SeekBarPreference keyOpacityPref;
  @Nullable private SeekBarPreference specialKeyOpacityPref;
  @Nullable private SeekBarPreference modifierKeyOpacityPref;
  @Nullable private SeekBarPreference enterKeyOpacityPref;
  @Nullable private SeekBarPreference spacebarOpacityPref;
  @Nullable private ListPreference keyBlendModePref;
  @Nullable private Preference keyLayerStackPref;
  @Nullable private ListPreference wallpaperQualityPref;
  @Nullable private CheckBoxPreference matchKeyShapePref;
  @Nullable private Preference tryNowPref;
  @Nullable private Preference resetPref;
  @Nullable private Preference applyToAllPref;

  KeyboardThemeCustomizationWallpaperKeyOverlayUi(
      @NonNull KeyboardThemeCustomizationBackgroundSection.Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(
      @NonNull Context context,
      @NonNull PreferenceCategory background,
      @NonNull
          KeyboardThemeCustomizationBackgroundPreferencesUi.ApplyToAllHandler applyToAllHandler) {
    wallpaperModePref = new ListPreference(context);
    wallpaperModePref.setKey("keyboard_theme_wallpaper_customization_mode");
    wallpaperModePref.setPersistent(false);
    wallpaperModePref.setTitle(R.string.keyboard_theme_wallpaper_customization_mode_title);
    final CharSequence wallpaperModeSummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_mode_summary);
    wallpaperModePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return wallpaperModeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return wallpaperModeSummaryBase;
          return wallpaperModeSummaryBase + "\n" + entry;
        });
    wallpaperModePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_wallpaper_customization_mode_background_only),
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_mode_background_key_tint),
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_mode_background_key_texture)
        });
    wallpaperModePref.setEntryValues(new CharSequence[] {"0", "1", "2"});
    wallpaperModePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          try {
            wallpaperStore.setWallpaperMode(themeId, Integer.parseInt(String.valueOf(newValue)));
            host.refreshState();
            host.updateLivePreview();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(wallpaperModePref);

    keyOpacityPref = new SeekBarPreference(context);
    keyOpacityPref.setKey("keyboard_theme_wallpaper_customization_key_opacity");
    keyOpacityPref.setPersistent(false);
    keyOpacityPref.setTitle(R.string.keyboard_theme_wallpaper_customization_key_opacity_title);
    final CharSequence keyOpacitySummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_key_opacity_summary);
    keyOpacityPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof SeekBarPreference seek)) return keyOpacitySummaryBase;
          return keyOpacitySummaryBase + "\n" + seek.getValue() + "%";
        });
    keyOpacityPref.setMin(0);
    keyOpacityPref.setMax(100);
    keyOpacityPref.setShowSeekBarValue(true);
    keyOpacityPref.setUpdatesContinuously(true);
    keyOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setKeyAlphaPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(keyOpacityPref);

    specialKeyOpacityPref = new SeekBarPreference(context);
    specialKeyOpacityPref.setKey("keyboard_theme_wallpaper_customization_special_key_opacity");
    specialKeyOpacityPref.setPersistent(false);
    specialKeyOpacityPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_special_key_opacity_title);
    final CharSequence specialKeyOpacitySummaryBase =
        context.getText(
            R.string.keyboard_theme_wallpaper_customization_special_key_opacity_summary);
    specialKeyOpacityPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof SeekBarPreference seek)) return specialKeyOpacitySummaryBase;
          return specialKeyOpacitySummaryBase + "\n" + seek.getValue() + "%";
        });
    specialKeyOpacityPref.setMin(0);
    specialKeyOpacityPref.setMax(100);
    specialKeyOpacityPref.setShowSeekBarValue(true);
    specialKeyOpacityPref.setUpdatesContinuously(true);
    specialKeyOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setSpecialKeyAlphaPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(specialKeyOpacityPref);

    modifierKeyOpacityPref = new SeekBarPreference(context);
    modifierKeyOpacityPref.setKey("keyboard_theme_wallpaper_customization_modifier_key_opacity");
    modifierKeyOpacityPref.setPersistent(false);
    modifierKeyOpacityPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_modifier_key_opacity_title);
    final CharSequence modifierKeyOpacitySummaryBase =
        context.getText(
            R.string.keyboard_theme_wallpaper_customization_modifier_key_opacity_summary);
    modifierKeyOpacityPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof SeekBarPreference seek)) return modifierKeyOpacitySummaryBase;
          return modifierKeyOpacitySummaryBase + "\n" + seek.getValue() + "%";
        });
    modifierKeyOpacityPref.setMin(0);
    modifierKeyOpacityPref.setMax(100);
    modifierKeyOpacityPref.setShowSeekBarValue(true);
    modifierKeyOpacityPref.setUpdatesContinuously(true);
    modifierKeyOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setModifierKeyAlphaPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(modifierKeyOpacityPref);

    enterKeyOpacityPref = new SeekBarPreference(context);
    enterKeyOpacityPref.setKey("keyboard_theme_wallpaper_customization_enter_key_opacity");
    enterKeyOpacityPref.setPersistent(false);
    enterKeyOpacityPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_enter_key_opacity_title);
    final CharSequence enterKeyOpacitySummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_enter_key_opacity_summary);
    enterKeyOpacityPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof SeekBarPreference seek)) return enterKeyOpacitySummaryBase;
          return enterKeyOpacitySummaryBase + "\n" + seek.getValue() + "%";
        });
    enterKeyOpacityPref.setMin(0);
    enterKeyOpacityPref.setMax(100);
    enterKeyOpacityPref.setShowSeekBarValue(true);
    enterKeyOpacityPref.setUpdatesContinuously(true);
    enterKeyOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setEnterKeyAlphaPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(enterKeyOpacityPref);

    spacebarOpacityPref = new SeekBarPreference(context);
    spacebarOpacityPref.setKey("keyboard_theme_wallpaper_customization_spacebar_opacity");
    spacebarOpacityPref.setPersistent(false);
    spacebarOpacityPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_spacebar_opacity_title);
    final CharSequence spacebarOpacitySummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_spacebar_opacity_summary);
    spacebarOpacityPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof SeekBarPreference seek)) return spacebarOpacitySummaryBase;
          return spacebarOpacitySummaryBase + "\n" + seek.getValue() + "%";
        });
    spacebarOpacityPref.setMin(0);
    spacebarOpacityPref.setMax(100);
    spacebarOpacityPref.setShowSeekBarValue(true);
    spacebarOpacityPref.setUpdatesContinuously(true);
    spacebarOpacityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setSpacebarAlphaPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(spacebarOpacityPref);

    keyBlendModePref = new ListPreference(context);
    keyBlendModePref.setKey("keyboard_theme_wallpaper_customization_key_blend_mode");
    keyBlendModePref.setPersistent(false);
    keyBlendModePref.setTitle(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_title);
    final CharSequence keyBlendModeSummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_summary);
    keyBlendModePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return keyBlendModeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return keyBlendModeSummaryBase;
          return keyBlendModeSummaryBase + "\n" + entry;
        });
    keyBlendModePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_normal),
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_key_blend_mode_multiply),
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_screen),
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_blend_mode_overlay),
          context.getString(
              R.string.keyboard_theme_wallpaper_customization_key_blend_mode_soft_light)
        });
    keyBlendModePref.setEntryValues(new CharSequence[] {"0", "1", "2", "3", "4"});
    keyBlendModePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          try {
            wallpaperStore.setKeyBlendMode(themeId, Integer.parseInt(String.valueOf(newValue)));
            host.updateLivePreview();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(keyBlendModePref);

    keyLayerStackPref = new Preference(context);
    keyLayerStackPref.setKey("keyboard_theme_wallpaper_customization_key_layer_stack");
    keyLayerStackPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_key_layer_stack_title);
    keyLayerStackPref.setSummary(
        R.string.keyboard_theme_wallpaper_customization_key_layer_stack_summary);
    keyLayerStackPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.show(
              context, wallpaperStore, host, themeId, true /*keyLayers*/);
          return true;
        });
    background.addPreference(keyLayerStackPref);

    wallpaperQualityPref = new ListPreference(context);
    wallpaperQualityPref.setKey("keyboard_theme_wallpaper_customization_quality");
    wallpaperQualityPref.setPersistent(false);
    wallpaperQualityPref.setTitle(R.string.keyboard_theme_wallpaper_customization_quality_title);
    final CharSequence wallpaperQualitySummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_quality_summary);
    wallpaperQualityPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return wallpaperQualitySummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return wallpaperQualitySummaryBase;
          return wallpaperQualitySummaryBase + "\n" + entry;
        });
    wallpaperQualityPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_wallpaper_customization_quality_low),
          context.getString(R.string.keyboard_theme_wallpaper_customization_quality_balanced),
          context.getString(R.string.keyboard_theme_wallpaper_customization_quality_high)
        });
    wallpaperQualityPref.setEntryValues(new CharSequence[] {"0", "1", "2"});
    wallpaperQualityPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          try {
            wallpaperStore.setWallpaperQuality(themeId, Integer.parseInt(String.valueOf(newValue)));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(wallpaperQualityPref);

    matchKeyShapePref = new CheckBoxPreference(context);
    matchKeyShapePref.setKey("keyboard_theme_wallpaper_customization_match_key_shape");
    matchKeyShapePref.setPersistent(false);
    matchKeyShapePref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_match_key_shape_title);
    matchKeyShapePref.setSummary(
        R.string.keyboard_theme_wallpaper_customization_match_key_shape_summary);
    matchKeyShapePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          final boolean enabled = Boolean.TRUE.equals(newValue);
          if (enabled
              && wallpaperStore.getWallpaperQuality(themeId)
                  != KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH) {
            wallpaperStore.setWallpaperQuality(
                themeId, KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH);
          }
          wallpaperStore.setMatchKeyShapeEnabled(themeId, enabled);
          host.refreshState();
          return true;
        });
    background.addPreference(matchKeyShapePref);

    tryNowPref = new Preference(context);
    tryNowPref.setKey("keyboard_theme_wallpaper_customization_try_now");
    tryNowPref.setTitle(R.string.keyboard_theme_wallpaper_customization_try_now_title);
    tryNowPref.setSummary(R.string.keyboard_theme_wallpaper_customization_try_now_summary);
    tryNowPref.setOnPreferenceClickListener(
        ignored -> {
          host.startTryNow();
          return true;
        });
    background.addPreference(tryNowPref);

    resetPref = new Preference(context);
    resetPref.setKey("keyboard_theme_wallpaper_customization_reset");
    resetPref.setTitle(R.string.keyboard_theme_wallpaper_customization_reset_title);
    resetPref.setSummary(R.string.keyboard_theme_wallpaper_customization_reset_summary);
    resetPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          wallpaperStore.clear(themeId);
          if (themeOverridesStore != null) {
            themeOverridesStore.clearEnsureReadableTextEnabled(themeId);
          }
          android.widget.Toast.makeText(
                  context,
                  R.string.keyboard_theme_wallpaper_customization_reset_toast,
                  android.widget.Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          host.updateLivePreview();
          return true;
        });
    background.addPreference(resetPref);

    applyToAllPref = new Preference(context);
    applyToAllPref.setKey("keyboard_theme_wallpaper_customization_apply_to_all");
    applyToAllPref.setTitle(R.string.keyboard_theme_wallpaper_customization_apply_to_all_title);
    applyToAllPref.setSummary(R.string.keyboard_theme_wallpaper_customization_apply_to_all_summary);
    applyToAllPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;

          if (!wallpaperStore.hasWallpaper(themeId) || wallpaperStore.isWallpaperInvalid(themeId)) {
            android.widget.Toast.makeText(
                    context,
                    R.string.keyboard_theme_wallpaper_customization_apply_to_all_pick_first_toast,
                    android.widget.Toast.LENGTH_SHORT)
                .show();
            return true;
          }

          new AlertDialog.Builder(context)
              .setTitle(R.string.keyboard_theme_wallpaper_customization_apply_to_all_dialog_title)
              .setMessage(
                  R.string.keyboard_theme_wallpaper_customization_apply_to_all_dialog_message)
              .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
              .setPositiveButton(
                  R.string.keyboard_theme_wallpaper_customization_apply_to_all_dialog_apply,
                  (dialog, which) -> {
                    dialog.dismiss();
                    applyToAllHandler.apply(context, themeId);
                  })
              .show();
          return true;
        });
    background.addPreference(applyToAllPref);
  }

  void setWallpaperImportInProgressUi() {
    if (wallpaperModePref != null) wallpaperModePref.setEnabled(false);
    if (keyOpacityPref != null) keyOpacityPref.setEnabled(false);
    if (specialKeyOpacityPref != null) specialKeyOpacityPref.setEnabled(false);
    if (modifierKeyOpacityPref != null) modifierKeyOpacityPref.setEnabled(false);
    if (enterKeyOpacityPref != null) enterKeyOpacityPref.setEnabled(false);
    if (spacebarOpacityPref != null) spacebarOpacityPref.setEnabled(false);
    if (keyBlendModePref != null) keyBlendModePref.setEnabled(false);
    if (keyLayerStackPref != null) keyLayerStackPref.setEnabled(false);
    if (wallpaperQualityPref != null) wallpaperQualityPref.setEnabled(false);
    if (matchKeyShapePref != null) matchKeyShapePref.setEnabled(false);
    if (tryNowPref != null) tryNowPref.setEnabled(false);
    if (resetPref != null) resetPref.setEnabled(false);
    if (applyToAllPref != null) applyToAllPref.setEnabled(false);
  }

  void refreshState(
      @NonNull String themeId, boolean importInProgress, boolean hasAnyWallpaperOverride) {
    final Context context = wallpaperModePref != null ? wallpaperModePref.getContext() : null;
    if (context == null) return;

    final boolean hasPhoto = wallpaperStore.hasWallpaper(themeId);
    final boolean isInvalid = wallpaperStore.isWallpaperInvalid(themeId);
    final int mode = wallpaperStore.getWallpaperMode(themeId);
    final int keyOpacityPercent = wallpaperStore.getKeyAlphaPercent(themeId);
    final int specialKeyOpacityPercent = wallpaperStore.getSpecialKeyAlphaPercent(themeId);
    final int modifierKeyOpacityPercent = wallpaperStore.getModifierKeyAlphaPercent(themeId);
    final int enterKeyOpacityPercent = wallpaperStore.getEnterKeyAlphaPercent(themeId);
    final int spacebarOpacityPercent = wallpaperStore.getSpacebarAlphaPercent(themeId);
    final int keyBlendMode = wallpaperStore.getKeyBlendMode(themeId);
    final boolean matchKeyShape = wallpaperStore.isMatchKeyShapeEnabled(themeId);
    final int wallpaperQuality = wallpaperStore.getWallpaperQuality(themeId);
    final boolean keyOverlayEnabled =
        hasPhoto
            && !isInvalid
            && !importInProgress
            && mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
    final KeyboardWallpaperLayer[] keyLayerStack = wallpaperStore.getKeyLayerStack(themeId);

    if (wallpaperModePref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      wallpaperModePref.setEnabled(enabled);
      wallpaperModePref.setValue(String.valueOf(mode));
    }

    if (keyOpacityPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
      keyOpacityPref.setEnabled(enabled);
      keyOpacityPref.setValue(keyOpacityPercent);
    }

    if (specialKeyOpacityPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
      specialKeyOpacityPref.setEnabled(enabled);
      specialKeyOpacityPref.setValue(specialKeyOpacityPercent);
    }

    if (modifierKeyOpacityPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
      modifierKeyOpacityPref.setEnabled(enabled);
      modifierKeyOpacityPref.setValue(modifierKeyOpacityPercent);
    }

    if (enterKeyOpacityPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
      enterKeyOpacityPref.setEnabled(enabled);
      enterKeyOpacityPref.setValue(enterKeyOpacityPercent);
    }

    if (spacebarOpacityPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
      spacebarOpacityPref.setEnabled(enabled);
      spacebarOpacityPref.setValue(spacebarOpacityPercent);
    }

    if (keyBlendModePref != null) {
      keyBlendModePref.setEnabled(keyOverlayEnabled);
      keyBlendModePref.setValue(String.valueOf(keyBlendMode));
    }

    if (keyLayerStackPref != null) {
      keyLayerStackPref.setEnabled(keyOverlayEnabled);
      keyLayerStackPref.setSummary(
          context.getString(R.string.keyboard_theme_wallpaper_customization_key_layer_stack_summary)
              + "\n"
              + KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.describeLayerStack(
                  context, keyLayerStack));
    }

    if (wallpaperQualityPref != null) {
      final boolean visible = hasPhoto && !isInvalid;
      wallpaperQualityPref.setEnabled(visible && !importInProgress);
      wallpaperQualityPref.setValue(String.valueOf(wallpaperQuality));
    }

    if (matchKeyShapePref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode == KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE;
      matchKeyShapePref.setEnabled(enabled);
      matchKeyShapePref.setChecked(matchKeyShape);
    }

    if (resetPref != null) {
      resetPref.setEnabled(!importInProgress && hasAnyWallpaperOverride);
    }

    if (applyToAllPref != null) {
      applyToAllPref.setEnabled(!importInProgress && hasPhoto && !isInvalid);
    }
  }

  void dispose() {}
}
