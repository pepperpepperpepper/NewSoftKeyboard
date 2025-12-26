/*
 * Copyright (c) 2013 Menny Even-Danan
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

package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.TypedValue;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.anysoftkeyboard.api.KeyCodes;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

public abstract class Keyboard {

  private static final String TAG = "NSKKbd";

  public static final String PREF_KEY_ROW_MODE_ENABLED_PREFIX =
      "settings_key_support_keyboard_type_state_row_type_";

  public static final int KEYBOARD_ROW_MODE_NONE = 0;
  public static final int KEYBOARD_ROW_MODE_NORMAL = 1;
  public static final int KEYBOARD_ROW_MODE_IM = 2;
  public static final int KEYBOARD_ROW_MODE_URL = 3;
  public static final int KEYBOARD_ROW_MODE_EMAIL = 4;
  public static final int KEYBOARD_ROW_MODE_PASSWORD = 5;
  private KeyboardDimens mKeyboardDimens;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    KEYBOARD_ROW_MODE_NONE,
    KEYBOARD_ROW_MODE_NORMAL,
    KEYBOARD_ROW_MODE_IM,
    KEYBOARD_ROW_MODE_URL,
    KEYBOARD_ROW_MODE_EMAIL,
    KEYBOARD_ROW_MODE_PASSWORD
  })
  public @interface KeyboardRowModeId {}

  @StringRes
  public static int getPrefKeyForEnabledRowMode(@KeyboardRowModeId int rowMode) {
    switch (rowMode) {
      case KEYBOARD_ROW_MODE_EMAIL:
        return R.string.settings_key_support_keyboard_type_state_row_type_4;
      case KEYBOARD_ROW_MODE_IM:
        return R.string.settings_key_support_keyboard_type_state_row_type_2;
      case KEYBOARD_ROW_MODE_URL:
        return R.string.settings_key_support_keyboard_type_state_row_type_3;
      case KEYBOARD_ROW_MODE_PASSWORD:
        return R.string.settings_key_support_keyboard_type_state_row_type_5;
      case KEYBOARD_ROW_MODE_NORMAL:
      case KEYBOARD_ROW_MODE_NONE:
      default:
        throw new RuntimeException("" + rowMode + " is not a valid KeyboardRowModeId for prefs!");
    }
  }

  // Keyboard XML Tags

  public static final int EDGE_LEFT = 1;
  public static final int EDGE_RIGHT = 1 << 1;
  public static final int EDGE_TOP = 1 << 2;
  public static final int EDGE_BOTTOM = 1 << 3;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      flag = true,
      value = {EDGE_LEFT, EDGE_RIGHT, EDGE_TOP, EDGE_BOTTOM})
  public @interface KeyEdgeValue {}

  public static final int KEY_EMBLEM_NONE = 0x00;
  public static final int KEY_EMBLEM_TEXT = 0x01;
  public static final int KEY_EMBLEM_ICON = 0x02;

  protected final int mLayoutResId;

  protected final float mKeysHeightFactor;

  @NonNull private final AddOn mAddOn;
  @NonNull final Context mLocalContext;
  @NonNull private final AddOn.AddOnResourceMapping mKeyboardResourceMap;

  /** Horizontal gap default for all rows */
  private int mDefaultHorizontalGap;

  /** Default key width */
  private int mDefaultWidth;

  /** Default key height */
  private int mDefaultHeightCode;

  /** Default gap between rows */
  private int mDefaultVerticalGap;

  /** Default {@link Key#showPreview} value. */
  public boolean showPreview = true;

  /** Default auto capitalize at the beginning of sentences and such */
  public boolean autoCap = true;

  /** Is the mKeyboard in the shifted state */
  private boolean mShifted;

  /** Key instance for the shift key, if present */
  private Key mShiftKey;

  /** Total height of the mKeyboard, including the padding and keys */
  private int mTotalHeight;

  /**
   * Total width of the mKeyboard, including left side gaps and keys, but not any gaps on the right
   * side.
   */
  private int mTotalWidth;

  /** List of keys in this mKeyboard */
  private List<Key> mKeys;

  /** List of modifier keys such as Shift & Alt, if any */
  private List<Key> mModifierKeys;

  /** Width of the screen available to fit the mKeyboard */
  protected int mDisplayWidth;

  /** Height of the screen */
  // private int mDisplayHeight;

  /** Keyboard mode, or zero, if none. */
  @KeyboardRowModeId protected final int mKeyboardMode;

  // Variables for pre-computing nearest keys.
  private final KeyboardProximityGrid proximityGrid = new KeyboardProximityGrid();
  private int mProximityThreshold;

  /** Number of key widths from current touch point to search for nearest keys. */
  private static float SEARCH_DISTANCE = 1.8f;

  /**
   * Container for keys in the mKeyboard. All keys in a row are at the same Y-coordinate. Some of
   * the key size defaults can be overridden per row from what the {@link Keyboard} defines.
   */
  public static class Row extends KeyboardRowBase {
    public Row(Keyboard parent) {
      super(parent);
    }

    public Row(
        @NonNull final AddOn.AddOnResourceMapping resourceMap,
        Resources res,
        Keyboard parent,
        XmlResourceParser parser) {
      super(resourceMap, res, parent, parser);
    }
  }

  /** Class for describing the position and characteristics of a single key in the mKeyboard. */
  public abstract static class Key extends KeyboardKeyBase {
    protected Key(Row parent, KeyboardDimens keyboardDimens) {
      super(parent, keyboardDimens);
    }

    protected Key(
        @NonNull AddOn.AddOnResourceMapping resourceMapping,
        Context keyboardContext,
        Row parent,
        KeyboardDimens keyboardDimens,
        int initialX,
        int initialY,
        XmlResourceParser parser) {
      super(resourceMapping, keyboardContext, parent, keyboardDimens, initialX, initialY, parser);
    }
  }

  /**
   * Creates a mKeyboard from the given xml key layout file.
   *
   * @param xmlLayoutResId the resource file that contains the mKeyboard layout and keys.
   */
  protected Keyboard(
      @NonNull AddOn keyboardAddOn, @NonNull Context hostAppContext, int xmlLayoutResId) {
    this(keyboardAddOn, hostAppContext, xmlLayoutResId, KEYBOARD_ROW_MODE_NORMAL);
  }

  protected KeyboardDimens getKeyboardDimens() {
    return mKeyboardDimens;
  }

  protected static int getKeyHeightCode(TypedArray a, int remoteIndex, int defaultHeightCode) {
    TypedValue value = a.peekValue(remoteIndex);
    if (value == null) {
      // means that it was not provided. So I take my mParent's
      return defaultHeightCode;
    } else if (value.type == TypedValue.TYPE_DIMENSION) {
      return a.getDimensionPixelOffset(remoteIndex, defaultHeightCode);
    } else if (value.type >= TypedValue.TYPE_FIRST_INT
        && value.type <= TypedValue.TYPE_LAST_INT
        && value.data <= 0
        && value.data >= -3) {
      return value.data;
    } else {
      Logger.w(TAG, "Key height attribute is incorrectly set! Defaulting to regular height.");
      return -1;
    }
  }

  /**
   * Creates a mKeyboard from the given xml key layout file. Weeds out rows that have a mKeyboard
   * mode defined but don't match the specified mode.
   *
   * @param xmlLayoutResId the resource file that contains the mKeyboard layout and keys.
   * @param modeId mKeyboard mode identifier
   */
  protected Keyboard(
      @NonNull AddOn keyboardAddOn,
      @NonNull Context hostAppContext,
      int xmlLayoutResId,
      @KeyboardRowModeId int modeId) {
    mKeysHeightFactor = KeyboardSupport.getKeyboardHeightFactor(hostAppContext).blockingFirst();
    mAddOn = keyboardAddOn;
    mKeyboardResourceMap = keyboardAddOn.getResourceMapping();

    mLocalContext = hostAppContext;
    mLayoutResId = xmlLayoutResId;
    if (modeId != KEYBOARD_ROW_MODE_NORMAL
        && modeId != KEYBOARD_ROW_MODE_EMAIL
        && modeId != KEYBOARD_ROW_MODE_URL
        && modeId != KEYBOARD_ROW_MODE_IM
        && modeId != KEYBOARD_ROW_MODE_PASSWORD) {
      throw new IllegalArgumentException(
          "modeId much be one of KeyboardRowModeId, not including" + " KEYBOARD_ROW_MODE_NONE.");
    }
    mKeyboardMode = modeId;

    mKeys = new ArrayList<>();
    mModifierKeys = new ArrayList<>();
  }

  @NonNull
  public AddOn getKeyboardAddOn() {
    return mAddOn;
  }

  public List<Key> getKeys() {
    return mKeys;
  }

  public List<Key> getModifierKeys() {
    return mModifierKeys;
  }

  protected int getVerticalGap() {
    return mDefaultVerticalGap;
  }

  int getDefaultWidth() {
    return mDefaultWidth;
  }

  int getDefaultHeightCode() {
    return mDefaultHeightCode;
  }

  int getDefaultHorizontalGap() {
    return mDefaultHorizontalGap;
  }

  /**
   * Returns the total height of the mKeyboard
   *
   * @return the total height of the mKeyboard
   */
  public int getHeight() {
    return mTotalHeight;
  }

  public int getMinWidth() {
    return mTotalWidth;
  }

  public void resetDimensions() {
    mTotalWidth = 0;
    mTotalHeight = 0;
    for (Key key : mKeys) {
      int x = Key.getEndX(key) + key.gap;
      if (x > mTotalWidth) {
        mTotalWidth = x;
      }
      int y = Key.getEndY(key);
      if (y > mTotalHeight) {
        mTotalHeight = y;
      }
    }
  }

  public boolean setShifted(boolean shiftState) {
    if (mShifted != shiftState) {
      mShifted = shiftState;
      return true;
    }
    return false;
  }

  public boolean isShifted() {
    return mShifted;
  }

  @Nullable
  public Key getShiftKey() {
    return mShiftKey;
  }

  protected final void computeNearestNeighbors() {
    proximityGrid.compute(mKeys, getMinWidth(), getHeight(), mProximityThreshold);
  }

  /**
   * Returns the indices of the keys that are closest to the given point.
   *
   * @param x the x-coordinate of the point
   * @param y the y-coordinate of the point
   * @return the array of integer indices for the nearest keys to the given point. If the given
   *     point is out of range, then an array of size zero is returned.
   */
  public int[] getNearestKeysIndices(int x, int y) {
    return proximityGrid.getNearestKeysIndices(
        mKeys, getMinWidth(), getHeight(), mProximityThreshold, x, y);
  }

  @Nullable
  protected Row createRowFromXml(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Resources res,
      XmlResourceParser parser,
      @KeyboardRowModeId int rowMode) {
    Row row = new Row(resourceMapping, res, this, parser);
    if (row.isRowValidForMode(rowMode)) {
      return row;
    } else {
      return null;
    }
  }

  protected abstract Key createKeyFromXml(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context hostAppContext,
      Context keyboardContext,
      Row parent,
      KeyboardDimens keyboardDimens,
      int x,
      int y,
      XmlResourceParser parser);

  public void loadKeyboard(final KeyboardDimens keyboardDimens) {
    if (mKeyboardDimens != null) {
      Logger.wtf(TAG, "loadKeyboard should only be called once");
    }

    mKeyboardDimens = keyboardDimens;
    mDisplayWidth = keyboardDimens.getKeyboardMaxWidth();
    final float rowVerticalGap = keyboardDimens.getRowVerticalGap();
    final float keyHorizontalGap = keyboardDimens.getKeyHorizontalGap();

    mDefaultHorizontalGap = 0;
    mDefaultWidth = mDisplayWidth / 10;
    mDefaultHeightCode = -1;

    final Context addOnContext = mAddOn.getPackageContext();
    if (addOnContext == null) {
      Logger.wtf(TAG, "loadKeyboard was called but add-on Context addon!");
      return;
    }
    Resources res = addOnContext.getResources();
    try (final XmlResourceParser parser = res.getXml(mLayoutResId)) {
      KeyboardXmlLoader.loadKeyboard(
          this, keyboardDimens, addOnContext, res, parser, rowVerticalGap, keyHorizontalGap);
    }
  }

  void addKeyFromParser(@NonNull Key key) {
    mKeys.add(key);
    if (key.getPrimaryCode() == KeyCodes.SHIFT) {
      mShiftKey = key;
      mModifierKeys.add(key);
    } else if (key.getPrimaryCode() == KeyCodes.ALT) {
      mModifierKeys.add(key);
    }
  }

  void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
    KeyboardLayoutAttributes parsed =
        KeyboardLayoutAttributesParser.parse(
            mKeyboardResourceMap, res, parser, mDisplayWidth, showPreview, autoCap);
    mDefaultWidth = parsed.defaultWidth;
    mDefaultHeightCode = parsed.defaultHeightCode;
    mDefaultHorizontalGap = parsed.defaultHorizontalGap;
    mDefaultVerticalGap = parsed.defaultVerticalGap;
    showPreview = parsed.showPreview;
    autoCap = parsed.autoCap;

    mProximityThreshold = (int) (mDefaultWidth * SEARCH_DISTANCE);
    // Square it for comparison
    mProximityThreshold = mProximityThreshold * mProximityThreshold;
  }

  void setTotalDimensionsFromParser(int width, int height) {
    mTotalWidth = width;
    mTotalHeight = height;
  }

  static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
    TypedValue value = a.peekValue(index);
    if (value == null) {
      return defValue;
    }
    if (value.type == TypedValue.TYPE_DIMENSION) {
      return a.getDimensionPixelOffset(index, defValue);
    } else if (value.type == TypedValue.TYPE_FRACTION) {
      // Round it to avoid values like 47.9999 from getting truncated
      return Math.round(a.getFraction(index, base, base, defValue));
    }
    return defValue;
  }

  @NonNull
  AddOn.AddOnResourceMapping getKeyboardResourceMap() {
    return mKeyboardResourceMap;
  }
}
