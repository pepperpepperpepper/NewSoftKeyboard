package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PackKeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksRepository;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardRuntimeLoader;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.OnKeyboardActionListener;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.PackThemeOverride;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyCode;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.DirectoryPackSource;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifestJson;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardWriter;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlWriter;

public class CustomKeyboardEditorFragment extends Fragment {
  private static final String TAG = "CustomKeyboardEditor";

  private static final String STATE_TEST_TYPING_ENABLED = "testTypingEnabled";
  private static final String STATE_TEST_TYPING_BUFFER = "testTypingBuffer";

  public static final String ARG_PACK_ID = "packId";
  public static final String ARG_KEYBOARD_ENTRY_ID = "keyboardEntryId";
  public static final String ARG_KEYBOARD_PATH = "keyboardPath";

  private static final String ATTR_CODES = "android:codes";
  private static final String ATTR_KEY_LABEL = "android:keyLabel";
  private static final String ATTR_KEY_WIDTH = "android:keyWidth";
  private static final String ATTR_POPUP_CHARACTERS = "android:popupCharacters";
  private static final String ATTR_POPUP_KEYBOARD = "android:popupKeyboard";
  private static final String ATTR_LONG_PRESS_CODE = "ask:longPressCode";
  private static final String ATTR_HINT_LABEL = "ask:hintLabel";
  private static final String ATTR_EXTRA_KEY_DATA = "ask:extra_key_data";
  private static final String ATTR_ROW_EDGE_FLAGS = "android:rowEdgeFlags";
  private static final String ATTR_KEYBOARD_MODE = "android:keyboardMode";

  private final PackKeyboardRuntimeLoader runtimeLoader = new PackKeyboardRuntimeLoader();

  private KeyboardView keyboardView;
  private TextView instructionsView;
  private SwitchCompat testTypingSwitch;
  private View testTypingContainer;
  private TextView testTypingBufferView;
  private Button testTypingClearButton;
  private TextView validationWarningsView;
  private TextView statusView;
  private Button themeButton;

  private int touchSlopPx;
  private int editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
  private float editorDownX;
  private float editorDownY;

  @Nullable private InstalledKeyboardPack pack;
  @Nullable private PackEntry keyboardEntry;
  @Nullable private PackEntry themeEntry;
  @Nullable private File keyboardXmlFile;
  @Nullable private KeyboardModel keyboardModel;
  @Nullable private PackKeyboardDefinition keyboardDefinition;

  @NonNull private final StringBuilder testTypingBuffer = new StringBuilder();
  private boolean testTypingEnabled;

  @NonNull
  private final EditorKeyboardActionListener editorKeyboardActionListener =
      new EditorKeyboardActionListener();

  @NonNull
  private final TestTypingKeyboardActionListener testTypingKeyboardActionListener =
      new TestTypingKeyboardActionListener();

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.custom_keyboard_editor, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    keyboardView = view.findViewById(R.id.custom_keyboard_editor_view);
    instructionsView = view.findViewById(R.id.custom_keyboard_editor_instructions);
    testTypingSwitch = view.findViewById(R.id.custom_keyboard_editor_test_typing_switch);
    testTypingContainer = view.findViewById(R.id.custom_keyboard_editor_test_typing_container);
    testTypingBufferView = view.findViewById(R.id.custom_keyboard_editor_test_typing_buffer);
    testTypingClearButton = view.findViewById(R.id.custom_keyboard_editor_test_typing_clear_button);
    validationWarningsView = view.findViewById(R.id.custom_keyboard_editor_validation_warnings);
    statusView = view.findViewById(R.id.custom_keyboard_editor_status);
    themeButton = view.findViewById(R.id.custom_keyboard_editor_theme_button);
    themeButton.setOnClickListener(v -> showThemeActionsDialog());

    // CHECKSTYLE:OFF: RawGetKeyboardTheme
    keyboardView.setKeyboardTheme(
        NskApplicationBase.getKeyboardThemeFactory(requireContext()).getEnabledAddOn());
    // CHECKSTYLE:ON: RawGetKeyboardTheme

    touchSlopPx = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
    keyboardView.setOnTouchListener(this::onKeyboardViewTouch);

    if (savedInstanceState != null) {
      testTypingEnabled = savedInstanceState.getBoolean(STATE_TEST_TYPING_ENABLED, false);
      String restoredBuffer = savedInstanceState.getString(STATE_TEST_TYPING_BUFFER, "");
      if (!TextUtils.isEmpty(restoredBuffer)) testTypingBuffer.append(restoredBuffer);
    }

    testTypingClearButton.setOnClickListener(
        v -> {
          testTypingBuffer.setLength(0);
          updateTestTypingBufferView();
        });

