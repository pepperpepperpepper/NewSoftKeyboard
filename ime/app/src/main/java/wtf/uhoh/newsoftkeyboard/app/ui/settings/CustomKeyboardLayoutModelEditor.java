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
    // android:horizontalGap is the space before a slot (e.g. a row indent on the first key);
    // it describes the position, not the key, so each slot keeps its original gap.
    String gapAtKeyIndex = attrOf(row.keys().get(keyIndex), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP);
    String gapAtNewIndex = attrOf(row.keys().get(newIndex), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP);
    keys.set(
        keyIndex,
        withKeyAttr(keys.get(keyIndex), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP, gapAtKeyIndex));
    keys.set(
        newIndex,
        withKeyAttr(keys.get(newIndex), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP, gapAtNewIndex));
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), normalizeKeyEdgeFlags(row.keys(), keys));
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
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), normalizeKeyEdgeFlags(row.keys(), keys));
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
    if (keyIndex == 0) {
      // The first slot's gap is the row indent; deleting the first key must not shift the row.
      String indent = attrOf(row.keys().get(0), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP);
      if (!TextUtils.isEmpty(indent)) {
        keys.set(0, withKeyAttr(keys.get(0), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP, indent));
      }
    }
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), normalizeKeyEdgeFlags(row.keys(), keys));
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  /**
   * Moves a key to an arbitrary location, possibly across rows. {@code toInsertIndex} is the
   * insertion position in the target row's coordinates as they are before the key is removed
   * from its source row. Slot gaps and edge flags are normalized on every touched row.
   */
  @NonNull
  static KeyboardModel moveKeyToLocation(
      @NonNull KeyboardModel model, int fromRow, int fromKey, int toRow, int toInsertIndex) {
    if (fromRow < 0 || fromRow >= model.rows().size()) return model;
    if (toRow < 0 || toRow >= model.rows().size()) return model;
    KeyboardRow sourceRow = model.rows().get(fromRow);
    if (fromKey < 0 || fromKey >= sourceRow.keys().size()) return model;
    KeyboardRow targetRow = model.rows().get(toRow);
    toInsertIndex = Math.max(0, Math.min(toInsertIndex, targetRow.keys().size()));

    if (fromRow == toRow) {
      // Inserting at the key's own slot (or right after it) changes nothing.
      if (toInsertIndex == fromKey || toInsertIndex == fromKey + 1) return model;
      List<KeySpec> keys = new ArrayList<>(sourceRow.keys());
      KeySpec moved = keys.remove(fromKey);
      int adjustedIndex = toInsertIndex > fromKey ? toInsertIndex - 1 : toInsertIndex;
      keys.add(adjustedIndex, moved);
      // Reorder keys, but keep each slot's horizontalGap where it was (row indent stays put).
      for (int i = 0; i < keys.size(); i++) {
        keys.set(
            i,
            withKeyAttr(
                keys.get(i),
                CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP,
                attrOf(sourceRow.keys().get(i), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP)));
      }
      List<KeyboardRow> rows = new ArrayList<>(model.rows());
      rows.set(
          fromRow,
          new KeyboardRow(
              sourceRow.rawRowAttributes(), normalizeKeyEdgeFlags(sourceRow.keys(), keys)));
      return new KeyboardModel(model.rawKeyboardAttributes(), rows);
    }

    // The last key of a row cannot be dragged out: it would leave an empty row behind.
    if (sourceRow.keys().size() <= 1) return model;

    List<KeySpec> sourceKeys = new ArrayList<>(sourceRow.keys());
    KeySpec moved = sourceKeys.remove(fromKey);
    if (fromKey == 0) {
      String indent = attrOf(sourceRow.keys().get(0), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP);
      if (!TextUtils.isEmpty(indent)) {
        sourceKeys.set(
            0, withKeyAttr(sourceKeys.get(0), CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP, indent));
      }
    }

    // The moved key's gap described its old slot; it does not apply in the new row.
    moved = withKeyAttr(moved, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP, null);
    List<KeySpec> targetKeys = new ArrayList<>(targetRow.keys());
    targetKeys.add(toInsertIndex, moved);

    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    rows.set(
        fromRow,
        new KeyboardRow(
            sourceRow.rawRowAttributes(), normalizeKeyEdgeFlags(sourceRow.keys(), sourceKeys)));
    rows.set(
        toRow,
        new KeyboardRow(
            targetRow.rawRowAttributes(), normalizeKeyEdgeFlags(targetRow.keys(), targetKeys)));
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  static KeyboardModel moveRow(@NonNull KeyboardModel model, int rowIndex, int offset) {
    int newIndex = rowIndex + offset;
    if (newIndex < 0 || newIndex >= model.rows().size()) return model;
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    Collections.swap(rows, rowIndex, newIndex);
    // rowEdgeFlags anchor a row to the keyboard's top/bottom (and "bottom" suppresses the
    // generic bottom row), so the flags stay with the position, not the moved row.
    String flagsAtRowIndex = model.rows().get(rowIndex).rawRowAttributes().get(CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS);
    String flagsAtNewIndex = model.rows().get(newIndex).rawRowAttributes().get(CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS);
    rows.set(
        rowIndex,
        withRowAttr(rows.get(rowIndex), CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS, flagsAtRowIndex));
    rows.set(
        newIndex,
        withRowAttr(rows.get(newIndex), CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS, flagsAtNewIndex));
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
  static KeyboardModel insertRowAbove(@NonNull KeyboardModel model, int rowIndex) {
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    int insertIndex = Math.max(0, Math.min(rows.size(), rowIndex));
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

  /**
   * Clamps a (rowIndex, keyIndex) location into the model's current bounds, so a selection can
   * survive structural edits (after a delete it lands on the nearest neighbor). Returns null when
   * the model has no keys at all.
   */
  @Nullable
  static int[] clampLocation(@NonNull KeyboardModel model, int rowIndex, int keyIndex) {
    if (model.rows().isEmpty()) return null;
    int clampedRow = Math.max(0, Math.min(rowIndex, model.rows().size() - 1));
    List<KeySpec> keys = model.rows().get(clampedRow).keys();
    if (keys.isEmpty()) return null;
    int clampedKey = Math.max(0, Math.min(keyIndex, keys.size() - 1));
    return new int[] {clampedRow, clampedKey};
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

  @Nullable
  private static String attrOf(@NonNull KeySpec key, @NonNull String name) {
    return key.rawAttributes().get(name);
  }

  @NonNull
  private static KeySpec withKeyAttr(
      @NonNull KeySpec key, @NonNull String name, @Nullable String value) {
    String current = key.rawAttributes().get(name);
    if (TextUtils.equals(current, value)) return key;
    Map<String, String> attrs = new HashMap<>(key.rawAttributes());
    if (TextUtils.isEmpty(value)) {
      attrs.remove(name);
    } else {
      attrs.put(name, value);
    }
    return new KeySpec(key.codes(), key.label(), key.popupCharacters(), attrs);
  }

  @NonNull
  private static KeyboardRow withRowAttr(
      @NonNull KeyboardRow row, @NonNull String name, @Nullable String value) {
    String current = row.rawRowAttributes().get(name);
    if (TextUtils.equals(current, value)) return row;
    Map<String, String> attrs = new HashMap<>(row.rawRowAttributes());
    if (TextUtils.isEmpty(value)) {
      attrs.remove(name);
    } else {
      attrs.put(name, value);
    }
    return new KeyboardRow(attrs, row.keys());
  }

  /**
   * keyEdgeFlags mark the row's outermost keys. After any structural change the previous
   * first/last key may sit mid-row (or be gone entirely), so whether the row uses left/right
   * markers is detected on the pre-mutation keys and re-anchored to the new edge keys.
   */
  @NonNull
  private static List<KeySpec> normalizeKeyEdgeFlags(
      @NonNull List<KeySpec> originalKeys, @NonNull List<KeySpec> keys) {
    boolean hadLeft = false;
    boolean hadRight = false;
    for (KeySpec key : originalKeys) {
      String flags = key.rawAttributes().get(CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS);
      if (flags != null) {
        hadLeft |= flags.contains("left");
        hadRight |= flags.contains("right");
      }
    }
    List<KeySpec> normalized = new ArrayList<>(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      StringBuilder wanted = new StringBuilder();
      if (hadLeft && i == 0) wanted.append("left");
      if (hadRight && i == keys.size() - 1) {
        if (wanted.length() > 0) wanted.append('|');
        wanted.append("right");
      }
      normalized.add(
          withKeyAttr(
              keys.get(i),
              CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS,
              wanted.length() == 0 ? null : wanted.toString()));
    }
    return normalized;
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
