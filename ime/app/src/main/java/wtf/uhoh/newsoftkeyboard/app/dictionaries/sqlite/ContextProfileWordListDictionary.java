package wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite;

import android.content.Context;
import androidx.annotation.NonNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Per-context-profile word list dictionary (stored in an internal SQLite DB per preset id). */
public class ContextProfileWordListDictionary extends SQLiteUserDictionaryBase {

  private static final String DB_PREFIX = "context_profile_word_list_";

  @NonNull private final String mPresetId;
  @NonNull private final String mDbFilename;

  public ContextProfileWordListDictionary(
      @NonNull Context context, @NonNull String presetId, @NonNull String locale) {
    super("ContextProfileWordListDictionary", context, locale);
    mPresetId = presetId;
    mDbFilename = dbFileNameForPresetId(presetId);
  }

  @NonNull
  public String getPresetId() {
    return mPresetId;
  }

  @Override
  protected WordsSQLiteConnection createStorage(String locale) {
    return new WordsSQLiteConnection(mContext, mDbFilename, locale);
  }

  public static void deleteStorageForPreset(@NonNull Context context, @NonNull String presetId) {
    context.deleteDatabase(dbFileNameForPresetId(presetId));
  }

  @NonNull
  private static String dbFileNameForPresetId(@NonNull String presetId) {
    return DB_PREFIX + sha256Hex(presetId) + ".db";
  }

  @NonNull
  private static String sha256Hex(@NonNull String input) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError("SHA-256 not available", e);
    }
    final byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
    final char[] hex = new char[bytes.length * 2];
    final char[] alphabet = "0123456789abcdef".toCharArray();
    for (int i = 0; i < bytes.length; i++) {
      final int b = bytes[i] & 0xFF;
      hex[i * 2] = alphabet[b >>> 4];
      hex[i * 2 + 1] = alphabet[b & 0x0F];
    }
    return new String(hex);
  }
}
