package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import com.google.android.voiceime.utils.SpeechToTextSecretsStore;
import wtf.uhoh.newsoftkeyboard.R;

/** Stores speech-to-text API keys encrypted at rest (Android Keystore). */
public class SpeechToTextApiKeyPreference extends EditTextPreference {

  @Nullable private CharSequence mBaseSummary;

  public SpeechToTextApiKeyPreference(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public SpeechToTextApiKeyPreference(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SpeechToTextApiKeyPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public SpeechToTextApiKeyPreference(@NonNull Context context) {
    super(context);
  }

  @Override
  public void onAttached() {
    super.onAttached();
    if (mBaseSummary == null) {
      mBaseSummary = super.getSummary();
    }
    setSummaryProvider(
        preference -> {
          final Context context = getContext();
          final CharSequence baseSummary = mBaseSummary != null ? mBaseSummary : "";
          final int statusResId =
              hasApiKey(context, getKey())
                  ? R.string.speech_to_text_provider_status_configured
                  : R.string.speech_to_text_provider_status_missing_api_key;
          return baseSummary + "\n" + context.getString(statusResId);
        });
  }

  @Override
  protected void onClick() {
    final Context context = getContext();
    final String preferenceKey = getKey();
    final boolean hasApiKey = hasApiKey(context, preferenceKey);
    final EditText editText = new EditText(context);
    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(getDialogTitle())
        .setMessage(
            context.getString(
                hasApiKey
                    ? R.string.speech_to_text_api_key_dialog_message_key_saved
                    : R.string.speech_to_text_api_key_dialog_message_key_missing))
        .setView(editText)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              String candidate = editText.getText().toString().trim();
              if (TextUtils.isEmpty(candidate)) {
                return;
              }
              store(candidate);
            })
        .setNeutralButton(
            R.string.openai_prompt_clear_button,
            (dialog, which) -> {
              store("");
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void store(@NonNull String apiKey) {
    final Context context = getContext();
    final String key = getKey();
    if (TextUtils.equals(key, context.getString(R.string.settings_key_openai_api_key))) {
      SpeechToTextSecretsStore.setOpenAIApiKey(context, apiKey);
    } else if (TextUtils.equals(key, context.getString(R.string.settings_key_elevenlabs_api_key))) {
      SpeechToTextSecretsStore.setElevenLabsApiKey(context, apiKey);
    } else if (TextUtils.equals(key, context.getString(R.string.settings_key_groq_api_key))) {
      SpeechToTextSecretsStore.setGroqApiKey(context, apiKey);
    }

    // Remove any legacy plaintext value that may still exist in SharedPreferences.
    PreferenceManager.getDefaultSharedPreferences(context).edit().remove(key).apply();
    notifyChanged();
  }

  private static boolean hasApiKey(@NonNull Context context, @Nullable String preferenceKey) {
    if (preferenceKey == null) return false;
    final String trimmedPreferenceKey = preferenceKey.trim();
    if (trimmedPreferenceKey.isEmpty()) return false;

    final String openAiKeyName = context.getString(R.string.settings_key_openai_api_key);
    if (TextUtils.equals(trimmedPreferenceKey, openAiKeyName)) {
      final String openAiApiKey = SpeechToTextSecretsStore.getOpenAIApiKey(context);
      if (openAiApiKey != null && !openAiApiKey.trim().isEmpty()) return true;
    }

    final String elevenLabsKeyName = context.getString(R.string.settings_key_elevenlabs_api_key);
    if (TextUtils.equals(trimmedPreferenceKey, elevenLabsKeyName)) {
      final String elevenLabsApiKey = SpeechToTextSecretsStore.getElevenLabsApiKey(context);
      if (elevenLabsApiKey != null && !elevenLabsApiKey.trim().isEmpty()) return true;
    }

    final String groqKeyName = context.getString(R.string.settings_key_groq_api_key);
    if (TextUtils.equals(trimmedPreferenceKey, groqKeyName)) {
      final String groqApiKey = SpeechToTextSecretsStore.getGroqApiKey(context);
      if (groqApiKey != null && !groqApiKey.trim().isEmpty()) return true;
    }

    final String legacyValue =
        PreferenceManager.getDefaultSharedPreferences(context).getString(trimmedPreferenceKey, "");
    return legacyValue != null && !legacyValue.trim().isEmpty();
  }
}
