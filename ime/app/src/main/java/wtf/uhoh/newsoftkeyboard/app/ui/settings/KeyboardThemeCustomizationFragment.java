package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public class KeyboardThemeCustomizationFragment extends PreferenceFragmentCompat {

  private KeyboardWallpaperOverrideStore wallpaperStore;
  private ActivityResultLauncher<String[]> pickWallpaperLauncher;
  private Disposable importDisposable;
  private Disposable previewDisposable;

  private Preference pickPhotoPref;
  private SeekBarPreference dimPref;
  private Preference resetPref;
  private Preference applyToAllPref;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    wallpaperStore = new KeyboardWallpaperOverrideStore(requireContext());
    pickWallpaperLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPhotoPicked);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    final var context = requireContext();
    final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    setPreferenceScreen(screen);

    final PreferenceCategory background = new PreferenceCategory(context);
    background.setTitle(R.string.keyboard_theme_wallpaper_customization_title);
    screen.addPreference(background);

    pickPhotoPref = new Preference(context);
    pickPhotoPref.setTitle(R.string.keyboard_theme_wallpaper_customization_pick_title);
    pickPhotoPref.setSummary(R.string.keyboard_theme_wallpaper_customization_pick_summary);
    pickPhotoPref.setOnPreferenceClickListener(
        ignored -> {
          pickWallpaperLauncher.launch(new String[] {"image/*"});
          return true;
        });
    background.addPreference(pickPhotoPref);

    dimPref = new SeekBarPreference(context);
    dimPref.setTitle(R.string.keyboard_theme_wallpaper_customization_dim_title);
    dimPref.setSummary(R.string.keyboard_theme_wallpaper_customization_dim_summary);
    dimPref.setMin(0);
    dimPref.setMax(100);
    dimPref.setShowSeekBarValue(true);
    dimPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final KeyboardTheme theme = getCurrentTheme();
          if (theme == null) return false;
          wallpaperStore.setDimPercent(theme.getId(), (Integer) newValue);
          return true;
        });
    background.addPreference(dimPref);

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
    return NskApplicationBase.getKeyboardThemeFactory(requireContext()).getEnabledAddOn();
  }

  private void refreshState() {
    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    final String themeId = theme.getId();
    final boolean hasPhoto = wallpaperStore.hasWallpaper(themeId);
    final boolean isInvalid = wallpaperStore.isWallpaperInvalid(themeId);
    final int dim = wallpaperStore.getDimPercent(themeId);

    if (pickPhotoPref != null) {
      pickPhotoPref.setSummary(
          getString(
              isInvalid
                  ? R.string.keyboard_theme_wallpaper_customization_pick_summary_invalid
                  : hasPhoto
                      ? R.string.keyboard_theme_wallpaper_customization_pick_summary_set
                      : R.string.keyboard_theme_wallpaper_customization_pick_summary));
    }
    refreshPhotoPreview(themeId, hasPhoto, isInvalid, dim);

    if (dimPref != null) {
      dimPref.setEnabled(hasPhoto && !isInvalid);
      dimPref.setValue(dim);
    }

    if (resetPref != null) {
      resetPref.setEnabled(hasPhoto || isInvalid || dim > 0);
    }

    if (applyToAllPref != null) {
      applyToAllPref.setEnabled(hasPhoto && !isInvalid);
    }
  }

  private void refreshPhotoPreview(
      @NonNull String themeId, boolean hasPhoto, boolean isInvalid, int dimPercent) {
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

    final var context = requireContext();
    final float density = context.getResources().getDisplayMetrics().density;
    final int sizePx = Math.max(48, Math.round(48f * density));
    final int clampedDim = Math.max(0, Math.min(100, dimPercent));

    previewDisposable =
        Single.fromCallable(
                () ->
                    KeyboardWallpaperPreview.create(
                        context.getResources(), file, sizePx, clampedDim))
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

    final KeyboardTheme theme = getCurrentTheme();
    if (theme == null) return;

    final String themeId = theme.getId();
    final var metrics = requireContext().getResources().getDisplayMetrics();
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
                          requireContext(),
                          R.string.keyboard_theme_wallpaper_customization_pick_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                  refreshState();
                },
                error -> {
                  if (!isAdded()) return;
                  showPickFailedDialog(error);
                  refreshState();
                });
  }

  private void showPickFailedDialog(@NonNull Throwable error) {
    final int messageResId;
    if (error instanceof SecurityException) {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_permission;
    } else if (error.getCause() instanceof OutOfMemoryError) {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_too_large;
    } else {
      messageResId = R.string.keyboard_theme_wallpaper_customization_pick_failed_generic;
    }

    new AlertDialog.Builder(requireContext())
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
        int dimPercent) {
      final android.graphics.Bitmap bitmap = decodeSquareThumbnail(file, targetSizePx);
      if (bitmap == null) return null;

      if (dimPercent > 0) {
        final android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        final android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setAlpha(Math.round(255f * (dimPercent / 100f)));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
      }

      return new KeyboardWallpaperPreview(
          new android.graphics.drawable.BitmapDrawable(resources, bitmap));
    }

    @Nullable
    private static android.graphics.Bitmap decodeSquareThumbnail(
        @NonNull File file, int targetSizePx) {
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
      final int side = Math.min(w, h);
      final int left = Math.max(0, (w - side) / 2);
      final int top = Math.max(0, (h - side) / 2);

      final android.graphics.Bitmap square;
      try {
        square = android.graphics.Bitmap.createBitmap(decoded, left, top, side, side);
      } catch (IllegalArgumentException | OutOfMemoryError e) {
        decoded.recycle();
        return null;
      }
      if (square != decoded) decoded.recycle();

      if (side == requested) return square;
      try {
        final android.graphics.Bitmap scaled =
            android.graphics.Bitmap.createScaledBitmap(square, requested, requested, true);
        if (scaled != square) square.recycle();
        return scaled;
      } catch (OutOfMemoryError oom) {
        square.recycle();
        return null;
      }
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
      int inSampleSize = 1;
      while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
      return Math.max(1, inSampleSize);
    }
  }
}
