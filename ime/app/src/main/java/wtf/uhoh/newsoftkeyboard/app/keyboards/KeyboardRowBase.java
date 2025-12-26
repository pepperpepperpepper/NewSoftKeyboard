package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;

/**
 * Implementation for {@link Keyboard.Row}.
 *
 * <p>This lives in a separate file to keep {@link Keyboard} focused and to make further refactors
 * easier, while preserving the externally-visible {@code Keyboard.Row} type.
 */
class KeyboardRowBase {
  /** Default width of a key in this row. */
  public int defaultWidth;

  /** Default height of a key in this row. */
  public int defaultHeightCode;

  /** Default horizontal gap between keys in this row. */
  public int defaultHorizontalGap;

  /**
   * Vertical gap following this row. NOTE: Usually we use the theme's value. This is an override.
   */
  public int verticalGap;

  /**
   * Edge flags for this row of keys. Possible values that can be assigned are {@link
   * Keyboard#EDGE_TOP Keyboard.EDGE_TOP} and {@link Keyboard#EDGE_BOTTOM Keyboard.EDGE_BOTTOM}
   */
  @Keyboard.KeyEdgeValue public int rowEdgeFlags;

  /** The keyboard mode for this row */
  @Keyboard.KeyboardRowModeId public int mode = Keyboard.KEYBOARD_ROW_MODE_NONE;

  protected Keyboard mParent;

  KeyboardRowBase(Keyboard parent) {
    mParent = parent;

    defaultWidth = parent.getDefaultWidth();
    defaultHeightCode = parent.getDefaultHeightCode();

    defaultHorizontalGap = parent.getDefaultHorizontalGap();
    verticalGap = parent.getVerticalGap();

    rowEdgeFlags = Keyboard.EDGE_TOP + Keyboard.EDGE_BOTTOM;
    mode = parent.mKeyboardMode;
  }

  KeyboardRowBase(
      @NonNull final AddOn.AddOnResourceMapping resourceMap,
      Resources res,
      Keyboard parent,
      XmlResourceParser parser) {
    mParent = parent;
    // some defaults
    defaultWidth = parent.getDefaultWidth();
    defaultHeightCode = parent.getDefaultHeightCode();
    defaultHorizontalGap = parent.getDefaultHorizontalGap();
    verticalGap = parent.getVerticalGap();

    final KeyboardRowAttributes parsed =
        KeyboardRowAttributesParser.parse(
            resourceMap,
            res,
            parser,
            parent.mDisplayWidth,
            parent.getDefaultWidth(),
            parent.getDefaultHeightCode(),
            parent.getDefaultHorizontalGap(),
            verticalGap);
    defaultWidth = parsed.defaultWidth;
    defaultHeightCode = parsed.defaultHeightCode;
    defaultHorizontalGap = parsed.defaultHorizontalGap;
    verticalGap = parsed.verticalGap;
    rowEdgeFlags = parsed.rowEdgeFlags;
    //noinspection WrongConstant
    mode = parsed.mode;
  }

  public boolean isRowValidForMode(@Keyboard.KeyboardRowModeId int keyboardRowId) {
    return (mode == Keyboard.KEYBOARD_ROW_MODE_NONE || mode == keyboardRowId);
  }
}
