package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationShadowsSection.ShadowTarget;

final class KeyboardThemeCustomizationTextShadowOverrides {

  record EffectiveValues(
      boolean usesTokenSecondary,
      @Nullable Integer shadowColor,
      @Nullable Integer shadowRadiusDp,
      @Nullable Integer shadowOffsetXDp,
      @Nullable Integer shadowOffsetYDp) {
    boolean hasAnyOverride() {
      return usesTokenSecondary
          || shadowColor != null
          || shadowRadiusDp != null
          || shadowOffsetXDp != null
          || shadowOffsetYDp != null;
    }
  }

  @NonNull
  static EffectiveValues readEffectiveValues(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target) {
    final Integer tokenSecondaryTextShadowColor = store.getTokenSecondaryTextShadowColor(themeId);
    final Integer tokenSecondaryTextShadowRadiusDp =
        store.getTokenSecondaryTextShadowRadiusDp(themeId);
    final Integer tokenSecondaryTextShadowOffsetXDp =
        store.getTokenSecondaryTextShadowOffsetXDp(themeId);
    final Integer tokenSecondaryTextShadowOffsetYDp =
        store.getTokenSecondaryTextShadowOffsetYDp(themeId);

    final boolean usesTokenSecondary =
        switch (target) {
          case ALL_KEYS -> store.isKeyTextShadowUseTokenSecondary(themeId);
          case SPECIAL_KEYS -> store.isSpecialKeyTextShadowUseTokenSecondary(themeId);
          case SPACEBAR -> store.isSpacebarKeyTextShadowUseTokenSecondary(themeId);
          case MODIFIER_KEYS -> store.isModifierKeyTextShadowUseTokenSecondary(themeId);
          case ENTER_KEY -> store.isEnterKeyTextShadowUseTokenSecondary(themeId);
        };

    Integer shadowColor = null;
    Integer shadowRadiusDp = null;
    Integer shadowOffsetXDp = null;
    Integer shadowOffsetYDp = null;
    switch (target) {
      case ALL_KEYS -> {
        shadowColor =
            usesTokenSecondary
                ? tokenSecondaryTextShadowColor
                : store.getKeyTextShadowColor(themeId);
        shadowRadiusDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowRadiusDp
                : store.getKeyTextShadowRadiusDp(themeId);
        shadowOffsetXDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetXDp
                : store.getKeyTextShadowOffsetXDp(themeId);
        shadowOffsetYDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetYDp
                : store.getKeyTextShadowOffsetYDp(themeId);
      }
      case SPECIAL_KEYS -> {
        shadowColor =
            usesTokenSecondary
                ? tokenSecondaryTextShadowColor
                : store.getSpecialKeyTextShadowColor(themeId);
        shadowRadiusDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowRadiusDp
                : store.getSpecialKeyTextShadowRadiusDp(themeId);
        shadowOffsetXDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetXDp
                : store.getSpecialKeyTextShadowOffsetXDp(themeId);
        shadowOffsetYDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetYDp
                : store.getSpecialKeyTextShadowOffsetYDp(themeId);
      }
      case SPACEBAR -> {
        shadowColor =
            usesTokenSecondary
                ? tokenSecondaryTextShadowColor
                : store.getSpacebarKeyTextShadowColor(themeId);
        shadowRadiusDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowRadiusDp
                : store.getSpacebarKeyTextShadowRadiusDp(themeId);
        shadowOffsetXDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetXDp
                : store.getSpacebarKeyTextShadowOffsetXDp(themeId);
        shadowOffsetYDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetYDp
                : store.getSpacebarKeyTextShadowOffsetYDp(themeId);
      }
      case MODIFIER_KEYS -> {
        shadowColor =
            usesTokenSecondary
                ? tokenSecondaryTextShadowColor
                : store.getModifierKeyTextShadowColor(themeId);
        shadowRadiusDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowRadiusDp
                : store.getModifierKeyTextShadowRadiusDp(themeId);
        shadowOffsetXDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetXDp
                : store.getModifierKeyTextShadowOffsetXDp(themeId);
        shadowOffsetYDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetYDp
                : store.getModifierKeyTextShadowOffsetYDp(themeId);
      }
      case ENTER_KEY -> {
        shadowColor =
            usesTokenSecondary
                ? tokenSecondaryTextShadowColor
                : store.getEnterKeyTextShadowColor(themeId);
        shadowRadiusDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowRadiusDp
                : store.getEnterKeyTextShadowRadiusDp(themeId);
        shadowOffsetXDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetXDp
                : store.getEnterKeyTextShadowOffsetXDp(themeId);
        shadowOffsetYDp =
            usesTokenSecondary
                ? tokenSecondaryTextShadowOffsetYDp
                : store.getEnterKeyTextShadowOffsetYDp(themeId);
      }
    }
    return new EffectiveValues(
        usesTokenSecondary, shadowColor, shadowRadiusDp, shadowOffsetXDp, shadowOffsetYDp);
  }

