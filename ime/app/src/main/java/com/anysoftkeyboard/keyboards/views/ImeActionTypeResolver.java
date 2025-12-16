package com.anysoftkeyboard.keyboards.views;

import android.view.inputmethod.EditorInfo;

/** Small helper to resolve IME action type from imeOptions flags. */
final class ImeActionTypeResolver {

  int resolveActionType(int imeOptions) {
    if ((imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
      return EditorInfo.IME_ACTION_NONE;
    }
    return (imeOptions & EditorInfo.IME_MASK_ACTION);
  }
}
