package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.graphics.RectF;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PackKeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksRepository;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardRuntimeLoader;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DesignerKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.OnKeyboardActionListener;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.DirectoryPackSource;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;

final class CustomKeyboardEditorSessionController {

  interface Host {
    @NonNull
    Fragment fragment();

    @NonNull
    KeyboardView keyboardView();

    @NonNull
    TextView instructionsView();

    @NonNull
    SwitchCompat testTypingSwitch();

    @NonNull
    View testTypingContainer();

    @NonNull
    TextView testTypingBufferView();

    @NonNull
    Button testTypingClearButton();

    @NonNull
    TextView validationWarningsView();

    @NonNull
    TextView statusView();

    @NonNull
    Button undoButton();

    @NonNull
    View selectionActionBar();

    void applyPersistedThemeIfAny();
  }

  @NonNull private final Host host;
  @NonNull private final PackKeyboardRuntimeLoader runtimeLoader = new PackKeyboardRuntimeLoader();
  @Nullable private CustomKeyboardLayoutEditor layoutEditor;

  private int touchSlopPx;
  private int editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
  private float editorDownX;
  private float editorDownY;

  @Nullable private InstalledKeyboardPack pack;
  @Nullable private PackEntry keyboardEntry;
  @Nullable private File keyboardXmlFile;
  @Nullable private KeyboardModel keyboardModel;
  @Nullable private PackKeyboardDefinition keyboardDefinition;
  @NonNull private final KeyboardModelUndoStack undoStack = new KeyboardModelUndoStack();

  private int selectedRowIndex = -1;
  private int selectedKeyIndex = -1;

  // Long-press drag-reorder state. The drag is purely visual until the finger lifts:
  // the model is written once, on drop.
  @Nullable private Runnable pendingDragStart;
  private boolean draggingKey;
  private int dragSourceRow = -1;
  private int dragSourceKey = -1;
  @Nullable private Keyboard.Key dragSourceRuntimeKey;
  private int dragTargetRow = -1;
  private int dragTargetInsertIndex = -1;
  private float lastTouchX;
  private float lastTouchY;
  @Nullable private Integer defaultRowWidthTextColor;

  @NonNull private final StringBuilder testTypingBuffer = new StringBuilder();
  private boolean testTypingEnabled;

  @NonNull
  private final EditorKeyboardActionListener editorKeyboardActionListener =
      new EditorKeyboardActionListener();

  @NonNull
  private final TestTypingKeyboardActionListener testTypingKeyboardActionListener =
      new TestTypingKeyboardActionListener();

  CustomKeyboardEditorSessionController(@NonNull Host host) {
    this.host = host;
  }

