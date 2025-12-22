package com.anysoftkeyboard.keyboards.physical;

import com.anysoftkeyboard.ime.AnySoftKeyboardBase;

public interface HardKeyboardTranslator {
  /*
   * Gets the current state of the hard keyboard, and may change the output key-code.
   */
  void translatePhysicalCharacter(
      HardKeyboardAction action, AnySoftKeyboardBase ime, int multiTapTimeout);
}
