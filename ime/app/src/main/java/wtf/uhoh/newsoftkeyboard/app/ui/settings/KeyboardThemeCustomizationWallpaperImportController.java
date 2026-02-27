package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.NonNull;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.IOException;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemeCustomizationWallpaperImportController {

  @FunctionalInterface
  interface ErrorHandler {
    void onError(@NonNull Throwable error);
  }

  @NonNull private final KeyboardThemeCustomizationBackgroundSection.Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;

  private Disposable importDisposable;

  KeyboardThemeCustomizationWallpaperImportController(
      @NonNull KeyboardThemeCustomizationBackgroundSection.Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
  }

  boolean isWallpaperImportInProgress() {
    return importDisposable != null && !importDisposable.isDisposed();
  }

  void disposeImport() {
    if (importDisposable != null) {
      importDisposable.dispose();
      importDisposable = null;
    }
  }

  void importWallpaperFromUri(
      @NonNull Context context,
      @NonNull String themeId,
      @NonNull Uri uri,
      int maxSize,
      @NonNull ErrorHandler errorHandler) {
    disposeImport();
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
                  if (!host.isAdded()) return;
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_wallpaper_customization_pick_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                  host.refreshState();
                  host.updateLivePreview();
                  host.scheduleEnsureReadableUpdateIfEnabled(themeId);
                },
                error -> {
                  if (!host.isAdded()) return;
                  errorHandler.onError(error);
                  host.refreshState();
                });
  }

  void applyWallpaperToAllThemes(@NonNull Context context, @NonNull String sourceThemeId) {
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);

    disposeImport();
    importDisposable =
        Single.fromCallable(
                () -> {
                  final List<KeyboardTheme> themes =
                      NskApplicationBase.getKeyboardThemeFactory(context).getAllAddOns();
                  int applied = 0;
                  int failed = 0;
                  for (KeyboardTheme theme : themes) {
                    if (theme == null) continue;
                    final String baseThemeId = theme.getId();
                    final String targetThemeId = presetStore.getActivePresetId(baseThemeId);
                    if (sourceThemeId.equals(targetThemeId)) continue;
                    try {
                      wallpaperStore.copyToTheme(sourceThemeId, targetThemeId);
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
                  if (!host.isAdded()) return;
                  final int applied = result.applied;
                  final int failed = result.failed;
                  if (applied > 0 && failed == 0) {
                    Toast.makeText(
                            context,
                            context.getString(
                                R.string
                                    .keyboard_theme_wallpaper_customization_apply_to_all_toast_success,
                                applied),
                            Toast.LENGTH_LONG)
                        .show();
                  } else if (applied > 0) {
                    Toast.makeText(
                            context,
                            context.getString(
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
                  host.refreshState();
                  host.updateLivePreview();
                },
                ignored -> {
                  if (!host.isAdded()) return;
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_wallpaper_customization_apply_to_all_toast_failed,
                          Toast.LENGTH_LONG)
                      .show();
                  host.refreshState();
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
}