  void onViewCreated(boolean restoredTestTypingEnabled, @NonNull String restoredTestTypingBuffer) {
    layoutEditor =
        new CustomKeyboardLayoutEditor(
            new CustomKeyboardLayoutEditor.Host() {
              @NonNull
              @Override
              public Fragment fragment() {
                return host.fragment();
              }

              @NonNull
              @Override
              public KeyboardView keyboardView() {
                return host.keyboardView();
              }

              @NonNull
              @Override
              public TextView statusView() {
                return host.statusView();
              }

              @Nullable
              @Override
              public InstalledKeyboardPack pack() {
                return pack;
              }

              @Nullable
              @Override
              public PackEntry keyboardEntry() {
                return keyboardEntry;
              }

              @Override
              public void setKeyboardEntry(@Nullable PackEntry entry) {
                keyboardEntry = entry;
              }

              @Nullable
              @Override
              public File keyboardXmlFile() {
                return keyboardXmlFile;
              }

              @Override
              public void setKeyboardXmlFile(@Nullable File file) {
                if (!Objects.equals(keyboardXmlFile, file)) {
                  selectedRowIndex = -1;
                  selectedKeyIndex = -1;
                }
                keyboardXmlFile = file;
              }

              @Nullable
              @Override
              public KeyboardModel keyboardModel() {
                return keyboardModel;
              }

              @Override
              public void setKeyboardModel(@Nullable KeyboardModel model) {
                keyboardModel = model;
              }

              @Nullable
              @Override
              public PackKeyboardDefinition keyboardDefinition() {
                return keyboardDefinition;
              }

              @Override
              public void setKeyboardDefinition(@Nullable PackKeyboardDefinition definition) {
                keyboardDefinition = definition;
              }

              @Override
              public void updateValidationWarnings() {
                CustomKeyboardEditorSessionController.this.updateValidationWarnings();
              }

              @NonNull
              @Override
              public KeyboardModelUndoStack undoStack() {
                return undoStack;
              }

              @Override
              public void onUndoStackChanged() {
                updateUndoButton();
              }
            },
            runtimeLoader);

    touchSlopPx = ViewConfiguration.get(host.fragment().requireContext()).getScaledTouchSlop();
    host.keyboardView().setOnTouchListener(this::onKeyboardViewTouch);

    testTypingEnabled = restoredTestTypingEnabled;
    if (!TextUtils.isEmpty(restoredTestTypingBuffer))
      testTypingBuffer.append(restoredTestTypingBuffer);

    host.testTypingClearButton()
        .setOnClickListener(
            v -> {
              testTypingBuffer.setLength(0);
              updateTestTypingBufferView();
            });

    host.undoButton()
        .setOnClickListener(
            v -> {
              CustomKeyboardLayoutEditor editor = layoutEditor;
              if (editor != null) editor.undoLastEdit();
            });
    updateUndoButton();

    final View selectionBar = host.selectionActionBar();
    selectionBar
        .findViewById(R.id.custom_keyboard_editor_selection_edit)
        .setOnClickListener(
            v -> {
              CustomKeyboardLayoutEditor editor = layoutEditor;
              if (editor != null && selectedRowIndex >= 0) {
                editor.editKeyAt(selectedRowIndex, selectedKeyIndex);
              }
            });
    selectionBar
        .findViewById(R.id.custom_keyboard_editor_selection_move_left)
        .setOnClickListener(v -> moveSelectedKey(-1));
    selectionBar
        .findViewById(R.id.custom_keyboard_editor_selection_move_right)
        .setOnClickListener(v -> moveSelectedKey(+1));
    selectionBar
        .findViewById(R.id.custom_keyboard_editor_selection_insert)
        .setOnClickListener(
            v -> {
              CustomKeyboardLayoutEditor editor = layoutEditor;
              if (editor != null && selectedRowIndex >= 0) {
                int row = selectedRowIndex;
                int newKeyIndex = selectedKeyIndex + 1;
                editor.insertKeyAfterAt(row, selectedKeyIndex);
                selectKeyAt(row, newKeyIndex);
              }
            });
    selectionBar
        .findViewById(R.id.custom_keyboard_editor_selection_delete)
        .setOnClickListener(
            v -> {
              CustomKeyboardLayoutEditor editor = layoutEditor;
              if (editor != null && selectedRowIndex >= 0) {
                editor.deleteKeyAt(selectedRowIndex, selectedKeyIndex);
              }
            });
    selectionBar
        .findViewById(R.id.custom_keyboard_editor_selection_row_actions)
        .setOnClickListener(
            v -> {
              CustomKeyboardLayoutEditor editor = layoutEditor;
              if (editor != null && selectedRowIndex >= 0) {
                editor.showLayoutActionsDialog(selectedRowIndex, selectedKeyIndex);
              }
            });
    refreshSelectionUi();

    host.testTypingSwitch().setOnCheckedChangeListener(null);
    host.testTypingSwitch().setChecked(testTypingEnabled);
    host.testTypingSwitch()
        .setOnCheckedChangeListener(
            (CompoundButton buttonView, boolean isChecked) -> setTestTypingEnabled(isChecked));
    setTestTypingEnabled(testTypingEnabled);
  }

  void onStart() {
    loadKeyboardOrShowError();
  }

  boolean testTypingEnabled() {
    return testTypingEnabled;
  }

  @NonNull
  String testTypingBuffer() {
    return testTypingBuffer.toString();
  }

  @Nullable
  InstalledKeyboardPack pack() {
    return pack;
  }

  void setPack(@Nullable InstalledKeyboardPack updatedPack) {
    pack = updatedPack;
  }

