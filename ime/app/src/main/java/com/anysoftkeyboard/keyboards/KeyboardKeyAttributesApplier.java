package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Xml;
import com.anysoftkeyboard.addons.AddOn;
import com.menny.android.anysoftkeyboard.R;

final class KeyboardKeyAttributesApplier {

  private KeyboardKeyAttributesApplier() {}

  static void applyAllFromXml(
      KeyboardKeyBase key,
      Context keyboardContext,
      AddOn.AddOnResourceMapping resourceMapping,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      XmlResourceParser parser) {
    int[] remoteKeyboardLayoutStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.KeyboardLayout);
    TypedArray a =
        keyboardContext.obtainStyledAttributes(
            Xml.asAttributeSet(parser), remoteKeyboardLayoutStyleable);
    int n = a.getIndexCount();
    for (int i = 0; i < n; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          resourceMapping.getLocalAttrId(remoteKeyboardLayoutStyleable[remoteIndex]);
      KeyboardKeyAttributesApplier.apply(key, parent, keyboardDimens, a, remoteIndex, localAttrId);
    }
    a.recycle();
    key.x += key.gap;

    int[] remoteKeyboardKeyLayoutStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.KeyboardLayout_Key);
    a =
        keyboardContext.obtainStyledAttributes(
            Xml.asAttributeSet(parser), remoteKeyboardKeyLayoutStyleable);
    n = a.getIndexCount();
    for (int i = 0; i < n; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          resourceMapping.getLocalAttrId(remoteKeyboardKeyLayoutStyleable[remoteIndex]);

      KeyboardKeyAttributesApplier.apply(key, parent, keyboardDimens, a, remoteIndex, localAttrId);
    }
    a.recycle();
  }

  static void apply(
      KeyboardKeyBase key,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      TypedArray a,
      int remoteIndex,
      int localAttrId) {
    final Keyboard keyboard = parent.mParent;
    switch (localAttrId) {
      case android.R.attr.keyWidth:
        key.width =
            Keyboard.getDimensionOrFraction(
                a, remoteIndex, keyboard.mDisplayWidth, parent.defaultWidth);
        break;
      case android.R.attr.keyHeight:
        int heightCode = Keyboard.getKeyHeightCode(a, remoteIndex, parent.defaultHeightCode);
        key.height =
            KeyboardSupport.getKeyHeightFromHeightCode(
                keyboardDimens, heightCode, keyboard.mKeysHeightFactor);
        break;
      case android.R.attr.horizontalGap:
        key.gap =
            Keyboard.getDimensionOrFraction(
                a, remoteIndex, keyboard.mDisplayWidth, parent.defaultHorizontalGap);
        break;
      case android.R.attr.codes:
        key.mCodes = KeyboardSupport.getKeyCodesFromTypedArray(a, remoteIndex);
        break;
      case android.R.attr.iconPreview:
        key.iconPreview = a.getDrawable(remoteIndex);
        KeyboardSupport.updateDrawableBounds(key.iconPreview);
        break;
      case android.R.attr.popupCharacters:
        key.popupCharacters = a.getText(remoteIndex);
        break;
      case android.R.attr.popupKeyboard:
        key.popupResId = a.getResourceId(remoteIndex, 0);
        break;
      case android.R.attr.isRepeatable:
        key.repeatable = a.getBoolean(remoteIndex, false);
        break;
      case R.attr.showPreview:
        key.showPreview = a.getBoolean(remoteIndex, keyboard.showPreview);
        break;
      case R.attr.keyDynamicEmblem:
        key.dynamicEmblem = a.getInt(remoteIndex, Keyboard.KEY_EMBLEM_NONE);
        break;
      case android.R.attr.isModifier:
        key.modifier = a.getBoolean(remoteIndex, false);
        break;
      case android.R.attr.keyEdgeFlags:
        key.edgeFlags = a.getInt(remoteIndex, 0);
        key.edgeFlags |= parent.rowEdgeFlags;
        break;
      case android.R.attr.keyIcon:
        key.icon = a.getDrawable(remoteIndex);
        KeyboardSupport.updateDrawableBounds(key.icon);
        break;
      case android.R.attr.keyLabel:
        key.label = a.getText(remoteIndex);
        break;
      case android.R.attr.keyOutputText:
        key.text = a.getText(remoteIndex);
        break;
      case R.attr.shiftedKeyOutputText:
        key.shiftedText = a.getText(remoteIndex);
        break;
      case R.attr.keyOutputTyping:
        key.typedText = a.getText(remoteIndex);
        break;
      case R.attr.shiftedKeyOutputTyping:
        key.shiftedTypedText = a.getText(remoteIndex);
        break;
    }
  }
}
