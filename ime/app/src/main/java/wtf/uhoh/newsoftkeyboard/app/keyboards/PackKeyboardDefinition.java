package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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

  @NonNull private final PackManifest manifest;
  @NonNull private final PackEntry keyboardEntry;
  @NonNull private final DirectoryPackSource packSource;
  @NonNull private final Resources hostResources;
  @NonNull private final String hostPackageName;
  @NonNull private final KeyboardCondenser packKeyboardCondenser;
  @NonNull private final PackKeyboardKeySpecApplier keySpecApplier;
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
    keySpecApplier = new PackKeyboardKeySpecApplier(hostResources, hostPackageName);
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

    final var keyboardDefaults =
        keySpecApplier.parseKeyboardDefaults(model.rawKeyboardAttributes(), mDisplayWidth);
    showPreview = keyboardDefaults.showPreview();
    autoCap = keyboardDefaults.autoCap();
    setProximityThresholdSquareFromDefaultWidth(keyboardDefaults.defaultWidth());

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
      final var rowDefaults =
          keySpecApplier.parseRowDefaults(
              keyboardDefaults, rowModel.rawRowAttributes(), mDisplayWidth);
      row.defaultWidth = rowDefaults.defaultWidth();
      row.defaultHeightCode = rowDefaults.defaultHeightCode();
      row.defaultHorizontalGap = rowDefaults.defaultHorizontalGap();
      row.verticalGap = rowDefaults.verticalGap();
      row.rowEdgeFlags = rowDefaults.rowEdgeFlags();
      //noinspection WrongConstant
      row.mode = rowDefaults.mode();

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
        keySpecApplier.applyKeySpec(
            key, row, keyboardDimens, keySpec, mDisplayWidth, mKeysHeightFactor, showPreview);
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
}
