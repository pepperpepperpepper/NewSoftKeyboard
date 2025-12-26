package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.res.TypedArray;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

/** Applies theme icon attributes to the {@link KeyIconResolver}. */
final class KeyIconAttributeSetter {

  private static final String TAG = "NSKKbdViewBase";

  private KeyIconAttributeSetter() {}

  static boolean apply(
      KeyboardTheme theme,
      TypedArray remoteTypeArray,
      final int localAttrId,
      final int remoteTypedArrayIndex,
      KeyIconResolver keyIconResolver) {
    final int keyCode =
        switch (localAttrId) {
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyShift -> KeyCodes.SHIFT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyControl -> KeyCodes.CTRL;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyAction -> KeyCodes.ENTER;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyBackspace -> KeyCodes.DELETE;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyCancel -> KeyCodes.CANCEL;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyGlobe -> KeyCodes.MODE_ALPHABET;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeySpace -> KeyCodes.SPACE;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyTab -> KeyCodes.TAB;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyArrowDown -> KeyCodes.ARROW_DOWN;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyArrowLeft -> KeyCodes.ARROW_LEFT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyArrowRight -> KeyCodes.ARROW_RIGHT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyArrowUp -> KeyCodes.ARROW_UP;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyInputMoveHome -> KeyCodes.MOVE_HOME;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyInputMoveEnd -> KeyCodes.MOVE_END;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyMic -> KeyCodes.VOICE_INPUT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeySettings -> KeyCodes.SETTINGS;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyCondenseNormal -> KeyCodes.MERGE_LAYOUT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyCondenseSplit -> KeyCodes.SPLIT_LAYOUT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyCondenseCompactToRight ->
              KeyCodes.COMPACT_LAYOUT_TO_RIGHT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyCondenseCompactToLeft ->
              KeyCodes.COMPACT_LAYOUT_TO_LEFT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyClipboardCopy -> KeyCodes.CLIPBOARD_COPY;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyClipboardCut -> KeyCodes.CLIPBOARD_CUT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyClipboardPaste -> KeyCodes.CLIPBOARD_PASTE;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyClipboardSelect ->
              KeyCodes.CLIPBOARD_SELECT_ALL;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyClipboardFineSelect ->
              KeyCodes.CLIPBOARD_SELECT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyQuickTextPopup -> KeyCodes.QUICK_TEXT_POPUP;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyQuickText -> KeyCodes.QUICK_TEXT;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyUndo -> KeyCodes.UNDO;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyRedo -> KeyCodes.REDO;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyForwardDelete -> KeyCodes.FORWARD_DELETE;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyImageInsert -> KeyCodes.IMAGE_MEDIA_POPUP;
          case wtf.uhoh.newsoftkeyboard.R.attr.iconKeyClearQuickTextHistory ->
              KeyCodes.CLEAR_QUICK_TEXT_HISTORY;
          default -> 0;
        };
    if (keyCode == 0) {
      if (BuildConfig.DEBUG) {
        throw new IllegalArgumentException(
            "No valid keycode for attr " + remoteTypeArray.getResourceId(remoteTypedArrayIndex, 0));
      }
      Logger.w(
          TAG,
          "No valid keycode for attr %d",
          remoteTypeArray.getResourceId(remoteTypedArrayIndex, 0));
      return false;
    } else {
      keyIconResolver.putIconBuilder(
          keyCode, DrawableBuilder.build(theme, remoteTypeArray, remoteTypedArrayIndex));
      return true;
    }
  }
}
