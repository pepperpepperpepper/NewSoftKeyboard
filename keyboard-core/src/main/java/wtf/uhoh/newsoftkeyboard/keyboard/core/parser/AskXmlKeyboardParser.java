package wtf.uhoh.newsoftkeyboard.keyboard.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;

public final class AskXmlKeyboardParser {
  public static final String ANDROID_NS_URI = "http://schemas.android.com/apk/res/android";

  private AskXmlKeyboardParser() {}

  public static KeyboardModel parse(InputStream xmlInputStream) throws IOException {
    try {
      var documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      var builder = documentBuilderFactory.newDocumentBuilder();
      var document = builder.parse(xmlInputStream);

      var keyboardElement = document.getDocumentElement();
      if (!"Keyboard".equals(keyboardElement.getTagName())) {
        throw new IOException(
            "Expected root tag <Keyboard>, got <" + keyboardElement.getTagName() + ">");
      }

      Map<String, String> rawKeyboardAttributes = rawAttributes(keyboardElement);

      var rowNodes = keyboardElement.getElementsByTagName("Row");
      var rows = new ArrayList<KeyboardRow>(rowNodes.getLength());
      for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
        var rowElement = (Element) rowNodes.item(rowIndex);
        rows.add(parseRow(rowElement));
      }

      return new KeyboardModel(rawKeyboardAttributes, rows);
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("Failed parsing keyboard XML", e);
    }
  }

  private static KeyboardRow parseRow(Element rowElement) {
    Map<String, String> rawRowAttributes = rawAttributes(rowElement);
    NodeList keyNodes = rowElement.getChildNodes();
    var keys = new ArrayList<KeySpec>();
    for (int keyIndex = 0; keyIndex < keyNodes.getLength(); keyIndex++) {
      Node node = keyNodes.item(keyIndex);
      if (node.getNodeType() != Node.ELEMENT_NODE) continue;
      if (!"Key".equals(node.getNodeName())) continue;
      keys.add(parseKey((Element) node));
    }
    return new KeyboardRow(rawRowAttributes, keys);
  }

  private static KeySpec parseKey(Element keyElement) {
    Map<String, String> rawAttributes = rawAttributes(keyElement);

    String label = optionalAttr(keyElement, ANDROID_NS_URI, "keyLabel");
    String popupCharacters = optionalAttr(keyElement, ANDROID_NS_URI, "popupCharacters");
    String codesRaw = keyElement.getAttributeNS(ANDROID_NS_URI, "codes");
    List<KeyCode> codes = parseCodes(codesRaw);

    return new KeySpec(codes, label, popupCharacters, rawAttributes);
  }

  private static Map<String, String> rawAttributes(Element element) {
    var rawAttributes = new HashMap<String, String>();
    NamedNodeMap attrs = element.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      Node attr = attrs.item(i);
      rawAttributes.put(attr.getNodeName(), attr.getNodeValue());
    }
    return Collections.unmodifiableMap(rawAttributes);
  }

  private static String optionalAttr(Element element, String nsUri, String localName) {
    String value = element.getAttributeNS(nsUri, localName);
    if (value == null || value.isEmpty()) return null;
    return value;
  }

  private static List<KeyCode> parseCodes(String raw) {
    if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();

    String trimmed = raw.trim();
    if (trimmed.startsWith("@")) {
      return Collections.singletonList(new KeyCode.Symbolic(trimmed));
    }

    return parseCommaSeparatedCodes(trimmed);
  }

  private static List<KeyCode> parseCommaSeparatedCodes(String trimmed) {
    var result = new ArrayList<KeyCode>();
    int startIndex = 0;
    while (startIndex < trimmed.length()) {
      int commaIndex = trimmed.indexOf(',', startIndex);
      String part =
          commaIndex == -1
              ? trimmed.substring(startIndex)
              : trimmed.substring(startIndex, commaIndex);
      String value = part.trim();
      if (!value.isEmpty()) {
        try {
          result.add(new KeyCode.Numeric(Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
          result.add(new KeyCode.Symbolic(value));
        }
      }

      if (commaIndex == -1) break;
      startIndex = commaIndex + 1;
    }
    return result;
  }
}
