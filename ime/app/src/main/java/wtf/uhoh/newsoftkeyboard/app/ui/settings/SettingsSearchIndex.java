package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;

final class SettingsSearchIndex {

  private SettingsSearchIndex() {}

  static void buildIndex(@NonNull Context context, @NonNull List<SearchItem> out) {
    final List<ScreenSpec> screens = new ArrayList<>();

    final String keyboardsCategory =
        context.getString(R.string.settings_category_keyboards_language_packs);
    final String typingCategory = context.getString(R.string.settings_category_typing);
    final String lookCategory = context.getString(R.string.settings_category_look_and_feel);
    final String gesturesCategory =
        context.getString(R.string.settings_category_gestures_quick_keys);
    final String clipboardCategory = context.getString(R.string.settings_category_clipboard);
    final String voiceCategory = context.getString(R.string.settings_category_voice);
    final String troubleshootingCategory =
        context.getString(R.string.settings_category_troubleshooting_backup);
    final String settingsUiCategory =
        context.getString(R.string.settings_category_settings_ui_launcher);
    final String helpCategory = context.getString(R.string.settings_category_help_about);

    screens.add(
        new ScreenSpec(
            R.id.keyboardsAndLanguagePacksFragment,
            R.xml.prefs_keyboards_language_packs,
            keyboardsCategory));
    screens.add(
        new ScreenSpec(R.id.typingSettingsFragment, R.xml.prefs_typing_settings, typingCategory));
    screens.add(
        new ScreenSpec(
            R.id.contextProfilesSettingsFragment,
            R.xml.prefs_context_profiles,
            typingCategory
                + " \u2192 "
                + context.getString(R.string.context_profiles_settings_title)));
    screens.add(
        new ScreenSpec(
            R.id.lookAndFeelSettingsFragment, R.xml.prefs_look_and_feel_settings, lookCategory));
    screens.add(
        new ScreenSpec(
            R.id.gesturesAndQuickKeysSettingsFragment,
            R.xml.prefs_gestures_and_quick_keys_settings,
            gesturesCategory));
    screens.add(
        new ScreenSpec(
            R.id.clipboardSettingsFragment, R.xml.prefs_clipboard_settings, clipboardCategory));
    screens.add(
        new ScreenSpec(R.id.voiceSettingsFragment, R.xml.prefs_speech_to_text, voiceCategory));
    screens.add(
        new ScreenSpec(
            R.id.troubleshootingAndBackupSettingsFragment,
            R.xml.prefs_troubleshooting_backup_settings,
            troubleshootingCategory));
    screens.add(
        new ScreenSpec(
            R.id.settingsUiAndLauncherFragment,
            R.xml.prefs_settings_ui_launcher,
            settingsUiCategory));
    screens.add(new ScreenSpec(R.id.helpAndAboutFragment, R.xml.prefs_help_about, helpCategory));

    final String nextWordPathPrefix =
        typingCategory + " \u2192 " + context.getString(R.string.settings_typing_next_word_title);
    screens.add(
        new ScreenSpec(R.id.nextWordSettingsFragment, R.xml.prefs_next_word, nextWordPathPrefix));
    screens.add(
        new ScreenSpec(R.id.presageModelsFragment, R.xml.prefs_presage_models, nextWordPathPrefix));

    final String powerSavingPrefix =
        troubleshootingCategory
            + " \u2192 "
            + context.getString(R.string.settings_performance_battery_section_title);
    screens.add(
        new ScreenSpec(
            R.id.powerSavingSettingsFragment, R.xml.power_saving_prefs, powerSavingPrefix));

    final String nightModePrefix =
        lookCategory + " \u2192 " + context.getString(R.string.settings_look_and_feel_theme_title);
    screens.add(
        new ScreenSpec(R.id.nightModeSettingsFragment, R.xml.night_mode_prefs, nightModePrefix));

    final String openAiPrefix =
        voiceCategory + " \u2192 " + context.getString(R.string.openai_speech_settings_title);
    screens.add(
        new ScreenSpec(R.id.openAISpeechSettingsFragment, R.xml.prefs_openai_speech, openAiPrefix));

    final String elevenLabsPrefix =
        voiceCategory + " \u2192 " + context.getString(R.string.elevenlabs_speech_settings_title);
    screens.add(
        new ScreenSpec(
            R.id.elevenLabsSpeechSettingsFragment,
            R.xml.prefs_elevenlabs_speech,
            elevenLabsPrefix));

    final Map<String, ActionTarget> actionTargets = buildActionTargets(context);
    final Set<String> hardwareKeyboardKeys = buildHardwareKeyboardKeys(context);
    final boolean hasHardwareKeyboard =
        context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

    final PreferenceManager preferenceManager = new PreferenceManager(context);
    for (ScreenSpec screen : screens) {
      final PreferenceScreen screenRoot =
          preferenceManager.inflateFromResource(context, screen.preferencesXmlResId, null);
      collectFromGroup(
          out,
          screenRoot,
          screen.pathPrefix,
          null,
          actionTargets,
          hardwareKeyboardKeys,
          hasHardwareKeyboard,
          screen.destinationId);
    }

    collectThemeCustomizationControls(context, out);
  }

