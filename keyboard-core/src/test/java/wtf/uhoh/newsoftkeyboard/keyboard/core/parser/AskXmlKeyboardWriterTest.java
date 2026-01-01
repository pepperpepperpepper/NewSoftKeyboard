package wtf.uhoh.newsoftkeyboard.keyboard.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Test;

public class AskXmlKeyboardWriterTest {
  @Test
  public void roundTripsBasicKeyboardFixture() throws Exception {
    try (InputStream in =
        AskXmlKeyboardWriterTest.class.getResourceAsStream("/fixtures/basic_keyboard.xml")) {
      assertNotNull("Missing test fixture resource", in);

      var model = AskXmlKeyboardParser.parse(in);

      var out = new ByteArrayOutputStream();
      AskXmlKeyboardWriter.write(model, out);

      var written = AskXmlKeyboardParser.parse(new ByteArrayInputStream(out.toByteArray()));

      assertEquals(model.rows().size(), written.rows().size());
      assertEquals(model.rows().get(0).keys().size(), written.rows().get(0).keys().size());
      assertTrue(written.rawKeyboardAttributes().containsKey("xmlns:android"));

      var firstKey = written.rows().get(0).keys().get(0);
      assertEquals("97", firstKey.rawAttributes().get("android:codes"));
      assertEquals("a", firstKey.rawAttributes().get("android:keyLabel"));
    }
  }
}
