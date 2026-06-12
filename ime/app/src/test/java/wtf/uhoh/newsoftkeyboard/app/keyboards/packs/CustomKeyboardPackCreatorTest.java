package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class CustomKeyboardPackCreatorTest {

  private static Context context() {
    return ApplicationProvider.getApplicationContext();
  }

  private static KeyboardModel parse(File xmlFile) throws Exception {
    try (InputStream in = new FileInputStream(xmlFile)) {
      return AskXmlKeyboardParser.parse(in);
    }
  }

  private static float rowWidthPercent(KeyboardModel model, KeyboardRow row) {
    float rowDefault =
        parsePercent(row.rawRowAttributes().get("android:keyWidth"), parsePercent(model.rawKeyboardAttributes().get("android:keyWidth"), 10f));
    float sum = 0f;
    for (KeySpec key : row.keys()) {
      sum += parsePercent(key.rawAttributes().get("android:keyWidth"), rowDefault);
      sum += parsePercent(key.rawAttributes().get("android:horizontalGap"), 0f);
    }
    return sum;
  }

  private static float parsePercent(String raw, float fallback) {
    if (raw == null || !raw.trim().endsWith("%p")) return fallback;
    return Float.parseFloat(raw.trim().replace("%p", "").trim());
  }

  private static boolean rowContainsCode(KeyboardRow row, int code) {
    for (KeySpec key : row.keys()) {
      for (KeyCode keyCode : key.codes()) {
        if (keyCode instanceof KeyCode.Numeric numeric && numeric.value() == code) return true;
      }
    }
    return false;
  }

  @Test
  public void testFullQwertyTemplateCreatesCompleteLinkedPack() throws Exception {
    InstalledKeyboardPack pack =
        CustomKeyboardPackCreator.createKeyboardPack(
            context(), "My Full", CustomKeyboardPackCreator.TEMPLATE_FULL_QWERTY);

    Assert.assertEquals(2, pack.manifest().keyboards().size());
    Assert.assertEquals("main", pack.manifest().keyboards().get(0).id());
    Assert.assertEquals("symbols", pack.manifest().keyboards().get(1).id());

    File mainXml = new File(pack.directory(), "keyboards/main.xml");
    File symbolsXml = new File(pack.directory(), "keyboards/symbols.xml");
    Assert.assertTrue(mainXml.isFile());
    Assert.assertTrue(symbolsXml.isFile());

    // The pack-id token must be fully substituted with the generated id.
    String packId = pack.manifest().id();
    for (File file : new File[] {mainXml, symbolsXml}) {
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      Assert.assertFalse(file + " still contains the token", content.contains("__PACK_ID__"));
    }

    KeyboardModel main = parse(mainXml);
    Assert.assertEquals(4, main.rows().size());

    // The bottom row is flagged (suppresses the generic bottom row) and complete.
    KeyboardRow bottomRow = main.rows().get(3);
    Assert.assertEquals("bottom", bottomRow.rawRowAttributes().get("android:rowEdgeFlags"));
    Assert.assertTrue("space", rowContainsCode(bottomRow, 32));
    Assert.assertTrue("enter", rowContainsCode(bottomRow, 10));
    Assert.assertTrue("symbols switch", rowContainsCode(bottomRow, -303));

    // Shift and delete are present with their behavior attributes.
    KeyboardRow thirdRow = main.rows().get(2);
    Assert.assertTrue("shift", rowContainsCode(thirdRow, -1));
    Assert.assertTrue("delete", rowContainsCode(thirdRow, -5));

    // Layer switches reference the generated pack id in both directions.
    String mainContent = new String(Files.readAllBytes(mainXml.toPath()), StandardCharsets.UTF_8);
    String symbolsContent =
        new String(Files.readAllBytes(symbolsXml.toPath()), StandardCharsets.UTF_8);
    Assert.assertTrue(mainContent.contains("pack::" + packId + "::symbols"));
    Assert.assertTrue(symbolsContent.contains("pack::" + packId + "::main"));

    // No row may exceed the full keyboard width.
    KeyboardModel symbols = parse(symbolsXml);
    for (KeyboardModel model : new KeyboardModel[] {main, symbols}) {
      for (int i = 0; i < model.rows().size(); i++) {
        float width = rowWidthPercent(model, model.rows().get(i));
        Assert.assertTrue("row " + i + " too wide: " + width, width <= 100.01f);
      }
    }

    // Every key resolves to numeric codes only — no silent Symbolic leftovers.
    for (KeyboardModel model : new KeyboardModel[] {main, symbols}) {
      for (KeyboardRow row : model.rows()) {
        for (KeySpec key : row.keys()) {
          List<KeyCode> codes = key.codes();
          Assert.assertFalse(key.rawAttributes().toString(), codes.isEmpty());
          for (KeyCode code : codes) {
            Assert.assertTrue(
                "non-numeric code on " + key.rawAttributes(), code instanceof KeyCode.Numeric);
          }
        }
      }
    }
  }

  @Test
  public void testBasicQwertyTemplateStillCreatesSingleEntryPack() throws Exception {
    InstalledKeyboardPack pack =
        CustomKeyboardPackCreator.createKeyboardPack(
            context(), "My Basic", CustomKeyboardPackCreator.TEMPLATE_BASIC_QWERTY);

    Assert.assertEquals(1, pack.manifest().keyboards().size());
    Assert.assertEquals("main", pack.manifest().keyboards().get(0).id());
    KeyboardModel main = parse(new File(pack.directory(), "keyboards/main.xml"));
    Assert.assertEquals(3, main.rows().size());
  }

  @Test
  public void testUnknownTemplateThrowsAndLeavesNoDirectory() throws Exception {
    File packsRoot = new KeyboardPacksRepository(context()).packsRootDir();
    String[] before = packsRoot.list();
    Assert.assertThrows(
        Exception.class,
        () -> CustomKeyboardPackCreator.createKeyboardPack(context(), "Nope", "no_such_template"));
    Assert.assertArrayEquals(before, packsRoot.list());
  }
}
