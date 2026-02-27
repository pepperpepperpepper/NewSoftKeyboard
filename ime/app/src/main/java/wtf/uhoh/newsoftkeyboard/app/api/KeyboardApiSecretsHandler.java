package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.anysoftkeyboard.api.KeyboardApiContract;
import com.google.android.voiceime.utils.SpeechToTextSecretsStore;
import wtf.uhoh.newsoftkeyboard.R;

final class KeyboardApiSecretsHandler {

  private KeyboardApiSecretsHandler() {}

  @NonNull
  static Bundle setSecret(@NonNull Context context, @NonNull Bundle extras) {
    final String secretIdRaw = extras.getString(KeyboardApiContract.EXTRA_SECRET_ID);
    final String secretId = secretIdRaw == null ? null : secretIdRaw.trim();
    if (secretId == null || secretId.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing secret_id");
    }

    if (!extras.containsKey(KeyboardApiContract.EXTRA_SECRET_VALUE)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing secret_value");
    }
    final Object secretObj = extras.get(KeyboardApiContract.EXTRA_SECRET_VALUE);
    if (!(secretObj instanceof String)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "secret_value must be a string");
    }
    final String secretValue = ((String) secretObj).trim();
    if (secretValue.length() > 4096) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "secret_value too long");
    }

    switch (secretId) {
      case KeyboardApiContract.SECRET_ID_OPENAI_API_KEY:
        SpeechToTextSecretsStore.setOpenAIApiKey(context, secretValue);
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(context.getString(R.string.settings_key_openai_api_key))
            .apply();
        break;
      case KeyboardApiContract.SECRET_ID_ELEVENLABS_API_KEY:
        SpeechToTextSecretsStore.setElevenLabsApiKey(context, secretValue);
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(context.getString(R.string.settings_key_elevenlabs_api_key))
            .apply();
        break;
      default:
        return KeyboardApiCallSupport.error(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown secret_id");
    }

    final boolean hasSecret = hasSecret(context, secretId);
    final Bundle out = KeyboardApiCallSupport.ok();
    out.putString(KeyboardApiContract.EXTRA_SECRET_ID, secretId);
    out.putBoolean(KeyboardApiContract.EXTRA_HAS_SECRET, hasSecret);
    return out;
  }

  @NonNull
  static Bundle getSecretStatus(@NonNull Context context, @NonNull Bundle extras) {
    final String secretIdRaw = extras.getString(KeyboardApiContract.EXTRA_SECRET_ID);
    final String secretId = secretIdRaw == null ? null : secretIdRaw.trim();
    if (secretId == null || secretId.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing secret_id");
    }
    if (!KeyboardApiContract.SECRET_ID_OPENAI_API_KEY.equals(secretId)
        && !KeyboardApiContract.SECRET_ID_ELEVENLABS_API_KEY.equals(secretId)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown secret_id");
    }

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putString(KeyboardApiContract.EXTRA_SECRET_ID, secretId);
    out.putBoolean(KeyboardApiContract.EXTRA_HAS_SECRET, hasSecret(context, secretId));
    return out;
  }

  private static boolean hasSecret(@NonNull Context context, @NonNull String secretId) {
    switch (secretId) {
      case KeyboardApiContract.SECRET_ID_OPENAI_API_KEY:
        return SpeechToTextSecretsStore.hasOpenAIApiKey(context);
      case KeyboardApiContract.SECRET_ID_ELEVENLABS_API_KEY:
        return SpeechToTextSecretsStore.hasElevenLabsApiKey(context);
      default:
        return false;
    }
  }
}
