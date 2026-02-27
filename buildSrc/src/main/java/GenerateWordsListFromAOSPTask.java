import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.TaskAction;

/**
 * Task to generate words-list XML file from a AOSP words-list file.
 * https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/master/dictionaries/
 */
@CacheableTask
public class GenerateWordsListFromAOSPTask extends DefaultTask {
  private static final Pattern mWordLineRegex =
      Pattern.compile("^\\s*word=([\\w\\p{L}'\"-]+),f=(\\d+)(?:,flags=([^,]*))?.*$");
  private static final int ABBREVIATION_ACRONYM_DOWNWEIGHT_FREQUENCY = 1;

  private File inputFile;
  private File outputWordsListFile;
  private int maxWordsInList = 500000;

  @TaskAction
  public void generateWordsList() throws IOException {
    if (inputFile == null) {
      throw new IllegalArgumentException("Please provide inputFile value.");
    }
    if (!inputFile.isFile()) throw new IllegalArgumentException("inputFile must be a file!");
    if (outputWordsListFile == null) {
      throw new IllegalArgumentException("Please provide outputWordsListFile value.");
    }

    final long inputSize = inputFile.length();
    System.out.println(
        "Reading input file " + inputFile.getName() + " (size " + inputSize + ")...");

    InputStream fileInput = new FileInputStream(inputFile);
    if (inputFile.getName().endsWith(".zip")) {
      fileInput = new ZipInputStream(fileInput);
    } else if (inputFile.getName().endsWith(".gz")) {
      fileInput = new GZIPInputStream(fileInput);
    }
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(fileInput, StandardCharsets.UTF_8));
    String wordDataLine;

    try (WordListWriter wordListWriter = new WordListWriter(outputWordsListFile)) {
      long read = 0;
      long wordsWritten = 0;
      while (null != (wordDataLine = reader.readLine())) {
        read += wordDataLine.length();
        // word=heh,f=0,flags=,originalFreq=53,possibly_offensive=true
        Matcher matcher = mWordLineRegex.matcher(wordDataLine);
        if (matcher.matches()) {
          final String word = matcher.group(1);
          int frequency = Integer.parseInt(matcher.group(2));
          final String flagsRaw = matcher.groupCount() >= 3 ? matcher.group(3) : null;
          if (flagsRaw != null && !flagsRaw.isEmpty()) {
            final String flags = flagsRaw.toLowerCase(Locale.ROOT);
            if (flags.contains("abbreviation") && looksLikeAllCapsAcronym(word)) {
              frequency = Math.min(frequency, ABBREVIATION_ACRONYM_DOWNWEIGHT_FREQUENCY);
            }
          }
          wordListWriter.addEntry(word, frequency);
          if ((wordsWritten % 50000) == 0) {
            System.out.print("." + ((100 * read) / inputSize) + "%.");
          }
          wordsWritten++;
          if (maxWordsInList == wordsWritten) {
            System.out.println("!!!!");
            System.out.println("Reached " + maxWordsInList + " words! Breaking parsing.");
            break;
          }
        }
      }
      System.out.print(".100%.");
    }

    System.out.println("Done.");
  }

  @InputFile
  @PathSensitive(RELATIVE)
  public File getInputFile() {
    return inputFile;
  }

  public void setInputFile(File inputFile) {
    this.inputFile = inputFile;
  }

  @OutputFile
  public File getOutputWordsListFile() {
    return outputWordsListFile;
  }

  public void setOutputWordsListFile(File outputWordsListFile) {
    this.outputWordsListFile = outputWordsListFile;
  }

  @Input
  public int getMaxWordsInList() {
    return maxWordsInList;
  }

  public void setMaxWordsInList(int maxWordsInList) {
    this.maxWordsInList = maxWordsInList;
  }

  private static boolean looksLikeAllCapsAcronym(String token) {
    String core = token;
    if (core.endsWith("'s") && core.length() > 2) {
      core = core.substring(0, core.length() - 2);
    }
    if (core.length() < 3) return false;

    boolean hasLetter = false;
    for (int i = 0; i < core.length(); i++) {
      final char c = core.charAt(i);
      if (!Character.isLetter(c)) return false;
      hasLetter = true;
      if (!Character.isUpperCase(c)) return false;
    }
    return hasLetter;
  }
}
