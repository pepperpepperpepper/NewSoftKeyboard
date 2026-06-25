package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PackKeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardRuntimeLoader;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardWriter;

final class CustomKeyboardLayoutEditor {
  static final String ATTR_CODES = "android:codes";
  static final String ATTR_KEY_LABEL = "android:keyLabel";
  static final String ATTR_KEY_WIDTH = "android:keyWidth";
  static final String ATTR_POPUP_CHARACTERS = "android:popupCharacters";
  static final String ATTR_POPUP_KEYBOARD = "android:popupKeyboard";
  static final String ATTR_LONG_PRESS_CODE = "ask:longPressCode";
  static final String ATTR_HINT_LABEL = "ask:hintLabel";
  static final String ATTR_EXTRA_KEY_DATA = "ask:extra_key_data";
  static final String ATTR_ROW_EDGE_FLAGS = "android:rowEdgeFlags";
  static final String ATTR_KEYBOARD_MODE = "android:keyboardMode";
  static final String ATTR_KEY_EDGE_FLAGS = "android:keyEdgeFlags";
  static final String ATTR_HORIZONTAL_GAP = "android:horizontalGap";

  interface Host {
    @NonNull
    Fragment fragment();

    @NonNull
    KeyboardView keyboardView();

    @NonNull
    TextView statusView();

    @Nullable
    InstalledKeyboardPack pack();

    @Nullable
    PackEntry keyboardEntry();

    void setKeyboardEntry(@Nullable PackEntry entry);

    @Nullable
    File keyboardXmlFile();

    void setKeyboardXmlFile(@Nullable File file);

    @Nullable
    KeyboardModel keyboardModel();

    void setKeyboardModel(@Nullable KeyboardModel model);

    @Nullable
    PackKeyboardDefinition keyboardDefinition();

    void setKeyboardDefinition(@Nullable PackKeyboardDefinition definition);

    void updateValidationWarnings();

    @NonNull
    KeyboardModelUndoStack undoStack();

    void onUndoStackChanged();
  }

  @NonNull private final Host host;
  @NonNull private final PackKeyboardRuntimeLoader runtimeLoader;

  CustomKeyboardLayoutEditor(@NonNull Host host, @NonNull PackKeyboardRuntimeLoader runtimeLoader) {
    this.host = host;
    this.runtimeLoader = runtimeLoader;
  }

  private void showKeyEditDialogAtLocation(int rowIndex, int keyIndex) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;

    final KeySpec currentSpec;
    try {
      currentSpec = model.rows().get(rowIndex).keys().get(keyIndex);
    } catch (IndexOutOfBoundsException e) {
      host.statusView().setText(R.string.custom_keyboards_error_key_not_found);
      return;
    }

    final Map<String, String> attrs = currentSpec.rawAttributes();

    Fragment fragment = host.fragment();
    View dialogView =
        LayoutInflater.from(fragment.requireContext())
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

    wireKeyTypeUi(dialogView, attrs, labelEdit, codesEdit, extraDataEdit);

