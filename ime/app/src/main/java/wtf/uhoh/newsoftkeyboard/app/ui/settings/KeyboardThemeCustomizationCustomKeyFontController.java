package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.io.File;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemeCustomizationCustomKeyFontController {

  enum CustomFontTarget {
    IMPORT_ONLY,
    KEY_FONT_FAMILY,
    TOKEN_SECONDARY_FONT_FAMILY,
    HINT_FONT_FAMILY,
    SUGGESTION_FONT_FAMILY,
    KEYBOARD_NAME_FONT_FAMILY
  }

  @NonNull private final KeyboardThemeCustomizationTypographySection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private Preference customFontMissingWarningPref;
  @Nullable private Preference importCustomKeyFontPref;
  @Nullable private Preference removeCustomKeyFontPref;

  @Nullable private Disposable fontImportDisposable;
  @Nullable private String pendingCustomKeyFontThemeId;
  @Nullable private CustomFontTarget pendingCustomKeyFontTarget;

  @Nullable private Disposable customFontValidationDisposable;
  @Nullable private String lastCustomFontValidationCacheKey;
  @Nullable private Boolean lastCustomFontLoadable;

  KeyboardThemeCustomizationCustomKeyFontController(
      @NonNull KeyboardThemeCustomizationTypographySection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory typography) {
    customFontMissingWarningPref = new Preference(context);
    customFontMissingWarningPref.setKey("keyboard_theme_override_key_custom_font_missing");
    customFontMissingWarningPref.setPersistent(false);
    customFontMissingWarningPref.setTitle(
        R.string.keyboard_theme_appearance_custom_font_missing_title);
    customFontMissingWarningPref.setSummary(
        R.string.keyboard_theme_appearance_custom_font_missing_summary);
    customFontMissingWarningPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          startCustomFontPick(themeId, CustomFontTarget.IMPORT_ONLY);
          return true;
        });
    customFontMissingWarningPref.setVisible(false);
    typography.addPreference(customFontMissingWarningPref);

    importCustomKeyFontPref = new Preference(context);
    importCustomKeyFontPref.setKey("keyboard_theme_override_key_custom_font_import");
    importCustomKeyFontPref.setPersistent(false);
    importCustomKeyFontPref.setTitle(R.string.keyboard_theme_appearance_custom_font_import_title);
    importCustomKeyFontPref.setSummary(
        R.string.keyboard_theme_appearance_custom_font_import_summary);
    importCustomKeyFontPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          startCustomFontPick(themeId, CustomFontTarget.IMPORT_ONLY);
          return true;
        });
    typography.addPreference(importCustomKeyFontPref);

    removeCustomKeyFontPref = new Preference(context);
    removeCustomKeyFontPref.setKey("keyboard_theme_override_key_custom_font_remove");
    removeCustomKeyFontPref.setPersistent(false);
    removeCustomKeyFontPref.setTitle(R.string.keyboard_theme_appearance_custom_font_remove_title);
    removeCustomKeyFontPref.setSummary(
        R.string.keyboard_theme_appearance_custom_font_remove_summary);
    removeCustomKeyFontPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          themeOverridesStore.clearCustomKeyFont(themeId);
          if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
              themeOverridesStore.getKeyFontFamily(themeId))) {
            themeOverridesStore.clearKeyFontFamily(themeId);
          }
          if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
              themeOverridesStore.getHintFontFamily(themeId))) {
            themeOverridesStore.clearHintFontFamily(themeId);
          }
          if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
              themeOverridesStore.getSuggestionFontFamily(themeId))) {
            themeOverridesStore.clearSuggestionFontFamily(themeId);
          }
          if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
              themeOverridesStore.getKeyboardNameFontFamily(themeId))) {
            themeOverridesStore.clearKeyboardNameFontFamily(themeId);
          }
          if (KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
              themeOverridesStore.getTokenSecondaryFontFamily(themeId))) {
            themeOverridesStore.clearTokenSecondaryFontFamily(themeId);
          }
          Toast.makeText(
                  context,
                  R.string.keyboard_theme_appearance_custom_font_remove_toast,
                  Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    typography.addPreference(removeCustomKeyFontPref);
  }

  boolean ensureCustomKeyFontAvailableOrStartPick(
      @NonNull Context context, @NonNull String themeId, @NonNull CustomFontTarget target) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    if (store.hasCustomKeyFont(themeId)) return true;

    startCustomFontPick(themeId, target);
    Toast.makeText(
            context,
            R.string.keyboard_theme_appearance_custom_font_pick_first_toast,
            Toast.LENGTH_SHORT)
        .show();
    return false;
  }

  boolean isFontImportInProgress() {
    return fontImportDisposable != null && !fontImportDisposable.isDisposed();
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    if (themeOverridesStore == null) return;

    final String keyFontFamily = themeOverridesStore.getKeyFontFamily(themeId);
    final String tokenSecondaryFontFamily =
        themeOverridesStore.getTokenSecondaryFontFamily(themeId);
    final String hintFontFamily = themeOverridesStore.getHintFontFamily(themeId);
    final String suggestionFontFamily = themeOverridesStore.getSuggestionFontFamily(themeId);
    final String keyboardNameFontFamily = themeOverridesStore.getKeyboardNameFontFamily(themeId);
    final boolean hasCustomKeyFont = themeOverridesStore.hasCustomKeyFont(themeId);
    final String customKeyFontName = themeOverridesStore.getCustomKeyFontDisplayName(themeId);

    final String resolvedHintFontFamily =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(hintFontFamily)
            ? tokenSecondaryFontFamily
            : hintFontFamily;
    final String resolvedSuggestionFontFamily =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(suggestionFontFamily)
            ? tokenSecondaryFontFamily
            : suggestionFontFamily;
    final String resolvedKeyboardNameFontFamily =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(
                keyboardNameFontFamily)
            ? tokenSecondaryFontFamily
            : keyboardNameFontFamily;
    final boolean customFontSelected =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(keyFontFamily)
            || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
                tokenSecondaryFontFamily)
            || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(resolvedHintFontFamily)
            || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
                resolvedSuggestionFontFamily)
            || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
                resolvedKeyboardNameFontFamily);
    final Boolean customFontLoadable =
        getOrStartCustomFontValidation(themeId, customFontSelected, hasCustomKeyFont);

    if (customFontMissingWarningPref != null) {
      customFontMissingWarningPref.setEnabled(!importInProgress);
      if (!customFontSelected) {
        customFontMissingWarningPref.setVisible(false);
      } else if (!hasCustomKeyFont) {
        customFontMissingWarningPref.setTitle(
            R.string.keyboard_theme_appearance_custom_font_missing_title);
        customFontMissingWarningPref.setSummary(
            R.string.keyboard_theme_appearance_custom_font_missing_summary);
        customFontMissingWarningPref.setVisible(true);
      } else if (Boolean.FALSE.equals(customFontLoadable)) {
        customFontMissingWarningPref.setTitle(
            R.string.keyboard_theme_appearance_custom_font_invalid_title);
        customFontMissingWarningPref.setSummary(
            R.string.keyboard_theme_appearance_custom_font_invalid_summary);
        customFontMissingWarningPref.setVisible(true);
      } else {
        customFontMissingWarningPref.setVisible(false);
      }
    }
    if (importCustomKeyFontPref != null) {
      importCustomKeyFontPref.setEnabled(!importInProgress);
      if (hasCustomKeyFont) {
        importCustomKeyFontPref.setSummary(
            customKeyFontName != null && !customKeyFontName.trim().isEmpty()
                ? contextString(
                    importCustomKeyFontPref,
                    R.string.keyboard_theme_appearance_custom_font_import_summary_imported,
                    customKeyFontName.trim())
                : contextString(
                    importCustomKeyFontPref,
                    R.string
                        .keyboard_theme_appearance_custom_font_import_summary_imported_unknown));
      } else {
        importCustomKeyFontPref.setSummary(
            R.string.keyboard_theme_appearance_custom_font_import_summary);
      }
    }
    if (removeCustomKeyFontPref != null) {
      removeCustomKeyFontPref.setEnabled(!importInProgress && hasCustomKeyFont);
    }
  }

  void onCustomKeyFontPicked(@NonNull Context context, @Nullable Uri uri) {
    final String themeId = pendingCustomKeyFontThemeId;
    final CustomFontTarget target = pendingCustomKeyFontTarget;
    pendingCustomKeyFontThemeId = null;
    pendingCustomKeyFontTarget = null;
    if (uri == null || themeId == null) return;

    if (themeOverridesStore == null) return;

    if (fontImportDisposable != null) {
      fontImportDisposable.dispose();
      fontImportDisposable = null;
    }

    fontImportDisposable =
        Single.fromCallable(
                () -> {
                  themeOverridesStore.importCustomKeyFontFromUri(themeId, uri);
                  return true;
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                ignored -> {
                  if (!host.isAdded() || themeOverridesStore == null) return;
                  if (target == CustomFontTarget.KEY_FONT_FAMILY) {
                    themeOverridesStore.setKeyFontFamily(
                        themeId, KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM);
                  } else if (target == CustomFontTarget.TOKEN_SECONDARY_FONT_FAMILY) {
                    themeOverridesStore.setTokenSecondaryFontFamily(
                        themeId, KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM);
                  } else if (target == CustomFontTarget.HINT_FONT_FAMILY) {
                    themeOverridesStore.setHintFontFamily(
                        themeId, KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM);
                  } else if (target == CustomFontTarget.SUGGESTION_FONT_FAMILY) {
                    themeOverridesStore.setSuggestionFontFamily(
                        themeId, KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM);
                  } else if (target == CustomFontTarget.KEYBOARD_NAME_FONT_FAMILY) {
                    themeOverridesStore.setKeyboardNameFontFamily(
                        themeId, KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM);
                  }
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_appearance_custom_font_import_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                  host.refreshState();
                },
                error -> {
                  if (!host.isAdded()) return;
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_appearance_custom_font_import_failed_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                  host.refreshState();
                });
    host.refreshState();
  }

  void dispose() {
    pendingCustomKeyFontThemeId = null;
    pendingCustomKeyFontTarget = null;

    if (fontImportDisposable != null) {
      fontImportDisposable.dispose();
      fontImportDisposable = null;
    }

    if (customFontValidationDisposable != null) {
      customFontValidationDisposable.dispose();
      customFontValidationDisposable = null;
    }
    lastCustomFontValidationCacheKey = null;
    lastCustomFontLoadable = null;
  }

  private void startCustomFontPick(@NonNull String themeId, @NonNull CustomFontTarget target) {
    pendingCustomKeyFontThemeId = themeId;
    pendingCustomKeyFontTarget = target;
    final ActivityResultLauncher<String[]> launcher = host.getPickKeyFontLauncher();
    if (launcher != null) {
      launcher.launch(
          new String[] {"font/*", "application/font-sfnt", "application/octet-stream", "*/*"});
    }
  }

  @Nullable
  private Boolean getOrStartCustomFontValidation(
      @NonNull String themeId, boolean customFontSelected, boolean hasCustomKeyFont) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (!customFontSelected || !hasCustomKeyFont || store == null) {
      if (customFontValidationDisposable != null) {
        customFontValidationDisposable.dispose();
        customFontValidationDisposable = null;
      }
      lastCustomFontValidationCacheKey = null;
      lastCustomFontLoadable = null;
      return null;
    }

    final File file = store.getCustomKeyFontFile(themeId);
    final String cacheKey = themeId + ":" + file.length() + ":" + file.lastModified();
    final boolean cacheKeyChanged = !cacheKey.equals(lastCustomFontValidationCacheKey);
    final boolean computeInProgress =
        customFontValidationDisposable != null && !customFontValidationDisposable.isDisposed();

    if (cacheKeyChanged || (!computeInProgress && lastCustomFontLoadable == null)) {
      if (customFontValidationDisposable != null) {
        customFontValidationDisposable.dispose();
        customFontValidationDisposable = null;
      }
      lastCustomFontValidationCacheKey = cacheKey;
      lastCustomFontLoadable = null;
      customFontValidationDisposable =
          Single.fromCallable(() -> store.canLoadCustomKeyFont(themeId))
              .subscribeOn(RxSchedulers.background())
              .observeOn(RxSchedulers.mainThread())
              .subscribe(
                  result -> {
                    if (!host.isAdded()) return;
                    lastCustomFontLoadable = result;
                    host.refreshState();
                  },
                  ignored -> {
                    if (!host.isAdded()) return;
                    lastCustomFontValidationCacheKey = null;
                    lastCustomFontLoadable = null;
                    host.refreshState();
                  });
    }

    return lastCustomFontLoadable;
  }

  @NonNull
  private static String contextString(@NonNull Preference preference, int resId, Object... args) {
    if (args == null || args.length == 0) {
      return preference.getContext().getString(resId);
    } else {
      return preference.getContext().getString(resId, args);
    }
  }
}
