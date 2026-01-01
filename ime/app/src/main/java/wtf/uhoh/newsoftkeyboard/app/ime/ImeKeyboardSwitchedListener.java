/*
 * Copyright (c) 2016 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodSubtype;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardSwitchedListener;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardSwitcher;
import wtf.uhoh.newsoftkeyboard.app.keyboards.NextKeyboardType;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksRepository;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.PackThemeOverride;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlParser;

public abstract class ImeKeyboardSwitchedListener extends ImeRxPrefs
    implements KeyboardSwitchedListener {

  private KeyboardSwitcher mKeyboardSwitcher;
  @Nullable private KeyboardDefinition mCurrentAlphabetKeyboard;
  @Nullable private KeyboardDefinition mCurrentSymbolsKeyboard;
  private boolean mInAlphabetKeyboardMode = true;

  @Nullable private CharSequence mExpectedSubtypeChangeKeyboardId;

  private int mLastPrimaryInNonAlphabetKeyboard = 0;

  @Override
  public void onCreate() {
    super.onCreate();

    mKeyboardSwitcher = createKeyboardSwitcher();
  }

  @Override
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {
    super.onOrientationChanged(oldOrientation, newOrientation);

    mKeyboardSwitcher.flushKeyboardsCache();
  }

  @Override
  public void onLowMemory() {
    Logger.w(TAG, "The OS has reported that it is low on memory!. I'll try to clear some cache.");
    mKeyboardSwitcher.onLowMemory();
    super.onLowMemory();
  }

  @NonNull
  protected KeyboardSwitcher createKeyboardSwitcher() {
    return new KeyboardSwitcher(this, getApplicationContext());
  }

  protected final KeyboardSwitcher getKeyboardSwitcher() {
    return mKeyboardSwitcher;
  }

  @Override
  public void onAddOnsCriticalChange() {
    mKeyboardSwitcher.flushKeyboardsCache();
    super.onAddOnsCriticalChange();
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull KeyboardDefinition keyboard) {
    // Preserve voice key state when switching keyboards
    boolean wasVoiceActive = false;
    boolean wasVoiceLocked = false;

    if (mCurrentAlphabetKeyboard != null) {
      wasVoiceActive = mCurrentAlphabetKeyboard.isVoiceActive();
      wasVoiceLocked = mCurrentAlphabetKeyboard.isVoiceLocked();
    }

    mCurrentAlphabetKeyboard = keyboard;

    mInAlphabetKeyboardMode = true;
    // about to report, so setting what is the expected keyboard ID (to discard the event
    mExpectedSubtypeChangeKeyboardId = mCurrentAlphabetKeyboard.getKeyboardId();
    NskApplicationBase.getDeviceSpecific()
        .reportCurrentInputMethodSubtypes(
            getInputMethodManager(),
            getSettingsInputMethodId(),
            getWindow().getWindow().getAttributes().token,
            keyboard.getLocale().toString(),
            keyboard.getKeyboardId());

    setKeyboardForView(keyboard);

    // Restore voice key state to the new keyboard
    if (wasVoiceActive) {
      mCurrentAlphabetKeyboard.setVoice(wasVoiceActive, wasVoiceLocked);
    }
  }

  @Override
  public void onSymbolsKeyboardSet(@NonNull KeyboardDefinition keyboard) {
    mLastPrimaryInNonAlphabetKeyboard = 0; // initializing
    mCurrentSymbolsKeyboard = keyboard;
    mInAlphabetKeyboardMode = false;
    setKeyboardForView(keyboard);
  }

  @Override
  public void onAvailableKeyboardsChanged(@NonNull List<KeyboardAddOnAndBuilder> builders) {
    NskApplicationBase.getDeviceSpecific()
        .reportInputMethodSubtypes(getInputMethodManager(), getSettingsInputMethodId(), builders);
  }

  protected final boolean isInAlphabetKeyboardMode() {
    return mInAlphabetKeyboardMode;
  }

  /**
   * Returns the last set alphabet keyboard. Notice: this may be null if the keyboard was not loaded
   * it (say, in the start up of the IME service).
   */
  @Nullable
  protected final KeyboardDefinition getCurrentAlphabetKeyboard() {
    return mCurrentAlphabetKeyboard;
  }

  /**
   * Returns the last set symbols keyboard. Notice: this may be null if the keyboard was not loaded
   * it (say, in the start up of the IME service).
   */
  @Nullable
  protected final KeyboardDefinition getCurrentSymbolsKeyboard() {
    return mCurrentSymbolsKeyboard;
  }

  /**
   * Returns the last set symbols keyboard for the current mode (alphabet or symbols). Notice: this
   * may be null if the keyboard was not loaded it (say, in the start up of the IME service).
   */
  @Nullable
  protected final KeyboardDefinition getCurrentKeyboard() {
    return mInAlphabetKeyboardMode ? mCurrentAlphabetKeyboard : mCurrentSymbolsKeyboard;
  }

  protected void setKeyboardForView(@NonNull KeyboardDefinition keyboard) {
    final InputViewBinder inputView = getInputView();
    if (inputView != null) {
      inputView.setKeyboard(
          keyboard,
          mKeyboardSwitcher.peekNextAlphabetKeyboard(),
          mKeyboardSwitcher.peekNextSymbolsKeyboard());
      if (inputView instanceof KeyboardViewBase keyboardView) {
        keyboardView.setPackThemeOverride(resolvePackThemeOverrideOrNull(keyboard));
      }
    }
  }

  @Nullable
  private PackThemeOverride resolvePackThemeOverrideOrNull(@NonNull KeyboardDefinition keyboard) {
    String packId = extractPackIdOrNull(keyboard.getKeyboardId());
    if (packId == null) return null;

    String themeId = CustomKeyboardPrefs.getSelectedThemeIdForPack(getApplicationContext(), packId);
    if (TextUtils.isEmpty(themeId)) return null;

    try {
      InstalledKeyboardPack pack =
          new KeyboardPacksRepository(getApplicationContext()).findInstalledPackById(packId);
      if (pack == null) return null;

      PackEntry themeEntry = null;
      for (PackEntry entry : pack.manifest().themes()) {
        if (entry.id().equals(themeId)) {
          themeEntry = entry;
          break;
        }
      }
      if (themeEntry == null) return null;

      File themeFile = new File(pack.directory(), themeEntry.path().value());
      try (InputStream in = new FileInputStream(themeFile)) {
        ThemeModel model = ThemeXmlParser.parse(in);
        return new PackThemeOverride(pack.directory(), model);
      }
    } catch (IOException e) {
      Logger.w(TAG, "Failed applying pack theme for %s: %s", packId, e.getMessage());
      return null;
    }
  }

  @Nullable
  private static String extractPackIdOrNull(@NonNull String keyboardId) {
    final String prefix = "pack::";
    if (!keyboardId.startsWith(prefix)) return null;
    String rest = keyboardId.substring(prefix.length());
    int sep = rest.indexOf("::");
    if (sep <= 0) return null;
    return rest.substring(0, sep);
  }

  @Override
  protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
    super.onCurrentInputMethodSubtypeChanged(newSubtype);
    final String newSubtypeExtraValue = newSubtype.getExtraValue();
    if (TextUtils.isEmpty(newSubtypeExtraValue)) {
      return; // This might mean this isn't one of our subtypes (no extra-value keyboard id).
    }

    if (shouldConsumeSubtypeChangedEvent(newSubtypeExtraValue)) {
      mKeyboardSwitcher.nextAlphabetKeyboard(currentInputEditorInfo(), newSubtypeExtraValue);
    }
  }

  protected boolean shouldConsumeSubtypeChangedEvent(String newSubtypeExtraValue) {
    // 1) we are NOT waiting for an expected report
    // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/668
    // every time we change the alphabet keyboard, we want to OS to acknowledge
    // before we allow another subtype switch via event
    if (mExpectedSubtypeChangeKeyboardId != null) {
      if (TextUtils.equals(mExpectedSubtypeChangeKeyboardId, newSubtypeExtraValue)) {
        mExpectedSubtypeChangeKeyboardId = null; // got it!
      } else {
        // still waiting for the reported keyboard-id
        return false;
      }
    }
    // 2) current alphabet keyboard is null
    if (mCurrentAlphabetKeyboard == null) return true;
    // 3) (special - discarding) the requested subtype keyboard id is what we already have
    return !TextUtils.equals(newSubtypeExtraValue, mCurrentAlphabetKeyboard.getKeyboardId());
  }

  @Override
  protected void onSharedPreferenceChange(String key) {
    if (key.startsWith(Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX)) {
      mKeyboardSwitcher.flushKeyboardsCache();
    } else {
      super.onSharedPreferenceChange(key);
    }
  }

  @Override
  public View onCreateInputView() {
    View view = super.onCreateInputView();

    mKeyboardSwitcher.setInputView(getInputView());
    final KeyboardDefinition currentKeyboard = getCurrentKeyboard();
    if (currentKeyboard != null) {
      setKeyboardForView(currentKeyboard);
    }

    return view;
  }

  @Override
  @CallSuper
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    if (primaryCode == KeyCodes.SPACE) {
      // should we switch to alphabet keyboard?
      if (mSwitchKeyboardOnSpace
          && !mInAlphabetKeyboardMode
          && mLastPrimaryInNonAlphabetKeyboard != 0
          && mLastPrimaryInNonAlphabetKeyboard != KeyCodes.SPACE) {
        Logger.d(TAG, "SPACE while in symbols mode");
        getKeyboardSwitcher().nextKeyboard(currentInputEditorInfo(), NextKeyboardType.Alphabet);
      }
    }

    if (!mInAlphabetKeyboardMode && primaryCode > 0) {
      mLastPrimaryInNonAlphabetKeyboard = primaryCode;
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mKeyboardSwitcher.destroy();
    mKeyboardSwitcher = null;
  }
}
