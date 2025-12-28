package wtf.uhoh.newsoftkeyboard.keyboard.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import org.junit.Test;

public class AskXmlKeyboardParserTest {
  @Test
  public void parsesBasicKeyboardFixture() throws Exception {
    try (InputStream in =
        AskXmlKeyboardParserTest.class.getResourceAsStream("/fixtures/basic_keyboard.xml")) {
      assertTrue("Missing test fixture resource", in != null);

      var model = AskXmlKeyboardParser.parse(in);

      assertEquals(1, model.rows().size());
      assertEquals(5, model.rows().get(0).keys().size());

      var firstKey = model.rows().get(0).keys().get(0);
      assertEquals("a", firstKey.label().orElseThrow());
      assertEquals(1, firstKey.codes().size());
      assertEquals(97, firstKey.codes().get(0).asNumeric().orElseThrow());
    }
  }
}
