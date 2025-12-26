/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOnsFactory;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;

public class KeyboardAddOnBrowserFragment
    extends AbstractAddOnsBrowserFragment<KeyboardAddOnAndBuilder> {

  public KeyboardAddOnBrowserFragment() {
    super(
        "LanguageAddOnBrowserFragment",
        R.string.keyboards_group,
        false,
        false,
        false,
        ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
  }

  @NonNull
  @Override
  protected AddOnsFactory<KeyboardAddOnAndBuilder> getAddOnFactory() {
    return NskApplicationBase.getKeyboardFactory(getContext());
  }

  @Nullable
  @Override
  protected String getMarketSearchKeyword() {
    return "language";
  }

  @Override
  protected int getMarketSearchTitle() {
    return R.string.search_market_for_keyboard_addons;
  }

  @Override
  protected void applyAddOnToDemoKeyboardView(
      @NonNull KeyboardAddOnAndBuilder addOn, @NonNull DemoKeyboardView demoKeyboardView) {
    KeyboardDefinition defaultKeyboard = addOn.createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    defaultKeyboard.loadKeyboard(demoKeyboardView.getThemedKeyboardDimens());
    demoKeyboardView.setKeyboard(defaultKeyboard, null, null);
  }
}
