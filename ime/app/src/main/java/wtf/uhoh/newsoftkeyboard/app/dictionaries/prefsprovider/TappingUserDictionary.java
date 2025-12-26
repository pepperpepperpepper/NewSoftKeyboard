package wtf.uhoh.newsoftkeyboard.app.dictionaries.prefsprovider;

import android.content.Context;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.UserDictionary;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.content.AndroidUserDictionary;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.FallbackUserDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.BTreeDictionary;

class TappingUserDictionary extends UserDictionary {

  private final BTreeDictionary.WordReadListener mWordsTapper;

  public TappingUserDictionary(
      Context context, String locale, BTreeDictionary.WordReadListener wordsTapper) {
    super(context, locale);
    mWordsTapper = wordsTapper;
  }

  @NonNull
  @Override
  protected AndroidUserDictionary createAndroidUserDictionary(Context context, String locale) {
    return new TappedAndroidUserDictionary(context, locale, mWordsTapper);
  }

  @NonNull
  @Override
  protected FallbackUserDictionary createFallbackUserDictionary(Context context, String locale) {
    return new TappedUserFallbackUserDictionary(context, locale, mWordsTapper);
  }
}