  private boolean onKeyboardViewTouch(@NonNull View v, @NonNull MotionEvent event) {
    if (testTypingEnabled) return false; // let KeyboardView handle actual typing simulation

    final KeyboardView keyboardView = host.keyboardView();
    KeyboardDefinition currentKeyboard = keyboardView.getKeyboard();
    if (currentKeyboard == null || currentKeyboard.getKeys().isEmpty()) return false;

    final int action = event.getActionMasked();
    switch (action) {
      case MotionEvent.ACTION_DOWN -> {
        editorDownX = event.getX();
        editorDownY = event.getY();
        lastTouchX = event.getX();
        lastTouchY = event.getY();
        editorDownKeyIndex = findKeyIndexForEvent(keyboardView, currentKeyboard, event);
        if (editorDownKeyIndex == KeyboardViewBase.NOT_A_KEY) {
          clearSelection();
        } else {
          scheduleDragStart(currentKeyboard, editorDownKeyIndex);
        }
        return true;
      }
      case MotionEvent.ACTION_MOVE -> {
        lastTouchX = event.getX();
        lastTouchY = event.getY();
        if (draggingKey) {
          updateKeyDragVisual();
          return true;
        }
        if (editorDownKeyIndex == KeyboardViewBase.NOT_A_KEY) return true;
        float dx = event.getX() - editorDownX;
        float dy = event.getY() - editorDownY;
        if ((dx * dx + dy * dy) > (touchSlopPx * touchSlopPx)) {
          editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
          cancelPendingDragStart();
        }
        return true;
      }
      case MotionEvent.ACTION_UP -> {
        cancelPendingDragStart();
        if (draggingKey) {
          commitKeyDrag();
          return true;
        }
        int keyIndex = editorDownKeyIndex;
        editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        if (keyIndex == KeyboardViewBase.NOT_A_KEY) return true;

        int upKeyIndex = findKeyIndexForEvent(keyboardView, currentKeyboard, event);
        if (upKeyIndex != keyIndex) return true;

        try {
          handleEditorKeyTap(currentKeyboard.getKeys().get(keyIndex));
        } catch (IndexOutOfBoundsException ignored) {
          // ignore
        }
        return true;
      }
      case MotionEvent.ACTION_CANCEL -> {
        cancelPendingDragStart();
        if (draggingKey) clearDragState();
        editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY;
        return true;
      }
      default -> {
        return true;
      }
    }
  }

  private static int findKeyIndexForEvent(
      @NonNull KeyboardView keyboardView,
      @NonNull KeyboardDefinition keyboard,
      @NonNull MotionEvent event) {
    int index =
        keyboardView
            .getKeyDetector()
            .getKeyIndexAndNearbyCodes((int) event.getX(), (int) event.getY(), null);
    if (index < 0 || index >= keyboard.getKeys().size()) return KeyboardViewBase.NOT_A_KEY;
    return index;
  }

  private void setTestTypingEnabled(boolean enabled) {
    testTypingEnabled = enabled;
    host.instructionsView()
        .setText(
            enabled
                ? R.string.custom_keyboards_test_typing_instructions
                : R.string.custom_keyboards_edit_instructions);
    host.testTypingContainer().setVisibility(enabled ? View.VISIBLE : View.GONE);
    updateTestTypingBufferView();

    host.keyboardView()
        .setOnKeyboardActionListener(
            enabled ? testTypingKeyboardActionListener : editorKeyboardActionListener);
    refreshSelectionUi();
  }

  private void handleEditorKeyTap(@NonNull Keyboard.Key key) {
    PackKeyboardDefinition definition = keyboardDefinition;
    CustomKeyboardLayoutEditor editor = layoutEditor;
    if (definition == null || editor == null) return;

    PackKeyboardDefinition.PackKeyLocation location = definition.getPackKeyLocation(key);
    if (location == null) {
      clearSelection();
      host.statusView().setText(R.string.custom_keyboards_error_key_not_editable);
      return;
    }
    if (location.rowIndex() == selectedRowIndex && location.keyIndex() == selectedKeyIndex) {
      // Tapping the selected key again opens the editor, like a double activation.
      editor.editKeyAt(selectedRowIndex, selectedKeyIndex);
    } else {
      selectKeyAt(location.rowIndex(), location.keyIndex());
    }
  }

  private void selectKeyAt(int rowIndex, int keyIndex) {
    selectedRowIndex = rowIndex;
    selectedKeyIndex = keyIndex;
    refreshSelectionUi();
  }

  private void clearSelection() {
    selectedRowIndex = -1;
    selectedKeyIndex = -1;
    refreshSelectionUi();
  }

