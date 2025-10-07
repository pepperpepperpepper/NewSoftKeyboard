package com.anysoftkeyboard.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddOnUICardManager {
    private static final String TAG = "AddOnUICardManager";
    private static final String PREFS_NAME = "addon_ui_cards";
    private static final String REGISTERED_CARDS_KEY = "registered_cards";
    
    private final Context context;
    private final SharedPreferences preferences;
    
    public AddOnUICardManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void registerUICard(AddOnUICard card) {
        // Verify the add-on is actually installed
        try {
            context.getPackageManager().getPackageInfo(card.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Attempted to register UI card for uninstalled package: " + card.getPackageName());
            return;
        }
        
        Set<String> registeredCards = preferences.getStringSet(REGISTERED_CARDS_KEY, new HashSet<>());
        Set<String> updatedCards = new HashSet<>(registeredCards);
        
        // Store the card data as a JSON string
        String cardKey = card.getPackageName();
        String cardData = cardToJson(card);
        updatedCards.add(cardKey);
        
        preferences.edit()
                .putStringSet(REGISTERED_CARDS_KEY, updatedCards)
                .putString(cardKey, cardData)
                .apply();
        
        Log.d(TAG, "Registered UI card for package: " + card.getPackageName());
    }
    
    public void unregisterUICard(String packageName) {
        Set<String> registeredCards = preferences.getStringSet(REGISTERED_CARDS_KEY, new HashSet<>());
        Set<String> updatedCards = new HashSet<>(registeredCards);
        updatedCards.remove(packageName);
        
        preferences.edit()
                .putStringSet(REGISTERED_CARDS_KEY, updatedCards)
                .remove(packageName)
                .apply();
        
        Log.d(TAG, "Unregistered UI card for package: " + packageName);
    }
    
    public List<AddOnUICard> getActiveUICards() {
        List<AddOnUICard> cards = new ArrayList<>();
        Set<String> registeredCards = preferences.getStringSet(REGISTERED_CARDS_KEY, new HashSet<>());
        
        for (String packageName : registeredCards) {
            // Verify the package is still installed
            try {
                context.getPackageManager().getPackageInfo(packageName, 0);
                String cardData = preferences.getString(packageName, null);
                if (cardData != null) {
                    AddOnUICard card = jsonToCard(cardData);
                    if (card != null) {
                        cards.add(card);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Package was uninstalled, clean up
                unregisterUICard(packageName);
            }
        }
        
        return cards;
    }
    
    private String cardToJson(AddOnUICard card) {
        return card.getPackageName() + "|" + card.getTitle() + "|" + card.getMessage() + "|" + 
               (card.getTargetFragment() != null ? card.getTargetFragment() : "");
    }
    
    private AddOnUICard jsonToCard(String json) {
        String[] parts = json.split("\\|", 4);
        if (parts.length >= 3) {
            String targetFragment = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
            return new AddOnUICard(parts[0], parts[1], parts[2], targetFragment);
        }
        return null;
    }
}