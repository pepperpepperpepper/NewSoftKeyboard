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

import androidx.annotation.NonNull;

import java.io.File;

/** Backend contract for third-party speech-to-text providers. */
public interface SpeechToTextBackend {

    /** Unique identifier used for persistence and analytics. */
    @NonNull
    String getId();

    /**
     * Returns {@code true} when this backend should be considered by the trigger.
     * This typically checks a persisted provider selection.
     */
    boolean isSelected(@NonNull Context context, @NonNull SharedPreferences prefs);

    /**
     * Returns {@code true} when the backend is configured well enough to start transcribing.
     */
    boolean isConfigured(@NonNull Context context, @NonNull SharedPreferences prefs);

    /**
     * Shows a user-facing hint describing what is missing from the configuration.
     */
    void showConfigurationError(@NonNull Context context);

    /**
     * Starts an asynchronous transcription request for the given audio file.
     */
    void startTranscription(
            @NonNull InputMethodService ime,
            @NonNull SharedPreferences prefs,
            @NonNull File audioFile,
            @NonNull String mediaType,
            @NonNull TranscriptionResultCallback callback);
}