  private void moveSelectedKey(int offset) {
    CustomKeyboardLayoutEditor editor = layoutEditor;
    KeyboardModel model = keyboardModel;
    if (editor == null || model == null || selectedRowIndex < 0) return;
    if (selectedRowIndex >= model.rows().size()) return;
    int newIndex = selectedKeyIndex + offset;
    if (newIndex < 0 || newIndex >= model.rows().get(selectedRowIndex).keys().size()) return;
    editor.moveKeyAt(selectedRowIndex, selectedKeyIndex, offset);
    selectKeyAt(selectedRowIndex, newIndex);
  }

  /** Re-resolves the selection against the current model; selection survives edits by clamping. */
  private void refreshSelectionUi() {
    final View bar = host.selectionActionBar();
    final KeyboardModel model = keyboardModel;
    final KeyboardView keyboardView = host.keyboardView();

    int[] clamped =
        (selectedRowIndex >= 0 && model != null && !testTypingEnabled)
            ? CustomKeyboardLayoutModelEditor.clampLocation(model, selectedRowIndex, selectedKeyIndex)
            : null;
    if (clamped == null) {
      if (keyboardView instanceof DesignerKeyboardView designerView) {
        designerView.setSelectedKey(null);
      }
      bar.setVisibility(View.GONE);
      return;
    }
    selectedRowIndex = clamped[0];
    selectedKeyIndex = clamped[1];

    KeySpec spec = model.rows().get(selectedRowIndex).keys().get(selectedKeyIndex);
    String label = spec.label();
    if (TextUtils.isEmpty(label)) {
      label = host.fragment().getString(R.string.custom_keyboards_selection_unlabeled_key);
    }
    String codes = spec.rawAttributes().get(CustomKeyboardLayoutEditor.ATTR_CODES);
    TextView summary = bar.findViewById(R.id.custom_keyboard_editor_selection_summary);
    summary.setText(
        TextUtils.isEmpty(codes)
            ? host.fragment().getString(R.string.custom_keyboards_selection_summary_no_codes, label)
            : host.fragment()
                .getString(R.string.custom_keyboards_selection_summary_with_codes, label, codes));

    bar.findViewById(R.id.custom_keyboard_editor_selection_move_left)
        .setEnabled(selectedKeyIndex > 0);
    bar.findViewById(R.id.custom_keyboard_editor_selection_move_right)
        .setEnabled(selectedKeyIndex < model.rows().get(selectedRowIndex).keys().size() - 1);

    TextView rowWidthView = bar.findViewById(R.id.custom_keyboard_editor_selection_row_width);
    Float rowWidthPercent =
        CustomKeyboardEditorValidationWarnings.rowWidthPercentOrNull(model, selectedRowIndex);
    if (rowWidthPercent == null) {
      rowWidthView.setVisibility(View.GONE);
    } else {
      rowWidthView.setText(
          host.fragment()
              .getString(
                  R.string.custom_keyboards_selection_row_width,
                  selectedRowIndex + 1,
                  String.format(Locale.getDefault(), "%.1f", rowWidthPercent)));
      if (defaultRowWidthTextColor == null) {
        defaultRowWidthTextColor = rowWidthView.getTextColors().getDefaultColor();
      }
      final int color;
      if (rowWidthPercent > 100.5f) {
        color = 0xFFE53935; // over-full: keys will clip
      } else if (rowWidthPercent > 97.5f) {
        color = 0xFFFB8C00; // approaching full
      } else {
        color = defaultRowWidthTextColor;
      }
      rowWidthView.setTextColor(color);
      rowWidthView.setVisibility(View.VISIBLE);
    }
    bar.setVisibility(View.VISIBLE);

    if (keyboardView instanceof DesignerKeyboardView designerView) {
      designerView.setSelectedKey(findRuntimeKey(selectedRowIndex, selectedKeyIndex));
    }
  }

  private void scheduleDragStart(@NonNull KeyboardDefinition currentKeyboard, int keyIndex) {
    cancelPendingDragStart();
    PackKeyboardDefinition definition = keyboardDefinition;
    if (definition == null) return;
    final Keyboard.Key key;
    try {
      key = currentKeyboard.getKeys().get(keyIndex);
    } catch (IndexOutOfBoundsException e) {
      return;
    }
    PackKeyboardDefinition.PackKeyLocation location = definition.getPackKeyLocation(key);
    if (location == null) return; // generic plugin-row keys cannot be dragged

    Runnable startDrag = () -> startKeyDrag(location.rowIndex(), location.keyIndex(), key);
    pendingDragStart = startDrag;
    host.keyboardView().postDelayed(startDrag, ViewConfiguration.getLongPressTimeout());
  }

