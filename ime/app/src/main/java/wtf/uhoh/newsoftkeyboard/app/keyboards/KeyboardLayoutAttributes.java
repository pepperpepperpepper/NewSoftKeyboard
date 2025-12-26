package wtf.uhoh.newsoftkeyboard.app.keyboards;

final class KeyboardLayoutAttributes {

  final int defaultWidth;
  final int defaultHeightCode;
  final int defaultHorizontalGap;
  final int defaultVerticalGap;
  final boolean showPreview;
  final boolean autoCap;

  KeyboardLayoutAttributes(
      int defaultWidth,
      int defaultHeightCode,
      int defaultHorizontalGap,
      int defaultVerticalGap,
      boolean showPreview,
      boolean autoCap) {
    this.defaultWidth = defaultWidth;
    this.defaultHeightCode = defaultHeightCode;
    this.defaultHorizontalGap = defaultHorizontalGap;
    this.defaultVerticalGap = defaultVerticalGap;
    this.showPreview = showPreview;
    this.autoCap = autoCap;
  }
}
