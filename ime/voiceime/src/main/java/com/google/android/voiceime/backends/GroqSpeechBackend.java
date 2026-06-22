/*
 * Copyright (C) 2026 AnySoftKeyboard
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

package com.google.android.voiceime.backends;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.voiceime.OpenAITranscriber;
import com.google.android.voiceime.R;
import com.google.android.voiceime.utils.SpeechToTextSecretsStore;
import java.io.File;

/**
 * Speech-to-text backend for Groq's transcription API (e.g. whisper-large-v3-turbo).
 *
 * <p>Groq exposes an OpenAI-compatible audio-transcription endpoint (Bearer auth, multipart {@code
 * file}/{@code model}), so this backend reuses {@link OpenAITranscriber} rather than duplicating the
 * HTTP layer. Only the defaults (endpoint, model) and the API-key storage differ.
 */
public final class GroqSpeechBackend implements SpeechToTextBackend {

  public static final String ID = "groq";

  static final String DEFAULT_ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions";
  static final String DEFAULT_MODEL = "whisper-large-v3-turbo";

  private final OpenAITranscriber mTranscriber = new OpenAITranscriber();

  private static final OpenAITranscriber.ErrorMessages GROQ_ERRORS =
      new OpenAITranscriber.ErrorMessages(
          R.string.groq_error_api_key_unset,
          R.string.groq_error_endpoint_unset,
          R.string.groq_error_endpoint_insecure,
          R.string.groq_error_network,
          R.string.groq_error_transcription_failed,
          R.string.groq_error_api_error,
          R.string.groq_error_recording_failed);

  @NonNull
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isSelected(@NonNull Context context, @NonNull SharedPreferences prefs) {
    String selectionKey = context.getString(R.string.settings_key_speech_to_text_backend);
    // Default OFF: no backend is selected until the user explicitly opts in.
    String selectedBackend = prefs.getString(selectionKey, null);
    return ID.equals(selectedBackend);
  }

  @Override
  public boolean isConfigured(@NonNull Context context, @NonNull SharedPreferences prefs) {
    if (!isSelected(context, prefs)) {
      return false;
    }
    return readAndMigrateApiKey(context, prefs) != null;
  }

  @Override
  public void showConfigurationError(@NonNull Context context) {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(
        () ->
            Toast.makeText(
                    context, context.getString(R.string.groq_error_api_key_unset), Toast.LENGTH_LONG)
                .show());
  }

  @Override
  public void startTranscription(
      @NonNull InputMethodService ime,
      @NonNull SharedPreferences prefs,
      @NonNull File audioFile,
      @NonNull String mediaType,
      @NonNull TranscriptionResultCallback callback) {

    Context context = ime.getApplicationContext();

    String apiKey = readAndMigrateApiKey(context, prefs);
    if (apiKey == null) {
      callback.onError(context.getString(R.string.groq_error_api_key_unset));
      return;
    }

    String endpoint =
        prefs.getString(context.getString(R.string.settings_key_groq_endpoint), DEFAULT_ENDPOINT);
    endpoint = endpoint != null ? endpoint.trim() : null;
    if (endpoint == null || endpoint.isEmpty()) {
      callback.onError(context.getString(R.string.groq_error_endpoint_unset));
      return;
    }
    if (!isHttpsUrl(endpoint)) {
      callback.onError(context.getString(R.string.groq_error_endpoint_insecure));
      return;
    }
    String model =
        prefs.getString(context.getString(R.string.settings_key_groq_model), DEFAULT_MODEL);
    String language = prefs.getString(context.getString(R.string.settings_key_groq_language), "");
    boolean addTrailingSpace =
        prefs.getBoolean(context.getString(R.string.settings_key_groq_add_trailing_space), true);

    callback.onTranscriptionStarted();
    // Groq's Whisper models take the OpenAI-compatible parameter set. The OpenAI-only options
    // (temperature/prompt/chunking) are inert here: chunking is gated to gpt-4o models and the
    // others are only attached when non-empty, so the defaults below are effectively no-ops.
    mTranscriber.startAsync(
        context,
        audioFile.getAbsolutePath(),
        mediaType,
        apiKey,
        endpoint,
        model,
        language,
        /* temperature= */ "0.0",
        /* responseFormat= */ "text",
        /* chunkingStrategy= */ "none",
        /* prompt= */ "",
        addTrailingSpace,
        /* useDefaultPrompt= */ false,
        /* defaultPromptType= */ "whisper",
        /* appendCustomPrompt= */ true,
        GROQ_ERRORS,
        new OpenAITranscriber.TranscriptionCallback() {
          @Override
          public void onResult(String result) {
            callback.onSuccess(result);
          }

          @Override
          public void onError(String error) {
            callback.onError(error);
          }
        });
  }

  private static boolean isHttpsUrl(@NonNull String rawUrl) {
    try {
      Uri uri = Uri.parse(rawUrl);
      return uri != null && "https".equalsIgnoreCase(uri.getScheme());
    } catch (Exception e) {
      return false;
    }
  }

  @Nullable
  private static String readAndMigrateApiKey(
      @NonNull Context context, @NonNull SharedPreferences prefs) {
    String apiKey = SpeechToTextSecretsStore.getGroqApiKey(context);
    if (apiKey != null && !apiKey.isEmpty()) {
      return apiKey;
    }

    String legacyKeyName = context.getString(R.string.settings_key_groq_api_key);
    String legacyApiKey = prefs.getString(legacyKeyName, "");
    if (legacyApiKey == null || legacyApiKey.isEmpty()) {
      return null;
    }

    SpeechToTextSecretsStore.setGroqApiKey(context, legacyApiKey);
    prefs.edit().remove(legacyKeyName).apply();
    return legacyApiKey;
  }
}
