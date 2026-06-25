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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A broad, navigable library of characters that can be placed on a key.
 *
 * <p>Completeness comes from three layers: (1) categorized Unicode ranges to <em>browse</em>;
 * (2) name search via {@link Character#getName(int)} across those ranges; and (3) {@link
 * #parseCodepoint} which accepts an arbitrary {@code U+XXXX} code or a pasted character, so any
 * codepoint at all — including ones not in a browse category (CJK, rare or future symbols) — is
 * reachable.
 *
 * <p>Pure logic; unit-testable on the host JVM.
 */
public final class CharacterLibrary {

  /** A named, browseable group backed by one or more inclusive codepoint ranges. */
  public static final class Category {
    @NonNull public final String title;
    @NonNull final int[][] ranges; // each entry is {startInclusive, endInclusive}

    Category(@NonNull String title, @NonNull int[][] ranges) {
      this.title = title;
      this.ranges = ranges;
    }
  }

  private static final List<Category> CATEGORIES = build();

  private CharacterLibrary() {}

  @NonNull
  public static List<Category> categories() {
    return CATEGORIES;
  }

  /** The string for a codepoint (handles supplementary-plane characters such as emoji). */
  @NonNull
  public static String glyph(int codePoint) {
    return new String(Character.toChars(codePoint));
  }

  /** The Unicode name of a codepoint, or {@code null} if unavailable. */
  @Nullable
  public static String nameOf(int codePoint) {
    try {
      return Character.getName(codePoint);
    } catch (Exception e) {
      return null;
    }
  }

  /** True when a codepoint is a sensible standalone key glyph (assigned, printable, not combining). */
  public static boolean isPickable(int codePoint) {
    if (!Character.isDefined(codePoint) || Character.isISOControl(codePoint)) return false;
    switch (Character.getType(codePoint)) {
      case Character.UPPERCASE_LETTER:
      case Character.LOWERCASE_LETTER:
      case Character.TITLECASE_LETTER:
      case Character.MODIFIER_LETTER:
      case Character.OTHER_LETTER:
      case Character.DECIMAL_DIGIT_NUMBER:
      case Character.LETTER_NUMBER:
      case Character.OTHER_NUMBER:
      case Character.DASH_PUNCTUATION:
      case Character.START_PUNCTUATION:
      case Character.END_PUNCTUATION:
      case Character.CONNECTOR_PUNCTUATION:
      case Character.OTHER_PUNCTUATION:
      case Character.INITIAL_QUOTE_PUNCTUATION:
      case Character.FINAL_QUOTE_PUNCTUATION:
      case Character.MATH_SYMBOL:
      case Character.CURRENCY_SYMBOL:
      case Character.MODIFIER_SYMBOL:
      case Character.OTHER_SYMBOL:
        return true;
      default:
        return false;
    }
  }

  /** All pickable codepoints in a category, in order. */
  @NonNull
  public static List<Integer> codepointsIn(@NonNull Category category) {
    final List<Integer> out = new ArrayList<>();
    for (int[] range : category.ranges) {
      for (int cp = range[0]; cp <= range[1]; cp++) {
        if (isPickable(cp)) out.add(cp);
      }
    }
    return out;
  }

  /**
   * Searches the browseable ranges by Unicode name (case-insensitive substring), exact glyph match,
   * or {@code U+hex} codepoint. Capped at {@code limit} results.
   */
  @NonNull
  public static List<Integer> search(@Nullable String query, int limit) {
    if (query == null) return Collections.emptyList();
    final String q = query.trim().toLowerCase();
    if (q.isEmpty()) return Collections.emptyList();

    final List<Integer> out = new ArrayList<>();

    // Direct codepoint / single pasted character first.
    final Integer direct = parseCodepoint(query);
    if (direct != null && isPickable(direct)) out.add(direct);

    for (Category category : CATEGORIES) {
      for (int[] range : category.ranges) {
        for (int cp = range[0]; cp <= range[1]; cp++) {
          if (out.size() >= limit) return out;
          if (!isPickable(cp)) continue;
          if (direct != null && cp == direct) continue; // already added
          final String name = nameOf(cp);
          if (name != null && name.toLowerCase().contains(q)) {
            out.add(cp);
          } else if (glyph(cp).equals(query.trim())) {
            out.add(cp);
          }
        }
      }
    }
    return out;
  }

  /**
   * Parses an arbitrary codepoint reference: {@code U+1F600}, {@code 1F600}, {@code 0x1F600}, a bare
   * decimal, or a single pasted character. Returns {@code null} if it isn't a single codepoint.
   */
  @Nullable
  public static Integer parseCodepoint(@Nullable String raw) {
    if (raw == null) return null;
    String t = raw.trim();
    if (t.isEmpty()) return null;

    // A single (possibly supplementary) character pasted directly.
    if (t.codePointCount(0, t.length()) == 1) {
      return t.codePointAt(0);
    }

    String hex = t;
    if (hex.startsWith("U+") || hex.startsWith("u+")) hex = hex.substring(2);
    else if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);

    try {
      int cp = Integer.parseInt(hex, 16);
      if (Character.isValidCodePoint(cp)) return cp;
    } catch (NumberFormatException ignored) {
      // not hex; try decimal
    }
    try {
      int cp = Integer.parseInt(t, 10);
      if (Character.isValidCodePoint(cp)) return cp;
    } catch (NumberFormatException ignored) {
      // not a codepoint
    }
    return null;
  }

  private static List<Category> build() {
    final List<Category> categories = new ArrayList<>();

    // Emoji first — the most-requested set. Covers every standard emoji block; non-renderable ones
    // are filtered out at display time by the device-font check.
    categories.add(
        cat(
            "Emoji",
            r(0x1F600, 0x1F64F), // Emoticons
            r(0x1F300, 0x1F5FF), // Misc Symbols & Pictographs
            r(0x1F680, 0x1F6FF), // Transport & Map
            r(0x1F900, 0x1F9FF), // Supplemental Symbols & Pictographs
            r(0x1FA70, 0x1FAFF), // Symbols & Pictographs Extended-A
            r(0x2600, 0x26FF), // Misc Symbols (☀ ☂ ★ ☎ …)
            r(0x2700, 0x27BF), // Dingbats (✂ ✈ ✉ ✏ ✔ …)
            r(0x2B00, 0x2BFF))); // Misc Symbols & Arrows (⭐ ⬆ …)

    categories.add(
        cat(
            "Letters & accents",
            r(0x41, 0x5A), r(0x61, 0x7A), r(0x00C0, 0x024F), r(0x1E00, 0x1EFF)));

    categories.add(
        cat(
            "Punctuation",
            r(0x21, 0x2F), r(0x3A, 0x40), r(0x5B, 0x60), r(0x7B, 0x7E), r(0x00A1, 0x00BF),
            r(0x2000, 0x206F), // General Punctuation
            r(0x2E00, 0x2E7F), // Supplemental Punctuation
            r(0x3000, 0x303F))); // CJK Symbols & Punctuation

    categories.add(cat("Currency", r(0x24, 0x24), r(0x00A2, 0x00A5), r(0x20A0, 0x20CF)));

    categories.add(
        cat(
            "Math & technical",
            r(0x00D7, 0x00D7), r(0x00F7, 0x00F7),
            r(0x2100, 0x214F), // Letterlike Symbols (™ ℃ № …)
            r(0x2150, 0x218F), // Number Forms (½ Ⅳ …)
            r(0x2200, 0x22FF), // Mathematical Operators
            r(0x2300, 0x23FF), // Misc Technical (⌘ ⌫ ⏏ …)
            r(0x2A00, 0x2AFF))); // Supplemental Math Operators

    categories.add(
        cat(
            "Arrows",
            r(0x2190, 0x21FF), r(0x27F0, 0x27FF), r(0x2900, 0x297F)));

    categories.add(
        cat(
            "Shapes & lines",
            r(0x2500, 0x257F), // Box Drawing
            r(0x2580, 0x259F), // Block Elements
            r(0x25A0, 0x25FF), // Geometric Shapes
            r(0x1F780, 0x1F7FF))); // Geometric Shapes Extended

    categories.add(cat("Braille", r(0x2800, 0x28FF)));

    categories.add(cat("Greek", r(0x0370, 0x03FF), r(0x1F00, 0x1FFF)));

    categories.add(cat("Cyrillic", r(0x0400, 0x052F)));

    categories.add(cat("Hebrew", r(0x0590, 0x05FF)));

    categories.add(cat("Arabic", r(0x0600, 0x06FF), r(0x0750, 0x077F)));

    categories.add(cat("Devanagari", r(0x0900, 0x097F)));

    categories.add(cat("Japanese kana", r(0x3040, 0x309F), r(0x30A0, 0x30FF)));

    categories.add(cat("Korean (Hangul)", r(0xAC00, 0xD7A3)));

    categories.add(cat("CJK (Chinese/Japanese)", r(0x3400, 0x4DBF), r(0x4E00, 0x9FFF)));

    categories.add(
        cat("Digits & numerals", r(0x30, 0x39), r(0x2070, 0x209F), r(0x00BC, 0x00BE)));

    return Collections.unmodifiableList(categories);
  }

  private static Category cat(String title, int[]... ranges) {
    return new Category(title, ranges);
  }

  private static int[] r(int start, int end) {
    return new int[] {start, end};
  }
}