    testTypingSwitch.setOnCheckedChangeListener(null);
    testTypingSwitch.setChecked(testTypingEnabled);
    testTypingSwitch.setOnCheckedChangeListener(
        (CompoundButton buttonView, boolean isChecked) -> setTestTypingEnabled(isChecked));
    setTestTypingEnabled(testTypingEnabled);
  }

  private boolean onKeyboardViewTouch(@NonNull View v, @NonNull MotionEvent event) {
    if (testTypingEnabled) return false; // let KeyboardView handle actual typing simulation

    KeyboardDefinition currentKeyboard = keyboardView != null ? keyboardView.getKeyboard() : null;
    if (currentKeyboard == null || currentKeyboard.getKeys().isEmpty()) return false;

    final int action = event.getActionMasked();
    switch (action) {
      case MotionEvent.ACTION_DOWN -> {
        editorDownX = event.getX();
        editorDownY = event.getY();
        editorDownKeyIndex = findKeyIndexForEvent(currentKeyboard, event);
        return true;
      }
      case MotionEvent.ACTION_MOVE -> {
        if (editorDownKeyIndex == KeyboardViewBase.NOT_A_KEY) return true;
        float dx = event.getX() - editorDownX;
        float dy = event.getY() - editorDownY;
        if ((dx * dx + dy * dy) > (touchSlopPx * touchSlopPx)) {
          editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        }
        return true;
      }
      case MotionEvent.ACTION_UP -> {
        int keyIndex = editorDownKeyIndex;
        editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        if (keyIndex == KeyboardViewBase.NOT_A_KEY) return true;

        int upKeyIndex = findKeyIndexForEvent(currentKeyboard, event);
        if (upKeyIndex != keyIndex) return true;

        try {
          showKeyEditDialog(currentKeyboard.getKeys().get(keyIndex));
        } catch (IndexOutOfBoundsException ignored) {
          // ignore
        }
        return true;
      }
      case MotionEvent.ACTION_CANCEL -> {
        editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        return true;
      }
      default -> {
        return true;
      }
    }
  }

  private int findKeyIndexForEvent(
      @NonNull KeyboardDefinition keyboard, @NonNull MotionEvent event) {
    if (keyboardView == null) return KeyboardViewBase.NOT_A_KEY;
    int index =
        keyboardView
            .getKeyDetector()
            .getKeyIndexAndNearbyCodes((int) event.getX(), (int) event.getY(), null);
    if (index < 0 || index >= keyboard.getKeys().size()) return KeyboardViewBase.NOT_A_KEY;
    return index;
  }

  @Override
  public void onStart() {
    super.onStart();
    loadKeyboardOrShowError();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(STATE_TEST_TYPING_ENABLED, testTypingEnabled);
    outState.putString(STATE_TEST_TYPING_BUFFER, testTypingBuffer.toString());
  }

  private void setTestTypingEnabled(boolean enabled) {
    testTypingEnabled = enabled;
    if (instructionsView != null) {
      instructionsView.setText(
          enabled
              ? R.string.custom_keyboards_test_typing_instructions
              : R.string.custom_keyboards_edit_instructions);
    }
    if (testTypingContainer != null) {
      testTypingContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }
    updateTestTypingBufferView();

    if (keyboardView != null) {
      keyboardView.setOnKeyboardActionListener(
          enabled ? testTypingKeyboardActionListener : editorKeyboardActionListener);
    }
  }

  private void updateTestTypingBufferView() {
    if (testTypingBufferView == null) return;
    if (testTypingBuffer.length() == 0) {
      testTypingBufferView.setText("");
    } else {
      testTypingBufferView.setText(testTypingBuffer.toString());
    }
  }

  private void loadKeyboardOrShowError() {
    clearValidationWarnings();
    final Bundle args = getArguments();
    final String packId = args != null ? args.getString(ARG_PACK_ID) : null;
    final String keyboardEntryId = args != null ? args.getString(ARG_KEYBOARD_ENTRY_ID) : null;
    final String keyboardPath = args != null ? args.getString(ARG_KEYBOARD_PATH) : null;
    if (TextUtils.isEmpty(packId)
        || (TextUtils.isEmpty(keyboardEntryId) && TextUtils.isEmpty(keyboardPath))) {
      statusView.setText(R.string.custom_keyboards_error_missing_pack);
      return;
    }

    try {
      pack = new KeyboardPacksRepository(requireContext()).findInstalledPackById(packId);
    } catch (IOException e) {
      statusView.setText(e.getMessage());
      return;
    }
    if (pack == null) {
      statusView.setText(R.string.custom_keyboards_error_missing_pack);
      return;
    }

    if (!TextUtils.isEmpty(keyboardPath)) {
      final PackPath path;
      try {
        path = PackPath.parse(keyboardPath);
      } catch (IllegalArgumentException e) {
        statusView.setText(e.getMessage());
        return;
      }

      keyboardEntry = null;
      keyboardXmlFile = new File(pack.directory(), path.value());
      keyboardModel = readKeyboardModelOrNull(keyboardXmlFile);

      try {
        PackEntry entry = new PackEntry("path:" + path.value().replace('/', '_'), path);
        keyboardDefinition =
            new PackKeyboardDefinition(
                requireContext().getApplicationContext(),
                pack.manifest(),
                entry,
                Keyboard.KEYBOARD_ROW_MODE_NORMAL,
                new DirectoryPackSource(pack.directory()),
                false);
      } catch (IOException e) {
        statusView.setText(R.string.custom_keyboards_error_failed_load_keyboard);
        return;
      }
    } else {
      keyboardEntry = findKeyboardEntry(pack, keyboardEntryId);
      if (keyboardEntry == null) {
        statusView.setText(R.string.custom_keyboards_error_missing_pack);
        return;
      }

      keyboardXmlFile = new File(pack.directory(), keyboardEntry.path().value());
      keyboardModel = readKeyboardModelOrNull(keyboardXmlFile);

      keyboardDefinition =
          runtimeLoader.tryLoadKeyboardDefinition(
              requireContext(), pack, keyboardEntry.id(), Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      if (keyboardDefinition == null) {
        statusView.setText(R.string.custom_keyboards_error_failed_load_keyboard);
        return;
      }
    }

    keyboardDefinition.loadKeyboard(keyboardView.getThemedKeyboardDimens());
    keyboardView.setKeyboard(keyboardDefinition, null, null);
    requireActivity().setTitle(keyboardDefinition.getKeyboardName());
    statusView.setText("");
    applyPersistedThemeIfAny();
    updateValidationWarnings();
  }

  private void showThemeActionsDialog() {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack == null) return;

    final List<ActionItem> actions = new ArrayList<>();
    if (!currentPack.manifest().themes().isEmpty()) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_theme_action_apply),
              this::showThemeSelectDialog));
    }
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_theme_action_create), this::showCreateThemeDialog));
    if (themeEntry != null) {
      PackEntry selected = themeEntry;
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_theme_action_edit),
              () -> showEditThemeDialog(selected)));
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_theme_action_clear), this::clearThemeOverride));
    }

    CharSequence[] titles = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) titles[i] = actions.get(i).title;

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_theme_actions_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              ActionItem item = actions.get(which);
              item.action.run();
            })
        .show();
  }

  private void showThemeSelectDialog() {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack == null) return;
    final List<PackEntry> themes = currentPack.manifest().themes();

    CharSequence[] titles = new CharSequence[themes.size() + 1];
    titles[0] = getString(R.string.custom_keyboards_theme_select_none);
    for (int i = 0; i < themes.size(); i++) titles[i + 1] = themes.get(i).id();

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_theme_select_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              if (which == 0) {
                clearThemeOverride();
              } else {
                applyThemeEntry(themes.get(which - 1));
              }
            })
        .show();
  }

  private void clearThemeOverride() {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack != null) {
      CustomKeyboardPrefs.setSelectedThemeIdForPack(
          requireContext(), currentPack.manifest().id(), null);
    }
    themeEntry = null;
    keyboardView.setPackThemeOverride(null);
  }

  private void applyThemeEntry(@NonNull PackEntry entry) {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack == null) return;

    File themeFile = new File(currentPack.directory(), entry.path().value());
    try (InputStream in = new FileInputStream(themeFile)) {
      ThemeModel model = ThemeXmlParser.parse(in);
      keyboardView.setPackThemeOverride(new PackThemeOverride(currentPack.directory(), model));
      themeEntry = entry;
      CustomKeyboardPrefs.setSelectedThemeIdForPack(
          requireContext(), currentPack.manifest().id(), entry.id());
    } catch (IOException e) {
      statusView.setText(e.getMessage());
    }
  }

  private void applyPersistedThemeIfAny() {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack == null) return;
    String themeId =
        CustomKeyboardPrefs.getSelectedThemeIdForPack(
            requireContext(), currentPack.manifest().id());
    if (TextUtils.isEmpty(themeId)) {
      keyboardView.setPackThemeOverride(null);
      themeEntry = null;
      return;
    }
    PackEntry entry = findThemeEntry(currentPack, themeId);
    if (entry == null) {
      keyboardView.setPackThemeOverride(null);
      themeEntry = null;
      return;
    }
    applyThemeEntry(entry);
  }

  private void showCreateThemeDialog() {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack == null) return;

    View dialogView =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.custom_keyboard_edit_theme_dialog, null, false);
    EditText idEdit = dialogView.findViewById(R.id.edit_theme_id);
    EditText keyboardBgEdit = dialogView.findViewById(R.id.edit_theme_keyboard_background);
    keyboardBgEdit.setText("#202020");

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_theme_create_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (ignored, which) -> {
              String rawId = idEdit.getText() != null ? idEdit.getText().toString() : "";
              String themeId = sanitizeId(rawId);
              if (TextUtils.isEmpty(themeId)) return;
              if (containsThemeEntryId(currentPack.manifest(), themeId)) {
                statusView.setText("Theme id already exists: " + themeId);
                return;
              }
              try {
                PackEntry entry = createThemeEntry(themeId);
                ThemeModel model = readThemeModelFromDialog(dialogView);
                writeThemeFile(currentPack, entry, model);
                PackManifest updatedManifest =
                    new PackManifest(
                        currentPack.manifest().schemaVersion(),
                        currentPack.manifest().id(),
                        currentPack.manifest().name(),
                        currentPack.manifest().version() + 1,
                        currentPack.manifest().minCoreVersion(),
                        currentPack.manifest().keyboards(),
                        appendEntry(currentPack.manifest().themes(), entry));
                writePackManifest(currentPack.directory(), updatedManifest);
                pack = new InstalledKeyboardPack(currentPack.directory(), updatedManifest);
                applyThemeEntry(entry);
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showEditThemeDialog(@NonNull PackEntry entry) {
    InstalledKeyboardPack currentPack = pack;
    if (currentPack == null) return;

    File themeFile = new File(currentPack.directory(), entry.path().value());
    final ThemeModel existing;
    try (InputStream in = new FileInputStream(themeFile)) {
      existing = ThemeXmlParser.parse(in);
    } catch (IOException e) {
      statusView.setText(e.getMessage());
      return;
    }

    View dialogView =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.custom_keyboard_edit_theme_dialog, null, false);
    EditText idEdit = dialogView.findViewById(R.id.edit_theme_id);
    idEdit.setText(entry.id());
    idEdit.setEnabled(false);

    EditText keyboardBgEdit = dialogView.findViewById(R.id.edit_theme_keyboard_background);
    EditText keyBgEdit = dialogView.findViewById(R.id.edit_theme_key_background);
    EditText keyTextEdit = dialogView.findViewById(R.id.edit_theme_key_text_color);
    EditText hintTextEdit = dialogView.findViewById(R.id.edit_theme_hint_text_color);
    EditText nameTextEdit = dialogView.findViewById(R.id.edit_theme_name_text_color);
    EditText deleteIconEdit = dialogView.findViewById(R.id.edit_theme_delete_icon);
    EditText shiftIconEdit = dialogView.findViewById(R.id.edit_theme_shift_icon);
    EditText actionIconEdit = dialogView.findViewById(R.id.edit_theme_action_icon);
    EditText spaceIconEdit = dialogView.findViewById(R.id.edit_theme_space_icon);

    keyboardBgEdit.setText(formatColor(existing.colors().get("keyboardBackground")));
    keyBgEdit.setText(formatColor(existing.colors().get("keyBackground")));
    keyTextEdit.setText(formatColor(existing.colors().get("keyTextColor")));
    hintTextEdit.setText(formatColor(existing.colors().get("hintTextColor")));
    nameTextEdit.setText(formatColor(existing.colors().get("keyboardNameTextColor")));

    deleteIconEdit.setText(formatPath(existing.icons().get("delete")));
    shiftIconEdit.setText(formatPath(existing.icons().get("shift")));
    actionIconEdit.setText(formatPath(existing.icons().get("action")));
    spaceIconEdit.setText(formatPath(existing.icons().get("space")));

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_theme_edit_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (ignored, which) -> {
              try {
                ThemeModel model = readThemeModelFromDialog(dialogView, existing.rawAttributes());
                writeThemeFile(currentPack, entry, model);
                applyThemeEntry(entry);
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @NonNull
  private static PackEntry createThemeEntry(@NonNull String id) {
    PackPath path = PackPath.parse("themes/" + id + ".xml");
    return new PackEntry(id, path);
  }

  @NonNull
  private static String sanitizeId(@Nullable String raw) {
    if (raw == null) return "";
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return "";

    StringBuilder builder = new StringBuilder(trimmed.length());
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      boolean allowed =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '.'
              || c == '_'
              || c == '-';
      builder.append(allowed ? c : '_');
    }
    return builder.toString();
  }

  private static boolean containsThemeEntryId(@NonNull PackManifest manifest, @NonNull String id) {
    for (PackEntry entry : manifest.themes()) {
      if (entry.id().equals(id)) return true;
    }
    return false;
  }

  private static void writeThemeFile(
      @NonNull InstalledKeyboardPack pack, @NonNull PackEntry entry, @NonNull ThemeModel model)
      throws IOException {
    File xmlFile = new File(pack.directory(), entry.path().value());
    File parent = xmlFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed creating directory at " + parent);
    }
    try (OutputStream out = new FileOutputStream(xmlFile, false)) {
      ThemeXmlWriter.write(model, out);
    }
  }

  @NonNull
  private ThemeModel readThemeModelFromDialog(@NonNull View dialogView) throws IOException {
    return readThemeModelFromDialog(dialogView, Collections.emptyMap());
  }

  @NonNull
  private ThemeModel readThemeModelFromDialog(
      @NonNull View dialogView, @NonNull Map<String, String> rawAttributes) throws IOException {
    Map<String, Integer> colors = new HashMap<>();
    putOptionalColor(
        colors,
        "keyboardBackground",
        getEditTextValue(dialogView, R.id.edit_theme_keyboard_background));
    putOptionalColor(
        colors, "keyBackground", getEditTextValue(dialogView, R.id.edit_theme_key_background));
    putOptionalColor(
        colors, "keyTextColor", getEditTextValue(dialogView, R.id.edit_theme_key_text_color));
    putOptionalColor(
        colors, "hintTextColor", getEditTextValue(dialogView, R.id.edit_theme_hint_text_color));
    putOptionalColor(
        colors,
        "keyboardNameTextColor",
        getEditTextValue(dialogView, R.id.edit_theme_name_text_color));

    Map<String, PackPath> icons = new HashMap<>();
    putOptionalIcon(icons, "delete", getEditTextValue(dialogView, R.id.edit_theme_delete_icon));
    putOptionalIcon(icons, "shift", getEditTextValue(dialogView, R.id.edit_theme_shift_icon));
    putOptionalIcon(icons, "action", getEditTextValue(dialogView, R.id.edit_theme_action_icon));
    putOptionalIcon(icons, "space", getEditTextValue(dialogView, R.id.edit_theme_space_icon));

    return new ThemeModel(colors, icons, rawAttributes);
  }

  private static void putOptionalColor(
      @NonNull Map<String, Integer> colors, @NonNull String key, @Nullable String raw)
      throws IOException {
    if (raw == null) return;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return;
    try {
      colors.put(key, Color.parseColor(trimmed));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid color for " + key + ": " + trimmed);
    }
  }

  private static void putOptionalIcon(
      @NonNull Map<String, PackPath> icons, @NonNull String key, @Nullable String raw)
      throws IOException {
    if (raw == null) return;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return;
    try {
      icons.put(key, PackPath.parse(trimmed));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid icon path for " + key + ": " + trimmed);
    }
  }

  @Nullable
  private static String getEditTextValue(@NonNull View dialogView, int id) {
    EditText edit = dialogView.findViewById(id);
    return edit.getText() != null ? edit.getText().toString() : null;
  }

  @Nullable
  private static String formatColor(@Nullable Integer argb) {
    if (argb == null) return null;
    int alpha = (argb >>> 24) & 0xFF;
    if (alpha == 0xFF) {
      return String.format("#%06X", argb & 0x00FF_FFFF);
    }
    return String.format("#%08X", argb);
  }

  @Nullable
  private static String formatPath(@Nullable PackPath path) {
    return path != null ? path.value() : null;
  }

  @Nullable
  private static PackEntry findKeyboardEntry(
      @NonNull InstalledKeyboardPack pack, @NonNull String entryId) {
    for (PackEntry entry : pack.manifest().keyboards()) {
      if (entry.id().equals(entryId)) return entry;
    }
    return null;
  }

  @Nullable
  private static PackEntry findThemeEntry(@NonNull InstalledKeyboardPack pack, @NonNull String id) {
    for (PackEntry entry : pack.manifest().themes()) {
      if (entry.id().equals(id)) return entry;
    }
    return null;
  }

  @Nullable
  private static KeyboardModel readKeyboardModelOrNull(@Nullable File keyboardXmlFile) {
    if (keyboardXmlFile == null) return null;
    try (InputStream in = new FileInputStream(keyboardXmlFile)) {
      return AskXmlKeyboardParser.parse(in);
    } catch (IOException e) {
      return null;
    }
  }

  private void showKeyEditDialog(@NonNull Keyboard.Key key) {
    PackKeyboardDefinition currentKeyboard = keyboardDefinition;
    if (currentKeyboard == null) return;

    PackKeyboardDefinition.PackKeyLocation location = currentKeyboard.getPackKeyLocation(key);
    if (location == null) {
      statusView.setText(R.string.custom_keyboards_error_key_not_editable);
      return;
    }

    showKeyEditDialogAtLocation(location.rowIndex(), location.keyIndex());
  }

  private void showKeyEditDialogAtLocation(int rowIndex, int keyIndex) {
    KeyboardModel model = keyboardModel;
    File xmlFile = keyboardXmlFile;
    if (model == null || xmlFile == null) return;

    final KeySpec currentSpec;
    try {
      currentSpec = model.rows().get(rowIndex).keys().get(keyIndex);
    } catch (IndexOutOfBoundsException e) {
      statusView.setText(R.string.custom_keyboards_error_key_not_found);
      return;
    }

    final Map<String, String> attrs = currentSpec.rawAttributes();

    View dialogView =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.custom_keyboard_edit_key_dialog, null, false);
    EditText labelEdit = dialogView.findViewById(R.id.edit_key_label);
    EditText codesEdit = dialogView.findViewById(R.id.edit_key_codes);
    EditText longPressEdit = dialogView.findViewById(R.id.edit_key_long_press_code);
    EditText widthEdit = dialogView.findViewById(R.id.edit_key_width);
    EditText hintLabelEdit = dialogView.findViewById(R.id.edit_key_hint_label);
    EditText popupCharsEdit = dialogView.findViewById(R.id.edit_key_popup_characters);
    EditText popupKeyboardEdit = dialogView.findViewById(R.id.edit_key_popup_keyboard);
    EditText extraDataEdit = dialogView.findViewById(R.id.edit_key_extra_data);
    Button layoutActionsButton = dialogView.findViewById(R.id.edit_key_layout_actions);

    labelEdit.setText(attrs.get(ATTR_KEY_LABEL));
    codesEdit.setText(attrs.get(ATTR_CODES));
    longPressEdit.setText(attrs.get(ATTR_LONG_PRESS_CODE));
    widthEdit.setText(attrs.get(ATTR_KEY_WIDTH));
    hintLabelEdit.setText(attrs.get(ATTR_HINT_LABEL));
    popupCharsEdit.setText(attrs.get(ATTR_POPUP_CHARACTERS));
    popupKeyboardEdit.setText(attrs.get(ATTR_POPUP_KEYBOARD));
    extraDataEdit.setText(attrs.get(ATTR_EXTRA_KEY_DATA));

    AlertDialog dialog =
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.custom_keyboards_edit_key_dialog_title)
            .setView(dialogView)
            .setPositiveButton(
                android.R.string.ok,
                (ignored, which) -> {
                  try {
                    KeyboardModel updated =
                        updateKeySpec(
                            model,
                            rowIndex,
                            keyIndex,
                            labelEdit.getText(),
                            codesEdit.getText(),
                            longPressEdit.getText(),
                            widthEdit.getText(),
                            hintLabelEdit.getText(),
                            popupCharsEdit.getText(),
                            popupKeyboardEdit.getText(),
                            extraDataEdit.getText());
                    applyModelUpdate(xmlFile, updated);
                  } catch (IOException e) {
                    statusView.setText(e.getMessage());
                  }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create();

    layoutActionsButton.setOnClickListener(
        v -> {
          dialog.dismiss();
          showLayoutActionsDialog(rowIndex, keyIndex);
        });

    dialog.show();
  }

  private void reloadKeyboardView() {
    PackKeyboardDefinition currentKeyboard = keyboardDefinition;
    if (currentKeyboard == null) return;
    currentKeyboard.loadKeyboard(keyboardView.getThemedKeyboardDimens());
    keyboardView.setKeyboard(currentKeyboard, null, null);
  }

  private void applyModelUpdate(@NonNull File xmlFile, @NonNull KeyboardModel updated)
      throws IOException {
    writeKeyboardModel(xmlFile, updated);
    keyboardModel = updated;
    CustomKeyboardPrefs.bumpGeneration(requireContext());
    reloadKeyboardView();
    updateValidationWarnings();
    statusView.setText("");
  }

  private void showLayoutActionsDialog(int rowIndex, int keyIndex) {
    KeyboardModel model = keyboardModel;
    File xmlFile = keyboardXmlFile;
    if (model == null || xmlFile == null) return;

    final List<ActionItem> actions = new ArrayList<>();
    KeyboardRow row = model.rows().get(rowIndex);
    final KeySpec currentKeySpec = row.keys().get(keyIndex);
    final Map<String, String> currentKeyAttrs = currentKeySpec.rawAttributes();
    if (keyIndex > 0) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_layout_action_move_key_left),
              () -> {
                try {
                  applyModelUpdate(xmlFile, moveKey(model, rowIndex, keyIndex, -1));
                } catch (IOException e) {
                  statusView.setText(e.getMessage());
                }
              }));
    }
    if (keyIndex < row.keys().size() - 1) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_layout_action_move_key_right),
              () -> {
                try {
                  applyModelUpdate(xmlFile, moveKey(model, rowIndex, keyIndex, +1));
                } catch (IOException e) {
                  statusView.setText(e.getMessage());
                }
              }));
    }
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_layout_action_insert_key_after),
            () -> {
              try {
                applyModelUpdate(xmlFile, insertKeyAfter(model, rowIndex, keyIndex));
                showKeyEditDialogAtLocation(rowIndex, keyIndex + 1);
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            }));
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_layout_action_delete_key),
            () -> confirmDeleteKey(model, xmlFile, rowIndex, keyIndex)));

    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_layout_action_create_popup_keyboard),
            () -> {
              InstalledKeyboardPack currentPack = pack;
              if (currentPack == null) return;
              try {
                String popupPath = createPopupKeyboardFile(currentPack);
                KeyboardModel updated =
                    updateKeySpec(
                        model,
                        rowIndex,
                        keyIndex,
                        currentKeyAttrs.get(ATTR_KEY_LABEL),
                        currentKeyAttrs.get(ATTR_CODES),
                        currentKeyAttrs.get(ATTR_LONG_PRESS_CODE),
                        currentKeyAttrs.get(ATTR_KEY_WIDTH),
                        currentKeyAttrs.get(ATTR_HINT_LABEL),
                        currentKeyAttrs.get(ATTR_POPUP_CHARACTERS),
                        popupPath,
                        currentKeyAttrs.get(ATTR_EXTRA_KEY_DATA));
                applyModelUpdate(xmlFile, updated);
                openEditorForKeyboardPath(currentPack.manifest().id(), popupPath);
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            }));

    final String existingPopupKeyboardRaw = currentKeyAttrs.get(ATTR_POPUP_KEYBOARD);
    final String existingPopupKeyboard =
        !TextUtils.isEmpty(existingPopupKeyboardRaw) ? existingPopupKeyboardRaw.trim() : null;
    if (!TextUtils.isEmpty(existingPopupKeyboard) && !existingPopupKeyboard.startsWith("@")) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_layout_action_edit_popup_keyboard),
              () -> {
                InstalledKeyboardPack currentPack = pack;
                if (currentPack == null) return;
                openEditorForKeyboardPath(currentPack.manifest().id(), existingPopupKeyboard);
              }));
    }

    if (pack != null && keyboardEntry != null) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_layout_action_create_symbols_layer),
              () -> createOrLinkSymbolsLayer(model, xmlFile, rowIndex, keyIndex, currentKeyAttrs)));
    }

    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_layout_action_edit_row),
            () -> showRowEditDialog(rowIndex)));
    if (rowIndex > 0) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_layout_action_move_row_up),
              () -> {
                try {
                  applyModelUpdate(xmlFile, moveRow(model, rowIndex, -1));
                } catch (IOException e) {
                  statusView.setText(e.getMessage());
                }
              }));
    }
    if (rowIndex < model.rows().size() - 1) {
      actions.add(
          new ActionItem(
              getString(R.string.custom_keyboards_layout_action_move_row_down),
              () -> {
                try {
                  applyModelUpdate(xmlFile, moveRow(model, rowIndex, +1));
                } catch (IOException e) {
                  statusView.setText(e.getMessage());
                }
              }));
    }
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_layout_action_insert_row_below),
            () -> {
              try {
                applyModelUpdate(xmlFile, insertRowBelow(model, rowIndex));
                showKeyEditDialogAtLocation(rowIndex + 1, 0);
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            }));
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_layout_action_delete_row),
            () -> confirmDeleteRow(model, xmlFile, rowIndex)));

    CharSequence[] titles = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) titles[i] = actions.get(i).title;

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_layout_actions_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              ActionItem item = actions.get(which);
              item.action.run();
            })
        .show();
  }

  @NonNull
  private static String createPopupKeyboardFile(@NonNull InstalledKeyboardPack pack)
      throws IOException {
    File keyboardsDir = new File(pack.directory(), "keyboards");
    if (!keyboardsDir.exists() && !keyboardsDir.mkdirs()) {
      throw new IOException("Failed creating keyboards directory at " + keyboardsDir);
    }

    String fileName = "popup_" + System.currentTimeMillis() + ".xml";
    File xmlFile = new File(keyboardsDir, fileName);
    int i = 0;
    while (xmlFile.exists() && i < 100) {
      i++;
      xmlFile = new File(keyboardsDir, "popup_" + System.currentTimeMillis() + "_" + i + ".xml");
    }

    Map<String, String> keyboardAttrs = new HashMap<>();
    keyboardAttrs.put("xmlns:android", "http://schemas.android.com/apk/res/android");
    keyboardAttrs.put("android:keyWidth", "10%p");
    keyboardAttrs.put("android:keyHeight", "-1");
    KeyboardModel model =
        new KeyboardModel(
            keyboardAttrs,
            Collections.singletonList(
                new KeyboardRow(Collections.singletonList(createPlaceholderKeySpec()))));
    writeKeyboardModel(xmlFile, model);
    return "keyboards/" + xmlFile.getName();
  }

  private void openEditorForKeyboardPath(@NonNull String packId, @NonNull String keyboardPath) {
    NavController navController = Navigation.findNavController(requireView());
    Bundle args = new Bundle();
    args.putString(ARG_PACK_ID, packId);
    args.putString(ARG_KEYBOARD_ENTRY_ID, "");
    args.putString(ARG_KEYBOARD_PATH, keyboardPath);
    navController.navigate(R.id.customKeyboardEditorFragment, args);
  }

  private void openEditorForKeyboardEntry(@NonNull String packId, @NonNull String entryId) {
    NavController navController = Navigation.findNavController(requireView());
    Bundle args = new Bundle();
    args.putString(ARG_PACK_ID, packId);
    args.putString(ARG_KEYBOARD_ENTRY_ID, entryId);
    args.putString(ARG_KEYBOARD_PATH, "");
    navController.navigate(R.id.customKeyboardEditorFragment, args);
  }

  private void switchToKeyboardEntry(@NonNull InstalledKeyboardPack pack, @NonNull String entryId) {
    PackEntry entry = findKeyboardEntry(pack, entryId);
    if (entry == null) {
      statusView.setText("Switch target not found: " + entryId);
      return;
    }

    PackKeyboardDefinition loaded =
        runtimeLoader.tryLoadKeyboardDefinition(
            requireContext(), pack, entry.id(), Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    if (loaded == null) {
      statusView.setText(R.string.custom_keyboards_error_failed_load_keyboard);
      return;
    }

    keyboardEntry = entry;
    keyboardXmlFile = new File(pack.directory(), entry.path().value());
    keyboardModel = readKeyboardModelOrNull(keyboardXmlFile);
    keyboardDefinition = loaded;

    keyboardDefinition.loadKeyboard(keyboardView.getThemedKeyboardDimens());
    keyboardView.setKeyboard(keyboardDefinition, null, null);
    requireActivity().setTitle(keyboardDefinition.getKeyboardName());
    updateValidationWarnings();
  }

  private void createOrLinkSymbolsLayer(
      @NonNull KeyboardModel model,
      @NonNull File xmlFile,
      int rowIndex,
      int keyIndex,
      @NonNull Map<String, String> currentKeyAttrs) {
    InstalledKeyboardPack currentPack = pack;
    PackKeyboardDefinition currentKeyboard = keyboardDefinition;
    if (currentPack == null || currentKeyboard == null) return;

    PackManifest manifest = currentPack.manifest();
    PackEntry symbolsEntry = findKeyboardEntry(currentPack, "symbols");
    if (symbolsEntry == null) {
      try {
        symbolsEntry = createSymbolsEntry(manifest);
        writeSymbolsKeyboardFile(currentPack, symbolsEntry, currentKeyboard.getKeyboardId());
        PackManifest updatedManifest =
            new PackManifest(
                manifest.schemaVersion(),
                manifest.id(),
                manifest.name(),
                manifest.version() + 1,
                manifest.minCoreVersion(),
                appendEntry(manifest.keyboards(), symbolsEntry),
                manifest.themes());
        writePackManifest(currentPack.directory(), updatedManifest);
      } catch (IOException e) {
        statusView.setText(e.getMessage());
        return;
      }
    }

    String targetKeyboardId = "pack::" + manifest.id() + "::" + symbolsEntry.id();

    try {
      KeyboardModel updated =
          updateKeySpec(
              model,
              rowIndex,
              keyIndex,
              "?123",
              Integer.toString(KeyCodes.CUSTOM_KEYBOARD_SWITCH),
              null,
              currentKeyAttrs.get(ATTR_KEY_WIDTH),
              null,
              null,
              null,
              targetKeyboardId);
      applyModelUpdate(xmlFile, updated);
      openEditorForKeyboardEntry(manifest.id(), symbolsEntry.id());
    } catch (IOException e) {
      statusView.setText(e.getMessage());
    }
  }

  @NonNull
  private static PackEntry createSymbolsEntry(@NonNull PackManifest manifest) {
    String entryId = "symbols";
    int suffix = 2;
    while (containsKeyboardEntryId(manifest, entryId)) {
      entryId = "symbols_" + suffix;
      suffix++;
    }
    PackPath path = PackPath.parse("keyboards/" + entryId + ".xml");
    return new PackEntry(entryId, path);
  }

  private static boolean containsKeyboardEntryId(
      @NonNull PackManifest manifest, @NonNull String id) {
    for (PackEntry entry : manifest.keyboards()) {
      if (entry.id().equals(id)) return true;
    }
    return false;
  }

  @NonNull
  private static List<PackEntry> appendEntry(
      @NonNull List<PackEntry> entries, @NonNull PackEntry entry) {
    List<PackEntry> updated = new ArrayList<>(entries);
    updated.add(entry);
    return Collections.unmodifiableList(updated);
  }

  private static void writePackManifest(@NonNull File packDir, @NonNull PackManifest manifest)
      throws IOException {
    File manifestFile = new File(packDir, "manifest.json");
    try (OutputStream out = new FileOutputStream(manifestFile, false)) {
      PackManifestJson.write(manifest, out);
    }
  }

  private static void writeSymbolsKeyboardFile(
      @NonNull InstalledKeyboardPack pack, @NonNull PackEntry entry, @NonNull String backTargetId)
      throws IOException {
    File xmlFile = new File(pack.directory(), entry.path().value());
    File parent = xmlFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed creating directory at " + parent);
    }

    Map<String, String> keyboardAttrs = new HashMap<>();
    keyboardAttrs.put("xmlns:android", "http://schemas.android.com/apk/res/android");
    keyboardAttrs.put("xmlns:ask", "http://schemas.android.com/apk/res-auto");
    keyboardAttrs.put("android:keyWidth", "10%p");
    keyboardAttrs.put("android:keyHeight", "-1");

    List<KeySpec> row1 = new ArrayList<>();
    row1.add(createKeySpec("1", "49", "left", null));
    row1.add(createKeySpec("2", "50", null, null));
    row1.add(createKeySpec("3", "51", null, null));
    row1.add(createKeySpec("4", "52", null, null));
    row1.add(createKeySpec("5", "53", null, null));
    row1.add(createKeySpec("6", "54", null, null));
    row1.add(createKeySpec("7", "55", null, null));
    row1.add(createKeySpec("8", "56", null, null));
    row1.add(createKeySpec("9", "57", null, null));
    row1.add(createKeySpec("0", "48", "right", null));

    List<KeySpec> row2 = new ArrayList<>();
    row2.add(createKeySpec("!", "33", "left", null));
    row2.add(createKeySpec("@", "64", null, null));
    row2.add(createKeySpec("#", "35", null, null));
    row2.add(createKeySpec("$", "36", null, null));
    row2.add(createKeySpec("%", "37", null, null));
    row2.add(createKeySpec("^", "94", null, null));
    row2.add(createKeySpec("&", "38", null, null));
    row2.add(createKeySpec("*", "42", null, null));
    row2.add(createKeySpec("(", "40", null, null));
    row2.add(createKeySpec(")", "41", "right", null));

    List<KeySpec> row3 = new ArrayList<>();
    row3.add(
        createKeySpec(
            "ABC",
            Integer.toString(KeyCodes.CUSTOM_KEYBOARD_SWITCH),
            "left",
            backTargetId,
            "20%p"));
    row3.add(createKeySpec("-", "45", null, null));
    row3.add(createKeySpec("_", "95", null, null));
    row3.add(createKeySpec("/", "47", null, null));
    row3.add(createKeySpec(":", "58", null, null));
    row3.add(createKeySpec(";", "59", null, null));
    row3.add(createKeySpec("\"", "34", null, null));
    row3.add(
        createKeySpec(
            "⌫",
            "-5",
            "right",
            null,
            "20%p",
            Collections.singletonMap("android:isRepeatable", "true")));

    KeyboardModel modelToWrite =
        new KeyboardModel(
            keyboardAttrs,
            Arrays.asList(new KeyboardRow(row1), new KeyboardRow(row2), new KeyboardRow(row3)));
    writeKeyboardModel(xmlFile, modelToWrite);
  }

  @NonNull
  private static KeySpec createKeySpec(
      @NonNull String label,
      @NonNull String codes,
      @Nullable String edgeFlags,
      @Nullable String extraData) {
    return createKeySpec(label, codes, edgeFlags, extraData, null);
  }

  @NonNull
  private static KeySpec createKeySpec(
      @NonNull String label,
      @NonNull String codes,
      @Nullable String edgeFlags,
      @Nullable String extraData,
      @Nullable String keyWidth) {
    return createKeySpec(label, codes, edgeFlags, extraData, keyWidth, Collections.emptyMap());
  }

  @NonNull
  private static KeySpec createKeySpec(
      @NonNull String label,
      @NonNull String codes,
      @Nullable String edgeFlags,
      @Nullable String extraData,
      @Nullable String keyWidth,
      @NonNull Map<String, String> additionalAttrs) {
    Map<String, String> attrs = new HashMap<>();
    attrs.put(ATTR_KEY_LABEL, label);
    attrs.put(ATTR_CODES, codes);
    if (!TextUtils.isEmpty(edgeFlags)) {
      attrs.put("android:keyEdgeFlags", edgeFlags);
    }
    if (!TextUtils.isEmpty(keyWidth)) {
      attrs.put(ATTR_KEY_WIDTH, keyWidth);
    }
    if (!TextUtils.isEmpty(extraData)) {
      attrs.put(ATTR_EXTRA_KEY_DATA, extraData);
    }
    attrs.putAll(additionalAttrs);
    List<KeyCode> parsedCodes = parseCodes(codes);
    return new KeySpec(parsedCodes, label, null, attrs);
  }

  private void confirmDeleteKey(
      @NonNull KeyboardModel model, @NonNull File xmlFile, int rowIndex, int keyIndex) {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_confirm_delete_key_title)
        .setMessage(R.string.custom_keyboards_confirm_delete_key_message)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              try {
                applyModelUpdate(xmlFile, deleteKey(model, rowIndex, keyIndex));
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmDeleteRow(@NonNull KeyboardModel model, @NonNull File xmlFile, int rowIndex) {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_confirm_delete_row_title)
        .setMessage(R.string.custom_keyboards_confirm_delete_row_message)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              try {
                applyModelUpdate(xmlFile, deleteRow(model, rowIndex));
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showRowEditDialog(int rowIndex) {
    KeyboardModel model = keyboardModel;
    File xmlFile = keyboardXmlFile;
    if (model == null || xmlFile == null) return;

    final KeyboardRow row;
    try {
      row = model.rows().get(rowIndex);
    } catch (IndexOutOfBoundsException e) {
      statusView.setText(R.string.custom_keyboards_error_key_not_found);
      return;
    }

    View dialogView =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.custom_keyboard_edit_row_dialog, null, false);
    EditText keyWidthEdit = dialogView.findViewById(R.id.edit_row_key_width);
    EditText keyHeightEdit = dialogView.findViewById(R.id.edit_row_key_height);
    EditText horizontalGapEdit = dialogView.findViewById(R.id.edit_row_horizontal_gap);
    EditText verticalGapEdit = dialogView.findViewById(R.id.edit_row_vertical_gap);
    EditText edgeFlagsEdit = dialogView.findViewById(R.id.edit_row_edge_flags);
    EditText keyboardModeEdit = dialogView.findViewById(R.id.edit_row_keyboard_mode);

    final Map<String, String> attrs = row.rawRowAttributes();
    keyWidthEdit.setText(attrs.get(ATTR_KEY_WIDTH));
    keyHeightEdit.setText(attrs.get("android:keyHeight"));
    horizontalGapEdit.setText(attrs.get("android:horizontalGap"));
    verticalGapEdit.setText(attrs.get("android:verticalGap"));
    edgeFlagsEdit.setText(attrs.get(ATTR_ROW_EDGE_FLAGS));
    keyboardModeEdit.setText(attrs.get(ATTR_KEYBOARD_MODE));

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_edit_row_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              KeyboardModel updated =
                  updateRow(
                      model,
                      rowIndex,
                      keyWidthEdit.getText(),
                      keyHeightEdit.getText(),
                      horizontalGapEdit.getText(),
                      verticalGapEdit.getText(),
                      edgeFlagsEdit.getText(),
                      keyboardModeEdit.getText());
              try {
                applyModelUpdate(xmlFile, updated);
              } catch (IOException e) {
                statusView.setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static void writeKeyboardModel(@NonNull File xmlFile, @NonNull KeyboardModel model)
      throws IOException {
    try (OutputStream out = new FileOutputStream(xmlFile, false)) {
      AskXmlKeyboardWriter.write(model, out);
    }
  }

  @NonNull
  private static KeyboardModel updateKeySpec(
      @NonNull KeyboardModel model,
      int rowIndex,
      int keyIndex,
      @Nullable CharSequence label,
      @Nullable CharSequence codes,
      @Nullable CharSequence longPressCode,
      @Nullable CharSequence width,
      @Nullable CharSequence hintLabel,
      @Nullable CharSequence popupCharacters,
      @Nullable CharSequence popupKeyboard,
      @Nullable CharSequence extraKeyData) {
    KeyboardRow targetRow = model.rows().get(rowIndex);
    KeySpec targetKey = targetRow.keys().get(keyIndex);

    Map<String, String> nextAttrs = new HashMap<>(targetKey.rawAttributes());
    updateAttr(nextAttrs, ATTR_KEY_LABEL, label);
    updateAttr(nextAttrs, ATTR_CODES, codes);
    updateAttr(nextAttrs, ATTR_LONG_PRESS_CODE, longPressCode);
    updateAttr(nextAttrs, ATTR_KEY_WIDTH, width);
    updateAttr(nextAttrs, ATTR_HINT_LABEL, hintLabel);
    updateAttr(nextAttrs, ATTR_POPUP_CHARACTERS, popupCharacters);
    updateAttr(nextAttrs, ATTR_POPUP_KEYBOARD, popupKeyboard);
    updateAttr(nextAttrs, ATTR_EXTRA_KEY_DATA, extraKeyData);

    List<KeyCode> parsedCodes = parseCodes(nextAttrs.get(ATTR_CODES));
    String nextLabel = normalizeOptionalString(label);
    String nextPopupCharacters = normalizeOptionalString(popupCharacters);
    KeySpec updatedKey = new KeySpec(parsedCodes, nextLabel, nextPopupCharacters, nextAttrs);

    List<KeySpec> updatedKeys = new ArrayList<>(targetRow.keys());
    updatedKeys.set(keyIndex, updatedKey);
    KeyboardRow updatedRow = new KeyboardRow(targetRow.rawRowAttributes(), updatedKeys);

    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    Map<String, String> updatedKeyboardAttrs = model.rawKeyboardAttributes();
    final boolean requiresAskNamespace =
        nextAttrs.containsKey(ATTR_LONG_PRESS_CODE)
            || nextAttrs.containsKey(ATTR_HINT_LABEL)
            || nextAttrs.containsKey(ATTR_EXTRA_KEY_DATA);
    if (requiresAskNamespace && !updatedKeyboardAttrs.containsKey("xmlns:ask")) {
      updatedKeyboardAttrs = new HashMap<>(updatedKeyboardAttrs);
      updatedKeyboardAttrs.put("xmlns:ask", "http://schemas.android.com/apk/res-auto");
    }
    return new KeyboardModel(updatedKeyboardAttrs, updatedRows);
  }

  @NonNull
  private static KeyboardModel updateRow(
      @NonNull KeyboardModel model,
      int rowIndex,
      @Nullable CharSequence keyWidth,
      @Nullable CharSequence keyHeight,
      @Nullable CharSequence horizontalGap,
      @Nullable CharSequence verticalGap,
      @Nullable CharSequence rowEdgeFlags,
      @Nullable CharSequence keyboardMode) {
    KeyboardRow row = model.rows().get(rowIndex);

    Map<String, String> nextAttrs = new HashMap<>(row.rawRowAttributes());
    updateAttr(nextAttrs, ATTR_KEY_WIDTH, keyWidth);
    updateAttr(nextAttrs, "android:keyHeight", keyHeight);
    updateAttr(nextAttrs, "android:horizontalGap", horizontalGap);
    updateAttr(nextAttrs, "android:verticalGap", verticalGap);
    updateAttr(nextAttrs, ATTR_ROW_EDGE_FLAGS, rowEdgeFlags);
    updateAttr(nextAttrs, ATTR_KEYBOARD_MODE, keyboardMode);

    KeyboardRow updatedRow = new KeyboardRow(nextAttrs, row.keys());
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  private static KeyboardModel moveKey(
      @NonNull KeyboardModel model, int rowIndex, int keyIndex, int offset) {
    KeyboardRow row = model.rows().get(rowIndex);
    int newIndex = keyIndex + offset;
    if (newIndex < 0 || newIndex >= row.keys().size()) return model;

    List<KeySpec> keys = new ArrayList<>(row.keys());
    Collections.swap(keys, keyIndex, newIndex);
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), keys);
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  private static KeyboardModel insertKeyAfter(
      @NonNull KeyboardModel model, int rowIndex, int keyIndex) {
    KeyboardRow row = model.rows().get(rowIndex);
    List<KeySpec> keys = new ArrayList<>(row.keys());
    int insertIndex = Math.min(keys.size(), keyIndex + 1);
    keys.add(insertIndex, createPlaceholderKeySpec());
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), keys);
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  private static KeyboardModel deleteKey(@NonNull KeyboardModel model, int rowIndex, int keyIndex) {
    KeyboardRow row = model.rows().get(rowIndex);
    if (row.keys().size() <= 1) {
      return deleteRow(model, rowIndex);
    }
    List<KeySpec> keys = new ArrayList<>(row.keys());
    if (keyIndex < 0 || keyIndex >= keys.size()) return model;
    keys.remove(keyIndex);
    KeyboardRow updatedRow = new KeyboardRow(row.rawRowAttributes(), keys);
    List<KeyboardRow> updatedRows = new ArrayList<>(model.rows());
    updatedRows.set(rowIndex, updatedRow);
    return new KeyboardModel(model.rawKeyboardAttributes(), updatedRows);
  }

  @NonNull
  private static KeyboardModel moveRow(@NonNull KeyboardModel model, int rowIndex, int offset) {
    int newIndex = rowIndex + offset;
    if (newIndex < 0 || newIndex >= model.rows().size()) return model;
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    Collections.swap(rows, rowIndex, newIndex);
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  private static KeyboardModel insertRowBelow(@NonNull KeyboardModel model, int rowIndex) {
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    int insertIndex = Math.min(rows.size(), rowIndex + 1);
    rows.add(insertIndex, new KeyboardRow(Collections.singletonList(createPlaceholderKeySpec())));
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  private static KeyboardModel deleteRow(@NonNull KeyboardModel model, int rowIndex) {
    if (model.rows().size() <= 1) return model;
    List<KeyboardRow> rows = new ArrayList<>(model.rows());
    if (rowIndex < 0 || rowIndex >= rows.size()) return model;
    rows.remove(rowIndex);
    if (rows.isEmpty()) return model;
    return new KeyboardModel(model.rawKeyboardAttributes(), rows);
  }

  @NonNull
  private static KeySpec createPlaceholderKeySpec() {
    Map<String, String> attrs = new HashMap<>();
    attrs.put(ATTR_KEY_LABEL, "?");
    attrs.put(ATTR_CODES, "63");
    List<KeyCode> codes = Collections.singletonList(new KeyCode.Numeric(63));
    return new KeySpec(codes, "?", null, attrs);
  }

  private static final class ActionItem {
    @NonNull final String title;
    @NonNull final Runnable action;

    private ActionItem(@NonNull String title, @NonNull Runnable action) {
      this.title = title;
      this.action = action;
    }
  }

  private static void updateAttr(
      @NonNull Map<String, String> attrs, @NonNull String name, @Nullable CharSequence value) {
    String normalized = normalizeOptionalString(value);
    if (TextUtils.isEmpty(normalized)) {
      attrs.remove(name);
    } else {
      attrs.put(name, normalized);
    }
  }

  @Nullable
  private static String normalizeOptionalString(@Nullable CharSequence value) {
    if (value == null) return null;
    String asString = value.toString();
    return TextUtils.isEmpty(asString.trim()) ? null : asString;
  }

  @NonNull
  private static List<KeyCode> parseCodes(@Nullable String raw) {
    if (raw == null) return Collections.emptyList();
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return Collections.emptyList();
    if (trimmed.startsWith("@")) return Collections.singletonList(new KeyCode.Symbolic(trimmed));

    String[] parts = trimmed.split(",");
    List<KeyCode> result = new ArrayList<>(parts.length);
    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) continue;
      if (token.startsWith("@")) {
        result.add(new KeyCode.Symbolic(token));
        continue;
      }
      if (token.length() != 1) {
        try {
          result.add(new KeyCode.Numeric(Integer.parseInt(token)));
        } catch (NumberFormatException e) {
          result.add(new KeyCode.Symbolic(token));
        }
      } else {
        result.add(new KeyCode.Numeric((int) token.charAt(0)));
      }
    }
    return result;
  }

  private final class EditorKeyboardActionListener implements OnKeyboardActionListener {
    @Override
    public void onPress(int primaryCode) {}

    @Override
    public void onRelease(int primaryCode) {}

    @Override
    public void onKey(
        int primaryCode,
        Keyboard.Key key,
        int multiTapIndex,
        int[] nearByKeyCodes,
        boolean fromUI) {
      if (!fromUI) return;
      showKeyEditDialog(key);
    }

    @Override
    public void onMultiTapStarted() {}

    @Override
    public void onMultiTapEnded() {}

    @Override
    public void onText(Keyboard.Key key, CharSequence text) {
      showKeyEditDialog(key);
    }

    @Override
    public void onTyping(Keyboard.Key key, CharSequence text) {
      showKeyEditDialog(key);
    }

    @Override
    public void onCancel() {}

    @Override
    public void onSwipeLeft(boolean twoFingers) {}

    @Override
    public void onSwipeRight(boolean twoFingers) {}

    @Override
    public void onSwipeDown() {}

    @Override
    public void onSwipeUp() {}

    @Override
    public void onPinch() {}

    @Override
    public void onSeparate() {}

    @Override
    public void onFirstDownKey(int primaryCode) {}

    @Override
    public boolean onGestureTypingInputStart(
        int x, int y, wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey key, long eventTime) {
      return false;
    }

    @Override
    public void onGestureTypingInput(int x, int y, long eventTime) {}

    @Override
    public boolean onGestureTypingInputDone() {
      return false;
    }

    @Override
    public void onLongPressDone(@NonNull Keyboard.Key key) {}
  }

  private final class TestTypingKeyboardActionListener implements OnKeyboardActionListener {
    @Override
    public void onPress(int primaryCode) {}

    @Override
    public void onRelease(int primaryCode) {}

    @Override
    public void onKey(
        int primaryCode,
        Keyboard.Key key,
        int multiTapIndex,
        int[] nearByKeyCodes,
        boolean fromUI) {
      if (!fromUI) return;

      if (primaryCode == KeyCodes.DELETE) {
        deleteLastCodePointFromTestBuffer();
        return;
      }

      if (primaryCode == KeyCodes.CUSTOM_KEYBOARD_SWITCH) {
        String extraData = null;
        if (key instanceof KeyboardKey keyboardKey) {
          extraData = keyboardKey.getExtraKeyData();
        }

        InstalledKeyboardPack currentPack = pack;
        if (currentPack != null && trySwitchKeyboardFromExtraKeyData(currentPack, extraData)) {
          return;
        }
      }

      if (key.text != null && key.text.length() > 0) {
        appendToTestBuffer(key.text);
        return;
      }

      if (primaryCode > 0) {
        appendCodePointToTestBuffer(primaryCode);
        return;
      }

      if (primaryCode == 0 && key.label != null && key.label.length() == 1) {
        appendCodePointToTestBuffer(Character.codePointAt(key.label, 0));
      }
    }

    @Override
    public void onMultiTapStarted() {}

    @Override
    public void onMultiTapEnded() {}

    @Override
    public void onText(Keyboard.Key key, CharSequence text) {
      if (text == null || text.length() == 0) return;
      appendToTestBuffer(text);
    }

    @Override
    public void onTyping(Keyboard.Key key, CharSequence text) {
      if (text == null || text.length() == 0) return;
      appendToTestBuffer(text);
    }

    @Override
    public void onCancel() {}

    @Override
    public void onSwipeLeft(boolean twoFingers) {}

    @Override
    public void onSwipeRight(boolean twoFingers) {}

    @Override
    public void onSwipeDown() {}

    @Override
    public void onSwipeUp() {}

    @Override
    public void onPinch() {}

    @Override
    public void onSeparate() {}

    @Override
    public void onFirstDownKey(int primaryCode) {}

    @Override
    public boolean onGestureTypingInputStart(
        int x, int y, wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey key, long eventTime) {
      return false;
    }

    @Override
    public void onGestureTypingInput(int x, int y, long eventTime) {}

    @Override
    public boolean onGestureTypingInputDone() {
      return false;
    }

    @Override
    public void onLongPressDone(@NonNull Keyboard.Key key) {}
  }

  private void appendToTestBuffer(@NonNull CharSequence text) {
    testTypingBuffer.append(text);
    updateTestTypingBufferView();
  }

  private void appendCodePointToTestBuffer(int codePoint) {
    testTypingBuffer.appendCodePoint(codePoint);
    updateTestTypingBufferView();
  }

  private void deleteLastCodePointFromTestBuffer() {
    int length = testTypingBuffer.length();
    if (length == 0) return;
    int codePoint = Character.codePointBefore(testTypingBuffer, length);
    int deleteCount = Character.charCount(codePoint);
    testTypingBuffer.delete(Math.max(0, length - deleteCount), length);
    updateTestTypingBufferView();
  }

  private boolean trySwitchKeyboardFromExtraKeyData(
      @NonNull InstalledKeyboardPack currentPack, @Nullable String extraKeyData) {
    if (TextUtils.isEmpty(extraKeyData)) return false;
    String data = extraKeyData.trim();
    if (!data.startsWith("pack::")) return false;

    String[] parts = data.split("::");
    if (parts.length < 3) return false;
    String packId = parts[1];
    String entryId = parts[2];
    if (TextUtils.isEmpty(packId) || TextUtils.isEmpty(entryId)) return false;

    if (!packId.equals(currentPack.manifest().id())) return false;
    switchToKeyboardEntry(currentPack, entryId);
    return true;
  }

  private void clearValidationWarnings() {
    if (validationWarningsView == null) return;
    validationWarningsView.setText("");
    validationWarningsView.setVisibility(View.GONE);
  }

  private void updateValidationWarnings() {
    InstalledKeyboardPack currentPack = pack;
    KeyboardModel model = keyboardModel;
    File xmlFile = keyboardXmlFile;
    if (currentPack == null || model == null || xmlFile == null) {
      clearValidationWarnings();
      return;
    }

    List<String> warnings = new ArrayList<>();

    boolean hasDelete = false;
    boolean hasSpace = false;
    boolean hasEnter = false;
    boolean hasSymbolsSwitch = false;

    KeyboardDefinition displayedKeyboard = keyboardView != null ? keyboardView.getKeyboard() : null;
    if (displayedKeyboard != null) {
      for (Keyboard.Key key : displayedKeyboard.getKeys()) {
        int count = key.getCodesCount();
        for (int i = 0; i < count; i++) {
          int code = key.getCodeAtIndex(i, false);
          if (code == KeyCodes.DELETE) hasDelete = true;
          if (code == KeyCodes.SPACE) hasSpace = true;
          if (code == KeyCodes.ENTER) hasEnter = true;
          if (code == KeyCodes.MODE_SYMBOLS
              || code == KeyCodes.KEYBOARD_MODE_CHANGE
              || code == KeyCodes.CUSTOM_KEYBOARD_SWITCH) {
            hasSymbolsSwitch = true;
          }
        }
      }
    } else {
      for (KeyboardRow row : model.rows()) {
        for (KeySpec keySpec : row.keys()) {
          for (KeyCode code : keySpec.codes()) {
            Integer numeric = code.asNumeric();
            if (numeric == null) continue;
            if (numeric == KeyCodes.DELETE) hasDelete = true;
            if (numeric == KeyCodes.SPACE) hasSpace = true;
            if (numeric == KeyCodes.ENTER) hasEnter = true;
            if (numeric == KeyCodes.MODE_SYMBOLS
                || numeric == KeyCodes.KEYBOARD_MODE_CHANGE
                || numeric == KeyCodes.CUSTOM_KEYBOARD_SWITCH) {
              hasSymbolsSwitch = true;
            }
          }
        }
      }
    }

    if (!hasDelete) warnings.add(getString(R.string.custom_keyboards_validation_missing_delete));
    if (!hasSpace) warnings.add(getString(R.string.custom_keyboards_validation_missing_space));
    if (!hasEnter) warnings.add(getString(R.string.custom_keyboards_validation_missing_enter));
    if (!hasSymbolsSwitch) {
      warnings.add(getString(R.string.custom_keyboards_validation_missing_symbols));
    }

    warnings.addAll(validatePopupKeyboards(currentPack, model));
    warnings.addAll(validateRowWidthPercents(model));
    warnings.addAll(validateTextAndCodepoints(model));

    if (warnings.isEmpty()) {
      clearValidationWarnings();
      return;
    }

    StringBuilder builder = new StringBuilder();
    builder.append(getString(R.string.custom_keyboards_validation_warnings_title));
    for (String warning : warnings) {
      builder.append("\n• ").append(warning);
    }

    validationWarningsView.setText(builder.toString());
    validationWarningsView.setVisibility(View.VISIBLE);
  }

  @NonNull
  private static List<String> validatePopupKeyboards(
      @NonNull InstalledKeyboardPack currentPack, @NonNull KeyboardModel model) {
    Set<String> popupPaths = new HashSet<>();
    for (KeyboardRow row : model.rows()) {
      for (KeySpec keySpec : row.keys()) {
        String popupKeyboard = keySpec.rawAttributes().get(ATTR_POPUP_KEYBOARD);
        if (popupKeyboard == null) continue;
        String trimmed = popupKeyboard.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("@")) continue;
        popupPaths.add(trimmed);
      }
    }

    if (popupPaths.isEmpty()) return Collections.emptyList();

    List<String> warnings = new ArrayList<>();
    for (String popupPath : popupPaths) {
      try {
        PackPath.parse(popupPath);
      } catch (IllegalArgumentException e) {
        warnings.add("Popup keyboard path is invalid: " + popupPath);
        continue;
      }

      File file = new File(currentPack.directory(), popupPath);
      if (!file.exists()) {
        warnings.add("Popup keyboard file not found: " + popupPath);
        continue;
      }
      try (InputStream in = new FileInputStream(file)) {
        AskXmlKeyboardParser.parse(in);
      } catch (IOException e) {
        warnings.add("Popup keyboard failed to parse: " + popupPath);
      }
    }

    return warnings;
  }

  @NonNull
  private static List<String> validateRowWidthPercents(@NonNull KeyboardModel model) {
    Float keyboardDefaultWidthPercent =
        parsePercentOrNull(model.rawKeyboardAttributes().get(ATTR_KEY_WIDTH));
    if (keyboardDefaultWidthPercent == null) return Collections.emptyList();

    List<String> warnings = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++) {
      KeyboardRow row = model.rows().get(rowIndex);
      Float rowDefaultWidthPercent = parsePercentOrNull(row.rawRowAttributes().get(ATTR_KEY_WIDTH));
      if (rowDefaultWidthPercent == null) rowDefaultWidthPercent = keyboardDefaultWidthPercent;

      float sum = 0f;
      boolean valid = true;
      for (KeySpec keySpec : row.keys()) {
        Float keyWidthPercent = parsePercentOrNull(keySpec.rawAttributes().get(ATTR_KEY_WIDTH));
        if (keyWidthPercent == null) keyWidthPercent = rowDefaultWidthPercent;
        if (keyWidthPercent == null) {
          valid = false;
          break;
        }
        sum += keyWidthPercent;
      }

      if (!valid) continue;
      if (sum > 100.5f) {
        warnings.add("Row " + (rowIndex + 1) + " total keyWidth is " + sum + "% (> 100%).");
      }
    }
    return warnings;
  }

  @Nullable
  private static Float parsePercentOrNull(@Nullable String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    if (!trimmed.endsWith("%p")) return null;
    String number = trimmed.substring(0, trimmed.length() - 2).trim();
    try {
      return Float.parseFloat(number);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @NonNull
  private static List<String> validateTextAndCodepoints(@NonNull KeyboardModel model) {
    int unpairedSurrogates = 0;
    int invalidCodePoints = 0;
    List<String> samples = new ArrayList<>(4);

    for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++) {
      KeyboardRow row = model.rows().get(rowIndex);
      for (int keyIndex = 0; keyIndex < row.keys().size(); keyIndex++) {
        KeySpec keySpec = row.keys().get(keyIndex);

        String label = keySpec.label();
        if (label != null && containsUnpairedSurrogate(label)) {
          unpairedSurrogates++;
          if (samples.size() < 4) {
            samples.add(
                "Row " + (rowIndex + 1) + " key " + (keyIndex + 1) + " has invalid UTF-16.");
          }
        }

        String popupCharacters = keySpec.popupCharacters();
        if (popupCharacters != null && containsUnpairedSurrogate(popupCharacters)) {
          unpairedSurrogates++;
          if (samples.size() < 4) {
            samples.add(
                "Row "
                    + (rowIndex + 1)
                    + " key "
                    + (keyIndex + 1)
                    + " popupCharacters has invalid UTF-16.");
          }
        }

        for (KeyCode code : keySpec.codes()) {
          Integer numeric = code.asNumeric();
          if (numeric == null || numeric <= 0) continue;
          if (!Character.isValidCodePoint(numeric) || isSurrogateCodePoint(numeric)) {
            invalidCodePoints++;
            if (samples.size() < 4) {
              samples.add(
                  "Row "
                      + (rowIndex + 1)
                      + " key "
                      + (keyIndex + 1)
                      + " has invalid code point: "
                      + numeric);
            }
          }
        }
      }
    }

    if (unpairedSurrogates == 0 && invalidCodePoints == 0) return Collections.emptyList();

    List<String> warnings = new ArrayList<>();
    if (unpairedSurrogates > 0) {
      warnings.add("Found invalid UTF-16 text in " + unpairedSurrogates + " field(s).");
    }
    if (invalidCodePoints > 0) {
      warnings.add("Found invalid Unicode code point(s): " + invalidCodePoints + ".");
    }
    warnings.addAll(samples);
    return warnings;
  }

  private static boolean isSurrogateCodePoint(int codePoint) {
    return codePoint >= 0xD800 && codePoint <= 0xDFFF;
  }

  private static boolean containsUnpairedSurrogate(@NonNull String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isHighSurrogate(c)) {
        if (i + 1 >= value.length()) return true;
        char next = value.charAt(i + 1);
        if (!Character.isLowSurrogate(next)) return true;
        i++;
      } else if (Character.isLowSurrogate(c)) {
        return true;
      }
    }
    return false;
  }
}
