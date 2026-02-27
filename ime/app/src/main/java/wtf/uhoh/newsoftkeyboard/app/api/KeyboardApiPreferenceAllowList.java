package wtf.uhoh.newsoftkeyboard.app.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;

final class KeyboardApiPreferenceAllowList {

  static final class PrefSpec {
    @NonNull final String key;
    @NonNull final String type;
    @NonNull final String readScope;
    @Nullable final String writeScope;
    final int minInt;
    final int maxInt;
    @Nullable final Set<String> allowedStringValues;

    private PrefSpec(
        @NonNull String key,
        @NonNull String type,
        @NonNull String readScope,
        @Nullable String writeScope,
        int minInt,
        int maxInt,
        @Nullable Set<String> allowedStringValues) {
      this.key = key;
      this.type = type;
      this.readScope = readScope;
      this.writeScope = writeScope;
      this.minInt = minInt;
      this.maxInt = maxInt;
      this.allowedStringValues = allowedStringValues;
    }

    static PrefSpec bool(
        @NonNull String key, @NonNull String readScope, @Nullable String writeScope) {
      return new PrefSpec(
          key, KeyboardApiContract.PREF_TYPE_BOOL, readScope, writeScope, 0, 0, null);
    }

    static PrefSpec integer(
        @NonNull String key,
        @NonNull String readScope,
        @Nullable String writeScope,
        int minInt,
        int maxInt) {
      return new PrefSpec(
          key, KeyboardApiContract.PREF_TYPE_INT, readScope, writeScope, minInt, maxInt, null);
    }

    static PrefSpec stringEnum(
        @NonNull String key,
        @NonNull String readScope,
        @Nullable String writeScope,
        @NonNull Set<String> allowedValues) {
      return new PrefSpec(
          key,
          KeyboardApiContract.PREF_TYPE_STRING,
          readScope,
          writeScope,
          0,
          0,
          Collections.unmodifiableSet(new HashSet<>(allowedValues)));
    }
  }

