package com.anysoftkeyboard.keyboards.views;

import android.content.res.Resources;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.menny.android.anysoftkeyboard.R;

/** Supplies default next-keyboard labels when none are provided. */
final class NextKeyboardNameResolver {

  CharSequence resolveNextAlphabetName(@NonNull Resources res, CharSequence candidate) {
    if (!TextUtils.isEmpty(candidate)) return candidate;
    return res.getString(R.string.change_lang_regular);
  }

  CharSequence resolveNextSymbolsName(@NonNull Resources res, CharSequence candidate) {
    if (!TextUtils.isEmpty(candidate)) return candidate;
    return res.getString(R.string.change_symbols_regular);
  }
}
