package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;

final class CustomKeyboardEditorValidationWarnings {

  private static final String ATTR_KEY_WIDTH = "android:keyWidth";
  private static final String ATTR_POPUP_KEYBOARD = "android:popupKeyboard";

  private CustomKeyboardEditorValidationWarnings() {}

  @NonNull
  static List<String> collect(
      @NonNull Context context,
      @NonNull InstalledKeyboardPack currentPack,
      @NonNull KeyboardModel model,
      @Nullable KeyboardDefinition displayedKeyboard) {
    List<String> warnings = new ArrayList<>();

    boolean hasDelete = false;
    boolean hasSpace = false;
    boolean hasEnter = false;
    boolean hasSymbolsSwitch = false;

    if (displayedKeyboard != null) {
      for (Keyboard.Key key : displayedKeyboard.getKeys()) {
        int count = key.getCodesCount();
        for (int i = 0; i < count; i++) {
          int code = key.getCodeAtIndex(i, false);
          if (code == KeyCodes.DELETE) hasDelete = true;
          if (code == KeyCodes.SPACE) hasSpace = true;
          if (code == KeyCodes.ENTER) hasEnter = true;
          if (code == KeyCodes.MODE_SYMBOLS
              || code == KeyCodes.KEYBOARD_MODE_CHANGE
              || code == KeyCodes.CUSTOM_KEYBOARD_SWITCH) {
            hasSymbolsSwitch = true;
          }
        }
      }
    } else {
      for (KeyboardRow row : model.rows()) {
        for (KeySpec keySpec : row.keys()) {
          for (KeyCode code : keySpec.codes()) {
            Integer numeric = code.asNumeric();
            if (numeric == null) continue;
            if (numeric == KeyCodes.DELETE) hasDelete = true;
            if (numeric == KeyCodes.SPACE) hasSpace = true;
            if (numeric == KeyCodes.ENTER) hasEnter = true;
            if (numeric == KeyCodes.MODE_SYMBOLS
                || numeric == KeyCodes.KEYBOARD_MODE_CHANGE
                || numeric == KeyCodes.CUSTOM_KEYBOARD_SWITCH) {
              hasSymbolsSwitch = true;
            }
          }
        }
      }
    }

    if (!hasDelete)
      warnings.add(context.getString(R.string.custom_keyboards_validation_missing_delete));
    if (!hasSpace)
      warnings.add(context.getString(R.string.custom_keyboards_validation_missing_space));
    if (!hasEnter)
      warnings.add(context.getString(R.string.custom_keyboards_validation_missing_enter));
    if (!hasSymbolsSwitch) {
      warnings.add(context.getString(R.string.custom_keyboards_validation_missing_symbols));
    }

    warnings.addAll(validatePopupKeyboards(currentPack, model));
    warnings.addAll(validateRowWidthPercents(model));
    warnings.addAll(validateTextAndCodepoints(model));

    return warnings;
  }

  @NonNull
  private static List<String> validatePopupKeyboards(
      @NonNull InstalledKeyboardPack currentPack, @NonNull KeyboardModel model) {
    Set<String> popupPaths = new HashSet<>();
    for (KeyboardRow row : model.rows()) {
      for (KeySpec keySpec : row.keys()) {
        String popupKeyboard = keySpec.rawAttributes().get(ATTR_POPUP_KEYBOARD);
        if (popupKeyboard == null) continue;
        String trimmed = popupKeyboard.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("@")) continue;
        popupPaths.add(trimmed);
      }
    }

    if (popupPaths.isEmpty()) return Collections.emptyList();

    List<String> warnings = new ArrayList<>();
    for (String popupPath : popupPaths) {
      try {
        PackPath.parse(popupPath);
      } catch (IllegalArgumentException e) {
        warnings.add("Popup keyboard path is invalid: " + popupPath);
        continue;
      }

      File file = new File(currentPack.directory(), popupPath);
      if (!file.exists()) {
        warnings.add("Popup keyboard file not found: " + popupPath);
        continue;
      }
      try (InputStream in = new FileInputStream(file)) {
        AskXmlKeyboardParser.parse(in);
      } catch (IOException e) {
        warnings.add("Popup keyboard failed to parse: " + popupPath);
      }
    }

