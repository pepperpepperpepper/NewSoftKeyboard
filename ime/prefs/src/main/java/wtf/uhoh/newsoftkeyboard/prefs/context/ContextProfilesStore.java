package wtf.uhoh.newsoftkeyboard.prefs.context;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/**
 * Stores user-defined context profiles (presets) and per-app bindings.
 *
 * <p>Note: This store is intentionally low-level and does not enforce any IME runtime security
 * guards. Runtime code must still enforce password/incognito/no-personalized-learning protections.
 */
public class ContextProfilesStore {

  public static final int MAX_PRESETS = 50;
  public static final int MAX_VOICE_RULES_PER_PRESET = 50;
  public static final int MAX_TYPED_RULES_PER_PRESET = 100;
  public static final int MAX_APP_BINDINGS = 100;
  public static final int MAX_RULE_MATCH_CHARS = 128;
  public static final int MAX_RULE_REPLACE_CHARS = 256;

  private static final int MAX_PRESET_JSON_CHARS = 120_000;
  private static final int MAX_BINDINGS_JSON_CHARS = 120_000;

  private static final String PREF_ENABLED = "context_profiles_enabled";
  private static final String PREF_PRESET_IDS = "context_profiles_preset_ids";
  private static final String PREF_PRESET_JSON_PREFIX = "context_profiles_preset_json::";
  private static final String PREF_APP_BINDINGS_JSON = "context_profiles_app_bindings_json";
  private static final String PREF_WORD_LIST_GENERATION_PREFIX =
      "context_profiles_word_list_generation::";

  private static final String USER_PRESET_ID_PREFIX = "context_preset::";

  private static final String JSON_ID = "id";
  private static final String JSON_NAME = "name";
  private static final String JSON_CREATED_AT = "created_at";
  private static final String JSON_VOICE_RULES = "voice_rules";
  private static final String JSON_TYPED_RULES = "typed_rules";
  private static final String JSON_SAFE_TOGGLES = "safe_toggles";
  private static final String JSON_CONTAINS_PERSONAL_CONTENT = "contains_personal_content";
  private static final String JSON_ALLOW_AUTO_APPLY_IN_NO_SUGGESTIONS_FIELDS =
      "allow_auto_apply_in_no_suggestions_fields";

  private static final String JSON_DISABLE_CONTACTS_DICTIONARY = "disable_contacts_dictionary";
  private static final String JSON_DISABLE_USER_DICTIONARY = "disable_user_dictionary";
  private static final String JSON_DISABLE_QUICK_FIXES = "disable_quick_fixes";
  private static final String JSON_DISABLE_NEXT_WORD_SUGGESTIONS = "disable_next_word_suggestions";

  private final SharedPreferences prefs;

  public ContextProfilesStore(@NonNull Context context) {
    prefs = DirectBootAwareSharedPreferences.create(context.getApplicationContext());
  }

  ContextProfilesStore(@NonNull SharedPreferences prefs) {
    this.prefs = prefs;
  }

  public boolean isEnabled() {
    return prefs.getBoolean(PREF_ENABLED, false);
  }

  public void setEnabled(boolean enabled) {
    prefs.edit().putBoolean(PREF_ENABLED, enabled).apply();
  }

  @NonNull
  public Preset createPreset(@NonNull String name) {
    final String trimmed = name.trim();
    if (trimmed.isEmpty()) throw new IllegalArgumentException("Preset name must not be empty.");

    final Set<String> existing = prefs.getStringSet(PREF_PRESET_IDS, null);
    if (existing != null && existing.size() >= MAX_PRESETS) {
      throw new IllegalStateException("Too many presets.");
    }

    final String id = USER_PRESET_ID_PREFIX + UUID.randomUUID();
    final Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
    updated.add(id);

    final long now = System.currentTimeMillis();
    final JSONObject json = new JSONObject();
    try {
      json.put(JSON_ID, id);
      json.put(JSON_NAME, trimmed);
      json.put(JSON_CREATED_AT, now);
      json.put(JSON_VOICE_RULES, new JSONArray());
      json.put(JSON_TYPED_RULES, new JSONArray());
      json.put(JSON_SAFE_TOGGLES, safeTogglesToJson(SafeToggles.DEFAULT));
      json.put(JSON_CONTAINS_PERSONAL_CONTENT, true);
      json.put(JSON_ALLOW_AUTO_APPLY_IN_NO_SUGGESTIONS_FIELDS, false);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to encode preset JSON.", e);
    }

    prefs
        .edit()
        .putStringSet(PREF_PRESET_IDS, updated)
        .putString(presetJsonKey(id), json.toString())
        .apply();
    return new Preset(
        id,
        trimmed,
        now,
        Collections.emptyList(),
        Collections.emptyList(),
        SafeToggles.DEFAULT,
        true,
        false);
  }

