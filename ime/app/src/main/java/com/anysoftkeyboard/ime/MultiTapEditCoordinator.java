package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import java.util.function.Consumer;

/**
 * Handles the small batch-edit window around multi-tap interactions so the key logic stays focused
 * in {@link AnySoftKeyboard}.
 */
public final class MultiTapEditCoordinator {

  private final InputConnectionRouter router;

  public MultiTapEditCoordinator(InputConnectionRouter router) {
    this.router = router;
  }

  public void onMultiTapStarted(Runnable beforeSuper) {
    final InputConnection ic = router.current();
    router.beginBatchEdit();
    beforeSuper.run();
  }

  public void onMultiTapEnded(Runnable afterSuper) {
    router.endBatchEdit();
    afterSuper.run();
  }
}
