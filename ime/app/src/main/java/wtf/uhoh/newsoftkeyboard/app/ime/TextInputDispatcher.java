package wtf.uhoh.newsoftkeyboard.app.ime;

import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Handles text injection paths (onText/onTyping) to shrink the IME host. */
final class TextInputDispatcher {

  interface Host {
    InputConnectionRouter inputConnectionRouter();

    WordComposer currentWord();

    void setPreviousWord(WordComposer word);

    AutoCorrectState autoCorrectState();

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    void markExpectingSelectionUpdate();

    void onKey(
        int primaryCode,
        Keyboard.Key keyboardKey,
        int multiTapIndex,
        int[] nearByKeyCodes,
        boolean fromUI);

    void clearSpaceTimeTracker();

    boolean isAutoCorrectOn();

    void setAutoCorrectOn(boolean on);
  }

  private final TypingSimulator typingSimulator;

  TextInputDispatcher(TypingSimulator typingSimulator) {
    this.typingSimulator = typingSimulator;
  }

  void onText(CharSequence text, Host host, String logTag) {
    Logger.d(logTag, "onText: '%s'", text);
    final InputConnectionRouter router = host.inputConnectionRouter();
    if (router.current() == null) {
      return;
    }
    router.beginBatchEdit();

    final WordComposer initialWordComposer = new WordComposer();
    host.currentWord().cloneInto(initialWordComposer);
    host.abortCorrectionAndResetPredictionState(false);
    router.commitText(text, 1);

    final AutoCorrectState state = host.autoCorrectState();
    state.wordRevertLength = initialWordComposer.charCount() + text.length();
    host.setPreviousWord(initialWordComposer);
    host.markExpectingSelectionUpdate();
    router.endBatchEdit();
  }

  void onTyping(Keyboard.Key key, CharSequence text, Host host, String logTag) {
    if (BuildConfig.DEBUG) {
      Logger.d(logTag, "onTyping: '%s'", text);
    }
    typingSimulator.simulate(
        text,
        new TypingSimulator.Host() {
          @Override
          public InputConnectionRouter inputConnectionRouter() {
            return host.inputConnectionRouter();
          }

          @Override
          public Keyboard.Key lastKey() {
            return key;
          }

          @Override
          public void onKey(
              int primaryCode,
              Keyboard.Key keyboardKey,
              int multiTapIndex,
              int[] nearByKeyCodes,
              boolean fromUI) {
            host.onKey(primaryCode, keyboardKey, multiTapIndex, nearByKeyCodes, fromUI);
          }

          @Override
          public void clearSpaceTimeTracker() {
            host.clearSpaceTimeTracker();
          }

          @Override
          public boolean isAutoCorrectOn() {
            return host.isAutoCorrectOn();
          }

          @Override
          public void setAutoCorrectOn(boolean on) {
            host.setAutoCorrectOn(on);
          }
        });
  }
}
