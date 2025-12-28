package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class KeyboardPackValidatorTest {
  @Test
  public void validatesBasicPackFixture() throws Exception {
    Path packDir = stageBasicPackFixture();

    KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(packDir);
    var result = KeyboardPackValidator.validate(pack);
    assertTrue("Expected pack to be valid: " + result.errors(), result.isValid());
  }

  @Test
  public void reportsMissingIcon() throws Exception {
    Path packDir = stageBasicPackFixture();
    Files.delete(packDir.resolve("icons/delete.png"));

    KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(packDir);
    var result = KeyboardPackValidator.validate(pack);
    assertFalse("Expected pack to be invalid", result.isValid());
    assertTrue(
        "Expected missing icon error, got: " + result.errors(),
        result.errors().stream().anyMatch(e -> e.contains("missing icon")));
  }

  private static Path stageBasicPackFixture() throws Exception {
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

    return packDir;
  }

  private static void copyResource(String resourcePath, Path destination) throws Exception {
    Files.createDirectories(destination.getParent());
    try (InputStream inputStream =
        KeyboardPackValidatorTest.class.getResourceAsStream(resourcePath)) {
      assertNotNull("Missing test fixture resource " + resourcePath, inputStream);
      Files.write(destination, inputStream.readAllBytes());
    }
  }
}
