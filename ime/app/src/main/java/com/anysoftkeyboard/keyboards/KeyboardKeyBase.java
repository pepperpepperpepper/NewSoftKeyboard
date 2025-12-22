package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;

/**
 * Implementation for {@link Keyboard.Key}.
 *
 * <p>This lives in a separate file to keep {@link Keyboard} focused and to make further refactors
 * easier, while preserving the externally-visible {@code Keyboard.Key} type.
 */
abstract class KeyboardKeyBase {
  /**
   * All the key codes (unicode or custom code) that this key could generate, zero'th being the most
   * important.
   */
  @NonNull protected int[] mCodes = new int[0];

  /** Label to display */
  public CharSequence label;

  /** Icon to display instead of a label. Icon takes precedence over a label */
  public Drawable icon;

  /** Preview version of the icon, for the preview popup */
  public Drawable iconPreview;

  /** Width of the key, not including the gap */
  public int width;

  /** Height of the key, not including the gap */
  public int height;

  /** The horizontal gap before this key */
  public int gap;

  /** X coordinate of the key in the keyboard layout */
  public int x;

  public static int getCenterX(@NonNull final Keyboard.Key k) {
    return k.x + k.width / 2;
  }

  public static int getEndX(@NonNull final Keyboard.Key k) {
    return k.x + k.width;
  }

  /** Y coordinate of the key in the keyboard layout */
  public int y;

  public static int getCenterY(@NonNull final Keyboard.Key k) {
    return k.y + k.height / 2;
  }

  public static int getEndY(@NonNull final Keyboard.Key k) {
    return k.y + k.height;
  }

  /** The current pressed state of this key */
  public boolean pressed;

  /** Text to output when pressed. This can be multiple characters, like ".com" */
  public CharSequence text;

  /** Text to output when pressed and shifted. This can be multiple characters, like ".com" */
  public CharSequence shiftedText;

  /** Text to output (as typed) when pressed. */
  public CharSequence typedText;

  /** Text to output (as typed) when pressed. and shifted. */
  public CharSequence shiftedTypedText;

  /** Popup characters */
  public CharSequence popupCharacters;

  /**
   * Flags that specify the anchoring to edges of the keyboard for detecting touch events that are
   * just out of the boundary of the key. This is a bit mask of {@link Keyboard#EDGE_LEFT}, {@link
   * Keyboard#EDGE_RIGHT}, {@link Keyboard#EDGE_TOP} and {@link Keyboard#EDGE_BOTTOM}.
   */
  @Keyboard.KeyEdgeValue public int edgeFlags;

  /** Whether this is a modifier key, such as Shift or Alt */
  public boolean modifier;

  /** The keyboard that this key belongs to */
  private Keyboard mKeyboard;

  public final Keyboard.Row row;

  /**
   * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
   * keyboard.
   */
  public int popupResId;

  public boolean externalResourcePopupLayout = false;

  /** Whether this key repeats itself when held down */
  public boolean repeatable;

  /** Whether this key should show previewPopup */
  public boolean showPreview;

  public int dynamicEmblem;

  /** Create an empty key with no attributes. */
  protected KeyboardKeyBase(Keyboard.Row parent, KeyboardDimens keyboardDimens) {
    row = parent;
    mKeyboard = parent.mParent;
    height =
        KeyboardSupport.getKeyHeightFromHeightCode(
            keyboardDimens, parent.defaultHeightCode, parent.mParent.mKeysHeightFactor);
    width = parent.defaultWidth;
    gap = parent.defaultHorizontalGap;
    edgeFlags = parent.rowEdgeFlags;
    showPreview = mKeyboard.showPreview;
  }

