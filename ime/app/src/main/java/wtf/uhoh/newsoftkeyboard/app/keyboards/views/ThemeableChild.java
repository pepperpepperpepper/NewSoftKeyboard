package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;

public interface ThemeableChild {
  void setKeyboardTheme(KeyboardTheme theme);

  void setThemeOverlay(OverlayData overlay);
}