  public void renamePreset(@NonNull String presetId, @NonNull String newName) {
    final String trimmed = newName.trim();
    if (trimmed.isEmpty()) throw new IllegalArgumentException("Preset name must not be empty.");
    final JSONObject json = readPresetJson(presetId);
    if (json == null) return;
    try {
      json.put(JSON_NAME, trimmed);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to update preset JSON.", e);
    }
    prefs.edit().putString(presetJsonKey(presetId), json.toString()).apply();
  }

  public void deletePreset(@NonNull String presetId) {
    final Set<String> existing = prefs.getStringSet(PREF_PRESET_IDS, null);
    final Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
    updated.remove(presetId);

    final SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(PREF_PRESET_IDS, updated);
    editor.remove(presetJsonKey(presetId));
    editor.remove(wordListGenerationKey(presetId));
    editor.apply();

    removePresetFromBindings(presetId);
  }

  /** Increments the generation for the preset's word list, so the IME can reload it. */
  public void bumpWordListGeneration(@NonNull String presetId) {
    final String key = wordListGenerationKey(presetId);
    final long next = prefs.getLong(key, 0L) + 1L;
    prefs.edit().putLong(key, next).apply();
  }

  public long getWordListGeneration(@NonNull String presetId) {
    return prefs.getLong(wordListGenerationKey(presetId), 0L);
  }

  @NonNull
  private static String wordListGenerationKey(@NonNull String presetId) {
    return PREF_WORD_LIST_GENERATION_PREFIX + presetId;
  }

