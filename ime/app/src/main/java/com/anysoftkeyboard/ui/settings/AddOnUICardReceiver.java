package com.anysoftkeyboard.ui.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.anysoftkeyboard.api.AddOnUICardAPI;

public class AddOnUICardReceiver extends BroadcastReceiver {
    private static final String TAG = "AddOnUICardReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        
        AddOnUICardManager manager = new AddOnUICardManager(context);
        
        switch (action) {
            case AddOnUICardAPI.ACTION_REGISTER_UI_CARD:
                handleRegisterUICard(context, intent, manager);
                break;
            case AddOnUICardAPI.ACTION_UNREGISTER_UI_CARD:
                handleUnregisterUICard(intent, manager);
                break;
        }
    }
    
    private void handleRegisterUICard(Context context, Intent intent, AddOnUICardManager manager) {
        String packageName = intent.getStringExtra(AddOnUICardAPI.EXTRA_PACKAGE_NAME);
        String title = intent.getStringExtra(AddOnUICardAPI.EXTRA_TITLE);
        String message = intent.getStringExtra(AddOnUICardAPI.EXTRA_MESSAGE);
        String targetFragment = intent.getStringExtra(AddOnUICardAPI.EXTRA_TARGET_FRAGMENT);
        
        if (packageName == null || title == null || message == null) {
            Log.w(TAG, "Missing required extras for UI card registration");
            return;
        }
        
        AddOnUICard card = new AddOnUICard(packageName, title, message, targetFragment);
        manager.registerUICard(card);
        
        Log.d(TAG, "Registered UI card: " + title + " from " + packageName);
    }
    
    private void handleUnregisterUICard(Intent intent, AddOnUICardManager manager) {
        String packageName = intent.getStringExtra(AddOnUICardAPI.EXTRA_PACKAGE_NAME);
        if (packageName == null) {
            Log.w(TAG, "Missing package name for UI card unregistration");
            return;
        }
        
        manager.unregisterUICard(packageName);
        Log.d(TAG, "Unregistered UI card from: " + packageName);
    }
}