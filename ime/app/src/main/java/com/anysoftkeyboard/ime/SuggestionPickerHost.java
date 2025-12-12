package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.List;

final class SuggestionPickerHost implements SuggestionPicker.Host {
  private final AnySoftKeyboardSuggestions host;

  SuggestionPickerHost(AnySoftKeyboardSuggestions host) {
    this.host = host;
  }

  @Override
  public InputConnection currentInputConnection() {
    return host.mInputConnectionRouter.current();
  }

  @Override
  public WordComposer prepareWordComposerForNextWord() {
    return host.prepareWordComposerForNextWord();
  }

  @Override
  public void checkAddToDictionaryWithAutoDictionary(
      CharSequence newWord, Suggest.AdditionType type) {
    host.checkAddToDictionaryWithAutoDictionary(newWord, type);
  }

  @Override
  public void setSuggestions(List<CharSequence> suggestions, int highlightedIndex) {
    host.setSuggestions(suggestions, highlightedIndex);
  }

  @Override
  public Suggest getSuggest() {
    return host.mSuggest;
  }

  @Override
  public CandidateView getCandidateView() {
    return host.mCandidateView;
  }

  @Override
  public boolean tryCommitCompletion(int index, InputConnection ic, CandidateView candidateView) {
    return host.completionHandler.tryCommitCompletion(index, ic, candidateView);
  }

  @Override
  public AnyKeyboard getCurrentAlphabetKeyboard() {
    return host.getCurrentAlphabetKeyboard();
  }

  @Override
  public void clearSuggestions() {
    host.clearSuggestions();
  }

  @Override
  public void commitWordToInput(CharSequence wordToCommit, CharSequence typedWord) {
    host.commitWordToInput(wordToCommit, typedWord);
  }

  @Override
  public void sendKeyChar(char c) {
    host.sendKeyChar(c);
  }

  @Override
  public void setSpaceTimeStamp(boolean isSpace) {
    host.setSpaceTimeStamp(isSpace);
  }

  @Override
  public boolean isPredictionOn() {
    return host.isPredictionOn();
  }

  @Override
  public boolean isAutoCompleteEnabled() {
    return host.mAutoComplete;
  }
}
