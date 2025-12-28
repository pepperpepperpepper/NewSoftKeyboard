package wtf.uhoh.newsoftkeyboard.linuxhost.output;

import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.adapters.TextOutputBackend;

public final class StdoutJsonTextOutputBackend implements TextOutputBackend {
  @Override
  public void apply(List<SemanticAction> actions) {
    Objects.requireNonNull(actions);
    for (SemanticAction action : actions) {
      System.out.println(SemanticActionJsonLine.toJsonLine(action));
    }
  }
}
