package wtf.uhoh.newsoftkeyboard.app.keyboards;

final class PackKeyboardKey extends KeyboardKey {
  private boolean shiftCodesAlways;
  private int packRowIndex = -1;
  private int packKeyIndex = -1;

  PackKeyboardKey(Keyboard.Row row, KeyboardDimens keyboardDimens) {
    super(row, keyboardDimens);
    enable();
  }

  void setPackLocation(int rowIndex, int keyIndex) {
    packRowIndex = rowIndex;
    packKeyIndex = keyIndex;
  }

  int packRowIndex() {
    return packRowIndex;
  }

  int packKeyIndex() {
    return packKeyIndex;
  }

  void setShiftCodesAlways(boolean shiftCodesAlways) {
    this.shiftCodesAlways = shiftCodesAlways;
  }

  @Override
  public boolean isShiftCodesAlways() {
    return shiftCodesAlways;
  }
}
