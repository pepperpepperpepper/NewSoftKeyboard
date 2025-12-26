package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Xml;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

final class KeyboardLayoutAttributesParser {

  private static final String TAG = "NSKKbd";

  static KeyboardLayoutAttributes parse(
      AddOn.AddOnResourceMapping addOnResourceMapping,
      Resources resources,
      XmlResourceParser parser,
      int displayWidth,
      boolean defaultShowPreview,
      boolean defaultAutoCap) {
    int defaultWidth = displayWidth / 10;
    int defaultHeightCode = -1;
    int defaultHorizontalGap = 0;
    int defaultVerticalGap = -1;
    boolean showPreview = defaultShowPreview;
    boolean autoCap = defaultAutoCap;

    int[] remoteKeyboardLayoutStyleable =
        addOnResourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.KeyboardLayout);
    TypedArray a =
        resources.obtainAttributes(Xml.asAttributeSet(parser), remoteKeyboardLayoutStyleable);

    int n = a.getIndexCount();
    for (int i = 0; i < n; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          addOnResourceMapping.getLocalAttrId(remoteKeyboardLayoutStyleable[remoteIndex]);

      try {
        switch (localAttrId) {
          case android.R.attr.keyWidth:
            defaultWidth =
                Keyboard.getDimensionOrFraction(a, remoteIndex, displayWidth, displayWidth / 10);
            break;
          case android.R.attr.keyHeight:
            defaultHeightCode = Keyboard.getKeyHeightCode(a, remoteIndex, -1);
            break;
          case android.R.attr.horizontalGap:
            defaultHorizontalGap = Keyboard.getDimensionOrFraction(a, remoteIndex, displayWidth, 0);
            break;
          case R.attr.showPreview:
            showPreview = a.getBoolean(remoteIndex, true /*showing preview by default*/);
            break;
          case android.R.attr.verticalGap:
            defaultVerticalGap =
                Keyboard.getDimensionOrFraction(a, remoteIndex, displayWidth, defaultVerticalGap);
            break;
          case R.attr.autoCap:
            autoCap = a.getBoolean(remoteIndex, true /*auto caps by default*/);
            break;
        }
      } catch (Exception e) {
        Logger.w(TAG, "Failed to set data from XML!", e);
      }
    }
    a.recycle();

    return new KeyboardLayoutAttributes(
        defaultWidth,
        defaultHeightCode,
        defaultHorizontalGap,
        defaultVerticalGap,
        showPreview,
        autoCap);
  }
}