  private void cancelPendingDragStart() {
    Runnable pending = pendingDragStart;
    pendingDragStart = null;
    if (pending != null) host.keyboardView().removeCallbacks(pending);
  }

  private void startKeyDrag(int rowIndex, int keyIndex, @NonNull Keyboard.Key key) {
    pendingDragStart = null;
    if (testTypingEnabled) return;
    draggingKey = true;
    dragSourceRow = rowIndex;
    dragSourceKey = keyIndex;
    dragSourceRuntimeKey = key;
    editorDownKeyIndex = KeyboardViewBase.NOT_A_KEY; // this gesture is a drag now, not a tap
    selectKeyAt(rowIndex, keyIndex);
    host.keyboardView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    updateKeyDragVisual();
  }

  private void updateKeyDragVisual() {
    KeyboardView keyboardView = host.keyboardView();
    PackKeyboardDefinition definition = keyboardDefinition;
    KeyboardDefinition displayed = keyboardView.getKeyboard();
    Keyboard.Key source = dragSourceRuntimeKey;
    if (!(keyboardView instanceof DesignerKeyboardView designerView)
        || definition == null
        || displayed == null
        || source == null) {
      return;
    }

    final int paddingLeft = keyboardView.getPaddingLeft();
    final int paddingTop = keyboardView.getPaddingTop();
    final float keyboardX = lastTouchX - paddingLeft;
    final float keyboardY = lastTouchY - paddingTop;

    // Group the editable keys by pack row (list order follows pack key order).
    SortedMap<Integer, List<Keyboard.Key>> rowsByIndex = new TreeMap<>();
    for (Keyboard.Key key : displayed.getKeys()) {
      PackKeyboardDefinition.PackKeyLocation location = definition.getPackKeyLocation(key);
      if (location == null) continue;
      rowsByIndex.computeIfAbsent(location.rowIndex(), unused -> new ArrayList<>()).add(key);
    }
    RectF ghost =
        new RectF(
            lastTouchX - source.width / 2f,
            lastTouchY - source.height / 2f,
            lastTouchX + source.width / 2f,
            lastTouchY + source.height / 2f);
    String ghostLabel = source.label != null ? source.label.toString() : null;
    if (rowsByIndex.isEmpty()) {
      dragTargetRow = -1;
      dragTargetInsertIndex = -1;
      designerView.setDragVisual(ghost, ghostLabel, null);
      return;
    }

    int bestRow = -1;
    List<Keyboard.Key> bestKeys = null;
    float bestDistance = Float.MAX_VALUE;
    for (Map.Entry<Integer, List<Keyboard.Key>> entry : rowsByIndex.entrySet()) {
      float top = Float.MAX_VALUE;
      float bottom = -Float.MAX_VALUE;
      for (Keyboard.Key key : entry.getValue()) {
        top = Math.min(top, key.y);
        bottom = Math.max(bottom, key.y + key.height);
      }
      float distance =
          (keyboardY >= top && keyboardY <= bottom)
              ? 0f
              : Math.min(Math.abs(keyboardY - top), Math.abs(keyboardY - bottom));
      if (distance < bestDistance) {
        bestDistance = distance;
        bestRow = entry.getKey();
        bestKeys = entry.getValue();
      }
    }

    float rowTop = Float.MAX_VALUE;
    float rowBottom = -Float.MAX_VALUE;
    for (Keyboard.Key key : bestKeys) {
      rowTop = Math.min(rowTop, key.y);
      rowBottom = Math.max(rowBottom, key.y + key.height);
    }
    int insertIndex = -1;
    float indicatorX = 0f;
    for (Keyboard.Key key : bestKeys) {
      if (keyboardX < key.x + key.width / 2f) {
        PackKeyboardDefinition.PackKeyLocation location = definition.getPackKeyLocation(key);
        insertIndex = location != null ? location.keyIndex() : 0;
        indicatorX = key.x;
        break;
      }
    }
    if (insertIndex < 0) {
      Keyboard.Key last = bestKeys.get(bestKeys.size() - 1);
      PackKeyboardDefinition.PackKeyLocation location = definition.getPackKeyLocation(last);
      insertIndex = (location != null ? location.keyIndex() : bestKeys.size() - 1) + 1;
      indicatorX = last.x + last.width;
    }

    dragTargetRow = bestRow;
    dragTargetInsertIndex = insertIndex;

    final float half = 2f * keyboardView.getResources().getDisplayMetrics().density;
    RectF indicator =
        new RectF(
            indicatorX + paddingLeft - half,
            rowTop + paddingTop,
            indicatorX + paddingLeft + half,
            rowBottom + paddingTop);
    designerView.setDragVisual(ghost, ghostLabel, indicator);
  }

