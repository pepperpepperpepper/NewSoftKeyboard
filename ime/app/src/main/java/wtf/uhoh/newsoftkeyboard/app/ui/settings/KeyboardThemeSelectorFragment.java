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

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.f2prateek.rx.preferences2.Preference;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOnsFactory;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayDataImpl;

public class KeyboardThemeSelectorFragment extends AbstractAddOnsBrowserFragment<KeyboardTheme> {

  private TextView mApplySummaryText;
  private Preference<Boolean> mApplyPrefs;
  private DemoKeyboardView mSelectedKeyboardView;
  @Nullable private TextView mCustomizeRowSummaryText;
  @Nullable private KeyboardThemePresetStore mPresetStore;
  @Nullable private KeyboardWallpaperOverrideStore mWallpaperStore;
  private OverlayData mOverlayData = new OverlayDataImpl();

  public KeyboardThemeSelectorFragment() {
    super("KeyboardThemeSelectorFragment", R.string.keyboard_theme_list_title, true, false, true);
  }

  @NonNull
  @Override
  protected AddOnsFactory<KeyboardTheme> getAddOnFactory() {
    return NskApplicationBase.getKeyboardThemeFactory(requireContext());
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    final MenuItem customize = menu.findItem(R.id.tweaks_menu_option);
    if (customize != null) {
      customize.setTitle(R.string.keyboard_theme_customize_menu_title);
    }
  }

  @Override
  protected void onTweaksOptionSelected() {
    AppearanceOwnerNavigation.navigateToOwner(
        requireView(), "nav:keyboard_theme_wallpaper_customization");
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    insertCustomizeRow(view);
    mSelectedKeyboardView = view.findViewById(R.id.demo_keyboard_view);
    mPresetStore = new KeyboardThemePresetStore(requireContext());
    mWallpaperStore = new KeyboardWallpaperOverrideStore(requireContext());
    updateCustomizeRowSummary(getAddOnFactory().getEnabledAddOn());

    mApplyPrefs =
        NskApplicationBase.prefs(requireContext())
            .getBoolean(
                R.string.settings_key_apply_remote_app_colors,
                R.bool.settings_default_apply_remote_app_colors);
    ViewGroup demoView = view.findViewById(R.id.demo_keyboard_view_background);
    final View applyOverlayView =
        getLayoutInflater().inflate(R.layout.prefs_adapt_theme_to_remote_app, demoView, false);
    demoView.addView(applyOverlayView);
    mApplySummaryText = applyOverlayView.findViewById(R.id.apply_overlay_summary);
    CheckBox checkBox = applyOverlayView.findViewById(R.id.apply_overlay);
    View demoAppsRoot = applyOverlayView.findViewById(R.id.overlay_demo_apps_root);
    checkBox.setOnCheckedChangeListener(
        (v, isChecked) -> {
          mApplyPrefs.set(isChecked);
          mApplySummaryText.setText(
              isChecked ? R.string.apply_overlay_summary_on : R.string.apply_overlay_summary_off);
          demoAppsRoot.setVisibility(isChecked ? View.VISIBLE : View.GONE);
          if (!isChecked) {
            mOverlayData = new OverlayDataImpl(); /*empty one, to clear overlay*/
            mSelectedKeyboardView.setThemeOverlay(mOverlayData);
          }
        });

    checkBox.setChecked(mApplyPrefs.get());

    demoAppsRoot.findViewById(R.id.theme_app_demo_phone).setOnClickListener(this::onDemoAppClicked);
    demoAppsRoot
        .findViewById(R.id.theme_app_demo_twitter)
        .setOnClickListener(this::onDemoAppClicked);
    demoAppsRoot
        .findViewById(R.id.theme_app_demo_whatsapp)
        .setOnClickListener(this::onDemoAppClicked);
    demoAppsRoot.findViewById(R.id.theme_app_demo_gmail).setOnClickListener(this::onDemoAppClicked);
  }

  private void insertCustomizeRow(@NonNull View view) {
    final ViewGroup root = view.findViewById(R.id.add_on_selection_root);
    if (root == null) return;

    final View listForeground = view.findViewById(R.id.list_foreground);
    final int listIndex = listForeground != null ? root.indexOfChild(listForeground) : -1;
    if (listIndex < 0) return;

    final View customizeRow =
        getLayoutInflater().inflate(R.layout.keyboard_theme_selector_customize_row, root, false);
    mCustomizeRowSummaryText = customizeRow.findViewById(R.id.keyboard_theme_customize_row_summary);
    customizeRow.setOnClickListener(
        ignored ->
            AppearanceOwnerNavigation.navigateToOwner(
                view, "nav:keyboard_theme_wallpaper_customization"));
    root.addView(customizeRow, listIndex);
  }

