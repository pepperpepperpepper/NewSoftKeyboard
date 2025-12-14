package com.anysoftkeyboard.ui.settings;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import java.util.List;

/**
 * Handles rendering the registered add-on UI cards into a container. Extracted to trim MainFragment.
 */
final class AddOnUICardPresenter {

  interface CardFactory {
    View create(@NonNull AddOnUICard card);
  }

  private AddOnUICardPresenter() {}

  static void refresh(
      @NonNull ViewGroup container,
      @NonNull AddOnUICardManager manager,
      @NonNull CardFactory cardFactory) {
    Logger.d("AddOnUICard", "refreshAddOnUICards() called");

    container.removeAllViews();
    List<AddOnUICard> activeCards = manager.getActiveUICards();
    Logger.d("AddOnUICard", "Found " + activeCards.size() + " active UI cards");

    for (int i = 0; i < activeCards.size(); i++) {
      AddOnUICard card = activeCards.get(i);
      Logger.d("AddOnUICard", "Processing card " + i + ": " + card.getTitle());
      View cardView = cardFactory.create(card);
      if (cardView != null) {
        container.addView(cardView);
      } else {
        Logger.w("AddOnUICard", "Failed to create card view for " + card.getTitle());
      }
    }

    container.setVisibility(activeCards.isEmpty() ? ViewGroup.GONE : ViewGroup.VISIBLE);
  }
}
