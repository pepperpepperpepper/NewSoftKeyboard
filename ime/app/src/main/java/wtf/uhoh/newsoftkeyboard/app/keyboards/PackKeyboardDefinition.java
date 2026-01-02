package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.addons.AddOnImpl;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboardextensions.KeyboardExtension;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.DirectoryPackSource;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;

/** A {@link KeyboardDefinition} backed by a file-based keyboard pack. */
public final class PackKeyboardDefinition extends KeyboardDefinition {
  private static final String TAG = "PackKeyboardDef";

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

  @NonNull private final PackManifest manifest;
  @NonNull private final PackEntry keyboardEntry;
  @NonNull private final DirectoryPackSource packSource;
  @NonNull private final Resources hostResources;
  @NonNull private final String hostPackageName;
  @NonNull private final KeyboardCondenser packKeyboardCondenser;
  @NonNull private final String keyboardId;
  @NonNull private final CharSequence keyboardName;
  private final boolean includeGenericRows;

  public PackKeyboardDefinition(
      @NonNull Context hostAppContext,
      @NonNull PackManifest manifest,
      @NonNull PackEntry keyboardEntry,
      @KeyboardRowModeId int mode,
      @NonNull DirectoryPackSource packSource)
      throws IOException {
    this(hostAppContext, manifest, keyboardEntry, mode, packSource, true);
  }

  public PackKeyboardDefinition(
      @NonNull Context hostAppContext,
      @NonNull PackManifest manifest,
      @NonNull PackEntry keyboardEntry,
      @KeyboardRowModeId int mode,
      @NonNull DirectoryPackSource packSource,
      boolean includeGenericRows)
      throws IOException {
    super(
        new PackKeyboardAddOn(
            hostAppContext,
            manifest,
            keyboardEntry,
            hostAppContext
                .getResources()
                .getInteger(com.anysoftkeyboard.api.R.integer.anysoftkeyboard_api_version_code)),
        hostAppContext,
        AddOn.INVALID_RES_ID,
        mode);

    this.manifest = Objects.requireNonNull(manifest);
    this.keyboardEntry = Objects.requireNonNull(keyboardEntry);
    this.packSource = Objects.requireNonNull(packSource);
    hostResources = hostAppContext.getResources();
    hostPackageName = hostAppContext.getPackageName();
    packKeyboardCondenser = new KeyboardCondenser(hostAppContext, this);
    keyboardId = PackKeyboardAddOn.buildKeyboardId(manifest, keyboardEntry);
    keyboardName = PackKeyboardAddOn.buildKeyboardName(manifest, keyboardEntry);
    this.includeGenericRows = includeGenericRows;
  }