  static boolean hasAnyOverride(
      @NonNull KeyboardThemeUserOverridesStore store, @NonNull String themeId) {
    return store.getKeyTextShadowColor(themeId) != null
        || store.isKeyTextShadowUseTokenSecondary(themeId)
        || store.isSpecialKeyTextShadowUseTokenSecondary(themeId)
        || store.isSpacebarKeyTextShadowUseTokenSecondary(themeId)
        || store.isModifierKeyTextShadowUseTokenSecondary(themeId)
        || store.isEnterKeyTextShadowUseTokenSecondary(themeId)
        || store.getKeyTextShadowRadiusDp(themeId) != null
        || store.getKeyTextShadowOffsetXDp(themeId) != null
        || store.getKeyTextShadowOffsetYDp(themeId) != null
        || store.getSpecialKeyTextShadowColor(themeId) != null
        || store.getSpecialKeyTextShadowRadiusDp(themeId) != null
        || store.getSpecialKeyTextShadowOffsetXDp(themeId) != null
        || store.getSpecialKeyTextShadowOffsetYDp(themeId) != null
        || store.getSpacebarKeyTextShadowColor(themeId) != null
        || store.getSpacebarKeyTextShadowRadiusDp(themeId) != null
        || store.getSpacebarKeyTextShadowOffsetXDp(themeId) != null
        || store.getSpacebarKeyTextShadowOffsetYDp(themeId) != null
        || store.getModifierKeyTextShadowColor(themeId) != null
        || store.getModifierKeyTextShadowRadiusDp(themeId) != null
        || store.getModifierKeyTextShadowOffsetXDp(themeId) != null
        || store.getModifierKeyTextShadowOffsetYDp(themeId) != null
        || store.getEnterKeyTextShadowColor(themeId) != null
        || store.getEnterKeyTextShadowRadiusDp(themeId) != null
        || store.getEnterKeyTextShadowOffsetXDp(themeId) != null
        || store.getEnterKeyTextShadowOffsetYDp(themeId) != null;
  }

  static void clearAllOverrides(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target) {
    switch (target) {
      case ALL_KEYS -> store.clearTextShadowOverrides(themeId);
      case SPECIAL_KEYS -> store.clearSpecialKeyTextShadowOverrides(themeId);
      case SPACEBAR -> store.clearSpacebarKeyTextShadowOverrides(themeId);
      case MODIFIER_KEYS -> store.clearModifierKeyTextShadowOverrides(themeId);
      case ENTER_KEY -> store.clearEnterKeyTextShadowOverrides(themeId);
    }
  }

  static void setUseTokenSecondary(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target,
      boolean enabled) {
    switch (target) {
      case ALL_KEYS -> store.setKeyTextShadowUseTokenSecondary(themeId, enabled);
      case SPECIAL_KEYS -> store.setSpecialKeyTextShadowUseTokenSecondary(themeId, enabled);
      case SPACEBAR -> store.setSpacebarKeyTextShadowUseTokenSecondary(themeId, enabled);
      case MODIFIER_KEYS -> store.setModifierKeyTextShadowUseTokenSecondary(themeId, enabled);
      case ENTER_KEY -> store.setEnterKeyTextShadowUseTokenSecondary(themeId, enabled);
    }
  }

  static void applyPreset(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target,
      int colorArgb,
      int radiusDp,
      int offsetXDp,
      int offsetYDp) {
    switch (target) {
      case ALL_KEYS -> {
        store.setKeyTextShadowColor(themeId, colorArgb);
        store.setKeyTextShadowRadiusDp(themeId, radiusDp);
        store.setKeyTextShadowOffsetXDp(themeId, offsetXDp);
        store.setKeyTextShadowOffsetYDp(themeId, offsetYDp);
      }
      case SPECIAL_KEYS -> {
        store.setSpecialKeyTextShadowColor(themeId, colorArgb);
        store.setSpecialKeyTextShadowRadiusDp(themeId, radiusDp);
        store.setSpecialKeyTextShadowOffsetXDp(themeId, offsetXDp);
        store.setSpecialKeyTextShadowOffsetYDp(themeId, offsetYDp);
      }
      case SPACEBAR -> {
        store.setSpacebarKeyTextShadowColor(themeId, colorArgb);
        store.setSpacebarKeyTextShadowRadiusDp(themeId, radiusDp);
        store.setSpacebarKeyTextShadowOffsetXDp(themeId, offsetXDp);
        store.setSpacebarKeyTextShadowOffsetYDp(themeId, offsetYDp);
      }
      case MODIFIER_KEYS -> {
        store.setModifierKeyTextShadowColor(themeId, colorArgb);
        store.setModifierKeyTextShadowRadiusDp(themeId, radiusDp);
        store.setModifierKeyTextShadowOffsetXDp(themeId, offsetXDp);
        store.setModifierKeyTextShadowOffsetYDp(themeId, offsetYDp);
      }
      case ENTER_KEY -> {
        store.setEnterKeyTextShadowColor(themeId, colorArgb);
        store.setEnterKeyTextShadowRadiusDp(themeId, radiusDp);
        store.setEnterKeyTextShadowOffsetXDp(themeId, offsetXDp);
        store.setEnterKeyTextShadowOffsetYDp(themeId, offsetYDp);
      }
    }
  }

