package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetTransfer;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemePresetImportExportController {

  @NonNull private final KeyboardThemeCustomizationPresetsSection.Host host;
  @NonNull private final KeyboardThemePresetStore presetStore;
  @Nullable private final KeyboardWallpaperOverrideStore wallpaperStore;

  @Nullable private Preference exportPresetPref;
  @Nullable private Preference importPresetPref;

  @Nullable private Disposable presetTransferDisposable;
  private boolean exportIncludeWallpaper = true;

  KeyboardThemePresetImportExportController(
      @NonNull KeyboardThemeCustomizationPresetsSection.Host host,
      @NonNull KeyboardThemePresetStore presetStore,
      @Nullable KeyboardWallpaperOverrideStore wallpaperStore) {
    this.host = host;
    this.presetStore = presetStore;
    this.wallpaperStore = wallpaperStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory presets) {
    exportPresetPref = new Preference(context);
    exportPresetPref.setKey("keyboard_theme_presets_export");
    exportPresetPref.setTitle(R.string.keyboard_theme_presets_export_title);
    exportPresetPref.setSummary(R.string.keyboard_theme_presets_export_summary);
    exportPresetPref.setOnPreferenceClickListener(
        ignored -> {
          final String baseThemeId = host.getBaseThemeIdOrNull();
          if (baseThemeId == null) return true;

          final ActivityResultLauncher<String> launcher = host.getExportPresetLauncher();
          if (launcher == null) {
            showPresetTransferFailedDialog(
                context,
                R.string.keyboard_theme_presets_export_failed_title,
                R.string.keyboard_theme_presets_export_failed_generic);
            host.refreshState();
            return true;
          }

          final String presetId = presetStore.getActivePresetId(baseThemeId);
          showPresetExportOptionsDialog(context, baseThemeId, presetId, launcher);
          return true;
        });
    presets.addPreference(exportPresetPref);

    importPresetPref = new Preference(context);
    importPresetPref.setKey("keyboard_theme_presets_import");
    importPresetPref.setTitle(R.string.keyboard_theme_presets_import_title);
    importPresetPref.setSummary(R.string.keyboard_theme_presets_import_summary);
    importPresetPref.setOnPreferenceClickListener(
        ignored -> {
          final ActivityResultLauncher<String[]> launcher = host.getImportPresetLauncher();
          if (launcher == null) {
            showPresetTransferFailedDialog(
                context,
                R.string.keyboard_theme_presets_import_failed_title,
                R.string.keyboard_theme_presets_import_failed_generic);
            host.refreshState();
            return true;
          }
          try {
            launcher.launch(
                new String[] {
                  KeyboardThemePresetTransfer.MIME_TYPE_ZIP,
                  "application/octet-stream",
                  "application/x-zip-compressed",
                  "*/*"
                });
          } catch (ActivityNotFoundException e) {
            showPresetTransferFailedDialog(
                context,
                R.string.keyboard_theme_presets_import_failed_title,
                R.string.keyboard_theme_presets_import_failed_no_picker);
          }
          return true;
        });
    presets.addPreference(importPresetPref);
  }

  boolean isPresetTransferInProgress() {
    return presetTransferDisposable != null && !presetTransferDisposable.isDisposed();
  }

  void dispose() {
    if (presetTransferDisposable != null) {
      presetTransferDisposable.dispose();
      presetTransferDisposable = null;
    }
  }

  void refreshState(boolean busy) {
    if (exportPresetPref != null) {
      exportPresetPref.setEnabled(!busy);
    }
    if (importPresetPref != null) {
      importPresetPref.setEnabled(!busy);
    }
  }

  void onPresetExportUri(@Nullable Uri uri) {
    if (uri == null) return;

    final Context context = exportPresetPref != null ? exportPresetPref.getContext() : null;
    if (context == null) return;

    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;

    final String presetId = presetStore.getActivePresetId(baseThemeId);

    @Nullable byte[] previewPngBytes = null;
    boolean presetHasWallpaper = true;
    if (wallpaperStore != null) {
      presetHasWallpaper =
          wallpaperStore.hasWallpaper(presetId) && !wallpaperStore.isWallpaperInvalid(presetId);
    }
    if (exportIncludeWallpaper || !presetHasWallpaper) {
      final DemoKeyboardView view = host.getLivePreviewKeyboardView();
      if (view != null) {
        previewPngBytes = capturePreviewPngBytes(view);
      }
    }
    presetStore.savePresetPreviewPngBytesBestEffort(presetId, previewPngBytes);

    dispose();

    final byte[] previewBytes = previewPngBytes;
    presetTransferDisposable =
        Single.fromCallable(
                () -> {
                  try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out == null)
                      throw new IOException("Could not open output stream for " + uri);
                    KeyboardThemePresetTransfer.exportPreset(
                        context, baseThemeId, presetId, out, exportIncludeWallpaper, previewBytes);
                    return true;
                  }
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                ignored -> {
                  if (!host.isAdded()) return;
                  Toast.makeText(
                          context, R.string.keyboard_theme_presets_export_toast, Toast.LENGTH_SHORT)
                      .show();
                  host.refreshState();
                },
                error -> {
                  if (!host.isAdded()) return;
                  showPresetExportFailedDialog(context, error);
                  host.refreshState();
                });
    host.refreshState();
  }

  @Nullable
  private static byte[] capturePreviewPngBytes(@NonNull DemoKeyboardView view) {
    final int sourceWidth = view.getWidth();
    final int sourceHeight = view.getHeight();
    if (sourceWidth <= 0 || sourceHeight <= 0) return null;

    final int maxWidthPx = 720;
    final int maxHeightPx = 360;
    final float scale =
        Math.min(
            1f, Math.min(maxWidthPx / (float) sourceWidth, maxHeightPx / (float) sourceHeight));
    final int outWidth = Math.max(1, Math.round(sourceWidth * scale));
    final int outHeight = Math.max(1, Math.round(sourceHeight * scale));

    final Bitmap bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
    try {
      final Canvas canvas = new Canvas(bitmap);
      canvas.scale(scale, scale);
      view.draw(canvas);

      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return null;
      return out.toByteArray();
    } finally {
      bitmap.recycle();
    }
  }

  void onPresetImportUri(@Nullable Uri uri) {
    if (uri == null) return;

    final Context context = importPresetPref != null ? importPresetPref.getContext() : null;
    if (context == null) return;

    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;

    dispose();

    presetTransferDisposable =
        Single.fromCallable(
                () -> {
                  try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Could not open input stream for " + uri);
                    return KeyboardThemePresetTransfer.readArchiveInfo(in);
                  }
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                info -> {
                  if (!host.isAdded()) return;
                  if (!baseThemeId.equals(info.baseThemeId())) {
                    showPresetImportFailedDialog(
                        context,
                        new IOException(
                            "Preset belongs to a different base theme: " + info.baseThemeId()));
                    host.refreshState();
                    return;
                  }

                  showPresetImportPreviewDialog(context, baseThemeId, uri, info);
                  host.refreshState();
                },
                error -> {
                  if (!host.isAdded()) return;
                  showPresetImportFailedDialog(context, error);
                  host.refreshState();
                });
    host.refreshState();
  }

  private void showPresetImportPreviewDialog(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull Uri uri,
      @NonNull KeyboardThemePresetTransfer.PresetArchiveInfo info) {
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    final float density = context.getResources().getDisplayMetrics().density;
    final int pad = Math.round(16f * density);
    root.setPadding(pad, pad, pad, pad);

    final ImageView previewImage = new ImageView(context);
    previewImage.setAdjustViewBounds(true);
    previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
    previewImage.setVisibility(android.view.View.GONE);
    root.addView(previewImage);

    final TextView details = new TextView(context);
    details.setText(buildPresetImportPreviewMessage(context, info));
    root.addView(details);

    final EditText nameInput = new EditText(context);
    nameInput.setSingleLine(true);
    nameInput.setHint(R.string.keyboard_theme_presets_save_as_hint);
    nameInput.setText(info.presetName());
    nameInput.setSelection(nameInput.getText().length());
    root.addView(nameInput);

    final CheckBox setActive = new CheckBox(context);
    setActive.setText(R.string.keyboard_theme_presets_import_set_active);
    setActive.setChecked(true);
    root.addView(setActive);

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.keyboard_theme_presets_import_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setPositiveButton(
                R.string.keyboard_theme_presets_import_action,
                (d, w) -> {
                  d.dismiss();
                  final String requestedName = String.valueOf(nameInput.getText());
                  startPresetImportFromUri(
                      context, baseThemeId, uri, requestedName, setActive.isChecked());
                })
            .show();

    if (info.hasPreviewImage()) {
      final Disposable previewDisposable =
          Single.fromCallable(
                  () -> {
                    try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                      if (in == null) return null;
                      return KeyboardThemePresetTransfer.readPreviewBitmap(in);
                    }
                  })
              .subscribeOn(RxSchedulers.background())
              .observeOn(RxSchedulers.mainThread())
              .subscribe(
                  bitmap -> {
                    if (bitmap == null || !dialog.isShowing()) return;
                    previewImage.setImageBitmap(bitmap);
                    previewImage.setVisibility(android.view.View.VISIBLE);
                  },
                  ignored -> {});
      dialog.setOnDismissListener(ignored -> previewDisposable.dispose());
    }
  }

  @NonNull
  private static String buildPresetImportPreviewMessage(
      @NonNull Context context, @NonNull KeyboardThemePresetTransfer.PresetArchiveInfo info) {
    final StringBuilder sb = new StringBuilder();
    final String baseThemeName = info.baseThemeName();
    if (baseThemeName != null) {
      sb.append(
          context.getString(
              R.string.keyboard_theme_presets_import_preview_base_theme_named,
              baseThemeName,
              info.baseThemeId()));
    } else {
      sb.append(
          context.getString(
              R.string.keyboard_theme_presets_import_preview_base_theme, info.baseThemeId()));
    }

    if (info.exportedAtMillis() > 0L) {
      final String formatted =
          DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
              .format(new Date(info.exportedAtMillis()));
      sb.append('\n')
          .append(
              context.getString(
                  R.string.keyboard_theme_presets_import_preview_exported, formatted));
    }

    final String exportedByVersionName = info.exportedByVersionName();
    if (exportedByVersionName != null) {
      sb.append('\n')
          .append(
              context.getString(
                  R.string.keyboard_theme_presets_import_preview_exported_by_version,
                  exportedByVersionName));
    }

    final List<String> parts = new ArrayList<>();
    if (info.hasWallpaper())
      parts.add(context.getString(R.string.keyboard_theme_appearance_background_title));
    if (info.hasColors())
      parts.add(context.getString(R.string.keyboard_theme_appearance_colors_title));
    if (info.hasTypography())
      parts.add(context.getString(R.string.keyboard_theme_appearance_typography_title));
    if (info.hasShadows())
      parts.add(context.getString(R.string.keyboard_theme_appearance_shadows_title));

    final String partsLabel =
        parts.isEmpty()
            ? context.getString(R.string.keyboard_theme_presets_import_preview_contains_none)
            : TextUtils.join(", ", parts);
    sb.append('\n')
        .append(
            context.getString(R.string.keyboard_theme_presets_import_preview_contains, partsLabel));

    if (info.hasCustomFont()) {
      final String customName = info.customFontName();
      if (customName != null && !customName.trim().isEmpty()) {
        sb.append('\n')
            .append(
                context.getString(
                    R.string.keyboard_theme_presets_import_preview_custom_font_named, customName));
      } else {
        sb.append('\n')
            .append(
                context.getString(
                    R.string.keyboard_theme_presets_import_preview_custom_font_included));
      }
    }

    return sb.toString();
  }

  private void startPresetImportFromUri(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull Uri uri,
      @Nullable String requestedName,
      boolean setActive) {
    final String trimmed = requestedName == null ? null : requestedName.trim();
    final String overrideName = trimmed == null || trimmed.isEmpty() ? null : trimmed;

    dispose();

    presetTransferDisposable =
        Single.fromCallable(
                () -> {
                  final KeyboardThemePresetTransfer.ImportedPreset imported;
                  try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Could not open input stream for " + uri);
                    imported =
                        KeyboardThemePresetTransfer.importPreset(
                            context, baseThemeId, in, overrideName);
                  }
                  // Best-effort: persist archive preview as the preset thumbnail to keep preset
                  // lists fast without having to re-render the preview at list time.
                  try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                    if (in != null) {
                      final byte[] previewBytes =
                          KeyboardThemePresetTransfer.readPreviewPngBytes(in);
                      presetStore.savePresetPreviewPngBytesBestEffort(
                          imported.presetId(), previewBytes);
                    }
                  } catch (Exception ignored) {
                    // ignore
                  }
                  return imported;
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                imported -> {
                  if (!host.isAdded()) return;
                  if (setActive) {
                    presetStore.setActivePresetId(baseThemeId, imported.presetId());
                  }
                  Toast.makeText(
                          context,
                          context.getString(
                              R.string.keyboard_theme_presets_import_toast, imported.presetName()),
                          Toast.LENGTH_SHORT)
                      .show();
                  host.refreshState();
                  if (setActive) host.updateLivePreview();
                },
                error -> {
                  if (!host.isAdded()) return;
                  showPresetImportFailedDialog(context, error);
                  host.refreshState();
                });
    host.refreshState();
  }

  private void showPresetExportOptionsDialog(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      @NonNull ActivityResultLauncher<String> launcher) {
    final int paddingPx = dpToPx(context, 16);

    final LinearLayout view = new LinearLayout(context);
    view.setOrientation(LinearLayout.VERTICAL);
    view.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final CheckBox includeWallpaper = new CheckBox(context);
    includeWallpaper.setChecked(exportIncludeWallpaper);
    includeWallpaper.setText(R.string.keyboard_theme_presets_export_include_wallpaper_title);
    view.addView(
        includeWallpaper,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.keyboard_theme_presets_export_title)
        .setMessage(R.string.keyboard_theme_presets_export_summary)
        .setView(view)
        .setPositiveButton(
            R.string.keyboard_theme_presets_export_choose_file,
            (dialog, which) -> {
              exportIncludeWallpaper = includeWallpaper.isChecked();
              final String fileName =
                  buildPresetExportFileName(context, baseThemeId, presetId, exportIncludeWallpaper);
              try {
                launcher.launch(fileName);
              } catch (ActivityNotFoundException e) {
                showPresetTransferFailedDialog(
                    context,
                    R.string.keyboard_theme_presets_export_failed_title,
                    R.string.keyboard_theme_presets_export_failed_no_picker);
              }
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  @NonNull
  private String buildPresetExportFileName(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      boolean includeWallpaper) {
    final String name;
    final String stored = presetStore.getPresetName(presetId);
    if (stored != null && !stored.trim().isEmpty()) {
      name = stored.trim();
    } else if (presetId.equals(baseThemeId)) {
      name = context.getString(R.string.keyboard_theme_presets_default_entry);
    } else {
      name = context.getString(R.string.keyboard_theme_presets_unnamed_entry);
    }

    final String safeBase = sanitizeFileNamePart(baseThemeId);
    final String safeName = sanitizeFileNamePart(name);
    return "nsk-preset_"
        + safeBase
        + "_"
        + safeName
        + (includeWallpaper ? "" : "_no-photo")
        + ".zip";
  }

  @NonNull
  private static String sanitizeFileNamePart(@NonNull String raw) {
    final String trimmed = raw.trim();
    if (trimmed.isEmpty()) return "preset";

    final int maxLen = 40;
    final StringBuilder out = new StringBuilder(Math.min(trimmed.length(), maxLen));
    boolean lastWasUnderscore = false;
    for (int i = 0; i < trimmed.length() && out.length() < maxLen; i++) {
      final char c = trimmed.charAt(i);
      final boolean isSafe =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '_'
              || c == '-'
              || c == '.';
      final char mapped = isSafe ? c : '_';
      if (mapped == '_') {
        if (lastWasUnderscore) continue;
        lastWasUnderscore = true;
      } else {
        lastWasUnderscore = false;
      }
      out.append(mapped);
    }

    int start = 0;
    while (start < out.length()
        && (out.charAt(start) == '_' || out.charAt(start) == '.' || out.charAt(start) == '-')) {
      start++;
    }
    int end = out.length();
    while (end > start
        && (out.charAt(end - 1) == '_'
            || out.charAt(end - 1) == '.'
            || out.charAt(end - 1) == '-')) {
      end--;
    }
    final String sanitized = out.substring(start, end);
    return sanitized.isEmpty() ? "preset" : sanitized;
  }

  private void showPresetExportFailedDialog(@NonNull Context context, @NonNull Throwable error) {
    final int messageResId;
    if (error instanceof SecurityException) {
      messageResId = R.string.keyboard_theme_presets_export_failed_permission;
    } else {
      messageResId = R.string.keyboard_theme_presets_export_failed_generic;
    }
    showPresetTransferFailedDialog(
        context, R.string.keyboard_theme_presets_export_failed_title, messageResId);
  }

  private void showPresetImportFailedDialog(@NonNull Context context, @NonNull Throwable error) {
    final int messageResId;
    if (error instanceof SecurityException) {
      showPresetTransferFailedDialog(
          context,
          R.string.keyboard_theme_presets_import_failed_title,
          R.string.keyboard_theme_presets_import_failed_permission);
      return;
    }

    if (error instanceof IOException) {
      final String message = error.getMessage();
      final String prefix = "Preset belongs to a different base theme: ";
      if (message != null && message.startsWith(prefix)) {
        final String otherBaseThemeId = message.substring(prefix.length()).trim();
        new AlertDialog.Builder(context)
            .setTitle(R.string.keyboard_theme_presets_import_failed_title)
            .setMessage(
                context.getString(
                    R.string.keyboard_theme_presets_import_failed_wrong_theme, otherBaseThemeId))
            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
            .show();
        return;
      }
    }

    messageResId = R.string.keyboard_theme_presets_import_failed_generic;
    showPresetTransferFailedDialog(
        context, R.string.keyboard_theme_presets_import_failed_title, messageResId);
  }

  private static void showPresetTransferFailedDialog(
      @NonNull Context context, int titleResId, int messageResId) {
    new AlertDialog.Builder(context)
        .setTitle(titleResId)
        .setMessage(messageResId)
        .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
        .show();
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    final float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
  }
}
