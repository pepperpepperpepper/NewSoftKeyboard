package wtf.uhoh.newsoftkeyboard.keyboard.core.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Test;

public class ThemeXmlWriterTest {
  @Test
  public void roundTripsBasicThemeFixture() throws Exception {
    try (InputStream in =
        ThemeXmlWriterTest.class.getResourceAsStream(
            "/fixtures/packs/basic_pack/themes/basic_theme.xml")) {
      assertNotNull("Missing test fixture resource", in);

      var theme = ThemeXmlParser.parse(in);

      var out = new ByteArrayOutputStream();
      ThemeXmlWriter.write(theme, out);

      var written = ThemeXmlParser.parse(new ByteArrayInputStream(out.toByteArray()));

      assertEquals(theme.colors(), written.colors());
      assertEquals(theme.icons(), written.icons());
      assertTrue(written.colors().containsKey("keyboardBackground"));
    }
  }
}
