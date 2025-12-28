package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlParser;

public class FileSystemKeyboardPackLoaderTest {
  @Test
  public void loadsBasicPackFixture() throws Exception {
    Path packDir = Files.createTempDirectory("nsk_pack_fixture_");

    copyResource("/fixtures/packs/basic_pack/manifest.json", packDir.resolve("manifest.json"));
    copyResource(
        "/fixtures/packs/basic_pack/keyboards/basic_keyboard.xml",
        packDir.resolve("keyboards/basic_keyboard.xml"));
    copyResource(
        "/fixtures/packs/basic_pack/themes/basic_theme.xml",
        packDir.resolve("themes/basic_theme.xml"));
    copyResource(
        "/fixtures/packs/basic_pack/icons/delete.png", packDir.resolve("icons/delete.png"));

    KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(packDir);
    assertEquals("basic_pack", pack.manifest().id());
    assertEquals("Basic Pack", pack.manifest().name());
    assertEquals(1, pack.manifest().version());

    assertEquals(1, pack.manifest().keyboards().size());
    assertEquals(1, pack.manifest().themes().size());

    var keyboardEntry = pack.manifest().keyboards().get(0);
    try (InputStream keyboardXml = pack.source().open(keyboardEntry.path().value())) {
      var model = AskXmlKeyboardParser.parse(keyboardXml);
      assertEquals(1, model.rows().size());
      assertEquals(5, model.rows().get(0).keys().size());
    }

    var themeEntry = pack.manifest().themes().get(0);
    try (InputStream themeXml = pack.source().open(themeEntry.path().value())) {
      var theme = ThemeXmlParser.parse(themeXml);
      assertTrue(theme.colors().containsKey("keyboardBackground"));
      assertNotNull(theme.icons().get("delete"));
    }
  }

  private static void copyResource(String resourcePath, Path destination) throws Exception {
    Files.createDirectories(destination.getParent());
    try (InputStream inputStream =
        FileSystemKeyboardPackLoaderTest.class.getResourceAsStream(resourcePath)) {
      assertNotNull("Missing test fixture resource " + resourcePath, inputStream);
      Files.write(destination, inputStream.readAllBytes());
    }
  }
}
