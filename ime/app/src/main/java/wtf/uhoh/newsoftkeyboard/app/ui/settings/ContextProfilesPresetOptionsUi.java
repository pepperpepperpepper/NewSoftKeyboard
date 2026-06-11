package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class ContextProfilesPresetOptionsUi {

  @NonNull private final ContextProfilesPresetsController.Host host;

  ContextProfilesPresetOptionsUi(@NonNull ContextProfilesPresetsController.Host host) {
    this.host = host;
  }

  void showSecurityOptionsDialog(@NonNull String presetId) {
    final ContextProfilesStore store = host.store();
    final ContextProfilesStore.Preset preset = store.getPreset(presetId);
    if (preset == null) return;

    final Context context = host.requireContext();
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView help = new TextView(context);
    help.setText(R.string.context_profiles_security_options_help);
    root.addView(
        help,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final SwitchCompat containsPersonalContent = new SwitchCompat(context);
    containsPersonalContent.setText(R.string.context_profiles_security_contains_personal_title);
    containsPersonalContent.setChecked(preset.containsPersonalContent);
    final LinearLayout.LayoutParams toggleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    toggleParams.topMargin = dpToPx(context, 12);
    root.addView(containsPersonalContent, toggleParams);

    final TextView containsPersonalSummary = new TextView(context);
    containsPersonalSummary.setText(R.string.context_profiles_security_contains_personal_summary);
    final LinearLayout.LayoutParams summaryParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    summaryParams.topMargin = dpToPx(context, 4);
    root.addView(containsPersonalSummary, summaryParams);

    final SwitchCompat allowAutoApplyInNoSuggestions = new SwitchCompat(context);
    allowAutoApplyInNoSuggestions.setText(
        R.string.context_profiles_security_allow_auto_apply_no_suggestions_title);
    allowAutoApplyInNoSuggestions.setChecked(preset.allowAutoApplyInNoSuggestionsFields);
    final LinearLayout.LayoutParams allowParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    allowParams.topMargin = dpToPx(context, 12);
    root.addView(allowAutoApplyInNoSuggestions, allowParams);

    final TextView allowAutoApplySummary = new TextView(context);
    allowAutoApplySummary.setText(
        R.string.context_profiles_security_allow_auto_apply_no_suggestions_summary);
    final LinearLayout.LayoutParams allowSummaryParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    allowSummaryParams.topMargin = dpToPx(context, 4);
    root.addView(allowAutoApplySummary, allowSummaryParams);

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_security_options_title)
        .setView(root)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              d.dismiss();
              store.setSecurityOptions(
                  presetId,
                  containsPersonalContent.isChecked(),
                  allowAutoApplyInNoSuggestions.isChecked());
            })
        .show();
  }

  void showSafeTogglesDialog(@NonNull String presetId) {
    final ContextProfilesStore store = host.store();
    final ContextProfilesStore.Preset preset = store.getPreset(presetId);
    if (preset == null) return;

    final Context context = host.requireContext();
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView help = new TextView(context);
    help.setText(R.string.context_profiles_safe_toggles_help);
    root.addView(
        help,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final SwitchCompat disableQuickFixes = new SwitchCompat(context);
    disableQuickFixes.setText(R.string.context_profiles_safe_toggles_disable_quick_fixes);
    disableQuickFixes.setChecked(preset.safeToggles.disableQuickFixes);
    final LinearLayout.LayoutParams toggleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    toggleParams.topMargin = dpToPx(context, 12);
    root.addView(disableQuickFixes, toggleParams);

    final SwitchCompat disableUserDictionary = new SwitchCompat(context);
    disableUserDictionary.setText(R.string.context_profiles_safe_toggles_disable_user_dictionary);
    disableUserDictionary.setChecked(preset.safeToggles.disableUserDictionary);
    root.addView(disableUserDictionary);

    final SwitchCompat disableContactsDictionary = new SwitchCompat(context);
    disableContactsDictionary.setText(
        R.string.context_profiles_safe_toggles_disable_contacts_dictionary);
    disableContactsDictionary.setChecked(preset.safeToggles.disableContactsDictionary);
    root.addView(disableContactsDictionary);

    final SwitchCompat disableNextWordSuggestions = new SwitchCompat(context);
    disableNextWordSuggestions.setText(
        R.string.context_profiles_safe_toggles_disable_next_word_suggestions);
    disableNextWordSuggestions.setChecked(preset.safeToggles.disableNextWordSuggestions);
    root.addView(disableNextWordSuggestions);

    final SwitchCompat disableMainDictionary = new SwitchCompat(context);
    disableMainDictionary.setText(R.string.context_profiles_safe_toggles_disable_main_dictionary);
    disableMainDictionary.setChecked(preset.safeToggles.disableMainDictionary);
    root.addView(disableMainDictionary);

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_safe_toggles_title)
        .setView(root)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              d.dismiss();
              store.setSafeToggles(
                  presetId,
                  new ContextProfilesStore.SafeToggles(
                      disableContactsDictionary.isChecked(),
                      disableUserDictionary.isChecked(),
                      disableQuickFixes.isChecked(),
                      disableNextWordSuggestions.isChecked(),
                      disableMainDictionary.isChecked()));
            })
        .show();
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }
}
