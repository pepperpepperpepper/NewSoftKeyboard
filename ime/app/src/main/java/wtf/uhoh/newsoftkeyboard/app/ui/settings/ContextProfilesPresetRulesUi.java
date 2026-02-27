package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ime.context.TypedRulesApplier;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class ContextProfilesPresetRulesUi {

  @NonNull private final ContextProfilesPresetsController.Host host;

  ContextProfilesPresetRulesUi(@NonNull ContextProfilesPresetsController.Host host) {
    this.host = host;
  }

  void showVoiceRulesDialog(@NonNull String presetId) {
    final ContextProfilesStore store = host.store();
    final ContextProfilesStore.Preset preset = store.getPreset(presetId);
    if (preset == null) return;

    final Context context = host.requireContext();
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView help = new TextView(context);
    help.setText(
        context.getString(
            R.string.context_profiles_voice_rules_help,
            ContextProfilesStore.MAX_VOICE_RULES_PER_PRESET,
            ContextProfilesStore.MAX_RULE_MATCH_CHARS,
            ContextProfilesStore.MAX_RULE_REPLACE_CHARS));
    root.addView(
        help,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final EditText previewInput = new EditText(context);
    previewInput.setSingleLine(true);
    previewInput.setHint(R.string.context_profiles_voice_rules_preview_hint);
    previewInput.setInputType(InputType.TYPE_CLASS_TEXT);
    final LinearLayout.LayoutParams previewParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    previewParams.topMargin = dpToPx(context, 12);
    root.addView(previewInput, previewParams);

    final TextView previewOutput = new TextView(context);
    final LinearLayout.LayoutParams outputParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    outputParams.topMargin = dpToPx(context, 8);
    root.addView(previewOutput, outputParams);

    final List<ContextProfilesStore.VoiceRule> mutableRules = new ArrayList<>(preset.voiceRules);
    final ListView list = new ListView(context);
    final VoiceRuleAdapter adapter = new VoiceRuleAdapter(context, mutableRules);
    list.setAdapter(adapter);
    final LinearLayout.LayoutParams listParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 280));
    listParams.topMargin = dpToPx(context, 12);
    root.addView(list, listParams);

    final Button addRule = new Button(context);
    addRule.setText(R.string.context_profiles_voice_rules_add_rule);
    final LinearLayout.LayoutParams addParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    addParams.topMargin = dpToPx(context, 8);
    root.addView(addRule, addParams);

    final Runnable refreshPreview =
        () -> {
          final String raw = String.valueOf(previewInput.getText());
          previewOutput.setText(buildVoiceRulePreview(context, raw, mutableRules));
        };
    refreshPreview.run();

    previewInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            refreshPreview.run();
          }
        });

    addRule.setOnClickListener(
        ignored ->
            showUpsertVoiceRuleDialog(presetId, mutableRules, adapter, refreshPreview, null));

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          final ContextProfilesStore.VoiceRule rule = adapter.getItem(position);
          if (rule == null) return;
          showUpsertVoiceRuleDialog(presetId, mutableRules, adapter, refreshPreview, rule);
        });

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(preset.name)
        .setView(root)
        .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
        .show();
  }

  void showTypedRulesDialog(@NonNull String presetId) {
    final ContextProfilesStore store = host.store();
    final ContextProfilesStore.Preset preset = store.getPreset(presetId);
    if (preset == null) return;

    final Context context = host.requireContext();
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView help = new TextView(context);
    help.setText(
        context.getString(
            R.string.context_profiles_typed_rules_help,
            ContextProfilesStore.MAX_TYPED_RULES_PER_PRESET,
            ContextProfilesStore.MAX_RULE_MATCH_CHARS,
            ContextProfilesStore.MAX_RULE_REPLACE_CHARS));
    root.addView(
        help,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final EditText previewInput = new EditText(context);
    previewInput.setSingleLine(true);
    previewInput.setHint(R.string.context_profiles_typed_rules_preview_hint);
    previewInput.setInputType(InputType.TYPE_CLASS_TEXT);
    final LinearLayout.LayoutParams previewParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    previewParams.topMargin = dpToPx(context, 12);
    root.addView(previewInput, previewParams);

    final TextView previewOutput = new TextView(context);
    final LinearLayout.LayoutParams outputParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    outputParams.topMargin = dpToPx(context, 8);
    root.addView(previewOutput, outputParams);

    final List<ContextProfilesStore.TypedRule> mutableRules = new ArrayList<>(preset.typedRules);
    final ListView list = new ListView(context);
    final TypedRuleAdapter adapter = new TypedRuleAdapter(context, mutableRules);
    list.setAdapter(adapter);
    final LinearLayout.LayoutParams listParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 280));
    listParams.topMargin = dpToPx(context, 12);
    root.addView(list, listParams);

    final Button addRule = new Button(context);
    addRule.setText(R.string.context_profiles_typed_rules_add_rule);
    final LinearLayout.LayoutParams addParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    addParams.topMargin = dpToPx(context, 8);
    root.addView(addRule, addParams);

    final Runnable refreshPreview =
        () -> {
          final String raw = String.valueOf(previewInput.getText());
          previewOutput.setText(buildTypedRulePreview(context, raw, mutableRules));
        };
    refreshPreview.run();

    previewInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            refreshPreview.run();
          }
        });

    addRule.setOnClickListener(
        ignored ->
            showUpsertTypedRuleDialog(presetId, mutableRules, adapter, refreshPreview, null));

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          final ContextProfilesStore.TypedRule rule = adapter.getItem(position);
          if (rule == null) return;
          showUpsertTypedRuleDialog(presetId, mutableRules, adapter, refreshPreview, rule);
        });

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(preset.name)
        .setView(root)
        .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
        .show();
  }

  private void showUpsertVoiceRuleDialog(
      @NonNull String presetId,
      @NonNull List<ContextProfilesStore.VoiceRule> rules,
      @NonNull VoiceRuleAdapter adapter,
      @NonNull Runnable refreshPreview,
      @Nullable ContextProfilesStore.VoiceRule editing) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();

    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final EditText match = new EditText(context);
    match.setHint(R.string.context_profiles_voice_rules_match_hint);
    match.setSingleLine(true);
    match.setInputType(InputType.TYPE_CLASS_TEXT);
    match.setFilters(
        new InputFilter[] {
          new InputFilter.LengthFilter(ContextProfilesStore.MAX_RULE_MATCH_CHARS)
        });
    root.addView(match);

    final EditText replace = new EditText(context);
    replace.setHint(R.string.context_profiles_voice_rules_replace_hint);
    replace.setSingleLine(true);
    replace.setInputType(InputType.TYPE_CLASS_TEXT);
    replace.setFilters(
        new InputFilter[] {
          new InputFilter.LengthFilter(ContextProfilesStore.MAX_RULE_REPLACE_CHARS)
        });
    final LinearLayout.LayoutParams replaceParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    replaceParams.topMargin = dpToPx(context, 8);
    root.addView(replace, replaceParams);

    final SwitchCompat autoApply = new SwitchCompat(context);
    autoApply.setText(R.string.context_profiles_voice_rules_auto_apply_title);
    final LinearLayout.LayoutParams toggleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    toggleParams.topMargin = dpToPx(context, 8);
    root.addView(autoApply, toggleParams);

    final TextView autoApplySummary = new TextView(context);
    autoApplySummary.setText(R.string.context_profiles_voice_rules_auto_apply_summary);
    final LinearLayout.LayoutParams autoApplySummaryParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    autoApplySummaryParams.topMargin = dpToPx(context, 4);
    root.addView(autoApplySummary, autoApplySummaryParams);

    if (editing != null) {
      match.setText(editing.match);
      replace.setText(editing.replace);
      autoApply.setChecked(editing.autoApply);
    } else {
      autoApply.setChecked(false);
    }

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(
                editing == null
                    ? R.string.context_profiles_voice_rules_add_title
                    : R.string.context_profiles_voice_rules_edit_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setPositiveButton(
                android.R.string.ok,
                (d, w) -> {
                  d.dismiss();
                  if (editing == null
                      && rules.size() >= ContextProfilesStore.MAX_VOICE_RULES_PER_PRESET) {
                    Toast.makeText(
                            context,
                            context.getString(
                                R.string.context_profiles_limit_too_many_voice_rules_toast,
                                ContextProfilesStore.MAX_VOICE_RULES_PER_PRESET),
                            Toast.LENGTH_SHORT)
                        .show();
                    return;
                  }
                  final String matchValue = String.valueOf(match.getText()).trim();
                  if (matchValue.isEmpty()) return;
                  final String replaceValue = String.valueOf(replace.getText());
                  final boolean autoApplyValue = autoApply.isChecked();

                  final ContextProfilesStore.VoiceRule updated =
                      new ContextProfilesStore.VoiceRule(matchValue, replaceValue, autoApplyValue);
                  if (editing != null) {
                    rules.remove(editing);
                  }
                  rules.add(updated);
                  store.setVoiceRules(presetId, rules);
                  adapter.refresh(rules);
                  refreshPreview.run();
                });

    if (editing != null) {
      builder.setNeutralButton(
          R.string.context_profiles_voice_rules_delete_action,
          (d, w) -> {
            d.dismiss();
            rules.remove(editing);
            store.setVoiceRules(presetId, rules);
            adapter.refresh(rules);
            refreshPreview.run();
          });
    }

    builder.show();
  }

  private void showUpsertTypedRuleDialog(
      @NonNull String presetId,
      @NonNull List<ContextProfilesStore.TypedRule> rules,
      @NonNull TypedRuleAdapter adapter,
      @NonNull Runnable refreshPreview,
      @Nullable ContextProfilesStore.TypedRule editing) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();

    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final EditText match = new EditText(context);
    match.setHint(R.string.context_profiles_typed_rules_match_hint);
    match.setSingleLine(true);
    match.setInputType(InputType.TYPE_CLASS_TEXT);
    match.setFilters(
        new InputFilter[] {
          new InputFilter.LengthFilter(ContextProfilesStore.MAX_RULE_MATCH_CHARS)
        });
    root.addView(match);

    final EditText replace = new EditText(context);
    replace.setHint(R.string.context_profiles_typed_rules_replace_hint);
    replace.setSingleLine(true);
    replace.setInputType(InputType.TYPE_CLASS_TEXT);
    replace.setFilters(
        new InputFilter[] {
          new InputFilter.LengthFilter(ContextProfilesStore.MAX_RULE_REPLACE_CHARS)
        });
    final LinearLayout.LayoutParams replaceParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    replaceParams.topMargin = dpToPx(context, 8);
    root.addView(replace, replaceParams);

    final SwitchCompat enabled = new SwitchCompat(context);
    enabled.setText(R.string.context_profiles_typed_rules_enabled_title);
    final LinearLayout.LayoutParams enabledParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    enabledParams.topMargin = dpToPx(context, 8);
    root.addView(enabled, enabledParams);

    final SwitchCompat autoApply = new SwitchCompat(context);
    autoApply.setText(R.string.context_profiles_typed_rules_auto_apply_title);
    final LinearLayout.LayoutParams autoApplyParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    autoApplyParams.topMargin = dpToPx(context, 8);
    root.addView(autoApply, autoApplyParams);

    final TextView autoApplySummary = new TextView(context);
    autoApplySummary.setText(R.string.context_profiles_typed_rules_auto_apply_summary);
    final LinearLayout.LayoutParams autoApplySummaryParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    autoApplySummaryParams.topMargin = dpToPx(context, 4);
    root.addView(autoApplySummary, autoApplySummaryParams);

    final SwitchCompat wholeWord = new SwitchCompat(context);
    wholeWord.setText(R.string.context_profiles_typed_rules_whole_word_title);
    final LinearLayout.LayoutParams wholeWordParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    wholeWordParams.topMargin = dpToPx(context, 8);
    root.addView(wholeWord, wholeWordParams);

    final TextView wholeWordSummary = new TextView(context);
    wholeWordSummary.setText(R.string.context_profiles_typed_rules_whole_word_summary);
    final LinearLayout.LayoutParams wholeWordSummaryParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    wholeWordSummaryParams.topMargin = dpToPx(context, 4);
    root.addView(wholeWordSummary, wholeWordSummaryParams);

    final SwitchCompat caseSensitive = new SwitchCompat(context);
    caseSensitive.setText(R.string.context_profiles_typed_rules_case_sensitive_title);
    final LinearLayout.LayoutParams caseSensitiveParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    caseSensitiveParams.topMargin = dpToPx(context, 8);
    root.addView(caseSensitive, caseSensitiveParams);

    final TextView caseSensitiveSummary = new TextView(context);
    caseSensitiveSummary.setText(R.string.context_profiles_typed_rules_case_sensitive_summary);
    final LinearLayout.LayoutParams caseSensitiveSummaryParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    caseSensitiveSummaryParams.topMargin = dpToPx(context, 4);
    root.addView(caseSensitiveSummary, caseSensitiveSummaryParams);

    if (editing != null) {
      match.setText(editing.match);
      replace.setText(editing.replace);
      enabled.setChecked(editing.enabled);
      autoApply.setChecked(editing.autoApply);
      wholeWord.setChecked(editing.matchWholeWord);
      caseSensitive.setChecked(editing.matchCaseSensitive);
    } else {
      enabled.setChecked(true);
      autoApply.setChecked(false);
      wholeWord.setChecked(true);
      caseSensitive.setChecked(false);
    }

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(
                editing == null
                    ? R.string.context_profiles_typed_rules_add_title
                    : R.string.context_profiles_typed_rules_edit_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setPositiveButton(
                android.R.string.ok,
                (d, w) -> {
                  d.dismiss();
                  if (editing == null
                      && rules.size() >= ContextProfilesStore.MAX_TYPED_RULES_PER_PRESET) {
                    Toast.makeText(
                            context,
                            context.getString(
                                R.string.context_profiles_limit_too_many_typed_rules_toast,
                                ContextProfilesStore.MAX_TYPED_RULES_PER_PRESET),
                            Toast.LENGTH_SHORT)
                        .show();
                    return;
                  }
                  final String matchValue = String.valueOf(match.getText()).trim();
                  if (matchValue.isEmpty()) return;
                  final String replaceValue = String.valueOf(replace.getText());
                  final boolean enabledValue = enabled.isChecked();
                  final boolean autoApplyValue = autoApply.isChecked();
                  final boolean matchWholeWord = wholeWord.isChecked();
                  final boolean matchCaseSensitive = caseSensitive.isChecked();

                  final ContextProfilesStore.TypedRule updated =
                      new ContextProfilesStore.TypedRule(
                          matchValue,
                          replaceValue,
                          autoApplyValue,
                          enabledValue,
                          matchCaseSensitive,
                          matchWholeWord);
                  if (editing != null) {
                    rules.remove(editing);
                  }
                  rules.add(updated);
                  store.setTypedRules(presetId, rules);
                  adapter.refresh(rules);
                  refreshPreview.run();
                });

    if (editing != null) {
      builder.setNeutralButton(
          R.string.context_profiles_typed_rules_delete_action,
          (d, w) -> {
            d.dismiss();
            rules.remove(editing);
            store.setTypedRules(presetId, rules);
            adapter.refresh(rules);
            refreshPreview.run();
          });
    }

    builder.show();
  }

  @NonNull
  private static String buildVoiceRulePreview(
      @NonNull Context context,
      @NonNull String input,
      @NonNull List<ContextProfilesStore.VoiceRule> rules) {
    if (input.trim().isEmpty())
      return context.getString(R.string.context_profiles_voice_rules_preview_empty);

    final String autoApplied = applyFirstAutoRule(input, rules);
    if (!autoApplied.equals(input)) {
      return context.getString(
          R.string.context_profiles_voice_rules_preview_auto_result, autoApplied);
    }

    final String suggestion = resolveFirstSuggestionReplacement(input, rules);
    if (suggestion != null) {
      return context.getString(
          R.string.context_profiles_voice_rules_preview_suggest_result, suggestion);
    }

    return context.getString(R.string.context_profiles_voice_rules_preview_no_match);
  }

  @NonNull
  private static String buildTypedRulePreview(
      @NonNull Context context,
      @NonNull String input,
      @NonNull List<ContextProfilesStore.TypedRule> rules) {
    if (input.trim().isEmpty())
      return context.getString(R.string.context_profiles_typed_rules_preview_empty);

    final TypedRulesApplier.Result result =
        TypedRulesApplier.resolve(input, rules, true /*allowSuggestions*/);

    final TypedRulesApplier.Replacement autoReplacement = result.autoReplacement();
    if (autoReplacement != null) {
      final String committedText = autoReplacement.committedText();
      final String output =
          input.endsWith(committedText)
              ? input.substring(0, input.length() - committedText.length())
                  + autoReplacement.replacementText()
              : autoReplacement.replacementText();
      return context.getString(R.string.context_profiles_typed_rules_preview_auto_result, output);
    }

    final TypedRulesApplier.TypedSuggestion suggestion = result.suggestion();
    if (suggestion != null) {
      return context.getString(
          R.string.context_profiles_typed_rules_preview_suggest_result,
          suggestion.suggestionText());
    }

    return context.getString(R.string.context_profiles_typed_rules_preview_no_match);
  }

  @NonNull
  private static String applyFirstAutoRule(
      @NonNull String formattedText, @NonNull List<ContextProfilesStore.VoiceRule> rules) {
    if (formattedText.isEmpty()) return formattedText;

    int start = 0;
    while (start < formattedText.length() && Character.isWhitespace(formattedText.charAt(start))) {
      start++;
    }
    int end = formattedText.length();
    while (end > start && Character.isWhitespace(formattedText.charAt(end - 1))) {
      end--;
    }

    final String core = formattedText.substring(start, end);
    if (core.isEmpty()) return formattedText;

    for (ContextProfilesStore.VoiceRule rule : rules) {
      if (rule == null || !rule.autoApply) continue;
      if (core.equalsIgnoreCase(rule.match)) {
        return formattedText.substring(0, start) + rule.replace + formattedText.substring(end);
      }
    }
    return formattedText;
  }

  @Nullable
  private static String resolveFirstSuggestionReplacement(
      @NonNull String formattedText, @NonNull List<ContextProfilesStore.VoiceRule> rules) {
    if (formattedText.isEmpty()) return null;

    int start = 0;
    while (start < formattedText.length() && Character.isWhitespace(formattedText.charAt(start))) {
      start++;
    }
    int end = formattedText.length();
    while (end > start && Character.isWhitespace(formattedText.charAt(end - 1))) {
      end--;
    }

    final String core = formattedText.substring(start, end);
    if (core.isEmpty()) return null;

    for (ContextProfilesStore.VoiceRule rule : rules) {
      if (rule == null || rule.autoApply) continue;
      if (core.equalsIgnoreCase(rule.match)) return rule.replace;
    }
    return null;
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }

  private static final class VoiceRuleAdapter extends ArrayAdapter<ContextProfilesStore.VoiceRule> {
    @NonNull private final List<ContextProfilesStore.VoiceRule> allRules;

    private VoiceRuleAdapter(
        @NonNull Context context, @NonNull List<ContextProfilesStore.VoiceRule> rules) {
      super(context, android.R.layout.simple_list_item_2, new ArrayList<>(rules));
      allRules = new ArrayList<>(rules);
    }

    void refresh(@NonNull List<ContextProfilesStore.VoiceRule> rules) {
      allRules.clear();
      allRules.addAll(rules);
      clear();
      addAll(rules);
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      final android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());
      final View view =
          convertView != null
              ? convertView
              : inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
      final ContextProfilesStore.VoiceRule rule = getItem(position);
      if (rule == null) return view;

      final TextView text1 = view.findViewById(android.R.id.text1);
      final TextView text2 = view.findViewById(android.R.id.text2);
      text1.setText(rule.match);
      text2.setText(
          rule.autoApply
              ? getContext()
                  .getString(R.string.context_profiles_voice_rule_row_auto_apply, rule.replace)
              : getContext()
                  .getString(R.string.context_profiles_voice_rule_row_suggest, rule.replace));
      return view;
    }
  }

  private static final class TypedRuleAdapter extends ArrayAdapter<ContextProfilesStore.TypedRule> {
    @NonNull private final List<ContextProfilesStore.TypedRule> allRules;

    private TypedRuleAdapter(
        @NonNull Context context, @NonNull List<ContextProfilesStore.TypedRule> rules) {
      super(context, android.R.layout.simple_list_item_2, new ArrayList<>(rules));
      allRules = new ArrayList<>(rules);
    }

    void refresh(@NonNull List<ContextProfilesStore.TypedRule> rules) {
      allRules.clear();
      allRules.addAll(rules);
      clear();
      addAll(rules);
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      final android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());
      final View view =
          convertView != null
              ? convertView
              : inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
      final ContextProfilesStore.TypedRule rule = getItem(position);
      if (rule == null) return view;

      final TextView text1 = view.findViewById(android.R.id.text1);
      final TextView text2 = view.findViewById(android.R.id.text2);
      text1.setText(rule.match);

      if (!rule.enabled) {
        text2.setText(R.string.context_profiles_typed_rule_row_disabled);
      } else {
        text2.setText(
            rule.autoApply
                ? getContext()
                    .getString(R.string.context_profiles_typed_rule_row_auto_apply, rule.replace)
                : getContext()
                    .getString(R.string.context_profiles_typed_rule_row_suggest, rule.replace));
      }
      return view;
    }
  }
}
