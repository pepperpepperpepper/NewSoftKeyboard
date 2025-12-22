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
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.voiceime.backends.SpeechToTextBackend;
import com.google.android.voiceime.backends.SpeechToTextBackendRegistry;
import com.google.android.voiceime.backends.TranscriptionResultCallback;
import java.io.File;

/** Trigger that delegates speech recognition to a configurable third-party backend. */
public class ThirdPartySpeechTrigger implements Trigger {

  private static final String TAG = "ThirdPartyTrigger";

  private final InputMethodService mInputMethodService;
  private final SpeechToTextBackend mBackend;
  private final AudioRecorderManager mAudioRecorderManager;
  private final SharedPreferences mSharedPreferences;
  private final Handler mMainHandler = new Handler(Looper.getMainLooper());

  private String mLastRecognitionResult;
  private String mRecordedAudioFilename;
  private String mAudioMediaType;

  private boolean mIsRecording = false;

  /** Callback interface for recording state changes */
  public interface RecordingStateCallback {
    void onRecordingStateChanged(boolean isRecording);
  }

  /** Callback interface for transcription state changes */
  public interface TranscriptionStateCallback {
    void onTranscriptionStateChanged(boolean isTranscribing);
  }

  /** Callback interface for transcription errors */
  public interface TranscriptionErrorCallback {
    void onTranscriptionError(String error);
  }

  /** Callback interface for when recording ends and audio is sent to the backend */
  public interface RecordingEndedCallback {
    void onRecordingEnded();
  }

  /** Callback interface for when transcribed text has been written to input field */
  public interface TextWrittenCallback {
    void onTextWritten(String text);
  }

  private RecordingStateCallback mRecordingStateCallback;
  private TranscriptionStateCallback mTranscriptionStateCallback;
  private TranscriptionErrorCallback mTranscriptionErrorCallback;
  private RecordingEndedCallback mRecordingEndedCallback;
  private TextWrittenCallback mTextWrittenCallback;

  public ThirdPartySpeechTrigger(
      @NonNull InputMethodService inputMethodService, @NonNull SpeechToTextBackend backend) {
    mInputMethodService = inputMethodService;
    mBackend = backend;
    mAudioRecorderManager = new AudioRecorderManager(inputMethodService);
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputMethodService);

