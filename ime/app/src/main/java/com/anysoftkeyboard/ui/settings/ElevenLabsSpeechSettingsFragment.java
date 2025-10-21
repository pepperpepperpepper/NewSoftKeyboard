package com.anysoftkeyboard.ui.settings;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import com.menny.android.anysoftkeyboard.R;
import net.evendanan.pixel.UiUtils;

public class ElevenLabsSpeechSettingsFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_elevenlabs_speech);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.elevenlabs_speech_settings_title);
  }
}
