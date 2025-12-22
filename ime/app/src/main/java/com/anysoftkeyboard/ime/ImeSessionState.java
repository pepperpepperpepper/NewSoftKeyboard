package com.anysoftkeyboard.ime;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Single source of truth for the active IME editor session state.
 *
 * <p>Owns:
 *
 * <ul>
 *   <li>Current {@link EditorInfo} snapshot
 *   <li>Selection/cursor tracking ({@link EditorStateTracker})
 *   <li>Current {@link InputConnection} access ({@link InputConnectionRouter})
 * </ul>
 *
 * This is intentionally small: it centralizes state while letting higher-level IME components keep
 * policy/behavior.
 */
public final class ImeSessionState {

  private final InputConnectionRouter inputConnectionRouter;
  private final BooleanSupplier selectionUpdateDelayed;
  private final EditorStateTracker editorStateTracker = new EditorStateTracker();

  @Nullable private EditorInfo currentEditorInfo;

  public ImeSessionState(
      @NonNull Supplier<InputConnection> inputConnectionSupplier,
      @NonNull BooleanSupplier selectionUpdateDelayed) {
    this.inputConnectionRouter = new InputConnectionRouter(inputConnectionSupplier);
    this.selectionUpdateDelayed = selectionUpdateDelayed;
  }

  public void onStartInput(@Nullable EditorInfo editorInfo) {
    currentEditorInfo = editorInfo;
  }

  public void onFinishInput() {
    currentEditorInfo = null;
    editorStateTracker.reset();
  }

  @Nullable
  public EditorInfo currentEditorInfo() {
    return currentEditorInfo;
  }

  @NonNull
  public InputConnectionRouter getInputConnectionRouter() {
    return inputConnectionRouter;
  }

  @Nullable
  public InputConnection currentInputConnection() {
    return inputConnectionRouter.current();
  }

  @Nullable
  public ExtractedText getExtractedText(@NonNull ExtractedTextRequest request) {
    final InputConnection connection = currentInputConnection();
    if (connection == null) {
      return null;
    }
    return connection.getExtractedText(request, 0);
  }

  public int getCursorPositionDangerous() {
    return editorStateTracker.getCursorPosition(
        selectionUpdateDelayed.getAsBoolean(), currentInputConnection());
  }

  public int getSelectionStartPositionDangerous() {
    return editorStateTracker.getSelectionStart();
  }

  public int getCandidateStartPositionDangerous() {
    return editorStateTracker.getCandidateStart();
  }

  public int getCandidateEndPositionDangerous() {
    return editorStateTracker.getCandidateEnd();
  }

  public void onUpdateSelection(
      int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
    editorStateTracker.setCursorAndSelection(newSelEnd, newSelStart);
    editorStateTracker.setCandidateRange(candidatesStart, candidatesEnd);
  }
}
