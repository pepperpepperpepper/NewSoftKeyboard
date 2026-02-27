package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeAppearanceOverridesSummaryController {

  @NonNull private final KeyboardThemeCustomizationPresetsSection.Host host;
  @Nullable private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private Preference overridesSummaryPref;

  @Nullable private String overridesPresetId;
  private boolean overridesBusy;
  private boolean overridesHasWallpaper;
  private boolean overridesHasColors;
  private boolean overridesHasTypography;
  private boolean overridesHasShadows;

  KeyboardThemeAppearanceOverridesSummaryController(
      @NonNull KeyboardThemeCustomizationPresetsSection.Host host,
      @Nullable KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory presets) {
    overridesSummaryPref = new Preference(context);
    overridesSummaryPref.setKey("keyboard_theme_appearance_overrides_summary");
    overridesSummaryPref.setTitle(R.string.keyboard_theme_appearance_overrides_summary_title);
    overridesSummaryPref.setOnPreferenceClickListener(
        ignored -> {
          showOverridesDialog(context);
          return true;
        });
    presets.addPreference(overridesSummaryPref);
  }

  void refreshState(@NonNull String presetId, boolean busy) {
    overridesPresetId = presetId;
    overridesBusy = busy;
    updateOverridesSummaryUi();
  }

  void refreshOverridesSummary(
      @NonNull String presetId,
      boolean busy,
      boolean hasWallpaperOverride,
      boolean hasColorOverride,
      boolean hasTypographyOverride,
      boolean hasShadowsOverride) {
    overridesPresetId = presetId;
    overridesBusy = busy;
    overridesHasWallpaper = hasWallpaperOverride;
    overridesHasColors = hasColorOverride;
    overridesHasTypography = hasTypographyOverride;
    overridesHasShadows = hasShadowsOverride;
    updateOverridesSummaryUi();
  }

  private void updateOverridesSummaryUi() {
    final Preference pref = overridesSummaryPref;
    if (pref == null) return;

    final boolean hasAny =
        overridesHasWallpaper
            || overridesHasColors
            || overridesHasTypography
            || overridesHasShadows;
    pref.setEnabled(hasAny && !overridesBusy);

    if (!hasAny) {
      pref.setSummary(R.string.keyboard_theme_appearance_overrides_summary_none);
      return;
    }

    final Context context = pref.getContext();
    final String summary =
        buildOverridesSummary(
            context,
            overridesHasWallpaper,
            overridesHasColors,
            overridesHasTypography,
            overridesHasShadows);
    pref.setSummary(summary);
  }

  @NonNull
  private static String buildOverridesSummary(
      @NonNull Context context,
      boolean hasWallpaper,
      boolean hasColors,
      boolean hasTypography,
      boolean hasShadows) {
    final StringBuilder sb = new StringBuilder();
    if (hasWallpaper)
      sb.append(context.getString(R.string.keyboard_theme_appearance_background_title));
    if (hasColors) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(context.getString(R.string.keyboard_theme_appearance_colors_title));
    }
    if (hasTypography) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(context.getString(R.string.keyboard_theme_appearance_typography_title));
    }
    if (hasShadows) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(context.getString(R.string.keyboard_theme_appearance_shadows_title));
    }
    return context.getString(
        R.string.keyboard_theme_appearance_overrides_summary_some, sb.toString());
  }

  private void showOverridesDialog(@NonNull Context context) {
    final String presetId = overridesPresetId;
    if (presetId == null) return;

    final boolean busy = overridesBusy;
    final KeyboardWallpaperOverrideStore wallpaperStore = this.wallpaperStore;
    final KeyboardThemeUserOverridesStore overridesStore = this.themeOverridesStore;

    final androidx.appcompat.app.AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog);
    builder.setTitle(R.string.keyboard_theme_appearance_overrides_summary_title);
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

    final androidx.appcompat.app.AlertDialog[] dialogHolder =
        new androidx.appcompat.app.AlertDialog[1];
    final Runnable dismissDialog =
        () -> {
          final androidx.appcompat.app.AlertDialog dialog = dialogHolder[0];
          if (dialog != null) dialog.dismiss();
        };

    final android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
    final android.widget.LinearLayout root = new android.widget.LinearLayout(context);
    root.setOrientation(android.widget.LinearLayout.VERTICAL);
    final int pad =
        context.getResources().getDimensionPixelSize(R.dimen.global_content_padding_side);
    root.setPadding(pad, pad, pad, pad);
    scrollView.addView(
        root,
        new android.widget.ScrollView.LayoutParams(
            android.widget.ScrollView.LayoutParams.MATCH_PARENT,
            android.widget.ScrollView.LayoutParams.WRAP_CONTENT));

    if (overridesHasWallpaper) {
      final List<KeyboardThemeAppearanceOverrideItem> backgroundItems =
          KeyboardThemeAppearanceBackgroundOverrideItems.build(context, presetId, wallpaperStore);
      addOverridesDialogSection(
          context,
          root,
          R.string.keyboard_theme_appearance_background_title,
          busy,
          () -> {
            dismissDialog.run();
            host.scrollToPreference("section:background");
          },
          () -> {
            if (wallpaperStore != null) {
              wallpaperStore.clear(presetId);
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_wallpaper_customization_reset_toast,
                      Toast.LENGTH_SHORT)
                  .show();
            }
            dismissDialog.run();
          },
          backgroundItems,
          dismissDialog);
    }

    if (overridesHasColors) {
      final List<KeyboardThemeAppearanceOverrideItem> colorItems =
          KeyboardThemeAppearanceColorOverrideItems.build(context, presetId, overridesStore);
      addOverridesDialogSection(
          context,
          root,
          R.string.keyboard_theme_appearance_colors_title,
          busy,
          () -> {
            dismissDialog.run();
            host.scrollToPreference("section:colors");
          },
          () -> {
            if (overridesStore != null) {
              overridesStore.clearColorOverrides(presetId);
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_appearance_reset_colors_toast,
                      Toast.LENGTH_SHORT)
                  .show();
            }
            dismissDialog.run();
          },
          colorItems,
          dismissDialog);
    }

    if (overridesHasTypography) {
      final List<KeyboardThemeAppearanceOverrideItem> typographyItems =
          KeyboardThemeAppearanceTypographyOverrideItems.build(context, presetId, overridesStore);
      addOverridesDialogSection(
          context,
          root,
          R.string.keyboard_theme_appearance_typography_title,
          busy,
          () -> {
            dismissDialog.run();
            host.scrollToPreference("section:typography");
          },
          () -> {
            if (overridesStore != null) {
              overridesStore.clearTypographyOverrides(presetId);
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_appearance_reset_typography_toast,
                      Toast.LENGTH_SHORT)
                  .show();
            }
            dismissDialog.run();
          },
          typographyItems,
          dismissDialog);
    }

    if (overridesHasShadows) {
      final List<KeyboardThemeAppearanceOverrideItem> shadowItems =
          KeyboardThemeAppearanceShadowOverrideItems.build(context, presetId, overridesStore);
      addOverridesDialogSection(
          context,
          root,
          R.string.keyboard_theme_appearance_shadows_title,
          busy,
          () -> {
            dismissDialog.run();
            host.scrollToPreference("section:shadows");
          },
          () -> {
            if (overridesStore != null) {
              overridesStore.clearTextShadowOverrides(presetId);
              overridesStore.clearSpecialKeyTextShadowOverrides(presetId);
              overridesStore.clearModifierKeyTextShadowOverrides(presetId);
              overridesStore.clearEnterKeyTextShadowOverrides(presetId);
              overridesStore.clearSpacebarKeyTextShadowOverrides(presetId);
              overridesStore.clearKeyBackgroundShadowOverrides(presetId);
              overridesStore.clearSpecialKeyBackgroundShadowOverrides(presetId);
              overridesStore.clearModifierKeyBackgroundShadowOverrides(presetId);
              overridesStore.clearEnterKeyBackgroundShadowOverrides(presetId);
              overridesStore.clearSpacebarKeyBackgroundShadowOverrides(presetId);
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_appearance_reset_text_shadow_toast,
                      Toast.LENGTH_SHORT)
                  .show();
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_appearance_reset_key_shadow_toast,
                      Toast.LENGTH_SHORT)
                  .show();
            }
            dismissDialog.run();
          },
          shadowItems,
          dismissDialog);
    }

    builder.setView(scrollView);
    final androidx.appcompat.app.AlertDialog dialog = builder.show();
    dialogHolder[0] = dialog;

    // Ensure the summary state is re-evaluated after any action.
    dialog.setOnDismissListener(ignored -> host.refreshState());
  }

  private void addOverridesDialogSection(
      @NonNull Context context,
      @NonNull android.widget.LinearLayout parent,
      int titleResId,
      boolean busy,
      @NonNull Runnable editAction,
      @NonNull Runnable resetAction,
      @NonNull List<KeyboardThemeAppearanceOverrideItem> items,
      @NonNull Runnable dismissDialog) {
    final android.widget.LinearLayout header = new android.widget.LinearLayout(context);
    header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
    header.setGravity(android.view.Gravity.CENTER_VERTICAL);

    final android.widget.TextView title = new android.widget.TextView(context);
    title.setText(titleResId);
    title.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1f));
    header.addView(title);

    final android.widget.Button edit = new android.widget.Button(context);
    edit.setText(R.string.keyboard_theme_appearance_overrides_action_edit);
    edit.setEnabled(!busy);
    edit.setOnClickListener(
        ignored -> {
          editAction.run();
        });
    header.addView(edit);

    final android.widget.Button reset = new android.widget.Button(context);
    reset.setText(R.string.keyboard_theme_appearance_overrides_action_reset);
    reset.setEnabled(!busy);
    reset.setOnClickListener(
        ignored -> {
          resetAction.run();
        });
    header.addView(reset);

    parent.addView(header);

    for (KeyboardThemeAppearanceOverrideItem item : items) {
      addOverridesDialogItemRow(context, parent, item, busy, dismissDialog);
    }

    final android.widget.Space space = new android.widget.Space(context);
    space.setLayoutParams(
        new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(context, 8)));
    parent.addView(space);
  }

  private void addOverridesDialogItemRow(
      @NonNull Context context,
      @NonNull android.widget.LinearLayout parent,
      @NonNull KeyboardThemeAppearanceOverrideItem item,
      boolean busy,
      @NonNull Runnable dismissDialog) {
    final android.widget.LinearLayout row = new android.widget.LinearLayout(context);
    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
    row.setPadding(0, dpToPx(context, 6), 0, dpToPx(context, 6));

    final android.widget.LinearLayout text = new android.widget.LinearLayout(context);
    text.setOrientation(android.widget.LinearLayout.VERTICAL);
    text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1f));

    final android.widget.TextView title = new android.widget.TextView(context);
    title.setText(item.title);
    text.addView(title);

    if (item.summary != null) {
      final android.widget.TextView summary = new android.widget.TextView(context);
      summary.setText(item.summary);
      summary.setTextAppearance(
          context, androidx.appcompat.R.style.TextAppearance_AppCompat_Caption);
      text.addView(summary);
    }
    row.addView(text);

    row.setEnabled(!busy);
    row.setClickable(!busy);
    row.setOnClickListener(
        ignored -> {
          dismissDialog.run();
          host.scrollToPreference(item.scrollToKey);
        });

    final android.widget.Button reset = new android.widget.Button(context);
    reset.setText(R.string.keyboard_theme_appearance_overrides_action_reset);
    reset.setEnabled(!busy);
    reset.setOnClickListener(
        ignored -> {
          item.resetAction.run();
          dismissDialog.run();
        });
    row.addView(reset);

    parent.addView(row);
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    final float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
  }
}
