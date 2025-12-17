package com.anysoftkeyboard.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.anysoftkeyboard.base.utils.Logger;
import com.menny.android.anysoftkeyboard.R;

/** Handles add-on link clicks and destination navigation outside MainFragment. */
final class AddOnLinkNavigator {

  private AddOnLinkNavigator() {}

  static void handleLink(@NonNull Fragment host, @NonNull String rawUrl) {
    try {
      Uri linkUri = Uri.parse(rawUrl);
      String scheme = linkUri.getScheme();
      if ("settings".equalsIgnoreCase(scheme)) {
        openAppSettings(host);
      } else if ("ask-settings".equalsIgnoreCase(scheme)) {
        openAskSettings(host, linkUri.getHost());
      } else {
        openExternalLink(host, linkUri);
      }
    } catch (Exception e) {
      Logger.w("AddOnLinkNavigator", "Failed to handle link span for url: " + rawUrl, e);
    }
  }

  static void navigateToDestination(@NonNull Fragment host, @NonNull String target) {
    try {
      Uri potentialUri = Uri.parse(target);
      String scheme = potentialUri.getScheme();
      if ("settings".equalsIgnoreCase(scheme)) {
        openAppSettings(host);
        return;
      } else if ("ask-settings".equalsIgnoreCase(scheme)) {
        openAskSettings(host, potentialUri.getHost());
        return;
      }

      if (TextUtils.isDigitsOnly(target)) {
        Navigation.findNavController(host.requireView()).navigate(Integer.parseInt(target));
      } else {
        Navigation.findNavController(host.requireView()).navigate(target);
      }
    } catch (Exception e) {
      Logger.w("AddOnLinkNavigator", "Failed to navigate to target: " + target, e);
      Toast.makeText(
              host.requireContext(), R.string.prefs_providers_operation_failed, Toast.LENGTH_SHORT)
          .show();
    }
  }

  private static void openAppSettings(@NonNull Fragment host) {
    try {
      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      Uri uri = Uri.fromParts("package", host.requireContext().getPackageName(), null);
      intent.setData(uri);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      host.startActivity(intent);
      Logger.d("AddOnLinkNavigator", "Opened app settings for package: " + uri);
    } catch (ActivityNotFoundException e) {
      Logger.w("AddOnLinkNavigator", "Unable to open app settings", e);
      Toast.makeText(
              host.requireContext(), R.string.prefs_providers_operation_failed, Toast.LENGTH_SHORT)
          .show();
    } catch (Exception e) {
      Logger.e("AddOnLinkNavigator", "Error opening app settings", e);
      Toast.makeText(
              host.requireContext(), R.string.prefs_providers_operation_failed, Toast.LENGTH_SHORT)
          .show();
    }
  }

  private static void openAskSettings(@NonNull Fragment host, String destination) {
    if (destination == null) {
      Toast.makeText(
              host.requireContext(), R.string.prefs_providers_operation_failed, Toast.LENGTH_SHORT)
          .show();
      return;
    }

    final int navTarget;
    switch (destination) {
      case "language":
        navTarget = R.id.action_mainFragment_to_languageSettingsFragment;
        break;
      case "speech-to-text":
        navTarget = R.id.action_mainFragment_to_speechToTextSettingsFragment;
        break;
      case "openai-speech":
        navTarget = R.id.action_mainFragment_to_openAISpeechSettingsFragment;
        break;
      default:
        Toast.makeText(
                host.requireContext(),
                R.string.prefs_providers_operation_failed,
                Toast.LENGTH_SHORT)
            .show();
        return;
    }

    try {
      Navigation.findNavController(host.requireView()).navigate(navTarget);
    } catch (Exception e) {
      Logger.w("AddOnLinkNavigator", "Failed to navigate to settings: " + destination, e);
      Toast.makeText(
              host.requireContext(), R.string.prefs_providers_operation_failed, Toast.LENGTH_SHORT)
          .show();
    }
  }

  private static void openExternalLink(@NonNull Fragment host, @NonNull Uri linkUri) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, linkUri);
    try {
      host.startActivity(browserIntent);
    } catch (ActivityNotFoundException e) {
      Logger.w("AddOnLinkNavigator", "Unable to open external link: " + linkUri, e);
      Toast.makeText(
              host.requireContext(), R.string.prefs_providers_operation_failed, Toast.LENGTH_SHORT)
          .show();
    }
  }
}
