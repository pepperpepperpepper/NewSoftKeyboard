package wtf.uhoh.newsoftkeyboard.app.ui.settings.wordseditor;

import android.os.Bundle;
import androidx.annotation.Nullable;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.ContextProfileWordListDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.EditableDictionary;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

public class ContextProfileWordListEditorFragment extends UserDictionaryEditorFragment {

  public static final String ARG_PRESET_ID = "preset_id";
  public static final String ARG_PRESET_NAME = "preset_name";

  @Nullable private String mPresetId;
  @Nullable private String mPresetName;
  @Nullable private ContextProfilesStore mStore;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle args = getArguments();
    mPresetId = args == null ? null : args.getString(ARG_PRESET_ID);
    mPresetName = args == null ? null : args.getString(ARG_PRESET_NAME);
    mStore = new ContextProfilesStore(requireContext().getApplicationContext());
  }

  @Override
  public void onStart() {
    super.onStart();
    final String titleName = mPresetName == null ? "" : mPresetName;
    UiUtils.setActivityTitle(this, getString(R.string.context_profiles_word_list_title, titleName));
  }

  @Override
  protected EditableDictionary createEditableDictionary(String locale) {
    final String presetId = mPresetId;
    if (presetId == null || presetId.trim().isEmpty()) {
      throw new IllegalStateException("Missing preset id for word list editor.");
    }

    return new ContextProfileWordListDictionary(
        requireContext().getApplicationContext(), presetId, locale);
  }

  @Override
  public void onWordDeleted(final LoadedWord word) {
    super.onWordDeleted(word);
    bumpGeneration();
  }

  @Override
  public void onWordUpdated(final String oldWord, final LoadedWord newWord) {
    super.onWordUpdated(oldWord, newWord);
    bumpGeneration();
  }

  private void bumpGeneration() {
    final ContextProfilesStore store = mStore;
    final String presetId = mPresetId;
    if (store == null || presetId == null || presetId.trim().isEmpty()) return;
    store.bumpWordListGeneration(presetId);
  }
}
