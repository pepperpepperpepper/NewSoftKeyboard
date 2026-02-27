package wtf.uhoh.newsoftkeyboard.app.api;

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
import java.security.SecureRandom;
import java.util.Calendar;
import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

/**
 * Stores per-controller API tokens encrypted at rest using Android Keystore.
 *
 * <p>These tokens are an additional defense-in-depth factor on top of package allow-listing and
 * signature pinning.
 */
public final class KeyboardApiControllerTokensStore {

  private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
  private static final String PREFS_NAME = "keyboard_api_controller_tokens";
  private static final String KEYSTORE_ALIAS = "AnySoftKeyboard.KeyboardApi.ControllerTokens";
  private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

  private static final int TOKEN_BYTES = 32;

  private KeyboardApiControllerTokensStore() {}

  @NonNull
  public static String generateNewToken() {
    final byte[] token = new byte[TOKEN_BYTES];
    new SecureRandom().nextBytes(token);
    return Base64.encodeToString(token, Base64.NO_WRAP);
  }

  public static boolean hasToken(@NonNull Context context, @NonNull String controllerPackage) {
    final SharedPreferences prefs = getPrefs(context);
    if (prefs == null) return false;
    final String encoded = prefs.getString(controllerPackage, null);
    return encoded != null && !encoded.isEmpty();
  }

  @Nullable
  public static String getToken(@NonNull Context context, @NonNull String controllerPackage) {
    final SharedPreferences prefs = getPrefs(context);
    if (prefs == null) return null;
    final String encoded = prefs.getString(controllerPackage, null);
    if (encoded == null || encoded.isEmpty()) return null;

    try {
      ensureKeyPairExists(context);
      final byte[] encrypted = Base64.decode(encoded, Base64.DEFAULT);
      final byte[] plaintext = decrypt(encrypted);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return null;
    }
  }

  public static void setToken(
      @NonNull Context context, @NonNull String controllerPackage, @Nullable String token) {
    final SharedPreferences prefs = getPrefs(context);
    if (prefs == null) return;
    if (token == null || token.trim().isEmpty()) {
      prefs.edit().remove(controllerPackage).apply();
      return;
    }

    try {
      ensureKeyPairExists(context);
      final byte[] encrypted = encrypt(token.getBytes(StandardCharsets.UTF_8));
      final String encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP);
      prefs.edit().putString(controllerPackage, encoded).apply();
    } catch (Exception e) {
      prefs.edit().remove(controllerPackage).apply();
    }
  }

  public static void clearToken(@NonNull Context context, @NonNull String controllerPackage) {
    setToken(context, controllerPackage, null);
  }

  @Nullable
  private static SharedPreferences getPrefs(@NonNull Context context) {
    final Context appContext =
        context.getApplicationContext() != null ? context.getApplicationContext() : context;
    return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  private static void ensureKeyPairExists(@NonNull Context context) throws Exception {
    final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
    keyStore.load(null);
    if (keyStore.containsAlias(KEYSTORE_ALIAS)) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      final KeyPairGenerator generator =
          KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);
      final KeyGenParameterSpec spec =
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

    final Calendar start = Calendar.getInstance();
    final Calendar end = Calendar.getInstance();
    end.add(Calendar.YEAR, 30);

    final KeyPairGeneratorSpec spec =
        new KeyPairGeneratorSpec.Builder(context)
            .setAlias(KEYSTORE_ALIAS)
            .setSubject(new X500Principal("CN=AnySoftKeyboard Keyboard API Tokens"))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.getTime())
            .setEndDate(end.getTime())
            .build();

    final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER);
    generator.initialize(spec);
    generator.generateKeyPair();
  }

  private static byte[] encrypt(@NonNull byte[] plaintext) throws Exception {
    final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
    keyStore.load(null);
    final PublicKey publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).getPublicKey();

    final Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    return cipher.doFinal(plaintext);
  }

  private static byte[] decrypt(@NonNull byte[] encrypted) throws Exception {
    final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
    keyStore.load(null);
    final PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEYSTORE_ALIAS, null);

    final Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(encrypted);
  }
}
