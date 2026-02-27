package wtf.uhoh.newsoftkeyboard.app.debug;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import wtf.uhoh.newsoftkeyboard.R;

public class TestInputActivity extends Activity {
  public static final String EXTRA_IME_FLAG_NO_PERSONALIZED_LEARNING =
      "wtf.uhoh.newsoftkeyboard.debug.ime_flag_no_personalized_learning";
  public static final String EXTRA_TYPE_TEXT_FLAG_NO_SUGGESTIONS =
      "wtf.uhoh.newsoftkeyboard.debug.type_text_flag_no_suggestions";
  public static final String EXTRA_SIMULATE_INVISIBLE_COMPOSING =
      "wtf.uhoh.newsoftkeyboard.debug.simulate_invisible_composing";
  public static final String EXTRA_SIMULATE_TYPE_NULL =
      "wtf.uhoh.newsoftkeyboard.debug.simulate_type_null";
  public static final String EXTRA_PREFILL_TEXT = "wtf.uhoh.newsoftkeyboard.debug.prefill_text";
  public static final String EXTRA_PREFILL_CURSOR_POSITION =
      "wtf.uhoh.newsoftkeyboard.debug.prefill_cursor_position";
  public static final String EXTRA_EDITOR_INPUT_TYPE_OVERRIDE =
      "wtf.uhoh.newsoftkeyboard.debug.editor_input_type_override";

  private static volatile boolean sLastShowResult = false;
  private EditText mEditText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_input);
    mEditText = findViewById(R.id.test_edit_text);
    applyEditorFlagOverridesFromIntent(getIntent());
    mEditText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (hasFocus) {
            showKeyboardSoon();
          }
        });
  }

  private void applyEditorFlagOverridesFromIntent(Intent intent) {
    if (intent == null || mEditText == null) return;
    if (mEditText instanceof TestInputEditText) {
      ((TestInputEditText) mEditText)
          .setSimulateInvisibleComposing(
              intent.getBooleanExtra(EXTRA_SIMULATE_INVISIBLE_COMPOSING, false));
      ((TestInputEditText) mEditText)
          .setSimulateTypeNull(intent.getBooleanExtra(EXTRA_SIMULATE_TYPE_NULL, false));
    }

    final int inputTypeOverride = intent.getIntExtra(EXTRA_EDITOR_INPUT_TYPE_OVERRIDE, -1);
    if (inputTypeOverride >= 0) {
      mEditText.setInputType(inputTypeOverride);
    }
    if (intent.getBooleanExtra(EXTRA_IME_FLAG_NO_PERSONALIZED_LEARNING, false)) {
      mEditText.setImeOptions(
          mEditText.getImeOptions() | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
    }
    if (intent.getBooleanExtra(EXTRA_TYPE_TEXT_FLAG_NO_SUGGESTIONS, false)) {
      mEditText.setInputType(mEditText.getInputType() | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    final String prefill = intent.getStringExtra(EXTRA_PREFILL_TEXT);
    if (prefill != null) {
      mEditText.setText(prefill);
      int cursor = intent.getIntExtra(EXTRA_PREFILL_CURSOR_POSITION, -1);
      if (cursor < 0 || cursor > prefill.length()) cursor = prefill.length();
      mEditText.setSelection(cursor);
    }
  }

  public static boolean getLastShowResult() {
    return sLastShowResult;
  }

  @Override
  protected void onResume() {
    super.onResume();
    mEditText.requestFocus();
    showKeyboardSoon();
    mEditText.post(this::showKeyboardSoon);
  }

  public void forceShowKeyboard() {
    showKeyboardSoon();
  }

  private void showKeyboardSoon() {
    if (mEditText == null) {
      return;
    }
    mEditText.post(
        () -> {
          InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          if (imm != null) {
            boolean shown = imm.showSoftInput(mEditText, InputMethodManager.SHOW_FORCED);
            sLastShowResult = shown;
            android.util.Log.d(
                "TestInputActivity", "showSoftInput returned " + shown + " for edit focus.");
            if (!shown) {
              android.util.Log.d("TestInputActivity", "toggleSoftInput fallback triggered.");
              imm.toggleSoftInputFromWindow(
                  mEditText.getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
              sLastShowResult = true;
            }
          }
        });
  }
}
