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

final class ThemeOverrideApplier {

  private ThemeOverrideApplier() {}

  static int caseOverride(String overrideValue) {
    return switch (overrideValue) {
      case "auto" -> 0;
      case "lower" -> 1;
      case "upper" -> 2;
      default -> -1;
    };
  }

  static float hintSizeMultiplier(String overrideValue) {
    return switch (overrideValue) {
      case "none" -> 0f;
      case "small" -> 0.7f;
      case "big" -> 1.3f;
      default -> 1f;
    };
  }
}