    return warnings;
  }

  @NonNull
  private static List<String> validateRowWidthPercents(@NonNull KeyboardModel model) {
    Float keyboardDefaultWidthPercent =
        parsePercentOrNull(model.rawKeyboardAttributes().get(ATTR_KEY_WIDTH));
    if (keyboardDefaultWidthPercent == null) return Collections.emptyList();

    List<String> warnings = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++) {
      KeyboardRow row = model.rows().get(rowIndex);
      Float rowDefaultWidthPercent = parsePercentOrNull(row.rawRowAttributes().get(ATTR_KEY_WIDTH));
      if (rowDefaultWidthPercent == null) rowDefaultWidthPercent = keyboardDefaultWidthPercent;

      float sum = 0f;
      boolean valid = true;
      for (KeySpec keySpec : row.keys()) {
        Float keyWidthPercent = parsePercentOrNull(keySpec.rawAttributes().get(ATTR_KEY_WIDTH));
        if (keyWidthPercent == null) keyWidthPercent = rowDefaultWidthPercent;
        if (keyWidthPercent == null) {
          valid = false;
          break;
        }
        sum += keyWidthPercent;
      }

      if (!valid) continue;
      if (sum > 100.5f) {
        warnings.add("Row " + (rowIndex + 1) + " total keyWidth is " + sum + "% (> 100%).");
      }
    }
    return warnings;
  }

  @Nullable
  private static Float parsePercentOrNull(@Nullable String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    if (!trimmed.endsWith("%p")) return null;
    String number = trimmed.substring(0, trimmed.length() - 2).trim();
    try {
      return Float.parseFloat(number);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @NonNull
  private static List<String> validateTextAndCodepoints(@NonNull KeyboardModel model) {
    int unpairedSurrogates = 0;
    int invalidCodePoints = 0;
    List<String> samples = new ArrayList<>(4);

    for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++) {
      KeyboardRow row = model.rows().get(rowIndex);
      for (int keyIndex = 0; keyIndex < row.keys().size(); keyIndex++) {
        KeySpec keySpec = row.keys().get(keyIndex);

        String label = keySpec.label();
        if (label != null && containsUnpairedSurrogate(label)) {
          unpairedSurrogates++;
          if (samples.size() < 4) {
            samples.add(
                "Row " + (rowIndex + 1) + " key " + (keyIndex + 1) + " has invalid UTF-16.");
          }
        }

        String popupCharacters = keySpec.popupCharacters();
        if (popupCharacters != null && containsUnpairedSurrogate(popupCharacters)) {
          unpairedSurrogates++;
          if (samples.size() < 4) {
            samples.add(
                "Row "
                    + (rowIndex + 1)
                    + " key "
                    + (keyIndex + 1)
                    + " popupCharacters has invalid UTF-16.");
          }
        }

        for (KeyCode code : keySpec.codes()) {
          Integer numeric = code.asNumeric();
          if (numeric == null || numeric <= 0) continue;
          if (!Character.isValidCodePoint(numeric) || isSurrogateCodePoint(numeric)) {
            invalidCodePoints++;
            if (samples.size() < 4) {
              samples.add(
                  "Row "
                      + (rowIndex + 1)
                      + " key "
                      + (keyIndex + 1)
                      + " has invalid code point: "
                      + numeric);
            }
          }
        }
      }
    }

    if (unpairedSurrogates == 0 && invalidCodePoints == 0) return Collections.emptyList();

    List<String> warnings = new ArrayList<>();
    if (unpairedSurrogates > 0) {
      warnings.add("Found invalid UTF-16 text in " + unpairedSurrogates + " field(s).");
    }
    if (invalidCodePoints > 0) {
      warnings.add("Found invalid Unicode code point(s): " + invalidCodePoints + ".");
    }
    warnings.addAll(samples);
    return warnings;
  }

  private static boolean isSurrogateCodePoint(int codePoint) {
    return codePoint >= 0xD800 && codePoint <= 0xDFFF;
  }

  private static boolean containsUnpairedSurrogate(@NonNull String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isHighSurrogate(c)) {
        if (i + 1 >= value.length()) return true;
        char next = value.charAt(i + 1);
        if (!Character.isLowSurrogate(next)) return true;
        i++;
      } else if (Character.isLowSurrogate(c)) {
        return true;
      }
    }
    return false;
  }
}
