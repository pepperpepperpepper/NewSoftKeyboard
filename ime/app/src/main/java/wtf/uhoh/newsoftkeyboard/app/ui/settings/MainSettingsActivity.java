/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.Objects;
import net.evendanan.pixel.EdgeEffectHacker;
import pub.devrel.easypermissions.AfterPermissionGranted;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiSettingsDeepLinks;
import wtf.uhoh.newsoftkeyboard.notification.NotificationIds;
import wtf.uhoh.newsoftkeyboard.permissions.PermissionRequestHelper;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

public class MainSettingsActivity extends AppCompatActivity {

  public static final String ACTION_REQUEST_PERMISSION_ACTIVITY =
      "ACTION_REQUEST_PERMISSION_ACTIVITY";
  public static final String ACTION_REVOKE_PERMISSION_ACTIVITY =
      "ACTION_REVOKE_PERMISSION_ACTIVITY";
  public static final String EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY =
      "EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY";

  private CharSequence mTitle;
  @Nullable private NavController mNavController;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.main_ui);

    mTitle = getTitle();

    mNavController =
        ((NavHostFragment)
                Objects.requireNonNull(
                    getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
            .getNavController();
    NavigationUI.setupActionBarWithNavController(this, mNavController);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // applying my very own Edge-Effect color
    EdgeEffectHacker.brandGlowEffect(this, ContextCompat.getColor(this, R.color.app_accent));
    handlePermissionRequest(getIntent());
    handleOpenAISettingsNavigation(getIntent());
    handleProgrammableApiOpenSettingsNavigation(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handlePermissionRequest(intent);
    handleOpenAISettingsNavigation(intent);
    handleProgrammableApiOpenSettingsNavigation(intent);
  }

  private void handleProgrammableApiOpenSettingsNavigation(@Nullable Intent intent) {
    if (intent == null) return;

    if (!intent.hasExtra(KeyboardApiContract.EXTRA_DESTINATION_ID)
        && !intent.hasExtra(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY)) {
      return;
    }

    final String destinationId = intent.getStringExtra(KeyboardApiContract.EXTRA_DESTINATION_ID);
    final String scrollToPrefKey =
        intent.getStringExtra(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY);
    intent.removeExtra(KeyboardApiContract.EXTRA_DESTINATION_ID);
    intent.removeExtra(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY);

    if (TextUtils.isEmpty(destinationId)) return;

    final Integer navDestinationId = KeyboardApiSettingsDeepLinks.toNavDestinationId(destinationId);
    if (navDestinationId == null) return;

    final Bundle args = new Bundle();
    if (!TextUtils.isEmpty(scrollToPrefKey)) {
      args.putString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY, scrollToPrefKey);
    }
    try {
      requireNavController().navigate(navDestinationId, args);
    } catch (RuntimeException ignored) {
      // Defensive: do not crash settings if the nav graph has changed unexpectedly.
    }
  }

  private void handleOpenAISettingsNavigation(Intent intent) {
    if (intent == null) return;

    // Handle navigation to OpenAI settings
    if (intent.hasExtra("navigate_to_openai_settings")) {
      intent.removeExtra("navigate_to_openai_settings");
      navigateToOpenAISettings();
      return;
    }
  }

  private void handlePermissionRequest(Intent intent) {
    if (intent == null) return;

    if (ACTION_REQUEST_PERMISSION_ACTIVITY.equals(intent.getAction())
        && intent.hasExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY)) {
      final String permission = intent.getStringExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY);
      intent.removeExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY);
      if (Objects.equals(permission, Manifest.permission.READ_CONTACTS)) {
        startContactsPermissionRequest();
      } else {
        throw new IllegalArgumentException("Unknown permission request " + permission);
      }
    }

    if (ACTION_REVOKE_PERMISSION_ACTIVITY.equals(intent.getAction())
        && intent.hasExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY)) {
      final String permission = intent.getStringExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY);
      intent.removeExtra(ACTION_REVOKE_PERMISSION_ACTIVITY);
      if (Objects.equals(permission, Manifest.permission.READ_CONTACTS)) {
        NskApplicationBase.notifier(this).cancel(NotificationIds.RequestContactsPermission);
        DirectBootAwareSharedPreferences.create(getApplicationContext())
            .edit()
            .putBoolean(getString(R.string.settings_key_use_contacts_dictionary), false)
            .apply();
        finish();
      } else {
        throw new IllegalArgumentException("Unknown permission request " + permission);
      }
    }
  }

  @AfterPermissionGranted(PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE)
  public void startContactsPermissionRequest() {
    NskApplicationBase.notifier(this).cancel(NotificationIds.RequestContactsPermission);
    PermissionRequestHelper.check(this, PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE);
  }

  public void navigateToOpenAISettings() {
    navigateToOpenAISettings(null);
  }

  public void navigateToOpenAISettings(String promptText) {
    final NavController navController = requireNavController();

    // Mark intent so the fragment opens the prompt dialog when we arrive.
    getIntent().putExtra("open_prompt_dialog", true);
    if (promptText != null) {
      getIntent().putExtra("prompt_text_to_load", promptText);
    }

    if (navController.getCurrentDestination() != null
        && navController.getCurrentDestination().getId() == R.id.openAISpeechSettingsFragment) {
      OpenAISpeechSettingsFragment currentFragment =
          (OpenAISpeechSettingsFragment)
              ((NavHostFragment)
                      getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                  .getChildFragmentManager()
                  .getFragments()
                  .get(0);
      if (currentFragment != null) {
        if (promptText != null) {
          currentFragment.updatePromptPreference(promptText);
        }
        currentFragment.showPromptDialog();
      }
      return;
    }
    navController.navigate(R.id.openAISpeechSettingsFragment);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionRequestHelper.onRequestPermissionsResult(
        requestCode, permissions, grantResults, this);
  }

  @Override
  public void setTitle(CharSequence title) {
    mTitle = title;
    if (getSupportActionBar() != null) getSupportActionBar().setTitle(mTitle);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      // Work around a rare crash (seen in Genymotion/Android 16) when a hardware MENU key opens an
      // overflow PopupWindow and the menu contents are mutated while it is still being laid out.
      // Touch users still have the overflow button; this only affects legacy hardware keys.
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU) return true;
    return super.onKeyUp(keyCode, event);
  }

  @Override
  public void invalidateOptionsMenu() {
    // Defensive: if an overflow menu popup is currently showing, a fragment-driven menu
    // invalidation can mutate the underlying MenuAdapter while it is still laid out, which can
    // crash on some Android/AppCompat combinations.
    closeOptionsMenu();
    super.invalidateOptionsMenu();
  }

  @Override
  public boolean onSupportNavigateUp() {
    final NavController navController = mNavController;
    return navController != null && navController.navigateUp();
  }

  @NonNull
  private NavController requireNavController() {
    final NavController controller = mNavController;
    if (controller != null) return controller;
    throw new IllegalStateException("NavController not initialized");
  }
}