  private void commitKeyDrag() {
    CustomKeyboardLayoutEditor editor = layoutEditor;
    final int fromRow = dragSourceRow;
    final int fromKey = dragSourceKey;
    final int toRow = dragTargetRow;
    final int toInsertIndex = dragTargetInsertIndex;
    clearDragState();
    if (editor == null || fromRow < 0 || toRow < 0) return;
    editor.moveKeyToLocationAt(fromRow, fromKey, toRow, toInsertIndex);
    int newIndex = toInsertIndex;
    if (toRow == fromRow && toInsertIndex > fromKey) newIndex = toInsertIndex - 1;
    selectKeyAt(toRow, newIndex);
  }

  private void clearDragState() {
    draggingKey = false;
    dragSourceRow = -1;
    dragSourceKey = -1;
    dragTargetRow = -1;
    dragTargetInsertIndex = -1;
    dragSourceRuntimeKey = null;
    if (host.keyboardView() instanceof DesignerKeyboardView designerView) {
      designerView.setDragVisual(null, null, null);
    }
  }

  @Nullable
  private Keyboard.Key findRuntimeKey(int rowIndex, int keyIndex) {
    PackKeyboardDefinition definition = keyboardDefinition;
    KeyboardDefinition displayed = host.keyboardView().getKeyboard();
    if (definition == null || displayed == null) return null;
    for (Keyboard.Key key : displayed.getKeys()) {
      PackKeyboardDefinition.PackKeyLocation location = definition.getPackKeyLocation(key);
      if (location != null && location.rowIndex() == rowIndex && location.keyIndex() == keyIndex) {
        return key;
      }
    }
    return null;
  }

  private void updateTestTypingBufferView() {
    if (testTypingBuffer.length() == 0) {
      host.testTypingBufferView().setText("");
    } else {
      host.testTypingBufferView().setText(testTypingBuffer.toString());
    }
  }

  private void updateUndoButton() {
    host.undoButton().setEnabled(!undoStack.isEmpty());
  }