  @NonNull
  static Map<String, PrefSpec> build(@NonNull android.content.Context context) {
    final HashMap<String, PrefSpec> m = new HashMap<>();

    final String scopeRead = KeyboardApiContract.SCOPE_PREFS_READ;
    final String scopeWriteEffects = KeyboardApiContract.SCOPE_PREFS_WRITE_EFFECTS;
    final String scopeWriteTyping = KeyboardApiContract.SCOPE_PREFS_WRITE_TYPING;
    final String scopeWriteGestures = KeyboardApiContract.SCOPE_PREFS_WRITE_GESTURES;
    final String scopeWriteUi = KeyboardApiContract.SCOPE_PREFS_WRITE_UI;
    final String scopeWriteClipboard = KeyboardApiContract.SCOPE_PREFS_WRITE_CLIPBOARD;
    final String scopeWriteVoice = KeyboardApiContract.SCOPE_PREFS_WRITE_VOICE;

    // Effects (safe controls)
    m.put(
        context.getString(R.string.settings_key_sound_on),
        PrefSpec.bool(
            context.getString(R.string.settings_key_sound_on), scopeRead, scopeWriteEffects));
    m.put(
        context.getString(R.string.settings_key_vibrate_on_long_press),
        PrefSpec.bool(
            context.getString(R.string.settings_key_vibrate_on_long_press),
            scopeRead,
            scopeWriteEffects));
    m.put(
        context.getString(R.string.settings_key_use_system_vibration),
        PrefSpec.bool(
            context.getString(R.string.settings_key_use_system_vibration),
            scopeRead,
            scopeWriteEffects));

    m.put(
        context.getString(R.string.settings_key_vibrate_on_key_press_duration_int),
        PrefSpec.integer(
            context.getString(R.string.settings_key_vibrate_on_key_press_duration_int),
            scopeRead,
            scopeWriteEffects,
            context
                .getResources()
                .getInteger(
                    R.integer
                        .settings_default_vibrate_on_key_press_duration_min_value_including_system),
            context
                .getResources()
                .getInteger(R.integer.settings_default_vibrate_on_key_press_duration_max_value)));

    m.put(
        context.getString(R.string.settings_key_system_vibration_fallback_duration_int),
        PrefSpec.integer(
            context.getString(R.string.settings_key_system_vibration_fallback_duration_int),
            scopeRead,
            scopeWriteEffects,
            context
                .getResources()
                .getInteger(R.integer.settings_default_vibrate_on_key_press_duration_min_value),
            context
                .getResources()
                .getInteger(R.integer.settings_default_vibrate_on_key_press_duration_max_value)));

    m.put(
        context.getString(R.string.settings_key_key_press_shows_preview_popup),
        PrefSpec.bool(
            context.getString(R.string.settings_key_key_press_shows_preview_popup),
            scopeRead,
            scopeWriteEffects));

    // Typing behavior (privacy-safe toggles; does not expose content)
    m.put(
        context.getString(R.string.settings_key_auto_capitalization),
        PrefSpec.bool(
            context.getString(R.string.settings_key_auto_capitalization),
            scopeRead,
            scopeWriteTyping));
    m.put(
        context.getString(R.string.settings_key_show_suggestions),
        PrefSpec.bool(
            context.getString(R.string.settings_key_show_suggestions),
            scopeRead,
            scopeWriteTyping));
    m.put(
        context.getString(R.string.settings_key_auto_space),
        PrefSpec.bool(
            context.getString(R.string.settings_key_auto_space), scopeRead, scopeWriteTyping));
    m.put(
        context.getString(R.string.settings_key_bool_should_swap_punctuation_and_space),
        PrefSpec.bool(
            context.getString(R.string.settings_key_bool_should_swap_punctuation_and_space),
            scopeRead,
            scopeWriteTyping));

    // UI (non-sensitive)
    m.put(
        context.getString(R.string.settings_key_keyboard_icon_in_status_bar),
        PrefSpec.bool(
            context.getString(R.string.settings_key_keyboard_icon_in_status_bar),
            scopeRead,
            scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_colorize_nav_bar),
        PrefSpec.bool(
            context.getString(R.string.settings_key_colorize_nav_bar), scopeRead, scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_show_keyboard_name_text_key),
        PrefSpec.bool(
            context.getString(R.string.settings_key_show_keyboard_name_text_key),
            scopeRead,
            scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_show_hint_text_key),
        PrefSpec.bool(
            context.getString(R.string.settings_key_show_hint_text_key), scopeRead, scopeWriteUi));

    // Night mode and power saving (safe, non-sensitive)
    final Set<String> nightModeValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.night_mode_values)));
    m.put(
        context.getString(R.string.settings_key_night_mode),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_night_mode),
            scopeRead,
            scopeWriteUi,
            nightModeValues));
    m.put(
        context.getString(R.string.settings_key_night_mode_app_theme_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_night_mode_app_theme_control),
            scopeRead,
            scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_night_mode_theme_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_night_mode_theme_control),
            scopeRead,
            scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_night_mode_sound_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_night_mode_sound_control),
            scopeRead,
            scopeWriteEffects));
    m.put(
        context.getString(R.string.settings_key_night_mode_vibration_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_night_mode_vibration_control),
            scopeRead,
            scopeWriteEffects));

    final Set<String> powerSaveModeValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.power_save_mode_values)));
    m.put(
        context.getString(R.string.settings_key_power_save_mode),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_power_save_mode),
            scopeRead,
            scopeWriteUi,
            powerSaveModeValues));
    m.put(
        context.getString(R.string.settings_key_power_save_mode_animation_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_power_save_mode_animation_control),
            scopeRead,
            scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_power_save_mode_theme_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_power_save_mode_theme_control),
            scopeRead,
            scopeWriteUi));
    m.put(
        context.getString(R.string.settings_key_power_save_mode_suggestions_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_power_save_mode_suggestions_control),
            scopeRead,
            scopeWriteTyping));
    m.put(
        context.getString(R.string.settings_key_power_save_mode_gesture_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_power_save_mode_gesture_control),
            scopeRead,
            scopeWriteGestures));
    m.put(
        context.getString(R.string.settings_key_power_save_mode_sound_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_power_save_mode_sound_control),
            scopeRead,
            scopeWriteEffects));
    m.put(
        context.getString(R.string.settings_key_power_save_mode_vibration_control),
        PrefSpec.bool(
            context.getString(R.string.settings_key_power_save_mode_vibration_control),
            scopeRead,
            scopeWriteEffects));

    // Voice (safe toggles + enums; no secrets)
    final Set<String> speechToTextBackendValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.speech_to_text_backend_values)));
    m.put(
        context.getString(R.string.settings_key_speech_to_text_backend),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_speech_to_text_backend),
            scopeRead,
            scopeWriteVoice,
            speechToTextBackendValues));

    final Set<String> openAiModelValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.openai_model_values)));
    m.put(
        context.getString(R.string.settings_key_openai_model),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_openai_model),
            scopeRead,
            scopeWriteVoice,
            openAiModelValues));

    final Set<String> openAiResponseFormatValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.openai_response_format_values)));
    m.put(
        context.getString(R.string.settings_key_openai_response_format),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_openai_response_format),
            scopeRead,
            scopeWriteVoice,
            openAiResponseFormatValues));

    final Set<String> openAiChunkingStrategyValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.openai_chunking_strategy_values)));
    m.put(
        context.getString(R.string.settings_key_openai_chunking_strategy),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_openai_chunking_strategy),
            scopeRead,
            scopeWriteVoice,
            openAiChunkingStrategyValues));

    m.put(
        context.getString(R.string.settings_key_openai_add_trailing_space),
        PrefSpec.bool(
            context.getString(R.string.settings_key_openai_add_trailing_space),
            scopeRead,
            scopeWriteVoice));
    m.put(
        context.getString(R.string.settings_key_elevenlabs_add_trailing_space),
        PrefSpec.bool(
            context.getString(R.string.settings_key_elevenlabs_add_trailing_space),
            scopeRead,
            scopeWriteVoice));
    m.put(
        context.getString(R.string.settings_key_openai_auto_punctuation),
        PrefSpec.bool(
            context.getString(R.string.settings_key_openai_auto_punctuation),
            scopeRead,
            scopeWriteVoice));
    m.put(
        context.getString(R.string.settings_key_openai_timestamps),
        PrefSpec.bool(
            context.getString(R.string.settings_key_openai_timestamps),
            scopeRead,
            scopeWriteVoice));

    // Gestures & quick-keys (safe)
    final Set<String> swipeActionValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context.getResources().getStringArray(R.array.swipe_action_types_values)));
    final Set<String> swipeVelocityValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context
                    .getResources()
                    .getStringArray(R.array.settings_key_swipe_velocity_threshold_values)));
    final Set<String> swipeDistanceValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context
                    .getResources()
                    .getStringArray(R.array.settings_key_swipe_distance_threshold_values)));

    m.put(
        context.getString(R.string.settings_key_gesture_typing),
        PrefSpec.bool(
            context.getString(R.string.settings_key_gesture_typing),
            scopeRead,
            scopeWriteGestures));

    m.put(
        context.getString(R.string.settings_key_swipe_up_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_up_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_down_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_down_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_left_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_left_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_right_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_right_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_left_space_bar_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_left_space_bar_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_right_space_bar_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_right_space_bar_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_left_two_fingers_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_left_two_fingers_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_right_two_fingers_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_right_two_fingers_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_swipe_up_from_spacebar_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_up_from_spacebar_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_pinch_gesture_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_pinch_gesture_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));
    m.put(
        context.getString(R.string.settings_key_separate_gesture_action),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_separate_gesture_action),
            scopeRead,
            scopeWriteGestures,
            swipeActionValues));

    m.put(
        context.getString(R.string.settings_key_swipe_velocity_threshold),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_velocity_threshold),
            scopeRead,
            scopeWriteGestures,
            swipeVelocityValues));
    m.put(
        context.getString(R.string.settings_key_swipe_distance_threshold),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_swipe_distance_threshold),
            scopeRead,
            scopeWriteGestures,
            swipeDistanceValues));

    m.put(
        context.getString(R.string.settings_key_one_shot_quick_text_popup),
        PrefSpec.bool(
            context.getString(R.string.settings_key_one_shot_quick_text_popup),
            scopeRead,
            scopeWriteGestures));
    m.put(
        context.getString(R.string.settings_key_search_quick_text_tags),
        PrefSpec.bool(
            context.getString(R.string.settings_key_search_quick_text_tags),
            scopeRead,
            scopeWriteGestures));

    final Set<String> initialQuickTextTabValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context
                    .getResources()
                    .getStringArray(R.array.settings_key_initial_quick_text_tab_values)));
    m.put(
        context.getString(R.string.settings_key_initial_quick_text_tab),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_initial_quick_text_tab),
            scopeRead,
            scopeWriteGestures,
            initialQuickTextTabValues));

    final Set<String> emojiGenderValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context
                    .getResources()
                    .getStringArray(R.array.settings_key_default_emoji_gender_values)));
    m.put(
        context.getString(R.string.settings_key_default_emoji_gender),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_default_emoji_gender),
            scopeRead,
            scopeWriteGestures,
            emojiGenderValues));

    final Set<String> emojiSkinToneValues =
        new HashSet<>(
            java.util.Arrays.asList(
                context
                    .getResources()
                    .getStringArray(R.array.settings_key_default_emoji_skin_tone_values)));
    m.put(
        context.getString(R.string.settings_key_default_emoji_skin_tone),
        PrefSpec.stringEnum(
            context.getString(R.string.settings_key_default_emoji_skin_tone),
            scopeRead,
            scopeWriteGestures,
            emojiSkinToneValues));

    m.put(
        context.getString(R.string.settings_key_do_not_flip_quick_key_codes_functionality),
        PrefSpec.bool(
            context.getString(R.string.settings_key_do_not_flip_quick_key_codes_functionality),
            scopeRead,
            scopeWriteGestures));

    // Clipboard behavior (do not expose clipboard content)
    m.put(
        context.getString(R.string.settings_key_os_clipboard_sync),
        PrefSpec.bool(
            context.getString(R.string.settings_key_os_clipboard_sync),
            scopeRead,
            scopeWriteClipboard));

    return Collections.unmodifiableMap(m);
  }

  @NonNull
  static List<String> getAllowedPrefKeys(@NonNull Map<String, PrefSpec> allowList) {
    final ArrayList<String> keys = new ArrayList<>(allowList.size());
    keys.addAll(allowList.keySet());
    Collections.sort(keys);
    return keys;
  }

  @NonNull
  static List<String> getAllowedPrefTypes(
      @NonNull List<String> sortedKeys, @NonNull Map<String, PrefSpec> allowList) {
    final ArrayList<String> types = new ArrayList<>(sortedKeys.size());
    for (String key : sortedKeys) {
      final PrefSpec spec = allowList.get(key);
      types.add(spec == null ? "" : spec.type);
    }
    return types;
  }
}
