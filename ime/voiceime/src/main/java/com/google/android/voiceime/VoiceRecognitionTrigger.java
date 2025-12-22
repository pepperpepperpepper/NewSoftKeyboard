/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.voiceime;

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.InputMethodSubtype;
import com.google.android.voiceime.backends.SpeechToTextBackend;
import com.google.android.voiceime.backends.SpeechToTextBackendRegistry;

/**
 * Triggers a voice recognition by using {@link ImeTrigger}, {@link IntentApiTrigger}, or {@link
 * ThirdPartySpeechTrigger}.
 */
public class VoiceRecognitionTrigger {

  private final InputMethodService mInputMethodService;

  private Trigger mTrigger;

  private ImeTrigger mImeTrigger;
  private IntentApiTrigger mIntentApiTrigger;
  private ThirdPartySpeechTrigger mThirdPartyTrigger;
  private SpeechToTextBackend mCurrentBackend;

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

  /** Callback interface for when recording ends and audio is sent to OpenAI */
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

  public VoiceRecognitionTrigger(InputMethodService inputMethodService) {
    mInputMethodService = inputMethodService;
    mTrigger = getTrigger();
  }

  private Trigger getTrigger() {
    SpeechToTextBackend backend =
        SpeechToTextBackendRegistry.getSelectedBackend(mInputMethodService);
    if (backend != null) {
      android.content.SharedPreferences prefs =
          android.preference.PreferenceManager.getDefaultSharedPreferences(mInputMethodService);
      if (prefs != null && backend.isConfigured(mInputMethodService, prefs)) {
        return getThirdPartyTrigger(backend);
      }
    }

    if (ImeTrigger.isInstalled(mInputMethodService)) {
      return getImeTrigger();
    } else if (IntentApiTrigger.isInstalled(mInputMethodService)) {
      return getIntentTrigger();
    } else {
      return null;
    }
  }

  private Trigger getIntentTrigger() {
    if (mIntentApiTrigger == null) {
      mIntentApiTrigger = new IntentApiTrigger(mInputMethodService);
    }
    return mIntentApiTrigger;
  }

  private Trigger getImeTrigger() {
    if (mImeTrigger == null) {
      mImeTrigger = new ImeTrigger(mInputMethodService);
    }
    return mImeTrigger;
  }

  private Trigger getThirdPartyTrigger(SpeechToTextBackend backend) {
    if (mThirdPartyTrigger == null || mCurrentBackend != backend) {
      mThirdPartyTrigger = new ThirdPartySpeechTrigger(mInputMethodService, backend);
      mCurrentBackend = backend;
      applyCallbacksToThirdPartyTrigger();
    }
    return mThirdPartyTrigger;
  }

  private void applyCallbacksToThirdPartyTrigger() {
    if (mThirdPartyTrigger == null) {
      return;
    }
    if (mRecordingStateCallback != null) {
      mThirdPartyTrigger.setRecordingStateCallback(
          isRecording -> {
            if (mRecordingStateCallback != null) {
              mRecordingStateCallback.onRecordingStateChanged(isRecording);
            }
          });
    } else {
      mThirdPartyTrigger.setRecordingStateCallback(null);
    }

    if (mTranscriptionStateCallback != null) {
      mThirdPartyTrigger.setTranscriptionStateCallback(
          isTranscribing -> {
            if (mTranscriptionStateCallback != null) {
              mTranscriptionStateCallback.onTranscriptionStateChanged(isTranscribing);
            }
          });
    } else {
      mThirdPartyTrigger.setTranscriptionStateCallback(null);
    }

    if (mTranscriptionErrorCallback != null) {
      mThirdPartyTrigger.setTranscriptionErrorCallback(
          error -> {
            if (mTranscriptionErrorCallback != null) {
              mTranscriptionErrorCallback.onTranscriptionError(error);
            }
          });
    } else {
      mThirdPartyTrigger.setTranscriptionErrorCallback(null);
    }

    if (mRecordingEndedCallback != null) {
      mThirdPartyTrigger.setRecordingEndedCallback(
          () -> {
            if (mRecordingEndedCallback != null) {
              mRecordingEndedCallback.onRecordingEnded();
            }
          });
    } else {
      mThirdPartyTrigger.setRecordingEndedCallback(null);
    }

    if (mTextWrittenCallback != null) {
      mThirdPartyTrigger.setTextWrittenCallback(
          text -> {
            if (mTextWrittenCallback != null) {
              mTextWrittenCallback.onTextWritten(text);
            }
          });
    } else {
      mThirdPartyTrigger.setTextWrittenCallback(null);
    }
  }

  public boolean isInstalled() {
    return mTrigger != null;
  }

  public boolean isEnabled() {
    return true;
  }

  // For testing
  public String getKind() {
    if (mThirdPartyTrigger != null && mCurrentBackend != null) {
      return mCurrentBackend.getId();
    } else if (mImeTrigger != null && mIntentApiTrigger != null) {
      return "both";
    } else if (mImeTrigger != null) {
      return "ime";
    } else if (mIntentApiTrigger != null) {
      return "intent";
    } else {
      return "none";
    }
  }

  /**
   * Starts a voice recognition
   *
   * @param language The language in which the recognition should be done. If the recognition is
   *     done through the Google voice typing, the parameter is ignored and the recognition is done
   *     using the locale of the calling IME.
   * @see InputMethodSubtype
   */
  public void startVoiceRecognition(String language) {
    if (mTrigger != null) {
      mTrigger.startVoiceRecognition(language);
    }
  }

  public void onStartInputView() {
    if (mTrigger != null) {
      mTrigger.onStartInputView();
    }

    // The trigger is refreshed as the system may have changed in the meanwhile.
    mTrigger = getTrigger();

    // Ensure callback is preserved after trigger refresh
    if (mRecordingStateCallback != null) {
      setRecordingStateCallback(mRecordingStateCallback);
    }
  }

  /**
   * Checks if currently recording voice input. This allows the UI to update the microphone button
   * state.
   */
  public boolean isRecording() {
    if (mTrigger instanceof ThirdPartySpeechTrigger) {
      return ((ThirdPartySpeechTrigger) mTrigger).isRecording();
    }
    return false;
  }

  /**
   * Sets the callback for recording state changes.
   *
   * @param callback The callback to be notified when recording state changes
   */
  public void setRecordingStateCallback(RecordingStateCallback callback) {
    mRecordingStateCallback = callback;
    applyCallbacksToThirdPartyTrigger();
  }

  public void setTranscriptionStateCallback(TranscriptionStateCallback callback) {
    mTranscriptionStateCallback = callback;
    applyCallbacksToThirdPartyTrigger();
  }

  public void setTranscriptionErrorCallback(TranscriptionErrorCallback callback) {
    mTranscriptionErrorCallback = callback;
    applyCallbacksToThirdPartyTrigger();
  }

  public void setRecordingEndedCallback(RecordingEndedCallback callback) {
    mRecordingEndedCallback = callback;
    applyCallbacksToThirdPartyTrigger();
  }

  public void setTextWrittenCallback(TextWrittenCallback callback) {
    mTextWrittenCallback = callback;
    applyCallbacksToThirdPartyTrigger();
  }
}
