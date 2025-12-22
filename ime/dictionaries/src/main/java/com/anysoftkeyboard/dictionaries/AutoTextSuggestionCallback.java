package com.anysoftkeyboard.dictionaries;

final class AutoTextSuggestionCallback implements Dictionary.WordCallback {

  private final Dictionary.WordCallback basicWordCallback;
  private final int autoTextFrequency;

  AutoTextSuggestionCallback(Dictionary.WordCallback callback, int autoTextFrequency) {
    this.basicWordCallback = callback;
    this.autoTextFrequency = autoTextFrequency;
  }

  @Override
  public boolean addWord(
      char[] word, int wordOffset, int wordLength, int frequency, Dictionary from) {
    return basicWordCallback.addWord(word, wordOffset, wordLength, autoTextFrequency, from);
  }
}
