package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PackKeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksRepository;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardRuntimeLoader;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.OnKeyboardActionListener;
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
        editorDownKeyIndex = findKeyIndexForEvent(keyboardView, currentKeyboard, event);
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

        int upKeyIndex = findKeyIndexForEvent(keyboardView, currentKeyboard, event);
        if (upKeyIndex != keyIndex) return true;

        try {
          CustomKeyboardLayoutEditor editor = layoutEditor;
          if (editor != null) editor.showKeyEditDialog(currentKeyboard.getKeys().get(keyIndex));
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

    // Undo snapshots belong to a single file; onStart re-runs this for the same file (e.g. after
    // backgrounding), in which case history stays valid and must survive.
    if (!Objects.equals(previousXmlFile, keyboardXmlFile)) {
      undoStack.clear();
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
      CustomKeyboardLayoutEditor editor = layoutEditor;
      if (editor != null) editor.showKeyEditDialog(key);
    }

    @Override
    public void onMultiTapStarted() {}

    @Override
    public void onMultiTapEnded() {}

    @Override
    public void onText(Keyboard.Key key, CharSequence text) {
      CustomKeyboardLayoutEditor editor = layoutEditor;
      if (editor != null) editor.showKeyEditDialog(key);
    }

    @Override
    public void onTyping(Keyboard.Key key, CharSequence text) {
      CustomKeyboardLayoutEditor editor = layoutEditor;
      if (editor != null) editor.showKeyEditDialog(key);
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
