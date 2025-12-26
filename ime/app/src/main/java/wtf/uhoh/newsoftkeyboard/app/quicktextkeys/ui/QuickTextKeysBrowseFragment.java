package wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOnsFactory;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PopupKeyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PopupListKeyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickTextKey;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.AbstractAddOnsBrowserFragment;

public class QuickTextKeysBrowseFragment extends AbstractAddOnsBrowserFragment<QuickTextKey> {

  private DefaultSkinTonePrefTracker mSkinToneTracker;
  private DefaultGenderPrefTracker mGenderTracker;

  public QuickTextKeysBrowseFragment() {
    super(
        "QuickKey",
        R.string.quick_text_keys_order,
        false,
        false,
        true,
        ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSkinToneTracker = new DefaultSkinTonePrefTracker(NskApplicationBase.prefs(requireContext()));
    mGenderTracker = new DefaultGenderPrefTracker(NskApplicationBase.prefs(requireContext()));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mSkinToneTracker.dispose();
    mGenderTracker.dispose();
  }

  @NonNull
  @Override
  protected AddOnsFactory<QuickTextKey> getAddOnFactory() {
    return NskApplicationBase.getQuickTextKeyFactory(requireContext());
  }

  @Override
  protected void onTweaksOptionSelected() {
    Navigation.findNavController(requireView())
        .navigate(
            QuickTextKeysBrowseFragmentDirections
                .actionQuickTextKeysBrowseFragmentToQuickTextSettingsFragment());
  }

  @Override
  protected void applyAddOnToDemoKeyboardView(
      @NonNull QuickTextKey addOn, @NonNull DemoKeyboardView demoKeyboardView) {
    KeyboardDefinition keyboard;
    if (addOn.isPopupKeyboardUsed()) {
      keyboard =
          new PopupKeyboard(
              addOn,
              getContext(),
              addOn.getPopupKeyboardResId(),
              demoKeyboardView.getThemedKeyboardDimens(),
              addOn.getName(),
              mSkinToneTracker.getDefaultSkinTone(),
              mGenderTracker.getDefaultGender());
    } else {
      keyboard =
          new PopupListKeyboard(
              addOn,
              getContext(),
              demoKeyboardView.getThemedKeyboardDimens(),
              addOn.getPopupListNames(),
              addOn.getPopupListValues(),
              addOn.getName());
    }
    keyboard.loadKeyboard(demoKeyboardView.getThemedKeyboardDimens());
    demoKeyboardView.setKeyboard(keyboard, null, null);

    final int keyboardViewMaxWidth =
        demoKeyboardView.getThemedKeyboardDimens().getKeyboardMaxWidth();
    if (keyboard.getMinWidth() > keyboardViewMaxWidth) {
      // fixing up the keyboard, so it will fit nicely in the width
      int currentY = 0;
      int xSub = 0;
      int rowsShown = 0;
      final int maxRowsToShow = 2;
      for (Keyboard.Key key : keyboard.getKeys()) {
        key.y = currentY;
        key.x -= xSub;
        if (Keyboard.Key.getEndX(key) > keyboardViewMaxWidth) {
          if (rowsShown < maxRowsToShow) {
            rowsShown++;
            currentY += key.height;
            xSub += key.x;
            key.y = currentY;
            key.x = 0;
          } else {
            break; // only showing maxRowsToShow rows
          }
        }
      }
      keyboard.resetDimensions();
    }
  }

  @Nullable
  @Override
  protected String getMarketSearchKeyword() {
    return "quick key";
  }

  @Override
  protected int getMarketSearchTitle() {
    return R.string.search_market_for_quick_key_addons;
  }
}
