package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SeekBarPreference;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.File;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperLayer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemeCustomizationWallpaperPhotoUi {

  @NonNull private final KeyboardThemeCustomizationBackgroundSection.Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;

  @Nullable private Preference pickPhotoPref;
  @Nullable private CheckBoxPreference highQualityImportPref;
  @Nullable private SeekBarPreference saturationPref;
  @Nullable private SeekBarPreference contrastPref;
  @Nullable private SeekBarPreference brightnessPref;
  @Nullable private SeekBarPreference temperaturePref;
  @Nullable private Preference backgroundLayerStackPref;
  @Nullable private ListPreference scaleModePref;
  @Nullable private ListPreference anchorPref;
  @Nullable private Preference rotatePhotoPref;

  @Nullable private Disposable previewDisposable;

  KeyboardThemeCustomizationWallpaperPhotoUi(
      @NonNull KeyboardThemeCustomizationBackgroundSection.Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory background) {
    pickPhotoPref = new Preference(context);
    pickPhotoPref.setKey("keyboard_theme_wallpaper_customization_pick");
    pickPhotoPref.setTitle(R.string.keyboard_theme_wallpaper_customization_pick_title);
    pickPhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_pick_summary);
    pickPhotoPref.setOnPreferenceClickListener(
        ignored -> {
          final ActivityResultLauncher<String[]> launcher = host.getPickWallpaperLauncher();
          if (launcher == null) {
            showPickFailedDialog(
                context, new IllegalStateException("Wallpaper picker is not available."));
            host.refreshState();
            return true;
          }
          try {
            launcher.launch(new String[] {"image/*"});
          } catch (ActivityNotFoundException e) {
            showPickFailedDialog(context, e);
            host.refreshState();
          }
          return true;
        });
    background.addPreference(pickPhotoPref);

    highQualityImportPref = new CheckBoxPreference(context);
    highQualityImportPref.setKey("keyboard_theme_wallpaper_customization_high_quality_import");
    highQualityImportPref.setPersistent(false);
    highQualityImportPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_high_quality_title);
    highQualityImportPref.setSummary(
        R.string.keyboard_theme_wallpaper_customization_high_quality_summary);
    highQualityImportPref.setChecked(wallpaperStore.isHighQualityImportEnabled());
    highQualityImportPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          wallpaperStore.setHighQualityImportEnabled(Boolean.TRUE.equals(newValue));
          return true;
        });
    background.addPreference(highQualityImportPref);

    rotatePhotoPref = new Preference(context);
    rotatePhotoPref.setKey("keyboard_theme_wallpaper_customization_rotate");
    rotatePhotoPref.setTitle(R.string.keyboard_theme_wallpaper_customization_rotate_title);
    rotatePhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_rotate_summary);
    rotatePhotoPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          wallpaperStore.rotateWallpaperClockwise90(themeId);
          android.widget.Toast.makeText(
                  context,
                  R.string.keyboard_theme_wallpaper_customization_rotate_toast,
                  android.widget.Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    background.addPreference(rotatePhotoPref);

    scaleModePref = new ListPreference(context);
    scaleModePref.setKey("keyboard_theme_wallpaper_customization_scale_mode");
    scaleModePref.setPersistent(false);
    scaleModePref.setTitle(R.string.keyboard_theme_wallpaper_customization_scale_title);
    final CharSequence scaleModeSummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_scale_summary);
    scaleModePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return scaleModeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return scaleModeSummaryBase;
          return scaleModeSummaryBase + "\n" + entry;
        });
    scaleModePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_crop),
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_fit),
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_stretch),
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_tile),
          context.getString(R.string.keyboard_theme_wallpaper_customization_scale_mirror)
        });
    scaleModePref.setEntryValues(new CharSequence[] {"0", "1", "2", "3", "4"});
    scaleModePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          try {
            wallpaperStore.setWallpaperScaleMode(
                themeId, Integer.parseInt(String.valueOf(newValue)));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(scaleModePref);

    anchorPref = new ListPreference(context);
    anchorPref.setKey("keyboard_theme_wallpaper_customization_anchor");
    anchorPref.setPersistent(false);
    anchorPref.setTitle(R.string.keyboard_theme_wallpaper_customization_anchor_title);
    final CharSequence anchorSummaryBase =
        context.getText(R.string.keyboard_theme_wallpaper_customization_anchor_summary);
    anchorPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return anchorSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return anchorSummaryBase;
          return anchorSummaryBase + "\n" + entry;
        });
    anchorPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_top_left),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_top),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_top_right),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_left),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_center),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_right),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom_left),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom),
          context.getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom_right)
        });
    anchorPref.setEntryValues(new CharSequence[] {"0", "1", "2", "3", "4", "5", "6", "7", "8"});
    anchorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          try {
            wallpaperStore.setWallpaperAnchor(themeId, Integer.parseInt(String.valueOf(newValue)));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(anchorPref);

    saturationPref = new SeekBarPreference(context);
    saturationPref.setKey("keyboard_theme_wallpaper_customization_saturation");
    saturationPref.setPersistent(false);
    saturationPref.setTitle(R.string.keyboard_theme_wallpaper_customization_saturation_title);
    saturationPref.setSummary(R.string.keyboard_theme_wallpaper_customization_saturation_summary);
    saturationPref.setMin(0);
    saturationPref.setMax(200);
    saturationPref.setShowSeekBarValue(true);
    saturationPref.setUpdatesContinuously(true);
    saturationPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setSaturationPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(saturationPref);

    contrastPref = new SeekBarPreference(context);
    contrastPref.setKey("keyboard_theme_wallpaper_customization_contrast");
    contrastPref.setPersistent(false);
    contrastPref.setTitle(R.string.keyboard_theme_wallpaper_customization_contrast_title);
    contrastPref.setSummary(R.string.keyboard_theme_wallpaper_customization_contrast_summary);
    contrastPref.setMin(0);
    contrastPref.setMax(200);
    contrastPref.setShowSeekBarValue(true);
    contrastPref.setUpdatesContinuously(true);
    contrastPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setContrastPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(contrastPref);

    brightnessPref = new SeekBarPreference(context);
    brightnessPref.setKey("keyboard_theme_wallpaper_customization_brightness");
    brightnessPref.setPersistent(false);
    brightnessPref.setTitle(R.string.keyboard_theme_wallpaper_customization_brightness_title);
    brightnessPref.setSummary(R.string.keyboard_theme_wallpaper_customization_brightness_summary);
    brightnessPref.setMin(0);
    brightnessPref.setMax(200);
    brightnessPref.setShowSeekBarValue(true);
    brightnessPref.setUpdatesContinuously(true);
    brightnessPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setBrightnessPercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(brightnessPref);

    temperaturePref = new SeekBarPreference(context);
    temperaturePref.setKey("keyboard_theme_wallpaper_customization_temperature");
    temperaturePref.setPersistent(false);
    temperaturePref.setTitle(R.string.keyboard_theme_wallpaper_customization_temperature_title);
    temperaturePref.setSummary(R.string.keyboard_theme_wallpaper_customization_temperature_summary);
    temperaturePref.setMin(0);
    temperaturePref.setMax(200);
    temperaturePref.setShowSeekBarValue(true);
    temperaturePref.setUpdatesContinuously(true);
    temperaturePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return false;
          wallpaperStore.setTemperaturePercent(themeId, (Integer) newValue);
          host.updateLivePreview();
          return true;
        });
    background.addPreference(temperaturePref);

    backgroundLayerStackPref = new Preference(context);
    backgroundLayerStackPref.setKey(
        "keyboard_theme_wallpaper_customization_background_layer_stack");
    backgroundLayerStackPref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_background_layer_stack_title);
    backgroundLayerStackPref.setSummary(
        R.string.keyboard_theme_wallpaper_customization_background_layer_stack_summary);
    backgroundLayerStackPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.show(
              context, wallpaperStore, host, themeId, false /*keyLayers*/);
          return true;
        });
    background.addPreference(backgroundLayerStackPref);
  }

  void setWallpaperImportInProgressUi() {
    if (pickPhotoPref != null) {
      pickPhotoPref.setEnabled(false);
      pickPhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_pick_summary_saving);
    }
    if (highQualityImportPref != null) highQualityImportPref.setEnabled(false);
    if (rotatePhotoPref != null) rotatePhotoPref.setEnabled(false);
    if (scaleModePref != null) scaleModePref.setEnabled(false);
    if (anchorPref != null) anchorPref.setEnabled(false);
    if (saturationPref != null) saturationPref.setEnabled(false);
    if (contrastPref != null) contrastPref.setEnabled(false);
    if (brightnessPref != null) brightnessPref.setEnabled(false);
    if (temperaturePref != null) temperaturePref.setEnabled(false);
    if (backgroundLayerStackPref != null) backgroundLayerStackPref.setEnabled(false);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    final Context context = pickPhotoPref != null ? pickPhotoPref.getContext() : null;
    if (context == null) return;

    final boolean hasPhoto = wallpaperStore.hasWallpaper(themeId);
    final boolean isInvalid = wallpaperStore.isWallpaperInvalid(themeId);
    final int saturation = wallpaperStore.getSaturationPercent(themeId);
    final int contrast = wallpaperStore.getContrastPercent(themeId);
    final int brightness = wallpaperStore.getBrightnessPercent(themeId);
    final int temperature = wallpaperStore.getTemperaturePercent(themeId);
    final int scaleMode = wallpaperStore.getWallpaperScaleMode(themeId);
    final int anchor = wallpaperStore.getWallpaperAnchor(themeId);
    final int rotationDegrees = wallpaperStore.getWallpaperRotationDegrees(themeId);
    final KeyboardWallpaperLayer[] backgroundLayerStack =
        wallpaperStore.getBackgroundLayerStack(themeId);

    if (pickPhotoPref != null) {
      pickPhotoPref.setEnabled(!importInProgress);
      pickPhotoPref.setSummary(
          context.getString(
              isInvalid
                  ? R.string.keyboard_theme_wallpaper_customization_pick_summary_invalid
                  : hasPhoto
                      ? R.string.keyboard_theme_wallpaper_customization_pick_summary_set
                      : R.string.keyboard_theme_wallpaper_customization_pick_summary));
    }

    final int previewDimPercent = wallpaperStore.getDimPercent(themeId);
    refreshPhotoPreview(
        themeId,
        hasPhoto,
        isInvalid,
        previewDimPercent,
        rotationDegrees,
        scaleMode,
        anchor,
        saturation,
        contrast,
        brightness,
        temperature);

    if (highQualityImportPref != null) {
      highQualityImportPref.setChecked(wallpaperStore.isHighQualityImportEnabled());
      highQualityImportPref.setEnabled(!importInProgress);
    }

    if (saturationPref != null) {
      saturationPref.setEnabled(hasPhoto && !isInvalid && !importInProgress);
      saturationPref.setValue(saturation);
    }

    if (contrastPref != null) {
      contrastPref.setEnabled(hasPhoto && !isInvalid && !importInProgress);
      contrastPref.setValue(contrast);
    }

    if (brightnessPref != null) {
      brightnessPref.setEnabled(hasPhoto && !isInvalid && !importInProgress);
      brightnessPref.setValue(brightness);
    }

    if (temperaturePref != null) {
      temperaturePref.setEnabled(hasPhoto && !isInvalid && !importInProgress);
      temperaturePref.setValue(temperature);
    }

    if (backgroundLayerStackPref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      backgroundLayerStackPref.setEnabled(enabled);
      backgroundLayerStackPref.setSummary(
          context.getString(
                  R.string.keyboard_theme_wallpaper_customization_background_layer_stack_summary)
              + "\n"
              + KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.describeLayerStack(
                  context, backgroundLayerStack));
    }

    if (rotatePhotoPref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      rotatePhotoPref.setEnabled(enabled);
    }

    if (scaleModePref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      scaleModePref.setEnabled(enabled);
      scaleModePref.setValue(String.valueOf(scaleMode));
    }

    if (anchorPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && (scaleMode == KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP
                  || scaleMode == KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT);
      anchorPref.setEnabled(enabled);
      anchorPref.setValue(String.valueOf(anchor));
    }
  }

  void showPickFailedDialog(@NonNull Context context, @NonNull Throwable error) {
    final int messageResId;
    if (error instanceof ActivityNotFoundException) {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_no_picker;
    } else if (error instanceof SecurityException) {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_permission;
    } else if (error.getCause() instanceof OutOfMemoryError) {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_too_large;
    } else {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_generic;
    }

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_theme_wallpaper_customization_pick_failed_title)
        .setMessage(messageResId)
        .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
        .show();
  }

  void dispose() {
    disposePreview();
  }

  private void disposePreview() {
    if (previewDisposable != null) {
      previewDisposable.dispose();
      previewDisposable = null;
    }
  }

  private void refreshPhotoPreview(
      @NonNull String themeId,
      boolean hasPhoto,
      boolean isInvalid,
      int dimPercent,
      int rotationDegrees,
      int scaleMode,
      int anchor,
      int saturationPercent,
      int contrastPercent,
      int brightnessPercent,
      int temperaturePercent) {
    if (pickPhotoPref == null) return;

    if (!hasPhoto || isInvalid) {
      disposePreview();
      pickPhotoPref.setIcon(null);
      pickPhotoPref.setIconSpaceReserved(false);
      return;
    }

    final File file = wallpaperStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      pickPhotoPref.setIcon(null);
      pickPhotoPref.setIconSpaceReserved(false);
      return;
    }

    // Avoid re-decoding if a prior load is in-flight; we only ever show the current theme.
    disposePreview();

    final var context = pickPhotoPref.getContext();
    final float density = context.getResources().getDisplayMetrics().density;
    final int sizePx = Math.max(64, Math.round(64f * density));
    final int clampedDim = Math.max(0, Math.min(100, dimPercent));
    final int normalizedRotation =
        KeyboardWallpaperOverrideConstants.normalizeRotationDegrees(rotationDegrees);

    previewDisposable =
        Single.fromCallable(
                () ->
                    KeyboardThemeCustomizationWallpaperPreview.createDrawable(
                        file,
                        sizePx,
                        clampedDim,
                        normalizedRotation,
                        scaleMode,
                        anchor,
                        saturationPercent,
                        contrastPercent,
                        brightnessPercent,
                        temperaturePercent))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                drawable -> {
                  if (!host.isAdded() || pickPhotoPref == null) return;
                  if (drawable == null) {
                    pickPhotoPref.setIcon(null);
                    pickPhotoPref.setIconSpaceReserved(false);
                  } else {
                    pickPhotoPref.setIcon(drawable);
                    pickPhotoPref.setIconSpaceReserved(true);
                  }
                },
                ignored -> {
                  if (!host.isAdded() || pickPhotoPref == null) return;
                  pickPhotoPref.setIcon(null);
                  pickPhotoPref.setIconSpaceReserved(false);
                });
  }
}
