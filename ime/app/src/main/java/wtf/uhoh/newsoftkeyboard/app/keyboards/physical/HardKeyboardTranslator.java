package wtf.uhoh.newsoftkeyboard.app.keyboards.physical;

import wtf.uhoh.newsoftkeyboard.app.ime.ImeBase;

public interface HardKeyboardTranslator {
  /*
   * Gets the current state of the hard keyboard, and may change the output key-code.
   */
  void translatePhysicalCharacter(HardKeyboardAction action, ImeBase ime, int multiTapTimeout);
}
