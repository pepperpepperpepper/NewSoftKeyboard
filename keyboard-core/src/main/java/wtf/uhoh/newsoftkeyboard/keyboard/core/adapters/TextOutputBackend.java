package wtf.uhoh.newsoftkeyboard.keyboard.core.adapters;

import java.util.List;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;

public interface TextOutputBackend {
  void apply(List<SemanticAction> actions);
}
