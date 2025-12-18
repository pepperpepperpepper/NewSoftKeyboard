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

package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.CallSuper;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.BTreeDictionary;
import com.anysoftkeyboard.ime.AnySoftKeyboardBase;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.utils.Workarounds;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.List;
import java.util.Locale;

public abstract class AnyKeyboard extends Keyboard {
  private static final String TAG = "NSKAnyKeyboard";
  static final int[] EMPTY_INT_ARRAY = new int[0];
  private final KeyboardModifierStates modifierStates = new KeyboardModifierStates();
  private Key mShiftKey;
  private Key mControlKey;
  private AnyKey mAltKey;
  private AnyKey mFunctionKey;
  private AnyKey mVoiceKey;
  private EnterKey mEnterKey;
  private boolean mRightToLeftLayout = false; // the "super" ctor will create
  private boolean mTopRowWasCreated;
  private boolean mBottomRowWasCreated;

  private int mGenericRowsHeight = 0;
  // max(generic row widths)
  private int mMaxGenericRowsWidth = 0;

  private KeyboardCondenser mKeyboardCondenser;

  // for popup keyboard
  // note: the context can be from a different package!
  protected AnyKeyboard(
      @NonNull AddOn keyboardAddOn,
      @NonNull Context askContext,
      @NonNull Context context,
      int xmlLayoutResId) {
    // should use the package context for creating the layout
    super(keyboardAddOn, askContext, xmlLayoutResId, KEYBOARD_ROW_MODE_NORMAL);
    // no generic rows in popup
  }

  // for the External
  // note: the context can be from a different package!
  protected AnyKeyboard(
      @NonNull AddOn keyboardAddOn,
      @NonNull Context askContext,
      int xmlLayoutResId,
      @KeyboardRowModeId int mode) {
    // should use the package context for creating the layout
    super(keyboardAddOn, askContext, xmlLayoutResId, mode);
  }

  @Override
  public void loadKeyboard(final KeyboardDimens keyboardDimens) {
    final KeyboardExtension topRowPlugin =
        AnyApplication.getTopRowFactory(mLocalContext).getEnabledAddOn();
    final KeyboardExtension bottomRowPlugin =
        AnyApplication.getBottomRowFactory(mLocalContext).getEnabledAddOn();

    loadKeyboard(keyboardDimens, topRowPlugin, bottomRowPlugin);
  }

  public void loadKeyboard(
      final KeyboardDimens keyboardDimens,
      @NonNull KeyboardExtension topRowPlugin,
      @NonNull KeyboardExtension bottomRowPlugin) {
    mShiftKey = null;
    mControlKey = null;
    mAltKey = null;
    mFunctionKey = null;
    modifierStates.resetAltAndFunction();
    super.loadKeyboard(keyboardDimens);

    addGenericRows(keyboardDimens, topRowPlugin, bottomRowPlugin);
    mRightToLeftLayout =
        KeyMembersInitializer.initKeysMembers(
            mLocalContext, mLocalContext, getKeys(), keyboardDimens, mRightToLeftLayout);
    mKeyboardCondenser = new KeyboardCondenser(mLocalContext, this);
    KeyEdgeFlagsFixer.fixEdgeFlags(getKeys());
  }

  public void onKeyboardViewWidthChanged(int newWidth, int oldWidth) {
    if (oldWidth == 0) oldWidth = mDisplayWidth;
    mDisplayWidth = newWidth;
    final double zoomFactor = ((double) newWidth) / ((double) oldWidth);
    for (Key key : getKeys()) {
      key.width = (int) (zoomFactor * key.width);
      key.x = (int) (zoomFactor * key.x);
    }
  }

