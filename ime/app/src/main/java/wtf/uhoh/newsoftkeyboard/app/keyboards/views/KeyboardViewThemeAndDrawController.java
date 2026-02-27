package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import java.io.File;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeResourcesHolder;

final class KeyboardViewThemeAndDrawController {

  @NonNull private final KeyboardViewBase view;
  @NonNull private final KeyboardThemeController keyboardThemeController;
  @NonNull private final ThemeOverlayCombiner themeOverlayCombiner;
  @NonNull private final KeyIconResolver keyIconResolver;
  @NonNull private final KeyboardDrawCoordinator keyboardDrawCoordinator;
  @NonNull private final KeyTextStyleState keyTextStyleState;
  @NonNull private final KeyShadowStyle keyShadowStyle;
  @NonNull private final KeyBackgroundShadowStyle keyBackgroundShadowStyle;
  @NonNull private final PerKeyTextShadowOverrides perKeyTextShadowOverrides;
  @NonNull private final PerKeyBackgroundShadowOverrides perKeyBackgroundShadowOverrides;

  @NonNull
  private final wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.PreviewPopupTheme
      previewPopupTheme;

  @NonNull private final LabelPaintConfigurator labelPaintConfigurator;
  @NonNull private final SpecialKeyManager specialKeyManager;
  @NonNull private final TextWidthCache textWidthCache;
  @NonNull private final ImeActionTypeResolver imeActionTypeResolver;

  @Nullable private Integer specialKeyTextColorOverride;
  @Nullable private Integer modifierKeyTextColorOverride;
  @Nullable private Integer enterKeyTextColorOverride;
  @Nullable private Integer userKeyBackgroundTintOverride;
  @Nullable private Integer userSpecialKeyBackgroundTintOverride;
  @Nullable private Integer userSpacebarBackgroundTintOverride;
  @Nullable private Integer userModifierKeyBackgroundTintOverride;
  @Nullable private Integer userEnterKeyBackgroundTintOverride;
  @Nullable private Typeface userHintTextTypefaceOverride;
  @Nullable private Typeface userKeyboardNameTextTypefaceOverride;
  @NonNull private final VoiceStatusBadgeState voiceStatusBadgeState = new VoiceStatusBadgeState();

  @Nullable private PackThemeOverride packThemeOverride;

  private boolean allowExpensiveWallpaperEffects = true;
  private boolean applyUserThemeOverrides = true;

  KeyboardViewThemeAndDrawController(
      @NonNull KeyboardViewBase view,
      @NonNull KeyboardThemeController keyboardThemeController,
      @NonNull ThemeOverlayCombiner themeOverlayCombiner,
      @NonNull KeyIconResolver keyIconResolver,
      @NonNull KeyboardDrawCoordinator keyboardDrawCoordinator,
      @NonNull KeyTextStyleState keyTextStyleState,
      @NonNull KeyShadowStyle keyShadowStyle,
      @NonNull KeyBackgroundShadowStyle keyBackgroundShadowStyle,
      @NonNull PerKeyTextShadowOverrides perKeyTextShadowOverrides,
      @NonNull PerKeyBackgroundShadowOverrides perKeyBackgroundShadowOverrides,
      @NonNull
          wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.PreviewPopupTheme previewPopupTheme,
      @NonNull LabelPaintConfigurator labelPaintConfigurator,
      @NonNull SpecialKeyManager specialKeyManager,
      @NonNull TextWidthCache textWidthCache,
      @NonNull ImeActionTypeResolver imeActionTypeResolver) {
    this.view = view;
    this.keyboardThemeController = keyboardThemeController;
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.keyIconResolver = keyIconResolver;
    this.keyboardDrawCoordinator = keyboardDrawCoordinator;
    this.keyTextStyleState = keyTextStyleState;
    this.keyShadowStyle = keyShadowStyle;
    this.keyBackgroundShadowStyle = keyBackgroundShadowStyle;
    this.perKeyTextShadowOverrides = perKeyTextShadowOverrides;
    this.perKeyBackgroundShadowOverrides = perKeyBackgroundShadowOverrides;
    this.previewPopupTheme = previewPopupTheme;
    this.labelPaintConfigurator = labelPaintConfigurator;
    this.specialKeyManager = specialKeyManager;
    this.textWidthCache = textWidthCache;
    this.imeActionTypeResolver = imeActionTypeResolver;
  }

