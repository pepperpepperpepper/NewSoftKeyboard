package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.res.Resources;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;

final class PackKeyboardKeySpecApplier {
  private static final String ANDROID_ATTR_PREFIX = "android:";
  private static final String ASK_ATTR_PREFIX = "ask:";

  private static final String ATTR_KEY_WIDTH = ANDROID_ATTR_PREFIX + "keyWidth";
  private static final String ATTR_KEY_HEIGHT = ANDROID_ATTR_PREFIX + "keyHeight";
  private static final String ATTR_HORIZONTAL_GAP = ANDROID_ATTR_PREFIX + "horizontalGap";
  private static final String ATTR_VERTICAL_GAP = ANDROID_ATTR_PREFIX + "verticalGap";
  private static final String ATTR_ROW_EDGE_FLAGS = ANDROID_ATTR_PREFIX + "rowEdgeFlags";
  private static final String ATTR_KEY_EDGE_FLAGS = ANDROID_ATTR_PREFIX + "keyEdgeFlags";
  private static final String ATTR_KEYBOARD_MODE = ANDROID_ATTR_PREFIX + "keyboardMode";
  private static final String ATTR_CODES = ANDROID_ATTR_PREFIX + "codes";
  private static final String ATTR_KEY_LABEL = ANDROID_ATTR_PREFIX + "keyLabel";
  private static final String ATTR_KEY_OUTPUT_TEXT = ANDROID_ATTR_PREFIX + "keyOutputText";
  private static final String ATTR_IS_REPEATABLE = ANDROID_ATTR_PREFIX + "isRepeatable";
  private static final String ATTR_IS_MODIFIER = ANDROID_ATTR_PREFIX + "isModifier";
  private static final String ATTR_POPUP_CHARACTERS = ANDROID_ATTR_PREFIX + "popupCharacters";
  private static final String ATTR_POPUP_KEYBOARD = ANDROID_ATTR_PREFIX + "popupKeyboard";

  private static final String ATTR_AUTO_CAP = ASK_ATTR_PREFIX + "autoCap";
  private static final String ATTR_SHOW_PREVIEW = ASK_ATTR_PREFIX + "showPreview";
  private static final String ATTR_SHIFTED_CODES = ASK_ATTR_PREFIX + "shiftedCodes";
  private static final String ATTR_SHIFTED_KEY_LABEL = ASK_ATTR_PREFIX + "shiftedKeyLabel";
  private static final String ATTR_LONG_PRESS_CODE = ASK_ATTR_PREFIX + "longPressCode";
  private static final String ATTR_HINT_LABEL = ASK_ATTR_PREFIX + "hintLabel";
  private static final String ATTR_IS_FUNCTIONAL = ASK_ATTR_PREFIX + "isFunctional";
  private static final String ATTR_SHOW_IN_LAYOUT = ASK_ATTR_PREFIX + "showInLayout";
  private static final String ATTR_EXTRA_KEY_DATA = ASK_ATTR_PREFIX + "extra_key_data";
  private static final String ATTR_IS_SHIFT_ALWAYS = ASK_ATTR_PREFIX + "isShiftAlways";
  private static final String ATTR_SHIFTED_OUTPUT_TEXT = ASK_ATTR_PREFIX + "shiftedKeyOutputText";

  @NonNull private final Resources hostResources;
  @NonNull private final String hostPackageName;

  PackKeyboardKeySpecApplier(@NonNull Resources hostResources, @NonNull String hostPackageName) {
    this.hostResources = hostResources;
    this.hostPackageName = hostPackageName;
  }

