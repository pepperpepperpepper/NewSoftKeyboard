package com.anysoftkeyboard.keyboards;

final class KeyboardRowAttributes {

  final int defaultWidth;
  final int defaultHeightCode;
  final int defaultHorizontalGap;
  final int verticalGap;
  final int rowEdgeFlags;
  final int mode;

  KeyboardRowAttributes(
      int defaultWidth,
      int defaultHeightCode,
      int defaultHorizontalGap,
      int verticalGap,
      int rowEdgeFlags,
      int mode) {
    this.defaultWidth = defaultWidth;
    this.defaultHeightCode = defaultHeightCode;
    this.defaultHorizontalGap = defaultHorizontalGap;
    this.verticalGap = verticalGap;
    this.rowEdgeFlags = rowEdgeFlags;
    this.mode = mode;
  }
}