  /**
   * Create a key with the given top-left coordinate and extract its attributes from the XML parser.
   *
   * @param parent the row that this key belongs to. The row must already be attached to a {@link
   *     Keyboard}.
   * @param initialX the x coordinate of the top-left
   * @param initialY the y coordinate of the top-left
   * @param parser the XML parser containing the attributes for this key
   */
  @SuppressWarnings("this-escape")
  protected KeyboardKeyBase(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context keyboardContext,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      int initialX,
      int initialY,
      XmlResourceParser parser) {
    this(parent, keyboardDimens);
    x = initialX;
    y = initialY;

    // setting up some defaults
    width = parent.defaultWidth;
    height =
        KeyboardSupport.getKeyHeightFromHeightCode(
            keyboardDimens, parent.defaultHeightCode, parent.mParent.mKeysHeightFactor);
    gap = parent.defaultHorizontalGap;
    mCodes = new int[0];
    iconPreview = null;
    popupCharacters = null;
    popupResId = 0;
    repeatable = false;
    dynamicEmblem = Keyboard.KEY_EMBLEM_NONE;
    modifier = false;

    KeyboardKeyAttributesApplier.applyAllFromXml(
        this, keyboardContext, resourceMapping, parent, keyboardDimens, parser);
    externalResourcePopupLayout = popupResId != 0;
    if (resourceMapping.getApiVersion() < 8 && mCodes.length == 0 && !TextUtils.isEmpty(label)) {
      mCodes = new int[] {Character.codePointAt(label, 0)};
    }

    if (shiftedText == null) {
      shiftedText = text;
    }
    if (shiftedTypedText == null) {
      shiftedTypedText = typedText;
    }
  }

  public int getMultiTapCode(int tapCount) {
    final int codesCount = getCodesCount();
    if (codesCount == 0) return KeyCodes.SPACE; // space is good for nothing
    int safeMultiTapIndex = tapCount < 0 ? 0 : tapCount % codesCount;
    return getCodeAtIndex(safeMultiTapIndex, mKeyboard.isShifted());
  }

  public int getPrimaryCode() {
    return mCodes.length > 0 ? mCodes[0] : 0;
  }

  public int getCodeAtIndex(int index, boolean isShifted) {
    return mCodes.length > 0 ? mCodes[index] : 0;
  }

  public int getCodesCount() {
    return mCodes.length;
  }

  /**
   * Informs the key that it has been pressed, in case it needs to change its appearance or state.
   *
   * @see #onReleased()
   */
  public void onPressed() {
    pressed = true;
  }

  /**
   * Changes the pressed state of the key. If it is a sticky key, it will also change the toggled
   * state of the key if the finger was release inside.
   *
   * @see #onPressed()
   */
  public void onReleased() {
    pressed = false;
  }

  /**
   * Detects if a point falls inside this key.
   *
   * @param x the x-coordinate of the point
   * @param y the y-coordinate of the point
   * @return whether or not the point falls inside the key. If the key is attached to an edge, it
   *     will assume that all points between the key and the edge are considered to be inside the
   *     key.
   */
  public boolean isInside(int x, int y) {
    final boolean leftEdge = (edgeFlags & Keyboard.EDGE_LEFT) != 0;
    final boolean rightEdge = (edgeFlags & Keyboard.EDGE_RIGHT) != 0;
    final boolean topEdge = (edgeFlags & Keyboard.EDGE_TOP) != 0;
    final boolean bottomEdge = (edgeFlags & Keyboard.EDGE_BOTTOM) != 0;
    return (x >= this.x || (leftEdge && x <= this.x + this.width))
        && (x < this.x + this.width || (rightEdge && x >= this.x))
        && (y >= this.y || (topEdge && y <= this.y + this.height))
        && (y < this.y + this.height || (bottomEdge && y >= this.y));
  }

  /**
   * Returns the square of the distance between the closest point inside the key and the given
   * point.
   *
   * @param x the x-coordinate of the point
   * @param y the y-coordinate of the point
   * @return the square of the distance of the point from and the key
   */
  public int squaredDistanceFrom(int x, int y) {
    final int closestX =
        (x < this.x) ? this.x : (x > (this.x + this.width)) ? (this.x + this.width) : x;
    final int closestY =
        (y < this.y) ? this.y : (y > (this.y + this.height)) ? (this.y + this.height) : y;
    final int xDist = closestX - x;
    final int yDist = closestY - y;
    /*
     * int xDist = this.x + width / 2 - x; int yDist = this.y + height /
     * 2 - y;
     */
    return xDist * xDist + yDist * yDist;
  }

  /**
   * Returns the drawable state for the key, based on the current state and type of the key.
   *
   * @return the drawable state of the key.
   * @see android.graphics.drawable.StateListDrawable#setState(int[])
   */
  public int[] getCurrentDrawableState(KeyDrawableStateProvider provider) {
    int[] states = provider.KEY_STATE_NORMAL;
    if (pressed) {
      states = provider.KEY_STATE_PRESSED;
    }
    return states;
  }
}
