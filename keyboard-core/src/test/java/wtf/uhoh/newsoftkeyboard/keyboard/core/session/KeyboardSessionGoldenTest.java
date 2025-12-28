package wtf.uhoh.newsoftkeyboard.keyboard.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;

public class KeyboardSessionGoldenTest {
  @Test
  public void emitsExpectedSemanticActionsForBasicFixture() throws Exception {
    var xml = KeyboardSessionGoldenTest.class.getResourceAsStream("/fixtures/basic_keyboard.xml");
    assertNotNull("Missing test fixture resource", xml);
    var model = AskXmlKeyboardParser.parse(xml);

    KeyboardSession session = new KeyboardSession(model);

    var log = new StringBuilder();
    for (int[] press : new int[][] {{0, 4}, {0, 0}, {0, 1}, {0, 2}, {0, 3}}) {
      for (var action : session.pressKey(press[0], press[1])) {
        log.append(action.toString()).append('\n');
      }
    }

    var expectedStream =
        KeyboardSessionGoldenTest.class.getResourceAsStream("/fixtures/basic_keyboard.golden.txt");
    assertNotNull("Missing golden output fixture", expectedStream);
    String expected = new String(expectedStream.readAllBytes(), StandardCharsets.UTF_8);

    assertEquals(expected, log.toString());
  }
}
