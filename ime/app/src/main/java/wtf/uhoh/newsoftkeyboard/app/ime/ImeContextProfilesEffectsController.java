package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ContextProfileAwareSuggest;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.ime.context.ContextProfilesController;
import wtf.uhoh.newsoftkeyboard.app.ime.context.TypedRulesApplier;
import wtf.uhoh.newsoftkeyboard.app.ime.context.VoiceTextPostProcessResult;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class ImeContextProfilesEffectsController {

  @NonNull private final ImeServiceBase service;
  @NonNull private final ContextProfilesController controller;
  @NonNull private final ContextProfilesStore store;

  private boolean temporarilyDisabled;

  @Nullable private VoiceTextPostProcessResult.VoiceSuggestion pendingVoiceSuggestion;
  @Nullable private VoiceTextPostProcessResult.VoiceSuggestion activeVoiceSuggestion;
  @Nullable private TypedRulesApplier.TypedSuggestion activeTypedSuggestion;

  ImeContextProfilesEffectsController(@NonNull ImeServiceBase service) {
    this.service = service;
    controller = new ContextProfilesController(service);
    store = new ContextProfilesStore(service);
  }

  @Nullable
  ContextProfilesController getContextProfilesController() {
    return controller;
  }

  @Nullable
  ContextProfilesStore getContextProfilesStore() {
    return store;
  }

  boolean areTemporarilyDisabled() {
    return temporarilyDisabled;
  }

  void onStartInputView(@NonNull EditorInfo editorInfo) {
    pendingVoiceSuggestion = null;
    activeVoiceSuggestion = null;
    controller.onStartInputView(editorInfo);
    applyContextProfileOverridesForCurrentEditor(editorInfo);
  }

  void applyContextProfileOverridesForCurrentEditor(@NonNull EditorInfo editorInfo) {
    final Suggest suggest = service.getSuggest();
    if (!(suggest instanceof ContextProfileAwareSuggest)) return;
    final ContextProfileAwareSuggest contextAwareSuggest = (ContextProfileAwareSuggest) suggest;

    if (temporarilyDisabled) {
      contextAwareSuggest.setContextProfileSafeToggles(null);
      contextAwareSuggest.setContextProfileWordList(null, 0L);
      return;
    }

    if (ImeServiceBase.isTextPassword(editorInfo)
        || ImeServiceBase.isNumberPassword(editorInfo)
        || suggest.isIncognitoMode()) {
      contextAwareSuggest.setContextProfileSafeToggles(null);
      contextAwareSuggest.setContextProfileWordList(null, 0L);
      return;
    }

    final ContextProfilesStore.Preset preset = controller.getActivePreset();
    contextAwareSuggest.setContextProfileSafeToggles(preset == null ? null : preset.safeToggles);

    final String presetId = preset == null ? null : preset.id;
    final long generation = presetId == null ? 0L : store.getWordListGeneration(presetId);
    contextAwareSuggest.setContextProfileWordList(presetId, generation);
  }

  void clearContextProfileOverrides() {
    final Suggest suggest = service.getSuggest();
    if (suggest instanceof ContextProfileAwareSuggest) {
      final ContextProfileAwareSuggest contextAwareSuggest = (ContextProfileAwareSuggest) suggest;
      contextAwareSuggest.setContextProfileSafeToggles(null);
      contextAwareSuggest.setContextProfileWordList(null, 0L);
    }
  }

  @NonNull
  String onVoiceTextPreCommit(@NonNull String formattedText) {
    if (temporarilyDisabled) return formattedText;
    final EditorInfo editorInfo = service.currentInputEditorInfo();
    if (editorInfo == null) return formattedText;
    if (ImeServiceBase.isTextPassword(editorInfo) || ImeServiceBase.isNumberPassword(editorInfo)) {
      return formattedText;
    }
    if (service.getSuggest().isIncognitoMode()) return formattedText;

    try {
      final VoiceTextPostProcessResult result =
          controller.postProcessVoiceTextWithSuggestion(formattedText, editorInfo);
      final VoiceTextPostProcessResult.VoiceSuggestion suggestion = result.suggestion();
      if (suggestion != null) {
        pendingVoiceSuggestion = suggestion;
        activeVoiceSuggestion = null;
        service.markExpectingSelectionUpdate();
      } else {
        pendingVoiceSuggestion = null;
        activeVoiceSuggestion = null;
        if (!result.textToCommit().equals(formattedText)) {
          service.markExpectingSelectionUpdate();
        }
      }
      return result.textToCommit();
    } catch (Throwable t) {
      pendingVoiceSuggestion = null;
      activeVoiceSuggestion = null;
      return formattedText;
    }
  }

  void showPendingVoiceSuggestionIfAny() {
    final VoiceTextPostProcessResult.VoiceSuggestion pending = pendingVoiceSuggestion;
    if (pending == null) return;

    pendingVoiceSuggestion = null;
    activeVoiceSuggestion = pending;
    service.setSuggestions(Collections.singletonList(pending.suggestionText()), 0);
  }

  boolean handlePickSuggestionManually(
      int index, @Nullable CharSequence suggestion, boolean withAutoSpaceEnabled) {
    if (activeVoiceSuggestion != null) {
      tryApplyActiveVoiceSuggestion();
      pendingVoiceSuggestion = null;
      activeVoiceSuggestion = null;
      service.clearSuggestions();
      return true;
    }

    if (activeTypedSuggestion != null) {
      final TypedRulesApplier.TypedSuggestion typedSuggestion = activeTypedSuggestion;
      if (typedSuggestion != null
          && index == 0
          && TextUtils.equals(suggestion, typedSuggestion.suggestionText())) {
        tryApplyActiveTypedSuggestion();
        activeTypedSuggestion = null;
        dismissTypedSuggestionFromSuggestionsStrip();
        return true;
      } else {
        activeTypedSuggestion = null;
        final int adjustedIndex = index > 0 ? index - 1 : index;
        service.pickSuggestionManuallyFromContextProfilesController(
            adjustedIndex, suggestion, withAutoSpaceEnabled);
        return true;
      }
    }

    return false;
  }

  void clearVoiceSuggestionState() {
    if (pendingVoiceSuggestion == null
        && activeVoiceSuggestion == null
        && activeTypedSuggestion == null) return;
    pendingVoiceSuggestion = null;
    activeVoiceSuggestion = null;
    activeTypedSuggestion = null;
    service.clearSuggestions();
  }

  void applyContextProfileTypedRulesForEditorAction(@Nullable EditorInfo editorInfo) {
    if (editorInfo == null) return;
    applyContextProfileTypedRules(editorInfo, false /*allowSuggestions*/);
  }

  void applyContextProfileTypedRulesOnSeparator(@NonNull EditorInfo editorInfo) {
    applyContextProfileTypedRules(editorInfo, true /*allowSuggestions*/);
  }

  private void applyContextProfileTypedRules(
      @NonNull EditorInfo editorInfo, boolean allowSuggestions) {
    if (temporarilyDisabled) return;
    if (ImeServiceBase.isTextPassword(editorInfo) || ImeServiceBase.isNumberPassword(editorInfo))
      return;
    if (service.getSuggest().isIncognitoMode()) return;

    final ContextProfilesStore.Preset preset = controller.getActivePreset();
    if (preset == null || preset.typedRules.isEmpty()) return;

    final InputConnectionRouter router = service.getImeSessionState().getInputConnectionRouter();
    if (!router.hasConnection()) return;

    final CharSequence beforeCursor =
        router.getTextBeforeCursor(TypedRulesApplier.MAX_MATCH_CHARS + 64, 0);
    if (beforeCursor == null) return;

    final boolean noSuggestionsField =
        (editorInfo.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
            == EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    final boolean allowAutoApply =
        !noSuggestionsField || preset.allowAutoApplyInNoSuggestionsFields;

    try {
      final TypedRulesApplier.Result result =
          TypedRulesApplier.resolve(
              beforeCursor.toString(), preset.typedRules, allowSuggestions && !noSuggestionsField);

      final TypedRulesApplier.Replacement autoReplacement = result.autoReplacement();
      if (autoReplacement != null) {
        if (!allowAutoApply) return;
        final int deleteCount = autoReplacement.committedText().length();
        if (deleteCount <= 0) return;
        final CharSequence verify = router.getTextBeforeCursor(deleteCount, 0);
        if (verify == null || !verify.toString().endsWith(autoReplacement.committedText())) return;

        router.beginBatchEdit();
        try {
          service.markExpectingSelectionUpdate();
          router.deleteSurroundingText(deleteCount, 0);
          router.commitText(autoReplacement.replacementText(), 1);
        } finally {
          router.endBatchEdit();
        }

        service.postUpdateSuggestions();
        return;
      }

      final TypedRulesApplier.TypedSuggestion suggestion = result.suggestion();
      if (suggestion == null) return;

      activeTypedSuggestion = suggestion;
      showTypedSuggestionInSuggestionsStrip(suggestion);
    } catch (Throwable ignored) {
      // ignore runtime errors and keep typed text unchanged
    }
  }

  private void showTypedSuggestionInSuggestionsStrip(
      @NonNull TypedRulesApplier.TypedSuggestion suggestion) {
    final ArrayList<CharSequence> merged = new ArrayList<>();
    merged.add(suggestion.suggestionText());

    final CandidateView candidateView = service.getCandidateViewForTests();
    final List<CharSequence> existing =
        candidateView == null ? null : candidateView.getSuggestions();
    if (existing != null) {
      for (CharSequence existingSuggestion : existing) {
        if (existingSuggestion == null) continue;
        if (TextUtils.equals(existingSuggestion, suggestion.suggestionText())) continue;
        merged.add(existingSuggestion);
      }
    }

    service.setSuggestions(merged, 0);
  }

  private void dismissTypedSuggestionFromSuggestionsStrip() {
    final CandidateView candidateView = service.getCandidateViewForTests();
    final List<CharSequence> current =
        candidateView == null ? null : candidateView.getSuggestions();
    if (current == null || current.size() <= 1) {
      service.clearSuggestions();
      return;
    }

    final ArrayList<CharSequence> withoutTypedSuggestion = new ArrayList<>(current.size() - 1);
    for (int i = 1; i < current.size(); i++) {
      final CharSequence item = current.get(i);
      if (item == null) continue;
      withoutTypedSuggestion.add(item);
    }
    service.setSuggestions(withoutTypedSuggestion, -1);
  }

  private boolean tryApplyActiveVoiceSuggestion() {
    final VoiceTextPostProcessResult.VoiceSuggestion suggestion = activeVoiceSuggestion;
    if (suggestion == null) return false;

    final InputConnectionRouter router = service.getImeSessionState().getInputConnectionRouter();
    if (!router.hasConnection()) return false;

    final String committedText = suggestion.committedText();
    if (committedText.isEmpty()) return false;
    final int length = committedText.length();

    final CharSequence before = router.getTextBeforeCursor(length, 0);
    if (before != null && before.toString().endsWith(committedText)) {
      router.beginBatchEdit();
      try {
        service.markExpectingSelectionUpdate();
        router.deleteSurroundingText(length, 0);
        router.commitText(suggestion.replacementText(), 1);
      } finally {
        router.endBatchEdit();
      }
      return true;
    }

    final CharSequence after = router.getTextAfterCursor(length, 0);
    if (after != null && after.toString().startsWith(committedText)) {
      router.beginBatchEdit();
      try {
        service.markExpectingSelectionUpdate();
        router.deleteSurroundingText(0, length);
        router.commitText(suggestion.replacementText(), 0);
      } finally {
        router.endBatchEdit();
      }
      return true;
    }

    return false;
  }

  private boolean tryApplyActiveTypedSuggestion() {
    final TypedRulesApplier.TypedSuggestion suggestion = activeTypedSuggestion;
    if (suggestion == null) return false;

    final InputConnectionRouter router = service.getImeSessionState().getInputConnectionRouter();
    if (!router.hasConnection()) return false;

    final String committedText = suggestion.committedText();
    if (committedText.isEmpty()) return false;
    final int length = committedText.length();

    final CharSequence before = router.getTextBeforeCursor(length, 0);
    if (before != null && before.toString().endsWith(committedText)) {
      router.beginBatchEdit();
      try {
        service.markExpectingSelectionUpdate();
        router.deleteSurroundingText(length, 0);
        router.commitText(suggestion.replacementText(), 1);
      } finally {
        router.endBatchEdit();
      }
      return true;
    }

    final CharSequence after = router.getTextAfterCursor(length, 0);
    if (after != null && after.toString().startsWith(committedText)) {
      router.beginBatchEdit();
      try {
        service.markExpectingSelectionUpdate();
        router.deleteSurroundingText(0, length);
        router.commitText(suggestion.replacementText(), 0);
      } finally {
        router.endBatchEdit();
      }
      return true;
    }

    return false;
  }

  boolean isContextProfilesGloballyEnabledForOptionsMenu() {
    return store.isEnabled();
  }

  boolean isContextProfilesEnabledForOptionsMenu() {
    return isContextProfilesGloballyEnabledForOptionsMenu() && !temporarilyDisabled;
  }

  void setTemporarilyDisabledForOptionsMenu(boolean disabled) {
    temporarilyDisabled = disabled;
    if (disabled) {
      clearVoiceSuggestionState();
      clearContextProfileOverrides();
    } else {
      final EditorInfo editorInfo = service.currentInputEditorInfo();
      if (editorInfo != null) applyContextProfileOverridesForCurrentEditor(editorInfo);
    }
    Toast.makeText(
            service.getApplicationContext(),
            disabled
                ? R.string.context_profiles_options_menu_toast_temporarily_disabled
                : R.string.context_profiles_options_menu_toast_enabled,
            Toast.LENGTH_SHORT)
        .show();
  }
}
