package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.NonNull;

/** View-owned contract for actions originating from {@link CandidateView}. */
public interface CandidateViewHost {

  void pickSuggestionManually(int index, @NonNull CharSequence suggestion);

  void addWordToDictionary(@NonNull String word);

  void removeFromUserDictionary(@NonNull String wordToRemove);
}
