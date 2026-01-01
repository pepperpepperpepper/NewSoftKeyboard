package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PackManifestJson {
  private PackManifestJson() {}

  public static PackManifest parse(InputStream inputStream) throws IOException {
    String json = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
    return parse(json);
  }

  public static PackManifest parse(String json) throws IOException {
    Cursor cursor = new Cursor(json);
    cursor.skipWhitespace();
    cursor.expect('{');

    Integer schemaVersion = null;
    String id = null;
    String name = null;
    Integer version = null;
    String minCoreVersion = null;
    List<PackEntry> keyboards = Collections.emptyList();
    List<PackEntry> themes = Collections.emptyList();

    while (true) {
      cursor.skipWhitespace();
      if (cursor.tryConsume('}')) break;

      String key = cursor.readString();
      cursor.skipWhitespace();
      cursor.expect(':');
      cursor.skipWhitespace();

      switch (key) {
        case "schemaVersion" -> schemaVersion = cursor.readInt();
        case "id" -> id = cursor.readString();
        case "name" -> name = cursor.readString();
        case "version" -> version = cursor.readInt();
        case "minCoreVersion" -> minCoreVersion = cursor.readString();
        case "keyboards" -> keyboards = readEntriesArray(cursor);
        case "themes" -> themes = readEntriesArray(cursor);
        default -> cursor.skipValue();
      }

      cursor.skipWhitespace();
      if (cursor.tryConsume(',')) continue;
      cursor.expect('}');
      break;
    }

    int schema = schemaVersion != null ? schemaVersion : PackManifest.SUPPORTED_SCHEMA_VERSION;
    if (schema != PackManifest.SUPPORTED_SCHEMA_VERSION) {
      throw new IOException(
          "Unsupported pack manifest schemaVersion "
              + schema
              + " (supported: "
              + PackManifest.SUPPORTED_SCHEMA_VERSION
              + ")");
    }

    if (id == null || id.trim().isEmpty())
      throw new IOException("Missing required manifest field: id");
    if (name == null || name.trim().isEmpty())
      throw new IOException("Missing required manifest field: name");
    if (version == null) throw new IOException("Missing required manifest field: version");

    final String sanitizedMinCoreVersion =
        minCoreVersion != null && !minCoreVersion.trim().isEmpty() ? minCoreVersion : null;

    return new PackManifest(schema, id, name, version, sanitizedMinCoreVersion, keyboards, themes);
  }

  public static void write(PackManifest manifest, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(manifest);
    Objects.requireNonNull(outputStream);
    outputStream.write(toJson(manifest).getBytes(StandardCharsets.UTF_8));
  }

  public static String toJson(PackManifest manifest) {
    Objects.requireNonNull(manifest);
    StringBuilder builder = new StringBuilder();
    builder.append("{\n");
    builder.append("  \"schemaVersion\": ").append(manifest.schemaVersion()).append(",\n");
    builder.append("  \"id\": \"").append(escapeString(manifest.id())).append("\",\n");
    builder.append("  \"name\": \"").append(escapeString(manifest.name())).append("\",\n");
    builder.append("  \"version\": ").append(manifest.version());

    if (manifest.minCoreVersion() != null && !manifest.minCoreVersion().trim().isEmpty()) {
      builder.append(",\n");
      builder
          .append("  \"minCoreVersion\": \"")
          .append(escapeString(manifest.minCoreVersion()))
          .append('"');
    }

    builder.append(",\n");
    builder.append("  \"keyboards\": ");
    appendEntriesArray(builder, manifest.keyboards(), 2);
    builder.append(",\n");
    builder.append("  \"themes\": ");
    appendEntriesArray(builder, manifest.themes(), 2);
    builder.append('\n');
    builder.append("}\n");
    return builder.toString();
  }

  private static void appendEntriesArray(
      StringBuilder builder, List<PackEntry> entries, int baseIndentSpaces) {
    if (entries == null || entries.isEmpty()) {
      builder.append("[]");
      return;
    }

    String baseIndent = spaces(baseIndentSpaces);
    String itemIndent = spaces(baseIndentSpaces + 2);
    builder.append("[\n");
    for (int i = 0; i < entries.size(); i++) {
      PackEntry entry = entries.get(i);
      builder.append(itemIndent);
      builder.append("{\"id\":\"");
      builder.append(escapeString(entry.id()));
      builder.append("\",\"path\":\"");
      builder.append(escapeString(entry.path().value()));
      builder.append("\"}");
      if (i < entries.size() - 1) builder.append(',');
      builder.append('\n');
    }
    builder.append(baseIndent).append(']');
  }

  private static String spaces(int count) {
    if (count <= 0) return "";
    StringBuilder builder = new StringBuilder(count);
    for (int i = 0; i < count; i++) builder.append(' ');
    return builder.toString();
  }

  private static String escapeString(String raw) {
    if (raw == null || raw.isEmpty()) return "";
    StringBuilder builder = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      switch (c) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\b' -> builder.append("\\b");
        case '\f' -> builder.append("\\f");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (c <= 0x1F) {
            builder.append(String.format("\\u%04x", (int) c));
          } else {
            builder.append(c);
          }
        }
      }
    }
    return builder.toString();
  }

  private static byte[] readAllBytes(InputStream inputStream) throws IOException {
    try (var out = new ByteArrayOutputStream()) {
      copy(inputStream, out);
      return out.toByteArray();
    }
  }

  private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[8 * 1024];
    int read;
    while ((read = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, read);
    }
  }

  private static List<PackEntry> readEntriesArray(Cursor cursor) throws IOException {
    cursor.expect('[');
    cursor.skipWhitespace();
    if (cursor.tryConsume(']')) return Collections.emptyList();

    var entries = new ArrayList<PackEntry>();
    while (true) {
      cursor.skipWhitespace();
      entries.add(readEntry(cursor));
      cursor.skipWhitespace();
      if (cursor.tryConsume(',')) continue;
      cursor.expect(']');
      break;
    }
    return Collections.unmodifiableList(new ArrayList<>(entries));
  }

  private static PackEntry readEntry(Cursor cursor) throws IOException {
    cursor.expect('{');

    String id = null;
    PackPath path = null;

    while (true) {
      cursor.skipWhitespace();
      if (cursor.tryConsume('}')) break;

      String key = cursor.readString();
      cursor.skipWhitespace();
      cursor.expect(':');
      cursor.skipWhitespace();

      switch (key) {
        case "id" -> id = cursor.readString();
        case "path" -> {
          String rawPath = cursor.readString();
          try {
            path = PackPath.parse(rawPath);
          } catch (IllegalArgumentException e) {
            throw new IOException("Invalid pack path: " + rawPath, e);
          }
        }
        default -> cursor.skipValue();
      }

      cursor.skipWhitespace();
      if (cursor.tryConsume(',')) continue;
      cursor.expect('}');
      break;
    }

    if (id == null || id.trim().isEmpty())
      throw new IOException("Entry missing required field: id");
    if (path == null) throw new IOException("Entry missing required field: path");
    return new PackEntry(id, path);
  }

  private static final class Cursor {
    private final String input;
    private int index;

    private Cursor(String input) {
      this.input = input;
    }

    void skipWhitespace() {
      while (index < input.length()) {
        char c = input.charAt(index);
        if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
          index++;
          continue;
        }
        break;
      }
    }

    boolean tryConsume(char expected) {
      if (index < input.length() && input.charAt(index) == expected) {
        index++;
        return true;
      }
      return false;
    }

    void expect(char expected) throws IOException {
      if (index >= input.length() || input.charAt(index) != expected) {
        throw new IOException("Expected '" + expected + "' at index " + index + ", got " + peek());
      }
      index++;
    }

    String peek() {
      if (index >= input.length()) return "<eof>";
      return "'" + input.charAt(index) + "'";
    }

    String readString() throws IOException {
      expect('"');
      StringBuilder builder = new StringBuilder();
      while (index < input.length()) {
        char c = input.charAt(index++);
        if (c == '"') {
          return builder.toString();
        }
        if (c == '\\') {
          if (index >= input.length()) throw new IOException("Unexpected end in string");
          char escaped = input.charAt(index++);
          switch (escaped) {
            case '"', '\\', '/' -> builder.append(escaped);
            case 'b' -> builder.append('\b');
            case 'f' -> builder.append('\f');
            case 'n' -> builder.append('\n');
            case 'r' -> builder.append('\r');
            case 't' -> builder.append('\t');
            case 'u' -> {
              if (index + 4 > input.length())
                throw new IOException("Invalid unicode escape in string");
              String hex = input.substring(index, index + 4);
              index += 4;
              try {
                builder.append((char) Integer.parseInt(hex, 16));
              } catch (NumberFormatException e) {
                throw new IOException("Invalid unicode escape \\u" + hex, e);
              }
            }
            default -> throw new IOException("Unsupported escape sequence: \\" + escaped);
          }
          continue;
        }
        builder.append(c);
      }
      throw new IOException("Unexpected end of input in string");
    }

    int readInt() throws IOException {
      String number = readNumber();
      try {
        return Integer.parseInt(number);
      } catch (NumberFormatException e) {
        throw new IOException("Invalid integer: " + number, e);
      }
    }

    String readNumber() throws IOException {
      int start = index;
      if (index < input.length() && input.charAt(index) == '-') index++;
      while (index < input.length()) {
        char c = input.charAt(index);
        if (c >= '0' && c <= '9') {
          index++;
          continue;
        }
        break;
      }
      if (start == index) throw new IOException("Expected number at index " + start);
      return input.substring(start, index);
    }

    void skipValue() throws IOException {
      skipWhitespace();
      if (index >= input.length()) throw new IOException("Expected value at EOF");

      char c = input.charAt(index);
      switch (c) {
        case '"' -> {
          readString();
        }
        case '{' -> skipObject();
        case '[' -> skipArray();
        default -> {
          if (c == '-' || (c >= '0' && c <= '9')) {
            readNumber();
            return;
          }
          if (consumeLiteral("true") || consumeLiteral("false") || consumeLiteral("null")) return;
          throw new IOException("Unsupported value at index " + index + ": " + peek());
        }
      }
    }

    private boolean consumeLiteral(String literal) {
      if (input.regionMatches(index, literal, 0, literal.length())) {
        index += literal.length();
        return true;
      }
      return false;
    }

    private void skipObject() throws IOException {
      expect('{');
      skipWhitespace();
      if (tryConsume('}')) return;
      while (true) {
        skipWhitespace();
        readString();
        skipWhitespace();
        expect(':');
        skipWhitespace();
        skipValue();
        skipWhitespace();
        if (tryConsume(',')) continue;
        expect('}');
        return;
      }
    }

    private void skipArray() throws IOException {
      expect('[');
      skipWhitespace();
      if (tryConsume(']')) return;
      while (true) {
        skipWhitespace();
        skipValue();
        skipWhitespace();
        if (tryConsume(',')) continue;
        expect(']');
        return;
      }
    }
  }
}
