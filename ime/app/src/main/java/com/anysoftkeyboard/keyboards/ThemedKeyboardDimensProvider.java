package com.anysoftkeyboard.keyboards;

import androidx.annotation.NonNull;

/**
 * Provides the {@link KeyboardDimens} describing the UI for the current keyboard view + theme.
 *
 * <p>This interface lives in the keyboard model layer so keyboard switching can depend on it
 * without importing view-layer types.
 */
public interface ThemedKeyboardDimensProvider {

  @NonNull
  KeyboardDimens getThemedKeyboardDimens();
}
