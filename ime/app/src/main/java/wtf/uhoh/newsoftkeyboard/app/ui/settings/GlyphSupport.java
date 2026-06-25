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

import android.graphics.Paint;
import android.os.Build;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Filters codepoints to those the current device font can actually render, so the character grid
 * never shows ".notdef"/tofu boxes for glyphs missing from an older device's fonts.
 *
 * <p>{@link Paint#hasGlyph(String)} is API 23+. On API 21-22 it is unavailable, so we don't filter
 * (degrading to the previous behavior on those rare devices rather than hiding everything).
 */
final class GlyphSupport {

  // Paint is not thread-safe; only ever touched from the UI thread (picker rendering).
  private static final Paint PAINT = new Paint();

  private GlyphSupport() {}

  static boolean isRenderable(int codePoint) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
    return PAINT.hasGlyph(new String(Character.toChars(codePoint)));
  }

  /** Returns a new list containing only the codepoints the device can render. */
  @NonNull
  static List<Integer> filterRenderable(@NonNull List<Integer> codePoints) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return codePoints;
    final List<Integer> out = new ArrayList<>(codePoints.size());
    for (int cp : codePoints) {
      if (PAINT.hasGlyph(new String(Character.toChars(cp)))) out.add(cp);
    }
    return out;
  }
}
