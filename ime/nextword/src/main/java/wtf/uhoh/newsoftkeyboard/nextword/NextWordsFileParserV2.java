package wtf.uhoh.newsoftkeyboard.nextword;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.base.Charsets;

/**
 * File structure: [1 byte VERSION (HAS TO BE 2] [ENTRIES] [1 byte Word length] [n bytes UTF8 word]
 * [1 byte count of next words] [4 bytes usedCount] [1 byte Next word length] [n bytes UTF8 word],
 * if n==0 no more next-words ... more entries
 */
public class NextWordsFileParserV2 implements NextWordsFileParser {

  @NonNull
  @Override
  public Iterable<NextWordsContainer> loadStoredNextWords(@NonNull InputStream inputStream)
      throws IOException {
    final byte[] buffer = new byte[256];
    // assuming that VERSION was read, and InputStream points to the next byte
    List<NextWordsContainer> loadedEntries = new ArrayList<>(2048);
    String word;
    while (null != (word = readWord(buffer, inputStream))) {
      final int nextWordsCount = inputStream.read();
      if (nextWordsCount <= 0) break;

      final NextWordsContainer container = new NextWordsContainer(word);
      for (int index = 0; index < nextWordsCount; index++) {
        final int usedCount;
        try {
          usedCount = readInt(inputStream);
        } catch (EOFException eof) {
          break;
        }
        final String nextWord = readWord(buffer, inputStream);
        if (nextWord == null) break;
        container.setNextWordUsedCount(nextWord, usedCount);
      }
      loadedEntries.add(container);
    }

    return loadedEntries;
  }

  private int readInt(@NonNull InputStream inputStream) throws IOException {
    final int b1 = inputStream.read();
    final int b2 = inputStream.read();
    final int b3 = inputStream.read();
    final int b4 = inputStream.read();
    if ((b1 | b2 | b3 | b4) < 0) throw new EOFException("Unexpected end-of-stream while reading.");
    return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
  }

  @Nullable
  private String readWord(@NonNull byte[] buffer, @NonNull InputStream inputStream)
      throws IOException {
    final int bytesToRead = inputStream.read();
    if (bytesToRead < 1) return null;
    final int actualReadBytes = inputStream.read(buffer, 0, bytesToRead);
    if (bytesToRead == actualReadBytes) {
      return new String(buffer, 0, bytesToRead, Charsets.UTF8);
    } else {
      return null;
    }
  }

  @Override
  public void storeNextWords(
      @NonNull Iterable<NextWordsContainer> nextWords, @NonNull OutputStream outputStream)
      throws IOException {
    // assuming output stream is pointing to the start of the file
    outputStream.write(2 /*VERSION*/);
    for (NextWordsContainer nextWordsContainer : nextWords) {
      writeWord(outputStream, nextWordsContainer.word);
      final List<NextWord> nextWordSuggestions = nextWordsContainer.getNextWordSuggestions();
      final int maxWordsToStore =
          Math.min(12 /*the maximum words we want to store*/, nextWordSuggestions.size());
      outputStream.write(maxWordsToStore);
      for (int index = 0; index < maxWordsToStore; index++) {
        final NextWord nextWord = nextWordSuggestions.get(index);
        writeInt(outputStream, nextWord.getUsedCount());
        writeWord(outputStream, nextWord.nextWord);
      }
    }
  }

  private void writeInt(@NonNull OutputStream outputStream, int value) throws IOException {
    outputStream.write((value >> 24) & 0xFF);
    outputStream.write((value >> 16) & 0xFF);
    outputStream.write((value >> 8) & 0xFF);
    outputStream.write(value & 0xFF);
  }

  private void writeWord(OutputStream outputStream, CharSequence word) throws IOException {
    byte[] buffer = word.toString().getBytes("UTF-8");
    if (buffer.length == 0) return;
    outputStream.write(buffer.length);
    outputStream.write(buffer);
  }
}
