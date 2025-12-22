package com.anysoftkeyboard.debug;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.menny.android.anysoftkeyboard.R;

public class TestInputActivity extends Activity {
  private static volatile boolean sLastShowResult = false;
  private EditText mEditText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_input);
    mEditText = findViewById(R.id.test_edit_text);
    mEditText.setOnFocusChangeListener(
        (view, hasFocus) -> {
          if (hasFocus) {
            showKeyboardSoon();
          }
        });
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