  protected void addGenericRows(
      @NonNull final KeyboardDimens keyboardDimens,
      @NonNull KeyboardExtension topRowPlugin,
      @NonNull KeyboardExtension bottomRowPlugin) {
    final boolean disallowGenericRowsOverride =
        KeyboardPrefs.disallowGenericRowOverride(mLocalContext);
    mGenericRowsHeight = 0;
    if (!mTopRowWasCreated || disallowGenericRowsOverride) {
      Logger.d(TAG, "Top row layout id %s", topRowPlugin.getId());
      GenericRowKeyboard rowKeyboard =
          new GenericRowKeyboard(
              topRowPlugin,
              mLocalContext,
              getKeyboardDimens(),
              isAlphabetKeyboard(),
              mKeyboardMode);
      fixKeyboardDueToGenericRow(rowKeyboard, true);
    }
    if (!mBottomRowWasCreated || disallowGenericRowsOverride) {
      Logger.d(TAG, "Bottom row layout id %s", bottomRowPlugin.getId());
      GenericRowKeyboard rowKeyboard =
          new GenericRowKeyboard(
              bottomRowPlugin,
              mLocalContext,
              getKeyboardDimens(),
              isAlphabetKeyboard(),
              mKeyboardMode);
      if (rowKeyboard.hasNoKeys()) {
        Logger.i(
            TAG,
            "Could not find any rows that match mode %d. Trying again with normal" + " mode.",
            mKeyboardMode);
        rowKeyboard =
            new GenericRowKeyboard(
                bottomRowPlugin,
                mLocalContext,
                getKeyboardDimens(),
                isAlphabetKeyboard(),
                KEYBOARD_ROW_MODE_NORMAL);
      }
      fixKeyboardDueToGenericRow(rowKeyboard, false);
    }
  }

  public abstract boolean isAlphabetKeyboard();

  private void fixKeyboardDueToGenericRow(
      @NonNull GenericRowKeyboard genericRowKeyboard, final boolean isTopRow) {
    final int genericRowsHeight = genericRowKeyboard.getHeight();
    final List<Key> keys = getKeys();
    final int rowKeyInsertIndex = isTopRow ? 0 : keys.size();
    final int rowKeyYOffset = isTopRow ? 0 : getHeight();
    final GenericRowApplier.Result result =
        GenericRowApplier.applyGenericRow(
            keys,
            genericRowKeyboard.getKeys(),
            genericRowsHeight,
            isTopRow,
            rowKeyYOffset,
            rowKeyInsertIndex,
            mMaxGenericRowsWidth);
    mMaxGenericRowsWidth = result.maxGenericRowsWidth;
    if (result.controlKey != null) mControlKey = result.controlKey;
    if (result.altKey != null) mAltKey = result.altKey;
    if (result.functionKey != null) mFunctionKey = result.functionKey;
    if (result.voiceKey != null) mVoiceKey = result.voiceKey;

    mGenericRowsHeight += genericRowsHeight;
  }

  @Override
  public int getHeight() {
    return super.getHeight() + mGenericRowsHeight;
  }

  // minWidth is actually 'total width', see android framework source code
  @Override
  public int getMinWidth() {
    return Math.max(mMaxGenericRowsWidth, super.getMinWidth());
  }

  public abstract String getDefaultDictionaryLocale();

  @NonNull
  public Locale getLocale() {
    return Locale.ROOT;
  }