    AlertDialog dialog =
        new AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.custom_keyboards_edit_key_dialog_title)
            .setView(dialogView)
            .setPositiveButton(
                android.R.string.ok,
                (ignored, which) -> {
                  try {
                    KeyboardModel updated =
                        CustomKeyboardLayoutModelEditor.updateKeySpec(
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
                    host.statusView().setText(e.getMessage());
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

  void editKeyAt(int rowIndex, int keyIndex) {
    showKeyEditDialogAtLocation(rowIndex, keyIndex);
  }

  private static final int KEY_TYPE_CHARACTER = 0;
  private static final int KEY_TYPE_FUNCTION = 1;
  private static final int KEY_TYPE_SWITCH = 2;

  private interface OnFunctionPicked {
    void onPicked(@NonNull KeyFunctionCatalog.Entry entry);
  }

  private interface OnSwitchPicked {
    void onPicked(@NonNull String targetId);
  }

  private interface OnCharacterPicked {
    void onPicked(@NonNull String text);
  }

  /**
   * Wires the friendly key-type controls (character field, function picker, layer-switch picker)
   * and the Advanced expander on top of the raw attribute EditTexts, which remain the single source
   * of truth read on OK.
   */
  private void wireKeyTypeUi(
      @NonNull View dialogView,
      @NonNull Map<String, String> attrs,
      @NonNull EditText labelEdit,
      @NonNull EditText codesEdit,
      @NonNull EditText extraDataEdit) {
    RadioGroup typeGroup = dialogView.findViewById(R.id.edit_key_type_group);
    View charGroup = dialogView.findViewById(R.id.edit_key_group_character);
    View functionGroup = dialogView.findViewById(R.id.edit_key_group_function);
    View switchGroup = dialogView.findViewById(R.id.edit_key_group_switch);
    EditText characterEdit = dialogView.findViewById(R.id.edit_key_character);
    Button browseCharacters = dialogView.findViewById(R.id.edit_key_browse_characters);
    TextView functionReadout = dialogView.findViewById(R.id.edit_key_function_readout);
    Button chooseFunction = dialogView.findViewById(R.id.edit_key_choose_function);
    TextView switchReadout = dialogView.findViewById(R.id.edit_key_switch_readout);
    Button chooseSwitch = dialogView.findViewById(R.id.edit_key_choose_switch);
    View advancedHeader = dialogView.findViewById(R.id.edit_key_advanced_header);
    View advancedContainer = dialogView.findViewById(R.id.edit_key_advanced_container);
    ImageView advancedChevron = dialogView.findViewById(R.id.edit_key_advanced_chevron);

    characterEdit.setText(attrs.get(ATTR_KEY_LABEL));
    updateFunctionReadout(functionReadout, codesEdit.getText().toString());
    updateSwitchReadout(switchReadout, extraDataEdit.getText().toString());

    // The character field mirrors into label + codes (codepoint of the first character).
    characterEdit.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            String text = s.toString();
            labelEdit.setText(text);
            codesEdit.setText(text.isEmpty() ? "" : Integer.toString(text.codePointAt(0)));
          }
        });

    browseCharacters.setOnClickListener(v -> showCharacterPicker(characterEdit::setText));

    typeGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
          charGroup.setVisibility(
              checkedId == R.id.edit_key_type_character ? View.VISIBLE : View.GONE);
          functionGroup.setVisibility(
              checkedId == R.id.edit_key_type_function ? View.VISIBLE : View.GONE);
          switchGroup.setVisibility(
              checkedId == R.id.edit_key_type_switch ? View.VISIBLE : View.GONE);
        });

    final int initialType = detectKeyType(attrs);
    typeGroup.check(
        initialType == KEY_TYPE_FUNCTION
            ? R.id.edit_key_type_function
            : initialType == KEY_TYPE_SWITCH
                ? R.id.edit_key_type_switch
                : R.id.edit_key_type_character);

    chooseFunction.setOnClickListener(
        v ->
            showFunctionPicker(
                entry -> {
                  codesEdit.setText(Integer.toString(entry.code));
                  if (TextUtils.isEmpty(labelEdit.getText())) labelEdit.setText(entry.defaultLabel);
                  updateFunctionReadout(functionReadout, codesEdit.getText().toString());
                }));

    chooseSwitch.setOnClickListener(
        v ->
            showSwitchPicker(
                targetId -> {
                  codesEdit.setText(Integer.toString(KeyCodes.CUSTOM_KEYBOARD_SWITCH));
                  extraDataEdit.setText(targetId);
                  if (TextUtils.isEmpty(labelEdit.getText())) labelEdit.setText("?123");
                  updateSwitchReadout(switchReadout, targetId);
                }));

    advancedHeader.setOnClickListener(
        v -> {
          boolean show = advancedContainer.getVisibility() != View.VISIBLE;
          advancedContainer.setVisibility(show ? View.VISIBLE : View.GONE);
          advancedChevron.setRotation(show ? 180f : 0f);
        });
  }

  private static int detectKeyType(@NonNull Map<String, String> attrs) {
    String extra = attrs.get(ATTR_EXTRA_KEY_DATA);
    Integer code = parseSingleNumericCode(attrs.get(ATTR_CODES));
    if ((code != null && code == KeyCodes.CUSTOM_KEYBOARD_SWITCH)
        || (extra != null && extra.startsWith("pack::"))) {
      return KEY_TYPE_SWITCH;
    }
    if (code != null && KeyFunctionCatalog.findByCode(code) != null) {
      return KEY_TYPE_FUNCTION;
    }
    return KEY_TYPE_CHARACTER;
  }

  @Nullable
  private static Integer parseSingleNumericCode(@Nullable String raw) {
    if (raw == null) return null;
    String token = raw.trim();
    if (token.isEmpty() || token.startsWith("@") || token.contains(",")) return null;
    if (token.length() == 1 && !Character.isDigit(token.charAt(0))) return (int) token.charAt(0);
    try {
      return Integer.parseInt(token);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static void updateFunctionReadout(@NonNull TextView readout, @Nullable String codes) {
    Integer code = parseSingleNumericCode(codes);
    KeyFunctionCatalog.Entry entry = code == null ? null : KeyFunctionCatalog.findByCode(code);
    readout.setText(
        entry != null
            ? entry.name
            : readout.getContext().getString(R.string.custom_keyboards_key_function_none));
  }

  private static void updateSwitchReadout(@NonNull TextView readout, @Nullable String extra) {
    if (extra != null && extra.startsWith("pack::")) {
      String[] parts = extra.split("::", 3);
      readout.setText(parts.length == 3 ? parts[2] : extra);
    } else {
      readout.setText(R.string.custom_keyboards_key_switch_none);
    }
  }

  private void showFunctionPicker(@NonNull OnFunctionPicked onPicked) {
    final List<KeyFunctionCatalog.Entry> flat = new ArrayList<>();
    final List<CharSequence> labels = new ArrayList<>();
    for (KeyFunctionCatalog.Category category : KeyFunctionCatalog.categories()) {
      for (KeyFunctionCatalog.Entry entry : category.entries) {
        flat.add(entry);
        labels.add(category.title + " · " + entry.name);
      }
    }
    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_choose_function_title)
        .setItems(
            labels.toArray(new CharSequence[0]), (d, which) -> onPicked.onPicked(flat.get(which)))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showCharacterPicker(@NonNull OnCharacterPicked onPicked) {
    final Context context = host.fragment().requireContext();
    final View view =
        LayoutInflater.from(context)
            .inflate(R.layout.custom_keyboard_character_picker, null, false);
    final EditText search = view.findViewById(R.id.char_picker_search);
    final Spinner categorySpinner = view.findViewById(R.id.char_picker_category);
    final GridView grid = view.findViewById(R.id.char_picker_grid);
    final EditText codepointEdit = view.findViewById(R.id.char_picker_codepoint);
    final Button useButton = view.findViewById(R.id.char_picker_use);

    final List<CharacterLibrary.Category> categories = CharacterLibrary.categories();
    final List<String> titles = new ArrayList<>();
    for (CharacterLibrary.Category category : categories) titles.add(category.title);
    categorySpinner.setAdapter(
        new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, titles));

    final CharacterGridAdapter gridAdapter = new CharacterGridAdapter(context);
    grid.setAdapter(gridAdapter);

    final Runnable showSelectedCategory =
        () -> {
          int pos = categorySpinner.getSelectedItemPosition();
          if (pos < 0 || pos >= categories.size()) pos = 0;
          gridAdapter.setCodepoints(CharacterLibrary.codepointsIn(categories.get(pos)));
        };

    categorySpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
            if (TextUtils.isEmpty(search.getText())) showSelectedCategory.run();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    search.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            final String query = s.toString().trim();
            if (query.isEmpty()) showSelectedCategory.run();
            else gridAdapter.setCodepoints(CharacterLibrary.search(query, 300));
          }
        });

    showSelectedCategory.run();

    final AlertDialog dialog =
        new AlertDialog.Builder(context)
            .setTitle(R.string.custom_keyboards_char_picker_title)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create();

    grid.setOnItemClickListener(
        (parent, v, position, id) -> {
          onPicked.onPicked(CharacterLibrary.glyph(gridAdapter.codePointAt(position)));
          dialog.dismiss();
        });

    useButton.setOnClickListener(
        v -> {
          final Integer cp =
              CharacterLibrary.parseCodepoint(codepointEdit.getText().toString());
          if (cp == null || !CharacterLibrary.isPickable(cp)) {
            Toast.makeText(
                    context, R.string.custom_keyboards_char_picker_invalid, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          onPicked.onPicked(CharacterLibrary.glyph(cp));
          dialog.dismiss();
        });

    dialog.show();
  }

  private void showSwitchPicker(@NonNull OnSwitchPicked onPicked) {
    InstalledKeyboardPack pack = host.pack();
    if (pack == null) return;
    PackManifest manifest = pack.manifest();
    PackEntry current = host.keyboardEntry();
    String currentId = current == null ? null : current.id();

    final List<PackEntry> targets = new ArrayList<>();
    final List<CharSequence> labels = new ArrayList<>();
    for (PackEntry entry : manifest.keyboards()) {
      if (currentId != null && currentId.equals(entry.id())) continue;
      targets.add(entry);
      labels.add(entry.id());
    }
    if (targets.isEmpty()) {
      new AlertDialog.Builder(host.fragment().requireContext())
          .setMessage(R.string.custom_keyboards_key_no_layers)
          .setPositiveButton(android.R.string.ok, null)
          .show();
      return;
    }
    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_choose_switch_title)
        .setItems(
            labels.toArray(new CharSequence[0]),
            (d, which) -> {
              PackEntry entry = targets.get(which);
              onPicked.onPicked("pack::" + manifest.id() + "::" + entry.id());
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  void moveKeyAt(int rowIndex, int keyIndex, int offset) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;
    try {
      applyModelUpdate(
          xmlFile, CustomKeyboardLayoutModelEditor.moveKey(model, rowIndex, keyIndex, offset));
    } catch (IOException e) {
      host.statusView().setText(e.getMessage());
    }
  }

  void insertKeyAfterAt(int rowIndex, int keyIndex) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;
    try {
      applyModelUpdate(
          xmlFile, CustomKeyboardLayoutModelEditor.insertKeyAfter(model, rowIndex, keyIndex));
      showKeyEditDialogAtLocation(rowIndex, keyIndex + 1);
    } catch (IOException e) {
      host.statusView().setText(e.getMessage());
    }
  }

  void moveKeyToLocationAt(int fromRow, int fromKey, int toRow, int toInsertIndex) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;
    try {
      applyModelUpdate(
          xmlFile,
          CustomKeyboardLayoutModelEditor.moveKeyToLocation(
              model, fromRow, fromKey, toRow, toInsertIndex));
    } catch (IOException e) {
      host.statusView().setText(e.getMessage());
    }
  }

  void deleteKeyAt(int rowIndex, int keyIndex) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;
    confirmDeleteKey(model, xmlFile, rowIndex, keyIndex);
  }

  private void reloadKeyboardView() {
    PackKeyboardDefinition currentKeyboard = host.keyboardDefinition();
    if (currentKeyboard == null) return;
    currentKeyboard.loadKeyboard(host.keyboardView().getThemedKeyboardDimens());
    host.keyboardView().setKeyboard(currentKeyboard, null, null);
  }

  private void applyModelUpdate(@NonNull File xmlFile, @NonNull KeyboardModel updated)
      throws IOException {
    final KeyboardModel previous = host.keyboardModel();
    // The pure model editors return the input model on out-of-bounds/no-op requests; persisting
    // those would burn undo entries and IME cache flushes on edits that changed nothing.
    if (previous == updated) return;
    if (previous != null
        && AskXmlKeyboardWriter.toXml(previous).equals(AskXmlKeyboardWriter.toXml(updated))) {
      return;
    }
    persistModel(xmlFile, updated);
    if (previous != null) {
      host.undoStack().push(previous);
      host.onUndoStackChanged();
    }
  }

  void undoLastEdit() {
    File xmlFile = host.keyboardXmlFile();
    KeyboardModel previous = host.undoStack().pop();
    if (xmlFile == null || previous == null) return;
    try {
      persistModel(xmlFile, previous);
    } catch (IOException e) {
      // The file still holds the newer content; keep the snapshot so undo can be retried.
      host.undoStack().push(previous);
      host.statusView().setText(e.getMessage());
    }
    host.onUndoStackChanged();
  }

  private void persistModel(@NonNull File xmlFile, @NonNull KeyboardModel model)
      throws IOException {
    CustomKeyboardLayoutPackFiles.writeKeyboardModel(xmlFile, model);
    host.setKeyboardModel(model);
    CustomKeyboardPrefs.bumpGeneration(host.fragment().requireContext());
    reloadKeyboardView();
    host.updateValidationWarnings();
    host.statusView().setText("");
  }

  void showLayoutActionsDialog(int rowIndex, int keyIndex) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;

    final List<ActionItem> actions = new ArrayList<>();
    KeyboardRow row = model.rows().get(rowIndex);
    final KeySpec currentKeySpec = row.keys().get(keyIndex);
    final Map<String, String> currentKeyAttrs = currentKeySpec.rawAttributes();
    Fragment fragment = host.fragment();
    if (keyIndex > 0) {
      actions.add(
          new ActionItem(
              fragment.getString(R.string.custom_keyboards_layout_action_move_key_left),
              () -> {
                try {
                  applyModelUpdate(
                      xmlFile,
                      CustomKeyboardLayoutModelEditor.moveKey(model, rowIndex, keyIndex, -1));
                } catch (IOException e) {
                  host.statusView().setText(e.getMessage());
                }
              }));
    }
    if (keyIndex < row.keys().size() - 1) {
      actions.add(
          new ActionItem(
              fragment.getString(R.string.custom_keyboards_layout_action_move_key_right),
              () -> {
                try {
                  applyModelUpdate(
                      xmlFile,
                      CustomKeyboardLayoutModelEditor.moveKey(model, rowIndex, keyIndex, +1));
                } catch (IOException e) {
                  host.statusView().setText(e.getMessage());
                }
              }));
    }
    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_insert_key_after),
            () -> {
              try {
                applyModelUpdate(
                    xmlFile,
                    CustomKeyboardLayoutModelEditor.insertKeyAfter(model, rowIndex, keyIndex));
                showKeyEditDialogAtLocation(rowIndex, keyIndex + 1);
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            }));
    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_delete_key),
            () -> confirmDeleteKey(model, xmlFile, rowIndex, keyIndex)));

    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_create_popup_keyboard),
            () -> {
              InstalledKeyboardPack currentPack = host.pack();
              if (currentPack == null) return;
              try {
                String popupPath =
                    CustomKeyboardLayoutPackFiles.createPopupKeyboardFile(currentPack);
                KeyboardModel updated =
                    CustomKeyboardLayoutModelEditor.updateKeySpec(
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
                host.statusView().setText(e.getMessage());
              }
            }));

    final String existingPopupKeyboardRaw = currentKeyAttrs.get(ATTR_POPUP_KEYBOARD);
    final String existingPopupKeyboard =
        !TextUtils.isEmpty(existingPopupKeyboardRaw) ? existingPopupKeyboardRaw.trim() : null;
    if (!TextUtils.isEmpty(existingPopupKeyboard) && !existingPopupKeyboard.startsWith("@")) {
      actions.add(
          new ActionItem(
              fragment.getString(R.string.custom_keyboards_layout_action_edit_popup_keyboard),
              () -> {
                InstalledKeyboardPack currentPack = host.pack();
                if (currentPack == null) return;
                openEditorForKeyboardPath(currentPack.manifest().id(), existingPopupKeyboard);
              }));
    }

    if (host.pack() != null && host.keyboardEntry() != null) {
      actions.add(
          new ActionItem(
              fragment.getString(R.string.custom_keyboards_layout_action_create_symbols_layer),
              () -> createOrLinkSymbolsLayer(model, xmlFile, rowIndex, keyIndex, currentKeyAttrs)));
    }

    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_edit_row),
            () -> showRowEditDialog(rowIndex)));
    if (rowIndex > 0) {
      actions.add(
          new ActionItem(
              fragment.getString(R.string.custom_keyboards_layout_action_move_row_up),
              () -> {
                try {
                  applyModelUpdate(
                      xmlFile, CustomKeyboardLayoutModelEditor.moveRow(model, rowIndex, -1));
                } catch (IOException e) {
                  host.statusView().setText(e.getMessage());
                }
              }));
    }
    if (rowIndex < model.rows().size() - 1) {
      actions.add(
          new ActionItem(
              fragment.getString(R.string.custom_keyboards_layout_action_move_row_down),
              () -> {
                try {
                  applyModelUpdate(
                      xmlFile, CustomKeyboardLayoutModelEditor.moveRow(model, rowIndex, +1));
                } catch (IOException e) {
                  host.statusView().setText(e.getMessage());
                }
              }));
    }
    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_insert_row_above),
            () -> {
              try {
                applyModelUpdate(
                    xmlFile, CustomKeyboardLayoutModelEditor.insertRowAbove(model, rowIndex));
                showKeyEditDialogAtLocation(rowIndex, 0);
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            }));
    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_insert_row_below),
            () -> {
              try {
                applyModelUpdate(
                    xmlFile, CustomKeyboardLayoutModelEditor.insertRowBelow(model, rowIndex));
                showKeyEditDialogAtLocation(rowIndex + 1, 0);
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            }));
    actions.add(
        new ActionItem(
            fragment.getString(R.string.custom_keyboards_layout_action_delete_row),
            () -> confirmDeleteRow(model, xmlFile, rowIndex)));

    CharSequence[] titles = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) titles[i] = actions.get(i).title;

    new AlertDialog.Builder(fragment.requireContext())
        .setTitle(R.string.custom_keyboards_layout_actions_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              ActionItem item = actions.get(which);
              item.action.run();
            })
        .show();
  }

  private void openEditorForKeyboardPath(@NonNull String packId, @NonNull String keyboardPath) {
    NavController navController = Navigation.findNavController(host.fragment().requireView());
    Bundle args = new Bundle();
    args.putString(CustomKeyboardEditorFragment.ARG_PACK_ID, packId);
    args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_ENTRY_ID, "");
    args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_PATH, keyboardPath);
    navController.navigate(R.id.customKeyboardEditorFragment, args);
  }

  private void openEditorForKeyboardEntry(@NonNull String packId, @NonNull String entryId) {
    NavController navController = Navigation.findNavController(host.fragment().requireView());
    Bundle args = new Bundle();
    args.putString(CustomKeyboardEditorFragment.ARG_PACK_ID, packId);
    args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_ENTRY_ID, entryId);
    args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_PATH, "");
    navController.navigate(R.id.customKeyboardEditorFragment, args);
  }

  void switchToKeyboardEntry(@NonNull InstalledKeyboardPack pack, @NonNull String entryId) {
    PackEntry entry = CustomKeyboardLayoutPackFiles.findKeyboardEntry(pack, entryId);
    if (entry == null) {
      host.statusView().setText("Switch target not found: " + entryId);
      return;
    }

    PackKeyboardDefinition loaded =
        runtimeLoader.tryLoadKeyboardDefinition(
            host.fragment().requireContext(), pack, entry.id(), Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    if (loaded == null) {
      host.statusView().setText(R.string.custom_keyboards_error_failed_load_keyboard);
      return;
    }

    host.setKeyboardEntry(entry);
    host.setKeyboardXmlFile(new File(pack.directory(), entry.path().value()));
    host.setKeyboardModel(
        CustomKeyboardLayoutPackFiles.readKeyboardModelOrNull(host.keyboardXmlFile()));
    host.setKeyboardDefinition(loaded);
    host.undoStack().clear();
    host.onUndoStackChanged();

    loaded.loadKeyboard(host.keyboardView().getThemedKeyboardDimens());
    host.keyboardView().setKeyboard(loaded, null, null);
    host.fragment().requireActivity().setTitle(loaded.getKeyboardName());
    host.updateValidationWarnings();
  }

  private void createOrLinkSymbolsLayer(
      @NonNull KeyboardModel model,
      @NonNull File xmlFile,
      int rowIndex,
      int keyIndex,
      @NonNull Map<String, String> currentKeyAttrs) {
    InstalledKeyboardPack currentPack = host.pack();
    PackKeyboardDefinition currentKeyboard = host.keyboardDefinition();
    if (currentPack == null || currentKeyboard == null) return;

    PackManifest manifest = currentPack.manifest();
    PackEntry symbolsEntry =
        CustomKeyboardLayoutPackFiles.findKeyboardEntry(currentPack, "symbols");
    if (symbolsEntry == null) {
      try {
        symbolsEntry = CustomKeyboardLayoutPackFiles.createSymbolsEntry(manifest);
        CustomKeyboardLayoutPackFiles.writeSymbolsKeyboardFile(
            currentPack, symbolsEntry, currentKeyboard.getKeyboardId());
        PackManifest updatedManifest =
            new PackManifest(
                manifest.schemaVersion(),
                manifest.id(),
                manifest.name(),
                manifest.version() + 1,
                manifest.minCoreVersion(),
                CustomKeyboardLayoutPackFiles.appendEntry(manifest.keyboards(), symbolsEntry),
                manifest.themes());
        CustomKeyboardLayoutPackFiles.writePackManifest(currentPack.directory(), updatedManifest);
      } catch (IOException e) {
        host.statusView().setText(e.getMessage());
        return;
      }
    }

    String targetKeyboardId = "pack::" + manifest.id() + "::" + symbolsEntry.id();

    try {
      KeyboardModel updated =
          CustomKeyboardLayoutModelEditor.updateKeySpec(
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
      host.statusView().setText(e.getMessage());
    }
  }

  private void confirmDeleteKey(
      @NonNull KeyboardModel model, @NonNull File xmlFile, int rowIndex, int keyIndex) {
    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_confirm_delete_key_title)
        .setMessage(R.string.custom_keyboards_confirm_delete_key_message)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              try {
                applyModelUpdate(
                    xmlFile, CustomKeyboardLayoutModelEditor.deleteKey(model, rowIndex, keyIndex));
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmDeleteRow(@NonNull KeyboardModel model, @NonNull File xmlFile, int rowIndex) {
    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_confirm_delete_row_title)
        .setMessage(R.string.custom_keyboards_confirm_delete_row_message)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              try {
                applyModelUpdate(
                    xmlFile, CustomKeyboardLayoutModelEditor.deleteRow(model, rowIndex));
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showRowEditDialog(int rowIndex) {
    KeyboardModel model = host.keyboardModel();
    File xmlFile = host.keyboardXmlFile();
    if (model == null || xmlFile == null) return;

    final KeyboardRow row;
    try {
      row = model.rows().get(rowIndex);
    } catch (IndexOutOfBoundsException e) {
      host.statusView().setText(R.string.custom_keyboards_error_key_not_found);
      return;
    }

    Fragment fragment = host.fragment();
    View dialogView =
        LayoutInflater.from(fragment.requireContext())
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

    new AlertDialog.Builder(fragment.requireContext())
        .setTitle(R.string.custom_keyboards_edit_row_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              KeyboardModel updated =
                  CustomKeyboardLayoutModelEditor.updateRow(
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
                host.statusView().setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static final class ActionItem {
    @NonNull final String title;
    @NonNull final Runnable action;

    private ActionItem(@NonNull String title, @NonNull Runnable action) {
      this.title = title;
      this.action = action;
    }
  }
}
