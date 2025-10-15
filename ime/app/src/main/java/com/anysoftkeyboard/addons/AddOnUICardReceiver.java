package com.anysoftkeyboard.addons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.anysoftkeyboard.ui.settings.AddOnUICard;
import com.anysoftkeyboard.ui.settings.AddOnUICardManager;

/**
 * Broadcast receiver that listens for UI card updates from add-ons
 */
public class AddOnUICardReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AddOnUICardReceiver";
    private static final String ACTION_UI_CARD_UPDATE = "com.anysoftkeyboard.UI_CARD_UPDATE";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with action: " + intent.getAction());
        if (ACTION_UI_CARD_UPDATE.equals(intent.getAction())) {
            Log.i(
                TAG,
                "Processing UI card broadcast from "
                    + intent.getStringExtra("addon_package")
                    + " extras="
                    + intent.getExtras());
            appendTrace(
                context,
                "Processing UI card from "
                    + intent.getStringExtra("addon_package")
                    + " action="
                    + intent.getStringExtra("action"));
            handleUICardUpdate(context, intent);
        } else {
            Log.d(TAG, "Ignoring unknown action: " + intent.getAction());
        }
    }
    
    private void handleUICardUpdate(Context context, Intent intent) {
        String addonPackage = intent.getStringExtra("addon_package");
        String action = intent.getStringExtra("action");
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String priority = intent.getStringExtra("priority");
        String targetFragment = intent.getStringExtra("target_fragment");
        
        Log.i(TAG, "Received UI card update from " + addonPackage + 
                   " - Action: " + action + ", Title: " + title + ", Message: " + message +
                   ", Target: " + targetFragment);
        
        if (addonPackage == null || action == null) {
            Log.w(TAG, "Invalid UI card update: missing required fields");
            return;
        }
        
        AddOnUICardManager manager = new AddOnUICardManager(context);
        
        if ("show_card".equals(action)) {
            if (title != null && message != null) {
                AddOnUICard card = new AddOnUICard(addonPackage, title, message, targetFragment);
                manager.registerUICard(card);
                appendTrace(context, "Registered card for " + addonPackage + " title=" + title);
                Log.d(TAG, "UI card registered for " + addonPackage);
            }
        } else if ("hide_card".equals(action)) {
            manager.unregisterUICard(addonPackage);
            appendTrace(context, "Unregistered card for " + addonPackage);
            Log.d(TAG, "UI card unregistered for " + addonPackage);
        }
    }

    private void appendTrace(Context context, String line) {
        File file = new File(context.getFilesDir(), "addon_ui_card_trace.log");
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.append(System.currentTimeMillis() + ": " + line + "\n");
        } catch (IOException e) {
            Log.w(TAG, "Failed writing trace log", e);
        }
    }
}
