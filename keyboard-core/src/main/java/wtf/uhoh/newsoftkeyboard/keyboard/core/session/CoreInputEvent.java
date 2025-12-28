package wtf.uhoh.newsoftkeyboard.keyboard.core.session;

public sealed interface CoreInputEvent permits CoreInputEvent.KeyPress {
  record KeyPress(int rowIndex, int keyIndex) implements CoreInputEvent {}
}
