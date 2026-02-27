package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;

public class CustomKeyboardEditorFragment extends Fragment {
  private static final String TAG = "CustomKeyboardEditor";

  private static final String STATE_TEST_TYPING_ENABLED = "testTypingEnabled";
  private static final String STATE_TEST_TYPING_BUFFER = "testTypingBuffer";

  public static final String ARG_PACK_ID = "packId";
  public static final String ARG_KEYBOARD_ENTRY_ID = "keyboardEntryId";
  public static final String ARG_KEYBOARD_PATH = "keyboardPath";

  private KeyboardView keyboardView;
  private TextView instructionsView;
  private SwitchCompat testTypingSwitch;
  private View testTypingContainer;
  private TextView testTypingBufferView;
  private Button testTypingClearButton;
  private TextView validationWarningsView;
  private TextView statusView;
  private Button themeButton;

  @Nullable private CustomKeyboardEditorSessionController sessionController;
  @Nullable private CustomKeyboardThemeEditorController themeController;

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

    themeController =
        new CustomKeyboardThemeEditorController(
            new CustomKeyboardThemeEditorController.Host() {
              @NonNull
              @Override
              public Fragment fragment() {
                return CustomKeyboardEditorFragment.this;
              }

              @NonNull
              @Override
              public KeyboardView keyboardView() {
                return CustomKeyboardEditorFragment.this.keyboardView;
              }

              @NonNull
              @Override
              public TextView statusView() {
                return CustomKeyboardEditorFragment.this.statusView;
              }

              @Nullable
              @Override
              public InstalledKeyboardPack pack() {
                CustomKeyboardEditorSessionController controller = sessionController;
                return controller != null ? controller.pack() : null;
              }

              @Override
              public void setPack(@NonNull InstalledKeyboardPack pack) {
                CustomKeyboardEditorSessionController controller = sessionController;
                if (controller != null) controller.setPack(pack);
              }
            });
    sessionController =
        new CustomKeyboardEditorSessionController(
            new CustomKeyboardEditorSessionController.Host() {
              @NonNull
              @Override
              public Fragment fragment() {
                return CustomKeyboardEditorFragment.this;
              }

              @NonNull
              @Override
              public KeyboardView keyboardView() {
                return CustomKeyboardEditorFragment.this.keyboardView;
              }

              @NonNull
              @Override
              public TextView instructionsView() {
                return CustomKeyboardEditorFragment.this.instructionsView;
              }

              @NonNull
              @Override
              public SwitchCompat testTypingSwitch() {
                return CustomKeyboardEditorFragment.this.testTypingSwitch;
              }

              @NonNull
              @Override
              public View testTypingContainer() {
                return CustomKeyboardEditorFragment.this.testTypingContainer;
              }

              @NonNull
              @Override
              public TextView testTypingBufferView() {
                return CustomKeyboardEditorFragment.this.testTypingBufferView;
              }

              @NonNull
              @Override
              public Button testTypingClearButton() {
                return CustomKeyboardEditorFragment.this.testTypingClearButton;
              }

              @NonNull
              @Override
              public TextView validationWarningsView() {
                return CustomKeyboardEditorFragment.this.validationWarningsView;
              }

              @NonNull
              @Override
              public TextView statusView() {
                return CustomKeyboardEditorFragment.this.statusView;
              }

              @Override
              public void applyPersistedThemeIfAny() {
                CustomKeyboardThemeEditorController controller = themeController;
                if (controller != null) controller.applyPersistedThemeIfAny();
              }
            });

    themeButton.setOnClickListener(
        v -> {
          CustomKeyboardThemeEditorController controller = themeController;
          if (controller != null) controller.showThemeActionsDialog();
        });

    // CHECKSTYLE:OFF: RawGetKeyboardTheme
    keyboardView.setKeyboardTheme(
        NskApplicationBase.getKeyboardThemeFactory(requireContext()).getEnabledAddOn());
    // CHECKSTYLE:ON: RawGetKeyboardTheme

    boolean restoredTestTypingEnabled =
        savedInstanceState != null
            && savedInstanceState.getBoolean(STATE_TEST_TYPING_ENABLED, false);
    String restoredTestTypingBuffer =
        savedInstanceState != null
            ? savedInstanceState.getString(STATE_TEST_TYPING_BUFFER, "")
            : "";
    if (TextUtils.isEmpty(restoredTestTypingBuffer)) restoredTestTypingBuffer = "";

    CustomKeyboardEditorSessionController controller = sessionController;
    if (controller != null) {
      controller.onViewCreated(restoredTestTypingEnabled, restoredTestTypingBuffer);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    CustomKeyboardEditorSessionController controller = sessionController;
    if (controller != null) controller.onStart();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    CustomKeyboardEditorSessionController controller = sessionController;
    if (controller == null) return;
    outState.putBoolean(STATE_TEST_TYPING_ENABLED, controller.testTypingEnabled());
    outState.putString(STATE_TEST_TYPING_BUFFER, controller.testTypingBuffer());
  }
}
