package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.theme.KeyboardTheme;
import java.util.HashSet;

/** Applies theme attributes with overlay-aware host wiring. */
final class ThemeAttributeLoaderRunner {

  void applyThemeAttributes(
      AnyKeyboardViewBase host, ThemeOverlayCombiner overlayCombiner, KeyboardTheme theme) {
    HashSet<Integer> doneAttrs = new HashSet<>();
    int[] padding = new int[] {0, 0, 0, 0};
    ThemeAttributeLoader themeAttributeLoader =
        new ThemeAttributeLoader(new ThemeHost(host, overlayCombiner, doneAttrs, padding));
    themeAttributeLoader.loadThemeAttributes(theme, doneAttrs, padding);
  }
}