    setupAudioRecorderCallbacks();
  }

  public static boolean isAvailable(@NonNull Context context) {
    SpeechToTextBackend backend = SpeechToTextBackendRegistry.getSelectedBackend(context);
    if (backend == null) {
      return false;
    }
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return backend.isConfigured(context, prefs);
  }

  public void setRecordingStateCallback(RecordingStateCallback callback) {
    mRecordingStateCallback = callback;
  }

  public void setTranscriptionStateCallback(TranscriptionStateCallback callback) {
    mTranscriptionStateCallback = callback;
  }

  public void setTranscriptionErrorCallback(TranscriptionErrorCallback callback) {
    mTranscriptionErrorCallback = callback;
  }

  public void setRecordingEndedCallback(RecordingEndedCallback callback) {
    mRecordingEndedCallback = callback;
  }

  public void setTextWrittenCallback(TextWrittenCallback callback) {
    mTextWrittenCallback = callback;
  }

  private void notifyRecordingStateChanged(boolean isRecording) {
    if (mRecordingStateCallback != null) {
      mRecordingStateCallback.onRecordingStateChanged(isRecording);
    }
  }

  private void notifyTranscriptionStateChanged(boolean isTranscribing) {
    if (mTranscriptionStateCallback != null) {
      mTranscriptionStateCallback.onTranscriptionStateChanged(isTranscribing);
    }
  }

  private void notifyTranscriptionError(String error) {
    if (mTranscriptionErrorCallback != null) {
      mTranscriptionErrorCallback.onTranscriptionError(error);
    }
  }

  private void notifyRecordingEnded() {
    if (mRecordingEndedCallback != null) {
      mRecordingEndedCallback.onRecordingEnded();
    }
  }

  private void notifyTextWritten(String text) {
    if (mTextWrittenCallback != null) {
      mTextWrittenCallback.onTextWritten(text);
    }
  }

  private void setupAudioRecorderCallbacks() {
    mAudioRecorderManager.setOnRecordingStopped(
        (success, errorMessage) -> {
          mIsRecording = false;
          notifyRecordingStateChanged(false);
          if (success) {
            notifyRecordingEnded();
            startTranscription();
          } else if (errorMessage != null) {
            Log.e(TAG, "Recording failed: " + errorMessage);
            showError(errorMessage);
          }
        });

    mAudioRecorderManager.setOnUpdateMicrophoneAmplitude(
        amplitude -> Log.d(TAG, "Microphone amplitude: " + amplitude));
  }

  @Override
  public void startVoiceRecognition(String language) {
    Log.d(TAG, "Voice recognition triggered for language: " + language);

    if (!mBackend.isConfigured(mInputMethodService, mSharedPreferences)) {
      mBackend.showConfigurationError(mInputMethodService);
      return;
    }

    if (mIsRecording) {
      stopRecording();
    } else {
      setupAudioFormat();
      startRecording();
    }
  }

  private void setupAudioFormat() {
    File cacheDir = mInputMethodService.getExternalCacheDir();
    if (cacheDir == null) {
      cacheDir = mInputMethodService.getCacheDir();
    }
    File target = new File(cacheDir, "recorded.m4a");
    mRecordedAudioFilename = target.getAbsolutePath();
    mAudioMediaType = "audio/mp4";
  }

  private void startRecording() {
    if (!mAudioRecorderManager.hasPermissions()) {
      showError(mInputMethodService.getString(R.string.openai_error_microphone_permission));
      return;
    }

    try {
      mAudioRecorderManager.startRecording(mRecordedAudioFilename, false);
      mAudioRecorderManager.setupAutoStop();
      mIsRecording = true;
      notifyRecordingStateChanged(true);
      Log.d(TAG, "Started recording to: " + mRecordedAudioFilename);
    } catch (Exception e) {
      Log.e(TAG, "Error starting recording", e);
      showError(mInputMethodService.getString(R.string.openai_error_recording_failed));
    }
  }

  private void stopRecording() {
    if (mAudioRecorderManager.isRecording()) {
      mAudioRecorderManager.stopRecording();
    }
    mIsRecording = false;
    notifyRecordingStateChanged(false);
  }

  private void startTranscription() {
    File audioFile = new File(mRecordedAudioFilename);
    if (!audioFile.exists()) {
      showError("Audio file not found: " + audioFile.getAbsolutePath());
      return;
    }
    if (audioFile.length() == 0) {
      showError("Audio file is empty");
      return;
    }

    mBackend.startTranscription(
        mInputMethodService,
        mSharedPreferences,
        audioFile,
        mAudioMediaType,
        new TranscriptionResultCallback() {
          @Override
          public void onTranscriptionStarted() {
            notifyTranscriptionStateChanged(true);
          }

          @Override
          public void onSuccess(@NonNull String text) {
            notifyTranscriptionStateChanged(false);
            onTranscriptionResult(text);
          }

          @Override
          public void onError(@NonNull String errorMessage) {
            notifyTranscriptionStateChanged(false);
            notifyTranscriptionError(errorMessage);
            onTranscriptionError(errorMessage);
          }
        });
  }

  private void onTranscriptionResult(String result) {
    mLastRecognitionResult = result;
    commitResult();
    notifyTextWritten(result);
    cleanupAudioFile();
  }

  private void onTranscriptionError(String error) {
    showError(error);
    cleanupAudioFile();
  }

  private void commitResult() {
    if (mLastRecognitionResult == null) {
      return;
    }

    try {
      android.view.inputmethod.InputConnection conn =
          mInputMethodService.getCurrentInputConnection();
      if (conn == null) {
        Log.w(TAG, "No input connection available");
        return;
      }

      if (!conn.beginBatchEdit()) {
        Log.w(TAG, "Could not begin batch edit");
        return;
      }

      try {
        conn.commitText(mLastRecognitionResult, 1);
        mLastRecognitionResult = null;
      } finally {
        conn.endBatchEdit();
      }
    } catch (Exception e) {
      Log.e(TAG, "Error committing transcription result", e);
    }
  }

  private void cleanupAudioFile() {
    if (mRecordedAudioFilename == null) {
      return;
    }
    try {
      File file = new File(mRecordedAudioFilename);
      if (file.exists() && !file.delete()) {
        Log.w(TAG, "Failed to delete audio file: " + mRecordedAudioFilename);
      } else {
        Log.d(TAG, "Deleted audio file: " + mRecordedAudioFilename);
      }
    } catch (Exception e) {
      Log.e(TAG, "Error cleaning up audio file", e);
    }
  }

  private void showError(String message) {
    Log.e(TAG, "Error: " + message);
    mMainHandler.post(
        () ->
            android.widget.Toast.makeText(
                    mInputMethodService, message, android.widget.Toast.LENGTH_LONG)
                .show());
  }

  public boolean isRecording() {
    return mIsRecording;
  }

  @Override
  public void onStartInputView() {
    mLastRecognitionResult = null;
    mIsRecording = false;
    notifyRecordingStateChanged(false);
    mAudioRecorderManager.stopRecording();
    cleanupAudioFile();
  }
}
