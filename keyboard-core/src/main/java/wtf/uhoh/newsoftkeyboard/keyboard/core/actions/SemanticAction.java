package wtf.uhoh.newsoftkeyboard.keyboard.core.actions;

import java.util.Objects;

public sealed interface SemanticAction
    permits SemanticAction.CommitText,
        SemanticAction.DeleteBackward,
        SemanticAction.DeleteForward,
        SemanticAction.PerformEditorAction {
  record CommitText(String text) implements SemanticAction {
    public CommitText {
      Objects.requireNonNull(text);
    }
  }

  record DeleteBackward(int count) implements SemanticAction {}

  record DeleteForward(int count) implements SemanticAction {}

  record PerformEditorAction(EditorAction action) implements SemanticAction {
    public PerformEditorAction {
      Objects.requireNonNull(action);
    }
  }
}
