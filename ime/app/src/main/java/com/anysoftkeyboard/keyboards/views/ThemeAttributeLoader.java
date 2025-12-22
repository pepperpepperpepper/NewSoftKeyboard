package com.anysoftkeyboard.keyboards.views;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import java.util.Set;

/** Loads theme attributes and icons, keeping AnyKeyboardViewBase thinner. */
final class ThemeAttributeLoader {

  interface Host {
    @NonNull
    ThemeResourcesHolder getThemeOverlayResources();

    int getKeyboardStyleResId(@NonNull KeyboardTheme theme);

    int getKeyboardIconsStyleResId(@NonNull KeyboardTheme theme);

    @NonNull
    KeyboardTheme getFallbackTheme();

    @NonNull
    int[] getActionKeyTypes();

    boolean setValueFromTheme(
        TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex);

    boolean setKeyIconValueFromTheme(
        KeyboardTheme theme,
        TypedArray remoteTypedArray,
        int localAttrId,
        int remoteTypedArrayIndex);

    void setBackground(Drawable background);

    void setPadding(int left, int top, int right, int bottom);

    int getWidth();

    @NonNull
    Resources getResources();

    void onKeyDrawableProviderReady(
        int keyTypeFunctionAttrId,
        int keyActionAttrId,
        int keyActionTypeDoneAttrId,
        int keyActionTypeSearchAttrId,
        int keyActionTypeGoAttrId);

    void onKeyboardDimensSet(int availableWidth);
  }

  private final Host host;

  ThemeAttributeLoader(@NonNull Host host) {
    this.host = host;
  }

  void loadThemeAttributes(KeyboardTheme theme, Set<Integer> doneLocalAttributeIds, int[] padding) {
    final var resourceMapping = theme.getResourceMapping();
    final int[] remoteKeyboardThemeStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewTheme);
    final int[] remoteKeyboardIconsThemeStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewIconsTheme);

    TypedArray a =
        theme
            .getPackageContext()
            .obtainStyledAttributes(
                host.getKeyboardStyleResId(theme), remoteKeyboardThemeStyleable);

    int keyTypeFunctionAttrId = 0;
    int keyActionAttrId = 0;
    int keyActionTypeDoneAttrId = 0;
    int keyActionTypeSearchAttrId = 0;
    int keyActionTypeGoAttrId = 0;

    // first pass: main theme values
    final int attrCount = a.getIndexCount();
    for (int i = 0; i < attrCount; i++) {
      final int index = a.getIndex(i);
      final int attrId = R.styleable.AnyKeyboardViewTheme[index];
      setValueFromThemeInternal(a, padding, attrId, index, doneLocalAttributeIds);
      if (attrId == R.attr.key_type_function) keyTypeFunctionAttrId = index;
    }
    a.recycle();

    // icons
    TypedArray iconsArray =
        theme
            .getPackageContext()
            .obtainStyledAttributes(theme.getIconsThemeResId(), remoteKeyboardIconsThemeStyleable);
    final int iconCount = iconsArray.getIndexCount();
    for (int i = 0; i < iconCount; i++) {
      final int remoteIndex = iconsArray.getIndex(i);
      final int localAttrId = R.styleable.AnyKeyboardViewIconsTheme[remoteIndex];
      if (setKeyIconValueFromThemeInternal(theme, iconsArray, localAttrId, remoteIndex)) {
        doneLocalAttributeIds.add(localAttrId);
        if (localAttrId == R.attr.iconKeyAction) {
          final int[] keyStateAttributes =
              resourceMapping.getRemoteStyleableArrayFromLocal(host.getActionKeyTypes());
          keyActionTypeDoneAttrId = keyStateAttributes[0];
          keyActionTypeSearchAttrId = keyStateAttributes[1];
          keyActionTypeGoAttrId = keyStateAttributes[2];
        }
      }
    }
    iconsArray.recycle();

    // fallback theme values
    KeyboardTheme fallbackTheme = host.getFallbackTheme();
    final int keyboardFallbackThemeStyleResId = host.getKeyboardStyleResId(fallbackTheme);
    TypedArray fallbackArray =
        fallbackTheme
            .getPackageContext()
            .obtainStyledAttributes(
                keyboardFallbackThemeStyleResId, R.styleable.AnyKeyboardViewTheme);
    final int fallbackCount = fallbackArray.getIndexCount();
    for (int i = 0; i < fallbackCount; i++) {
      final int index = fallbackArray.getIndex(i);
      final int attrId = R.styleable.AnyKeyboardViewTheme[index];
      setValueFromThemeInternal(fallbackArray, padding, attrId, index, doneLocalAttributeIds);
    }
    fallbackArray.recycle();

    // fallback icons
    TypedArray fallbackIconsArray =
        fallbackTheme
            .getPackageContext()
            .obtainStyledAttributes(
                fallbackTheme.getIconsThemeResId(), R.styleable.AnyKeyboardViewIconsTheme);
    final int fallbackIconsCount = fallbackIconsArray.getIndexCount();
    for (int i = 0; i < fallbackIconsCount; i++) {
      final int index = fallbackIconsArray.getIndex(i);
      final int attrId = R.styleable.AnyKeyboardViewIconsTheme[index];
      if (!doneLocalAttributeIds.contains(attrId)) {
        setKeyIconValueFromThemeInternal(fallbackTheme, fallbackIconsArray, attrId, index);
      }
    }
    fallbackIconsArray.recycle();

    host.onKeyDrawableProviderReady(
        keyTypeFunctionAttrId,
        keyActionAttrId,
        keyActionTypeDoneAttrId,
        keyActionTypeSearchAttrId,
        keyActionTypeGoAttrId);

    // padding and dims
    Drawable keyboardBackground = host.getThemeOverlayResources().getKeyboardBackground();
    if (keyboardBackground != null) {
      Rect backgroundPadding = new Rect();
      keyboardBackground.getPadding(backgroundPadding);
      padding[0] += backgroundPadding.left;
      padding[1] += backgroundPadding.top;
      padding[2] += backgroundPadding.right;
      padding[3] += backgroundPadding.bottom;
    }
    host.setBackground(host.getThemeOverlayResources().getKeyboardBackground());
    host.setPadding(padding[0], padding[1], padding[2], padding[3]);

    final Resources res = host.getResources();
    final int viewWidth =
        (host.getWidth() > 0) ? host.getWidth() : res.getDisplayMetrics().widthPixels;
    host.onKeyboardDimensSet(viewWidth - padding[0] - padding[2]);
  }

  private void setValueFromThemeInternal(
      TypedArray remoteTypedArray,
      int[] padding,
      int localAttrId,
      int remoteTypedArrayIndex,
      Set<Integer> doneLocalAttributeIds) {
    try {
      if (host.setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex)) {
        doneLocalAttributeIds.add(localAttrId);
      }
    } catch (RuntimeException e) {
      if (BuildConfig.DEBUG) throw e;
    }
  }

  private boolean setKeyIconValueFromThemeInternal(
      KeyboardTheme theme,
      TypedArray remoteTypedArray,
      int localAttrId,
      int remoteTypedArrayIndex) {
    try {
      return host.setKeyIconValueFromTheme(
          theme, remoteTypedArray, localAttrId, remoteTypedArrayIndex);
    } catch (RuntimeException e) {
      if (BuildConfig.DEBUG) throw e;
      return false;
    }
  }
}