  private void loadKeyboardOrShowError() {
    clearValidationWarnings();
    final File previousXmlFile = keyboardXmlFile;
    final Bundle args = host.fragment().getArguments();
    final String packId =
        args != null ? args.getString(CustomKeyboardEditorFragment.ARG_PACK_ID) : null;
    final String keyboardEntryId =
        args != null ? args.getString(CustomKeyboardEditorFragment.ARG_KEYBOARD_ENTRY_ID) : null;
    final String keyboardPath =
        args != null ? args.getString(CustomKeyboardEditorFragment.ARG_KEYBOARD_PATH) : null;
    if (TextUtils.isEmpty(packId)
        || (TextUtils.isEmpty(keyboardEntryId) && TextUtils.isEmpty(keyboardPath))) {
      host.statusView().setText(R.string.custom_keyboards_error_missing_pack);
      return;
    }

    try {
      pack =
          new KeyboardPacksRepository(host.fragment().requireContext())
              .findInstalledPackById(packId);
    } catch (IOException e) {
      host.statusView().setText(e.getMessage());
      return;
    }
    if (pack == null) {
      host.statusView().setText(R.string.custom_keyboards_error_missing_pack);
      return;
    }

    if (!TextUtils.isEmpty(keyboardPath)) {
      final PackPath path;
      try {
        path = PackPath.parse(keyboardPath);
      } catch (IllegalArgumentException e) {
        host.statusView().setText(e.getMessage());
        return;
      }

      keyboardEntry = null;
      keyboardXmlFile = new File(pack.directory(), path.value());
      keyboardModel = readKeyboardModelOrNull(keyboardXmlFile);

      try {
        PackEntry entry = new PackEntry("path:" + path.value().replace('/', '_'), path);
        keyboardDefinition =
            new PackKeyboardDefinition(
                host.fragment().requireContext().getApplicationContext(),
                pack.manifest(),
                entry,
                Keyboard.KEYBOARD_ROW_MODE_NORMAL,
                new DirectoryPackSource(pack.directory()),
                false);
      } catch (IOException e) {
        host.statusView().setText(R.string.custom_keyboards_error_failed_load_keyboard);
        return;
      }
    } else {
      keyboardEntry = findKeyboardEntry(pack, keyboardEntryId);
      if (keyboardEntry == null) {
        host.statusView().setText(R.string.custom_keyboards_error_missing_pack);
        return;
      }

      keyboardXmlFile = new File(pack.directory(), keyboardEntry.path().value());
      keyboardModel = readKeyboardModelOrNull(keyboardXmlFile);

      keyboardDefinition =
          runtimeLoader.tryLoadKeyboardDefinition(
              host.fragment().requireContext(),
              pack,
              keyboardEntry.id(),
              Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      if (keyboardDefinition == null) {
        host.statusView().setText(R.string.custom_keyboards_error_failed_load_keyboard);
        return;
      }
    }

    keyboardDefinition.loadKeyboard(host.keyboardView().getThemedKeyboardDimens());
    host.keyboardView().setKeyboard(keyboardDefinition, null, null);
    host.fragment().requireActivity().setTitle(keyboardDefinition.getKeyboardName());
    host.statusView().setText("");
    host.applyPersistedThemeIfAny();
    updateValidationWarnings();

    // Undo snapshots and the key selection belong to a single file; onStart re-runs this for the
    // same file (e.g. after backgrounding), in which case both stay valid and must survive.
    if (!Objects.equals(previousXmlFile, keyboardXmlFile)) {
      undoStack.clear();
      selectedRowIndex = -1;
      selectedKeyIndex = -1;
      refreshSelectionUi();
    }
    updateUndoButton();
  }

  private static void clearValidationWarnings(@NonNull TextView validationWarningsView) {
    validationWarningsView.setText("");
    validationWarningsView.setVisibility(View.GONE);
  }

  private void clearValidationWarnings() {
    clearValidationWarnings(host.validationWarningsView());
  }

  void updateValidationWarnings() {
    // Runs after every persisted model change and every (re)load — the right moments to
    // re-resolve the selection against the new model and runtime keys.
    refreshSelectionUi();

    InstalledKeyboardPack currentPack = pack;
    KeyboardModel model = keyboardModel;
    File xmlFile = keyboardXmlFile;
    if (currentPack == null || model == null || xmlFile == null) {
      clearValidationWarnings();
      return;
    }

    KeyboardDefinition displayedKeyboard = host.keyboardView().getKeyboard();
    List<String> warnings =
        CustomKeyboardEditorValidationWarnings.collect(
            host.fragment().requireContext(), currentPack, model, displayedKeyboard);

    if (warnings.isEmpty()) {
      clearValidationWarnings();
      return;
    }

    StringBuilder builder = new StringBuilder();
    builder.append(host.fragment().getString(R.string.custom_keyboards_validation_warnings_title));
    for (String warning : warnings) {
      builder.append("\n• ").append(warning);
    }

    host.validationWarningsView().setText(builder.toString());
    host.validationWarningsView().setVisibility(View.VISIBLE);
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
  private static KeyboardModel readKeyboardModelOrNull(@Nullable File keyboardXmlFile) {
    if (keyboardXmlFile == null) return null;
    try (InputStream in = new FileInputStream(keyboardXmlFile)) {
      return AskXmlKeyboardParser.parse(in);
    } catch (IOException e) {
      return null;
    }
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
      handleEditorKeyTap(key);
    }

    @Override
    public void onMultiTapStarted() {}

    @Override
    public void onMultiTapEnded() {}

    @Override
    public void onText(Keyboard.Key key, CharSequence text) {
      handleEditorKeyTap(key);
    }

    @Override
    public void onTyping(Keyboard.Key key, CharSequence text) {
      handleEditorKeyTap(key);
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
    public boolean onGestureTypingInputStart(int x, int y, KeyboardKey key, long eventTime) {
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
    public boolean onGestureTypingInputStart(int x, int y, KeyboardKey key, long eventTime) {
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
    CustomKeyboardLayoutEditor editor = layoutEditor;
    if (editor != null) editor.switchToKeyboardEntry(currentPack, entryId);
    return true;
  }
}
