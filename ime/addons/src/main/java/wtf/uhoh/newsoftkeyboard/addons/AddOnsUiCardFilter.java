package wtf.uhoh.newsoftkeyboard.addons;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AddOnsUiCardFilter {

  private AddOnsUiCardFilter() {}

  @NonNull
  static <E extends AddOn> List<E> filterAddOnsWithUiCard(@NonNull List<E> allAddOns) {
    final List<E> uiCardAddOns = new ArrayList<>();
    for (E addOn : allAddOns) {
      if (addOn instanceof AddOnImpl && ((AddOnImpl) addOn).hasUICard()) {
        uiCardAddOns.add(addOn);
      }
    }
    return Collections.unmodifiableList(uiCardAddOns);
  }
}
