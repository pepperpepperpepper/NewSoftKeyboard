package com.anysoftkeyboard.keyboards;

import androidx.annotation.NonNull;
import java.util.List;

public interface KeyboardSwitchedListener {
  void onAlphabetKeyboardSet(@NonNull AnyKeyboard keyboard);

  void onSymbolsKeyboardSet(@NonNull AnyKeyboard keyboard);

  void onAvailableKeyboardsChanged(@NonNull List<KeyboardAddOnAndBuilder> builders);
}
