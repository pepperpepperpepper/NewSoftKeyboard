package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class GroqSpeechSettingsFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_groq_speech);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.groq_speech_settings_title);
  }
}
