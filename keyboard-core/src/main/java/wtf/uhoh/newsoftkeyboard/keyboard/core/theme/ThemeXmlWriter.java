package wtf.uhoh.newsoftkeyboard.keyboard.core.theme;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;

public final class ThemeXmlWriter {
  private static final String INDENT = "    ";

  private ThemeXmlWriter() {}

  public static void write(ThemeModel model, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(model);
    Objects.requireNonNull(outputStream);
    outputStream.write(toXml(model).getBytes(StandardCharsets.UTF_8));
  }

  public static String toXml(ThemeModel model) {
    StringBuilder builder = new StringBuilder();
    builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    builder.append("<KeyboardTheme");
    appendAttributes(builder, model.rawAttributes());
    builder.append(">\n");

    for (Map.Entry<String, Integer> entry : sortedEntries(model.colors())) {
      builder
          .append(INDENT)
          .append("<Color name=\"")
          .append(escapeAttr(entry.getKey()))
          .append("\" value=\"")
          .append(formatColor(entry.getValue()))
          .append("\"/>\n");
    }

    for (Map.Entry<String, PackPath> entry : sortedEntries(model.icons())) {
      builder
          .append(INDENT)
          .append("<Icon name=\"")
          .append(escapeAttr(entry.getKey()))
          .append("\" path=\"")
          .append(escapeAttr(entry.getValue().value()))
          .append("\"/>\n");
    }

    builder.append("</KeyboardTheme>\n");
    return builder.toString();
  }

  private static <T> List<Map.Entry<String, T>> sortedEntries(Map<String, T> map) {
    if (map == null || map.isEmpty()) return List.of();
    List<Map.Entry<String, T>> entries = new ArrayList<>(map.entrySet());
    entries.sort(Comparator.comparing(Map.Entry::getKey));
    return entries;
  }

  private static void appendAttributes(StringBuilder builder, Map<String, String> attrs) {
    if (attrs == null || attrs.isEmpty()) return;

    List<Map.Entry<String, String>> entries = new ArrayList<>(attrs.entrySet());
    entries.sort(Comparator.comparing(e -> e.getKey(), ThemeXmlWriter::compareAttrNames));

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

  private static String formatColor(int argb) {
    int alpha = (argb >>> 24) & 0xFF;
    if (alpha == 0xFF) {
      return String.format(Locale.ROOT, "#%06x", argb & 0x00FF_FFFF);
    }
    return String.format(Locale.ROOT, "#%08x", argb);
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
