package wtf.uhoh.newsoftkeyboard.keyboard.core.theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;

public final class ThemeXmlParser {
  private ThemeXmlParser() {}

  public static ThemeModel parse(InputStream xmlInputStream) throws IOException {
    try {
      var documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      var builder = documentBuilderFactory.newDocumentBuilder();
      var document = builder.parse(xmlInputStream);

      var themeElement = document.getDocumentElement();
      String rootTag = themeElement.getTagName();
      if (!"KeyboardTheme".equals(rootTag) && !"Theme".equals(rootTag)) {
        throw new IOException(
            "Expected root tag <KeyboardTheme> or <Theme>, got <"
                + themeElement.getTagName()
                + ">");
      }

      var rawAttributes = new HashMap<String, String>();
      NamedNodeMap attrs = themeElement.getAttributes();
      for (int i = 0; i < attrs.getLength(); i++) {
        Node attr = attrs.item(i);
        rawAttributes.put(attr.getNodeName(), attr.getNodeValue());
      }

      var colors = new HashMap<String, Integer>();
      NodeList colorNodes = themeElement.getElementsByTagName("Color");
      for (int i = 0; i < colorNodes.getLength(); i++) {
        Element colorElement = (Element) colorNodes.item(i);
        String name = colorElement.getAttribute("name");
        String value = colorElement.getAttribute("value");
        if (name == null || name.isBlank() || value == null || value.isBlank()) continue;
        colors.put(name, parseColor(value));
      }

      var icons = new HashMap<String, PackPath>();
      NodeList iconNodes = themeElement.getElementsByTagName("Icon");
      for (int i = 0; i < iconNodes.getLength(); i++) {
        Element iconElement = (Element) iconNodes.item(i);
        String name = iconElement.getAttribute("name");
        String path = iconElement.getAttribute("path");
        if (name == null || name.isBlank() || path == null || path.isBlank()) continue;
        icons.put(name, PackPath.parse(path));
      }

      return new ThemeModel(colors, icons, rawAttributes);
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("Failed parsing theme XML", e);
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid theme XML attribute", e);
    }
  }

  private static int parseColor(String raw) {
    String value = raw.trim();
    if (value.startsWith("@")) {
      throw new IllegalArgumentException(
          "Android resource references are not allowed in pack themes: " + raw);
    }
    if (!value.startsWith("#")) {
      throw new IllegalArgumentException("Expected #RRGGBB or #AARRGGBB color, got: " + raw);
    }

    String hex = value.substring(1).toLowerCase(Locale.ROOT);
    if (hex.length() == 6) {
      int rgb = Integer.parseUnsignedInt(hex, 16);
      return 0xFF00_0000 | rgb;
    }
    if (hex.length() == 8) {
      return (int) Long.parseUnsignedLong(hex, 16);
    }
    throw new IllegalArgumentException(
        "Unsupported color format (expected #RRGGBB or #AARRGGBB): " + raw);
  }
}
