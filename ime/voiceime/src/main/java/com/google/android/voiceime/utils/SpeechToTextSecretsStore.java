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

package com.google.android.voiceime.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

/**
 * Stores speech-to-text secrets (API keys) encrypted at rest using Android Keystore.
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>On API 21-22, Android Keystore does not support symmetric keys, so this uses an RSA key
 *       pair to encrypt small secrets directly.
 *   <li>The keystore key is not exported, so encrypted values won't be recoverable after a restore
 *       to a different device (expected for secrets).
 * </ul>
 */
public final class SpeechToTextSecretsStore {

  private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
  private static final String PREFS_NAME = "speech_to_text_secrets";
  private static final String KEYSTORE_ALIAS = "AnySoftKeyboard.SpeechToText.Secrets";
  private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

  private static final String KEY_OPENAI_API_KEY = "openai_api_key";
  private static final String KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key";

  private SpeechToTextSecretsStore() {}

  @Nullable
  public static String getOpenAIApiKey(@NonNull Context context) {
    return getSecret(context, KEY_OPENAI_API_KEY);
  }

  public static boolean hasOpenAIApiKey(@NonNull Context context) {
    return hasSecret(context, KEY_OPENAI_API_KEY);
  }

  public static void setOpenAIApiKey(@NonNull Context context, @Nullable String apiKey) {
    setSecret(context, KEY_OPENAI_API_KEY, apiKey);
  }

  @Nullable
  public static String getElevenLabsApiKey(@NonNull Context context) {
    return getSecret(context, KEY_ELEVENLABS_API_KEY);
  }

  public static boolean hasElevenLabsApiKey(@NonNull Context context) {
    return hasSecret(context, KEY_ELEVENLABS_API_KEY);
  }

  public static void setElevenLabsApiKey(@NonNull Context context, @Nullable String apiKey) {
    setSecret(context, KEY_ELEVENLABS_API_KEY, apiKey);
  }

  private static boolean hasSecret(@NonNull Context context, @NonNull String key) {
    final Context appContext =
        context.getApplicationContext() != null ? context.getApplicationContext() : context;
    final SharedPreferences prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    if (prefs == null) return false;
    final String encoded = prefs.getString(key, null);
    return encoded != null && !encoded.isEmpty();
  }

  @Nullable
  private static String getSecret(@NonNull Context context, @NonNull String key) {
    final Context appContext =
        context.getApplicationContext() != null ? context.getApplicationContext() : context;
    final SharedPreferences prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    if (prefs == null) return null;
    final String encoded = prefs.getString(key, null);
    if (encoded == null || encoded.isEmpty()) return null;

    try {
      ensureKeyPairExists(appContext);
      byte[] encrypted = Base64.decode(encoded, Base64.DEFAULT);
      byte[] plaintextBytes = decrypt(encrypted);
      return new String(plaintextBytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return null;
    }
  }

  private static void setSecret(
      @NonNull Context context, @NonNull String key, @Nullable String value) {
    final Context appContext =
        context.getApplicationContext() != null ? context.getApplicationContext() : context;
    final SharedPreferences prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    if (prefs == null) return;
    if (value == null || value.trim().isEmpty()) {
      prefs.edit().remove(key).apply();
      return;
    }

    try {
      ensureKeyPairExists(appContext);
      byte[] encrypted = encrypt(value.getBytes(StandardCharsets.UTF_8));
      String encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP);
      prefs.edit().putString(key, encoded).apply();
    } catch (Exception e) {
      // If encryption fails, do not store the secret.
      prefs.edit().remove(key).apply();
    }
  }

  private static void ensureKeyPairExists(@NonNull Context context) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
    keyStore.load(null);
    if (keyStore.containsAlias(KEYSTORE_ALIAS)) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyPairGenerator generator =
          KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);
      KeyGenParameterSpec spec =
          new KeyGenParameterSpec.Builder(
                  KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setKeySize(2048)
              .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
              .build();
      generator.initialize(spec);
      generator.generateKeyPair();
      return;
    }

    Calendar start = Calendar.getInstance();
    Calendar end = Calendar.getInstance();
    end.add(Calendar.YEAR, 30);

    KeyPairGeneratorSpec spec =
        new KeyPairGeneratorSpec.Builder(context)
            .setAlias(KEYSTORE_ALIAS)
            .setSubject(new X500Principal("CN=AnySoftKeyboard SpeechToText Secrets"))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.getTime())
            .setEndDate(end.getTime())
            .build();

    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER);
    generator.initialize(spec);
    generator.generateKeyPair();
  }

  private static byte[] encrypt(byte[] plaintext) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
    keyStore.load(null);
    PublicKey publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).getPublicKey();

    Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    return cipher.doFinal(plaintext);
  }

  private static byte[] decrypt(byte[] encrypted) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
    keyStore.load(null);
    PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEYSTORE_ALIAS, null);

    Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(encrypted);
  }
}
