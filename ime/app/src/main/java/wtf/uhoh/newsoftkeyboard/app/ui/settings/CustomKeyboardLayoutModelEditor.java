package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;

final class CustomKeyboardLayoutModelEditor {
  private CustomKeyboardLayoutModelEditor() {}

  @NonNull
  static KeyboardModel updateKeySpec(
      @NonNull KeyboardModel model,
      int rowIndex,
      int keyIndex,
      @Nullable CharSequence label,
      @Nullable CharSequence codes,
      @Nullable CharSequence longPressCode,
      @Nullable CharSequence width,
      @Nullable CharSequence hintLabel,
      @Nullable CharSequence popupCharacters,
      @Nullable CharSequence popupKeyboard,
      @Nullable CharSequence extraKeyData) {
    KeyboardRow targetRow = model.rows().get(rowIndex);
    KeySpec targetKey = targetRow.keys().get(keyIndex);

    Map<String, String> nextAttrs = new HashMap<>(targetKey.rawAttributes());
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_KEY_LABEL, label);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_CODES, codes);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_LONG_PRESS_CODE, longPressCode);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_KEY_WIDTH, width);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_HINT_LABEL, hintLabel);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_POPUP_CHARACTERS, popupCharacters);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_POPUP_KEYBOARD, popupKeyboard);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_EXTRA_KEY_DATA, extraKeyData);

    List<KeyCode> parsedCodes = parseCodes(nextAttrs.get(CustomKeyboardLayoutEditor.ATTR_CODES));
    String nextLabel = normalizeOptionalString(label);
    String nextPopupCharacters = normalizeOptionalString(popupCharacters);
    KeySpec updatedKey = new KeySpec(parsedCodes, nextLabel, nextPopupCharacters, nextAttrs);

    List<KeySpec> updatedKeys = new ArrayList<>(targetRow.keys());
    updatedKeys.set(keyIndex, updatedKey);
    KeyboardRow updatedRow = new KeyboardRow(targetRow.rawRowAttributes(), updatedKeys);

    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    Map<String, String> updatedKeyboardAttrs = model.rawKeyboardAttributes();
    final boolean requiresAskNamespace =
        nextAttrs.containsKey(CustomKeyboardLayoutEditor.ATTR_LONG_PRESS_CODE)
            || nextAttrs.containsKey(CustomKeyboardLayoutEditor.ATTR_HINT_LABEL)
            || nextAttrs.containsKey(CustomKeyboardLayoutEditor.ATTR_EXTRA_KEY_DATA);
    if (requiresAskNamespace && !updatedKeyboardAttrs.containsKey("xmlns:ask")) {
      updatedKeyboardAttrs = new HashMap<>(updatedKeyboardAttrs);
      updatedKeyboardAttrs.put("xmlns:ask", "http://schemas.android.com/apk/res-auto");
    }
    return new KeyboardModel(updatedKeyboardAttrs, updatedRows);
  }

  @NonNull
  static KeyboardModel updateRow(
      @NonNull KeyboardModel model,
      int rowIndex,
      @Nullable CharSequence keyWidth,
      @Nullable CharSequence keyHeight,
      @Nullable CharSequence horizontalGap,
      @Nullable CharSequence verticalGap,
      @Nullable CharSequence rowEdgeFlags,
      @Nullable CharSequence keyboardMode) {
    KeyboardRow row = model.rows().get(rowIndex);

    Map<String, String> nextAttrs = new HashMap<>(row.rawRowAttributes());
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_KEY_WIDTH, keyWidth);
    updateAttr(nextAttrs, "android:keyHeight", keyHeight);
    updateAttr(nextAttrs, "android:horizontalGap", horizontalGap);
    updateAttr(nextAttrs, "android:verticalGap", verticalGap);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS, rowEdgeFlags);
    updateAttr(nextAttrs, CustomKeyboardLayoutEditor.ATTR_KEYBOARD_MODE, keyboardMode);

    KeyboardRow updatedRow = new KeyboardRow(nextAttrs, row.keys());
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  static KeyboardModel moveKey(
      @NonNull KeyboardModel model, int rowIndex, int keyIndex, int offset) {
    KeyboardRow row = model.rows().get(rowIndex);
    int newIndex = keyIndex + offset;
    if (newIndex < 0 || newIndex >= row.keys().size()) return model;

    List<KeySpec> keys = new ArrayList<>(row.keys());
    Collections.swap(keys, keyIndex, newIndex);
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), keys);
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  static KeyboardModel insertKeyAfter(@NonNull KeyboardModel model, int rowIndex, int keyIndex) {
    KeyboardRow row = model.rows().get(rowIndex);
    List<KeySpec> keys = new ArrayList<>(row.keys());
    int insertIndex = Math.min(keys.size(), keyIndex + 1);
    keys.add(insertIndex, createPlaceholderKeySpec());
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), keys);
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  static KeyboardModel deleteKey(@NonNull KeyboardModel model, int rowIndex, int keyIndex) {
    KeyboardRow row = model.rows().get(rowIndex);
    if (row.keys().size() <= 1) {
      return deleteRow(model, rowIndex);
    }
    List<KeySpec> keys = new ArrayList<>(row.keys());
    if (keyIndex < 0 || keyIndex >= keys.size()) return model;
    keys.remove(keyIndex);
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), keys);
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  static KeyboardModel moveRow(@NonNull KeyboardModel model, int rowIndex, int offset) {
    int newIndex = rowIndex + offset;
    if (newIndex < 0 || newIndex >= model.rows().size()) return model;
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    Collections.swap(rows, rowIndex, newIndex);
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  static KeyboardModel insertRowBelow(@NonNull KeyboardModel model, int rowIndex) {
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    int insertIndex = Math.min(rows.size(), rowIndex + 1);
    rows.add(insertIndex, new KeyboardRow(Collections.singletonList(createPlaceholderKeySpec())));
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  static KeyboardModel deleteRow(@NonNull KeyboardModel model, int rowIndex) {
    if (model.rows().size() <= 1) return model;
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    if (rowIndex < 0 || rowIndex >= rows.size()) return model;
    rows.remove(rowIndex);
    if (rows.isEmpty()) return model;
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  static KeySpec createPlaceholderKeySpec() {
    Map<String, String> attrs = new HashMap<>();
    attrs.put(CustomKeyboardLayoutEditor.ATTR_KEY_LABEL, "?");
    attrs.put(CustomKeyboardLayoutEditor.ATTR_CODES, "63");
    List<KeyCode> codes = Collections.singletonList(new KeyCode.Numeric(63));
    return new KeySpec(codes, "?", null, attrs);
  }

  @NonNull
  static KeySpec createKeySpec(
      @NonNull String label,
      @NonNull String codes,
      @Nullable String edgeFlags,
      @Nullable String extraData) {
    return createKeySpec(label, codes, edgeFlags, extraData, null);
  }

  @NonNull
  static KeySpec createKeySpec(
      @NonNull String label,
      @NonNull String codes,
      @Nullable String edgeFlags,
      @Nullable String extraData,
      @Nullable String keyWidth) {
    return createKeySpec(label, codes, edgeFlags, extraData, keyWidth, Collections.emptyMap());
  }

  @NonNull
  static KeySpec createKeySpec(
      @NonNull String label,
      @NonNull String codes,
      @Nullable String edgeFlags,
      @Nullable String extraData,
      @Nullable String keyWidth,
      @NonNull Map<String, String> additionalAttrs) {
    Map<String, String> attrs = new HashMap<>();
    attrs.put(CustomKeyboardLayoutEditor.ATTR_KEY_LABEL, label);
    attrs.put(CustomKeyboardLayoutEditor.ATTR_CODES, codes);
    if (!TextUtils.isEmpty(edgeFlags)) {
      attrs.put("android:keyEdgeFlags", edgeFlags);
    }
    if (!TextUtils.isEmpty(keyWidth)) {
      attrs.put(CustomKeyboardLayoutEditor.ATTR_KEY_WIDTH, keyWidth);
    }
    if (!TextUtils.isEmpty(extraData)) {
      attrs.put(CustomKeyboardLayoutEditor.ATTR_EXTRA_KEY_DATA, extraData);
    }
    attrs.putAll(additionalAttrs);
    List<KeyCode> parsedCodes = parseCodes(codes);
    return new KeySpec(parsedCodes, label, null, attrs);
  }

  private static void updateAttr(
      @NonNull Map<String, String> attrs, @NonNull String name, @Nullable CharSequence value) {
    String normalized = normalizeOptionalString(value);
    if (TextUtils.isEmpty(normalized)) {
      attrs.remove(name);
    } else {
      attrs.put(name, normalized);
    }
  }

  @Nullable
  private static String normalizeOptionalString(@Nullable CharSequence value) {
    if (value == null) return null;
    String asString = value.toString();
    return TextUtils.isEmpty(asString.trim()) ? null : asString;
  }

  @NonNull
  private static List<KeyCode> parseCodes(@Nullable String raw) {
    if (raw == null) return Collections.emptyList();
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return Collections.emptyList();
    if (trimmed.startsWith("@")) return Collections.singletonList(new KeyCode.Symbolic(trimmed));

    String[] parts = trimmed.split(",");
    List<KeyCode> result = new ArrayList<>(parts.length);
    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) continue;
      if (token.startsWith("@")) {
        result.add(new KeyCode.Symbolic(token));
        continue;
      }
      if (token.length() != 1) {
        try {
          result.add(new KeyCode.Numeric(Integer.parseInt(token)));
        } catch (NumberFormatException e) {
          result.add(new KeyCode.Symbolic(token));
        }
      } else {
        result.add(new KeyCode.Numeric((int) token.charAt(0)));
      }
    }
    return result;
  }
}
