package wtf.uhoh.newsoftkeyboard.app.ime.context;

import android.content.Context;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

/** Resolves and applies the active context profile for the current editor/app. */
public final class ContextProfilesController {

  @NonNull private final ContextProfilesStore store;

  private boolean storeEnabled;
  @Nullable private String activePackageName;
  @Nullable private ContextFieldSelector activeFieldSelector;
  @Nullable private String activePresetId;
  @Nullable private ContextProfilesStore.Preset activePreset;
  @Nullable private String sessionOverridePackageName;
  @Nullable private String sessionOverridePresetId;
  @Nullable private ContextProfilesStore.Preset sessionOverridePreset;

  public ContextProfilesController(@NonNull Context context) {
    store = new ContextProfilesStore(context.getApplicationContext());
  }

  public void onStartInputView(@NonNull EditorInfo editorInfo) {
    final String pkgRaw = editorInfo.packageName;
    final String pkg = pkgRaw == null ? "" : pkgRaw.trim();
    final boolean enabled = store.isEnabled();

    final ContextFieldSelector selector =
        pkg.isEmpty() ? null : ContextFieldSelectorResolver.resolve(editorInfo);

    final String presetId;
    if (!enabled || pkg.isEmpty() || selector == null) {
      presetId = null;
      clearSessionOverrideLocked();
    } else {
      final boolean sessionOverrideApplies =
          pkg.equals(sessionOverridePackageName) && sessionOverridePresetId != null;
      if (sessionOverrideApplies) {
        presetId = sessionOverridePresetId;
      } else {
        if (sessionOverridePackageName != null && !pkg.equals(sessionOverridePackageName)) {
          clearSessionOverrideLocked();
        }
        final String selectorBinding = store.getBoundPresetId(pkg, selector);
        final String bound =
            selectorBinding != null
                ? selectorBinding
                : store.getBoundPresetId(pkg, ContextFieldSelector.ALL_FIELDS);
        presetId = bound == null || bound.trim().isEmpty() ? null : bound.trim();
      }
    }

    if (pkg.equals(activePackageName)
        && enabled == storeEnabled
        && selector == activeFieldSelector
        && Objects.equals(presetId, activePresetId)) {
      return;
    }

    storeEnabled = enabled;
    activePackageName = pkg.isEmpty() ? null : pkg;
    activeFieldSelector = selector;
    activePresetId = presetId;
    if (presetId != null
        && presetId.equals(sessionOverridePresetId)
        && sessionOverridePreset != null) {
      activePreset = sessionOverridePreset;
    } else {
      activePreset = presetId == null ? null : store.getPreset(presetId);
    }
  }

  public boolean setSessionOverridePreset(@NonNull String packageName, @NonNull String presetId) {
    if (packageName.trim().isEmpty() || presetId.trim().isEmpty()) return false;
    if (!store.isEnabled()) return false;

    final ContextProfilesStore.Preset preset = store.getPreset(presetId);
    if (preset == null) return false;

    sessionOverridePackageName = packageName.trim();
    sessionOverridePresetId = presetId.trim();
    sessionOverridePreset = preset;
    return true;
  }

  public void clearSessionOverride() {
    clearSessionOverrideLocked();
  }

  private void clearSessionOverrideLocked() {
    sessionOverridePackageName = null;
    sessionOverridePresetId = null;
    sessionOverridePreset = null;
  }

  @Nullable
  public ContextProfilesStore.Preset getActivePreset() {
    return activePreset;
  }

  @NonNull
  public String postProcessVoiceText(
      @NonNull String formattedText, @NonNull EditorInfo editorInfo) {
    return postProcessVoiceTextWithSuggestion(formattedText, editorInfo).textToCommit();
  }

  @NonNull
  public VoiceTextPostProcessResult postProcessVoiceTextWithSuggestion(
      @NonNull String formattedText, @NonNull EditorInfo editorInfo) {
    final ContextProfilesStore.Preset preset = activePreset;
    if (preset == null) return new VoiceTextPostProcessResult(formattedText, null);

    final List<ContextProfilesStore.VoiceRule> rules = preset.voiceRules;
    if (rules.isEmpty()) return new VoiceTextPostProcessResult(formattedText, null);

    final boolean noSuggestions =
        (editorInfo.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
            == EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final boolean allowSuggestions = !noSuggestions;
    final boolean allowAutoApply = allowSuggestions || preset.allowAutoApplyInNoSuggestionsFields;
    return VoiceRulesApplier.apply(formattedText, rules, allowSuggestions, allowAutoApply);
  }
}
