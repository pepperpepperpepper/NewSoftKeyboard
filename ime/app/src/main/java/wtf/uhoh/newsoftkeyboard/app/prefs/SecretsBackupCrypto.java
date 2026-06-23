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

import androidx.annotation.NonNull;
import com.lambdapioneer.argon2kt.Argon2Kt;
import com.lambdapioneer.argon2kt.Argon2Mode;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Crypto primitives for the passphrase-encrypted secrets backup: Argon2id key derivation plus
 * AES-256-GCM authenticated encryption.
 *
 * <p>The Argon2id step requires the native argon2 library and therefore only runs on-device; the
 * AES-GCM helpers are pure JCE and are unit-testable on the host JVM.
 */
public final class SecretsBackupCrypto {

  public static final String KDF_ARGON2ID = "argon2id";

  // Argon2id cost parameters. Generous because backup/restore is a rare, one-shot operation.
  public static final int ARGON2_T_COST = 3; // iterations
  public static final int ARGON2_M_COST_KIB = 65536; // 64 MiB
  public static final int ARGON2_PARALLELISM = 1;

  public static final int KEY_LEN_BYTES = 32; // AES-256
  public static final int SALT_LEN_BYTES = 16;
  public static final int IV_LEN_BYTES = 12; // GCM nonce
  private static final int GCM_TAG_BITS = 128;

  private static final SecureRandom RANDOM = new SecureRandom();

  private SecretsBackupCrypto() {}

  @NonNull
  public static byte[] randomBytes(int length) {
    final byte[] out = new byte[length];
    RANDOM.nextBytes(out);
    return out;
  }

  /**
   * Derives a 32-byte AES key from a passphrase using Argon2id. Requires the native argon2 library
   * (device only).
   */
  @NonNull
  public static byte[] deriveKey(
      @NonNull byte[] passphrase, @NonNull byte[] salt, int tCost, int mCostKib, int parallelism) {
    final Argon2Kt argon2 = new Argon2Kt();
    return argon2
        .hash(Argon2Mode.ARGON2_ID, passphrase, salt, tCost, mCostKib, parallelism, KEY_LEN_BYTES)
        .rawHashAsByteArray();
  }

  @NonNull
  public static byte[] aesGcmEncrypt(@NonNull byte[] key, @NonNull byte[] iv, @NonNull byte[] plain)
      throws GeneralSecurityException {
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
    return cipher.doFinal(plain);
  }

  /**
   * Decrypts AES-GCM ciphertext. Throws {@link javax.crypto.AEADBadTagException} when the key
   * (i.e. the passphrase) is wrong or the data was tampered with.
   */
  @NonNull
  public static byte[] aesGcmDecrypt(
      @NonNull byte[] key, @NonNull byte[] iv, @NonNull byte[] cipherText)
      throws GeneralSecurityException {
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
    return cipher.doFinal(cipherText);
  }

  /** UTF-8 encodes a passphrase without interning it as a {@link String}. */
  @NonNull
  public static byte[] charsToBytes(@NonNull char[] chars) {
    final ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
    final byte[] out = new byte[encoded.remaining()];
    encoded.get(out);
    if (encoded.hasArray()) {
      Arrays.fill(encoded.array(), (byte) 0);
    }
    return out;
  }
}
