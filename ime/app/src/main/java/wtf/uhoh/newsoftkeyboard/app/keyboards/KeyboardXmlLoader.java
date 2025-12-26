package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import androidx.annotation.NonNull;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

final class KeyboardXmlLoader {

  private static final String TAG = "NSKKbd";

  private static final String TAG_KEYBOARD = "Keyboard";
  private static final String TAG_ROW = "Row";
  private static final String TAG_KEY = "Key";

  static void loadKeyboard(
      @NonNull Keyboard keyboard,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull Context addOnContext,
      @NonNull Resources resources,
      @NonNull XmlResourceParser parser,
      float rowVerticalGap,
      float keyHorizontalGap) {
    boolean inKey = false;
    boolean inRow = false;
    boolean inUnknown = false;
    float x = 0;
    float y = rowVerticalGap; // starts with a gap
    int rowHeight = 0;
    Keyboard.Key key = null;
    Keyboard.Row currentRow = null;
    float lastVerticalGap = rowVerticalGap;
    int totalWidth = 0;

    try {
      int event;
      while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
        if (event == XmlResourceParser.START_TAG) {
          String tag = parser.getName();
          if (TAG_ROW.equals(tag)) {
            inRow = true;
            x = 0;
            rowHeight = 0;
            currentRow =
                keyboard.createRowFromXml(
                    keyboard.getKeyboardResourceMap(), resources, parser, keyboard.mKeyboardMode);
            if (currentRow == null) {
              skipToEndOfRow(parser);
              inRow = false;
            }
          } else if (TAG_KEY.equals(tag)) {
            inKey = true;
            x += (keyHorizontalGap / 2);
            key =
                keyboard.createKeyFromXml(
                    keyboard.getKeyboardResourceMap(),
                    keyboard.mLocalContext,
                    addOnContext,
                    currentRow,
                    keyboardDimens,
                    (int) x,
                    (int) y,
                    parser);
            rowHeight = Math.max(rowHeight, key.height);
            key.width = (int) (key.width - keyHorizontalGap); // the gap is on both sides
            keyboard.addKeyFromParser(key);
          } else if (TAG_KEYBOARD.equals(tag)) {
            keyboard.parseKeyboardAttributes(resources, parser);
          } else {
            inUnknown = true;
            Logger.w(TAG, "Unknown tag '%s' while parsing mKeyboard!", tag);
          }
        } else if (event == XmlResourceParser.END_TAG) {
          if (inKey) {
            inKey = false;
            x += key.gap + key.width;
            x += (keyHorizontalGap / 2);
            if (x > totalWidth) {
              totalWidth = (int) x;
            }
          } else if (inRow) {
            inRow = false;
            if (currentRow.verticalGap >= 0) lastVerticalGap = currentRow.verticalGap;
            else lastVerticalGap = rowVerticalGap;
            y += lastVerticalGap;
            y += rowHeight;
          } else if (inUnknown) {
            inUnknown = false;
          }
        }
      }
    } catch (XmlPullParserException e) {
      Logger.e(TAG, e, "Parse error: %s", e.getMessage());
    } catch (IOException e) {
      Logger.e(TAG, e, "Read error: %s", e.getMessage());
    }

    keyboard.setTotalDimensionsFromParser(totalWidth, (int) (y - lastVerticalGap));
  }

  private static void skipToEndOfRow(XmlResourceParser parser)
      throws XmlPullParserException, IOException {
    int event;
    while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
      if (event == XmlResourceParser.END_TAG && parser.getName().equals(TAG_ROW)) {
        break;
      }
    }
  }
}
