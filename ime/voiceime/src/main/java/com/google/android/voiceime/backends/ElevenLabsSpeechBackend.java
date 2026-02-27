/*
 * Copyright (C) 2025 AnySoftKeyboard
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
import com.google.android.voiceime.ElevenLabsTranscriber;
import com.google.android.voiceime.R;
import com.google.android.voiceime.utils.SpeechToTextSecretsStore;
import java.io.File;

/** Speech-to-text backend for the ElevenLabs transcription API. */
public final class ElevenLabsSpeechBackend implements SpeechToTextBackend {

  public static final String ID = "elevenlabs";

  private final ElevenLabsTranscriber mTranscriber = new ElevenLabsTranscriber();

  @NonNull
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isSelected(@NonNull Context context, @NonNull SharedPreferences prefs) {
    String selectionKey = context.getString(R.string.settings_key_speech_to_text_backend);
    String selectedBackend = prefs.getString(selectionKey, OpenAISpeechBackend.ID);
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
                    context,
                    context.getString(R.string.elevenlabs_error_api_key_unset),
                    Toast.LENGTH_LONG)
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
      callback.onError(context.getString(R.string.elevenlabs_error_api_key_unset));
      return;
    }

    String endpoint =
        prefs.getString(
            context.getString(R.string.settings_key_elevenlabs_endpoint),
            "https://api.elevenlabs.io/v1/speech-to-text");
    endpoint = endpoint != null ? endpoint.trim() : null;
    if (endpoint == null || endpoint.isEmpty()) {
      callback.onError(context.getString(R.string.elevenlabs_error_endpoint_unset));
      return;
    }
    if (!isHttpsUrl(endpoint)) {
      callback.onError(context.getString(R.string.elevenlabs_error_endpoint_insecure));
      return;
    }
    String modelId =
        prefs.getString(context.getString(R.string.settings_key_elevenlabs_model), "scribe_v1");
    String language =
        prefs.getString(context.getString(R.string.settings_key_elevenlabs_language), "");
    boolean addTrailingSpace =
        prefs.getBoolean(
            context.getString(R.string.settings_key_elevenlabs_add_trailing_space), true);

    callback.onTranscriptionStarted();
    mTranscriber.startAsync(
        context,
        audioFile,
        mediaType,
        apiKey,
        endpoint,
        modelId,
        language,
        addTrailingSpace,
        new ElevenLabsTranscriber.Callback() {
          @Override
          public void onSuccess(@NonNull String transcription) {
            callback.onSuccess(transcription);
          }

          @Override
          public void onError(@NonNull String errorMessage) {
            callback.onError(errorMessage);
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
    String apiKey = SpeechToTextSecretsStore.getElevenLabsApiKey(context);
    if (apiKey != null && !apiKey.isEmpty()) {
      return apiKey;
    }

    String legacyKeyName = context.getString(R.string.settings_key_elevenlabs_api_key);
    String legacyApiKey = prefs.getString(legacyKeyName, "");
    if (legacyApiKey == null || legacyApiKey.isEmpty()) {
      return null;
    }

    SpeechToTextSecretsStore.setElevenLabsApiKey(context, legacyApiKey);
    prefs.edit().remove(legacyKeyName).apply();
    return legacyApiKey;
  }
}
