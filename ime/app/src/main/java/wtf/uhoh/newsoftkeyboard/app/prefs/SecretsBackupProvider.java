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

package wtf.uhoh.newsoftkeyboard.app.prefs;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.voiceime.utils.SpeechToTextSecretsStore;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.prefs.backup.PrefItem;
import wtf.uhoh.newsoftkeyboard.prefs.backup.PrefsProvider;
import wtf.uhoh.newsoftkeyboard.prefs.backup.PrefsRoot;

/**
 * Backs up the speech-to-text API keys, encrypted with a user passphrase (Argon2id + AES-256-GCM)
 * so the backup file never contains a plaintext key. The keys themselves live in the Keystore-bound
 * {@link SpeechToTextSecretsStore} and are non-portable by design; this provider is the only way to
 * carry them across an uninstall/reinstall.
 *
 * <p>The passphrase is supplied via {@link #setPassphrase(char[])} before the backup/restore flow
 * runs; without it the provider is a no-op (so an accidentally-checked secrets box can't silently
 * write or wipe keys).
 */
public final class SecretsBackupProvider implements PrefsProvider {

  static final String PROVIDER_ID = "SecretsBackupProvider";
  private static final int VERSION = 1;
  private static final int BASE64_FLAGS = Base64.NO_WRAP;

  // Logical secret ids persisted in the backup, decoupled from the storage's internal key names.
  private static final String SECRET_OPENAI = "openai";
  private static final String SECRET_ELEVENLABS = "elevenlabs";
  private static final String SECRET_GROQ = "groq";

  private final Context mContext;
  @Nullable private char[] mPassphrase;

