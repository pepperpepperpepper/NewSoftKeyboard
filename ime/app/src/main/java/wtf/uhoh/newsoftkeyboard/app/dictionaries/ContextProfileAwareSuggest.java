package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

/** Optional Suggest extension which can apply context-profile session overrides. */
public interface ContextProfileAwareSuggest {
  void setContextProfileSafeToggles(@Nullable ContextProfilesStore.SafeToggles safeToggles);

  /** Sets the active preset word list for suggestions (null disables). */
  void setContextProfileWordList(@Nullable String presetId, long generation);
}
