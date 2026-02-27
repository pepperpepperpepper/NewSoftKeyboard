package wtf.uhoh.newsoftkeyboard.app.ime.context;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;

final class ContextFieldSelectorResolver {

  @Nullable
  static ContextFieldSelector resolve(@NonNull EditorInfo editorInfo) {
    if ((editorInfo.inputType & EditorInfo.TYPE_MASK_CLASS) != EditorInfo.TYPE_CLASS_TEXT) {
      return null;
    }

    final int variation = editorInfo.inputType & EditorInfo.TYPE_MASK_VARIATION;
    final int action = editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;

    if (action == EditorInfo.IME_ACTION_SEARCH
        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
      return ContextFieldSelector.SEARCH;
    }

    return switch (variation) {
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
          EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
          EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT ->
          ContextFieldSelector.EMAIL;
      case EditorInfo.TYPE_TEXT_VARIATION_URI -> ContextFieldSelector.URL;
      case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
          EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE ->
          ContextFieldSelector.IM;
      default -> ContextFieldSelector.TEXT;
    };
  }

  private ContextFieldSelectorResolver() {}
}