  public SecretsBackupProvider(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  public void setPassphrase(@Nullable char[] passphrase) {
    mPassphrase = passphrase;
  }

  /** True when at least one STT API key is stored and could be backed up. */
  public static boolean hasAnySecret(@NonNull Context context) {
    return SpeechToTextSecretsStore.hasOpenAIApiKey(context)
        || SpeechToTextSecretsStore.hasElevenLabsApiKey(context)
        || SpeechToTextSecretsStore.hasGroqApiKey(context);
  }

  @Override
  public String providerId() {
    return PROVIDER_ID;
  }

  @Override
  public PrefsRoot getPrefsRoot() {
    final PrefsRoot root = new PrefsRoot(VERSION);
    if (mPassphrase == null) {
      return root; // no passphrase -> nothing to export
    }

    final Map<String, String> secrets = collectSecrets();
    if (secrets.isEmpty()) {
      return root;
    }

    final byte[] salt = SecretsBackupCrypto.randomBytes(SecretsBackupCrypto.SALT_LEN_BYTES);
    byte[] passphraseBytes = SecretsBackupCrypto.charsToBytes(mPassphrase);
    byte[] key = null;
    try {
      key =
          SecretsBackupCrypto.deriveKey(
              passphraseBytes,
              salt,
              SecretsBackupCrypto.ARGON2_T_COST,
              SecretsBackupCrypto.ARGON2_M_COST_KIB,
              SecretsBackupCrypto.ARGON2_PARALLELISM);

      root.addValue("kdf", SecretsBackupCrypto.KDF_ARGON2ID);
      root.addValue("kv", Integer.toString(VERSION));
      root.addValue("salt", Base64.encodeToString(salt, BASE64_FLAGS));
      root.addValue("t", Integer.toString(SecretsBackupCrypto.ARGON2_T_COST));
      root.addValue("m", Integer.toString(SecretsBackupCrypto.ARGON2_M_COST_KIB));
      root.addValue("p", Integer.toString(SecretsBackupCrypto.ARGON2_PARALLELISM));

      for (Map.Entry<String, String> secret : secrets.entrySet()) {
        final byte[] iv = SecretsBackupCrypto.randomBytes(SecretsBackupCrypto.IV_LEN_BYTES);
        final byte[] plain = secret.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final byte[] cipher = SecretsBackupCrypto.aesGcmEncrypt(key, iv, plain);
        Arrays.fill(plain, (byte) 0);
        root.createChild()
            .addValue("provider", secret.getKey())
            .addValue("iv", Base64.encodeToString(iv, BASE64_FLAGS))
            .addValue("cipher", Base64.encodeToString(cipher, BASE64_FLAGS));
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to encrypt API keys for backup.", e);
    } finally {
      if (key != null) Arrays.fill(key, (byte) 0);
      Arrays.fill(passphraseBytes, (byte) 0);
    }
    return root;
  }

  @Override
  public void storePrefsRoot(PrefsRoot prefsRoot) {
    if (mPassphrase == null) {
      return;
    }
    final String salt64 = prefsRoot.getValue("salt");
    if (TextUtils.isEmpty(salt64)) {
      return; // backup contained no secrets section
    }

    final int tCost = parseIntOr(prefsRoot.getValue("t"), SecretsBackupCrypto.ARGON2_T_COST);
    final int mCost = parseIntOr(prefsRoot.getValue("m"), SecretsBackupCrypto.ARGON2_M_COST_KIB);
    final int parallelism =
        parseIntOr(prefsRoot.getValue("p"), SecretsBackupCrypto.ARGON2_PARALLELISM);

    byte[] passphraseBytes = SecretsBackupCrypto.charsToBytes(mPassphrase);
    byte[] key = null;
    try {
      key =
          SecretsBackupCrypto.deriveKey(
              passphraseBytes, Base64.decode(salt64, BASE64_FLAGS), tCost, mCost, parallelism);

      for (PrefItem child : prefsRoot.getChildren()) {
        final String provider = child.getValue("provider");
        final String iv64 = child.getValue("iv");
        final String cipher64 = child.getValue("cipher");
        if (TextUtils.isEmpty(provider) || TextUtils.isEmpty(iv64) || TextUtils.isEmpty(cipher64)) {
          continue;
        }
        final byte[] plain =
            SecretsBackupCrypto.aesGcmDecrypt(
                key, Base64.decode(iv64, BASE64_FLAGS), Base64.decode(cipher64, BASE64_FLAGS));
        final String apiKey = new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        Arrays.fill(plain, (byte) 0);
        applySecret(provider, apiKey);
      }
    } catch (javax.crypto.AEADBadTagException e) {
      throw new RuntimeException("Wrong passphrase, or the backup is corrupted.", e);
    } catch (Exception e) {
      throw new RuntimeException("Failed to restore API keys.", e);
    } finally {
      if (key != null) Arrays.fill(key, (byte) 0);
      Arrays.fill(passphraseBytes, (byte) 0);
    }
  }

  private Map<String, String> collectSecrets() {
    final Map<String, String> out = new LinkedHashMap<>();
    putIfPresent(out, SECRET_OPENAI, SpeechToTextSecretsStore.getOpenAIApiKey(mContext));
    putIfPresent(out, SECRET_ELEVENLABS, SpeechToTextSecretsStore.getElevenLabsApiKey(mContext));
    putIfPresent(out, SECRET_GROQ, SpeechToTextSecretsStore.getGroqApiKey(mContext));
    return out;
  }

  private void applySecret(@NonNull String provider, @NonNull String apiKey) {
    switch (provider) {
      case SECRET_OPENAI -> SpeechToTextSecretsStore.setOpenAIApiKey(mContext, apiKey);
      case SECRET_ELEVENLABS -> SpeechToTextSecretsStore.setElevenLabsApiKey(mContext, apiKey);
      case SECRET_GROQ -> SpeechToTextSecretsStore.setGroqApiKey(mContext, apiKey);
      default -> {
        /* unknown provider id; ignore for forward-compat */
      }
    }
  }

  private static void putIfPresent(
      @NonNull Map<String, String> map, @NonNull String id, @Nullable String value) {
    if (!TextUtils.isEmpty(value)) {
      map.put(id, value);
    }
  }

  private static int parseIntOr(@Nullable String value, int fallback) {
    if (TextUtils.isEmpty(value)) return fallback;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
