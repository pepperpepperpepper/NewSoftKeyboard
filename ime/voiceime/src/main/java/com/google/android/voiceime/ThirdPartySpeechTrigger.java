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
import com.google.android.voiceime.utils.SpeechToTextFileUtils;
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
  private volatile boolean mHasPendingRecording;

  private volatile boolean mIsRecording = false;
  private volatile boolean mIsTranscribing = false;

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
            reportError(errorMessage);
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
      if (mIsTranscribing) {
        Log.w(TAG, "Transcription in progress. Ignoring new voice trigger.");
        return;
      }
      mLastRecognitionResult = null;
      mHasPendingRecording = false;
      cleanupAudioFile();
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
      reportError(mInputMethodService.getString(R.string.openai_error_microphone_permission));
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
      reportError(mInputMethodService.getString(R.string.openai_error_recording_failed));
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
    if (mIsTranscribing) {
      Log.w(TAG, "Already transcribing, ignoring startTranscription request.");
      return;
    }
    File audioFile = new File(mRecordedAudioFilename);
    if (!audioFile.exists()) {
      Log.e(TAG, "Audio file not found: " + audioFile.getAbsolutePath());
      reportError(mInputMethodService.getString(R.string.openai_error_recording_failed));
      mHasPendingRecording = false;
      return;
    }
    if (audioFile.length() == 0) {
      Log.e(TAG, "Audio file is empty");
      reportError(mInputMethodService.getString(R.string.openai_error_recording_failed));
      mHasPendingRecording = false;
      cleanupAudioFile();
      return;
    }

    mHasPendingRecording = true;
    mIsTranscribing = true;
    try {
      mBackend.startTranscription(
          mInputMethodService,
          mSharedPreferences,
          audioFile,
          mAudioMediaType,
          new TranscriptionResultCallback() {
            @Override
            public void onTranscriptionStarted() {
              runOnMainThread(() -> notifyTranscriptionStateChanged(true));
            }

            @Override
            public void onSuccess(@NonNull String text) {
              runOnMainThread(
                  () -> {
                    mIsTranscribing = false;
                    notifyTranscriptionStateChanged(false);
                    onTranscriptionResult(text);
                  });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
              runOnMainThread(
                  () -> {
                    mIsTranscribing = false;
                    notifyTranscriptionStateChanged(false);
                    notifyTranscriptionError(errorMessage);
                  });
            }
          });
    } catch (RuntimeException runtimeException) {
      final String message =
          runtimeException.getMessage() != null
              ? runtimeException.getMessage()
              : "Failed to start transcription.";
      runOnMainThread(
          () -> {
            mIsTranscribing = false;
            notifyTranscriptionStateChanged(false);
            notifyTranscriptionError(message);
          });
    }
  }

  private void onTranscriptionResult(String result) {
    mLastRecognitionResult = result;
    if (commitResult()) {
      notifyTextWritten(result);
      mHasPendingRecording = false;
      cleanupAudioFile();
      return;
    }

    notifyTranscriptionError(
        mInputMethodService.getString(R.string.speech_to_text_error_insert_failed));
  }

  private boolean commitResult() {
    if (mLastRecognitionResult == null) {
      return false;
    }

    try {
      android.view.inputmethod.InputConnection conn =
          mInputMethodService.getCurrentInputConnection();
      if (conn == null) {
        Log.w(TAG, "No input connection available");
        return false;
      }

      if (!conn.beginBatchEdit()) {
        Log.w(TAG, "Could not begin batch edit");
        return false;
      }

      try {
        if (conn.commitText(mLastRecognitionResult, 1)) {
          mLastRecognitionResult = null;
          return true;
        }
      } finally {
        conn.endBatchEdit();
      }
    } catch (Exception e) {
      Log.e(TAG, "Error committing transcription result", e);
    }
    return false;
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

  private void reportError(@NonNull String message) {
    Log.e(TAG, "Error: " + message);
    runOnMainThread(() -> notifyTranscriptionError(message));
  }

  public boolean isRecording() {
    return mIsRecording;
  }

  public boolean retryLastTranscription() {
    if (mIsRecording || mIsTranscribing || mRecordedAudioFilename == null) {
      return false;
    }
    if (mLastRecognitionResult != null) {
      if (commitResult()) {
        mHasPendingRecording = false;
        cleanupAudioFile();
        return true;
      }
      return false;
    }
    startTranscription();
    return mIsTranscribing;
  }

  public boolean savePendingRecording() {
    if (mRecordedAudioFilename == null) {
      return false;
    }
    File source = new File(mRecordedAudioFilename);
    if (!source.exists() || source.length() == 0) {
      return false;
    }

    File externalFilesDir = mInputMethodService.getExternalFilesDir(null);
    if (externalFilesDir == null) {
      reportError(
          mInputMethodService.getString(R.string.speech_to_text_error_save_recording_failed));
      return false;
    }

    File recordingsDir = new File(externalFilesDir, "voice_recordings");
    if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
      reportError(
          mInputMethodService.getString(R.string.speech_to_text_error_save_recording_failed));
      return false;
    }

    String extension = "m4a";
    int dotIndex = source.getName().lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < source.getName().length() - 1) {
      extension = source.getName().substring(dotIndex + 1);
    }

    File copied =
        SpeechToTextFileUtils.copyToDirectory(
            source, recordingsDir.getAbsolutePath(), "voice_recording", extension);
    if (copied == null) {
      reportError(
          mInputMethodService.getString(R.string.speech_to_text_error_save_recording_failed));
      return false;
    }

    return true;
  }

  public void discardPendingTranscription() {
    mLastRecognitionResult = null;
    mHasPendingRecording = false;
    cleanupAudioFile();
  }

  @Override
  public void onStartInputView() {
    mIsRecording = false;
    mIsTranscribing = false;
    notifyRecordingStateChanged(false);
    mAudioRecorderManager.stopRecording();
    if (!mHasPendingRecording) {
      mLastRecognitionResult = null;
      cleanupAudioFile();
    }
  }

  private void runOnMainThread(@NonNull Runnable action) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action.run();
    } else {
      mMainHandler.post(action);
    }
  }
}
