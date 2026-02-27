package com.google.android.voiceime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class VoiceRecognitionTriggerTest {

  @Test
  public void usesSelectedThirdPartyBackendEvenWhenNotConfigured() {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs =
        android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().clear().apply();
    prefs
        .edit()
        .putString(context.getString(R.string.settings_key_speech_to_text_backend), "openai")
        .remove(context.getString(R.string.settings_key_openai_api_key))
        .apply();

    InputMethodService service = mockImeService(context);
    VoiceRecognitionTrigger trigger = new VoiceRecognitionTrigger(service);

    assertEquals("openai", trigger.getKind());
  }

  @Test
  public void disableThirdPartyBackendDoesNotForceThirdPartyTrigger() {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs =
        android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().clear().apply();
    prefs
        .edit()
        .putString(context.getString(R.string.settings_key_speech_to_text_backend), "none")
        .apply();

    InputMethodService service = mockImeService(context);
    VoiceRecognitionTrigger trigger = new VoiceRecognitionTrigger(service);

    assertNotEquals("openai", trigger.getKind());
    assertNotEquals("elevenlabs", trigger.getKind());
  }

  private static InputMethodService mockImeService(Context context) {
    InputMethodService service = Mockito.mock(InputMethodService.class);
    Mockito.when(service.getApplicationContext()).thenReturn(context);
    Mockito.when(service.getCacheDir()).thenReturn(context.getCacheDir());
    Mockito.when(service.getExternalCacheDir()).thenReturn(context.getExternalCacheDir());
    Mockito.when(service.getExternalFilesDir(null)).thenReturn(context.getExternalFilesDir(null));
    Mockito.when(service.getResources()).thenReturn(context.getResources());
    Mockito.when(service.getString(Mockito.anyInt()))
        .thenAnswer(invocation -> context.getString(invocation.getArgument(0)));
    Mockito.when(service.getSystemService(Mockito.anyString()))
        .thenAnswer(invocation -> context.getSystemService((String) invocation.getArgument(0)));
    Mockito.when(service.getPackageManager()).thenReturn(context.getPackageManager());
    Mockito.when(service.getPackageName()).thenReturn(context.getPackageName());
    Mockito.when(service.getSharedPreferences(Mockito.anyString(), Mockito.anyInt()))
        .thenAnswer(
            invocation ->
                context.getSharedPreferences(invocation.getArgument(0), invocation.getArgument(1)));
    return service;
  }
}