  // this function is called from within the super constructor.
@Override
  protected Key createKeyFromXml(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context askContext,
      Context keyboardContext,
      Row parent,
      KeyboardDimens keyboardDimens,
      int x,
      int y,
      XmlResourceParser parser) {
    AnyKey key = new AnyKey(resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);

    if (key.mCodes.length > 0) {
      final int primaryCode = key.mCodes[0];

      // creating less sensitive keys if required
      switch (primaryCode) {
        case KeyCodes.DELETE:
        case KeyCodes.FORWARD_DELETE:
        case KeyCodes.MODE_ALPHABET:
        case KeyCodes.KEYBOARD_MODE_CHANGE:
        case KeyCodes.KEYBOARD_CYCLE:
        case KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE:
        case KeyCodes.KEYBOARD_REVERSE_CYCLE:
        case KeyCodes.ALT:
        case KeyCodes.MODE_SYMBOLS:
        case KeyCodes.QUICK_TEXT:
        case KeyCodes.DOMAIN:
        case KeyCodes.CANCEL:
        case KeyCodes.CTRL:
          mControlKey = key;
          break;
        case KeyCodes.ALT_MODIFIER:
          mAltKey = key;
          break;
        case KeyCodes.SHIFT:
          mShiftKey = key; // I want to reference used by than super.
          break;
        case KeyCodes.FUNCTION:
          mFunctionKey = key;
          break;
        case KeyCodes.VOICE_INPUT:
          key =
              mVoiceKey =
                  new VoiceKey(
                      resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);
          break;
      }

      if (primaryCode == KeyCodes.DELETE) {
        AnyKey anyKey = key;
        if (anyKey.longPressCode == 0) {
          anyKey.longPressCode = KeyCodes.DELETE_WORD;
        }
      }

      // detecting LTR languages
      if (!mRightToLeftLayout
          && primaryCode >= 0
          && Workarounds.isRightToLeftCharacter((char) primaryCode)) {
        mRightToLeftLayout = true; // one is enough
      }
      switch (primaryCode) {
        case KeyCodes.QUICK_TEXT:
          AnyKey anyKey = key;
          if (anyKey.longPressCode == 0
              && anyKey.popupResId == 0
              && TextUtils.isEmpty(anyKey.popupCharacters)) {
            anyKey.longPressCode = KeyCodes.QUICK_TEXT_POPUP;
          }
          break;
        case KeyCodes.DOMAIN:
          key.text = key.label = KeyboardPrefs.getDefaultDomain(askContext);
          key.popupResId = R.xml.popup_domains;
          break;

        default:
          // setting the character label
          if (isAlphabetKey(key) && (key.icon == null)) {
            final boolean labelIsOriginallyEmpty = TextUtils.isEmpty(key.label);
            if (labelIsOriginallyEmpty) {
              final int code = key.mCodes[0];
              // check the ASCII table, everything below 32,
              // is not printable
              if (code > 31 && !Character.isWhitespace(code)) {
                key.label = new String(new int[] {code}, 0, 1);
              }
            }
          }
      }
    }

    setupKeyAfterCreation(key);

    return key;
  }

  @Override
  @Nullable
  protected Row createRowFromXml(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Resources res,
      XmlResourceParser parser,
      @KeyboardRowModeId int rowMode) {
    Row aRow = super.createRowFromXml(resourceMapping, res, parser, rowMode);
    if (aRow != null) {
      if ((aRow.rowEdgeFlags & Keyboard.EDGE_TOP) != 0) {
        mTopRowWasCreated = true;
      }
      if ((aRow.rowEdgeFlags & Keyboard.EDGE_BOTTOM) != 0) {
        mBottomRowWasCreated = true;
      }
    }

    return aRow;
  }

  private boolean isAlphabetKey(Key key) {
    return !key.repeatable && key.getPrimaryCode() > 0;
  }

  public boolean isStartOfWordLetter(int keyValue) {
    return Character.isLetter(keyValue);
  }

  public boolean isInnerWordLetter(int keyValue) {
    return isStartOfWordLetter(keyValue)
        || keyValue == BTreeDictionary.QUOTE
        || keyValue == BTreeDictionary.CURLY_QUOTE
        || Character.getType(keyValue) == Character.NON_SPACING_MARK
        || Character.getType(keyValue) == Character.COMBINING_SPACING_MARK;
  }

  public abstract char[] getSentenceSeparators();

  /**
   * This looks at the ime options given by the current editor, to set the appropriate label on the
   * keyboard's enter key (if it has one).
   */
  public void setImeOptions(Resources res, EditorInfo editor) {
    if (mEnterKey == null) {
      return;
    }

    mEnterKey.enable();
  }

  @NonNull
  public abstract CharSequence getKeyboardName();

  public boolean isLeftToRightLanguage() {
    return !mRightToLeftLayout;
  }

  @DrawableRes
  public abstract int getKeyboardIconResId();

  public boolean setShiftLocked(boolean shiftLocked) {
    if (keyboardSupportShift()) {
      return modifierStates.setShiftLocked(shiftLocked);
    }

    return false;
  }

