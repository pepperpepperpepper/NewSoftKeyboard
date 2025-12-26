package wtf.uhoh.newsoftkeyboard.app.testing;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService;

@Implements(value = InputMethodManager.class)
public final class InputMethodManagerShadow
    extends org.robolectric.shadows.ShadowInputMethodManager {

  private boolean mInitialized;
  private boolean mStatusIconShown;
  private String mLastStatusIconPackageName;
  private int mLastStatusIconId;
  private IBinder mLastStatusIconImeToken;

  @SuppressWarnings("this-escape")
  private void ensureInitialized() {
    if (mInitialized) return;
    // adding three IMEs: NSK, Google, and AOSP (disabled)
    final List<InputMethodInfo> inputMethodInfos = new ArrayList<>();
    final List<InputMethodInfo> enabledInputMethods = new ArrayList<>();

    final InputMethodInfo nskIme =
        new InputMethodInfo(
            BuildConfig.APPLICATION_ID,
            TestableImeService.class.getName(),
            "New Soft Keyboard",
            ".MainSettingsActivity");
    final InputMethodInfo gBoardIme =
        new InputMethodInfo(
            "com.google.keyboard", "GoogleKeyboard", "GoogleKeyboard", ".MainSettingsActivity");
    final InputMethodInfo aospIme =
        new InputMethodInfo(
            "android.ime.KeyboardService",
            "SoftKeyboard",
            "AOSP Keyboard",
            ".MainSettingsActivity");

    inputMethodInfos.add(nskIme);
    enabledInputMethods.add(nskIme);
    inputMethodInfos.add(gBoardIme);
    enabledInputMethods.add(gBoardIme);
    inputMethodInfos.add(aospIme);

    super.setInputMethodInfoList(ImmutableList.copyOf(inputMethodInfos));
    super.setEnabledInputMethodInfoList(ImmutableList.copyOf(enabledInputMethods));
    mInitialized = true;
  }

  @Implementation
  public List<InputMethodInfo> getInputMethodList() {
    ensureInitialized();
    return super.getInputMethodList();
  }

  @Implementation
  public List<InputMethodInfo> getEnabledInputMethodList() {
    ensureInitialized();
    return super.getEnabledInputMethodList();
  }

  public static void setKeyboardEnabled(Context context, boolean enabled) {
    InputMethodManager imeService =
        (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
    var inputMethodManagerShadow = (InputMethodManagerShadow) Shadow.extract(imeService);
    List<InputMethodInfo> allInputs = imeService.getInputMethodList();
    inputMethodManagerShadow.setEnabledInputMethodInfoList(
        allInputs.stream()
            .filter(
                ime -> enabled || !Objects.equals(ime.getPackageName(), context.getPackageName()))
            .toList());
  }

  public static void setKeyboardAsCurrent(Context context, boolean isCurrent) {
    // TODO support API 34
    var currentFlat =
        isCurrent
            ? new ComponentName(BuildConfig.APPLICATION_ID, NewSoftKeyboardService.class.getName())
                .flattenToString()
            : new ComponentName("com.example", ".OtherSoftKeyboard").flattenToString();
    Settings.Secure.putString(
        context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, currentFlat);
  }

  @Implementation
  public void showStatusIcon(IBinder imeToken, String packageName, int iconId) {
    mLastStatusIconImeToken = imeToken;
    mLastStatusIconPackageName = packageName;
    mLastStatusIconId = iconId;
    mStatusIconShown = true;
  }

  public void clearStatusIconDetails() {
    mLastStatusIconImeToken = null;
    mLastStatusIconPackageName = null;
    mLastStatusIconId = 0;
  }

  @Implementation
  public void hideStatusIcon(IBinder imeToken) {
    mLastStatusIconImeToken = imeToken;
    mStatusIconShown = false;
  }

  public boolean isStatusIconShown() {
    return mStatusIconShown;
  }

  public String getLastStatusIconPackageName() {
    return mLastStatusIconPackageName;
  }

  public int getLastStatusIconId() {
    return mLastStatusIconId;
  }

  public IBinder getLastStatusIconImeToken() {
    return mLastStatusIconImeToken;
  }
}
