package wtf.uhoh.newsoftkeyboard.linuxhost.output;

import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;

final class SemanticActionJsonLine {
  private SemanticActionJsonLine() {}

  static String toJsonLine(SemanticAction action) {
    Objects.requireNonNull(action);

    if (action instanceof SemanticAction.CommitText commitText) {
      return "{\"type\":\"commit\",\"text\":" + jsonString(commitText.text()) + "}";
    } else if (action instanceof SemanticAction.DeleteBackward deleteBackward) {
      return "{\"type\":\"delete_backward\",\"count\":" + deleteBackward.count() + "}";
    } else if (action instanceof SemanticAction.DeleteForward deleteForward) {
      return "{\"type\":\"delete_forward\",\"count\":" + deleteForward.count() + "}";
    } else if (action instanceof SemanticAction.PerformEditorAction editorAction) {
      return "{\"type\":\"editor_action\",\"action\":"
          + jsonString(editorAction.action().name())
          + "}";
    } else {
      return "{\"type\":\"unknown\",\"value\":" + jsonString(action.toString()) + "}";
    }
  }

  private static String jsonString(String raw) {
    Objects.requireNonNull(raw);
    StringBuilder builder = new StringBuilder(raw.length() + 2);
    builder.append('"');
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
          if (c < 0x20) {
            builder.append(String.format("\\u%04x", (int) c));
          } else {
            builder.append(c);
          }
        }
      }
    }
    builder.append('"');
    return builder.toString();
  }
}
