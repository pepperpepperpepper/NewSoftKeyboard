package wtf.uhoh.newsoftkeyboard.nextword;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NextWordsStorage {

  private static final String TAG = "NextWordsStorage";
  private final Context mContext;
  private final String mNextWordsStorageFilename;

  public NextWordsStorage(@NonNull Context context, @NonNull String locale) {
    mContext = context;
    mNextWordsStorageFilename = "next_words_" + locale + ".txt";
  }

  @NonNull
  public Iterable<NextWordsContainer> loadStoredNextWords() {
    final int version;
    final List<NextWordsContainer> loadedNextWords;
    try (final FileInputStream inputStream = mContext.openFileInput(mNextWordsStorageFilename)) {
      version = inputStream.read();
      if (version < 1) {
        Log.w(TAG, "Failed to read version from file " + mNextWordsStorageFilename);
        return Collections.emptyList();
      }
      final NextWordsFileParser parser;
      switch (version) {
        case 1:
          parser = new NextWordsFileParserV1();
          break;
        case 2:
          parser = new NextWordsFileParserV2();
          break;
        default:
          Log.w(TAG, String.format("Version %d is not supported!", version));
          return Collections.emptyList();
      }
      loadedNextWords = new ArrayList<>();
      for (NextWordsContainer nextWordsContainer : parser.loadStoredNextWords(inputStream)) {
        loadedNextWords.add(nextWordsContainer);
      }
    } catch (FileNotFoundException e) {
      Log.w(TAG, e);
      Log.w(
          TAG,
          String.format(
              "Failed to find %s. Maybe it's just the first time.", mNextWordsStorageFilename));
      return Collections.emptyList();
    } catch (IOException e) {
      Log.w(TAG, e);
      Log.w(
          TAG,
          String.format(
              "Failed to open %s. Maybe it's just the first time.", mNextWordsStorageFilename));
      return Collections.emptyList();
    }

    if (version == 1 && !loadedNextWords.isEmpty()) {
      // v1 did not persist usage-counts; as a best-effort migration, we treat list order as a
      // ranking signal, synthesize conservative counts, and persist as v2 for future runs.
      final List<NextWordsContainer> migrated = migrateV1ToV2(loadedNextWords);
      storeNextWords(migrated);
      return migrated;
    } else {
      return loadedNextWords;
    }
  }

  public void storeNextWords(@NonNull Iterable<NextWordsContainer> nextWords) {
    NextWordsFileParser parser = new NextWordsFileParserV2();
    FileOutputStream outputStream = null;
    try {
      Log.d(TAG, "Storing next-words into " + mNextWordsStorageFilename);
      outputStream = mContext.openFileOutput(mNextWordsStorageFilename, Context.MODE_PRIVATE);
      parser.storeNextWords(nextWords, outputStream);
      outputStream.flush();
    } catch (IOException e) {
      Log.w(TAG, e);
      Log.w(TAG, String.format("Failed to store to %s. Deleting", mNextWordsStorageFilename));
      mContext.deleteFile(mNextWordsStorageFilename);
    } catch (NullPointerException npe) {
      // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/528
      // after reading
      // http://stackoverflow.com/questions/10259421/nullpointerexception-at-openfileoutput-in-activity
      // and
      // https://github.com/android/platform_frameworks_base/blob/android-sdk-4.0.3_r1/core/java/android/app/ContextImpl.java#L614
      // I'm guessing that there is not much I can do here :(
      Log.w(TAG, npe);
      Log.w(TAG, String.format("Failed to store to %s with an NPE.", mNextWordsStorageFilename));
    } finally {
      if (outputStream != null)
        try {
          outputStream.close();
        } catch (IOException e) {
          Log.w(TAG, "Failed to close output stream while in finally.", e);
        }
    }
  }

  @NonNull
  private static List<NextWordsContainer> migrateV1ToV2(
      @NonNull List<NextWordsContainer> loadedNextWords) {
    final List<NextWordsContainer> migrated = new ArrayList<>(loadedNextWords.size());
    for (NextWordsContainer nextWordsContainer : loadedNextWords) {
      final NextWordsContainer migratedContainer = new NextWordsContainer(nextWordsContainer.word);
      final List<NextWord> nextWords = nextWordsContainer.getNextWordSuggestions();
      for (int index = 0; index < nextWords.size(); index++) {
        // NOTE: historic v1 stores only ordering (and was written with an incorrect comparator);
        // we use a small synthetic count range to retain a ranking signal without overstating
        // confidence.
        migratedContainer.setNextWordUsedCount(nextWords.get(index).nextWord, index + 1);
      }
      migrated.add(migratedContainer);
    }
    return migrated;
  }
}
