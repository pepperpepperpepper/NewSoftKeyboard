package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AtomicPackFileWriterTest {

  @Rule public TemporaryFolder mFolder = new TemporaryFolder();

  private static String contentOf(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  @Test
  public void testWritesNewFileAndCreatesParentDirectories() throws Exception {
    File target = new File(mFolder.getRoot(), "pack/keyboards/main.xml");

    AtomicPackFileWriter.write(target, out -> out.write("<Keyboard/>".getBytes(StandardCharsets.UTF_8)));

    Assert.assertEquals("<Keyboard/>", contentOf(target));
  }

  @Test
  public void testReplacesExistingFileAndLeavesNoTempFiles() throws Exception {
    File target = mFolder.newFile("manifest.json");
    Files.write(target.toPath(), "old".getBytes(StandardCharsets.UTF_8));

    AtomicPackFileWriter.write(target, out -> out.write("new".getBytes(StandardCharsets.UTF_8)));

    Assert.assertEquals("new", contentOf(target));
    Assert.assertArrayEquals(
        new String[] {"manifest.json"}, mFolder.getRoot().list());
  }

  @Test
  public void testFailedWriteKeepsOriginalFileIntactAndCleansUpTemp() throws Exception {
    File target = mFolder.newFile("main.xml");
    Files.write(target.toPath(), "original".getBytes(StandardCharsets.UTF_8));

    Assert.assertThrows(
        IOException.class,
        () ->
            AtomicPackFileWriter.write(
                target,
                out -> {
                  out.write("partial".getBytes(StandardCharsets.UTF_8));
                  throw new IOException("boom");
                }));

    Assert.assertEquals("original", contentOf(target));
    Assert.assertArrayEquals(new String[] {"main.xml"}, mFolder.getRoot().list());
  }
}