  KeyboardDefaults parseKeyboardDefaults(
      @NonNull Map<String, String> rawAttributes, int displayWidth) {
    int defaultWidth = displayWidth / 10;
    int defaultHeightCode = -1;
    int defaultHorizontalGap = 0;
    int defaultVerticalGap = -1;

    boolean parsedShowPreview = true;
    boolean parsedAutoCap = true;

    if (rawAttributes.containsKey(ATTR_KEY_WIDTH)) {
      defaultWidth =
          parseDimensionOrFraction(rawAttributes.get(ATTR_KEY_WIDTH), displayWidth, defaultWidth);
    }
    if (rawAttributes.containsKey(ATTR_KEY_HEIGHT)) {
      defaultHeightCode =
          parseKeyHeightCode(rawAttributes.get(ATTR_KEY_HEIGHT), defaultHeightCode, displayWidth);
    }
    if (rawAttributes.containsKey(ATTR_HORIZONTAL_GAP)) {
      defaultHorizontalGap =
          parseDimensionOrFraction(
              rawAttributes.get(ATTR_HORIZONTAL_GAP), displayWidth, defaultHorizontalGap);
    }
    if (rawAttributes.containsKey(ATTR_VERTICAL_GAP)) {
      defaultVerticalGap =
          parseDimensionOrFraction(
              rawAttributes.get(ATTR_VERTICAL_GAP), displayWidth, defaultVerticalGap);
    }

    if (rawAttributes.containsKey(ATTR_SHOW_PREVIEW)) {
      parsedShowPreview = parseBoolean(rawAttributes.get(ATTR_SHOW_PREVIEW), parsedShowPreview);
    }
    if (rawAttributes.containsKey(ATTR_AUTO_CAP)) {
      parsedAutoCap = parseBoolean(rawAttributes.get(ATTR_AUTO_CAP), parsedAutoCap);
    }

    return new KeyboardDefaults(
        defaultWidth,
        defaultHeightCode,
        defaultHorizontalGap,
        defaultVerticalGap,
        parsedShowPreview,
        parsedAutoCap);
  }

  RowDefaults parseRowDefaults(
      @NonNull KeyboardDefaults keyboardDefaults,
      @NonNull Map<String, String> rowAttrs,
      int displayWidth) {
    int defaultWidth = keyboardDefaults.defaultWidth;
    int defaultHeightCode = keyboardDefaults.defaultHeightCode;
    int defaultHorizontalGap = keyboardDefaults.defaultHorizontalGap;
    int verticalGap = keyboardDefaults.defaultVerticalGap;

    int rowEdgeFlags = 0;
    int mode = Keyboard.KEYBOARD_ROW_MODE_NONE;

    if (rowAttrs.containsKey(ATTR_KEY_WIDTH)) {
      defaultWidth =
          parseDimensionOrFraction(rowAttrs.get(ATTR_KEY_WIDTH), displayWidth, defaultWidth);
    }
    if (rowAttrs.containsKey(ATTR_KEY_HEIGHT)) {
      defaultHeightCode =
          parseKeyHeightCode(rowAttrs.get(ATTR_KEY_HEIGHT), defaultHeightCode, displayWidth);
    }
    if (rowAttrs.containsKey(ATTR_HORIZONTAL_GAP)) {
      defaultHorizontalGap =
          parseDimensionOrFraction(
              rowAttrs.get(ATTR_HORIZONTAL_GAP), displayWidth, defaultHorizontalGap);
    }
    if (rowAttrs.containsKey(ATTR_VERTICAL_GAP)) {
      verticalGap =
          parseDimensionOrFraction(rowAttrs.get(ATTR_VERTICAL_GAP), displayWidth, verticalGap);
    }
    if (rowAttrs.containsKey(ATTR_ROW_EDGE_FLAGS)) {
      rowEdgeFlags = parseEdgeFlags(rowAttrs.get(ATTR_ROW_EDGE_FLAGS));
    }
    if (rowAttrs.containsKey(ATTR_KEYBOARD_MODE)) {
      mode = parseKeyboardMode(rowAttrs.get(ATTR_KEYBOARD_MODE));
    }

    return new RowDefaults(
        defaultWidth, defaultHeightCode, defaultHorizontalGap, verticalGap, rowEdgeFlags, mode);
  }

