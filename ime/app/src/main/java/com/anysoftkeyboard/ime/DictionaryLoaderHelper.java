package com.anysoftkeyboard.ime;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.menny.android.anysoftkeyboard.AnyApplication;
import java.util.List;
import java.util.function.Function;

/** Loads keyboard dictionaries while keeping {@link AnySoftKeyboardSuggestions} slimmer. */
final class DictionaryLoaderHelper {

  boolean setDictionariesForCurrentKeyboard(
      @NonNull Context context,
      @NonNull PredictionState predictionState,
      @NonNull DictionaryLoadGate dictionaryLoadGate,
      boolean shouldLoadDictionariesForGestureTyping,
      @Nullable AnyKeyboard currentAlphabetKeyboard,
      boolean inAlphabetKeyboardMode,
      @NonNull SentenceSeparators sentenceSeparators,
      @NonNull Suggest suggest,
      @NonNull Function<AnyKeyboard, DictionaryBackgroundLoader.Listener> listenerProvider) {

    if (!dictionaryLoadGate.shouldLoad(
        predictionState.predictionOn, shouldLoadDictionariesForGestureTyping)) {
      return false;
    }

    if (currentAlphabetKeyboard == null || !inAlphabetKeyboardMode) {
      return false;
    }

    sentenceSeparators.updateFrom(currentAlphabetKeyboard.getSentenceSeparators());
    sentenceSeparators.add(KeyCodes.ENTER);

    List<DictionaryAddOnAndBuilder> buildersForKeyboard =
        AnyApplication.getExternalDictionaryFactory(context)
            .getBuildersForKeyboard(currentAlphabetKeyboard);

    suggest.setupSuggestionsForKeyboard(
        buildersForKeyboard, listenerProvider.apply(currentAlphabetKeyboard));
    return true;
  }
}
