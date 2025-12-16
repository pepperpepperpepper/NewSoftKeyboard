/*
 * Copyright (c) 2025 The NewSoftKeyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.keyboards.views;

/** Handles preview dismissal and touch cancel during input view reset/detach. */
class InputResetter {

  private final KeyPreviewManagerFacade keyPreviewManager;
  private final KeyPressTimingHandler keyPressTimingHandler;
  private final TouchDispatcher touchDispatcher;

  InputResetter(
      KeyPreviewManagerFacade keyPreviewManager,
      KeyPressTimingHandler keyPressTimingHandler,
      TouchDispatcher touchDispatcher) {
    this.keyPreviewManager = keyPreviewManager;
    this.keyPressTimingHandler = keyPressTimingHandler;
    this.touchDispatcher = touchDispatcher;
  }

  boolean resetInputView() {
    keyPreviewManager.dismissAll();
    keyPressTimingHandler.cancelAllMessages();
    touchDispatcher.cancelAllPointers();
    return false;
  }

  void onStartTemporaryDetach() {
    keyPreviewManager.dismissAll();
    keyPressTimingHandler.cancelAllMessages();
  }
}