  void applyKeySpec(
      @NonNull PackKeyboardKey key,
      @NonNull Keyboard.Row row,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeySpec keySpec,
      int displayWidth,
      float keysHeightFactor,
      boolean keyboardDefaultShowPreview) {
    final Map<String, String> attrs = keySpec.rawAttributes();

    // label
    String label = keySpec.label() != null ? keySpec.label() : attrs.get(ATTR_KEY_LABEL);
    if (label != null) key.label = label;

    // output text
    if (attrs.containsKey(ATTR_KEY_OUTPUT_TEXT)) {
      key.text = attrs.get(ATTR_KEY_OUTPUT_TEXT);
    }
    if (attrs.containsKey(ATTR_SHIFTED_OUTPUT_TEXT)) {
      key.shiftedText = attrs.get(ATTR_SHIFTED_OUTPUT_TEXT);
    }

    // popup
    String popupCharacters =
        keySpec.popupCharacters() != null
            ? keySpec.popupCharacters()
            : attrs.get(ATTR_POPUP_CHARACTERS);
    if (popupCharacters != null) {
      key.popupCharacters = popupCharacters;
    }
    if (attrs.containsKey(ATTR_POPUP_KEYBOARD)) {
      String raw = attrs.get(ATTR_POPUP_KEYBOARD);
      if (raw != null) {
        String value = raw.trim();
        if (value.startsWith("@")) {
          int popupResId = resolveXmlResourceId(value);
          if (popupResId != 0) {
            key.popupResId = popupResId;
          }
        } else if (!value.isEmpty()) {
          key.popupKeyboardPackPath = value;
        }
      }
    }

    // sizing + gaps
    if (attrs.containsKey(ATTR_KEY_WIDTH)) {
      key.width =
          parseDimensionOrFraction(attrs.get(ATTR_KEY_WIDTH), displayWidth, row.defaultWidth);
    }
    if (attrs.containsKey(ATTR_KEY_HEIGHT)) {
      int heightCode =
          parseKeyHeightCode(attrs.get(ATTR_KEY_HEIGHT), row.defaultHeightCode, displayWidth);
      key.height =
          KeyboardSupport.getKeyHeightFromHeightCode(keyboardDimens, heightCode, keysHeightFactor);
    }
    if (attrs.containsKey(ATTR_HORIZONTAL_GAP)) {
      key.gap =
          parseDimensionOrFraction(
              attrs.get(ATTR_HORIZONTAL_GAP), displayWidth, row.defaultHorizontalGap);
    }
    key.x += key.gap;

    // codes
    int[] codes = parseCodes(attrs.get(ATTR_CODES));
    if (codes.length == 0 && key.label != null && key.label.length() == 1) {
      codes = new int[] {Character.codePointAt(key.label, 0)};
    }
    key.mCodes = codes;

    // shifted codes
    int[] shiftedCodes =
        attrs.containsKey(ATTR_SHIFTED_CODES)
            ? parseCodes(attrs.get(ATTR_SHIFTED_CODES))
            : defaultShiftedCodes(codes);
    key.mShiftedCodes = ensureSized(shiftedCodes, codes);

    // shifted label
    if (attrs.containsKey(ATTR_SHIFTED_KEY_LABEL)) {
      key.shiftedKeyLabel = attrs.get(ATTR_SHIFTED_KEY_LABEL);
    }
    if (attrs.containsKey(ATTR_HINT_LABEL)) {
      key.hintLabel = attrs.get(ATTR_HINT_LABEL);
    }

    // edge flags
    int edgeFlags = row.rowEdgeFlags;
    if (attrs.containsKey(ATTR_KEY_EDGE_FLAGS)) {
      edgeFlags |= parseEdgeFlags(attrs.get(ATTR_KEY_EDGE_FLAGS));
    }
    key.edgeFlags = edgeFlags;

    // behavior
    if (attrs.containsKey(ATTR_IS_REPEATABLE)) {
      key.repeatable = parseBoolean(attrs.get(ATTR_IS_REPEATABLE), false);
    }
    if (attrs.containsKey(ATTR_IS_MODIFIER)) {
      key.modifier = parseBoolean(attrs.get(ATTR_IS_MODIFIER), false);
    }
    if (attrs.containsKey(ATTR_SHOW_PREVIEW)) {
      key.showPreview = parseBoolean(attrs.get(ATTR_SHOW_PREVIEW), keyboardDefaultShowPreview);
    }

    if (attrs.containsKey(ATTR_LONG_PRESS_CODE)) {
      key.longPressCode = parseSingleCode(attrs.get(ATTR_LONG_PRESS_CODE));
    }
    if (attrs.containsKey(ATTR_IS_FUNCTIONAL)) {
      key.setFunctionalKey(parseBoolean(attrs.get(ATTR_IS_FUNCTIONAL), false));
    }
    if (attrs.containsKey(ATTR_SHOW_IN_LAYOUT)) {
      try {
        //noinspection WrongConstant
        key.showKeyInLayout = Integer.parseInt(attrs.get(ATTR_SHOW_IN_LAYOUT));
      } catch (NumberFormatException ignored) {
        // keep default
      }
    }
    if (attrs.containsKey(ATTR_EXTRA_KEY_DATA)) {
      key.setExtraKeyData(attrs.get(ATTR_EXTRA_KEY_DATA));
    }

    final boolean shiftAlwaysOverride =
        attrs.containsKey(ATTR_IS_SHIFT_ALWAYS)
            ? parseBoolean(attrs.get(ATTR_IS_SHIFT_ALWAYS), false)
            : isDefaultShiftCodesAlways(key.mShiftedCodes);
    key.setShiftCodesAlways(shiftAlwaysOverride);
  }

