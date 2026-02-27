/*
 * Copyright (c) 2025 AnySoftKeyboard
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

package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import wtf.uhoh.newsoftkeyboard.R;

/**
 * DialogFragment wrapper for OpenAI saved prompts management. This provides a proper dialog
 * background and window management.
 */
public class OpenAISavedPromptsDialogFragment extends DialogFragment
    implements OpenAISavedPromptsFragment.OnPromptSelectedListener {

  private OpenAISavedPromptsFragment promptsFragment;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.openai_saved_prompts_dialog_container, container, false);

    // Create and embed the actual prompts fragment
    if (getChildFragmentManager().findFragmentById(R.id.prompts_fragment_container) == null) {
      promptsFragment = new OpenAISavedPromptsFragment();
      if (getArguments() != null) {
        promptsFragment.setArguments(getArguments());
      }
      getChildFragmentManager()
          .beginTransaction()
          .replace(R.id.prompts_fragment_container, promptsFragment)
          .commit();
    } else {
      // If fragment already exists, set up the listener
      promptsFragment =
          (OpenAISavedPromptsFragment)
              getChildFragmentManager().findFragmentById(R.id.prompts_fragment_container);
    }

    // Listener will be set in onResume to ensure fragment is ready

    return view;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);

    // Request a window feature to set the title
    if (dialog.getWindow() != null) {
      dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }

    return dialog;
  }

  @Override
  public void onStart() {
    super.onStart();

    // Set dialog properties for proper background and sizing
    if (getDialog() != null && getDialog().getWindow() != null) {
      Window window = getDialog().getWindow();

      // Set a solid background
      window.setBackgroundDrawableResource(android.R.color.white);

      // Set dialog size to match parent width and wrap height
      window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      // Add some margin for better appearance
      window.getDecorView().setPadding(32, 32, 32, 32);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // Set up the listener when fragment is resumed
    if (promptsFragment == null) {
      promptsFragment =
          (OpenAISavedPromptsFragment)
              getChildFragmentManager().findFragmentById(R.id.prompts_fragment_container);
    }

    if (promptsFragment != null) {
      promptsFragment.setListener(this);
    }
  }

  @Override
  public void onPromptSelected(String promptText) {
    dismiss();

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              if (getActivity() instanceof MainSettingsActivity) {
                ((MainSettingsActivity) getActivity()).navigateToOpenAISettings(promptText);
              }
            });
  }
}
