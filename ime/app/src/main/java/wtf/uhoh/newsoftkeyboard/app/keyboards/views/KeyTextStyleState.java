package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Typeface;

/** Holds text size/style state to keep KeyboardViewBase lean. */
final class KeyTextStyleState {

  private float keyTextSize;
  private Typeface keyTextStyle = Typeface.DEFAULT;
  private Typeface labelTextStyle = Typeface.DEFAULT_BOLD;
  private float labelTextSize;
  private float keyboardNameTextSize;
  private float hintTextSize;
  private float hintTextSizeMultiplier;
  private int themeHintLabelAlign;
  private int themeHintLabelVAlign;
  private int textCaseForceOverrideType;
  private int textCaseType;
  private boolean autoFitKeyLabels = true;
  private float keyLabelAutoFitMinScale = 0.3f;
  private boolean ellipsizeKeyLabels = true;
  private float keyTextSizeScale = 1f;
  private float labelTextSizeScale = 1f;
  private float keyboardNameTextSizeScale = 1f;
  private float hintTextSizeScale = 1f;

  float keyTextSize() {
    return keyTextSize * keyTextSizeScale;
  }

  void setKeyTextSize(int size) {
    this.keyTextSize = size;
  }

  void setKeyTextSizeScale(float scale) {
    this.keyTextSizeScale = sanitizeScale(scale);
  }

  Typeface keyTextStyle() {
    return keyTextStyle;
  }

  void setKeyTextStyle(Typeface style) {
    this.keyTextStyle = style != null ? style : Typeface.DEFAULT;
    this.labelTextStyle = Typeface.create(this.keyTextStyle, Typeface.BOLD);
  }

  Typeface labelTextStyle() {
    return labelTextStyle;
  }

  float labelTextSize() {
    return labelTextSize * labelTextSizeScale;
  }

  void setLabelTextSize(int size) {
    this.labelTextSize = size;
  }

  void setLabelTextSizeScale(float scale) {
    this.labelTextSizeScale = sanitizeScale(scale);
  }

  float keyboardNameTextSize() {
    return keyboardNameTextSize * keyboardNameTextSizeScale;
  }

  void setKeyboardNameTextSize(int size) {
    this.keyboardNameTextSize = size;
  }

  void setKeyboardNameTextSizeScale(float scale) {
    this.keyboardNameTextSizeScale = sanitizeScale(scale);
  }

  float hintTextSize() {
    return hintTextSize * hintTextSizeScale;
  }

  void setHintTextSize(int size) {
    this.hintTextSize = size;
  }

  void setHintTextSizeScale(float scale) {
    this.hintTextSizeScale = sanitizeScale(scale);
  }

  float hintTextSizeMultiplier() {
    return hintTextSizeMultiplier;
  }

  void setHintTextSizeMultiplier(float multiplier) {
    this.hintTextSizeMultiplier = multiplier;
  }

  int themeHintLabelAlign() {
    return themeHintLabelAlign;
  }

  void setThemeHintLabelAlign(int align) {
    this.themeHintLabelAlign = align;
  }

  int themeHintLabelVAlign() {
    return themeHintLabelVAlign;
  }

  void setThemeHintLabelVAlign(int vAlign) {
    this.themeHintLabelVAlign = vAlign;
  }

  int textCaseForceOverrideType() {
    return textCaseForceOverrideType;
  }

  void setTextCaseForceOverrideType(int type) {
    this.textCaseForceOverrideType = type;
  }

  int textCaseType() {
    return textCaseType;
  }

  void setTextCaseType(int type) {
    this.textCaseType = type;
  }

  boolean autoFitKeyLabels() {
    return autoFitKeyLabels;
  }

  void setAutoFitKeyLabels(boolean autoFitKeyLabels) {
    this.autoFitKeyLabels = autoFitKeyLabels;
  }

  float keyLabelAutoFitMinScale() {
    return keyLabelAutoFitMinScale;
  }

  void setKeyLabelAutoFitMinScale(float keyLabelAutoFitMinScale) {
    if (Float.isNaN(keyLabelAutoFitMinScale) || Float.isInfinite(keyLabelAutoFitMinScale)) {
      this.keyLabelAutoFitMinScale = 0.3f;
    } else {
      this.keyLabelAutoFitMinScale = Math.max(0.1f, Math.min(1f, keyLabelAutoFitMinScale));
    }
  }

  boolean ellipsizeKeyLabels() {
    return ellipsizeKeyLabels;
  }

  void setEllipsizeKeyLabels(boolean ellipsizeKeyLabels) {
    this.ellipsizeKeyLabels = ellipsizeKeyLabels;
  }

  private static float sanitizeScale(float scale) {
    if (Float.isNaN(scale) || Float.isInfinite(scale)) return 1f;
    return Math.max(0.5f, Math.min(2f, scale));
  }
}
