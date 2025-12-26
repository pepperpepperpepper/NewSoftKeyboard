package wtf.uhoh.newsoftkeyboard.app.dictionaries.prefsprovider;

import android.content.Context;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.FallbackUserDictionary;

class TappedUserFallbackUserDictionary extends FallbackUserDictionary {

  private final WordReadListener mWordsTapper;

  public TappedUserFallbackUserDictionary(
      Context context, String locale, WordReadListener wordsTapper) {
    super(context, locale);
    mWordsTapper = wordsTapper;
  }

  @NonNull
  @Override
  protected WordReadListener createWordReadListener() {
    return mWordsTapper;
  }
}
