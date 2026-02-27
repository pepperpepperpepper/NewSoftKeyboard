package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemeCustomizationPresetsSection {

  interface Host {
    @Nullable
    String getBaseThemeIdOrNull();

    boolean isAdded();

    void refreshState();

    void updateLivePreview();

    void scrollToPreference(@NonNull String key);

    @Nullable
    DemoKeyboardView getLivePreviewKeyboardView();

    @Nullable
    ActivityResultLauncher<String> getExportPresetLauncher();

    @Nullable
    ActivityResultLauncher<String[]> getImportPresetLauncher();
  }

  @NonNull private final Host host;
  @NonNull private final KeyboardThemePresetStore presetStore;
  @Nullable private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @NonNull
  private final KeyboardThemeAppearanceOverridesSummaryController overridesSummaryController;

  @NonNull private final KeyboardThemePresetImportExportController presetImportExportController;
  @NonNull private final KeyboardThemePresetPerAppBindingsController perAppBindingsController;

  @Nullable private Preference presetSelectionPref;
  @Nullable private Preference saveAsPresetPref;
  @Nullable private Preference renamePresetPref;
  @Nullable private Preference deletePresetPref;

  KeyboardThemeCustomizationPresetsSection(
      @NonNull Host host,
      @NonNull KeyboardThemePresetStore presetStore,
      @Nullable KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.presetStore = presetStore;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
    this.overridesSummaryController =
        new KeyboardThemeAppearanceOverridesSummaryController(
            host, wallpaperStore, themeOverridesStore);
    this.presetImportExportController =
        new KeyboardThemePresetImportExportController(host, presetStore, wallpaperStore);
    this.perAppBindingsController =
        new KeyboardThemePresetPerAppBindingsController(host, presetStore);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    final PreferenceCategory presets = new PreferenceCategory(context);
    presets.setKey("section:presets");
    presets.setTitle(R.string.keyboard_theme_presets_title);
    screen.addPreference(presets);

    overridesSummaryController.addPreferences(context, presets);

    presetSelectionPref = new Preference(context);
    presetSelectionPref.setKey("keyboard_theme_preset_selection");
    presetSelectionPref.setTitle(R.string.keyboard_theme_presets_current_title);
    presetSelectionPref.setOnPreferenceClickListener(
        ignored -> {
          showPresetSelectionDialog(context);
          return true;
        });
    presets.addPreference(presetSelectionPref);

    saveAsPresetPref = new Preference(context);
    saveAsPresetPref.setKey("keyboard_theme_presets_save_as");
    saveAsPresetPref.setTitle(R.string.keyboard_theme_presets_save_as_title);
    saveAsPresetPref.setSummary(R.string.keyboard_theme_presets_save_as_summary);
    saveAsPresetPref.setOnPreferenceClickListener(
        ignored -> {
          showSaveAsPresetDialog(context);
          return true;
        });
    presets.addPreference(saveAsPresetPref);

    renamePresetPref = new Preference(context);
    renamePresetPref.setTitle(R.string.keyboard_theme_presets_rename_title);
    renamePresetPref.setOnPreferenceClickListener(
        ignored -> {
          showRenamePresetDialog(context);
          return true;
        });
    presets.addPreference(renamePresetPref);

    deletePresetPref = new Preference(context);
    deletePresetPref.setTitle(R.string.keyboard_theme_presets_delete_title);
    deletePresetPref.setOnPreferenceClickListener(
        ignored -> {
          showDeletePresetDialog(context);
          return true;
        });
    presets.addPreference(deletePresetPref);

    presetImportExportController.addPreferences(context, presets);
    perAppBindingsController.addPreferences(context, presets);
  }

  boolean isPresetTransferInProgress() {
    return presetImportExportController.isPresetTransferInProgress();
  }

  void dispose() {
    presetImportExportController.dispose();
  }

  void refreshState(@NonNull String baseThemeId, @NonNull String presetId, boolean busy) {
    final Preference selection = presetSelectionPref;
    if (selection != null) {
      selection.setSummary(buildPresetDisplayName(selection.getContext(), baseThemeId, presetId));
      selection.setEnabled(!busy);
    }

    final boolean isDefaultPreset = presetId.equals(baseThemeId);
    if (saveAsPresetPref != null) {
      saveAsPresetPref.setEnabled(!busy);
    }
    if (renamePresetPref != null) {
      renamePresetPref.setEnabled(!busy && !isDefaultPreset);
    }
    if (deletePresetPref != null) {
      deletePresetPref.setEnabled(!busy && !isDefaultPreset);
    }
    presetImportExportController.refreshState(busy);

    overridesSummaryController.refreshState(presetId, busy);
    perAppBindingsController.refreshState(baseThemeId, presetId, busy);
  }

  void refreshOverridesSummary(
      @NonNull String presetId,
      boolean busy,
      boolean hasWallpaperOverride,
      boolean hasColorOverride,
      boolean hasTypographyOverride,
      boolean hasShadowsOverride) {
    overridesSummaryController.refreshOverridesSummary(
        presetId,
        busy,
        hasWallpaperOverride,
        hasColorOverride,
        hasTypographyOverride,
        hasShadowsOverride);
  }

  @NonNull
  private String buildPresetDisplayName(
      @NonNull Context context, @NonNull String baseThemeId, @NonNull String presetId) {
    if (presetId.equals(baseThemeId)) {
      return context.getString(R.string.keyboard_theme_presets_default_entry);
    }
    final String name = presetStore.getPresetName(presetId);
    if (name != null && !name.trim().isEmpty()) return name.trim();
    return context.getString(R.string.keyboard_theme_presets_unnamed_entry);
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    final float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
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

  void onPresetExportUri(@Nullable Uri uri) {
    presetImportExportController.onPresetExportUri(uri);
  }

  void onPresetImportUri(@Nullable Uri uri) {
    presetImportExportController.onPresetImportUri(uri);
  }

  private static final class PresetEntry {
    @NonNull final String presetId;
    @NonNull final String title;
    @Nullable final String summary;

    private PresetEntry(@NonNull String presetId, @NonNull String title, @Nullable String summary) {
      this.presetId = presetId;
      this.title = title;
      this.summary = summary;
    }
  }

  private static final class PresetEntryAdapter extends ArrayAdapter<PresetEntry> {

    private final int thumbWidthPx;
    private final int thumbHeightPx;
    private final int thumbPaddingPx;
    @NonNull private final Drawable placeholder;

    @NonNull
    private final java.util.Map<String, Bitmap> bitmapsByPresetId = new java.util.HashMap<>();

    @NonNull
    private final java.util.Map<String, Drawable> drawablesByPresetId = new java.util.HashMap<>();

    private PresetEntryAdapter(@NonNull Context context, @NonNull List<PresetEntry> entries) {
      super(context, android.R.layout.simple_list_item_2, new ArrayList<>(entries));
      thumbWidthPx = dpToPx(context, 80);
      thumbHeightPx = dpToPx(context, 48);
      thumbPaddingPx = dpToPx(context, 12);

      final Drawable rawPlaceholder =
          ContextCompat.getDrawable(context, android.R.drawable.ic_menu_gallery);
      placeholder =
          rawPlaceholder != null
              ? rawPlaceholder
              : new android.graphics.drawable.ColorDrawable(0x00000000);
      placeholder.setBounds(0, 0, thumbWidthPx, thumbHeightPx);
    }

    void setThumbnails(@NonNull java.util.Map<String, Bitmap> bitmaps) {
      recycleThumbnails();
      bitmapsByPresetId.clear();
      drawablesByPresetId.clear();

      bitmapsByPresetId.putAll(bitmaps);
      for (java.util.Map.Entry<String, Bitmap> entry : bitmaps.entrySet()) {
        final Bitmap bitmap = entry.getValue();
        if (bitmap == null) continue;
        final android.graphics.drawable.BitmapDrawable drawable =
            new android.graphics.drawable.BitmapDrawable(getContext().getResources(), bitmap);
        drawable.setBounds(0, 0, thumbWidthPx, thumbHeightPx);
        drawablesByPresetId.put(entry.getKey(), drawable);
      }
    }

    void recycleThumbnails() {
      for (Bitmap bitmap : bitmapsByPresetId.values()) {
        if (bitmap != null) bitmap.recycle();
      }
      bitmapsByPresetId.clear();
      drawablesByPresetId.clear();
    }

    @NonNull
    @Override
    public android.view.View getView(
        int position, @Nullable android.view.View convertView, @NonNull ViewGroup parent) {
      final android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());
      final android.view.View view =
          convertView != null
              ? convertView
              : inflater.inflate(android.R.layout.simple_list_item_2, parent, false);

      final PresetEntry entry = getItem(position);
      if (entry == null) return view;

      final TextView text1 = view.findViewById(android.R.id.text1);
      final TextView text2 = view.findViewById(android.R.id.text2);
      text1.setText(entry.title);

      final String summary = entry.summary;
      if (summary == null || summary.isEmpty()) {
        text2.setVisibility(View.GONE);
      } else {
        text2.setVisibility(View.VISIBLE);
        text2.setText(summary);
      }

      final Drawable thumb = drawablesByPresetId.get(entry.presetId);
      text1.setCompoundDrawables(thumb != null ? thumb : placeholder, null, null, null);
      text1.setCompoundDrawablePadding(thumbPaddingPx);

      return view;
    }
  }

  private void showPresetSelectionDialog(@NonNull Context context) {
    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;

    final String currentPresetId = presetStore.getActivePresetId(baseThemeId);
    final DemoKeyboardView livePreview = host.getLivePreviewKeyboardView();
    if (livePreview != null && !presetStore.hasPresetPreview(currentPresetId)) {
      livePreview.post(
          () ->
              presetStore.savePresetPreviewPngBytesBestEffort(
                  currentPresetId, capturePreviewPngBytes(livePreview)));
    }

    final List<KeyboardThemePresetStore.Preset> presets = presetStore.listPresets(baseThemeId);
    final List<PresetEntry> entries = new ArrayList<>(presets.size());
    for (KeyboardThemePresetStore.Preset preset : presets) {
      final String presetId = preset.id();
      final String title =
          presetId.equals(currentPresetId)
              ? "\u2713 " + buildPresetDisplayName(context, baseThemeId, presetId)
              : buildPresetDisplayName(context, baseThemeId, presetId);

      final boolean hasPhoto =
          wallpaperStore != null
              && wallpaperStore.hasWallpaper(presetId)
              && !wallpaperStore.isWallpaperInvalid(presetId);
      entries.add(
          new PresetEntry(
              presetId,
              title,
              hasPhoto
                  ? context.getString(R.string.keyboard_theme_customize_current_summary_photo_set)
                  : null));
    }

    final PresetEntryAdapter adapter = new PresetEntryAdapter(context, entries);
    final ListView list = new ListView(context);
    list.setAdapter(adapter);

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.keyboard_theme_presets_current_title)
            .setView(list)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .show();

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          final PresetEntry entry = adapter.getItem(position);
          if (entry == null) return;
          dialog.dismiss();
          if (entry.presetId.equals(currentPresetId)) return;

          try {
            presetStore.setActivePresetId(baseThemeId, entry.presetId);
          } catch (IllegalArgumentException e) {
            return;
          }

          host.refreshState();
          host.updateLivePreview();

          final DemoKeyboardView preview = host.getLivePreviewKeyboardView();
          if (preview != null) {
            preview.post(
                () ->
                    presetStore.savePresetPreviewPngBytesBestEffort(
                        entry.presetId, capturePreviewPngBytes(preview)));
          }
        });

    final int thumbWidthPx = dpToPx(context, 80);
    final int thumbHeightPx = dpToPx(context, 48);
    final Disposable thumbsDisposable =
        Single.fromCallable(
                () -> {
                  final java.util.Map<String, Bitmap> thumbnails = new java.util.HashMap<>();
                  for (PresetEntry entry : entries) {
                    final Bitmap bitmap =
                        presetStore.readPresetPreviewBitmap(
                            entry.presetId, thumbWidthPx, thumbHeightPx);
                    if (bitmap != null) thumbnails.put(entry.presetId, bitmap);
                  }
                  return thumbnails;
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(adapter::setThumbnails, ignored -> {});

    dialog.setOnDismissListener(
        ignored -> {
          thumbsDisposable.dispose();
          adapter.recycleThumbnails();
        });
  }

  private void showSaveAsPresetDialog(@NonNull Context context) {
    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;
    if (wallpaperStore == null || themeOverridesStore == null) return;

    final EditText input = new EditText(context);
    input.setSingleLine(true);
    input.setHint(R.string.keyboard_theme_presets_save_as_hint);

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_theme_presets_save_as_title)
        .setView(input)
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(
            R.string.keyboard_theme_presets_save_as_action,
            (dialog, which) -> {
              dialog.dismiss();

              final String sourcePresetId = presetStore.getActivePresetId(baseThemeId);
              final DemoKeyboardView preview = host.getLivePreviewKeyboardView();
              final byte[] previewBytes = preview != null ? capturePreviewPngBytes(preview) : null;
              final String name = String.valueOf(input.getText());

              final KeyboardThemePresetStore.Preset created;
              try {
                created = presetStore.createPreset(baseThemeId, name);
              } catch (IllegalArgumentException e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
              }

              final String targetPresetId = created.id();
              try {
                themeOverridesStore.copyToTheme(sourcePresetId, targetPresetId);
                if (wallpaperStore.hasWallpaper(sourcePresetId)
                    && !wallpaperStore.isWallpaperInvalid(sourcePresetId)) {
                  wallpaperStore.copyToTheme(sourcePresetId, targetPresetId);
                }
                presetStore.setActivePresetId(baseThemeId, targetPresetId);
                presetStore.savePresetPreviewPngBytesBestEffort(targetPresetId, previewBytes);
                Toast.makeText(
                        context, R.string.keyboard_theme_presets_save_as_toast, Toast.LENGTH_SHORT)
                    .show();
              } catch (Exception e) {
                presetStore.deletePreset(targetPresetId);
                themeOverridesStore.clearAllOverrides(targetPresetId);
                wallpaperStore.clear(targetPresetId);
                Toast.makeText(
                        context,
                        R.string.keyboard_theme_presets_save_as_failed_toast,
                        Toast.LENGTH_SHORT)
                    .show();
              }

              host.refreshState();
              host.updateLivePreview();
            })
        .show();
  }

  private void showRenamePresetDialog(@NonNull Context context) {
    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;

    final String presetId = presetStore.getActivePresetId(baseThemeId);
    if (presetId.equals(baseThemeId)) return;

    final String currentName = presetStore.getPresetName(presetId);
    final EditText input = new EditText(context);
    input.setSingleLine(true);
    input.setText(currentName != null ? currentName : "");
    input.setSelection(input.getText().length());

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_theme_presets_rename_title)
        .setView(input)
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(
            R.string.keyboard_theme_presets_rename_action,
            (dialog, which) -> {
              dialog.dismiss();
              try {
                presetStore.renamePreset(presetId, String.valueOf(input.getText()));
                Toast.makeText(
                        context, R.string.keyboard_theme_presets_rename_toast, Toast.LENGTH_SHORT)
                    .show();
                host.refreshState();
              } catch (IllegalArgumentException e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
              }
            })
        .show();
  }

  private void showDeletePresetDialog(@NonNull Context context) {
    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;
    if (wallpaperStore == null || themeOverridesStore == null) return;

    final String presetId = presetStore.getActivePresetId(baseThemeId);
    if (presetId.equals(baseThemeId)) return;

    final String name = presetStore.getPresetName(presetId);
    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_theme_presets_delete_title)
        .setMessage(
            context.getString(
                R.string.keyboard_theme_presets_delete_message,
                name != null && !name.isEmpty()
                    ? name
                    : context.getString(R.string.keyboard_theme_presets_unnamed_entry)))
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(
            R.string.keyboard_theme_presets_delete_action,
            (dialog, which) -> {
              dialog.dismiss();
              presetStore.deletePreset(presetId);
              themeOverridesStore.clearAllOverrides(presetId);
              wallpaperStore.clear(presetId);
              Toast.makeText(
                      context, R.string.keyboard_theme_presets_delete_toast, Toast.LENGTH_SHORT)
                  .show();
              host.refreshState();
              host.updateLivePreview();
            })
        .show();
  }
}
