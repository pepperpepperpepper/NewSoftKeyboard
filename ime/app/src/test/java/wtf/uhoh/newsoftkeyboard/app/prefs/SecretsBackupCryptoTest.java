package wtf.uhoh.newsoftkeyboard.app.prefs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import org.junit.Test;

/**
 * Host-JVM coverage for the AES-GCM layer of the secrets backup. The Argon2id KDF requires the
 * native argon2 library and is therefore exercised on-device only.
 */
public class SecretsBackupCryptoTest {

  private static byte[] key32() {
    final byte[] key = new byte[SecretsBackupCrypto.KEY_LEN_BYTES];
    for (int i = 0; i < key.length; i++) key[i] = (byte) i;
    return key;
  }

  @Test
  public void aesGcmRoundTrip() throws Exception {
    final byte[] key = key32();
    final byte[] iv = SecretsBackupCrypto.randomBytes(SecretsBackupCrypto.IV_LEN_BYTES);
    final byte[] plain = "gsk-super-secret-key-123".getBytes(StandardCharsets.UTF_8);

    final byte[] cipher = SecretsBackupCrypto.aesGcmEncrypt(key, iv, plain);
    assertFalse("ciphertext must differ from plaintext", Arrays.equals(plain, cipher));

    final byte[] decrypted = SecretsBackupCrypto.aesGcmDecrypt(key, iv, cipher);
    assertArrayEquals(plain, decrypted);
  }

  @Test
  public void wrongKeyFailsAuthentication() throws Exception {
    final byte[] iv = SecretsBackupCrypto.randomBytes(SecretsBackupCrypto.IV_LEN_BYTES);
    final byte[] plain = "secret".getBytes(StandardCharsets.UTF_8);
    final byte[] cipher = SecretsBackupCrypto.aesGcmEncrypt(key32(), iv, plain);

    final byte[] wrongKey = key32();
    wrongKey[0] = (byte) (wrongKey[0] ^ 0xFF);
    try {
      SecretsBackupCrypto.aesGcmDecrypt(wrongKey, iv, cipher);
      fail("decrypt with wrong key should throw");
    } catch (AEADBadTagException expected) {
      // wrong passphrase path
    }
  }

  @Test
  public void tamperedCiphertextFails() throws Exception {
    final byte[] key = key32();
    final byte[] iv = SecretsBackupCrypto.randomBytes(SecretsBackupCrypto.IV_LEN_BYTES);
    final byte[] cipher =
        SecretsBackupCrypto.aesGcmEncrypt(key, iv, "data".getBytes(StandardCharsets.UTF_8));
    cipher[0] ^= 0x01;
    try {
      SecretsBackupCrypto.aesGcmDecrypt(key, iv, cipher);
      fail("tampered ciphertext should throw");
    } catch (AEADBadTagException expected) {
      // integrity protection working
    }
  }

  @Test
  public void charsToBytesIsUtf8() {
    final char[] chars = "pä55wörd".toCharArray();
    assertArrayEquals(
        "pä55wörd".getBytes(StandardCharsets.UTF_8), SecretsBackupCrypto.charsToBytes(chars));
  }

  @Test
  public void randomBytesLength() {
    assertEquals(16, SecretsBackupCrypto.randomBytes(16).length);
  }
}