  @Override
  public void loadKeyboard(final KeyboardDimens keyboardDimens) {
    setKeyboardDimensReflectively(keyboardDimens);
    mDisplayWidth = keyboardDimens.getKeyboardMaxWidth();

    // reset key state (PackKeyboardDefinition instances may be cached)
    setShifted(false);
    setShiftLocked(false);
    setControl(false);
    setAlt(false, false);
    setFunction(false, false);
    setVoice(false, false);

    getKeys().clear();
    getModifierKeys().clear();

    final float rowVerticalGap = keyboardDimens.getRowVerticalGap();
    final float keyHorizontalGap = keyboardDimens.getKeyHorizontalGap();

    KeyboardModel model;
    try (InputStream in = packSource.open(keyboardEntry.path().value())) {
      model = AskXmlKeyboardParser.parse(in);
    } catch (IOException e) {
      Logger.e(TAG, e, "Failed to parse pack keyboard XML for %s", keyboardEntry.id());
      return;
    }

    final KeyboardDefaults keyboardDefaults = parseKeyboardDefaults(model.rawKeyboardAttributes());
    showPreview = keyboardDefaults.showPreview;
    autoCap = keyboardDefaults.autoCap;
    setProximityThresholdSquareFromDefaultWidth(keyboardDefaults.defaultWidth);

    boolean hasTopRow = false;
    boolean hasBottomRow = false;

    float x;
    float y = rowVerticalGap; // starts with a gap
    int rowHeight;
    float lastVerticalGap = rowVerticalGap;
    int totalWidth = 0;

    int rowModelIndex = 0;
    for (KeyboardRow rowModel : model.rows()) {
      final Row row = new Row(this);
      final RowDefaults rowDefaults =
          parseRowDefaults(keyboardDefaults, rowModel.rawRowAttributes());
      row.defaultWidth = rowDefaults.defaultWidth;
      row.defaultHeightCode = rowDefaults.defaultHeightCode;
      row.defaultHorizontalGap = rowDefaults.defaultHorizontalGap;
      row.verticalGap = rowDefaults.verticalGap;
      row.rowEdgeFlags = rowDefaults.rowEdgeFlags;
      //noinspection WrongConstant
      row.mode = rowDefaults.mode;

      if ((row.rowEdgeFlags & EDGE_TOP) != 0) hasTopRow = true;
      if ((row.rowEdgeFlags & EDGE_BOTTOM) != 0) hasBottomRow = true;

      if (!row.isRowValidForMode(mKeyboardMode)) {
        rowModelIndex++;
        continue;
      }

      x = 0;
      rowHeight = 0;
      int keyModelIndex = 0;
      for (KeySpec keySpec : rowModel.keys()) {
        x += (keyHorizontalGap / 2f);

        final PackKeyboardKey key = new PackKeyboardKey(row, keyboardDimens);
        key.setPackLocation(rowModelIndex, keyModelIndex);
        key.x = (int) x;
        key.y = (int) y;
        applyKeySpec(key, row, keyboardDimens, keySpec);
        rowHeight = Math.max(rowHeight, key.height);
        key.width = (int) (key.width - keyHorizontalGap); // the gap is on both sides

        addKeyFromParser(key);
        setupKeyAfterCreation(key);

        x += key.gap + key.width;
        x += (keyHorizontalGap / 2f);
        if (x > totalWidth) totalWidth = (int) x;
        keyModelIndex++;
      }

      if (row.verticalGap >= 0) lastVerticalGap = row.verticalGap;
      else lastVerticalGap = rowVerticalGap;
      y += lastVerticalGap;
      y += rowHeight;
      rowModelIndex++;
    }

    setTotalDimensionsFromParser(totalWidth, (int) (y - lastVerticalGap));
    setGenericRowPresenceFlagsReflectively(hasTopRow, hasBottomRow);

    if (includeGenericRows) {
      final KeyboardExtension topRowPlugin =
          NskApplicationBase.getTopRowFactory(mLocalContext).getEnabledAddOn();
      final KeyboardExtension bottomRowPlugin =
          NskApplicationBase.getBottomRowFactory(mLocalContext).getEnabledAddOn();
      addGenericRows(keyboardDimens, topRowPlugin, bottomRowPlugin);
    }

    // Preserve the same initialization steps as XML-based keyboards.
    boolean rtl =
        KeyMembersInitializer.initKeysMembers(
            mLocalContext, mLocalContext, getKeys(), keyboardDimens, false);
    setRightToLeftLayoutReflectively(rtl);
    KeyEdgeFlagsFixer.fixEdgeFlags(getKeys());
  }

