package com.google.android.voiceime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Looper;
import android.view.inputmethod.InputConnection;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.voiceime.backends.SpeechToTextBackend;
import com.google.android.voiceime.backends.TranscriptionResultCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ThirdPartySpeechTriggerTest {

  @Test
  public void deletesRecordingAfterCommitSuccess() throws IOException {
    Context context = ApplicationProvider.getApplicationContext();
    InputMethodService service = mockImeService(context);
    InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(service.getCurrentInputConnection()).thenReturn(inputConnection);

    File audioFile = createTempAudioFile(context);
    SpeechToTextBackend backend = succeedBackend("hello world");

    ThirdPartySpeechTrigger trigger = new ThirdPartySpeechTrigger(service, backend);
    ReflectionHelpers.setField(trigger, "mRecordedAudioFilename", audioFile.getAbsolutePath());
    ReflectionHelpers.setField(trigger, "mAudioMediaType", "audio/mp4");

    trigger.retryLastTranscription();
    shadowOf(Looper.getMainLooper()).idle();

    assertFalse("Recording should be deleted after a successful commit", audioFile.exists());
  }

  @Test
  public void keepsRecordingWhenCommitFails() throws IOException {
    Context context = ApplicationProvider.getApplicationContext();
    InputMethodService service = mockImeService(context);
    InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(false);
    Mockito.when(service.getCurrentInputConnection()).thenReturn(inputConnection);

    File audioFile = createTempAudioFile(context);
    SpeechToTextBackend backend = succeedBackend("hello world");

    ThirdPartySpeechTrigger trigger = new ThirdPartySpeechTrigger(service, backend);
    ReflectionHelpers.setField(trigger, "mRecordedAudioFilename", audioFile.getAbsolutePath());
    ReflectionHelpers.setField(trigger, "mAudioMediaType", "audio/mp4");

    trigger.retryLastTranscription();
    shadowOf(Looper.getMainLooper()).idle();

    assertTrue("Recording should be retained when commit fails", audioFile.exists());
  }

  @Test
  public void savePendingRecordingCopiesFileToExternalStorage() throws IOException {
    Context context = ApplicationProvider.getApplicationContext();
    InputMethodService service = mockImeService(context);
    File externalFilesDir = context.getExternalFilesDir(null);
    assertNotNull("Test environment should provide external files dir", externalFilesDir);

    File audioFile = createTempAudioFile(context);
    SpeechToTextBackend backend = succeedBackend("hello world");

    ThirdPartySpeechTrigger trigger = new ThirdPartySpeechTrigger(service, backend);
    ReflectionHelpers.setField(trigger, "mRecordedAudioFilename", audioFile.getAbsolutePath());
    ReflectionHelpers.setField(trigger, "mAudioMediaType", "audio/mp4");

    File recordingsDir = new File(externalFilesDir, "voice_recordings");
    int before = countFiles(recordingsDir);

    assertTrue(trigger.savePendingRecording());
    shadowOf(Looper.getMainLooper()).idle();

    int after = countFiles(recordingsDir);
    assertEquals("Saving should create exactly one new file", before + 1, after);
    assertTrue("Original recording should not be deleted when saving", audioFile.exists());
  }

  private static InputMethodService mockImeService(Context context) {
    InputMethodService service = Mockito.mock(InputMethodService.class);
    Mockito.when(service.getApplicationContext()).thenReturn(context);
    Mockito.when(service.getCacheDir()).thenReturn(context.getCacheDir());
    Mockito.when(service.getExternalCacheDir()).thenReturn(context.getExternalCacheDir());
    Mockito.when(service.getExternalFilesDir(null)).thenReturn(context.getExternalFilesDir(null));
    Mockito.when(service.getResources()).thenReturn(context.getResources());
    Mockito.when(service.getPackageName()).thenReturn(context.getPackageName());
    Mockito.when(service.getSharedPreferences(Mockito.anyString(), Mockito.anyInt()))
        .thenAnswer(
            invocation ->
                context.getSharedPreferences(invocation.getArgument(0), invocation.getArgument(1)));
    return service;
  }

  private static File createTempAudioFile(Context context) throws IOException {
    File audioFile = new File(context.getCacheDir(), "recorded_test.m4a");
    try (FileOutputStream out = new FileOutputStream(audioFile)) {
      out.write(new byte[] {1, 2, 3});
    }
    return audioFile;
  }

  private static SpeechToTextBackend succeedBackend(String result) {
    return new SpeechToTextBackend() {
      @Override
      public String getId() {
        return "fake";
      }

      @Override
      public boolean isSelected(Context context, android.content.SharedPreferences prefs) {
        return true;
      }

      @Override
      public boolean isConfigured(Context context, android.content.SharedPreferences prefs) {
        return true;
      }

      @Override
      public void showConfigurationError(Context context) {}

      @Override
      public void startTranscription(
          InputMethodService ime,
          android.content.SharedPreferences prefs,
          File audioFile,
          String mediaType,
          TranscriptionResultCallback callback) {
        callback.onTranscriptionStarted();
        callback.onSuccess(result);
      }
    };
  }

  private static int countFiles(File directory) {
    if (directory == null || !directory.exists()) {
      return 0;
    }
    File[] files = directory.listFiles();
    return files == null ? 0 : files.length;
  }
}