  private static void collectThemeCustomizationControls(
      @NonNull Context context, @NonNull List<SearchItem> out) {
    final String pathPrefix =
        context.getString(R.string.settings_category_look_and_feel)
            + " \u2192 "
            + context.getString(R.string.settings_look_and_feel_theme_title)
            + " \u2192 "
            + context.getString(R.string.keyboard_theme_wallpaper_customization_title);
    final int destinationId = R.id.keyboardThemeCustomizationFragment;

    for (AppearanceOwnerRegistry.ThemeCustomizationSearchEntry entry :
        AppearanceOwnerRegistry.themeCustomizationSearchEntries()) {
      addCustomSearchItem(
          context,
          out,
          entry.titleResId,
          entry.summaryResId,
          pathPrefix,
          destinationId,
          entry.scrollToKey);
    }
  }

  private static void addCustomSearchItem(
      @NonNull Context context,
      @NonNull List<SearchItem> out,
      int titleResId,
      int summaryResId,
      @NonNull String path,
      int destinationId,
      @Nullable String scrollToPreferenceKey) {
    out.add(
        new SearchItem(
            context.getString(titleResId),
            context.getString(summaryResId),
            path,
            TypeBadge.EDITOR,
            false,
            true,
            destinationId,
            scrollToPreferenceKey));
  }

