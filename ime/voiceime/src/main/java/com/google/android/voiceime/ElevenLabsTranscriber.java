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

package com.google.android.voiceime;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Handles transcription requests to the ElevenLabs speech-to-text API. */
public final class ElevenLabsTranscriber {

    private static final String TAG = "ElevenLabsTranscriber";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public interface Callback {
        void onSuccess(@NonNull String transcription);

        void onError(@NonNull String errorMessage);
    }

    public void startAsync(
            @NonNull Context context,
            @NonNull File audioFile,
            @NonNull String mediaType,
            @NonNull String apiKey,
            @NonNull String endpoint,
            @NonNull String modelId,
            @NonNull String language,
            boolean addTrailingSpace,
            @NonNull Callback callback) {

        new Thread(
                        () -> {
                            try {
                                String result =
                                        performRequest(
                                                audioFile,
                                                mediaType,
                                                apiKey,
                                                endpoint,
                                                modelId,
                                                language);
                                if (addTrailingSpace) {
                                    result = result + " ";
                                }
                                postSuccess(callback, result);
                            } catch (IOException e) {
                                Log.e(TAG, "Network failure", e);
                                postError(callback, "Network error: " + e.getMessage());
                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected failure", e);
                                postError(callback, "Transcription failed: " + e.getMessage());
                            }
                        })
                .start();
    }

    private String performRequest(
            File audioFile,
            String mediaType,
            String apiKey,
            String endpoint,
            String modelId,
            String language)
            throws IOException {

        RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse(mediaType));
        MultipartBody.Builder multipartBuilder =
                new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", audioFile.getName(), fileBody)
                        .addFormDataPart("model_id", modelId);

        if (!language.isEmpty()) {
            multipartBuilder.addFormDataPart("language", language);
        }

        Request request =
                new Request.Builder()
                        .url(endpoint)
                        .addHeader("xi-api-key", apiKey)
                        .post(multipartBuilder.build())
                        .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "empty";
                throw new IOException(
                        "HTTP "
                                + response.code()
                                + " "
                                + response.message()
                                + " - "
                                + body);
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    private void postSuccess(Callback callback, String text) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> callback.onSuccess(text));
    }

    private void postError(Callback callback, String message) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> callback.onError(message));
    }
}
