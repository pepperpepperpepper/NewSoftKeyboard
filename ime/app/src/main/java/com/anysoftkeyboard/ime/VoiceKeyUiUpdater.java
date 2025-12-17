package com.anysoftkeyboard.ime;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;

/** Handles updating the voice key UI state on the current input view. */
public final class VoiceKeyUiUpdater {

  private static final String TAG = "NSK-VoiceKey";

  public void applyState(@Nullable InputViewBinder inputView, boolean active, boolean locked) {
    if (inputView == null) return;
    Logger.d(TAG, "voice Setting UI active:%s, locked: %s", active, locked);
    inputView.setVoice(active, locked);
  }
}
