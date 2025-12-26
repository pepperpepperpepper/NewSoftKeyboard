package wtf.uhoh.newsoftkeyboard.dictionaries;

public final class AbbreviationSuggestionCallback implements Dictionary.WordCallback {

  private final Dictionary.WordCallback basicWordCallback;
  private final int abbreviationFrequency;

  public AbbreviationSuggestionCallback(
      Dictionary.WordCallback callback, int abbreviationFrequency) {
    this.basicWordCallback = callback;
    this.abbreviationFrequency = abbreviationFrequency;
  }

  @Override
  public boolean addWord(
      char[] word, int wordOffset, int wordLength, int frequency, Dictionary from) {
    return basicWordCallback.addWord(word, wordOffset, wordLength, abbreviationFrequency, from);
  }
}