  static void clearColor(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target) {
    switch (target) {
      case ALL_KEYS -> store.clearKeyTextShadowColor(themeId);
      case SPECIAL_KEYS -> store.clearSpecialKeyTextShadowColor(themeId);
      case SPACEBAR -> store.clearSpacebarKeyTextShadowColor(themeId);
      case MODIFIER_KEYS -> store.clearModifierKeyTextShadowColor(themeId);
      case ENTER_KEY -> store.clearEnterKeyTextShadowColor(themeId);
    }
  }

  static void setColor(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target,
      int argb) {
    switch (target) {
      case ALL_KEYS -> store.setKeyTextShadowColor(themeId, argb);
      case SPECIAL_KEYS -> store.setSpecialKeyTextShadowColor(themeId, argb);
      case SPACEBAR -> store.setSpacebarKeyTextShadowColor(themeId, argb);
      case MODIFIER_KEYS -> store.setModifierKeyTextShadowColor(themeId, argb);
      case ENTER_KEY -> store.setEnterKeyTextShadowColor(themeId, argb);
    }
  }

  static void clearRadiusDp(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target) {
    switch (target) {
      case ALL_KEYS -> store.clearKeyTextShadowRadiusDp(themeId);
      case SPECIAL_KEYS -> store.clearSpecialKeyTextShadowRadiusDp(themeId);
      case SPACEBAR -> store.clearSpacebarKeyTextShadowRadiusDp(themeId);
      case MODIFIER_KEYS -> store.clearModifierKeyTextShadowRadiusDp(themeId);
      case ENTER_KEY -> store.clearEnterKeyTextShadowRadiusDp(themeId);
    }
  }

  static void setRadiusDp(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target,
      int value) {
    switch (target) {
      case ALL_KEYS -> store.setKeyTextShadowRadiusDp(themeId, value);
      case SPECIAL_KEYS -> store.setSpecialKeyTextShadowRadiusDp(themeId, value);
      case SPACEBAR -> store.setSpacebarKeyTextShadowRadiusDp(themeId, value);
      case MODIFIER_KEYS -> store.setModifierKeyTextShadowRadiusDp(themeId, value);
      case ENTER_KEY -> store.setEnterKeyTextShadowRadiusDp(themeId, value);
    }
  }

  static void clearOffsetXDp(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target) {
    switch (target) {
      case ALL_KEYS -> store.clearKeyTextShadowOffsetXDp(themeId);
      case SPECIAL_KEYS -> store.clearSpecialKeyTextShadowOffsetXDp(themeId);
      case SPACEBAR -> store.clearSpacebarKeyTextShadowOffsetXDp(themeId);
      case MODIFIER_KEYS -> store.clearModifierKeyTextShadowOffsetXDp(themeId);
      case ENTER_KEY -> store.clearEnterKeyTextShadowOffsetXDp(themeId);
    }
  }

  static void setOffsetXDp(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target,
      int value) {
    switch (target) {
      case ALL_KEYS -> store.setKeyTextShadowOffsetXDp(themeId, value);
      case SPECIAL_KEYS -> store.setSpecialKeyTextShadowOffsetXDp(themeId, value);
      case SPACEBAR -> store.setSpacebarKeyTextShadowOffsetXDp(themeId, value);
      case MODIFIER_KEYS -> store.setModifierKeyTextShadowOffsetXDp(themeId, value);
      case ENTER_KEY -> store.setEnterKeyTextShadowOffsetXDp(themeId, value);
    }
  }

  static void clearOffsetYDp(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target) {
    switch (target) {
      case ALL_KEYS -> store.clearKeyTextShadowOffsetYDp(themeId);
      case SPECIAL_KEYS -> store.clearSpecialKeyTextShadowOffsetYDp(themeId);
      case SPACEBAR -> store.clearSpacebarKeyTextShadowOffsetYDp(themeId);
      case MODIFIER_KEYS -> store.clearModifierKeyTextShadowOffsetYDp(themeId);
      case ENTER_KEY -> store.clearEnterKeyTextShadowOffsetYDp(themeId);
    }
  }

  static void setOffsetYDp(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String themeId,
      @NonNull ShadowTarget target,
      int value) {
    switch (target) {
      case ALL_KEYS -> store.setKeyTextShadowOffsetYDp(themeId, value);
      case SPECIAL_KEYS -> store.setSpecialKeyTextShadowOffsetYDp(themeId, value);
      case SPACEBAR -> store.setSpacebarKeyTextShadowOffsetYDp(themeId, value);
      case MODIFIER_KEYS -> store.setModifierKeyTextShadowOffsetYDp(themeId, value);
      case ENTER_KEY -> store.setEnterKeyTextShadowOffsetYDp(themeId, value);
    }
  }

  private KeyboardThemeCustomizationTextShadowOverrides() {}
}