  @Nullable
  KeyboardTheme getLastSetKeyboardTheme() {
    return keyboardThemeController.lastSetTheme();
  }

  void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    keyboardThemeController.setKeyboardTheme(theme);
    applyPackThemeOverrideIfAny();
  }

  void setThemeOverlay(@NonNull OverlayData overlay) {
    keyboardThemeController.setThemeOverlay(overlay);
    applyPackThemeOverrideIfAny();
  }

  @Nullable
  PackThemeOverride getPackThemeOverride() {
    return packThemeOverride;
  }

  void setPackThemeOverride(@Nullable PackThemeOverride override) {
    packThemeOverride = override;
    keyboardThemeController.reapplyKeyboardTheme();
    applyPackThemeOverrideIfAny();
  }

  private void applyPackThemeOverrideIfAny() {
    PackThemeOverride override = packThemeOverride;
    if (override == null) return;

    final var model = override.themeModel();
    for (var entry : model.colors().entrySet()) {
      String key = normalizeThemeKeyName(entry.getKey());
      int color = entry.getValue();
      switch (key) {
        case "keyboardbackground" -> {
          themeOverlayCombiner.setThemeKeyboardBackground(new ColorDrawable(color));
          view.setBackground(themeOverlayCombiner.getThemeResources().getKeyboardBackground());
        }
        case "keybackground" ->
            themeOverlayCombiner.setThemeKeyBackground(new ColorDrawable(color));
        case "keytextcolor" ->
            themeOverlayCombiner.setThemeTextColor(ColorStateList.valueOf(color));
        case "hinttextcolor" -> themeOverlayCombiner.setThemeHintTextColor(color);
        case "keyboardnametextcolor", "nametextcolor" ->
            themeOverlayCombiner.setThemeNameTextColor(color);
        default -> {}
      }
    }

    if (!model.icons().isEmpty()) {
      keyIconResolver.clearCache(true);
      final File packDir = override.packDirectory();
      for (var entry : model.icons().entrySet()) {
        int keyCode = keyCodeForPackIconName(entry.getKey());
        if (keyCode == 0) continue;

        File iconFile = new File(packDir, entry.getValue().value());
        if (!iconFile.isFile()) continue;
        keyIconResolver.putIconBuilder(keyCode, DrawableBuilder.buildFromFile(iconFile));
      }
    }

    view.invalidateAllKeys();
  }

  void setVoiceInputState(@NonNull VoiceInputState state) {
    if (voiceStatusBadgeState.setState(state, view.getContext())) {
      view.invalidateAllKeys();
    }
  }

  void onDraw(@NonNull final Canvas canvas) {
    if (view.keyboardRenderState.keyboardChanged) {
      view.invalidateAllKeys();
      view.keyboardRenderState.keyboardChanged = false;
    }
    keyboardDrawCoordinator.draw(
        canvas,
        keyboardThemeController.lastSetTheme(),
        view.keyboardRenderState.keyboard,
        view.keyboardRenderState.keys,
        view.keyboardRenderState.drawableStatesProvider,
        view.keyboardRenderState.keyboardName,
        voiceStatusBadgeState.badgeText(),
        specialKeyTextColorOverride,
        modifierKeyTextColorOverride,
        enterKeyTextColorOverride,
        userKeyBackgroundTintOverride,
        userSpecialKeyBackgroundTintOverride,
        userSpacebarBackgroundTintOverride,
        userModifierKeyBackgroundTintOverride,
        userEnterKeyBackgroundTintOverride,
        allowExpensiveWallpaperEffects,
        view.getWidth(),
        view.getHeight(),
        view.getPaddingLeft(),
        view.getPaddingTop());
  }

  void setAllowExpensiveWallpaperEffects(boolean allow) {
    if (allowExpensiveWallpaperEffects == allow) return;
    allowExpensiveWallpaperEffects = allow;
    view.invalidate();
  }

  void setApplyUserThemeOverrides(boolean apply) {
    if (applyUserThemeOverrides == apply) return;
    applyUserThemeOverrides = apply;
    keyboardThemeController.reapplyKeyboardTheme();
  }

  boolean shouldApplyUserThemeOverrides() {
    return applyUserThemeOverrides;
  }

  void clearUserThemeOverridesForPreview() {
    specialKeyTextColorOverride = null;
    modifierKeyTextColorOverride = null;
    enterKeyTextColorOverride = null;

    userKeyBackgroundTintOverride = null;
    userSpecialKeyBackgroundTintOverride = null;
    userSpacebarBackgroundTintOverride = null;
    userModifierKeyBackgroundTintOverride = null;
    userEnterKeyBackgroundTintOverride = null;

    userHintTextTypefaceOverride = null;
    userKeyboardNameTextTypefaceOverride = null;

    keyTextStyleState.setAutoFitKeyLabels(true);
    keyTextStyleState.setKeyLabelAutoFitMinScale(0.3f);
    keyTextStyleState.setEllipsizeKeyLabels(true);
    keyTextStyleState.setKeyTextSizeScale(1f);
    keyTextStyleState.setLabelTextSizeScale(1f);
    keyTextStyleState.setHintTextSizeScale(1f);
    keyTextStyleState.setKeyboardNameTextSizeScale(1f);

    perKeyTextShadowOverrides.special().set(null, null, null, null);
    perKeyTextShadowOverrides.spacebar().set(null, null, null, null);
    perKeyTextShadowOverrides.modifier().set(null, null, null, null);
    perKeyTextShadowOverrides.enter().set(null, null, null, null);

    keyBackgroundShadowStyle.disable();
    perKeyBackgroundShadowOverrides.special().set(null, null, null);
    perKeyBackgroundShadowOverrides.spacebar().set(null, null, null);
    perKeyBackgroundShadowOverrides.modifier().set(null, null, null);
    perKeyBackgroundShadowOverrides.enter().set(null, null, null);
  }

  void applyUserKeyTextTypefaceOverrides(
      @Nullable Typeface fontFamilyOverride, @Nullable Integer fontStyleOverride) {
    if (fontFamilyOverride == null && fontStyleOverride == null) return;

    final Typeface baseTypeface = keyTextStyleState.keyTextStyle();
    final Typeface familyTypeface = fontFamilyOverride != null ? fontFamilyOverride : baseTypeface;

    final int resolvedStyle =
        fontStyleOverride != null
            ? Math.max(Typeface.NORMAL, Math.min(Typeface.BOLD_ITALIC, fontStyleOverride))
            : baseTypeface.getStyle();

    final Typeface updated = Typeface.create(familyTypeface, resolvedStyle);
    keyTextStyleState.setKeyTextStyle(updated);
    previewPopupTheme.setKeyStyle(updated);
  }

  void applyUserHintTextTypefaceOverrides(
      @Nullable Typeface fontFamilyOverride, @Nullable Integer fontStyleOverride) {
    if (fontFamilyOverride == null && fontStyleOverride == null) {
      userHintTextTypefaceOverride = null;
      return;
    }

    final Typeface baseTypeface = keyTextStyleState.labelTextStyle();
    final Typeface familyTypeface = fontFamilyOverride != null ? fontFamilyOverride : baseTypeface;

    final int resolvedStyle =
        fontStyleOverride != null
            ? Math.max(Typeface.NORMAL, Math.min(Typeface.BOLD_ITALIC, fontStyleOverride))
            : baseTypeface.getStyle();

    userHintTextTypefaceOverride = Typeface.create(familyTypeface, resolvedStyle);
  }

  void applyUserKeyboardNameTextTypefaceOverrides(
      @Nullable Typeface fontFamilyOverride, @Nullable Integer fontStyleOverride) {
    if (fontFamilyOverride == null && fontStyleOverride == null) {
      userKeyboardNameTextTypefaceOverride = null;
      return;
    }

    final Typeface baseTypeface = keyTextStyleState.labelTextStyle();
    final Typeface familyTypeface = fontFamilyOverride != null ? fontFamilyOverride : baseTypeface;

    final int resolvedStyle =
        fontStyleOverride != null
            ? Math.max(Typeface.NORMAL, Math.min(Typeface.BOLD_ITALIC, fontStyleOverride))
            : baseTypeface.getStyle();

    userKeyboardNameTextTypefaceOverride = Typeface.create(familyTypeface, resolvedStyle);
  }

  void applyUserKeyLabelAutoFitOverrides(@Nullable Boolean enabled) {
    keyTextStyleState.setAutoFitKeyLabels(enabled == null || enabled);
  }

  void applyUserKeyLabelAutoFitMinSizePercentOverrides(@Nullable Integer percent) {
    final int effectivePercent = percent != null ? Math.max(10, Math.min(100, percent)) : 30;
    keyTextStyleState.setKeyLabelAutoFitMinScale(effectivePercent / 100f);
  }

  void applyUserKeyLabelEllipsizeOverrides(@Nullable Boolean enabled) {
    keyTextStyleState.setEllipsizeKeyLabels(enabled == null || enabled);
  }

  void applyUserTextSizeScaleOverrides(
      @Nullable Integer keyLabelTextSizePercent,
      @Nullable Integer hintTextSizePercent,
      @Nullable Integer keyboardNameTextSizePercent) {
    final float keyScale = percentToScale(keyLabelTextSizePercent);
    keyTextStyleState.setKeyTextSizeScale(keyScale);
    keyTextStyleState.setLabelTextSizeScale(keyScale);
    keyTextStyleState.setHintTextSizeScale(percentToScale(hintTextSizePercent));
    keyTextStyleState.setKeyboardNameTextSizeScale(percentToScale(keyboardNameTextSizePercent));
  }

  private static float percentToScale(@Nullable Integer percent) {
    if (percent == null) return 1f;
    final int clamped = Math.max(50, Math.min(200, percent));
    return clamped / 100f;
  }

  void applyUserKeyTextShadowOverrides(
      @Nullable Integer shadowColor,
      @Nullable Integer shadowRadiusPx,
      @Nullable Integer shadowOffsetXPx,
      @Nullable Integer shadowOffsetYPx) {
    if (shadowColor != null) keyShadowStyle.setColor(shadowColor);
    if (shadowRadiusPx != null) keyShadowStyle.setRadius(shadowRadiusPx);
    if (shadowOffsetXPx != null) keyShadowStyle.setOffsetX(shadowOffsetXPx);
    if (shadowOffsetYPx != null) keyShadowStyle.setOffsetY(shadowOffsetYPx);
  }

  void applyUserPerKeyTextShadowOverrides(
      @Nullable Integer specialKeyShadowColor,
      @Nullable Integer specialKeyShadowRadiusPx,
      @Nullable Integer specialKeyShadowOffsetXPx,
      @Nullable Integer specialKeyShadowOffsetYPx,
      @Nullable Integer spacebarShadowColor,
      @Nullable Integer spacebarShadowRadiusPx,
      @Nullable Integer spacebarShadowOffsetXPx,
      @Nullable Integer spacebarShadowOffsetYPx,
      @Nullable Integer modifierKeyShadowColor,
      @Nullable Integer modifierKeyShadowRadiusPx,
      @Nullable Integer modifierKeyShadowOffsetXPx,
      @Nullable Integer modifierKeyShadowOffsetYPx,
      @Nullable Integer enterKeyShadowColor,
      @Nullable Integer enterKeyShadowRadiusPx,
      @Nullable Integer enterKeyShadowOffsetXPx,
      @Nullable Integer enterKeyShadowOffsetYPx) {
    perKeyTextShadowOverrides
        .special()
        .set(
            specialKeyShadowColor,
            specialKeyShadowRadiusPx,
            specialKeyShadowOffsetXPx,
            specialKeyShadowOffsetYPx);
    perKeyTextShadowOverrides
        .spacebar()
        .set(
            spacebarShadowColor,
            spacebarShadowRadiusPx,
            spacebarShadowOffsetXPx,
            spacebarShadowOffsetYPx);
    perKeyTextShadowOverrides
        .modifier()
        .set(
            modifierKeyShadowColor,
            modifierKeyShadowRadiusPx,
            modifierKeyShadowOffsetXPx,
            modifierKeyShadowOffsetYPx);
    perKeyTextShadowOverrides
        .enter()
        .set(
            enterKeyShadowColor,
            enterKeyShadowRadiusPx,
            enterKeyShadowOffsetXPx,
            enterKeyShadowOffsetYPx);
  }

  void applyUserKeyBackgroundShadowOverrides(
      @Nullable Integer shadowColor,
      @Nullable Integer shadowOffsetXPx,
      @Nullable Integer shadowOffsetYPx,
      @Nullable Integer shadowSpreadPx) {
    if (shadowColor == null) {
      keyBackgroundShadowStyle.disable();
      return;
    }

    keyBackgroundShadowStyle.setEnabled(true);
    keyBackgroundShadowStyle.setColor(shadowColor);
    keyBackgroundShadowStyle.setOffsetX(shadowOffsetXPx != null ? shadowOffsetXPx : 0);
    keyBackgroundShadowStyle.setOffsetY(shadowOffsetYPx != null ? shadowOffsetYPx : 0);
    keyBackgroundShadowStyle.setSpread(shadowSpreadPx != null ? Math.max(0, shadowSpreadPx) : 0);
  }

  void applyUserPerKeyBackgroundShadowOverrides(
      @Nullable Integer specialKeyShadowColor,
      @Nullable Integer specialKeyShadowOffsetXPx,
      @Nullable Integer specialKeyShadowOffsetYPx,
      @Nullable Integer spacebarShadowColor,
      @Nullable Integer spacebarShadowOffsetXPx,
      @Nullable Integer spacebarShadowOffsetYPx,
      @Nullable Integer modifierKeyShadowColor,
      @Nullable Integer modifierKeyShadowOffsetXPx,
      @Nullable Integer modifierKeyShadowOffsetYPx,
      @Nullable Integer enterKeyShadowColor,
      @Nullable Integer enterKeyShadowOffsetXPx,
      @Nullable Integer enterKeyShadowOffsetYPx) {
    perKeyBackgroundShadowOverrides
        .special()
        .set(specialKeyShadowColor, specialKeyShadowOffsetXPx, specialKeyShadowOffsetYPx);
    perKeyBackgroundShadowOverrides
        .spacebar()
        .set(spacebarShadowColor, spacebarShadowOffsetXPx, spacebarShadowOffsetYPx);
    perKeyBackgroundShadowOverrides
        .modifier()
        .set(modifierKeyShadowColor, modifierKeyShadowOffsetXPx, modifierKeyShadowOffsetYPx);
    perKeyBackgroundShadowOverrides
        .enter()
        .set(enterKeyShadowColor, enterKeyShadowOffsetXPx, enterKeyShadowOffsetYPx);
  }

  void applyUserSpecialKeyTextColorOverride(@Nullable Integer textColor) {
    specialKeyTextColorOverride = textColor;
  }

  void applyUserModifierKeyTextColorOverride(@Nullable Integer textColor) {
    modifierKeyTextColorOverride = textColor;
  }

  void applyUserEnterKeyTextColorOverride(@Nullable Integer textColor) {
    enterKeyTextColorOverride = textColor;
  }

  void applyUserKeyBackgroundTintOverrides(
      @Nullable Integer keyBackgroundTint,
      @Nullable Integer specialKeyBackgroundTint,
      @Nullable Integer spacebarBackgroundTint,
      @Nullable Integer modifierKeyBackgroundTint,
      @Nullable Integer enterKeyBackgroundTint) {
    userKeyBackgroundTintOverride = keyBackgroundTint;
    userSpecialKeyBackgroundTintOverride = specialKeyBackgroundTint;
    userSpacebarBackgroundTintOverride = spacebarBackgroundTint;
    userModifierKeyBackgroundTintOverride = modifierKeyBackgroundTint;
    userEnterKeyBackgroundTintOverride = enterKeyBackgroundTint;
  }

  void setPaintForLabelText(@NonNull Paint paint) {
    labelPaintConfigurator.setPaintForLabelText(
        paint, keyTextStyleState.labelTextSize(), keyTextStyleState.labelTextStyle());
  }

  void setPaintForKeyboardNameText(@NonNull Paint paint) {
    paint.setTypeface(
        userKeyboardNameTextTypefaceOverride != null
            ? userKeyboardNameTextTypefaceOverride
            : keyTextStyleState.labelTextStyle());
  }

  void setPaintForHintText(@NonNull Paint paint) {
    paint.setTypeface(
        userHintTextTypefaceOverride != null
            ? userHintTextTypefaceOverride
            : keyTextStyleState.labelTextStyle());
  }

  void setPaintToKeyText(@NonNull Paint paint) {
    labelPaintConfigurator.setPaintToKeyText(
        paint, keyTextStyleState.keyTextSize(), keyTextStyleState.keyTextStyle());
  }

  void setKeyboardActionType(final int imeOptions) {
    view.keyboardRenderState.keyboardActionType =
        imeActionTypeResolver.resolveActionType(imeOptions);
    setSpecialKeysIconsAndLabels();
  }

  void setSpecialKeysIconsAndLabels() {
    specialKeyManager.applySpecialKeys(
        view.keyboardRenderState.keyboard,
        view.keyboardRenderState.keyboardActionType,
        view.keyboardRenderState.nextAlphabetKeyboardName,
        view.keyboardRenderState.nextSymbolsKeyboardName,
        textWidthCache,
        view::findKeyByPrimaryKeyCode,
        view.getContext());
  }

  @NonNull
  CharSequence guessLabelForKey(int keyCode) {
    return specialKeyManager.guessLabelForKey(
        keyCode,
        view.keyboardRenderState.keyboardActionType,
        view.keyboardRenderState.nextAlphabetKeyboardName,
        view.keyboardRenderState.nextSymbolsKeyboardName,
        view.keyboardRenderState.keyboard,
        view.getContext());
  }

  @Nullable
  android.graphics.drawable.Drawable getDrawableForKeyCode(int keyCode) {
    return keyIconResolver.getIconForKeyCode(keyCode);
  }

  @Nullable
  ThemeResourcesHolder getCurrentResourcesHolder() {
    return themeOverlayCombiner.getThemeResources();
  }

  float getLabelTextSize() {
    return keyTextStyleState.labelTextSize();
  }

  float getKeyTextSize() {
    return keyTextStyleState.keyTextSize();
  }

  private static int keyCodeForPackIconName(@Nullable String raw) {
    if (raw == null) return 0;

    String value = raw.trim();
    if (value.isEmpty()) return 0;

    int slash = value.lastIndexOf('/');
    if (slash >= 0 && slash < value.length() - 1) value = value.substring(slash + 1);
    if (value.startsWith("iconKey")) value = value.substring("iconKey".length());

    StringBuilder builder = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
        builder.append(Character.toLowerCase(c));
      }
    }
    String name = builder.toString();

    return switch (name) {
      case "shift" -> KeyCodes.SHIFT;
      case "control", "ctrl" -> KeyCodes.CTRL;
      case "action", "enter" -> KeyCodes.ENTER;
      case "backspace", "delete" -> KeyCodes.DELETE;
      case "cancel" -> KeyCodes.CANCEL;
      case "globe" -> KeyCodes.MODE_ALPHABET;
      case "space" -> KeyCodes.SPACE;
      case "tab" -> KeyCodes.TAB;
      case "arrowdown", "down" -> KeyCodes.ARROW_DOWN;
      case "arrowleft", "left" -> KeyCodes.ARROW_LEFT;
      case "arrowright", "right" -> KeyCodes.ARROW_RIGHT;
      case "arrowup", "up" -> KeyCodes.ARROW_UP;
      case "inputmovehome", "movehome", "home" -> KeyCodes.MOVE_HOME;
      case "inputmoveend", "moveend", "end" -> KeyCodes.MOVE_END;
      case "mic", "voiceinput", "voice" -> KeyCodes.VOICE_INPUT;
      case "settings" -> KeyCodes.SETTINGS;
      case "condensenormal", "mergelayout" -> KeyCodes.MERGE_LAYOUT;
      case "condensesplit", "splitlayout" -> KeyCodes.SPLIT_LAYOUT;
      case "condensecompacttoright", "compactlayouttoright" -> KeyCodes.COMPACT_LAYOUT_TO_RIGHT;
      case "condensecompacttoleft", "compactlayouttoleft" -> KeyCodes.COMPACT_LAYOUT_TO_LEFT;
      case "clipboardcopy" -> KeyCodes.CLIPBOARD_COPY;
      case "clipboardcut" -> KeyCodes.CLIPBOARD_CUT;
      case "clipboardpaste" -> KeyCodes.CLIPBOARD_PASTE;
      case "clipboardselect" -> KeyCodes.CLIPBOARD_SELECT_ALL;
      case "clipboardfineselect" -> KeyCodes.CLIPBOARD_SELECT;
      case "quicktextpopup" -> KeyCodes.QUICK_TEXT_POPUP;
      case "quicktext" -> KeyCodes.QUICK_TEXT;
      case "undo" -> KeyCodes.UNDO;
      case "redo" -> KeyCodes.REDO;
      case "forwarddelete" -> KeyCodes.FORWARD_DELETE;
      case "imageinsert" -> KeyCodes.IMAGE_MEDIA_POPUP;
      case "clearquicktexthistory" -> KeyCodes.CLEAR_QUICK_TEXT_HISTORY;
      default -> 0;
    };
  }

  @NonNull
  private static String normalizeThemeKeyName(@Nullable String raw) {
    if (raw == null) return "";
    String value = raw.trim();
    if (value.isEmpty()) return "";

    int slash = value.lastIndexOf('/');
    if (slash >= 0 && slash < value.length() - 1) value = value.substring(slash + 1);

    StringBuilder builder = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
        builder.append(Character.toLowerCase(c));
      }
    }
    return builder.toString();
  }
}