  @Override
  public boolean isShifted() {
    if (keyboardSupportShift()) {
      return modifierStates.isShifted();
    } else {
      return false;
    }
  }

  @Override
  public boolean setShifted(boolean shiftState) {
    if (keyboardSupportShift()) {
      return modifierStates.setShifted(shiftState);
    } else {
      super.setShifted(shiftState);
      return false;
    }
  }

  public boolean keyboardSupportShift() {
    return mShiftKey != null;
  }

  public boolean isShiftLocked() {
    return modifierStates.isShiftLocked();
  }

  public boolean isControl() {
    if (mControlKey != null) {
      return modifierStates.isControl();
    } else {
      return false;
    }
  }

  public boolean setControl(boolean control) {
    if (mControlKey != null) {
      return modifierStates.setControl(control);
    } else {
      return false;
    }
  }

  public boolean isControlActive() {
    return mControlKey != null && modifierStates.isControlActive();
  }

  public boolean setAlt(boolean active, boolean locked) {
    if (mAltKey != null) {
      return modifierStates.setAlt(active, locked);
    }
    return false;
  }

  public boolean isAltActive() {
    return mAltKey != null && modifierStates.isAltActive();
  }

  public boolean isAltLocked() {
    return mAltKey != null && modifierStates.isAltLocked();
  }

  public boolean setFunction(boolean active, boolean locked) {
    if (mFunctionKey != null) {
      return modifierStates.setFunction(active, locked);
    }
    return false;
  }

  public boolean isFunctionActive() {
    return mFunctionKey != null && modifierStates.isFunctionActive();
  }

  public boolean isFunctionLocked() {
    return mFunctionKey != null && modifierStates.isFunctionLocked();
  }

  public boolean setVoice(boolean active, boolean locked) {
    if (mVoiceKey == null) return false;

    final boolean stateChanged = modifierStates.setVoice(active, locked);

    if (mVoiceKey instanceof VoiceKey voiceKey) {
      voiceKey.setVoiceActive(active);
    }

    return stateChanged;
  }

  public boolean isVoiceActive() {
    return modifierStates.isVoiceActive();
  }

  public boolean isVoiceLocked() {
    return modifierStates.isVoiceLocked();
  }

  @CallSuper
  protected boolean setupKeyAfterCreation(AnyKey key) {
    // if the keyboard XML already specified the popup, then no
    // need to override
    if (key.popupResId != 0) {
      return true;
    }

    // filling popup res for external keyboards
    if (key.popupCharacters != null) {
      if (key.popupCharacters.length() > 0) {
        key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
      }
      return true;
    }

    return false;
  }

  @NonNull
  public abstract String getKeyboardId();

  @KeyboardRowModeId
  public int getKeyboardMode() {
    return mKeyboardMode;
  }

  public void setCondensedKeys(CondenseType condenseType) {
    if (mKeyboardCondenser.setCondensedKeys(condenseType, getKeyboardDimens())) {
      computeNearestNeighbors(); // keyboard has changed, so we need to recompute the
      // neighbors.
    }
  }

  public interface HardKeyboardAction {
    int getKeyCode();

    boolean isAltActive();

    boolean isShiftActive();

    void setNewKeyCode(int keyCode);
  }

  public interface HardKeyboardTranslator {
    /*
     * Gets the current state of the hard keyboard, and may change the
     * output key-code.
     */
    void translatePhysicalCharacter(
        HardKeyboardAction action, AnySoftKeyboardBase ime, int multiTapTimeout);
  }

  public static class AnyKey extends AnyKeyboardKey {

    public AnyKey(Row row, KeyboardDimens keyboardDimens) {
      super(row, keyboardDimens);
    }

    public AnyKey(
        @NonNull AddOn.AddOnResourceMapping resourceMapping,
        Context keyboardContext,
        Keyboard.Row parent,
        KeyboardDimens keyboardDimens,
        int x,
        int y,
        XmlResourceParser parser) {
      super(resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);
    }
  }
}
