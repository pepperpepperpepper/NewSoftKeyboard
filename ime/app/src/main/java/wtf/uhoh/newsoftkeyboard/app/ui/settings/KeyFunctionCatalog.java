/*
 * Copyright (C) 2026 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Curated, categorized catalog of the keyboard's function key codes, so the key editor can offer a
 * "pick a function" list instead of asking the user to type raw integers like {@code -5}.
 *
 * <p>Codes mirror {@link KeyCodes} (the canonical constants), so they stay in sync. The editor
 * writes the raw integer into {@code android:codes} — not the symbolic {@code @integer/...} ref —
 * because validation's raw-model path only recognizes numeric codes.
 *
 * <p>Pure layer switches that need a switch-target ({@code CUSTOM_KEYBOARD_SWITCH}) are intentionally
 * excluded here; those are handled by the editor's "Layout switch" key type.
 */
public final class KeyFunctionCatalog {

  /** A single selectable function. {@code defaultLabel} is a suggested key label (may be empty). */
  public static final class Entry {
    public final int code;
    @NonNull public final String name;
    @NonNull public final String defaultLabel;

    Entry(int code, @NonNull String name, @NonNull String defaultLabel) {
      this.code = code;
      this.name = name;
      this.defaultLabel = defaultLabel;
    }
  }

  /** A named group of {@link Entry} items, for display as a section. */
  public static final class Category {
    @NonNull public final String title;
    @NonNull public final List<Entry> entries;

    Category(@NonNull String title, @NonNull List<Entry> entries) {
      this.title = title;
      this.entries = Collections.unmodifiableList(entries);
    }
  }

  private static final List<Category> CATEGORIES = build();
  private static final Map<Integer, Entry> BY_CODE = index(CATEGORIES);

  private KeyFunctionCatalog() {}

  @NonNull
  public static List<Category> categories() {
    return CATEGORIES;
  }

  /** The catalog entry for a numeric code, or {@code null} if it isn't a catalogued function. */
  @Nullable
  public static Entry findByCode(int code) {
    return BY_CODE.get(code);
  }

  private static List<Category> build() {
    final List<Category> categories = new ArrayList<>();

    categories.add(
        category(
            "Editing",
            entry(KeyCodes.DELETE, "Backspace", "⌫"),
            entry(KeyCodes.FORWARD_DELETE, "Forward delete", "⌦"),
            entry(KeyCodes.DELETE_WORD, "Delete word", "⌫"),
            entry(KeyCodes.CLEAR_INPUT, "Clear input", "")));

    categories.add(
        category(
            "Whitespace & action",
            entry(KeyCodes.ENTER, "Enter / Done", "↵"),
            entry(KeyCodes.SPACE, "Space", ""),
            entry(KeyCodes.TAB, "Tab", "⇥")));

    categories.add(
        category(
            "Modifiers",
            entry(KeyCodes.SHIFT, "Shift", "⇧"),
            entry(KeyCodes.SHIFT_LOCK, "Caps lock", "⇪"),
            entry(KeyCodes.CTRL, "Ctrl", "Ctrl"),
            entry(KeyCodes.CTRL_LOCK, "Ctrl lock", "Ctrl"),
            entry(KeyCodes.ALT, "Alt", "Alt")));

    categories.add(
        category(
            "Navigation",
            entry(KeyCodes.ARROW_LEFT, "Arrow left", "←"),
            entry(KeyCodes.ARROW_RIGHT, "Arrow right", "→"),
            entry(KeyCodes.ARROW_UP, "Arrow up", "↑"),
            entry(KeyCodes.ARROW_DOWN, "Arrow down", "↓"),
            entry(KeyCodes.MOVE_HOME, "Home", "Home"),
            entry(KeyCodes.MOVE_END, "End", "End")));

    categories.add(
        category(
            "Clipboard",
            entry(KeyCodes.CLIPBOARD_COPY, "Copy", "Copy"),
            entry(KeyCodes.CLIPBOARD_CUT, "Cut", "Cut"),
            entry(KeyCodes.CLIPBOARD_PASTE, "Paste", "Paste"),
            entry(KeyCodes.CLIPBOARD_SELECT, "Select", "Select"),
            entry(KeyCodes.CLIPBOARD_SELECT_ALL, "Select all", "Select all"),
            entry(KeyCodes.UNDO, "Undo", "↶"),
            entry(KeyCodes.REDO, "Redo", "↷")));

    categories.add(
        category(
            "Layers",
            entry(KeyCodes.MODE_SYMBOLS, "Symbols layer", "?123"),
            entry(KeyCodes.MODE_ALPHABET, "Alphabet layer", "ABC"),
            entry(KeyCodes.KEYBOARD_MODE_CHANGE, "Switch input mode", "?123")));

    categories.add(
        category(
            "Other",
            entry(KeyCodes.SETTINGS, "Settings", "⚙"),
            entry(KeyCodes.VOICE_INPUT, "Voice input", "🎙"),
            entry(KeyCodes.QUICK_TEXT, "Emoji", "😀"),
            entry(KeyCodes.EMOJI_SEARCH, "Emoji search", "🔍"),
            entry(KeyCodes.DOMAIN, "Domain (.com)", ".com"),
            entry(KeyCodes.CANCEL, "Close keyboard", "✕")));

    return Collections.unmodifiableList(categories);
  }

  private static Category category(String title, Entry... entries) {
    final List<Entry> list = new ArrayList<>(entries.length);
    Collections.addAll(list, entries);
    return new Category(title, list);
  }

  private static Entry entry(int code, String name, String defaultLabel) {
    return new Entry(code, name, defaultLabel);
  }

  private static Map<Integer, Entry> index(List<Category> categories) {
    final Map<Integer, Entry> map = new LinkedHashMap<>();
    for (Category category : categories) {
      for (Entry entry : category.entries) {
        map.put(entry.code, entry);
      }
    }
    return Collections.unmodifiableMap(map);
  }
}
