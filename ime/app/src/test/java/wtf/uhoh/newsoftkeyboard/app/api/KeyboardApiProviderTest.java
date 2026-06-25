package wtf.uhoh.newsoftkeyboard.app.api;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ContentProviderController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowSystemClock;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardApiProviderTest {

  private KeyboardApiProvider mProvider;
  private ContentProviderController<KeyboardApiProvider> mProviderController;
  private ServiceController<TestableImeService> mImeServiceController;
  private TestableImeService mImeServiceUnderTest;

  @Before
  public void setup() {
    KeyboardApiRateLimiter.resetForTests();
    mProvider = new KeyboardApiProvider();
    mProviderController = ContentProviderController.of(mProvider);
    mProviderController.create("wtf.uhoh.newsoftkeyboard.keyboardapi");
  }

  @After
  public void tearDown() {
    mProviderController.shutdown();
    if (mImeServiceController != null) {
      mImeServiceController.destroy();
      mImeServiceController = null;
      mImeServiceUnderTest = null;
    }
  }

  @Test
  public void deniesWhenDisabled() {
    final Bundle out =
        mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_PING, null, null);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setPreferenceRejectsUnknownKey() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY, "not_a_real_key");
    extras.putBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL, true);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_PREF_NOT_ALLOWED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setPreferenceRejectsOutOfRangeInt() {
    enableApi();
    final Context context = getApplicationContext();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY,
        context.getString(R.string.settings_key_vibrate_on_key_press_duration_int));
    extras.putInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_INT, 9999);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void openSettingsRejectsUnknownDestination() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_DESTINATION_ID, "nav:unknown");

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_OPEN_SETTINGS, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void openSettingsSupportsKnownDestination() {
    enableApi();
    final Context context = getApplicationContext();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_DESTINATION_ID,
        KeyboardApiSettingsDeepLinks.DESTINATION_KEYBOARDS_MANAGER);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_OPEN_SETTINGS, null, extras);
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        context.getString(R.string.deeplink_url_keyboards),
        out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_DEEPLINK_URI));
  }

  @Test
  public void openSettingsSupportsScrollToPrefKey() {
    enableApi();
    final Context context = getApplicationContext();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_DESTINATION_ID,
        KeyboardApiSettingsDeepLinks.DESTINATION_CATEGORY_TYPING);
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY,
        context.getString(R.string.settings_key_auto_capitalization));

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_OPEN_SETTINGS, null, extras);
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        context.getString(R.string.deeplink_url_typing),
        out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_DEEPLINK_URI));
    Assert.assertEquals(
        context.getString(R.string.settings_key_auto_capitalization),
        out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY));
  }

  @Test
  public void getCapabilitiesIncludesSessionOverrides() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_CAPABILITIES,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final ArrayList<String> methods =
        out.getStringArrayList(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SUPPORTED_METHODS);
    Assert.assertNotNull(methods);
    Assert.assertTrue(
        methods.contains(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET));
    Assert.assertTrue(
        methods.contains(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_THEME_PRESET));
    Assert.assertTrue(
        methods.contains(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_KEYBOARD_ID));
    Assert.assertTrue(
        methods.contains(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_SESSION_OVERRIDES));

    final ArrayList<String> scopes =
        out.getStringArrayList(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SUPPORTED_SCOPES);
    Assert.assertNotNull(scopes);
    Assert.assertTrue(
        scopes.contains(com.anysoftkeyboard.api.KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET));
    Assert.assertTrue(
        scopes.contains(com.anysoftkeyboard.api.KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME));
    Assert.assertTrue(
        scopes.contains(com.anysoftkeyboard.api.KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT));
  }

  @Test
  public void clearSessionOverridesRejectsNonFocusedCallerWhenAutomationControllersDisabled() {
    enableApi();
    startImeForEditorPackage("com.example.target");

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_SESSION_OVERRIDES,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void clearSessionOverridesAllowsNonFocusedCallerWhenAutomationControllerAllowListed() {
    enableApi();
    enableHighRiskActions();
    enableAutomationControllers();

    final Context context = getApplicationContext();
    final String targetPackage = "com.example.target";
    new KeyboardApiPrefs(context)
        .setAllowedSessionTargetPackages(
            context.getPackageName(), Collections.singleton(targetPackage));

    startImeForEditorPackage(targetPackage);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_SESSION_OVERRIDES,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
  }

  @Test
  public void setSessionPresetRejectsNonAllowListedId() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SESSION_PRESET_ID, "not_allow_listed");
    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_SCOPE_DENIED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setSessionPresetRejectsDisallowedContext() {
    enableApi();

    final Context context = getApplicationContext();
    final String presetId = "context_preset::test";
    new KeyboardApiPrefs(context)
        .setAllowedSessionPresetIds(context.getPackageName(), Collections.singleton(presetId));

    startImeForEditorPackage(context.getPackageName());
    mImeServiceUnderTest.setIncognito(true, true);

    final Bundle extras = new Bundle();
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SESSION_PRESET_ID, presetId);
    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_DISALLOWED_CONTEXT,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setSessionPresetRateLimitsUiActions() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SESSION_PRESET_ID, "irrelevant");

    final Bundle first =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET, null, extras);
    Assert.assertNotNull(first);

    final Bundle second =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET, null, extras);
    Assert.assertNotNull(second);
    Assert.assertFalse(second.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_RATE_LIMITED,
        second.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
    Assert.assertTrue(
        second.getLong(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_RETRY_AFTER_MS, 0L) > 0L);
  }

  @Test
  public void auditLogRecordsCalls() {
    enableApi();

    final Bundle pingOut =
        mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_PING, null, null);
    Assert.assertNotNull(pingOut);
    Assert.assertTrue(pingOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final Bundle auditExtras = new Bundle();
    auditExtras.putInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_AUDIT_LIMIT, 25);
    final Bundle auditOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_AUDIT_LOG, null, auditExtras);
    Assert.assertNotNull(auditOut);
    Assert.assertTrue(auditOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final ArrayList<String> entries =
        auditOut.getStringArrayList(
            com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_AUDIT_ENTRIES);
    Assert.assertNotNull(entries);
    boolean found = false;
    for (String e : entries) {
      if (e != null && e.contains("\tping\t")) {
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);
  }

  @Test
  public void auditLogDoesNotIncludeSessionPresetId() {
    enableApi();

    final Bundle clearOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_AUDIT_LOG, null, new Bundle());
    Assert.assertNotNull(clearOut);
    Assert.assertTrue(clearOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final String sessionPresetId = "secret-session-preset-id";
    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SESSION_PRESET_ID, sessionPresetId);
    final Bundle setOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET, null, extras);
    Assert.assertNotNull(setOut);

    final Bundle auditExtras = new Bundle();
    auditExtras.putInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_AUDIT_LIMIT, 100);
    final Bundle auditOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_AUDIT_LOG, null, auditExtras);
    Assert.assertNotNull(auditOut);
    Assert.assertTrue(auditOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final ArrayList<String> entries =
        auditOut.getStringArrayList(
            com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_AUDIT_ENTRIES);
    Assert.assertNotNull(entries);
    boolean found = false;
    for (String e : entries) {
      if (e != null
          && e.contains(
              "\t"
                  + com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SESSION_PRESET
                  + "\t")) {
        found = true;
      }
      Assert.assertFalse(e != null && e.contains(sessionPresetId));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void rateLimitsBurstCalls() {
    enableApi();
    ShadowSystemClock.advanceBy(Duration.ofMillis(1));

    for (int i = 0; i < 25; i++) {
      final Bundle out =
          mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_PING, null, null);
      Assert.assertNotNull(out);
      Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    }

    final Bundle out =
        mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_PING, null, null);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_RATE_LIMITED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
    Assert.assertTrue(
        out.getLong(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_RETRY_AFTER_MS, 0L) > 0L);
  }

  @Test
  public void toggleIncognitoRequiresImeActive() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_TOGGLE_INCOGNITO,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_IME_NOT_ACTIVE,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void switchLanguageRequiresImeActive() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SWITCH_LANGUAGE, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_IME_NOT_ACTIVE,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void switchKeyboardModeRequiresImeActive() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SWITCH_KEYBOARD_MODE,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_IME_NOT_ACTIVE,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void sendNavigationKeyRequiresHighRiskToggle() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_NAVIGATION_KEY,
        com.anysoftkeyboard.api.KeyboardApiContract.NAVIGATION_KEY_LEFT);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_HIGH_RISK_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void sendNavigationKeyRequiresImeActiveWhenHighRiskEnabled() {
    enableApi();
    enableHighRiskActions();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_NAVIGATION_KEY,
        com.anysoftkeyboard.api.KeyboardApiContract.NAVIGATION_KEY_LEFT);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_IME_NOT_ACTIVE,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void sendNavigationKeyValidatesArgumentsBeforeImeActive() {
    enableApi();
    enableHighRiskActions();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_NAVIGATION_KEY, "not_a_real_key");

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void sendTabRequiresHighRiskToggle() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SEND_TAB, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_HIGH_RISK_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void sendEscapeRequiresHighRiskToggle() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SEND_ESCAPE, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_HIGH_RISK_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void clipboardPasteRequiresHighRiskToggle() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLIPBOARD_PASTE, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_HIGH_RISK_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void clipboardCopyRequiresCopyCutToggleWhenHighRiskEnabled() {
    enableApi();
    enableHighRiskActions();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLIPBOARD_COPY, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_CLIPBOARD_COPY_CUT_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void clipboardCopyRequiresImeActiveWhenTogglesEnabled() {
    enableApi();
    enableHighRiskActions();
    enableClipboardCopyCut();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLIPBOARD_COPY, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_IME_NOT_ACTIVE,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void runSnippetRequiresHighRiskToggle() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SNIPPET_ID, "irrelevant");
    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_SNIPPET, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_HIGH_RISK_DISABLED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void runSnippetRequiresSnippetId() {
    enableApi();
    enableHighRiskActions();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_SNIPPET, null, new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setSecretRejectsMissingSecretId() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SECRET_VALUE, "not_a_real_key");
    final Bundle out =
        mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SECRET, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setSecretRejectsUnknownSecretId() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SECRET_ID, "unknown_secret");
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SECRET_VALUE, "value");
    final Bundle out =
        mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SECRET, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void getSecretStatusRejectsUnknownSecretId() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SECRET_ID, "unknown_secret");
    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_SECRET_STATUS, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void openMediaInsertionUiRequiresImeActive() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_OPEN_MEDIA_INSERTION_UI,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_IME_NOT_ACTIVE,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void setPreferenceSupportsTypingPrefs() {
    enableApi();
    final Context context = getApplicationContext();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY,
        context.getString(R.string.settings_key_auto_capitalization));
    extras.putBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL, true);
    final Bundle setOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE, null, extras);
    Assert.assertNotNull(setOut);
    Assert.assertTrue(setOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final Bundle getExtras = new Bundle();
    getExtras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY,
        context.getString(R.string.settings_key_auto_capitalization));
    final Bundle getOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_PREFERENCE, null, getExtras);
    Assert.assertNotNull(getOut);
    Assert.assertTrue(getOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertTrue(
        getOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL));
  }

  @Test
  public void setPreferenceSupportsGestureStringPrefs() {
    enableApi();
    final Context context = getApplicationContext();

    final String key = context.getString(R.string.settings_key_swipe_up_action);
    final String[] allowedValues =
        context.getResources().getStringArray(R.array.swipe_action_types_values);
    Assert.assertNotNull(allowedValues);
    Assert.assertTrue(allowedValues.length > 0);
    final String value = allowedValues[0];

    final Bundle setExtras = new Bundle();
    setExtras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY, key);
    setExtras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_STRING, value);
    final Bundle setOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE, null, setExtras);
    Assert.assertNotNull(setOut);
    Assert.assertTrue(setOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final Bundle getExtras = new Bundle();
    getExtras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY, key);
    final Bundle getOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_PREFERENCE, null, getExtras);
    Assert.assertNotNull(getOut);
    Assert.assertTrue(getOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        value, getOut.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_STRING));
  }

  @Test
  public void setPreferenceRejectsDisallowedStringValue() {
    enableApi();
    final Context context = getApplicationContext();

    final Bundle setExtras = new Bundle();
    setExtras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY,
        context.getString(R.string.settings_key_swipe_up_action));
    setExtras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_STRING, "not_allowed");
    final Bundle setOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE, null, setExtras);
    Assert.assertNotNull(setOut);
    Assert.assertFalse(setOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        setOut.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void clearQuickTextHistoryClearsPreference() {
    enableApi();
    final Context context = getApplicationContext();

    final String key = context.getString(R.string.settings_key_quick_text_history);
    wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context)
        .edit()
        .putString(key, "some-history")
        .apply();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_QUICK_TEXT_HISTORY,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    Assert.assertFalse(
        wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context)
            .contains(key));
  }

  @Test
  public void clearLearningDataDeletesNextWordFilesAndAutoDictionaryDbWhenImeNotActive()
      throws IOException {
    enableApi();
    final Context context = getApplicationContext();

    try (FileOutputStream out =
        context.openFileOutput("next_words_test_locale.txt", Context.MODE_PRIVATE)) {
      out.write(1);
      out.flush();
    }

    context.openOrCreateDatabase("auto_dict_2.db", Context.MODE_PRIVATE, null).close();
    Assert.assertTrue(context.getDatabasePath("auto_dict_2.db").exists());

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_LEARNING_DATA,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    Assert.assertFalse(context.getFileStreamPath("next_words_test_locale.txt").exists());
    Assert.assertFalse(context.getDatabasePath("auto_dict_2.db").exists());
  }

  @Test
  public void clearClipboardHistorySucceedsWhenImeNotActive() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_CLEAR_CLIPBOARD_HISTORY,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
  }

  @Test
  public void setPreferencesReturnsPerKeyResults() {
    enableApi();
    final Context context = getApplicationContext();

    final Bundle changes = new Bundle();
    changes.putBoolean(context.getString(R.string.settings_key_sound_on), true);
    changes.putBoolean("not_allowed", true);

    final Bundle extras = new Bundle();
    extras.putBundle(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_CHANGES, changes);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCES, null, extras);
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final Bundle results =
        out.getBundle(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_RESULTS);
    Assert.assertNotNull(results);
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_OK,
        results.getInt(context.getString(R.string.settings_key_sound_on)));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_PREF_NOT_ALLOWED,
        results.getInt("not_allowed"));
  }

  @Test
  public void runMacroRunsEligibleStepsAndReportsResults() throws Exception {
    enableApi();
    final Context context = getApplicationContext();

    final org.json.JSONObject setCap = new org.json.JSONObject();
    setCap.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE);
    final org.json.JSONObject setCapArgs = new org.json.JSONObject();
    setCapArgs.put(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY,
        context.getString(R.string.settings_key_auto_capitalization));
    setCapArgs.put(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL, true);
    setCap.put("args", setCapArgs);

    final org.json.JSONObject reload = new org.json.JSONObject();
    reload.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RELOAD_SETTINGS);

    final org.json.JSONArray steps = new org.json.JSONArray();
    steps.put(setCap);
    steps.put(reload);

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, steps.toString());

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertTrue(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final org.json.JSONArray results =
        new org.json.JSONArray(
            out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_RESULTS));
    Assert.assertEquals(2, results.length());
    Assert.assertTrue(results.getJSONObject(0).getBoolean("ok"));
    Assert.assertTrue(results.getJSONObject(1).getBoolean("ok"));

    // The first step's effect actually landed.
    final Bundle getExtras = new Bundle();
    getExtras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY,
        context.getString(R.string.settings_key_auto_capitalization));
    final Bundle getOut =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_PREFERENCE, null, getExtras);
    Assert.assertNotNull(getOut);
    Assert.assertTrue(getOut.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL));
  }

  @Test
  public void runMacroRejectsOverLength() throws Exception {
    enableApi();

    final org.json.JSONArray steps = new org.json.JSONArray();
    for (int i = 0; i < com.anysoftkeyboard.api.KeyboardApiContract.MAX_MACRO_STEPS + 1; i++) {
      final org.json.JSONObject step = new org.json.JSONObject();
      step.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RELOAD_SETTINGS);
      steps.put(step);
    }

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, steps.toString());

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_MACRO_TOO_LONG,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void runMacroRejectsNonEligibleStep() throws Exception {
    enableApi();

    final org.json.JSONObject setSecret = new org.json.JSONObject();
    setSecret.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_SECRET);
    final org.json.JSONArray steps = new org.json.JSONArray();
    steps.put(setSecret);

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, steps.toString());

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_MACRO_STEP_NOT_ALLOWED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));

    final org.json.JSONArray results =
        new org.json.JSONArray(
            out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_RESULTS));
    Assert.assertEquals(1, results.length());
    Assert.assertFalse(results.getJSONObject(0).getBoolean("ok"));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_MACRO_STEP_NOT_ALLOWED,
        results.getJSONObject(0).getInt("error_code"));
  }

  @Test
  public void runMacroRejectsMalformedJson() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, "{not valid json");

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void runMacroStopsOnErrorByDefaultAndSkipsRemainder() throws Exception {
    enableApi();
    final Context context = getApplicationContext();

    // Step 0 fails (unknown pref key); step 1 would succeed but must be skipped.
    final org.json.JSONObject badSet = new org.json.JSONObject();
    badSet.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE);
    final org.json.JSONObject badArgs = new org.json.JSONObject();
    badArgs.put(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY, "not_a_real_key");
    badArgs.put(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL, true);
    badSet.put("args", badArgs);

    final org.json.JSONObject reload = new org.json.JSONObject();
    reload.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RELOAD_SETTINGS);

    final org.json.JSONArray steps = new org.json.JSONArray();
    steps.put(badSet);
    steps.put(reload);

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, steps.toString());

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final org.json.JSONArray results =
        new org.json.JSONArray(
            out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_RESULTS));
    Assert.assertEquals(2, results.length());
    Assert.assertFalse(results.getJSONObject(0).getBoolean("ok"));
    Assert.assertTrue(results.getJSONObject(1).optBoolean("skipped", false));
  }

  @Test
  public void runMacroStopOnErrorFalseContinuesPastFailure() throws Exception {
    enableApi();

    final org.json.JSONObject badSet = new org.json.JSONObject();
    badSet.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_SET_PREFERENCE);
    final org.json.JSONObject badArgs = new org.json.JSONObject();
    badArgs.put(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_KEY, "not_a_real_key");
    badArgs.put(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_PREF_BOOL, true);
    badSet.put("args", badArgs);

    final org.json.JSONObject reload = new org.json.JSONObject();
    reload.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RELOAD_SETTINGS);

    final org.json.JSONArray steps = new org.json.JSONArray();
    steps.put(badSet);
    steps.put(reload);

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, steps.toString());
    extras.putBoolean(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STOP_ON_ERROR, false);

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));

    final org.json.JSONArray results =
        new org.json.JSONArray(
            out.getString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_RESULTS));
    Assert.assertEquals(2, results.length());
    Assert.assertFalse(results.getJSONObject(0).getBoolean("ok"));
    Assert.assertFalse(results.getJSONObject(1).has("skipped"));
    Assert.assertTrue(results.getJSONObject(1).getBoolean("ok"));
  }

  @Test
  public void runMacroRejectsEmptySteps() {
    enableApi();

    final Bundle extras = new Bundle();
    extras.putString(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, "[]");

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_BAD_ARGUMENTS,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void runMacroBatchChargedToRateLimiter() throws Exception {
    enableApi();
    ShadowSystemClock.advanceBy(Duration.ofMillis(1));

    // Consume most of the per-window budget with cheap reads.
    for (int i = 0; i < 20; i++) {
      mProvider.call(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_PING, null, null);
    }

    final org.json.JSONArray steps = new org.json.JSONArray();
    for (int i = 0; i < 8; i++) {
      final org.json.JSONObject step = new org.json.JSONObject();
      step.put("method", com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RELOAD_SETTINGS);
      steps.put(step);
    }

    final Bundle extras = new Bundle();
    extras.putString(
        com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_MACRO_STEPS, steps.toString());

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO, null, extras);
    Assert.assertNotNull(out);
    Assert.assertFalse(out.getBoolean(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_OK));
    Assert.assertEquals(
        com.anysoftkeyboard.api.KeyboardApiContract.ERR_RATE_LIMITED,
        out.getInt(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_ERROR_CODE));
  }

  @Test
  public void getCapabilitiesIncludesRunMacro() {
    enableApi();

    final Bundle out =
        mProvider.call(
            com.anysoftkeyboard.api.KeyboardApiContract.METHOD_GET_CAPABILITIES,
            null,
            new Bundle());
    Assert.assertNotNull(out);
    final ArrayList<String> methods =
        out.getStringArrayList(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SUPPORTED_METHODS);
    Assert.assertNotNull(methods);
    Assert.assertTrue(
        methods.contains(com.anysoftkeyboard.api.KeyboardApiContract.METHOD_RUN_MACRO));
    final ArrayList<String> scopes =
        out.getStringArrayList(com.anysoftkeyboard.api.KeyboardApiContract.EXTRA_SUPPORTED_SCOPES);
    Assert.assertNotNull(scopes);
    Assert.assertTrue(
        scopes.contains(com.anysoftkeyboard.api.KeyboardApiContract.SCOPE_AUTOMATION_MACRO));
  }

  private static void enableApi() {
    final Context context = getApplicationContext();
    wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context)
        .edit()
        .putBoolean(KeyboardApiPrefs.KEY_API_ENABLED, true)
        .apply();
  }

  private static void enableHighRiskActions() {
    final Context context = getApplicationContext();
    wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context)
        .edit()
        .putBoolean(KeyboardApiPrefs.KEY_HIGH_RISK_ACTIONS_ENABLED, true)
        .apply();
  }

  private static void enableClipboardCopyCut() {
    final Context context = getApplicationContext();
    wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context)
        .edit()
        .putBoolean(KeyboardApiPrefs.KEY_CLIPBOARD_COPY_CUT_ENABLED, true)
        .apply();
  }

  private static void enableAutomationControllers() {
    final Context context = getApplicationContext();
    wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context)
        .edit()
        .putBoolean(KeyboardApiPrefs.KEY_AUTOMATION_CONTROLLERS_ENABLED, true)
        .apply();
  }

  private void startImeForEditorPackage(@NonNull String editorPackageName) {
    mImeServiceController = Robolectric.buildService(TestableImeService.class);
    mImeServiceUnderTest = mImeServiceController.create().get();
    mImeServiceUnderTest.onCreateInputView();

    final EditorInfo editorInfo =
        TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT);
    editorInfo.packageName = editorPackageName;
    mImeServiceUnderTest.onStartInput(editorInfo, false);
  }
}
