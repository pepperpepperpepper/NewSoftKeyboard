package com.anysoftkeyboard.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;
import com.anysoftkeyboard.base.utils.Logger;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;

final class OpenAIVoiceSetupCardController {

    private static final String TAG = "OpenAIVoiceCard";

    private OpenAIVoiceSetupCardController() {}

    static void sync(Context context) {
        boolean needsPermissions =
                needsMicrophonePermission(context) || needsNotificationPermission(context);
        boolean needsApiConfiguration = needsApiKey(context);

        if (!needsPermissions && !needsApiConfiguration) {
            Logger.d(TAG, "OpenAI setup complete; removing UI card if present.");
            new AddOnUICardManager(context).unregisterUICard(context.getPackageName());
            return;
        }

        String title = context.getString(R.string.openai_ui_card_title);
        String message = buildMessage(context, needsPermissions, needsApiConfiguration);
        AddOnUICard card =
                new AddOnUICard(context.getPackageName(), title, message, null);
        new AddOnUICardManager(context).registerUICard(card);
        Logger.d(
                TAG,
                "Registered OpenAI setup UI card. permissionsNeeded="
                        + needsPermissions
                        + " apiConfigNeeded="
                        + needsApiConfiguration);
        Log.d(
                TAG,
                "Registered OpenAI setup UI card message="
                        + message);
    }

    private static String buildMessage(
            Context context, boolean includePermissions, boolean includeApiConfiguration) {
        List<String> steps = new ArrayList<>();
        if (includePermissions) {
            steps.add(context.getString(R.string.openai_ui_card_message_permissions));
        }
        if (includeApiConfiguration) {
            steps.add(context.getString(R.string.openai_ui_card_message_api));
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

    private static boolean needsApiKey(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.settings_key_openai_api_key);
        String apiKey = prefs.getString(key, "");
        return TextUtils.isEmpty(apiKey);
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
