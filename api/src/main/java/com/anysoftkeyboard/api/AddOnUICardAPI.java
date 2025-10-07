package com.anysoftkeyboard.api;

/**
 * Public API for add-ons to register UI cards in AnySoftKeyboard's main settings screen.
 * Add-ons can broadcast these intents to register/unregister UI cards.
 */
public class AddOnUICardAPI {
    
    /**
     * Action to register a UI card for an add-on.
     * Use with sendBroadcast() to register a card that will appear in the main settings.
     */
    public static final String ACTION_REGISTER_UI_CARD = "com.anysoftkeyboard.action.REGISTER_UI_CARD";
    
    /**
     * Action to unregister a UI card for an add-on.
     * Use with sendBroadcast() to remove a previously registered card.
     */
    public static final String ACTION_UNREGISTER_UI_CARD = "com.anysoftkeyboard.action.UNREGISTER_UI_CARD";
    
    /**
     * Extra: The package name of the add-on registering the card.
     * Required for both register and unregister actions.
     */
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    
    /**
     * Extra: The title to display on the UI card.
     * Required for register action.
     */
    public static final String EXTRA_TITLE = "title";
    
    /**
     * Extra: The message to display on the UI card.
     * Required for register action.
     */
    public static final String EXTRA_MESSAGE = "message";
    
    /**
     * Extra: The target fragment to navigate to when the card is clicked.
     * Optional for register action. If not provided, card will not be clickable.
     * Example: "userInterfaceSettingsFragment"
     */
    public static final String EXTRA_TARGET_FRAGMENT = "target_fragment";
}