  @NonNull
  private int[] parseCodes(@Nullable String raw) {
    if (raw == null || raw.trim().isEmpty()) return new int[0];

    String value = raw.trim();
    if (value.startsWith("@")) {
      return new int[] {resolveIntegerOrZero(value)};
    }

    String[] parts = value.split(",");
    var result = new ArrayList<Integer>(parts.length);
    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) continue;

      if (token.startsWith("@")) {
        result.add(resolveIntegerOrZero(token));
        continue;
      }

      if (token.length() != 1) {
        try {
          result.add(Integer.parseInt(token));
        } catch (NumberFormatException ignored) {
        }
      } else {
        result.add((int) token.charAt(0));
      }
    }

    int[] codes = new int[result.size()];
    for (int i = 0; i < result.size(); i++) codes[i] = result.get(i);
    return codes;
  }

  private int parseSingleCode(@Nullable String raw) {
    int[] codes = parseCodes(raw);
    return codes.length > 0 ? codes[0] : 0;
  }

  private int resolveXmlResourceId(@Nullable String raw) {
    if (raw == null) return 0;
    String value = raw.trim();
    if (!value.startsWith("@")) return 0;

    ResRef ref = ResRef.parse(value);
    if (ref == null || !"xml".equals(ref.type)) return 0;

    Resources resources = ref.isAndroid ? Resources.getSystem() : hostResources;
    String pkg = ref.isAndroid ? "android" : hostPackageName;
    return resources.getIdentifier(ref.name, ref.type, pkg);
  }

  private int resolveIntegerOrZero(@NonNull String raw) {
    ResRef ref = ResRef.parse(raw);
    if (ref == null) return 0;

    if ("integer".equals(ref.type)) {
      Resources resources = ref.isAndroid ? Resources.getSystem() : hostResources;
      String pkg = ref.isAndroid ? "android" : hostPackageName;
      int id = resources.getIdentifier(ref.name, "integer", pkg);
      return id != 0 ? resources.getInteger(id) : 0;
    }

    return 0;
  }

  private static int[] ensureSized(@NonNull int[] maybeWrongSize, @NonNull int[] targetSize) {
    if (maybeWrongSize.length == targetSize.length) return maybeWrongSize;
    int[] resized = new int[targetSize.length];
    int copyCount = Math.min(maybeWrongSize.length, resized.length);
    System.arraycopy(maybeWrongSize, 0, resized, 0, copyCount);
    for (int i = copyCount; i < resized.length; i++) {
      resized[i] = targetSize[i];
    }
    return resized;
  }

  private static int[] defaultShiftedCodes(@NonNull int[] codes) {
    if (codes.length == 0) return new int[0];
    int[] shifted = new int[codes.length];
    for (int i = 0; i < codes.length; i++) {
      int code = codes[i];
      if (Character.isLetter(code)) {
        shifted[i] = Character.toUpperCase(code);
      } else {
        shifted[i] = code;
      }
    }
    return shifted;
  }

  private static boolean isDefaultShiftCodesAlways(@NonNull int[] shiftedCodes) {
    return shiftedCodes.length == 0
        || Character.isLetter(shiftedCodes[0])
        || Character.getType(shiftedCodes[0]) == Character.NON_SPACING_MARK
        || Character.getType(shiftedCodes[0]) == Character.COMBINING_SPACING_MARK;
  }

  private static boolean parseBoolean(@Nullable String raw, boolean defaultValue) {
    if (raw == null) return defaultValue;
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if (v.equals("true") || v.equals("1")) return true;
    if (v.equals("false") || v.equals("0")) return false;
    return defaultValue;
  }

  private int parseKeyboardMode(@Nullable String raw) {
    if (raw == null || raw.trim().isEmpty()) return Keyboard.KEYBOARD_ROW_MODE_NONE;
    String value = raw.trim();
    if (value.startsWith("@")) {
      ResRef ref = ResRef.parse(value);
      if (ref == null) return Keyboard.KEYBOARD_ROW_MODE_NONE;
      if (!"integer".equals(ref.type)) return Keyboard.KEYBOARD_ROW_MODE_NONE;
      Resources res = ref.isAndroid ? Resources.getSystem() : hostResources;
      String pkg = ref.isAndroid ? "android" : hostPackageName;
      int id = res.getIdentifier(ref.name, "integer", pkg);
      return id != 0 ? res.getInteger(id) : Keyboard.KEYBOARD_ROW_MODE_NONE;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return Keyboard.KEYBOARD_ROW_MODE_NONE;
    }
  }

  private static int parseEdgeFlags(@Nullable String raw) {
    if (raw == null || raw.trim().isEmpty()) return 0;
    String value = raw.trim();

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
    }

    int result = 0;
    for (String part : value.split("\\|")) {
      switch (part.trim().toLowerCase(Locale.ROOT)) {
        case "left" -> result |= Keyboard.EDGE_LEFT;
        case "right" -> result |= Keyboard.EDGE_RIGHT;
        case "top" -> result |= Keyboard.EDGE_TOP;
        case "bottom" -> result |= Keyboard.EDGE_BOTTOM;
      }
    }
    return result;
  }

  private int parseDimensionOrFraction(@Nullable String raw, int base, int defaultValue) {
    if (raw == null) return defaultValue;
    String value = raw.trim();
    if (value.isEmpty()) return defaultValue;

    if (value.startsWith("@")) {
      ResRef ref = ResRef.parse(value);
      if (ref == null) return defaultValue;
      Resources res = ref.isAndroid ? Resources.getSystem() : hostResources;
      String pkg = ref.isAndroid ? "android" : hostPackageName;
      int id = res.getIdentifier(ref.name, ref.type, pkg);
      if (id == 0) return defaultValue;
      return switch (ref.type) {
        case "integer" -> res.getInteger(id);
        case "dimen" -> res.getDimensionPixelOffset(id);
        case "fraction" -> Math.round(res.getFraction(id, base, base));
        default -> defaultValue;
      };
    }

    if (value.endsWith("%p") || value.endsWith("%")) {
      int percentEnd = value.indexOf('%');
      if (percentEnd <= 0) return defaultValue;
      try {
        float pct = Float.parseFloat(value.substring(0, percentEnd));
        return Math.round((pct / 100f) * base);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    if (value.endsWith("dp") || value.endsWith("dip")) {
      try {
        float dp = Float.parseFloat(value.replace("dip", "").replace("dp", ""));
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, hostResources.getDisplayMetrics()));
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    if (value.endsWith("px")) {
      try {
        float px = Float.parseFloat(value.substring(0, value.length() - 2));
        return Math.round(px);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private int parseKeyHeightCode(@Nullable String raw, int defaultHeightCode, int displayWidth) {
    if (raw == null || raw.trim().isEmpty()) return defaultHeightCode;
    String value = raw.trim();

    if (value.startsWith("@")) {
      ResRef ref = ResRef.parse(value);
      if (ref == null) return defaultHeightCode;
      Resources res = ref.isAndroid ? Resources.getSystem() : hostResources;
      String pkg = ref.isAndroid ? "android" : hostPackageName;
      int id = res.getIdentifier(ref.name, ref.type, pkg);
      if (id == 0) return defaultHeightCode;
      return switch (ref.type) {
        case "integer" -> res.getInteger(id);
        case "dimen" -> res.getDimensionPixelOffset(id);
        default -> defaultHeightCode;
      };
    }

    if (value.endsWith("dp") || value.endsWith("dip") || value.endsWith("px")) {
      return parseDimensionOrFraction(value, displayWidth, defaultHeightCode);
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultHeightCode;
    }
  }

  record KeyboardDefaults(
      int defaultWidth,
      int defaultHeightCode,
      int defaultHorizontalGap,
      int defaultVerticalGap,
      boolean showPreview,
      boolean autoCap) {}

  record RowDefaults(
      int defaultWidth,
      int defaultHeightCode,
      int defaultHorizontalGap,
      int verticalGap,
      int rowEdgeFlags,
      int mode) {}

  private static final class ResRef {
    final boolean isAndroid;
    @NonNull final String type;
    @NonNull final String name;

    private ResRef(boolean isAndroid, @NonNull String type, @NonNull String name) {
      this.isAndroid = isAndroid;
      this.type = type;
      this.name = name;
    }

    @Nullable
    static ResRef parse(@NonNull String raw) {
      String value = raw.trim();
      if (!value.startsWith("@")) return null;

      String withoutAt = value.substring(1);
      boolean isAndroid = false;
      if (withoutAt.startsWith("android:")) {
        isAndroid = true;
        withoutAt = withoutAt.substring("android:".length());
      } else {
        int colon = withoutAt.indexOf(':');
        if (colon != -1) {
          // unknown package - treat as unsupported
          return null;
        }
      }

      int slash = withoutAt.indexOf('/');
      if (slash <= 0 || slash == withoutAt.length() - 1) return null;
      String type = withoutAt.substring(0, slash);
      String name = withoutAt.substring(slash + 1);
      return new ResRef(isAndroid, type, name);
    }
  }
}
