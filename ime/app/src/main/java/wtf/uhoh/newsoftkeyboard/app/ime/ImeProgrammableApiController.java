package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.inputmethod.EditorInfoCompat;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.List;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.NextKeyboardType;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickTextKey;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickTextKeyFactory;

public final class ImeProgrammableApiController {

  @NonNull private final ImeServiceBase imeService;

  ImeProgrammableApiController(@NonNull ImeServiceBase imeService) {
    this.imeService = imeService;
  }

  public boolean isInAlphabetModeForProgrammableApi() {
    return imeService.isInAlphabetKeyboardMode();
  }

  public boolean isInPasswordFieldForProgrammableApi() {
    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    if (ImeClipboard.isTextPassword(editorInfo)) return true;
    return editorInfo != null && ImeIncognito.isNumberPassword(editorInfo);
  }

  @Nullable
  public String getCurrentLocaleForProgrammableApi() {
    final KeyboardDefinition alphabetKeyboard = imeService.getCurrentAlphabetKeyboard();
    if (alphabetKeyboard == null) return null;
    final Locale locale = alphabetKeyboard.getLocale();
    if (Locale.ROOT.equals(locale)) return null;
    return locale.toLanguageTag();
  }

  @Nullable
  public String getCurrentKeyboardIdForProgrammableApi() {
    final KeyboardDefinition keyboard = imeService.getCurrentKeyboard();
    if (keyboard == null) return null;
    return keyboard.getKeyboardId();
  }

  public boolean switchLanguageForProgrammableApi(@Nullable String keyboardId, boolean previous) {
    imeService.abortCorrectionAndResetPredictionState(false);

    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    if (keyboardId != null && !keyboardId.trim().isEmpty()) {
      return imeService.getKeyboardSwitcher().nextAlphabetKeyboard(editorInfo, keyboardId) != null;
    }

    final List<KeyboardAddOnAndBuilder> enabledKeyboards =
        imeService.getKeyboardSwitcher().getEnabledKeyboardsBuilders();
    if (enabledKeyboards.isEmpty()) return false;

    int baseIndex = -1;
    final KeyboardDefinition currentAlphabetKeyboard = imeService.getCurrentAlphabetKeyboard();
    if (currentAlphabetKeyboard != null) {
      final String currentId = currentAlphabetKeyboard.getKeyboardId();
      for (int i = 0; i < enabledKeyboards.size(); i++) {
        if (TextUtils.equals(enabledKeyboards.get(i).getId(), currentId)) {
          baseIndex = i;
          break;
        }
      }
    }

    final int targetIndex;
    if (previous) {
      targetIndex =
          baseIndex == -1
              ? enabledKeyboards.size() - 1
              : (baseIndex - 1 + enabledKeyboards.size()) % enabledKeyboards.size();
    } else {
      targetIndex = (baseIndex + 1) % enabledKeyboards.size();
    }

    return imeService
            .getKeyboardSwitcher()
            .nextAlphabetKeyboard(editorInfo, enabledKeyboards.get(targetIndex).getId())
        != null;
  }

  public boolean switchKeyboardModeForProgrammableApi(@Nullable String mode) {
    imeService.abortCorrectionAndResetPredictionState(false);

    final EditorInfo editorInfo = imeService.currentInputEditorInfo();

    if (mode == null
        || mode.trim().isEmpty()
        || KeyboardApiContract.KEYBOARD_MODE_TOGGLE.equals(mode)) {
      imeService.getKeyboardSwitcher().nextKeyboard(editorInfo, NextKeyboardType.OtherMode);
      return true;
    }

    switch (mode) {
      case KeyboardApiContract.KEYBOARD_MODE_ALPHABET:
        if (!imeService.isInAlphabetKeyboardMode()) {
          imeService.getKeyboardSwitcher().nextKeyboard(editorInfo, NextKeyboardType.Alphabet);
        }
        return true;
      case KeyboardApiContract.KEYBOARD_MODE_SYMBOLS:
        if (imeService.isInAlphabetKeyboardMode()) {
          imeService.getKeyboardSwitcher().nextKeyboard(editorInfo, NextKeyboardType.Symbols);
        }
        return true;
      default:
        return false;
    }
  }