  @NonNull
  private static Map<String, ActionTarget> buildActionTargets(@NonNull Context context) {
    final Map<String, ActionTarget> targets = new HashMap<>();

    targets.put(
        "nav:keyboards_manager",
        new ActionTarget(R.id.keyboardAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:language_packs_manager",
        new ActionTarget(R.id.keyboardsAndLanguagePacksFragment, TypeBadge.MANAGER, null));

    targets.put(
        "nav:toolbar_top_row_selector",
        new ActionTarget(R.id.topRowAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:toolbar_swipe_row_selector",
        new ActionTarget(R.id.extensionAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:toolbar_bottom_row_selector",
        new ActionTarget(R.id.bottomRowAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:toolbar_input_field_modes",
        new ActionTarget(
            R.id.lookAndFeelSettingsFragment, TypeBadge.ACTION, "nav:toolbar_input_field_modes"));

    targets.put(
        "nav:user_dictionary_editor",
        new ActionTarget(R.id.userDictionaryEditorFragment, TypeBadge.EDITOR, null));
    targets.put(
        "nav:abbreviation_editor",
        new ActionTarget(R.id.abbreviationDictionaryEditorFragment, TypeBadge.EDITOR, null));
    targets.put(
        "nav:next_word_settings",
        new ActionTarget(R.id.nextWordSettingsFragment, TypeBadge.EDITOR, null));

    targets.put(
        "nav:quick_keys_manager",
        new ActionTarget(R.id.quickTextKeysBrowseFragment, TypeBadge.MANAGER, null));

    targets.put(
        "speech_to_text_openai_settings",
        new ActionTarget(R.id.openAISpeechSettingsFragment, TypeBadge.EDITOR, null));
    targets.put(
        "speech_to_text_elevenlabs_settings",
        new ActionTarget(R.id.elevenLabsSpeechSettingsFragment, TypeBadge.EDITOR, null));

    targets.put(
        "nav:nextword_models",
        new ActionTarget(R.id.presageModelsFragment, TypeBadge.MANAGER, null));
    targets.put(
        context.getString(R.string.settings_key_manage_presage_models),
        new ActionTarget(R.id.presageModelsFragment, TypeBadge.MANAGER, null));

    targets.put(
        "nav:developer_tools",
        new ActionTarget(R.id.developerToolsFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:logcat_viewer", new ActionTarget(R.id.logCatViewFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:about", new ActionTarget(R.id.aboutNewSoftKeyboardFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:licenses",
        new ActionTarget(R.id.additionalSoftwareLicensesFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:changelog", new ActionTarget(R.id.fullChangeLogFragment, TypeBadge.ACTION, null));

    // Registry-driven action targets (deep-link to the single owner surface).
    for (AppearanceOwnerRegistry.Owner owner : AppearanceOwnerRegistry.owners()) {
      if (targets.containsKey(owner.canonicalId)) continue;
      if (!(owner.canonicalId.startsWith("nav:")
          || owner.canonicalId.startsWith("settings_key_"))) {
        continue;
      }

      final TypeBadge typeBadge;
      if (owner.canonicalId.startsWith("settings_key_")) {
        typeBadge = TypeBadge.SETTING;
      } else {
        typeBadge =
            switch (owner.ownerDestinationId) {
              case R.id.keyboardThemeSelectorFragment -> TypeBadge.MANAGER;
              case R.id.keyboardThemeCustomizationFragment,
                  R.id.nightModeSettingsFragment,
                  R.id.powerSavingSettingsFragment ->
                  TypeBadge.EDITOR;
              default -> TypeBadge.ACTION;
            };
      }

      targets.put(
          owner.canonicalId,
          new ActionTarget(owner.ownerDestinationId, typeBadge, owner.scrollToKey));
    }

    return targets;
  }

  @NonNull
  private static Set<String> buildHardwareKeyboardKeys(@NonNull Context context) {
    final Set<String> keys = new HashSet<>();
    keys.add("use_keyrepeat");
    keys.add(context.getString(R.string.settings_key_hide_soft_when_physical));
    keys.add(context.getString(R.string.settings_key_enable_alt_space_language_shortcut));
    keys.add(context.getString(R.string.settings_key_enable_shift_space_language_shortcut));
    return keys;
  }

  private static void collectFromGroup(
      @NonNull List<SearchItem> out,
      @NonNull PreferenceGroup group,
      @NonNull String pathPrefix,
      @Nullable String currentSection,
      @NonNull Map<String, ActionTarget> actionTargets,
      @NonNull Set<String> hardwareKeyboardKeys,
      boolean hasHardwareKeyboard,
      int defaultDestinationId) {
    for (int i = 0; i < group.getPreferenceCount(); i++) {
      final Preference preference = group.getPreference(i);
      if (preference instanceof PreferenceCategory) {
        final CharSequence title = preference.getTitle();
        final String sectionTitle = title == null ? null : title.toString();
        collectFromGroup(
            out,
            (PreferenceCategory) preference,
            pathPrefix,
            sectionTitle,
            actionTargets,
            hardwareKeyboardKeys,
            hasHardwareKeyboard,
            defaultDestinationId);
        continue;
      }

      if (preference instanceof PreferenceGroup) {
        collectFromGroup(
            out,
            (PreferenceGroup) preference,
            pathPrefix,
            currentSection,
            actionTargets,
            hardwareKeyboardKeys,
            hasHardwareKeyboard,
            defaultDestinationId);
        continue;
      }

      final CharSequence title = preference.getTitle();
      if (title == null || TextUtils.isEmpty(title)) {
        continue;
      }

      final String key = preference.getKey();
      if (TextUtils.isEmpty(key)) {
        continue;
      }

      if (key.startsWith("info:") || "summary".equals(key)) {
        continue;
      }

      final String path =
          currentSection == null ? pathPrefix : pathPrefix + " \u2192 " + currentSection;

      final ActionTarget actionTarget = actionTargets.get(key);
      final TypeBadge typeBadge;
      final int destinationId;
      final String scrollToKey;
      if (actionTarget != null) {
        typeBadge = actionTarget.typeBadge;
        destinationId = actionTarget.destinationId;
        scrollToKey = actionTarget.scrollToPreferenceKey;
      } else {
        typeBadge = guessBadgeFromPreference(preference);
        destinationId = defaultDestinationId;
        scrollToKey = key;
      }

      final boolean isBeta = containsBetaMarker(title.toString());
      final boolean enabled =
          hasHardwareKeyboard
              || !hardwareKeyboardKeys.contains(key)
              || typeBadge != TypeBadge.SETTING;

      final CharSequence summary = preference.getSummary();
      out.add(
          new SearchItem(
              title.toString(),
              summary == null ? "" : summary.toString(),
              path,
              typeBadge,
              isBeta,
              enabled,
              destinationId,
              scrollToKey));
    }
  }

  @NonNull
  private static TypeBadge guessBadgeFromPreference(@NonNull Preference preference) {
    if (preference instanceof androidx.preference.TwoStatePreference
        || preference instanceof androidx.preference.ListPreference
        || preference instanceof androidx.preference.EditTextPreference) {
      return TypeBadge.SETTING;
    }
    if (preference instanceof net.evendanan.pixel.SlidePreference) {
      return TypeBadge.SETTING;
    }
    // Plain Preference with a key is typically a navigation/action row.
    if ("nav:".equals(preference.getKey())) {
      return TypeBadge.ACTION;
    }
    if (preference.getKey() != null && preference.getKey().startsWith("nav:")) {
      return TypeBadge.ACTION;
    }
    return TypeBadge.ACTION;
  }

  private static boolean containsBetaMarker(@NonNull String title) {
    final String lower = title.toLowerCase(Locale.US);
    // Avoid matching words like "alphabet".
    return lower.contains("[beta]") || lower.contains("[beta") || lower.contains("beta]");
  }

  enum TypeBadge {
    SETTING("Setting"),
    ACTION("Action"),
    MANAGER("Manager"),
    EDITOR("Editor");

    final String label;

    TypeBadge(String label) {
      this.label = label;
    }
  }

  private static final class ScreenSpec {
    final int destinationId;
    final int preferencesXmlResId;
    @NonNull final String pathPrefix;

    ScreenSpec(int destinationId, int preferencesXmlResId, @NonNull String pathPrefix) {
      this.destinationId = destinationId;
      this.preferencesXmlResId = preferencesXmlResId;
      this.pathPrefix = pathPrefix;
    }
  }

  private static final class ActionTarget {
    final int destinationId;
    @NonNull final TypeBadge typeBadge;
    @Nullable final String scrollToPreferenceKey;

    ActionTarget(
        int destinationId, @NonNull TypeBadge typeBadge, @Nullable String scrollToPreferenceKey) {
      this.destinationId = destinationId;
      this.typeBadge = typeBadge;
      this.scrollToPreferenceKey = scrollToPreferenceKey;
    }
  }

  static final class SearchItem {
    @NonNull final String title;
    @NonNull final String summary;
    @NonNull final String path;
    @NonNull final TypeBadge typeBadge;
    final boolean beta;
    final boolean enabled;
    final int destinationId;
    @Nullable final String scrollToPreferenceKey;
    @NonNull final String searchableText;

    SearchItem(
        @NonNull String title,
        @NonNull String summary,
        @NonNull String path,
        @NonNull TypeBadge typeBadge,
        boolean beta,
        boolean enabled,
        int destinationId,
        @Nullable String scrollToPreferenceKey) {
      this.title = title;
      this.summary = summary;
      this.path = path;
      this.typeBadge = typeBadge;
      this.beta = beta;
      this.enabled = enabled;
      this.destinationId = destinationId;
      this.scrollToPreferenceKey = scrollToPreferenceKey;
      this.searchableText =
          (title + "\n" + summary + "\n" + path + "\n" + typeBadge.label).toLowerCase(Locale.US);
    }
  }
}
