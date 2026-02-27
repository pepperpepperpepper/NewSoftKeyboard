package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.util.concurrent.TimeUnit;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemeCustomizationReadabilityController {

  @NonNull private final KeyboardThemeCustomizationColorsSection.Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private Preference readabilityStatusPref;
  @Nullable private CheckBoxPreference ensureReadableTextPref;
  @Nullable private Preference autoReadableColorsPref;

  @Nullable private Disposable autoReadableDisposable;
  @Nullable private Disposable ensureReadableDebounceDisposable;
  @Nullable private Disposable readabilityCheckDisposable;

  @Nullable private String lastReadabilityCacheKey;
  @Nullable private AutoReadableColors lastReadabilityColors;

  KeyboardThemeCustomizationReadabilityController(
      @NonNull KeyboardThemeCustomizationColorsSection.Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory colors) {
    readabilityStatusPref = new Preference(context);
    readabilityStatusPref.setKey("keyboard_theme_appearance_readability_status");
    readabilityStatusPref.setPersistent(false);
    readabilityStatusPref.setSelectable(false);
    readabilityStatusPref.setTitle(R.string.keyboard_theme_appearance_readability_status_title);
    readabilityStatusPref.setSummary(
        R.string.keyboard_theme_appearance_readability_status_no_photo_summary);
    colors.addPreference(readabilityStatusPref);

    ensureReadableTextPref = new CheckBoxPreference(context);
    ensureReadableTextPref.setKey("keyboard_theme_appearance_ensure_readable_text");
    ensureReadableTextPref.setPersistent(false);
    ensureReadableTextPref.setTitle(R.string.keyboard_theme_appearance_ensure_readable_text_title);
    ensureReadableTextPref.setSummary(
        R.string.keyboard_theme_appearance_ensure_readable_text_summary);
    ensureReadableTextPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final boolean enabled = Boolean.TRUE.equals(newValue);
          if (!enabled) {
            themeOverridesStore.clearEnsureReadableTextEnabled(themeId);
            host.refreshState();
            return true;
          }

          if (!wallpaperStore.hasWallpaper(themeId) || wallpaperStore.isWallpaperInvalid(themeId)) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_auto_readable_no_photo_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          }

          themeOverridesStore.setEnsureReadableTextEnabled(themeId, true);
          host.refreshState();
          applyAutoReadableTextColors(themeId, /* includeShadow= */ false, /* showToast= */ true);
          return true;
        });
    colors.addPreference(ensureReadableTextPref);

    autoReadableColorsPref = new Preference(context);
    autoReadableColorsPref.setKey("keyboard_theme_appearance_auto_readable_colors");
    autoReadableColorsPref.setPersistent(false);
    autoReadableColorsPref.setTitle(R.string.keyboard_theme_appearance_auto_readable_title);
    autoReadableColorsPref.setSummary(R.string.keyboard_theme_appearance_auto_readable_summary);
    autoReadableColorsPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          if (!wallpaperStore.hasWallpaper(themeId) || wallpaperStore.isWallpaperInvalid(themeId)) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_auto_readable_no_photo_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return true;
          }
          new AlertDialog.Builder(context)
              .setTitle(R.string.keyboard_theme_appearance_auto_readable_dialog_title)
              .setMessage(R.string.keyboard_theme_appearance_auto_readable_dialog_message)
              .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
              .setPositiveButton(
                  R.string.keyboard_theme_appearance_auto_readable_dialog_apply,
                  (dialog, which) -> {
                    dialog.dismiss();
                    applyAutoReadableTextColors(themeId);
                  })
              .show();
          return true;
        });
    colors.addPreference(autoReadableColorsPref);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    final boolean hasPhoto = wallpaperStore.hasWallpaper(themeId);
    final boolean isInvalid = wallpaperStore.isWallpaperInvalid(themeId);
    final int dimPercent = wallpaperStore.getDimPercent(themeId);

    if (autoReadableColorsPref != null) {
      autoReadableColorsPref.setEnabled(!importInProgress && hasPhoto && !isInvalid);
    }

    final boolean ensureReadableEnabled =
        themeOverridesStore != null && themeOverridesStore.isEnsureReadableTextEnabled(themeId);
    if (ensureReadableTextPref != null) {
      ensureReadableTextPref.setEnabled(
          !importInProgress && (ensureReadableEnabled || (hasPhoto && !isInvalid)));
      ensureReadableTextPref.setChecked(ensureReadableEnabled);
    }

    final Integer keyTextColor =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyTextColor(themeId) != null
                ? themeOverridesStore.getKeyTextColor(themeId)
                : themeOverridesStore.getTokenPrimaryTextColor(themeId)
            : null;

    refreshReadabilityStatus(
        themeId,
        hasPhoto,
        isInvalid,
        importInProgress,
        dimPercent,
        keyTextColor,
        ensureReadableEnabled);
  }

  void scheduleUpdateIfEnabled(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    final KeyboardWallpaperOverrideStore wallpaper = wallpaperStore;
    if (store == null || !store.isEnsureReadableTextEnabled(themeId)) return;
    if (!wallpaper.hasWallpaper(themeId) || wallpaper.isWallpaperInvalid(themeId)) return;

    if (ensureReadableDebounceDisposable != null) {
      ensureReadableDebounceDisposable.dispose();
      ensureReadableDebounceDisposable = null;
    }

    ensureReadableDebounceDisposable =
        Single.timer(300, TimeUnit.MILLISECONDS, RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                ignored -> {
                  final KeyboardThemeUserOverridesStore latestStore = themeOverridesStore;
                  if (latestStore == null || !latestStore.isEnsureReadableTextEnabled(themeId))
                    return;
                  if (!wallpaper.hasWallpaper(themeId) || wallpaper.isWallpaperInvalid(themeId))
                    return;
                  applyAutoReadableTextColors(
                      themeId, /* includeShadow= */ false, /* showToast= */ false);
                },
                ignored -> {});
  }

  void dispose() {
    if (autoReadableDisposable != null) {
      autoReadableDisposable.dispose();
      autoReadableDisposable = null;
    }
    if (ensureReadableDebounceDisposable != null) {
      ensureReadableDebounceDisposable.dispose();
      ensureReadableDebounceDisposable = null;
    }
    if (readabilityCheckDisposable != null) {
      readabilityCheckDisposable.dispose();
      readabilityCheckDisposable = null;
    }
  }

  private void applyAutoReadableTextColors(@NonNull String themeId) {
    applyAutoReadableTextColors(themeId, /* includeShadow= */ true, /* showToast= */ true);
  }

  private void applyAutoReadableTextColors(
      @NonNull String themeId, boolean includeShadow, boolean showToast) {
    if (themeOverridesStore == null) return;
    final Context context =
        KeyboardThemeCustomizationColorUiUtil.preferenceContext(readabilityStatusPref);
    if (context == null) return;

    final File file = wallpaperStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      if (showToast) {
        Toast.makeText(
                context,
                R.string.keyboard_theme_appearance_auto_readable_no_photo_toast,
                Toast.LENGTH_SHORT)
            .show();
      }
      return;
    }

    if (autoReadableDisposable != null) {
      autoReadableDisposable.dispose();
      autoReadableDisposable = null;
    }

    final int dimPercent = wallpaperStore.getDimPercent(themeId);

    autoReadableDisposable =
        Single.fromCallable(() -> computeAutoReadableColors(file, dimPercent))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                result -> {
                  if (!host.isAdded() || themeOverridesStore == null) return;
                  themeOverridesStore.setKeyTextColor(themeId, result.textColor);
                  themeOverridesStore.setSpecialKeyTextColor(themeId, result.textColor);
                  themeOverridesStore.setModifierKeyTextColor(themeId, result.textColor);
                  themeOverridesStore.setEnterKeyTextColor(themeId, result.textColor);
                  themeOverridesStore.setSpacebarTextColor(themeId, result.textColor);
                  themeOverridesStore.setHintTextColor(themeId, result.hintColor);
                  if (includeShadow) {
                    themeOverridesStore.setKeyTextShadowColor(themeId, result.shadowColor);
                    themeOverridesStore.setKeyTextShadowRadiusDp(themeId, 1);
                    themeOverridesStore.setKeyTextShadowOffsetXDp(themeId, 0);
                    themeOverridesStore.setKeyTextShadowOffsetYDp(themeId, 1);
                  }
                  if (showToast) {
                    Toast.makeText(
                            context,
                            R.string.keyboard_theme_appearance_auto_readable_applied_toast,
                            Toast.LENGTH_SHORT)
                        .show();
                  }
                  host.refreshState();
                },
                error -> {
                  if (!host.isAdded()) return;
                  if (showToast) {
                    Toast.makeText(
                            context,
                            R.string.keyboard_theme_appearance_auto_readable_failed_toast,
                            Toast.LENGTH_SHORT)
                        .show();
                  }
                });
  }

  private void refreshReadabilityStatus(
      @NonNull String themeId,
      boolean hasPhoto,
      boolean isInvalid,
      boolean importInProgress,
      int dimPercent,
      @Nullable Integer keyTextColor,
      boolean ensureReadableEnabled) {
    final Preference pref = readabilityStatusPref;
    final Context context = KeyboardThemeCustomizationColorUiUtil.preferenceContext(pref);
    if (pref == null || context == null) return;

    if (!hasPhoto) {
      lastReadabilityCacheKey = null;
      lastReadabilityColors = null;
      pref.setSummary(R.string.keyboard_theme_appearance_readability_status_no_photo_summary);
      return;
    }
    if (isInvalid) {
      lastReadabilityCacheKey = null;
      lastReadabilityColors = null;
      pref.setSummary(R.string.keyboard_theme_appearance_readability_status_invalid_photo_summary);
      return;
    }
    if (importInProgress) {
      pref.setSummary(R.string.keyboard_theme_appearance_readability_status_analyzing_summary);
      return;
    }

    final File file = wallpaperStore.getWallpaperFile(themeId);
    final String cacheKey = file.getAbsolutePath() + ":" + file.lastModified() + ":" + dimPercent;
    final boolean cacheKeyChanged = !cacheKey.equals(lastReadabilityCacheKey);
    final boolean computeInProgress =
        readabilityCheckDisposable != null && !readabilityCheckDisposable.isDisposed();

    if (cacheKeyChanged || (!computeInProgress && lastReadabilityColors == null)) {
      if (readabilityCheckDisposable != null) {
        readabilityCheckDisposable.dispose();
        readabilityCheckDisposable = null;
      }
      lastReadabilityCacheKey = cacheKey;
      lastReadabilityColors = null;

      readabilityCheckDisposable =
          Single.fromCallable(() -> computeAutoReadableColors(file, dimPercent))
              .subscribeOn(RxSchedulers.background())
              .observeOn(RxSchedulers.mainThread())
              .subscribe(
                  result -> {
                    if (!host.isAdded()) return;
                    lastReadabilityColors = result;
                    host.refreshState();
                  },
                  ignored -> {
                    if (!host.isAdded()) return;
                    lastReadabilityCacheKey = null;
                    lastReadabilityColors = null;
                    host.refreshState();
                  });
    }

    final AutoReadableColors recommended = lastReadabilityColors;
    if (recommended == null) {
      pref.setSummary(R.string.keyboard_theme_appearance_readability_status_analyzing_summary);
      return;
    }

    final int photoAverageColor = recommended.photoAverageColor;
    final DemoKeyboardView preview = host.getLivePreviewKeyboardView();
    final int effectiveKeyTextColor =
        preview != null
            ? preview.getCurrentResourcesHolder().getKeyTextColor().getDefaultColor()
            : keyTextColor != null ? keyTextColor : recommended.textColor;
    final int effectiveHintTextColor =
        preview != null
            ? preview.getCurrentResourcesHolder().getHintTextColor()
            : recommended.hintColor;
    final int effectiveSpacebarTextColor =
        preview != null
            ? preview.getCurrentResourcesHolder().getNameTextColor()
            : effectiveKeyTextColor;

    final KeyboardThemeUserOverridesStore overridesStore = themeOverridesStore;
    final Integer specialKeyTextOverride =
        overridesStore != null
            ? overridesStore.getSpecialKeyTextColor(themeId) != null
                ? overridesStore.getSpecialKeyTextColor(themeId)
                : overridesStore.getTokenAccentColor(themeId)
            : null;
    final Integer modifierKeyTextOverride =
        overridesStore != null ? overridesStore.getModifierKeyTextColor(themeId) : null;
    final Integer enterKeyTextOverride =
        overridesStore != null ? overridesStore.getEnterKeyTextColor(themeId) : null;

    final int effectiveSpecialKeyTextColor =
        specialKeyTextOverride != null ? specialKeyTextOverride : effectiveKeyTextColor;
    final int effectiveModifierKeyTextColor =
        modifierKeyTextOverride != null ? modifierKeyTextOverride : effectiveSpecialKeyTextColor;
    final int effectiveEnterKeyTextColor =
        enterKeyTextOverride != null ? enterKeyTextOverride : effectiveSpecialKeyTextColor;

    final StringBuilder summary = new StringBuilder();
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_photo_average_summary,
            KeyboardThemeCustomizationColorUiUtil.formatColor(photoAverageColor)));
    summary.append("\n");
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_recommended_summary,
            KeyboardThemeCustomizationColorUiUtil.formatColor(recommended.textColor)));
    summary.append("\n");
    summary.append(
        context.getString(
            ensureReadableEnabled
                ? R.string.keyboard_theme_appearance_readability_status_auto_on_summary
                : R.string.keyboard_theme_appearance_readability_status_auto_off_summary));
    summary.append("\n");
    if (preview != null || keyTextColor != null) {
      summary.append(
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_current_key_summary,
              KeyboardThemeCustomizationColorUiUtil.formatColor(effectiveKeyTextColor)));
    } else {
      summary.append(
          context.getString(
              R.string.keyboard_theme_appearance_readability_status_current_key_default_summary));
    }
    summary.append("\n");

    final double keyContrast = contrastRatio(effectiveKeyTextColor, photoAverageColor);
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_line_summary,
            context.getString(
                R.string.keyboard_theme_appearance_readability_status_contrast_keys_label),
            keyContrast,
            contrastLabel(context, keyContrast)));
    summary.append("\n");

    final double specialContrast = contrastRatio(effectiveSpecialKeyTextColor, photoAverageColor);
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_line_summary,
            context.getString(
                R.string.keyboard_theme_appearance_readability_status_contrast_special_keys_label),
            specialContrast,
            contrastLabel(context, specialContrast)));
    summary.append("\n");

    final double modifierContrast = contrastRatio(effectiveModifierKeyTextColor, photoAverageColor);
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_line_summary,
            context.getString(
                R.string.keyboard_theme_appearance_readability_status_contrast_modifier_keys_label),
            modifierContrast,
            contrastLabel(context, modifierContrast)));
    summary.append("\n");

    final double enterContrast = contrastRatio(effectiveEnterKeyTextColor, photoAverageColor);
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_line_summary,
            context.getString(
                R.string.keyboard_theme_appearance_readability_status_contrast_enter_key_label),
            enterContrast,
            contrastLabel(context, enterContrast)));
    summary.append("\n");

    final double hintContrast = contrastRatio(effectiveHintTextColor, photoAverageColor);
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_line_summary,
            context.getString(
                R.string.keyboard_theme_appearance_readability_status_contrast_hints_label),
            hintContrast,
            contrastLabel(context, hintContrast)));
    summary.append("\n");

    final double spacebarContrast = contrastRatio(effectiveSpacebarTextColor, photoAverageColor);
    summary.append(
        context.getString(
            R.string.keyboard_theme_appearance_readability_status_contrast_line_summary,
            context.getString(
                R.string.keyboard_theme_appearance_readability_status_contrast_spacebar_label),
            spacebarContrast,
            contrastLabel(context, spacebarContrast)));

    if (!ensureReadableEnabled && keyContrast < 3.0d) {
      summary.append("\n");
      summary.append(
          context.getString(R.string.keyboard_theme_appearance_readability_status_warning_summary));
    }

    pref.setSummary(summary.toString());
  }

  private static AutoReadableColors computeAutoReadableColors(@NonNull File file, int dimPercent) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    int sample = 1;
    final int maxDim = Math.max(options.outWidth, options.outHeight);
    while (maxDim > 0 && (maxDim / sample) > 64) {
      sample *= 2;
    }
    options.inJustDecodeBounds = false;
    options.inSampleSize = sample;
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

    final Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    if (bitmap == null) return AutoReadableColors.fallback();

    try {
      final int width = bitmap.getWidth();
      final int height = bitmap.getHeight();
      if (width <= 0 || height <= 0) return AutoReadableColors.fallback();

      final int step = Math.max(1, Math.min(width, height) / 32);
      long luminanceSum = 0L;
      long rSum = 0L;
      long gSum = 0L;
      long bSum = 0L;
      long count = 0L;
      for (int y = 0; y < height; y += step) {
        for (int x = 0; x < width; x += step) {
          final int c = bitmap.getPixel(x, y);
          final int r = (c >> 16) & 0xFF;
          final int g = (c >> 8) & 0xFF;
          final int b = c & 0xFF;
          final int lum = (r * 2126 + g * 7152 + b * 722) / 10000;
          luminanceSum += lum;
          rSum += r;
          gSum += g;
          bSum += b;
          count++;
        }
      }
      if (count <= 0) return AutoReadableColors.fallback();

      final float dimFactor = (100f - Math.max(0, Math.min(100, dimPercent))) / 100f;
      final float avgLum = (luminanceSum / (float) count) * dimFactor;
      final int avgR = clamp8(Math.round((rSum / (float) count) * dimFactor));
      final int avgG = clamp8(Math.round((gSum / (float) count) * dimFactor));
      final int avgB = clamp8(Math.round((bSum / (float) count) * dimFactor));
      final int avgColor = Color.rgb(avgR, avgG, avgB);
      final boolean dark = avgLum < 128f;
      final int textColor = dark ? Color.WHITE : Color.BLACK;
      final int hintColor = dark ? 0xCCFFFFFF : 0xCC000000;
      final int shadowColor = dark ? 0xB0000000 : 0xBFFFFFFF;
      return new AutoReadableColors(textColor, hintColor, shadowColor, avgColor);
    } finally {
      bitmap.recycle();
    }
  }

  private static int clamp8(int value) {
    return Math.max(0, Math.min(255, value));
  }

  private static final class AutoReadableColors {
    final int textColor;
    final int hintColor;
    final int shadowColor;
    final int photoAverageColor;

    AutoReadableColors(int textColor, int hintColor, int shadowColor, int photoAverageColor) {
      this.textColor = textColor;
      this.hintColor = hintColor;
      this.shadowColor = shadowColor;
      this.photoAverageColor = photoAverageColor;
    }

    static AutoReadableColors fallback() {
      return new AutoReadableColors(Color.WHITE, 0xCCFFFFFF, 0xB0000000, Color.BLACK);
    }
  }

  private static String contrastLabel(@NonNull Context context, double ratio) {
    if (ratio >= 4.5d) {
      return context.getString(
          R.string.keyboard_theme_appearance_readability_status_contrast_ok_label);
    } else if (ratio >= 3.0d) {
      return context.getString(
          R.string.keyboard_theme_appearance_readability_status_contrast_low_label);
    } else {
      return context.getString(
          R.string.keyboard_theme_appearance_readability_status_contrast_bad_label);
    }
  }

  private static double contrastRatio(int fg, int bg) {
    final double l1 = relativeLuminance(fg);
    final double l2 = relativeLuminance(bg);
    final double lighter = Math.max(l1, l2);
    final double darker = Math.min(l1, l2);
    return (lighter + 0.05d) / (darker + 0.05d);
  }

  private static double relativeLuminance(int argb) {
    final double r = srgbToLinear((argb >> 16) & 0xFF);
    final double g = srgbToLinear((argb >> 8) & 0xFF);
    final double b = srgbToLinear(argb & 0xFF);
    return (0.2126d * r) + (0.7152d * g) + (0.0722d * b);
  }

  private static double srgbToLinear(int channel) {
    final double v = Math.max(0d, Math.min(255d, channel)) / 255d;
    if (v <= 0.03928d) return v / 12.92d;
    return Math.pow((v + 0.055d) / 1.055d, 2.4d);
  }
}
