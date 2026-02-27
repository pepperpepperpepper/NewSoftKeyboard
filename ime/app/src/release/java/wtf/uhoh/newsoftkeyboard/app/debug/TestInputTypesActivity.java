package wtf.uhoh.newsoftkeyboard.app.debug;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import wtf.uhoh.newsoftkeyboard.R;

public class TestInputTypesActivity extends Activity {
  private static volatile boolean sLastShowResult = false;
  private EditText mPlainEditText;
  private EditText mNoSuggestionsEditText;
  private EditText mClasslessNoSuggestionsEditText;
  private EditText mTypeNullEditText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_input_types);
    mPlainEditText = findViewById(R.id.test_edit_text_plain);
    mNoSuggestionsEditText = findViewById(R.id.test_edit_text_no_suggestions);
    mClasslessNoSuggestionsEditText = findViewById(R.id.test_edit_text_classless_no_suggestions);
    mTypeNullEditText = findViewById(R.id.test_edit_text_type_null);

    mPlainEditText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (hasFocus) showKeyboardSoon(mPlainEditText);
        });
    mNoSuggestionsEditText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (hasFocus) showKeyboardSoon(mNoSuggestionsEditText);
        });
    mClasslessNoSuggestionsEditText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (hasFocus) showKeyboardSoon(mClasslessNoSuggestionsEditText);
        });
    mTypeNullEditText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (hasFocus) showKeyboardSoon(mTypeNullEditText);
        });
  }

  public static boolean getLastShowResult() {
    return sLastShowResult;
  }

  @Override
  protected void onResume() {
    super.onResume();
    mPlainEditText.requestFocus();
    showKeyboardSoon(mPlainEditText);
    mPlainEditText.post(() -> showKeyboardSoon(mPlainEditText));
  }

  public void forceShowKeyboard() {
    EditText focused = null;
    try {
      if (getCurrentFocus() instanceof EditText edit) focused = edit;
    } catch (Throwable ignored) {
    }
    showKeyboardSoon(focused != null ? focused : mPlainEditText);
  }

  private void showKeyboardSoon(EditText editText) {
    if (editText == null) {
      return;
    }
    editText.post(
        () -> {
          InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          if (imm != null) {
            boolean shown = imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
            sLastShowResult = shown;
            android.util.Log.d(
                "TestInputTypesActivity", "showSoftInput returned " + shown + " for edit focus.");
            if (!shown) {
              android.util.Log.d("TestInputTypesActivity", "toggleSoftInput fallback triggered.");
              imm.toggleSoftInputFromWindow(
                  editText.getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
              sLastShowResult = true;
            }
          }
        });
  }
}
