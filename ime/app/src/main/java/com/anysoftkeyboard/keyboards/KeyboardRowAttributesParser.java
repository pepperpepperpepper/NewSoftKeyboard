package com.anysoftkeyboard.keyboards;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Xml;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.base.utils.Logger;
import com.menny.android.anysoftkeyboard.R;

final class KeyboardRowAttributesParser {

  private static final String TAG = "NSKKbd";

  static KeyboardRowAttributes parse(
      AddOn.AddOnResourceMapping resourceMap,
      Resources res,
      XmlResourceParser parser,
      int displayWidth,
      int defaultWidth,
      int defaultHeightCode,
      int defaultHorizontalGap,
      int defaultVerticalGap) {
    int parsedDefaultWidth = defaultWidth;
    int parsedDefaultHeightCode = defaultHeightCode;
    int parsedDefaultHorizontalGap = defaultHorizontalGap;
    int verticalGap = defaultVerticalGap;
    int rowEdgeFlags = 0;
    int mode = Keyboard.KEYBOARD_ROW_MODE_NONE;

    int[] remoteKeyboardLayoutStyleable =
        resourceMap.getRemoteStyleableArrayFromLocal(R.styleable.KeyboardLayout);
    TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), remoteKeyboardLayoutStyleable);
    int n = a.getIndexCount();
    for (int i = 0; i < n; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          resourceMap.getLocalAttrId(remoteKeyboardLayoutStyleable[remoteIndex]);

      try {
        switch (localAttrId) {
          case android.R.attr.keyWidth:
            parsedDefaultWidth =
                Keyboard.getDimensionOrFraction(a, remoteIndex, displayWidth, defaultWidth);
            break;
          case android.R.attr.keyHeight:
            parsedDefaultHeightCode = Keyboard.getKeyHeightCode(a, remoteIndex, defaultHeightCode);
            break;
          case android.R.attr.horizontalGap:
            parsedDefaultHorizontalGap =
                Keyboard.getDimensionOrFraction(a, remoteIndex, displayWidth, defaultHorizontalGap);
            break;
          case android.R.attr.verticalGap:
            verticalGap =
                Keyboard.getDimensionOrFraction(a, remoteIndex, displayWidth, verticalGap);
            break;
        }
      } catch (Exception e) {
        Logger.w(TAG, "Failed to set data from XML!", e);
      }
    }
    a.recycle();

    int[] remoteKeyboardRowLayoutStyleable =
        resourceMap.getRemoteStyleableArrayFromLocal(R.styleable.KeyboardLayout_Row);
    a = res.obtainAttributes(Xml.asAttributeSet(parser), remoteKeyboardRowLayoutStyleable);
    n = a.getIndexCount();
    for (int i = 0; i < n; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          resourceMap.getLocalAttrId(remoteKeyboardRowLayoutStyleable[remoteIndex]);

      try {
        switch (localAttrId) {
          case android.R.attr.rowEdgeFlags:
            rowEdgeFlags = a.getInt(remoteIndex, 0);
            break;
          case android.R.attr.keyboardMode:
            final int modeResource = a.getResourceId(remoteIndex, 0);
            if (modeResource != 0) {
              mode = res.getInteger(modeResource);
            } else {
              mode = Keyboard.KEYBOARD_ROW_MODE_NONE;
            }
            break;
        }
      } catch (Exception e) {
        Logger.w(TAG, "Failed to set data from XML!", e);
      }
    }
    a.recycle();

    return new KeyboardRowAttributes(
        parsedDefaultWidth,
        parsedDefaultHeightCode,
        parsedDefaultHorizontalGap,
        verticalGap,
        rowEdgeFlags,
        mode);
  }
}
