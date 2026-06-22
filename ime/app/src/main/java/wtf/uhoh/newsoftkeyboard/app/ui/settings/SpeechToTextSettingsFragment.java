package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.voiceime.backends.SpeechToTextBackend;
import com.google.android.voiceime.backends.SpeechToTextBackendRegistry;
import java.util.List;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class SpeechToTextSettingsFragment extends PreferenceFragmentCompat {

  private ListPreference mBackendPreference;
  private Preference mOpenAiPreference;
  private Preference mElevenLabsPreference;
  private Preference mGroqPreference;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_speech_to_text);

    mBackendPreference = findPreference(getString(R.string.settings_key_speech_to_text_backend));
    mOpenAiPreference = findPreference("speech_to_text_openai_settings");
    mElevenLabsPreference = findPreference("speech_to_text_elevenlabs_settings");
    mGroqPreference = findPreference("speech_to_text_groq_settings");

    ensureBackendSelectionInitialized();
    bindPreferenceHandlers();
    updateBackendSummaries();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    UiUtils.setActivityTitle(this, R.string.speech_to_text_settings_title);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateBackendSummaries();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void ensureBackendSelectionInitialized() {
    if (mBackendPreference == null) {
      return;
    }
    String currentValue = mBackendPreference.getValue();
    if (currentValue == null || currentValue.isEmpty()) {
      boolean legacyOpenAiEnabled =
          mBackendPreference
              .getSharedPreferences()
              .getBoolean(getString(R.string.settings_key_openai_enabled), false);
      if (legacyOpenAiEnabled) {
        mBackendPreference.setValue("openai");
      } else {
        mBackendPreference.setValue("none");
      }
    }
  }

  private void bindPreferenceHandlers() {
    if (mBackendPreference != null) {
      mBackendPreference.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            String selection = String.valueOf(newValue);
            // Audio leaves the device only for third-party backends. Require explicit, per-provider
            // consent before persisting such a selection (Play sensitive-data / prominent-disclosure
            // policy). "none" needs no consent.
            if (isThirdPartyBackend(selection) && !hasConsentFor(selection)) {
              showConsentDialog(selection);
              return false;
            }
            applyBackendSelection(selection);
            return true;
          });
    }

    if (mOpenAiPreference != null) {
      mOpenAiPreference.setOnPreferenceClickListener(
          preference -> {
            Navigation.findNavController(requireView()).navigate(R.id.openAISpeechSettingsFragment);
            return true;
          });
    }

    if (mElevenLabsPreference != null) {
      mElevenLabsPreference.setOnPreferenceClickListener(
          preference -> {
            Navigation.findNavController(requireView())
                .navigate(R.id.elevenLabsSpeechSettingsFragment);
            return true;
          });
    }

    if (mGroqPreference != null) {
      mGroqPreference.setOnPreferenceClickListener(
          preference -> {
            Navigation.findNavController(requireView()).navigate(R.id.groqSpeechSettingsFragment);
            return true;
          });
    }
  }

  private static boolean isThirdPartyBackend(String selection) {
    return "openai".equals(selection)
        || "elevenlabs".equals(selection)
        || "groq".equals(selection);
  }

  private boolean hasConsentFor(String selection) {
    if (mBackendPreference == null) {
      return false;
    }
    String consented =
        mBackendPreference
            .getSharedPreferences()
            .getString(getString(R.string.settings_key_speech_to_text_consented_backend), "");
    return selection.equals(consented);
  }

  private String backendDisplayName(String selection) {
    if ("openai".equals(selection)) {
      return getString(R.string.speech_to_text_backend_entry_openai);
    }
    if ("elevenlabs".equals(selection)) {
      return getString(R.string.speech_to_text_backend_entry_elevenlabs);
    }
    if ("groq".equals(selection)) {
      return getString(R.string.speech_to_text_backend_entry_groq);
    }
    return selection;
  }

  private void showConsentDialog(String selection) {
    final String provider = backendDisplayName(selection);
    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.speech_to_text_consent_title, provider))
        .setMessage(getString(R.string.speech_to_text_consent_message, provider))
        .setPositiveButton(
            R.string.speech_to_text_consent_accept,
            (dialog, which) -> {
              mBackendPreference
                  .getSharedPreferences()
                  .edit()
                  .putString(
                      getString(R.string.settings_key_speech_to_text_consented_backend), selection)
                  .apply();
              // setValue() persists directly and does not re-fire the change listener.
              mBackendPreference.setValue(selection);
              applyBackendSelection(selection);
            })
        .setNegativeButton(R.string.speech_to_text_consent_decline, null)
        .show();
  }

  private void applyBackendSelection(String selection) {
    propagateLegacyFlags(selection);
    updateBackendSummaries(selection);
  }

  private void propagateLegacyFlags(String selection) {
    if (mBackendPreference == null) {
      return;
    }
    boolean enableOpenAi = "openai".equals(selection);
    mBackendPreference
        .getSharedPreferences()
        .edit()
        .putBoolean(getString(R.string.settings_key_openai_enabled), enableOpenAi)
        .apply();
  }

  private void updateBackendSummaries() {
    updateBackendSummaries(mBackendPreference != null ? mBackendPreference.getValue() : "none");
  }

  private void updateBackendSummaries(String selectedBackendId) {
    if (mOpenAiPreference == null
        || mElevenLabsPreference == null
        || mGroqPreference == null
        || mBackendPreference == null) {
      return;
    }
    List<SpeechToTextBackend> backends = SpeechToTextBackendRegistry.getBackends();
    boolean openAiConfigured = false;
    boolean elevenLabsConfigured = false;
    boolean groqConfigured = false;
    for (SpeechToTextBackend backend : backends) {
      if ("openai".equals(backend.getId())) {
        openAiConfigured =
            backend.isConfigured(requireContext(), mBackendPreference.getSharedPreferences());
      } else if ("elevenlabs".equals(backend.getId())) {
        elevenLabsConfigured =
            backend.isConfigured(requireContext(), mBackendPreference.getSharedPreferences());
      } else if ("groq".equals(backend.getId())) {
        groqConfigured =
            backend.isConfigured(requireContext(), mBackendPreference.getSharedPreferences());
      }
    }

    mOpenAiPreference.setSummary(
        buildStatusSummary(
            getString(R.string.openai_speech_settings_summary),
            selectedBackendId,
            "openai",
            openAiConfigured));

    mElevenLabsPreference.setSummary(
        buildStatusSummary(
            getString(R.string.elevenlabs_speech_settings_summary),
            selectedBackendId,
            "elevenlabs",
            elevenLabsConfigured));

    mGroqPreference.setSummary(
        buildStatusSummary(
            getString(R.string.groq_speech_settings_summary),
            selectedBackendId,
            "groq",
            groqConfigured));
  }

  private CharSequence buildStatusSummary(
      String baseSummary, String selectedBackendId, String backendId, boolean configured) {
    if (backendId.equals(selectedBackendId)) {
      return baseSummary
          + "\n"
          + getString(
              configured
                  ? R.string.speech_to_text_provider_status_configured
                  : R.string.speech_to_text_provider_status_missing_api_key);
    }
    return baseSummary + "\n" + getString(R.string.speech_to_text_provider_status_not_selected);
  }

  private void scrollToRequestedPreferenceIfNeeded() {
    final Bundle args = getArguments();
    if (args == null) return;
    final String key = args.getString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
    if (TextUtils.isEmpty(key)) return;
    scrollToPreference(key);
    args.remove(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
  }
}
