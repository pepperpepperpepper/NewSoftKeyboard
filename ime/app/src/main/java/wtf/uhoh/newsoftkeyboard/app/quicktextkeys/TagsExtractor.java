package wtf.uhoh.newsoftkeyboard.app.quicktextkeys;

import androidx.annotation.NonNull;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.dictionaries.KeyCodesProvider;

public interface TagsExtractor {
  /** Is this extractor actually do anything. */
  boolean isEnabled();

  /** Returns a list of all quick-text outputs related to the given tag. */
  List<CharSequence> getOutputForTag(
      @NonNull CharSequence typedTagToSearch, KeyCodesProvider wordComposer);

  /** releases all resources of this instance. */
  void close();
}