  private KeyboardDefaults parseKeyboardDefaults(@NonNull Map<String, String> rawAttributes) {
    int defaultWidth = mDisplayWidth / 10;
    int defaultHeightCode = -1;
    int defaultHorizontalGap = 0;
    int defaultVerticalGap = -1;

    boolean parsedShowPreview = true;
    boolean parsedAutoCap = true;

    if (rawAttributes.containsKey(ATTR_KEY_WIDTH)) {
      defaultWidth =
          parseDimensionOrFraction(rawAttributes.get(ATTR_KEY_WIDTH), mDisplayWidth, defaultWidth);
    }
    if (rawAttributes.containsKey(ATTR_KEY_HEIGHT)) {
      defaultHeightCode =
          parseKeyHeightCode(rawAttributes.get(ATTR_KEY_HEIGHT), defaultHeightCode, hostResources);
    }
    if (rawAttributes.containsKey(ATTR_HORIZONTAL_GAP)) {
      defaultHorizontalGap =
          parseDimensionOrFraction(
              rawAttributes.get(ATTR_HORIZONTAL_GAP), mDisplayWidth, defaultHorizontalGap);
    }
    if (rawAttributes.containsKey(ATTR_VERTICAL_GAP)) {
      defaultVerticalGap =
          parseDimensionOrFraction(
              rawAttributes.get(ATTR_VERTICAL_GAP), mDisplayWidth, defaultVerticalGap);
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

  private RowDefaults parseRowDefaults(
      @NonNull KeyboardDefaults keyboardDefaults, @NonNull Map<String, String> rowAttrs) {
    int defaultWidth = keyboardDefaults.defaultWidth;
    int defaultHeightCode = keyboardDefaults.defaultHeightCode;
    int defaultHorizontalGap = keyboardDefaults.defaultHorizontalGap;
    int verticalGap = keyboardDefaults.defaultVerticalGap;

    int rowEdgeFlags = 0;
    int mode = KEYBOARD_ROW_MODE_NONE;

    if (rowAttrs.containsKey(ATTR_KEY_WIDTH)) {
      defaultWidth =
          parseDimensionOrFraction(rowAttrs.get(ATTR_KEY_WIDTH), mDisplayWidth, defaultWidth);
    }
    if (rowAttrs.containsKey(ATTR_KEY_HEIGHT)) {
      defaultHeightCode =
          parseKeyHeightCode(rowAttrs.get(ATTR_KEY_HEIGHT), defaultHeightCode, hostResources);
    }
    if (rowAttrs.containsKey(ATTR_HORIZONTAL_GAP)) {
      defaultHorizontalGap =
          parseDimensionOrFraction(
              rowAttrs.get(ATTR_HORIZONTAL_GAP), mDisplayWidth, defaultHorizontalGap);
    }
    if (rowAttrs.containsKey(ATTR_VERTICAL_GAP)) {
      verticalGap =
          parseDimensionOrFraction(rowAttrs.get(ATTR_VERTICAL_GAP), mDisplayWidth, verticalGap);
    }
    if (rowAttrs.containsKey(ATTR_ROW_EDGE_FLAGS)) {
      rowEdgeFlags = parseEdgeFlags(rowAttrs.get(ATTR_ROW_EDGE_FLAGS));
    }
    if (rowAttrs.containsKey(ATTR_KEYBOARD_MODE)) {
      mode = parseKeyboardMode(rowAttrs.get(ATTR_KEYBOARD_MODE), hostResources);
    }

    return new RowDefaults(
        defaultWidth, defaultHeightCode, defaultHorizontalGap, verticalGap, rowEdgeFlags, mode);
  }

  private void applyKeySpec(
      @NonNull PackKeyboardKey key,
      @NonNull Keyboard.Row row,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeySpec keySpec) {
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
          parseDimensionOrFraction(attrs.get(ATTR_KEY_WIDTH), mDisplayWidth, row.defaultWidth);
    }
    if (attrs.containsKey(ATTR_KEY_HEIGHT)) {
      int heightCode =
          parseKeyHeightCode(attrs.get(ATTR_KEY_HEIGHT), row.defaultHeightCode, hostResources);
      key.height =
          KeyboardSupport.getKeyHeightFromHeightCode(keyboardDimens, heightCode, mKeysHeightFactor);
    }
    if (attrs.containsKey(ATTR_HORIZONTAL_GAP)) {
      key.gap =
          parseDimensionOrFraction(
              attrs.get(ATTR_HORIZONTAL_GAP), mDisplayWidth, row.defaultHorizontalGap);
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
      key.showPreview = parseBoolean(attrs.get(ATTR_SHOW_PREVIEW), showPreview);
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

  private int parseKeyboardMode(@Nullable String raw, @NonNull Resources resources) {
    if (raw == null || raw.trim().isEmpty()) return KEYBOARD_ROW_MODE_NONE;
    String value = raw.trim();
    if (value.startsWith("@")) {
      ResRef ref = ResRef.parse(value);
      if (ref == null) return KEYBOARD_ROW_MODE_NONE;
      if (!"integer".equals(ref.type)) return KEYBOARD_ROW_MODE_NONE;
      Resources res = ref.isAndroid ? Resources.getSystem() : resources;
      String pkg = ref.isAndroid ? "android" : hostPackageName;
      int id = res.getIdentifier(ref.name, "integer", pkg);
      return id != 0 ? res.getInteger(id) : KEYBOARD_ROW_MODE_NONE;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return KEYBOARD_ROW_MODE_NONE;
    }
  }

  private int parseEdgeFlags(@Nullable String raw) {
    if (raw == null || raw.trim().isEmpty()) return 0;
    String value = raw.trim();

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
    }

    int result = 0;
    for (String part : value.split("\\|")) {
      switch (part.trim().toLowerCase(Locale.ROOT)) {
        case "left" -> result |= EDGE_LEFT;
        case "right" -> result |= EDGE_RIGHT;
        case "top" -> result |= EDGE_TOP;
        case "bottom" -> result |= EDGE_BOTTOM;
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

  private int parseKeyHeightCode(
      @Nullable String raw, int defaultHeightCode, @NonNull Resources resources) {
    if (raw == null || raw.trim().isEmpty()) return defaultHeightCode;
    String value = raw.trim();

    if (value.startsWith("@")) {
      ResRef ref = ResRef.parse(value);
      if (ref == null) return defaultHeightCode;
      Resources res = ref.isAndroid ? Resources.getSystem() : resources;
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
      return parseDimensionOrFraction(value, mDisplayWidth, defaultHeightCode);
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultHeightCode;
    }
  }

  @Override
  public boolean isAlphabetKeyboard() {
    return true;
  }

  @Override
  public String getDefaultDictionaryLocale() {
    return "";
  }

  @Override
  public char[] getSentenceSeparators() {
    return new char[0];
  }

  @NonNull
  @Override
  public CharSequence getKeyboardName() {
    return keyboardName;
  }

  @NonNull
  @Override
  public String getKeyboardId() {
    return keyboardId;
  }

  @Override
  public int getKeyboardIconResId() {
    return 0;
  }

  @Override
  public void setCondensedKeys(CondenseType condenseType) {
    if (packKeyboardCondenser.setCondensedKeys(condenseType, getKeyboardDimens())) {
      // KeyboardView will recompute proximity thresholds.
      resetDimensions();
    }
  }

  private void setKeyboardDimensReflectively(@NonNull KeyboardDimens keyboardDimens) {
    try {
      Field field = Keyboard.class.getDeclaredField("mKeyboardDimens");
      field.setAccessible(true);
      field.set(this, keyboardDimens);
    } catch (ReflectiveOperationException e) {
      Logger.w(TAG, "Failed setting KeyboardDimens reflectively. Generic rows may misbehave.", e);
    }
  }

  private void setGenericRowPresenceFlagsReflectively(boolean hasTopRow, boolean hasBottomRow) {
    setBooleanField(KeyboardDefinition.class, "mTopRowWasCreated", hasTopRow);
    setBooleanField(KeyboardDefinition.class, "mBottomRowWasCreated", hasBottomRow);
  }

  private void setRightToLeftLayoutReflectively(boolean isRtl) {
    setBooleanField(KeyboardDefinition.class, "mRightToLeftLayout", isRtl);
  }

  private void setBooleanField(@NonNull Class<?> clazz, @NonNull String fieldName, boolean value) {
    try {
      Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.setBoolean(this, value);
    } catch (ReflectiveOperationException e) {
      Logger.w(TAG, "Failed setting %s.%s", clazz.getSimpleName(), fieldName);
    }
  }

  private record KeyboardDefaults(
      int defaultWidth,
      int defaultHeightCode,
      int defaultHorizontalGap,
      int defaultVerticalGap,
      boolean showPreview,
      boolean autoCap) {}

  private record RowDefaults(
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

  private static final class PackKeyboardAddOn extends AddOnImpl {
    PackKeyboardAddOn(
        @NonNull Context hostAppContext,
        @NonNull PackManifest manifest,
        @NonNull PackEntry entry,
        int apiVersion) {
      super(
          hostAppContext,
          hostAppContext,
          apiVersion,
          buildKeyboardId(manifest, entry),
          buildKeyboardName(manifest, entry),
          "",
          false,
          0);
    }

    @NonNull
    static String buildKeyboardId(@NonNull PackManifest manifest, @NonNull PackEntry entry) {
      return "pack::" + manifest.id() + "::" + entry.id();
    }

    @NonNull
    static CharSequence buildKeyboardName(
        @NonNull PackManifest manifest, @NonNull PackEntry entry) {
      return manifest.name() + " — " + entry.id();
    }
  }

  private static final class PackKeyboardKey extends KeyboardKey {
    private boolean shiftCodesAlways;
    private int packRowIndex = -1;
    private int packKeyIndex = -1;

    PackKeyboardKey(Keyboard.Row row, KeyboardDimens keyboardDimens) {
      super(row, keyboardDimens);
      enable();
    }

    void setPackLocation(int rowIndex, int keyIndex) {
      packRowIndex = rowIndex;
      packKeyIndex = keyIndex;
    }

    int packRowIndex() {
      return packRowIndex;
    }

    int packKeyIndex() {
      return packKeyIndex;
    }

    void setShiftCodesAlways(boolean shiftCodesAlways) {
      this.shiftCodesAlways = shiftCodesAlways;
    }

    @Override
    public boolean isShiftCodesAlways() {
      return shiftCodesAlways;
    }
  }

  public static final class PackKeyLocation {
    private final int rowIndex;
    private final int keyIndex;

    private PackKeyLocation(int rowIndex, int keyIndex) {
      this.rowIndex = rowIndex;
      this.keyIndex = keyIndex;
    }

    public int rowIndex() {
      return rowIndex;
    }

    public int keyIndex() {
      return keyIndex;
    }
  }

  @Nullable
  public PackKeyLocation getPackKeyLocation(@NonNull Keyboard.Key key) {
    if (key instanceof PackKeyboardKey) {
      PackKeyboardKey packKey = (PackKeyboardKey) key;
      if (packKey.packRowIndex() >= 0 && packKey.packKeyIndex() >= 0) {
        return new PackKeyLocation(packKey.packRowIndex(), packKey.packKeyIndex());
      }
    }
    return null;
  }

  @Nullable
  public KeyboardDefinition tryCreatePopupKeyboard(
      @NonNull String popupKeyboardPackPath, @NonNull KeyboardDimens keyboardDimens) {
    Objects.requireNonNull(popupKeyboardPackPath);
    Objects.requireNonNull(keyboardDimens);
    try {
      PackPath path = PackPath.parse(popupKeyboardPackPath);
      String entryId = "popup:" + path.value().replace('/', '_');
      PackEntry popupEntry = new PackEntry(entryId, path);
      var keyboard =
          new PackKeyboardDefinition(
              mLocalContext,
              manifest,
              popupEntry,
              Keyboard.KEYBOARD_ROW_MODE_NORMAL,
              packSource,
              false);
      keyboard.loadKeyboard(keyboardDimens);
      return keyboard;
    } catch (Exception e) {
      Logger.w(TAG, e, "Failed creating popup keyboard from '%s'", popupKeyboardPackPath);
      return null;
    }
  }
}
