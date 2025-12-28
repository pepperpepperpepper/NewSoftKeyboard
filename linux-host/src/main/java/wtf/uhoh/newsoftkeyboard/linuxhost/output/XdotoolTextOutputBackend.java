package wtf.uhoh.newsoftkeyboard.linuxhost.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.EditorAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.adapters.TextOutputBackend;

public final class XdotoolTextOutputBackend implements TextOutputBackend {
  private final Optional<String> windowId;
  private final int delayMs;

  public XdotoolTextOutputBackend(Optional<String> windowId, int delayMs) {
    this.windowId = Objects.requireNonNull(windowId);
    this.delayMs = delayMs;
  }

  @Override
  public void apply(List<SemanticAction> actions) {
    Objects.requireNonNull(actions);
    activateWindowIfNeeded();
    StringBuilder pendingText = new StringBuilder();

    for (SemanticAction action : actions) {
      if (action instanceof SemanticAction.CommitText commitText) {
        pendingText.append(commitText.text());
        continue;
      }

      flushPendingText(pendingText);
      applyNonTextAction(action);
    }

    flushPendingText(pendingText);
  }

  private void activateWindowIfNeeded() {
    windowId.ifPresent(
        id -> {
          if (id.isBlank()) return;
          // `xdotool type --window <id>` may still require focus; activate first for reliability.
          run(List.of("xdotool", "windowactivate", "--sync", id));
        });
  }

  private void flushPendingText(StringBuilder pendingText) {
    if (pendingText.isEmpty()) return;
    runType(pendingText.toString());
    pendingText.setLength(0);
  }

  private void applyNonTextAction(SemanticAction action) {
    if (action instanceof SemanticAction.DeleteBackward deleteBackward) {
      runKeyRepeat("BackSpace", deleteBackward.count());
    } else if (action instanceof SemanticAction.DeleteForward deleteForward) {
      runKeyRepeat("Delete", deleteForward.count());
    } else if (action instanceof SemanticAction.PerformEditorAction editorAction) {
      runEditorAction(editorAction.action());
    } else {
      // ignore unknown actions
    }
  }

  private void runEditorAction(EditorAction action) {
    switch (action) {
      case ENTER, NEXT, DONE -> runKey("Return");
      case TAB -> runKey("Tab");
    }
  }

  private void runType(String text) {
    var command = new ArrayList<String>();
    command.add("xdotool");
    command.add("type");
    windowId.ifPresent(id -> addWindowArg(command, id));
    if (delayMs >= 0) {
      command.add("--delay");
      command.add(Integer.toString(delayMs));
    }
    command.add("--clearmodifiers");
    command.add("--");
    command.add(text);
    run(command);
  }

  private void runKey(String key) {
    runKeyRepeat(key, 1);
  }

  private void runKeyRepeat(String key, int count) {
    if (count <= 0) return;
    var command = new ArrayList<String>();
    command.add("xdotool");
    command.add("key");
    windowId.ifPresent(id -> addWindowArg(command, id));
    command.add("--clearmodifiers");
    if (count > 1) {
      command.add("--repeat");
      command.add(Integer.toString(count));
    }
    command.add(key);
    run(command);
  }

  private static void addWindowArg(List<String> command, String windowId) {
    if (windowId.isBlank()) return;
    command.add("--window");
    command.add(windowId);
  }

  private static void run(List<String> command) {
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        System.err.println("xdotool failed (" + exitCode + "): " + String.join(" ", command));
        if (!output.isBlank()) {
          System.err.println(output.trim());
        }
      }
    } catch (IOException e) {
      System.err.println("Failed running: " + String.join(" ", command) + " (" + e + ")");
      printProcessOutput(process);
    } catch (InterruptedException e) {
      System.err.println("Interrupted running: " + String.join(" ", command));
      printProcessOutput(process);
      Thread.currentThread().interrupt();
    }
  }

  private static void printProcessOutput(Process process) {
    if (process == null) return;
    try {
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (!output.isBlank()) {
        System.err.println(output.trim());
      }
    } catch (IOException ignored) {
      // ignore
    }
  }
}
