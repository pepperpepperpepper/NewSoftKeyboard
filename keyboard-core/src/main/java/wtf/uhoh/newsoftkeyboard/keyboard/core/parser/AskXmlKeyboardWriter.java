package wtf.uhoh.newsoftkeyboard.keyboard.core.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;

public final class AskXmlKeyboardWriter {
  private static final String INDENT_ROW = "    ";
  private static final String INDENT_KEY = "        ";

  private AskXmlKeyboardWriter() {}

  public static void write(KeyboardModel model, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(model);
    Objects.requireNonNull(outputStream);
    outputStream.write(toXml(model).getBytes(StandardCharsets.UTF_8));
  }

  public static String toXml(KeyboardModel model) {
    StringBuilder builder = new StringBuilder();
    builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    builder.append("<Keyboard");
    appendAttributes(builder, model.rawKeyboardAttributes());
    builder.append(">\n");

    for (KeyboardRow row : model.rows()) {
      builder.append(INDENT_ROW).append("<Row");
      appendAttributes(builder, row.rawRowAttributes());
      builder.append(">\n");

      for (KeySpec key : row.keys()) {
        builder.append(INDENT_KEY).append("<Key");
        appendAttributes(builder, key.rawAttributes());
        builder.append("/>\n");
      }

      builder.append(INDENT_ROW).append("</Row>\n");
    }

    builder.append("</Keyboard>\n");
    return builder.toString();
  }

  private static void appendAttributes(StringBuilder builder, Map<String, String> attrs) {
    if (attrs == null || attrs.isEmpty()) return;

    List<Map.Entry<String, String>> entries = new ArrayList<>(attrs.entrySet());
    entries.sort(Comparator.comparing(e -> e.getKey(), AskXmlKeyboardWriter::compareAttrNames));

    for (Map.Entry<String, String> entry : entries) {
      builder
          .append(' ')
          .append(entry.getKey())
          .append("=\"")
          .append(escapeAttr(entry.getValue()))
          .append('"');
    }
  }

  private static int compareAttrNames(String a, String b) {
    int aGroup = attrGroup(a);
    int bGroup = attrGroup(b);
    if (aGroup != bGroup) return Integer.compare(aGroup, bGroup);
    return a.compareTo(b);
  }

  private static int attrGroup(String name) {
    if (name == null) return 99;
    if (name.startsWith("xmlns")) return 0;
    if (name.startsWith("android:")) return 1;
    return 2;
  }

  private static String escapeAttr(String raw) {
    if (raw == null || raw.isEmpty()) return "";
    StringBuilder builder = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      switch (c) {
        case '&' -> builder.append("&amp;");
        case '<' -> builder.append("&lt;");
        case '"' -> builder.append("&quot;");
        case '>' -> builder.append("&gt;");
        default -> builder.append(c);
      }
    }
    return builder.toString();
  }
}