  @NonNull
  public List<Preset> listPresets() {
    final Set<String> ids = prefs.getStringSet(PREF_PRESET_IDS, Collections.emptySet());
    if (ids == null || ids.isEmpty()) return Collections.emptyList();

    final List<Preset> out = new ArrayList<>(Math.min(ids.size(), MAX_PRESETS));
    for (String id : ids) {
      if (id == null || id.trim().isEmpty()) continue;
      final Preset preset = getPreset(id);
      if (preset != null) out.add(preset);
      if (out.size() >= MAX_PRESETS) break;
    }
    out.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));
    return out;
  }

  @Nullable
  public Preset getPreset(@NonNull String presetId) {
    final JSONObject json = readPresetJson(presetId);
    if (json == null) return null;
    final String name = json.optString(JSON_NAME, "").trim();
    if (name.isEmpty()) return null;
    final long createdAtMillis = json.optLong(JSON_CREATED_AT, 0L);

    final List<VoiceRule> voiceRules = readVoiceRules(json.optJSONArray(JSON_VOICE_RULES));
    final List<TypedRule> typedRules = readTypedRules(json.optJSONArray(JSON_TYPED_RULES));
    final SafeToggles safeToggles = readSafeToggles(json.optJSONObject(JSON_SAFE_TOGGLES));
    final boolean containsPersonalContent = json.optBoolean(JSON_CONTAINS_PERSONAL_CONTENT, true);
    final boolean allowAutoApplyInNoSuggestionsFields =
        json.optBoolean(JSON_ALLOW_AUTO_APPLY_IN_NO_SUGGESTIONS_FIELDS, false);
    return new Preset(
        presetId,
        name,
        createdAtMillis,
        voiceRules,
        typedRules,
        safeToggles,
        containsPersonalContent,
        allowAutoApplyInNoSuggestionsFields);
  }

  public void setVoiceRules(@NonNull String presetId, @NonNull List<VoiceRule> voiceRules) {
    final JSONObject json = readPresetJson(presetId);
    if (json == null) return;

    final JSONArray rulesJson = new JSONArray();
    int written = 0;
    for (VoiceRule rule : voiceRules) {
      if (rule == null) continue;
      if (written >= MAX_VOICE_RULES_PER_PRESET) break;
      if (rule.match.length() > MAX_RULE_MATCH_CHARS) continue;
      final String replace = rule.replace == null ? "" : rule.replace;
      if (replace.length() > MAX_RULE_REPLACE_CHARS) continue;
      final JSONObject ruleJson = new JSONObject();
      try {
        ruleJson.put("match", rule.match);
        ruleJson.put("replace", replace);
        ruleJson.put("auto_apply", rule.autoApply);
      } catch (JSONException e) {
        throw new IllegalStateException("Failed to encode voice rule JSON.", e);
      }
      rulesJson.put(ruleJson);
      written++;
    }

    try {
      json.put(JSON_VOICE_RULES, rulesJson);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to update preset JSON.", e);
    }
    prefs.edit().putString(presetJsonKey(presetId), json.toString()).apply();
  }

  public void setSafeToggles(@NonNull String presetId, @NonNull SafeToggles safeToggles) {
    final JSONObject json = readPresetJson(presetId);
    if (json == null) return;
    try {
      json.put(JSON_SAFE_TOGGLES, safeTogglesToJson(safeToggles));
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to update preset JSON.", e);
    }
    prefs.edit().putString(presetJsonKey(presetId), json.toString()).apply();
  }

  public void setTypedRules(@NonNull String presetId, @NonNull List<TypedRule> typedRules) {
    final JSONObject json = readPresetJson(presetId);
    if (json == null) return;

    final JSONArray rulesJson = new JSONArray();
    int written = 0;
    for (TypedRule rule : typedRules) {
      if (rule == null) continue;
      if (written >= MAX_TYPED_RULES_PER_PRESET) break;
      if (rule.match.length() > MAX_RULE_MATCH_CHARS) continue;
      final String replace = rule.replace == null ? "" : rule.replace;
      if (replace.length() > MAX_RULE_REPLACE_CHARS) continue;
      final JSONObject ruleJson = new JSONObject();
      try {
        ruleJson.put("match", rule.match);
        ruleJson.put("replace", replace);
        ruleJson.put("auto_apply", rule.autoApply);
        ruleJson.put("enabled", rule.enabled);
        ruleJson.put("case_sensitive", rule.matchCaseSensitive);
        ruleJson.put("whole_word", rule.matchWholeWord);
      } catch (JSONException e) {
        throw new IllegalStateException("Failed to encode typed rule JSON.", e);
      }
      rulesJson.put(ruleJson);
      written++;
    }

    try {
      json.put(JSON_TYPED_RULES, rulesJson);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to update preset JSON.", e);
    }
    prefs.edit().putString(presetJsonKey(presetId), json.toString()).apply();
  }

  public void bindAppToPreset(
      @NonNull String packageName,
      @NonNull ContextFieldSelector selector,
      @NonNull String presetId) {
    final String pkg = packageName.trim();
    if (pkg.isEmpty()) throw new IllegalArgumentException("Package name must not be empty.");
    final String id = presetId.trim();
    if (id.isEmpty()) throw new IllegalArgumentException("Preset id must not be empty.");

    final JSONObject root = readBindingsJson();
    final JSONObject packageJson =
        root.optJSONObject(pkg) != null ? root.optJSONObject(pkg) : new JSONObject();
    final boolean alreadyBound = packageJson.has(selector.id());
    if (!alreadyBound && countBindings(root) >= MAX_APP_BINDINGS) {
      throw new IllegalStateException("Too many app bindings.");
    }
    try {
      packageJson.put(selector.id(), id);
      root.put(pkg, packageJson);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to write app binding.", e);
    }
    writeBindingsJson(root);
  }

  public void unbindApp(@NonNull String packageName, @NonNull ContextFieldSelector selector) {
    final String pkg = packageName.trim();
    if (pkg.isEmpty()) return;

    final JSONObject root = readBindingsJson();
    final JSONObject packageJson = root.optJSONObject(pkg);
    if (packageJson == null) return;
    packageJson.remove(selector.id());
    if (packageJson.length() == 0) {
      root.remove(pkg);
    } else {
      try {
        root.put(pkg, packageJson);
      } catch (JSONException e) {
        throw new IllegalStateException("Failed to write app binding.", e);
      }
    }
    writeBindingsJson(root);
  }

  @Nullable
  public String getBoundPresetId(
      @NonNull String packageName, @NonNull ContextFieldSelector selector) {
    final String pkg = packageName.trim();
    if (pkg.isEmpty()) return null;

    final JSONObject root = readBindingsJson();
    final JSONObject packageJson = root.optJSONObject(pkg);
    if (packageJson == null) return null;

    final String value = packageJson.optString(selector.id(), null);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  @NonNull
  public List<AppBinding> listBindings() {
    final JSONObject root = readBindingsJson();
    if (root.length() == 0) return Collections.emptyList();

    final List<AppBinding> out = new ArrayList<>();
    outer:
    for (Iterator<String> it = root.keys(); it.hasNext(); ) {
      final String pkg = it.next();
      if (pkg == null || pkg.trim().isEmpty()) continue;
      final JSONObject packageJson = root.optJSONObject(pkg);
      if (packageJson == null) continue;

      for (Iterator<String> keys = packageJson.keys(); keys.hasNext(); ) {
        final String selectorId = keys.next();
        final ContextFieldSelector selector = ContextFieldSelector.fromId(selectorId);
        if (selector == null) continue;
        final String presetId = packageJson.optString(selector.id(), null);
        if (presetId == null || presetId.trim().isEmpty()) continue;
        out.add(new AppBinding(pkg, selector, presetId.trim()));
        if (out.size() >= MAX_APP_BINDINGS) break outer;
      }
    }

    out.sort(
        Comparator.comparing(AppBinding::packageName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(binding -> binding.selector.id(), String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  public void clearAllBindings() {
    prefs.edit().remove(PREF_APP_BINDINGS_JSON).apply();
  }

  private void removePresetFromBindings(@NonNull String presetId) {
    final JSONObject root = readBindingsJson();
    if (root.length() == 0) return;

    final List<String> packagesToRemove = new ArrayList<>();
    for (Iterator<String> it = root.keys(); it.hasNext(); ) {
      final String pkg = it.next();
      final JSONObject packageJson = root.optJSONObject(pkg);
      if (packageJson == null) continue;

      final List<String> selectorsToRemove = new ArrayList<>();
      for (Iterator<String> keys = packageJson.keys(); keys.hasNext(); ) {
        final String selectorId = keys.next();
        if (presetId.equals(packageJson.optString(selectorId, ""))) {
          selectorsToRemove.add(selectorId);
        }
      }
      for (String selectorId : selectorsToRemove) packageJson.remove(selectorId);
      if (packageJson.length() == 0) packagesToRemove.add(pkg);
    }

    for (String pkg : packagesToRemove) root.remove(pkg);
    writeBindingsJson(root);
  }

  @NonNull
  private static String presetJsonKey(@NonNull String presetId) {
    return PREF_PRESET_JSON_PREFIX + presetId;
  }

  @Nullable
  private JSONObject readPresetJson(@NonNull String presetId) {
    final String raw = prefs.getString(presetJsonKey(presetId), null);
    if (raw == null || raw.trim().isEmpty()) return null;
    if (raw.length() > MAX_PRESET_JSON_CHARS) {
      prefs.edit().remove(presetJsonKey(presetId)).apply();
      return null;
    }
    try {
      return new JSONObject(raw);
    } catch (JSONException e) {
      // Corrupt JSON; wipe it and continue.
      prefs.edit().remove(presetJsonKey(presetId)).apply();
      return null;
    }
  }

  @NonNull
  private JSONObject readBindingsJson() {
    final String raw = prefs.getString(PREF_APP_BINDINGS_JSON, null);
    if (raw == null || raw.trim().isEmpty()) return new JSONObject();
    if (raw.length() > MAX_BINDINGS_JSON_CHARS) {
      prefs.edit().remove(PREF_APP_BINDINGS_JSON).apply();
      return new JSONObject();
    }
    try {
      return new JSONObject(raw);
    } catch (JSONException e) {
      // Corrupt JSON; wipe it and continue.
      prefs.edit().remove(PREF_APP_BINDINGS_JSON).apply();
      return new JSONObject();
    }
  }

  private void writeBindingsJson(@NonNull JSONObject json) {
    prefs.edit().putString(PREF_APP_BINDINGS_JSON, json.toString()).apply();
  }

  @NonNull
  private static List<VoiceRule> readVoiceRules(@Nullable JSONArray rules) {
    if (rules == null || rules.length() == 0) return Collections.emptyList();
    final int limit = Math.min(rules.length(), MAX_VOICE_RULES_PER_PRESET);
    final List<VoiceRule> out = new ArrayList<>(limit);
    for (int i = 0; i < limit; i++) {
      final JSONObject ruleJson = rules.optJSONObject(i);
      if (ruleJson == null) continue;
      final String match = ruleJson.optString("match", "").trim();
      final String replace = ruleJson.optString("replace", "");
      if (match.length() > MAX_RULE_MATCH_CHARS) continue;
      if (replace.length() > MAX_RULE_REPLACE_CHARS) continue;
      if (match.isEmpty()) continue;
      out.add(new VoiceRule(match, replace, ruleJson.optBoolean("auto_apply", false)));
    }
    return out;
  }

  @NonNull
  private static List<TypedRule> readTypedRules(@Nullable JSONArray rules) {
    if (rules == null || rules.length() == 0) return Collections.emptyList();
    final int limit = Math.min(rules.length(), MAX_TYPED_RULES_PER_PRESET);
    final List<TypedRule> out = new ArrayList<>(limit);
    for (int i = 0; i < limit; i++) {
      final JSONObject ruleJson = rules.optJSONObject(i);
      if (ruleJson == null) continue;
      final String match = ruleJson.optString("match", "").trim();
      final String replace = ruleJson.optString("replace", "");
      if (match.length() > MAX_RULE_MATCH_CHARS) continue;
      if (replace.length() > MAX_RULE_REPLACE_CHARS) continue;
      if (match.isEmpty()) continue;
      out.add(
          new TypedRule(
              match,
              replace,
              ruleJson.optBoolean("auto_apply", false),
              ruleJson.optBoolean("enabled", true),
              ruleJson.optBoolean("case_sensitive", false),
              ruleJson.optBoolean("whole_word", true)));
    }
    return out;
  }

  private static int countBindings(@NonNull JSONObject root) {
    if (root.length() == 0) return 0;

    int count = 0;
    for (Iterator<String> it = root.keys(); it.hasNext(); ) {
      final String pkg = it.next();
      if (pkg == null || pkg.trim().isEmpty()) continue;
      final JSONObject packageJson = root.optJSONObject(pkg);
      if (packageJson == null) continue;
      for (Iterator<String> keys = packageJson.keys(); keys.hasNext(); ) {
        final String selectorId = keys.next();
        final String presetId = packageJson.optString(selectorId, null);
        if (presetId == null || presetId.trim().isEmpty()) continue;
        count++;
        if (count >= MAX_APP_BINDINGS) return count;
      }
    }

    return count;
  }

  @NonNull
  private static SafeToggles readSafeToggles(@Nullable JSONObject json) {
    if (json == null) return SafeToggles.DEFAULT;
    return new SafeToggles(
        json.optBoolean(JSON_DISABLE_CONTACTS_DICTIONARY, false),
        json.optBoolean(JSON_DISABLE_USER_DICTIONARY, false),
        json.optBoolean(JSON_DISABLE_QUICK_FIXES, false),
        json.optBoolean(JSON_DISABLE_NEXT_WORD_SUGGESTIONS, false));
  }

  @NonNull
  private static JSONObject safeTogglesToJson(@NonNull SafeToggles toggles) {
    final JSONObject json = new JSONObject();
    try {
      json.put(JSON_DISABLE_CONTACTS_DICTIONARY, toggles.disableContactsDictionary);
      json.put(JSON_DISABLE_USER_DICTIONARY, toggles.disableUserDictionary);
      json.put(JSON_DISABLE_QUICK_FIXES, toggles.disableQuickFixes);
      json.put(JSON_DISABLE_NEXT_WORD_SUGGESTIONS, toggles.disableNextWordSuggestions);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to encode safe toggles JSON.", e);
    }
    return json;
  }

  public void setSecurityOptions(
      @NonNull String presetId,
      boolean containsPersonalContent,
      boolean allowAutoApplyInNoSuggestionsFields) {
    final JSONObject json = readPresetJson(presetId);
    if (json == null) return;
    try {
      json.put(JSON_CONTAINS_PERSONAL_CONTENT, containsPersonalContent);
      json.put(JSON_ALLOW_AUTO_APPLY_IN_NO_SUGGESTIONS_FIELDS, allowAutoApplyInNoSuggestionsFields);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to update preset JSON.", e);
    }
    prefs.edit().putString(presetJsonKey(presetId), json.toString()).apply();
  }

  public static final class Preset {
    @NonNull public final String id;
    @NonNull public final String name;
    public final long createdAtMillis;
    @NonNull public final List<VoiceRule> voiceRules;
    @NonNull public final List<TypedRule> typedRules;
    @NonNull public final SafeToggles safeToggles;
    public final boolean containsPersonalContent;
    public final boolean allowAutoApplyInNoSuggestionsFields;

    Preset(
        @NonNull String id,
        @NonNull String name,
        long createdAtMillis,
        @NonNull List<VoiceRule> voiceRules,
        @NonNull List<TypedRule> typedRules,
        @NonNull SafeToggles safeToggles,
        boolean containsPersonalContent,
        boolean allowAutoApplyInNoSuggestionsFields) {
      this.id = id;
      this.name = name;
      this.createdAtMillis = createdAtMillis;
      this.voiceRules = voiceRules;
      this.typedRules = typedRules;
      this.safeToggles = safeToggles;
      this.containsPersonalContent = containsPersonalContent;
      this.allowAutoApplyInNoSuggestionsFields = allowAutoApplyInNoSuggestionsFields;
    }
  }

  public static final class SafeToggles {
    public static final SafeToggles DEFAULT = new SafeToggles(false, false, false, false);

    public final boolean disableContactsDictionary;
    public final boolean disableUserDictionary;
    public final boolean disableQuickFixes;
    public final boolean disableNextWordSuggestions;

    public SafeToggles(
        boolean disableContactsDictionary,
        boolean disableUserDictionary,
        boolean disableQuickFixes,
        boolean disableNextWordSuggestions) {
      this.disableContactsDictionary = disableContactsDictionary;
      this.disableUserDictionary = disableUserDictionary;
      this.disableQuickFixes = disableQuickFixes;
      this.disableNextWordSuggestions = disableNextWordSuggestions;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SafeToggles)) return false;
      SafeToggles that = (SafeToggles) o;
      return disableContactsDictionary == that.disableContactsDictionary
          && disableUserDictionary == that.disableUserDictionary
          && disableQuickFixes == that.disableQuickFixes
          && disableNextWordSuggestions == that.disableNextWordSuggestions;
    }

    @Override
    public int hashCode() {
      int result = Boolean.hashCode(disableContactsDictionary);
      result = 31 * result + Boolean.hashCode(disableUserDictionary);
      result = 31 * result + Boolean.hashCode(disableQuickFixes);
      result = 31 * result + Boolean.hashCode(disableNextWordSuggestions);
      return result;
    }
  }

  public static final class VoiceRule {
    @NonNull public final String match;
    @NonNull public final String replace;
    public final boolean autoApply;

    public VoiceRule(@NonNull String match, @NonNull String replace, boolean autoApply) {
      final String trimmed = match.trim();
      if (trimmed.isEmpty()) throw new IllegalArgumentException("Match text must not be empty.");
      this.match = trimmed;
      this.replace = replace == null ? "" : replace;
      this.autoApply = autoApply;
    }
  }

  public static final class TypedRule {
    @NonNull public final String match;
    @NonNull public final String replace;
    public final boolean autoApply;
    public final boolean enabled;
    public final boolean matchCaseSensitive;
    public final boolean matchWholeWord;

    public TypedRule(
        @NonNull String match, @NonNull String replace, boolean autoApply, boolean enabled) {
      this(match, replace, autoApply, enabled, false, true);
    }

    public TypedRule(
        @NonNull String match,
        @NonNull String replace,
        boolean autoApply,
        boolean enabled,
        boolean matchCaseSensitive,
        boolean matchWholeWord) {
      final String trimmed = match.trim();
      if (trimmed.isEmpty()) throw new IllegalArgumentException("Match text must not be empty.");
      this.match = trimmed;
      this.replace = replace == null ? "" : replace;
      this.autoApply = autoApply;
      this.enabled = enabled;
      this.matchCaseSensitive = matchCaseSensitive;
      this.matchWholeWord = matchWholeWord;
    }
  }

  public static final class AppBinding {
    @NonNull public final String packageName;
    @NonNull public final ContextFieldSelector selector;
    @NonNull public final String presetId;

    AppBinding(
        @NonNull String packageName,
        @NonNull ContextFieldSelector selector,
        @NonNull String presetId) {
      this.packageName = packageName;
      this.selector = selector;
      this.presetId = presetId;
    }

    @NonNull
    public String packageName() {
      return packageName;
    }
  }
}
