package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import com.anysoftkeyboard.api.KeyCodes;
import io.reactivex.Single;
import io.reactivex.disposables.SerialDisposable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.evendanan.pixel.GeneralDialogController;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.Clipboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public abstract class ImeClipboard extends ImeSwipeListener {

  private boolean mArrowSelectionState;
  private Clipboard mClipboard;
  protected static final int MAX_CHARS_PER_CODE_POINT = 2;
  private static final long MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_ENTRY = 15 * 1000;
  private static final long MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT = 120 * 1000;
  private long mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
  private boolean mOsClipboardSyncEnabled = false;
  private boolean mClipboardActionAlwaysVisible = false;
  private final SerialDisposable mClipboardTextAutoHideDisposable = new SerialDisposable();
  private final SerialDisposable mClipboardActionAutoHideDisposable = new SerialDisposable();
  private final Clipboard.ClipboardUpdatedListener mClipboardUpdatedListener =
      new Clipboard.ClipboardUpdatedListener() {
        @Override
        public void onClipboardEntryAdded(@NonNull CharSequence label) {
          onClipboardEntryChanged(label);
        }

        @Override
        public void onClipboardCleared() {
          onClipboardEntryChanged(null);
        }
      };

  @Nullable private CharSequence mLastSyncedClipboardLabel;

  @VisibleForTesting
  protected interface ClipboardActionOwner
      extends wtf.uhoh.newsoftkeyboard.app.ime.ClipboardStripActionProvider.ClipboardActionOwner {}

  @VisibleForTesting
  protected static class ClipboardStripActionProvider
      extends wtf.uhoh.newsoftkeyboard.app.ime.ClipboardStripActionProvider {
    ClipboardStripActionProvider(@NonNull ClipboardActionOwner owner) {
      super(owner);
    }
  }

  @VisibleForTesting
  protected final ClipboardActionOwner mClipboardActionOwnerImpl =
      new ClipboardActionOwner() {
        @NonNull
        @Override
        public Context getContext() {
          return ImeClipboard.this;
        }

        @Override
        public void outputClipboardText() {
          ImeClipboard.this.performPaste();
          mSuggestionClipboardEntry.setAsHint(false);
        }

        @Override
        public void showAllClipboardOptions() {
          ImeClipboard.this.showAllClipboardEntries(null);
          mSuggestionClipboardEntry.setAsHint(false);
        }
      };

  @VisibleForTesting protected ClipboardStripActionProvider mSuggestionClipboardEntry;

  @Override
  public void onCreate() {
    super.onCreate();
    mClipboard = NskApplicationBase.getDeviceSpecific().createClipboard(getApplicationContext());
    mSuggestionClipboardEntry = new ClipboardStripActionProvider(mClipboardActionOwnerImpl);
    addDisposable(mClipboardTextAutoHideDisposable);
    addDisposable(mClipboardActionAutoHideDisposable);
    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_os_clipboard_sync, R.bool.settings_default_os_clipboard_sync)
            .asObservable()
            .distinctUntilChanged()
            .subscribe(
                syncClipboard -> {
                  mOsClipboardSyncEnabled = syncClipboard;
                  mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
                  mLastSyncedClipboardLabel = null;
                  cancelClipboardActionAutoHide();
                  cancelClipboardTextAutoHide();
                  mClipboard.setClipboardUpdatedListener(
                      syncClipboard ? mClipboardUpdatedListener : null);
                  final var inputViewContainer = getInputViewContainer();
                  if (!syncClipboard && inputViewContainer != null) {
                    inputViewContainer.removeStripAction(mSuggestionClipboardEntry);
                  }
                },
                GenericOnError.onError("settings_key_os_clipboard_sync")));
    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_clipboard_action_always_visible,
                R.bool.settings_default_clipboard_action_always_visible)
            .asObservable()
            .distinctUntilChanged()
            .subscribe(
                alwaysVisible -> {
                  mClipboardActionAlwaysVisible = alwaysVisible;
                  if (alwaysVisible) {
                    cancelClipboardActionAutoHide();
                  } else {
                    scheduleClipboardActionAutoHideIfNeeded();
                  }
                  updateClipboardActionIconVisibility(currentInputEditorInfo());
                },
                GenericOnError.onError("settings_key_clipboard_action_always_visible")));
  }

  public void clearClipboardHistoryForProgrammableApi() {
    if (mClipboard != null) {
      mClipboard.deleteAllEntries();
    }
  }

  private void onClipboardEntryChanged(@Nullable CharSequence clipboardEntry) {
    if (TextUtils.isEmpty(clipboardEntry)) {
      mLastSyncedClipboardLabel = null;
      mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
      cancelClipboardActionAutoHide();
      cancelClipboardTextAutoHide();
      updateClipboardActionIconVisibility(currentInputEditorInfo());
    } else {
      mLastSyncedClipboardLabel = clipboardEntry;
      mLastSyncedClipboardEntryTime = SystemClock.uptimeMillis();
      // if we already showing the view, we want to update it contents
      if (isInputViewShown()) {
        updateClipboardActionIconVisibility(currentInputEditorInfo());
      }
      scheduleClipboardTextAutoHide();
      if (!mClipboardActionAlwaysVisible) {
        scheduleClipboardActionAutoHideIfNeeded();
      }
    }
  }

  private boolean shouldShowClipboardActionIcon() {
    if (!mOsClipboardSyncEnabled || mClipboard == null) return false;
    if (mClipboardActionAlwaysVisible) {
      return !mClipboard.isOsClipboardEmpty() || mClipboard.getClipboardEntriesCount() > 0;
    }
    return mLastSyncedClipboardEntryTime + MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT
            > SystemClock.uptimeMillis()
        && !TextUtils.isEmpty(mLastSyncedClipboardLabel);
  }

  private void updateClipboardActionIconVisibility(@Nullable EditorInfo info) {
    // This method can be called before the IM view is created, while the service is already alive.
    final var inputViewContainer = getInputViewContainer();
    if (inputViewContainer == null) return;

    if (!shouldShowClipboardActionIcon()) {
      inputViewContainer.removeStripAction(mSuggestionClipboardEntry);
      return;
    }

    inputViewContainer.addStripAction(mSuggestionClipboardEntry, true);
    inputViewContainer.setActionsStripVisibility(true);

    if (!TextUtils.isEmpty(mLastSyncedClipboardLabel)) {
      mSuggestionClipboardEntry.setClipboardText(
          mLastSyncedClipboardLabel, isTextPassword(info) || isNumberPassword(info));
      if (mLastSyncedClipboardEntryTime + MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_ENTRY
          <= SystemClock.uptimeMillis()) {
        mSuggestionClipboardEntry.setAsHint(true);
      }
    } else {
      // Keep the icon visible, but hide any previous text hint.
      mSuggestionClipboardEntry.setAsHint(true);
    }
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    updateClipboardActionIconVisibility(info);
    scheduleClipboardActionAutoHideIfNeeded();
  }

  protected static boolean isTextPassword(@Nullable EditorInfo info) {
    if (info == null) return false;

    final int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
    if (inputClass != 0 && inputClass != EditorInfo.TYPE_CLASS_TEXT) return false;

    final int variation = info.inputType & EditorInfo.TYPE_MASK_VARIATION;
    return variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
        || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
  }

  protected static boolean isNumberPassword(@Nullable EditorInfo info) {
    if (info == null) return false;
    final int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
    if (inputClass != EditorInfo.TYPE_CLASS_NUMBER) return false;
    final int variation = info.inputType & EditorInfo.TYPE_MASK_VARIATION;
    return variation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD;
  }

  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    if (mSuggestionClipboardEntry.isVisible()) {
      if (mClipboardActionAlwaysVisible) {
        // Keep the clipboard icon visible, but hide the preview text once the user starts typing.
        mSuggestionClipboardEntry.setAsHint(false);
      } else {
        // Default behavior: hide the clipboard action once the user starts typing to avoid clutter.
        mLastSyncedClipboardLabel = null;
        mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
        cancelClipboardActionAutoHide();
        cancelClipboardTextAutoHide();
        updateClipboardActionIconVisibility(currentInputEditorInfo());
      }
    }
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    getInputViewContainer().removeStripAction(mSuggestionClipboardEntry);
  }

  private void scheduleClipboardTextAutoHide() {
    cancelClipboardTextAutoHide();
    mClipboardTextAutoHideDisposable.set(
        Single.timer(
                MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_ENTRY,
                TimeUnit.MILLISECONDS,
                RxSchedulers.mainThread())
            .subscribe(
                ignored -> {
                  if (!mSuggestionClipboardEntry.isVisible()) return;
                  if (!mSuggestionClipboardEntry.isFullyVisible()) return;
                  mSuggestionClipboardEntry.setAsHint(false);
                },
                GenericOnError.onError("scheduleClipboardTextAutoHide")));
  }

  private void cancelClipboardTextAutoHide() {
    mClipboardTextAutoHideDisposable.set(io.reactivex.disposables.Disposables.disposed());
  }

  private void scheduleClipboardActionAutoHideIfNeeded() {
    cancelClipboardActionAutoHide();
    if (mClipboardActionAlwaysVisible) return;
    if (mLastSyncedClipboardEntryTime == Long.MIN_VALUE) return;
    final long now = SystemClock.uptimeMillis();
    final long hideAt = mLastSyncedClipboardEntryTime + MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT;
    final long delay = hideAt - now;
    if (delay <= 0) return;

    mClipboardActionAutoHideDisposable.set(
        Single.timer(delay, TimeUnit.MILLISECONDS, RxSchedulers.mainThread())
            .subscribe(
                ignored -> updateClipboardActionIconVisibility(currentInputEditorInfo()),
                GenericOnError.onError("scheduleClipboardActionAutoHideIfNeeded")));
  }

  private void cancelClipboardActionAutoHide() {
    mClipboardActionAutoHideDisposable.set(io.reactivex.disposables.Disposables.disposed());
  }

  private void showAllClipboardEntries(Keyboard.Key key) {
    int entriesCount = mClipboard.getClipboardEntriesCount();
    if (entriesCount == 0) {
      showToastMessage(R.string.clipboard_is_empty_toast, true);
    } else {
      final List<CharSequence> nonEmpties = new ArrayList<>(entriesCount);
      for (int entryIndex = 0; entryIndex < entriesCount; entryIndex++) {
        nonEmpties.add(mClipboard.getText(entryIndex));
      }
      final CharSequence[] entries = nonEmpties.toArray(new CharSequence[0]);
      DialogInterface.OnClickListener onClickListener =
          (dialog, which) -> {
            if (which == 0 && !mClipboard.isOsClipboardEmpty()) {
              performPaste();
            } else {
              onText(key, entries[which]);
            }
          };
      showOptionsDialogWithData(
          R.string.clipboard_paste_entries_title,
          R.drawable.ic_clipboard_paste_in_app,
          new CharSequence[0],
          onClickListener,
          new GeneralDialogController.DialogPresenter() {
            @Override
            public void beforeDialogShown(@NonNull AlertDialog dialog, @Nullable Object data) {}

            @Override
            public void onSetupDialogRequired(
                Context context, AlertDialog.Builder builder, int optionId, @Nullable Object data) {
              builder.setNeutralButton(
                  R.string.delete_all_clipboard_entries,
                  (dialog, which) -> {
                    mClipboard.deleteAllEntries();
                    dialog.dismiss();
                  });
              builder.setAdapter(new ClipboardEntriesAdapter(context, entries), onClickListener);
            }
          });
    }
  }

  private void performPaste() {
    if (mClipboard.isOsClipboardEmpty()) {
      showToastMessage(R.string.clipboard_is_empty_toast, true);
    } else {
      // let the OS perform the paste (it may be a complex clip, better not handle it)
      sendDownUpKeyEvents(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
    }
  }

  private void performCopy(boolean alsoCut) {
    if (alsoCut) {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON);
    } else {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON);
      // showing toast, since there isn't any other UI feedback
      // starting with Android 33, the OS shows a thing
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        showToastMessage(R.string.clipboard_copy_done_toast, true);
      }
    }
  }

  protected void handleClipboardOperation(
      final Keyboard.Key key, final int primaryCode, InputConnectionRouter inputConnectionRouter) {
    abortCorrectionAndResetPredictionState(false);
    switch (primaryCode) {
      case KeyCodes.CLIPBOARD_PASTE -> performPaste();
      case KeyCodes.CLIPBOARD_CUT, KeyCodes.CLIPBOARD_COPY ->
          performCopy(primaryCode == KeyCodes.CLIPBOARD_CUT);
      case KeyCodes.CLIPBOARD_SELECT_ALL -> {
        if (!inputConnectionRouter.hasConnection()) {
          return;
        }
        final CharSequence toLeft = inputConnectionRouter.getTextBeforeCursor(10240, 0);
        final CharSequence toRight = inputConnectionRouter.getTextAfterCursor(10240, 0);
        final int leftLength = toLeft == null ? 0 : toLeft.length();
        final int rightLength = toRight == null ? 0 : toRight.length();
        if (leftLength != 0 || rightLength != 0) {
          inputConnectionRouter.setSelection(0, leftLength + rightLength);
        }
      }
      case KeyCodes.CLIPBOARD_PASTE_POPUP -> showAllClipboardEntries(key);
      case KeyCodes.CLIPBOARD_SELECT -> {
        mArrowSelectionState = !mArrowSelectionState;
        if (mArrowSelectionState) {
          showToastMessage(R.string.clipboard_fine_select_enabled_toast, true);
        }
      }
      case KeyCodes.UNDO -> sendDownUpKeyEvents(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON);
      case KeyCodes.REDO ->
          sendDownUpKeyEvents(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON);
      default ->
          throw new IllegalArgumentException(
              "The keycode " + primaryCode + " is not covered by handleClipboardOperation!");
    }
  }

  protected boolean handleSelectionExpending(
      int keyEventKeyCode, InputConnectionRouter inputConnectionRouter) {
    if (mArrowSelectionState && inputConnectionRouter.hasConnection()) {
      final int selectionEnd = getCursorPosition();
      final int selectionStart = getSelectionStartPositionDangerous();
      markExpectingSelectionUpdate();
      switch (keyEventKeyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
          // A Unicode code-point can be made up of two Java chars.
          // We check if that's what happening before the cursor:
          final CharSequence toLeftText =
              inputConnectionRouter.getTextBeforeCursor(MAX_CHARS_PER_CODE_POINT, 0);
          final String toLeft = toLeftText == null ? "" : toLeftText.toString();
          if (toLeft.length() == 0) {
            inputConnectionRouter.setSelection(selectionStart, selectionEnd);
          } else {
            inputConnectionRouter.setSelection(
                selectionStart - Character.charCount(toLeft.codePointBefore(toLeft.length())),
                selectionEnd);
          }
          return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          final CharSequence toRightText =
              inputConnectionRouter.getTextAfterCursor(MAX_CHARS_PER_CODE_POINT, 0);
          final String toRight = toRightText == null ? "" : toRightText.toString();
          if (toRight.length() == 0) {
            inputConnectionRouter.setSelection(selectionStart, selectionEnd);
          } else {
            inputConnectionRouter.setSelection(
                selectionStart, selectionEnd + Character.charCount(toRight.codePointAt(0)));
          }
          return true;
        default:
          mArrowSelectionState = false;
      }
    }
    return false;
  }

  @Override
  public void onPress(int primaryCode) {
    if (mArrowSelectionState
        && (primaryCode != KeyCodes.ARROW_LEFT && primaryCode != KeyCodes.ARROW_RIGHT)) {
      mArrowSelectionState = false;
    }
  }

  private class ClipboardEntriesAdapter extends ArrayAdapter<CharSequence> {
    public ClipboardEntriesAdapter(@NonNull Context context, CharSequence[] items) {
      super(context, R.layout.clipboard_dialog_entry, R.id.clipboard_entry_text, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      View view = super.getView(position, convertView, parent);
      View deleteView = view.findViewById(R.id.clipboard_entry_delete);
      deleteView.setTag(R.id.clipboard_entry_delete, position);
      deleteView.setOnClickListener(this::onItemDeleteClicked);

      return view;
    }

    private void onItemDeleteClicked(View view) {
      int position = (int) view.getTag(R.id.clipboard_entry_delete);
      mClipboard.deleteEntry(position);
      closeGeneralOptionsDialog();
    }
  }
}
