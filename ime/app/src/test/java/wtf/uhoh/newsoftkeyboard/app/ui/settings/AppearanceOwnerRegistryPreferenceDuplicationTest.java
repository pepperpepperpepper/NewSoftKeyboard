package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class AppearanceOwnerRegistryPreferenceDuplicationTest {

  @Test
  public void testSettingsKeyPreferencesAreStatusOnlyOutsideOwner() {
    final Context context = ApplicationProvider.getApplicationContext();

    final Map<String, AppearanceOwnerRegistry.Owner> ownersById = new HashMap<>();
    for (AppearanceOwnerRegistry.Owner owner : AppearanceOwnerRegistry.owners()) {
      if (owner.canonicalId.startsWith("settings_key_")) {
        ownersById.put(owner.canonicalId, owner);
      }
    }

    final Map<Integer, Integer> destinationForXml = new HashMap<>();
    destinationForXml.put(
        R.xml.prefs_keyboards_language_packs, R.id.keyboardsAndLanguagePacksFragment);
    destinationForXml.put(R.xml.prefs_typing_settings, R.id.typingSettingsFragment);
    destinationForXml.put(R.xml.prefs_look_and_feel_settings, R.id.lookAndFeelSettingsFragment);
    destinationForXml.put(
        R.xml.prefs_gestures_and_quick_keys_settings, R.id.gesturesAndQuickKeysSettingsFragment);
    destinationForXml.put(R.xml.prefs_clipboard_settings, R.id.clipboardSettingsFragment);
    destinationForXml.put(R.xml.prefs_speech_to_text, R.id.voiceSettingsFragment);
    destinationForXml.put(
        R.xml.prefs_troubleshooting_backup_settings, R.id.troubleshootingAndBackupSettingsFragment);
    destinationForXml.put(R.xml.prefs_settings_ui_launcher, R.id.settingsUiAndLauncherFragment);
    destinationForXml.put(R.xml.prefs_help_about, R.id.helpAndAboutFragment);
    destinationForXml.put(R.xml.prefs_next_word, R.id.nextWordSettingsFragment);
    destinationForXml.put(R.xml.prefs_presage_models, R.id.presageModelsFragment);
    destinationForXml.put(R.xml.prefs_openai_speech, R.id.openAISpeechSettingsFragment);
    destinationForXml.put(R.xml.prefs_elevenlabs_speech, R.id.elevenLabsSpeechSettingsFragment);
    destinationForXml.put(R.xml.night_mode_prefs, R.id.nightModeSettingsFragment);
    destinationForXml.put(R.xml.power_saving_prefs, R.id.powerSavingSettingsFragment);

    final PreferenceManager preferenceManager = new PreferenceManager(context);
    for (Map.Entry<Integer, Integer> entry : destinationForXml.entrySet()) {
      final int xmlResId = entry.getKey();
      final int destinationId = entry.getValue();

      final PreferenceScreen screen =
          preferenceManager.inflateFromResource(context, xmlResId, /* rootPreferences= */ null);

      forEachPreference(
          screen,
          pref -> {
            final String key = pref.getKey();
            if (key == null) return;

            final AppearanceOwnerRegistry.Owner owner = ownersById.get(key);
            if (owner == null) return;
            if (owner.ownerDestinationId == destinationId) return;

            Assert.assertFalse(
                "Preference with key "
                    + key
                    + " must be status-only outside its owner screen. Found TwoStatePreference in "
                    + xmlResId,
                pref instanceof TwoStatePreference);
            Assert.assertFalse(
                "Preference with key "
                    + key
                    + " must be status-only outside its owner screen. Found DialogPreference in "
                    + xmlResId,
                pref instanceof DialogPreference);
          });
    }
  }

  private static void forEachPreference(
      Preference preference, java.util.function.Consumer<Preference> consumer) {
    consumer.accept(preference);
    if (!(preference instanceof PreferenceGroup group)) return;
    for (int i = 0; i < group.getPreferenceCount(); i++) {
      forEachPreference(group.getPreference(i), consumer);
    }
  }
}
