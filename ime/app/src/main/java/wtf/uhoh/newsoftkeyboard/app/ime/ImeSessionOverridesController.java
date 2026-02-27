package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.ime.context.ContextProfilesController;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeSessionOverrideStore;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class ImeSessionOverridesController {

  @NonNull private final ImeServiceBase imeService;

  @Nullable private String overridePackageName;
  @Nullable private String overrideContextPresetId;
  @Nullable private String overrideThemePresetId;
  @Nullable private String overrideKeyboardId;
  @Nullable private String overridePreviousKeyboardId;
  @Nullable private SessionOverridesStripAction sessionOverridesStripAction;
  private boolean restoreActionsStripAfterSessionOverride;

  ImeSessionOverridesController(@NonNull ImeServiceBase imeService) {
    this.imeService = imeService;
  }

  void maybeClearSessionOverridesForNewEditor(@NonNull EditorInfo editorInfo) {
    if (!hasAnySessionOverrides()) return;

    final String currentPackageName = trimOrNull(editorInfo.packageName);
    final String currentOverridePackageName = overridePackageName;
    final boolean samePackage =
        currentOverridePackageName != null
            && currentPackageName != null
            && currentOverridePackageName.equals(currentPackageName);

    if (!samePackage) {
      clearSessionOverridesInternal(null, currentOverridePackageName, false);
      return;
    }

    if (ImeClipboard.isTextPassword(editorInfo)
        || ImeIncognito.isNumberPassword(editorInfo)
        || imeService.suggest().isIncognitoMode()) {
      clearSessionOverridesInternal(null, currentPackageName, false);
    }
  }

  void applySessionKeyboardOverrideIfAny(@NonNull EditorInfo editorInfo) {
    final String currentOverrideKeyboardId = overrideKeyboardId;
    final String currentOverridePackageName = overridePackageName;
    if (currentOverrideKeyboardId == null || currentOverridePackageName == null) return;

    final String currentPackageName = trimOrNull(editorInfo.packageName);
    if (currentPackageName == null || !currentOverridePackageName.equals(currentPackageName))
      return;

    if (ImeClipboard.isTextPassword(editorInfo)
        || ImeIncognito.isNumberPassword(editorInfo)
        || imeService.suggest().isIncognitoMode()) return;

    final KeyboardDefinition currentAlphabetKeyboard = imeService.getCurrentAlphabetKeyboard();
    if (currentAlphabetKeyboard != null
        && currentOverrideKeyboardId.equals(currentAlphabetKeyboard.getKeyboardId())) {
      return;
    }

    try {
      final KeyboardDefinition switched =
          imeService
              .getKeyboardSwitcher()
              .nextAlphabetKeyboardForSessionOverride(editorInfo, currentOverrideKeyboardId);
      if (switched == null) {
        overrideKeyboardId = null;
        overridePreviousKeyboardId = null;
      }
    } catch (Throwable t) {
      overrideKeyboardId = null;
      overridePreviousKeyboardId = null;
    }
  }

  void clearSessionOverridesOnFinishInputView() {
    if (!hasAnySessionOverrides()) return;
    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    final String pkg = editorInfo == null ? null : trimOrNull(editorInfo.packageName);
    clearSessionOverridesInternal(null, pkg != null ? pkg : overridePackageName, false);
  }

  void updateSessionOverridesIndicator() {
    final KeyboardViewContainerView container = imeService.getInputViewContainer();
    if (container == null) return;

    if (sessionOverridesStripAction == null) {
      sessionOverridesStripAction =
          new SessionOverridesStripAction(this::clearSessionOverridesFromUi);
    }

    if (hasAnySessionOverrides()) {
      final CandidateView candidateView = container.getCandidateView();
      if (candidateView != null && candidateView.getVisibility() != View.VISIBLE) {
        restoreActionsStripAfterSessionOverride = true;
        container.setActionsStripVisibility(true);
      }
      container.addStripAction(sessionOverridesStripAction, true);
    } else if (sessionOverridesStripAction != null) {
      container.removeStripAction(sessionOverridesStripAction);
      if (restoreActionsStripAfterSessionOverride) {
        restoreActionsStripAfterSessionOverride = false;
        container.setActionsStripVisibility(false);
      }
    }
  }

  boolean setSessionPresetForProgrammableApi(@NonNull String presetId) {
    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    if (editorInfo == null) return false;
    if (ImeClipboard.isTextPassword(editorInfo)
        || ImeIncognito.isNumberPassword(editorInfo)
        || imeService.suggest().isIncognitoMode()) {
      return false;
    }

    if (imeService.areContextProfilesTemporarilyDisabled()) return false;
    final ContextProfilesController controller = imeService.getContextProfilesController();
    final ContextProfilesStore store = imeService.getContextProfilesStore();
    if (controller == null || store == null) return false;
    if (!store.isEnabled()) return false;

    final String pkg = trimOrNull(editorInfo.packageName);
    if (pkg == null) return false;

    final String trimmedPresetId = presetId.trim();
    if (trimmedPresetId.isEmpty()) return false;

    if (!controller.setSessionOverridePreset(pkg, trimmedPresetId)) return false;
    overridePackageName = pkg;
    overrideContextPresetId = trimmedPresetId;

    controller.onStartInputView(editorInfo);
    imeService.applyContextProfileOverridesForCurrentEditor(editorInfo);
    updateSessionOverridesIndicator();
    return true;
  }

  boolean setSessionThemePresetForProgrammableApi(@NonNull String presetId) {
    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    if (editorInfo == null) return false;
    if (ImeClipboard.isTextPassword(editorInfo)
        || ImeIncognito.isNumberPassword(editorInfo)
        || imeService.suggest().isIncognitoMode()) {
      return false;
    }

    final String pkg = trimOrNull(editorInfo.packageName);
    if (pkg == null) return false;

    final KeyboardTheme theme = imeService.mCurrentTheme;
    if (theme == null) return false;
    final String baseThemeId = theme.getId();
    if (baseThemeId == null || baseThemeId.trim().isEmpty()) return false;

    final String trimmedPresetId = presetId.trim();
    if (trimmedPresetId.isEmpty()) return false;
    if (!isValidThemePresetIdForBaseTheme(baseThemeId, trimmedPresetId)) return false;

    KeyboardThemeSessionOverrideStore.setOverride(pkg, baseThemeId, trimmedPresetId);
    overridePackageName = pkg;
    overrideThemePresetId = trimmedPresetId;

    imeService.refreshThemeAndOverlayForSessionOverride();
    updateSessionOverridesIndicator();
    return true;
  }

  boolean setSessionKeyboardIdForProgrammableApi(@NonNull String keyboardId) {
    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    if (editorInfo == null) return false;
    if (ImeClipboard.isTextPassword(editorInfo)
        || ImeIncognito.isNumberPassword(editorInfo)
        || imeService.suggest().isIncognitoMode()) {
      return false;
    }

    final String pkg = trimOrNull(editorInfo.packageName);
    if (pkg == null) return false;

    final String trimmedKeyboardId = keyboardId.trim();
    if (trimmedKeyboardId.isEmpty()) return false;
    if (!isEnabledKeyboardId(trimmedKeyboardId)) return false;

    final KeyboardDefinition currentAlphabetKeyboard = imeService.getCurrentAlphabetKeyboard();
    final String currentAlphabetKeyboardId =
        currentAlphabetKeyboard == null ? null : currentAlphabetKeyboard.getKeyboardId();

    imeService.abortCorrectionAndResetPredictionState(false);
    final KeyboardDefinition switched =
        imeService
            .getKeyboardSwitcher()
            .nextAlphabetKeyboardForSessionOverride(editorInfo, trimmedKeyboardId);
    if (switched == null) return false;

    overridePackageName = pkg;
    overrideKeyboardId = trimmedKeyboardId;
    overridePreviousKeyboardId = currentAlphabetKeyboardId;

    updateSessionOverridesIndicator();
    return true;
  }

  void clearSessionOverridesForProgrammableApi() {
    clearSessionOverridesInternal(
        imeService.currentInputEditorInfo(),
        imeService.getProgrammableApiController().getCurrentEditorPackageNameForProgrammableApi(),
        true);
  }

  private boolean isEnabledKeyboardId(@NonNull String keyboardId) {
    final List<KeyboardAddOnAndBuilder> enabledKeyboards =
        imeService.getKeyboardSwitcher().getEnabledKeyboardsBuilders();
    if (enabledKeyboards.isEmpty()) return false;
    for (KeyboardAddOnAndBuilder builder : enabledKeyboards) {
      if (builder == null) continue;
      if (keyboardId.equals(builder.getId())) return true;
    }
    return false;
  }

  private boolean isValidThemePresetIdForBaseTheme(
      @NonNull String baseThemeId, @NonNull String presetId) {
    if (presetId.equals(baseThemeId)) return true;
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(imeService);
    for (KeyboardThemePresetStore.Preset preset : presetStore.listPresets(baseThemeId)) {
      if (presetId.equals(preset.id())) {
        return true;
      }
    }
    return false;
  }

  private void clearSessionOverridesFromUi() {
    clearSessionOverridesInternal(
        imeService.currentInputEditorInfo(),
        imeService.getProgrammableApiController().getCurrentEditorPackageNameForProgrammableApi(),
        true);
  }

  private void clearSessionOverridesInternal(
      @Nullable EditorInfo editorInfoForRestore,
      @Nullable String packageNameToClear,
      boolean restoreKeyboard) {
    final String pkg = packageNameToClear == null ? null : packageNameToClear.trim();

    final String previousThemePresetId = overrideThemePresetId;
    final String previousContextPresetId = overrideContextPresetId;
    final String previousKeyboardId = overrideKeyboardId;
    final String previousKeyboardRestoreId = overridePreviousKeyboardId;

    if (restoreKeyboard && editorInfoForRestore != null && previousKeyboardRestoreId != null) {
      final String editorPkg = trimOrNull(editorInfoForRestore.packageName);
      if (editorPkg != null && editorPkg.equals(pkg)) {
        restoreSessionKeyboardIfNeeded(
            editorInfoForRestore, previousKeyboardId, previousKeyboardRestoreId);
      }
    }

    overridePackageName = null;
    overrideContextPresetId = null;
    overrideThemePresetId = null;
    overrideKeyboardId = null;
    overridePreviousKeyboardId = null;

    final ContextProfilesController contextProfilesController =
        imeService.getContextProfilesController();
    if (contextProfilesController != null) {
      contextProfilesController.clearSessionOverride();
    }
    if (pkg != null) {
      KeyboardThemeSessionOverrideStore.clearForPackage(pkg);
    } else {
      KeyboardThemeSessionOverrideStore.clearAll();
    }

    if (editorInfoForRestore != null && contextProfilesController != null) {
      contextProfilesController.onStartInputView(editorInfoForRestore);
      imeService.applyContextProfileOverridesForCurrentEditor(editorInfoForRestore);
    } else if (previousContextPresetId != null) {
      imeService.clearContextProfileOverrides();
    }

    if (editorInfoForRestore != null && previousThemePresetId != null) {
      imeService.refreshThemeAndOverlayForSessionOverride();
    }

    updateSessionOverridesIndicator();
  }

  private void restoreSessionKeyboardIfNeeded(
      @NonNull EditorInfo editorInfo,
      @Nullable String overriddenKeyboardId,
      @NonNull String restoreKeyboardId) {
    if (overriddenKeyboardId == null) return;
    final KeyboardDefinition currentAlphabetKeyboard = imeService.getCurrentAlphabetKeyboard();
    if (currentAlphabetKeyboard == null) return;
    if (!overriddenKeyboardId.equals(currentAlphabetKeyboard.getKeyboardId())) return;

    imeService.abortCorrectionAndResetPredictionState(false);
    imeService
        .getKeyboardSwitcher()
        .nextAlphabetKeyboardForSessionOverride(editorInfo, restoreKeyboardId);
  }

  private boolean hasAnySessionOverrides() {
    return overrideContextPresetId != null
        || overrideThemePresetId != null
        || overrideKeyboardId != null;
  }

  @Nullable
  private static String trimOrNull(@Nullable String value) {
    if (value == null) return null;
    final String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
