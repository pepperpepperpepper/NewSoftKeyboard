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
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry exposing the available third-party speech-to-text providers.
 * Implementations are discovered statically for now.
 */
public final class SpeechToTextBackendRegistry {

    private static final List<SpeechToTextBackend> BACKENDS = new ArrayList<>();

    static {
        registerBackend(new OpenAISpeechBackend());
        registerBackend(new ElevenLabsSpeechBackend());
    }

    private SpeechToTextBackendRegistry() {
        // No instances.
    }

    public static void registerBackend(@NonNull SpeechToTextBackend backend) {
        synchronized (BACKENDS) {
            if (!BACKENDS.contains(backend)) {
                BACKENDS.add(backend);
            }
        }
    }

    @NonNull
    public static List<SpeechToTextBackend> getBackends() {
        synchronized (BACKENDS) {
            return Collections.unmodifiableList(new ArrayList<>(BACKENDS));
        }
    }

    @Nullable
    public static SpeechToTextBackend getSelectedBackend(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs == null) {
            return null;
        }
        for (SpeechToTextBackend backend : getBackends()) {
            if (backend.isSelected(context, prefs)) {
                return backend;
            }
        }
        return null;
    }
}
