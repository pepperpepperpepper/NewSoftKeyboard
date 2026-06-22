package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.google.android.voiceime.backends.SpeechToTextBackend;
import com.google.android.voiceime.backends.SpeechToTextBackendRegistry;
import com.google.android.voiceime.utils.SpeechToTextSecretsStore;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

final class SpeechToTextSetupCardController {

  private static final String TAG = "SpeechToTextCard";

  private SpeechToTextSetupCardController() {}

  static void sync(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SpeechToTextBackend backend = SpeechToTextBackendRegistry.getSelectedBackend(context);

    if (backend == null) {
      Logger.d(TAG, "No speech-to-text backend selected; removing UI card if present.");
      new AddOnUICardManager(context).unregisterUICard(context.getPackageName());
      return;
    }

    boolean needsPermissions =
        needsMicrophonePermission(context) || needsNotificationPermission(context);
    String backendId = backend.getId();
    boolean needsApiConfiguration = needsApiKey(context, prefs, backendId);

    if (!needsPermissions && !needsApiConfiguration) {
      Logger.d(TAG, "Speech-to-text setup complete; removing UI card if present.");
      new AddOnUICardManager(context).unregisterUICard(context.getPackageName());
      return;
    }

    String title;
    int apiMessageRes;
    if (backendId.equals("elevenlabs")) {
      title = context.getString(R.string.elevenlabs_ui_card_title);
      apiMessageRes = R.string.elevenlabs_ui_card_message_api;
    } else if (backendId.equals("groq")) {
      title = context.getString(R.string.groq_ui_card_title);
      apiMessageRes = R.string.groq_ui_card_message_api;
    } else {
      title = context.getString(R.string.openai_ui_card_title);
      apiMessageRes = R.string.openai_ui_card_message_api;
    }
    String message = buildMessage(context, needsPermissions, needsApiConfiguration, apiMessageRes);
    AddOnUICard card = new AddOnUICard(context.getPackageName(), title, message, null);
    new AddOnUICardManager(context).registerUICard(card);
    Logger.d(
        TAG,
        "Registered speech-to-text setup UI card. permissionsNeeded="
            + needsPermissions
            + " apiConfigNeeded="
            + needsApiConfiguration);
  }

  private static String buildMessage(
      Context context,
      boolean includePermissions,
      boolean includeApiConfiguration,
      int apiMessageRes) {
    List<String> steps = new ArrayList<>();
    if (includePermissions) {
      steps.add(context.getString(R.string.openai_ui_card_message_permissions));
    }
    if (includeApiConfiguration) {
      steps.add(context.getString(apiMessageRes));
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < steps.size(); i++) {
      if (i > 0) {
        builder.append("<br/><br/>");
      }
      builder.append(i + 1).append(". ").append(steps.get(i));
    }
    return builder.toString();
  }

  private static boolean needsApiKey(Context context, SharedPreferences prefs, String backendId) {
    if ("elevenlabs".equals(backendId)) {
      String apiKey = SpeechToTextSecretsStore.getElevenLabsApiKey(context);
      if (!TextUtils.isEmpty(apiKey)) {
        return false;
      }
      String legacyKeyName = context.getString(R.string.settings_key_elevenlabs_api_key);
      String legacyApiKey = prefs.getString(legacyKeyName, "");
      if (!TextUtils.isEmpty(legacyApiKey)) {
        SpeechToTextSecretsStore.setElevenLabsApiKey(context, legacyApiKey);
        prefs.edit().remove(legacyKeyName).apply();
        return false;
      }
      return true;
    }
    if ("groq".equals(backendId)) {
      String apiKey = SpeechToTextSecretsStore.getGroqApiKey(context);
      if (!TextUtils.isEmpty(apiKey)) {
        return false;
      }
      String legacyKeyName = context.getString(R.string.settings_key_groq_api_key);
      String legacyApiKey = prefs.getString(legacyKeyName, "");
      if (!TextUtils.isEmpty(legacyApiKey)) {
        SpeechToTextSecretsStore.setGroqApiKey(context, legacyApiKey);
        prefs.edit().remove(legacyKeyName).apply();
        return false;
      }
      return true;
    }
    String apiKey = SpeechToTextSecretsStore.getOpenAIApiKey(context);
    if (!TextUtils.isEmpty(apiKey)) {
      return false;
    }
    String legacyKeyName = context.getString(R.string.settings_key_openai_api_key);
    String legacyApiKey = prefs.getString(legacyKeyName, "");
    if (!TextUtils.isEmpty(legacyApiKey)) {
      SpeechToTextSecretsStore.setOpenAIApiKey(context, legacyApiKey);
      prefs.edit().remove(legacyKeyName).apply();
      return false;
    }
    return true;
  }

  private static boolean needsMicrophonePermission(Context context) {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED;
  }

  private static boolean needsNotificationPermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED;
    }
    return !NotificationManagerCompat.from(context).areNotificationsEnabled();
  }
}