  private void updateCustomizeRowSummary(@Nullable KeyboardTheme theme) {
    final TextView summary = mCustomizeRowSummaryText;
    if (summary == null || theme == null) return;
    final KeyboardThemePresetStore presetStore = mPresetStore;
    if (presetStore == null) return;
    final String baseThemeId = theme.getId();
    final String presetId = presetStore.getActivePresetId(baseThemeId);

    String presetName = presetStore.getPresetName(presetId);
    if (TextUtils.isEmpty(presetName)) {
      presetName =
          presetId.equals(baseThemeId)
              ? getString(R.string.keyboard_theme_presets_default_entry)
              : getString(R.string.keyboard_theme_presets_unnamed_entry);
    }

    final KeyboardWallpaperOverrideStore wallpaperStore = mWallpaperStore;
    final boolean hasPhoto =
        wallpaperStore != null
            && wallpaperStore.hasWallpaper(presetId)
            && !wallpaperStore.isWallpaperInvalid(presetId);
    summary.setText(
        hasPhoto
            ? getString(R.string.keyboard_theme_customize_current_summary_preset, presetName)
                + " \u2022 "
                + getString(R.string.keyboard_theme_customize_current_summary_photo_set)
            : getString(R.string.keyboard_theme_customize_current_summary_preset, presetName));
  }

  private void onDemoAppClicked(View view) {
    final int primaryBackground;
    final int secondaryBackground;
    final int primaryText;
    final int secondaryText;
    switch (view.getId()) {
      case R.id.theme_app_demo_phone:
        primaryBackground = R.color.overlay_demo_app_phone_primary_background;
        secondaryBackground = R.color.overlay_demo_app_phone_secondary_background;
        primaryText = R.color.overlay_demo_app_phone_primary_text;
        secondaryText = R.color.overlay_demo_app_phone_primary_text;
        break;
      case R.id.theme_app_demo_twitter:
        primaryBackground = R.color.overlay_demo_app_twitter_primary_background;
        secondaryBackground = R.color.overlay_demo_app_twitter_secondary_background;
        primaryText = R.color.overlay_demo_app_twitter_primary_text;
        secondaryText = R.color.overlay_demo_app_twitter_primary_text;
        break;
      case R.id.theme_app_demo_whatsapp:
        primaryBackground = R.color.overlay_demo_app_whatsapp_primary_background;
        secondaryBackground = R.color.overlay_demo_app_whatsapp_secondary_background;
        primaryText = R.color.overlay_demo_app_whatsapp_primary_text;
        secondaryText = R.color.overlay_demo_app_whatsapp_primary_text;
        break;
      case R.id.theme_app_demo_gmail:
        primaryBackground = R.color.overlay_demo_app_gmail_primary_background;
        secondaryBackground = R.color.overlay_demo_app_gmail_secondary_background;
        primaryText = R.color.overlay_demo_app_gmail_primary_text;
        secondaryText = R.color.overlay_demo_app_gmail_primary_text;
        break;
      default:
        throw new IllegalArgumentException("Unknown demo app view ID " + view.getId());
    }

    Activity activity = requireActivity();
    mOverlayData =
        new OverlayDataImpl(
            ContextCompat.getColor(activity, primaryBackground),
            ContextCompat.getColor(activity, secondaryBackground),
            ContextCompat.getColor(activity, primaryText),
            ContextCompat.getColor(activity, primaryText),
            ContextCompat.getColor(activity, secondaryText));

    mSelectedKeyboardView.setThemeOverlay(mOverlayData);
  }

  @Override
  protected int getMarketSearchTitle() {
    return R.string.search_market_for_keyboard_addons;
  }

  @Nullable
  @Override
  protected String getMarketSearchKeyword() {
    return "theme";
  }

  @Override
  protected void applyAddOnToDemoKeyboardView(
      @NonNull KeyboardTheme addOn, @NonNull DemoKeyboardView demoKeyboardView) {
    demoKeyboardView.setKeyboardTheme(addOn);
    mSelectedKeyboardView.setThemeOverlay(mOverlayData);
    KeyboardDefinition defaultKeyboard =
        NskApplicationBase.getKeyboardFactory(requireContext())
            .getEnabledAddOn()
            .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    defaultKeyboard.loadKeyboard(demoKeyboardView.getThemedKeyboardDimens());
    demoKeyboardView.setKeyboard(defaultKeyboard, null, null);

    if (demoKeyboardView == mSelectedKeyboardView) {
      updateCustomizeRowSummary(addOn);
    }
  }
}
