package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperTransform;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public class KeyboardThemeCustomizationFragment extends PreferenceFragmentCompat {

  private KeyboardWallpaperOverrideStore wallpaperStore;
  private KeyboardWallpaperResolver wallpaperPreviewResolver;
  private ActivityResultLauncher<String[]> pickWallpaperLauncher;
  private Disposable importDisposable;
  private Disposable previewDisposable;

  @Nullable private DemoKeyboardView livePreviewKeyboardView;

  private Preference pickPhotoPref;
  private CheckBoxPreference highQualityImportPref;
  private SeekBarPreference dimPref;
  private ListPreference scaleModePref;
  private ListPreference anchorPref;
  private ListPreference wallpaperModePref;
  private SeekBarPreference keyOpacityPref;
  private CheckBoxPreference matchKeyShapePref;
  private Preference tryNowPref;
  private Preference rotatePhotoPref;
  private Preference resetPref;
  private Preference applyToAllPref;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    wallpaperStore = new KeyboardWallpaperOverrideStore(context);
    wallpaperPreviewResolver = new KeyboardWallpaperResolver(context);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    pickWallpaperLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPhotoPicked);
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    final var context = requireContext();
    if (wallpaperStore == null) {
      wallpaperStore = new KeyboardWallpaperOverrideStore(context);
    }
    if (wallpaperPreviewResolver == null) {
      wallpaperPreviewResolver = new KeyboardWallpaperResolver(context);
    }
    final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    setPreferenceScreen(screen);

    final PreferenceCategory preview = new PreferenceCategory(context);
    preview.setTitle(R.string.keyboard_theme_wallpaper_customization_preview_title);
    screen.addPreference(preview);

    final KeyboardLivePreviewPreference previewPref =
        new KeyboardLivePreviewPreference(context, this::bindLivePreviewKeyboardView);
    previewPref.setKey("info:keyboard_theme_wallpaper_preview");
    preview.addPreference(previewPref);

    final PreferenceCategory background = new PreferenceCategory(context);
    background.setTitle(R.string.keyboard_theme_wallpaper_customization_title);
    screen.addPreference(background);

    pickPhotoPref = new Preference(context);
    pickPhotoPref.setTitle(R.string.keyboard_theme_wallpaper_customization_pick_title);
    pickPhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_pick_summary);
    pickPhotoPref.setOnPreferenceClickListener(
        ignored -> {
          if (pickWallpaperLauncher == null) {
            showPickFailedDialog(new IllegalStateException("Wallpaper picker is not available."));
            refreshState();
            return true;
          }
          try {
            pickWallpaperLauncher.launch(new String[] {"image/*"});
          } catch (ActivityNotFoundException e) {
            showPickFailedDialog(e);
            refreshState();
          }
          return true;
        });
    background.addPreference(pickPhotoPref);

    highQualityImportPref = new CheckBoxPreference(context);
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
    rotatePhotoPref.setTitle(R.string.keyboard_theme_wallpaper_customization_rotate_title);
    rotatePhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_rotate_summary);
    rotatePhotoPref.setOnPreferenceClickListener(
        ignored -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return true;
          wallpaperStore.rotateWallpaperClockwise90(theme.getId());
          Toast.makeText(
                  context,
                  R.string.keyboard_theme_wallpaper_customization_rotate_toast,
                  Toast.LENGTH_SHORT)
              .show();
          refreshState();
          return true;
        });
    background.addPreference(rotatePhotoPref);

    scaleModePref = new ListPreference(context);
    scaleModePref.setKey("keyboard_theme_wallpaper_customization_scale_mode");
    scaleModePref.setPersistent(false);
    scaleModePref.setTitle(R.string.keyboard_theme_wallpaper_customization_scale_title);
    final CharSequence scaleModeSummaryBase =
        getText(R.string.keyboard_theme_wallpaper_customization_scale_summary);
    scaleModePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return scaleModeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return scaleModeSummaryBase;
          return scaleModeSummaryBase + "\n" + entry;
        });
    scaleModePref.setEntries(
        new CharSequence[] {
          getString(R.string.keyboard_theme_wallpaper_customization_scale_crop),
          getString(R.string.keyboard_theme_wallpaper_customization_scale_fit),
          getString(R.string.keyboard_theme_wallpaper_customization_scale_stretch),
          getString(R.string.keyboard_theme_wallpaper_customization_scale_tile),
          getString(R.string.keyboard_theme_wallpaper_customization_scale_mirror)
        });
    scaleModePref.setEntryValues(new CharSequence[] {"0", "1", "2", "3", "4"});
    scaleModePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          try {
            wallpaperStore.setWallpaperScaleMode(
                theme.getId(), Integer.parseInt(String.valueOf(newValue)));
            refreshState();
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
        getText(R.string.keyboard_theme_wallpaper_customization_anchor_summary);
    anchorPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return anchorSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return anchorSummaryBase;
          return anchorSummaryBase + "\n" + entry;
        });
    anchorPref.setEntries(
        new CharSequence[] {
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_top_left),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_top),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_top_right),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_left),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_center),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_right),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom_left),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom),
          getString(R.string.keyboard_theme_wallpaper_customization_anchor_bottom_right)
        });
    anchorPref.setEntryValues(new CharSequence[] {"0", "1", "2", "3", "4", "5", "6", "7", "8"});
    anchorPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          try {
            wallpaperStore.setWallpaperAnchor(
                theme.getId(), Integer.parseInt(String.valueOf(newValue)));
            refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(anchorPref);

    dimPref = new SeekBarPreference(context);
    dimPref.setTitle(R.string.keyboard_theme_wallpaper_customization_dim_title);
    dimPref.setSummary(R.string.keyboard_theme_wallpaper_customization_dim_summary);
    dimPref.setMin(0);
    dimPref.setMax(100);
    dimPref.setShowSeekBarValue(true);
    dimPref.setUpdatesContinuously(true);
    dimPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          wallpaperStore.setDimPercent(theme.getId(), (Integer) newValue);
          updateLivePreview();
          return true;
        });
    background.addPreference(dimPref);

    wallpaperModePref = new ListPreference(context);
    wallpaperModePref.setKey("keyboard_theme_wallpaper_customization_mode");
    wallpaperModePref.setPersistent(false);
    wallpaperModePref.setTitle(R.string.keyboard_theme_wallpaper_customization_mode_title);
    final CharSequence wallpaperModeSummaryBase =
        getText(R.string.keyboard_theme_wallpaper_customization_mode_summary);
    wallpaperModePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return wallpaperModeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return wallpaperModeSummaryBase;
          return wallpaperModeSummaryBase + "\n" + entry;
        });
    wallpaperModePref.setEntries(
        new CharSequence[] {
          getString(R.string.keyboard_theme_wallpaper_customization_mode_background_only),
          getString(R.string.keyboard_theme_wallpaper_customization_mode_background_key_tint),
          getString(R.string.keyboard_theme_wallpaper_customization_mode_background_key_texture)
        });
    wallpaperModePref.setEntryValues(new CharSequence[] {"0", "1", "2"});
    wallpaperModePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          try {
            wallpaperStore.setWallpaperMode(
                theme.getId(), Integer.parseInt(String.valueOf(newValue)));
            refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    background.addPreference(wallpaperModePref);

    keyOpacityPref = new SeekBarPreference(context);
    keyOpacityPref.setTitle(R.string.keyboard_theme_wallpaper_customization_key_opacity_title);
    final CharSequence keyOpacitySummaryBase =
        getText(R.string.keyboard_theme_wallpaper_customization_key_opacity_summary);
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
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          wallpaperStore.setKeyAlphaPercent(theme.getId(), (Integer) newValue);
          updateLivePreview();
          return true;
        });
    background.addPreference(keyOpacityPref);

    matchKeyShapePref = new CheckBoxPreference(context);
    matchKeyShapePref.setTitle(
        R.string.keyboard_theme_wallpaper_customization_match_key_shape_title);
    matchKeyShapePref.setSummary(
        R.string.keyboard_theme_wallpaper_customization_match_key_shape_summary);
    matchKeyShapePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          wallpaperStore.setMatchKeyShapeEnabled(theme.getId(), Boolean.TRUE.equals(newValue));
          updateLivePreview();
          return true;
        });
    background.addPreference(matchKeyShapePref);

    tryNowPref = new Preference(context);
    tryNowPref.setTitle(R.string.keyboard_theme_wallpaper_customization_try_now_title);
    tryNowPref.setSummary(R.string.keyboard_theme_wallpaper_customization_try_now_summary);
    tryNowPref.setOnPreferenceClickListener(
        ignored -> {
          startActivity(new Intent(context, TestInputActivity.class));
          return true;
        });
    background.addPreference(tryNowPref);

    resetPref = new Preference(context);
    resetPref.setTitle(R.string.keyboard_theme_wallpaper_customization_reset_title);
    resetPref.setSummary(R.string.keyboard_theme_wallpaper_customization_reset_summary);
    resetPref.setOnPreferenceClickListener(
        ignored -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return true;
          wallpaperStore.clear(theme.getId());
          Toast.makeText(
                  context,
                  R.string.keyboard_theme_wallpaper_customization_reset_toast,
                  Toast.LENGTH_SHORT)
              .show();
          refreshState();
          updateLivePreview();
          return true;
        });
    background.addPreference(resetPref);

    applyToAllPref = new Preference(context);
    applyToAllPref.setTitle(R.string.keyboard_theme_wallpaper_customization_apply_to_all_title);
    applyToAllPref.setSummary(R.string.keyboard_theme_wallpaper_customization_apply_to_all_summary);
    applyToAllPref.setOnPreferenceClickListener(
        ignored -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return true;

          final String themeId = theme.getId();
          if (!wallpaperStore.hasWallpaper(themeId) || wallpaperStore.isWallpaperInvalid(themeId)) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_wallpaper_customization_apply_to_all_pick_first_toast,
                    Toast.LENGTH_SHORT)
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
                    applyWallpaperToAllThemes(themeId);
                  })
              .show();
          return true;
        });
    background.addPreference(applyToAllPref);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.keyboard_theme_wallpaper_customization_title);
    refreshState();
  }

  @Override
  public void onDestroy() {
    if (importDisposable != null) {
      importDisposable.dispose();
      importDisposable = null;
    }
    if (previewDisposable != null) {
      previewDisposable.dispose();
      previewDisposable = null;
    }
    livePreviewKeyboardView = null;
    super.onDestroy();
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshState();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    refreshState();
  }

  @Nullable
  private KeyboardTheme getCurrentTheme() {
    final var context = getContext();
    if (context == null) return null;
    return NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
  }

  private void refreshState() {
    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    final String themeId = theme.getId();
    final boolean importInProgress = importDisposable != null && !importDisposable.isDisposed();
    final boolean hasPhoto = wallpaperStore.hasWallpaper(themeId);
    final boolean isInvalid = wallpaperStore.isWallpaperInvalid(themeId);
    final int dim = wallpaperStore.getDimPercent(themeId);
    final int scaleMode = wallpaperStore.getWallpaperScaleMode(themeId);
    final int anchor = wallpaperStore.getWallpaperAnchor(themeId);
    final int mode = wallpaperStore.getWallpaperMode(themeId);
    final int keyOpacityPercent = wallpaperStore.getKeyAlphaPercent(themeId);
    final int rotationDegrees = wallpaperStore.getWallpaperRotationDegrees(themeId);
    final boolean matchKeyShape = wallpaperStore.isMatchKeyShapeEnabled(themeId);

    if (pickPhotoPref != null) {
      pickPhotoPref.setEnabled(!importInProgress);
      pickPhotoPref.setSummary(
          getString(
              isInvalid
                  ? R.string.keyboard_theme_wallpaper_customization_pick_summary_invalid
                  : hasPhoto
                      ? R.string.keyboard_theme_wallpaper_customization_pick_summary_set
                      : R.string.keyboard_theme_wallpaper_customization_pick_summary));
    }
    refreshPhotoPreview(themeId, hasPhoto, isInvalid, dim, rotationDegrees, scaleMode, anchor);

    if (highQualityImportPref != null) {
      highQualityImportPref.setChecked(wallpaperStore.isHighQualityImportEnabled());
      highQualityImportPref.setEnabled(!importInProgress);
    }

    if (dimPref != null) {
      dimPref.setEnabled(hasPhoto && !isInvalid && !importInProgress);
      dimPref.setValue(dim);
    }

    if (rotatePhotoPref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      rotatePhotoPref.setEnabled(enabled);
      rotatePhotoPref.setVisible(enabled);
    }

    if (scaleModePref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      scaleModePref.setEnabled(enabled);
      scaleModePref.setVisible(enabled);
      scaleModePref.setValue(String.valueOf(scaleMode));
    }

    if (anchorPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && (scaleMode == KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_CROP
                  || scaleMode == KeyboardWallpaperOverrideStore.WALLPAPER_SCALE_MODE_FIT);
      anchorPref.setEnabled(enabled);
      anchorPref.setVisible(enabled);
      anchorPref.setValue(String.valueOf(anchor));
    }

    if (wallpaperModePref != null) {
      final boolean enabled = hasPhoto && !isInvalid && !importInProgress;
      wallpaperModePref.setEnabled(enabled);
      wallpaperModePref.setVisible(enabled);
      wallpaperModePref.setValue(String.valueOf(mode));
    }

    if (keyOpacityPref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode != KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_ONLY;
      keyOpacityPref.setEnabled(enabled);
      keyOpacityPref.setVisible(enabled);
      keyOpacityPref.setValue(keyOpacityPercent);
    }

    if (matchKeyShapePref != null) {
      final boolean enabled =
          hasPhoto
              && !isInvalid
              && !importInProgress
              && mode == KeyboardWallpaperOverrideStore.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE;
      matchKeyShapePref.setEnabled(enabled);
      matchKeyShapePref.setVisible(enabled);
      matchKeyShapePref.setChecked(matchKeyShape);
    }

    if (resetPref != null) {
      resetPref.setEnabled(!importInProgress && (hasPhoto || isInvalid || dim > 0));
    }

    if (applyToAllPref != null) {
      applyToAllPref.setEnabled(!importInProgress && hasPhoto && !isInvalid);
    }

    ensureLivePreviewConfigured();
    updateLivePreview();
  }

  private void bindLivePreviewKeyboardView(@NonNull DemoKeyboardView view) {
    livePreviewKeyboardView = view;
    ensureLivePreviewConfigured();
    updateLivePreview();
  }

  private void ensureLivePreviewConfigured() {
    final DemoKeyboardView preview = livePreviewKeyboardView;
    if (preview == null) return;

    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    preview.setAllowExpensiveWallpaperEffects(true);
    preview.setKeyboardTheme(theme);

    final KeyboardDefinition defaultKeyboard =
        NskApplicationBase.getKeyboardFactory(requireContext())
            .getEnabledAddOn()
            .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    defaultKeyboard.loadKeyboard(preview.getThemedKeyboardDimens());
    preview.setKeyboard(defaultKeyboard, null, null);
  }

  private void updateLivePreview() {
    final DemoKeyboardView preview = livePreviewKeyboardView;
    if (preview == null) return;
    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    if (wallpaperPreviewResolver != null) {
      wallpaperPreviewResolver.applyPhotoOverrideIfAnyAsync(preview, theme);
    }
    preview.invalidate();
  }

  private void refreshPhotoPreview(
      @NonNull String themeId,
      boolean hasPhoto,
      boolean isInvalid,
      int dimPercent,
      int rotationDegrees,
      int scaleMode,
      int anchor) {
    if (pickPhotoPref == null) return;

    if (!hasPhoto || isInvalid) {
      if (previewDisposable != null) {
        previewDisposable.dispose();
        previewDisposable = null;
      }
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
    if (previewDisposable != null) {
      previewDisposable.dispose();
      previewDisposable = null;
    }

    final var context = getContext();
    if (context == null) return;
    final float density = context.getResources().getDisplayMetrics().density;
    final int sizePx = Math.max(64, Math.round(64f * density));
    final int clampedDim = Math.max(0, Math.min(100, dimPercent));
    final int normalizedRotation =
        KeyboardWallpaperOverrideStore.normalizeRotationDegrees(rotationDegrees);

    previewDisposable =
        Single.fromCallable(
                () ->
                    KeyboardWallpaperPreview.create(
                        context.getResources(),
                        file,
                        sizePx,
                        clampedDim,
                        normalizedRotation,
                        scaleMode,
                        anchor))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                preview -> {
                  if (!isAdded() || pickPhotoPref == null) return;
                  if (preview == null) {
                    pickPhotoPref.setIcon(null);
                    pickPhotoPref.setIconSpaceReserved(false);
                  } else {
                    pickPhotoPref.setIcon(preview.drawable);
                    pickPhotoPref.setIconSpaceReserved(true);
                  }
                },
                ignored -> {
                  if (!isAdded() || pickPhotoPref == null) return;
                  pickPhotoPref.setIcon(null);
                  pickPhotoPref.setIconSpaceReserved(false);
                });
  }

  private void onPhotoPicked(@Nullable Uri uri) {
    if (uri == null) return;

    final var context = getContext();
    if (context == null) return;

    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    final String themeId = theme.getId();
    final var metrics = context.getResources().getDisplayMetrics();
    final int maxSize =
        Math.max(2048, Math.min(4096, Math.max(metrics.widthPixels, metrics.heightPixels)));

    if (importDisposable != null) {
      importDisposable.dispose();
      importDisposable = null;
    }

    if (pickPhotoPref != null) {
      pickPhotoPref.setEnabled(false);
      pickPhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_pick_summary_saving);
    }
    if (highQualityImportPref != null) highQualityImportPref.setEnabled(false);
    if (dimPref != null) dimPref.setEnabled(false);
    if (resetPref != null) resetPref.setEnabled(false);
    if (applyToAllPref != null) applyToAllPref.setEnabled(false);

    importDisposable =
        Single.fromCallable(
                () -> {
                  wallpaperStore.importFromUri(themeId, uri, maxSize, maxSize);
                  return true;
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                ignored -> {
                  if (!isAdded()) return;
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_wallpaper_customization_pick_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                  refreshState();
                  updateLivePreview();
                },
                error -> {
                  if (!isAdded()) return;
                  showPickFailedDialog(error);
                  refreshState();
                });
  }

  private void showPickFailedDialog(@NonNull Throwable error) {
    final var context = getContext();
    if (context == null) return;

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

  private void applyWallpaperToAllThemes(@NonNull String sourceThemeId) {
    final var context = requireContext();

    if (importDisposable != null) {
      importDisposable.dispose();
      importDisposable = null;
    }

    if (pickPhotoPref != null) pickPhotoPref.setEnabled(false);
    if (dimPref != null) dimPref.setEnabled(false);
    if (resetPref != null) resetPref.setEnabled(false);
    if (applyToAllPref != null) applyToAllPref.setEnabled(false);

    importDisposable =
        Single.fromCallable(
                () -> {
                  final List<KeyboardTheme> themes =
                      NskApplicationBase.getKeyboardThemeFactory(context).getAllAddOns();
                  int applied = 0;
                  int failed = 0;
                  for (KeyboardTheme theme : themes) {
                    if (theme == null) continue;
                    final String themeId = theme.getId();
                    if (sourceThemeId.equals(themeId)) continue;
                    try {
                      wallpaperStore.copyToTheme(sourceThemeId, themeId);
                      applied++;
                    } catch (IOException e) {
                      failed++;
                    }
                  }
                  return new ApplyToAllResult(applied, failed);
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                result -> {
                  if (!isAdded()) return;
                  final int applied = result.applied;
                  final int failed = result.failed;
                  if (applied > 0 && failed == 0) {
                    Toast.makeText(
                            context,
                            getString(
                                R.string
                                    .keyboard_theme_wallpaper_customization_apply_to_all_toast_success,
                                applied),
                            Toast.LENGTH_LONG)
                        .show();
                  } else if (applied > 0) {
                    Toast.makeText(
                            context,
                            getString(
                                R.string
                                    .keyboard_theme_wallpaper_customization_apply_to_all_toast_partial,
                                applied,
                                failed),
                            Toast.LENGTH_LONG)
                        .show();
                  } else {
                    Toast.makeText(
                            context,
                            R.string
                                .keyboard_theme_wallpaper_customization_apply_to_all_toast_failed,
                            Toast.LENGTH_LONG)
                        .show();
                  }
                  refreshState();
                  updateLivePreview();
                },
                ignored -> {
                  if (!isAdded()) return;
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_wallpaper_customization_apply_to_all_toast_failed,
                          Toast.LENGTH_LONG)
                      .show();
                  refreshState();
                });
  }

  private static final class KeyboardLivePreviewPreference extends Preference {
    interface ViewBinder {
      void bind(@NonNull DemoKeyboardView view);
    }

    @Nullable private final ViewBinder binder;

    KeyboardLivePreviewPreference(@NonNull Context context, @Nullable ViewBinder binder) {
      super(context);
      this.binder = binder;
      setSelectable(false);
      setPersistent(false);
      setLayoutResource(R.layout.keyboard_theme_wallpaper_live_preview);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
      super.onBindViewHolder(holder);
      final DemoKeyboardView view =
          (DemoKeyboardView) holder.itemView.findViewById(R.id.wallpaper_live_preview_keyboard);
      if (view != null && binder != null) {
        binder.bind(view);
      }
    }
  }

  private static final class ApplyToAllResult {
    final int applied;
    final int failed;

    ApplyToAllResult(int applied, int failed) {
      this.applied = applied;
      this.failed = failed;
    }
  }

  private static final class KeyboardWallpaperPreview {
    @NonNull final android.graphics.drawable.Drawable drawable;

    private KeyboardWallpaperPreview(@NonNull android.graphics.drawable.Drawable drawable) {
      this.drawable = drawable;
    }

    @Nullable
    static KeyboardWallpaperPreview create(
        @NonNull android.content.res.Resources resources,
        @NonNull File file,
        int targetSizePx,
        int dimPercent,
        int rotationDegrees,
        int scaleMode,
        int anchor) {
      final android.graphics.Bitmap bitmap = decodeThumbnail(file, targetSizePx);
      if (bitmap == null) return null;

      final android.graphics.drawable.Drawable baseDrawable =
          new WallpaperPreviewDrawable(bitmap, rotationDegrees, scaleMode, anchor);

      if (dimPercent <= 0) {
        return new KeyboardWallpaperPreview(baseDrawable);
      }

      final android.graphics.drawable.ColorDrawable dim =
          new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK);
      dim.setAlpha(Math.round(255f * (Math.max(0, Math.min(100, dimPercent)) / 100f)));
      final android.graphics.drawable.LayerDrawable out =
          new android.graphics.drawable.LayerDrawable(
              new android.graphics.drawable.Drawable[] {baseDrawable, dim});
      return new KeyboardWallpaperPreview(out);
    }

    @Nullable
    private static android.graphics.Bitmap decodeThumbnail(@NonNull File file, int targetSizePx) {
      final android.graphics.BitmapFactory.Options bounds =
          new android.graphics.BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

      final int requested = Math.max(1, targetSizePx);
      final android.graphics.BitmapFactory.Options options =
          new android.graphics.BitmapFactory.Options();
      options.inSampleSize =
          calculateInSampleSize(bounds.outWidth, bounds.outHeight, requested, requested);
      options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;

      final android.graphics.Bitmap decoded;
      try {
        decoded = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
      } catch (OutOfMemoryError oom) {
        return null;
      }
      if (decoded == null) return null;

      final int w = decoded.getWidth();
      final int h = decoded.getHeight();
      final int maxDim = Math.max(w, h);
      if (maxDim <= requested) return decoded;

      final float scale = requested / (float) maxDim;
      final int scaledW = Math.max(1, Math.round(w * scale));
      final int scaledH = Math.max(1, Math.round(h * scale));

      final android.graphics.Bitmap scaled;
      try {
        scaled = android.graphics.Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true);
      } catch (OutOfMemoryError oom) {
        decoded.recycle();
        return null;
      }
      if (scaled != decoded) decoded.recycle();
      return scaled;
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
      int inSampleSize = 1;
      while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
      return Math.max(1, inSampleSize);
    }

    private static final class WallpaperPreviewDrawable extends android.graphics.drawable.Drawable {
      @NonNull private final android.graphics.Bitmap bitmap;
      @NonNull private final android.graphics.Paint paint;
      @NonNull private final android.graphics.BitmapShader shader;
      @NonNull private final android.graphics.Matrix shaderMatrix = new android.graphics.Matrix();
      private final int rotationDegrees;
      private final int scaleMode;
      private final int anchor;
      private int alpha = 0xFF;

      WallpaperPreviewDrawable(
          @NonNull android.graphics.Bitmap bitmap, int rotationDegrees, int scaleMode, int anchor) {
        this.bitmap = bitmap;
        this.rotationDegrees = rotationDegrees;
        this.scaleMode = scaleMode;
        this.anchor = anchor;
        this.paint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
        final android.graphics.Shader.TileMode tileMode =
            KeyboardWallpaperTransform.tileModeForScaleMode(scaleMode);
        this.shader = new android.graphics.BitmapShader(bitmap, tileMode, tileMode);
        this.paint.setShader(shader);
      }

      @Override
      protected void onBoundsChange(android.graphics.Rect bounds) {
        super.onBoundsChange(bounds);
        KeyboardWallpaperTransform.updateShaderMatrix(
            shaderMatrix,
            bitmap.getWidth(),
            bitmap.getHeight(),
            bounds,
            rotationDegrees,
            scaleMode,
            anchor);
        shader.setLocalMatrix(shaderMatrix);
      }

      @Override
      public void draw(@NonNull android.graphics.Canvas canvas) {
        paint.setAlpha(alpha);
        canvas.drawRect(getBounds(), paint);
      }

      @Override
      public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
      }

      @Override
      public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
      }

      @Override
      public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
      }
    }
  }
}
