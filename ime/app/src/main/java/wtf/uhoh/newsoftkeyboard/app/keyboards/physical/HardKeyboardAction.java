package wtf.uhoh.newsoftkeyboard.app.keyboards.physical;

public interface HardKeyboardAction {
  int getKeyCode();

  boolean isAltActive();

  boolean isShiftActive();

  void setNewKeyCode(int keyCode);
}