  public boolean sendNavigationKeyForProgrammableApi(int keyCode) {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    sendNavigationKeyEvent(keyCode);
    return true;
  }

  public boolean sendTabForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    TerminalKeySender.sendTab(
        inputConnectionRouter,
        TerminalKeySender.isTerminalEmulation(imeService.currentInputEditorInfo()));
    return true;
  }

  public boolean sendEscapeForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    final boolean terminalEmulation =
        TerminalKeySender.isTerminalEmulation(imeService.currentInputEditorInfo());
    TerminalKeySender.sendEscape(
        inputConnectionRouter,
        terminalEmulation,
        () -> inputConnectionRouter.commitText("\u001b", 1));
    return true;
  }

  public boolean clipboardCopyForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    imeService.handleClipboardOperation(null, KeyCodes.CLIPBOARD_COPY, inputConnectionRouter);
    return true;
  }

  public boolean clipboardCutForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    imeService.handleClipboardOperation(null, KeyCodes.CLIPBOARD_CUT, inputConnectionRouter);
    return true;
  }

  public boolean clipboardPasteForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    imeService.handleClipboardOperation(null, KeyCodes.CLIPBOARD_PASTE, inputConnectionRouter);
    return true;
  }

  public boolean clipboardSelectAllForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    imeService.handleClipboardOperation(null, KeyCodes.CLIPBOARD_SELECT_ALL, inputConnectionRouter);
    return true;
  }

  public boolean undoForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    imeService.handleClipboardOperation(null, KeyCodes.UNDO, inputConnectionRouter);
    return true;
  }

  public boolean redoForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;
    imeService.handleClipboardOperation(null, KeyCodes.REDO, inputConnectionRouter);
    return true;
  }

  public boolean runSnippetForProgrammableApi(@NonNull String snippetId) {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;

    final QuickTextKeyFactory factory = NskApplicationBase.getQuickTextKeyFactory(imeService);
    if (!factory.isAddOnEnabled(snippetId)) return false;

    final QuickTextKey quickTextKey = factory.getAddOnById(snippetId);
    if (quickTextKey == null) return false;

    final CharSequence output = quickTextKey.getKeyOutputText();
    if (output == null || output.length() == 0) return false;

    imeService.abortCorrectionAndResetPredictionState(false);
    imeService.onText(null, output);
    return true;
  }

  public boolean openMediaInsertionUiForProgrammableApi() {
    final InputConnectionRouter inputConnectionRouter =
        imeService.getImeSessionState().getInputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) return false;

    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    final String[] mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
    if (mimeTypes == null || mimeTypes.length == 0) return false;

    imeService.handleMediaInsertionKey();
    return true;
  }

  @Nullable
  public String getCurrentEditorPackageNameForProgrammableApi() {
    final EditorInfo editorInfo = imeService.currentInputEditorInfo();
    if (editorInfo == null) return null;
    final String pkg = editorInfo.packageName;
    if (pkg == null) return null;
    final String trimmed = pkg.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public boolean setSessionPresetForProgrammableApi(@NonNull String presetId) {
    return imeService.getSessionOverridesController().setSessionPresetForProgrammableApi(presetId);
  }

  public boolean setSessionThemePresetForProgrammableApi(@NonNull String presetId) {
    return imeService
        .getSessionOverridesController()
        .setSessionThemePresetForProgrammableApi(presetId);
  }

  public boolean setSessionKeyboardIdForProgrammableApi(@NonNull String keyboardId) {
    return imeService
        .getSessionOverridesController()
        .setSessionKeyboardIdForProgrammableApi(keyboardId);
  }

  public void clearSessionOverridesForProgrammableApi() {
    imeService.getSessionOverridesController().clearSessionOverridesForProgrammableApi();
  }

  private void sendNavigationKeyEvent(int keyEventCode) {
    final InputViewBinder inputView = imeService.getInputView();
    final boolean temporarilyDisableShift = inputView != null && inputView.isShifted();
    if (temporarilyDisableShift) {
      inputView.setShifted(false);
    }
    imeService.sendDownUpKeyEvents(keyEventCode, 0);
    if (temporarilyDisableShift) {
      imeService.handleShift();
    }
  }
}
