package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.File;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.app.theme.WallpaperColorSchemeExtractor;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemeCustomizationAutoPhotoThemeDialog {

  @NonNull private final KeyboardThemeCustomizationColorsSection.Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private Preference autoThemeFromPhotoPref;
  @Nullable private Preference highContrastDarkPref;

  @Nullable private Disposable autoThemeFromPhotoDisposable;

  KeyboardThemeCustomizationAutoPhotoThemeDialog(
      @NonNull KeyboardThemeCustomizationColorsSection.Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory colors) {
    autoThemeFromPhotoPref = new Preference(context);
    autoThemeFromPhotoPref.setKey("keyboard_theme_appearance_auto_photo_colors");
    autoThemeFromPhotoPref.setPersistent(false);
    autoThemeFromPhotoPref.setTitle(R.string.keyboard_theme_appearance_auto_photo_colors_title);
    autoThemeFromPhotoPref.setSummary(R.string.keyboard_theme_appearance_auto_photo_colors_summary);
    autoThemeFromPhotoPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          if (!wallpaperStore.hasWallpaper(themeId) || wallpaperStore.isWallpaperInvalid(themeId)) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_auto_photo_colors_no_photo_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return true;
          }
          showAutoPhotoThemeDialog(context, themeId);
          return true;
        });
    colors.addPreference(autoThemeFromPhotoPref);

    highContrastDarkPref = new Preference(context);
    highContrastDarkPref.setKey("keyboard_theme_appearance_high_contrast_dark");
    highContrastDarkPref.setPersistent(false);
    highContrastDarkPref.setTitle(R.string.keyboard_theme_appearance_high_contrast_dark_title);
    highContrastDarkPref.setSummary(R.string.keyboard_theme_appearance_high_contrast_dark_summary);
    highContrastDarkPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          new AlertDialog.Builder(context)
              .setTitle(R.string.keyboard_theme_appearance_high_contrast_dark_dialog_title)
              .setMessage(R.string.keyboard_theme_appearance_high_contrast_dark_dialog_message)
              .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
              .setPositiveButton(
                  R.string.keyboard_theme_appearance_high_contrast_dark_dialog_apply,
                  (dialog, which) -> {
                    dialog.dismiss();
                    applyHighContrastDark(context, themeId);
                  })
              .show();
          return true;
        });
    colors.addPreference(highContrastDarkPref);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    final boolean hasPhoto = wallpaperStore.hasWallpaper(themeId);
    final boolean isInvalid = wallpaperStore.isWallpaperInvalid(themeId);
    if (autoThemeFromPhotoPref != null) {
      autoThemeFromPhotoPref.setEnabled(!importInProgress && hasPhoto && !isInvalid);
    }
    if (highContrastDarkPref != null) {
      highContrastDarkPref.setEnabled(!importInProgress);
    }
  }

  void dispose() {
    if (autoThemeFromPhotoDisposable != null) {
      autoThemeFromPhotoDisposable.dispose();
      autoThemeFromPhotoDisposable = null;
    }
  }

  private void showAutoPhotoThemeDialog(@NonNull Context context, @NonNull String themeId) {
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    final int padding = KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 16);
    root.setPadding(padding, padding, padding, padding);

    final TextView message = new TextView(context);
    message.setText(R.string.keyboard_theme_appearance_auto_photo_colors_dialog_message);
    root.addView(
        message,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final TextView paletteTitle = new TextView(context);
    paletteTitle.setText(R.string.keyboard_theme_appearance_auto_photo_colors_palette_title);
    final LinearLayout.LayoutParams paletteTitleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    paletteTitleParams.topMargin = KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 12);
    root.addView(paletteTitle, paletteTitleParams);

    final WallpaperColorSchemeExtractor.PaletteSource[] paletteSources =
        new WallpaperColorSchemeExtractor.PaletteSource[] {
          WallpaperColorSchemeExtractor.PaletteSource.DOMINANT,
          WallpaperColorSchemeExtractor.PaletteSource.VIBRANT,
          WallpaperColorSchemeExtractor.PaletteSource.MUTED,
          WallpaperColorSchemeExtractor.PaletteSource.DARK_VIBRANT,
          WallpaperColorSchemeExtractor.PaletteSource.LIGHT_VIBRANT,
          WallpaperColorSchemeExtractor.PaletteSource.DARK_MUTED,
          WallpaperColorSchemeExtractor.PaletteSource.LIGHT_MUTED
        };
    final String[] paletteLabels =
        new String[] {
          context.getString(R.string.keyboard_theme_appearance_auto_photo_colors_palette_dominant),
          context.getString(R.string.keyboard_theme_appearance_auto_photo_colors_palette_vibrant),
          context.getString(R.string.keyboard_theme_appearance_auto_photo_colors_palette_muted),
          context.getString(
              R.string.keyboard_theme_appearance_auto_photo_colors_palette_dark_vibrant),
          context.getString(
              R.string.keyboard_theme_appearance_auto_photo_colors_palette_light_vibrant),
          context.getString(
              R.string.keyboard_theme_appearance_auto_photo_colors_palette_dark_muted),
          context.getString(
              R.string.keyboard_theme_appearance_auto_photo_colors_palette_light_muted)
        };

    final Spinner paletteSpinner = new Spinner(context);
    final ArrayAdapter<String> paletteAdapter =
        new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, paletteLabels);
    paletteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    paletteSpinner.setAdapter(paletteAdapter);
    root.addView(
        paletteSpinner,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final TextView intensityTitle = new TextView(context);
    intensityTitle.setText(R.string.keyboard_theme_appearance_auto_photo_colors_intensity_title);
    final LinearLayout.LayoutParams intensityTitleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    intensityTitleParams.topMargin = KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 12);
    root.addView(intensityTitle, intensityTitleParams);

    final TextView intensityValue = new TextView(context);
    intensityValue.setText(
        context.getString(
            R.string.keyboard_theme_appearance_auto_photo_colors_intensity_value, 100));
    root.addView(
        intensityValue,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final SeekBar intensity = new SeekBar(context);
    intensity.setMax(100);
    intensity.setProgress(100);
    root.addView(
        intensity,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    intensity.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intensityValue.setText(
                context.getString(
                    R.string.keyboard_theme_appearance_auto_photo_colors_intensity_value,
                    progress));
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    final CheckBox applyKeyboardBackground = new CheckBox(context);
    applyKeyboardBackground.setText(
        R.string.keyboard_theme_appearance_auto_photo_colors_apply_background_title);
    applyKeyboardBackground.setChecked(true);
    root.addView(
        applyKeyboardBackground,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final CheckBox applyKeyBackground = new CheckBox(context);
    applyKeyBackground.setText(
        R.string.keyboard_theme_appearance_auto_photo_colors_apply_key_background_title);
    applyKeyBackground.setChecked(true);
    root.addView(
        applyKeyBackground,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final CheckBox applySpecialKeys = new CheckBox(context);
    applySpecialKeys.setText(
        R.string.keyboard_theme_appearance_auto_photo_colors_apply_special_keys_title);
    applySpecialKeys.setChecked(true);
    root.addView(
        applySpecialKeys,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final CheckBox applyTextColors = new CheckBox(context);
    applyTextColors.setText(R.string.keyboard_theme_appearance_auto_photo_colors_apply_text_title);
    applyTextColors.setChecked(true);
    root.addView(
        applyTextColors,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final CheckBox applyTextShadow = new CheckBox(context);
    applyTextShadow.setText(
        R.string.keyboard_theme_appearance_auto_photo_colors_apply_shadow_title);
    applyTextShadow.setChecked(true);
    root.addView(
        applyTextShadow,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final TextView specialIntensityTitle = new TextView(context);
    specialIntensityTitle.setText(
        R.string.keyboard_theme_appearance_auto_photo_colors_special_intensity_title);
    final LinearLayout.LayoutParams specialIntensityTitleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    specialIntensityTitleParams.topMargin =
        KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 12);
    specialIntensityTitleParams.leftMargin =
        KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 16);
    root.addView(specialIntensityTitle, specialIntensityTitleParams);

    final TextView specialIntensityValue = new TextView(context);
    specialIntensityValue.setText(
        context.getString(
            R.string.keyboard_theme_appearance_auto_photo_colors_intensity_value, 100));
    final LinearLayout.LayoutParams specialIntensityValueParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    specialIntensityValueParams.leftMargin =
        KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 16);
    root.addView(specialIntensityValue, specialIntensityValueParams);

    final SeekBar specialIntensity = new SeekBar(context);
    specialIntensity.setMax(100);
    specialIntensity.setProgress(100);
    final LinearLayout.LayoutParams specialIntensityParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    specialIntensityParams.leftMargin = KeyboardThemeCustomizationColorUiUtil.dpToPx(context, 16);
    root.addView(specialIntensity, specialIntensityParams);

    specialIntensity.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            specialIntensityValue.setText(
                context.getString(
                    R.string.keyboard_theme_appearance_auto_photo_colors_intensity_value,
                    progress));
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    final AlertDialog dialog =
        new AlertDialog.Builder(context)
            .setTitle(R.string.keyboard_theme_appearance_auto_photo_colors_dialog_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setPositiveButton(
                R.string.keyboard_theme_appearance_auto_photo_colors_dialog_apply, null)
            .create();

    dialog.setOnShowListener(
        ignored -> {
          final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
          if (button == null) return;
          button.setOnClickListener(
              ignoredClick -> {
                final boolean applyKeyboardBackgroundChecked = applyKeyboardBackground.isChecked();
                final boolean applyKeyBackgroundChecked = applyKeyBackground.isChecked();
                final boolean applyTextColorsChecked = applyTextColors.isChecked();
                applyAutoPhotoThemeColors(
                    context,
                    themeId,
                    paletteSources[
                        Math.max(
                            0,
                            Math.min(
                                paletteSources.length - 1,
                                paletteSpinner.getSelectedItemPosition()))],
                    intensity.getProgress(),
                    specialIntensity.getProgress(),
                    applyKeyboardBackgroundChecked,
                    applyKeyBackgroundChecked,
                    applySpecialKeys.isChecked() && applyKeyBackgroundChecked,
                    applyTextColorsChecked,
                    applyTextShadow.isChecked() && applyTextColorsChecked);
                dialog.dismiss();
              });
        });
    dialog.show();
  }

  private void applyAutoPhotoThemeColors(
      @NonNull Context context,
      @NonNull String themeId,
      @NonNull WallpaperColorSchemeExtractor.PaletteSource paletteSource,
      int intensityPercent,
      int specialKeysIntensityPercent,
      boolean applyKeyboardBackground,
      boolean applyKeyBackground,
      boolean applySpecialKeys,
      boolean applyTextColors,
      boolean applyTextShadow) {
    if (themeOverridesStore == null) return;

    final File file = wallpaperStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      Toast.makeText(
              context,
              R.string.keyboard_theme_appearance_auto_photo_colors_no_photo_toast,
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    if (autoThemeFromPhotoDisposable != null) {
      autoThemeFromPhotoDisposable.dispose();
      autoThemeFromPhotoDisposable = null;
    }

    final int dimPercent = wallpaperStore.getDimPercent(themeId);
    final int clampedIntensity = Math.max(0, Math.min(100, intensityPercent));
    final int clampedSpecialKeysIntensity = Math.max(0, Math.min(100, specialKeysIntensityPercent));
    final float specialKeysMix = clampedSpecialKeysIntensity / 100f;

    autoThemeFromPhotoDisposable =
        Single.fromCallable(
                () -> WallpaperColorSchemeExtractor.extract(file, dimPercent, paletteSource))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                result -> {
                  if (!host.isAdded() || themeOverridesStore == null) return;

                  if (applyKeyboardBackground) {
                    themeOverridesStore.setKeyboardBackgroundTint(
                        themeId, result.keyboardBackgroundTint);
                    themeOverridesStore.setKeyboardBackgroundOpacityPercent(
                        themeId,
                        KeyboardThemeCustomizationColorUiUtil.scalePercent(
                            result.keyboardBackgroundOpacityPercent, clampedIntensity));
                  }

                  if (applyKeyBackground) {
                    themeOverridesStore.setKeyBackgroundTint(themeId, result.keyBackgroundTint);
                    if (applySpecialKeys) {
                      final int specialKeyTint =
                          KeyboardThemeCustomizationColorUiUtil.blendColors(
                              result.keyBackgroundTint,
                              result.specialKeyBackgroundTint,
                              specialKeysMix);
                      final int spacebarTint =
                          KeyboardThemeCustomizationColorUiUtil.blendColors(
                              result.keyBackgroundTint,
                              result.spacebarBackgroundTint,
                              specialKeysMix);
                      themeOverridesStore.setSpecialKeyBackgroundTint(themeId, specialKeyTint);
                      themeOverridesStore.setModifierKeyBackgroundTint(themeId, specialKeyTint);
                      themeOverridesStore.setEnterKeyBackgroundTint(themeId, specialKeyTint);
                      themeOverridesStore.setSpacebarBackgroundTint(themeId, spacebarTint);
                    }
                    themeOverridesStore.setKeyBackgroundOpacityPercent(
                        themeId,
                        KeyboardThemeCustomizationColorUiUtil.scalePercent(
                            result.keyBackgroundOpacityPercent, clampedIntensity));
                  }

                  if (applyTextColors) {
                    themeOverridesStore.setKeyTextColor(themeId, result.keyTextColor);
                    themeOverridesStore.setSpecialKeyTextColor(themeId, result.keyTextColor);
                    themeOverridesStore.setModifierKeyTextColor(themeId, result.keyTextColor);
                    themeOverridesStore.setEnterKeyTextColor(themeId, result.keyTextColor);
                    themeOverridesStore.setSpacebarTextColor(themeId, result.keyTextColor);
                    themeOverridesStore.setHintTextColor(themeId, result.hintTextColor);
                  }

                  if (applyTextShadow) {
                    themeOverridesStore.setKeyTextShadowColor(themeId, result.keyTextShadowColor);
                    themeOverridesStore.setKeyTextShadowRadiusDp(themeId, 1);
                    themeOverridesStore.setKeyTextShadowOffsetXDp(themeId, 0);
                    themeOverridesStore.setKeyTextShadowOffsetYDp(themeId, 1);
                  }

                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_appearance_auto_photo_colors_applied_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                  host.refreshState();
                },
                error -> {
                  if (!host.isAdded()) return;
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_appearance_auto_photo_colors_failed_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                });
  }

  private void applyHighContrastDark(@NonNull Context context, @NonNull String themeId) {
    if (themeOverridesStore == null) return;

    themeOverridesStore.setKeyTextColor(themeId, Color.WHITE);
    themeOverridesStore.setSpecialKeyTextColor(themeId, Color.WHITE);
    themeOverridesStore.setModifierKeyTextColor(themeId, Color.WHITE);
    themeOverridesStore.setEnterKeyTextColor(themeId, Color.WHITE);
    themeOverridesStore.setSpacebarTextColor(themeId, Color.WHITE);
    themeOverridesStore.setHintTextColor(themeId, 0xBFFFFFFF);

    themeOverridesStore.setKeyboardBackgroundTint(themeId, Color.BLACK);
    themeOverridesStore.setKeyBackgroundTint(themeId, Color.BLACK);
    themeOverridesStore.setModifierKeyBackgroundTint(themeId, Color.BLACK);
    themeOverridesStore.setEnterKeyBackgroundTint(themeId, Color.BLACK);
    themeOverridesStore.setKeyboardBackgroundOpacityPercent(themeId, 100);
    themeOverridesStore.setKeyBackgroundOpacityPercent(themeId, 100);

    themeOverridesStore.setKeyTextShadowColor(themeId, 0xB0000000);
    themeOverridesStore.setKeyTextShadowRadiusDp(themeId, 1);
    themeOverridesStore.setKeyTextShadowOffsetXDp(themeId, 0);
    themeOverridesStore.setKeyTextShadowOffsetYDp(themeId, 1);

    Toast.makeText(
            context,
            R.string.keyboard_theme_appearance_high_contrast_dark_toast,
            Toast.LENGTH_SHORT)
        .show();
    host.refreshState();
  }
}
