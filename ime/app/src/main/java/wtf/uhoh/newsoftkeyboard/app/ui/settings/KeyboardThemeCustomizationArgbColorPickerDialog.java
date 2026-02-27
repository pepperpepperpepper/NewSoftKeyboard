package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import wtf.uhoh.newsoftkeyboard.R;

final class KeyboardThemeCustomizationArgbColorPickerDialog {
  private KeyboardThemeCustomizationArgbColorPickerDialog() {}

  private static final String EXTRA_LINKED_SOURCE_LABEL = "nsk.color_picker.linked_source_label";
  private static final String EXTRA_LINKED_COLOR_ARGB = "nsk.color_picker.linked_color_argb";

  static void setLinkedValueInfo(
      @NonNull EditTextPreference preference,
      @Nullable String sourceLabel,
      @Nullable Integer argb) {
    final Bundle extras = preference.getExtras();
    extras.remove(EXTRA_LINKED_SOURCE_LABEL);
    extras.remove(EXTRA_LINKED_COLOR_ARGB);
    if (sourceLabel != null) extras.putString(EXTRA_LINKED_SOURCE_LABEL, sourceLabel);
    if (argb != null) extras.putInt(EXTRA_LINKED_COLOR_ARGB, argb);
  }

  static void show(@NonNull Context context, @NonNull EditTextPreference preference) {
    final String initialRaw = preference.getText() == null ? "" : preference.getText().trim();
    final boolean initialUseThemeDefault = TextUtils.isEmpty(initialRaw);
    int initialColor = Color.WHITE;
    if (!initialUseThemeDefault) {
      try {
        initialColor = Color.parseColor(initialRaw);
      } catch (IllegalArgumentException ignored) {
        initialColor = Color.WHITE;
      }
    }

    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    final int padding = dpToPx(context, 16);
    root.setPadding(padding, padding, padding, padding);

    final CheckBox useThemeDefault = new CheckBox(context);
    useThemeDefault.setText(R.string.keyboard_theme_color_picker_use_linked_value);
    useThemeDefault.setChecked(initialUseThemeDefault);
    root.addView(
        useThemeDefault,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final Bundle extras = preference.getExtras();
    final String linkedSourceLabel = extras.getString(EXTRA_LINKED_SOURCE_LABEL);
    final boolean hasLinkedColor = extras.containsKey(EXTRA_LINKED_COLOR_ARGB);
    final int linkedColor = extras.getInt(EXTRA_LINKED_COLOR_ARGB, 0);

    final TextView linkedInfo = new TextView(context);
    if (linkedSourceLabel == null) {
      linkedInfo.setVisibility(View.GONE);
    } else {
      linkedInfo.setVisibility(View.VISIBLE);
      linkedInfo.setText(
          hasLinkedColor
              ? context.getString(
                  R.string.keyboard_theme_color_picker_linked_value_with_color,
                  linkedSourceLabel,
                  formatColor(linkedColor))
              : context.getString(
                  R.string.keyboard_theme_color_picker_linked_value, linkedSourceLabel));
      linkedInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }
    final LinearLayout.LayoutParams linkedInfoParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    linkedInfoParams.topMargin = dpToPx(context, 2);
    root.addView(linkedInfo, linkedInfoParams);

    final View swatch = new View(context);
    final LinearLayout.LayoutParams swatchParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 40));
    swatchParams.topMargin = dpToPx(context, 8);
    swatch.setLayoutParams(swatchParams);
    root.addView(swatch);

    final EditText hexEdit = new EditText(context);
    hexEdit.setSingleLine(true);
    hexEdit.setInputType(InputType.TYPE_CLASS_TEXT);
    hexEdit.setHint(R.string.keyboard_theme_color_picker_hex_hint);
    root.addView(
        hexEdit,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final TextView errorText = new TextView(context);
    errorText.setText(R.string.keyboard_theme_appearance_invalid_color_toast);
    errorText.setTextColor(Color.RED);
    errorText.setVisibility(View.GONE);
    root.addView(
        errorText,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final ColorChannelControls alpha =
        addColorChannelRow(
            context,
            root,
            R.string.keyboard_theme_color_picker_alpha_title,
            (initialColor >>> 24) & 0xFF);
    final ColorChannelControls red =
        addColorChannelRow(
            context,
            root,
            R.string.keyboard_theme_color_picker_red_title,
            (initialColor >>> 16) & 0xFF);
    final ColorChannelControls green =
        addColorChannelRow(
            context,
            root,
            R.string.keyboard_theme_color_picker_green_title,
            (initialColor >>> 8) & 0xFF);
    final ColorChannelControls blue =
        addColorChannelRow(
            context, root, R.string.keyboard_theme_color_picker_blue_title, initialColor & 0xFF);

    final boolean[] ignoreHexChanges = new boolean[] {false};
    final boolean[] ignoreSeekChanges = new boolean[] {false};
    final int[] currentColor = new int[] {initialColor};

    final Runnable updateFromChannels =
        () -> {
          if (useThemeDefault.isChecked()) return;
          final int a = alpha.seekBar.getProgress();
          final int r = red.seekBar.getProgress();
          final int g = green.seekBar.getProgress();
          final int b = blue.seekBar.getProgress();
          currentColor[0] = Color.argb(a, r, g, b);
          swatch.setBackground(new ColorDrawable(currentColor[0]));
          errorText.setVisibility(View.GONE);
          ignoreHexChanges[0] = true;
          hexEdit.setText(formatColor(currentColor[0]));
          ignoreHexChanges[0] = false;
        };

    final SeekBar.OnSeekBarChangeListener channelListener =
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            alpha.valueView.setText(String.valueOf(alpha.seekBar.getProgress()));
            red.valueView.setText(String.valueOf(red.seekBar.getProgress()));
            green.valueView.setText(String.valueOf(green.seekBar.getProgress()));
            blue.valueView.setText(String.valueOf(blue.seekBar.getProgress()));
            if (ignoreSeekChanges[0]) return;
            updateFromChannels.run();
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    alpha.seekBar.setOnSeekBarChangeListener(channelListener);
    red.seekBar.setOnSeekBarChangeListener(channelListener);
    green.seekBar.setOnSeekBarChangeListener(channelListener);
    blue.seekBar.setOnSeekBarChangeListener(channelListener);

    hexEdit.addTextChangedListener(
        new android.text.TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(android.text.Editable s) {
            if (ignoreHexChanges[0] || useThemeDefault.isChecked()) return;
            final String raw = s == null ? "" : s.toString().trim();
            if (raw.isEmpty()) {
              errorText.setVisibility(View.GONE);
              return;
            }
            try {
              final int parsed = Color.parseColor(raw);
              currentColor[0] = parsed;
              swatch.setBackground(new ColorDrawable(parsed));
              errorText.setVisibility(View.GONE);

              ignoreSeekChanges[0] = true;
              alpha.seekBar.setProgress((parsed >>> 24) & 0xFF);
              red.seekBar.setProgress((parsed >>> 16) & 0xFF);
              green.seekBar.setProgress((parsed >>> 8) & 0xFF);
              blue.seekBar.setProgress(parsed & 0xFF);
              ignoreSeekChanges[0] = false;
            } catch (IllegalArgumentException e) {
              errorText.setVisibility(View.VISIBLE);
            }
          }
        });

    final Runnable updateEnabledState =
        () -> {
          final boolean enabled = !useThemeDefault.isChecked();
          hexEdit.setEnabled(enabled);
          alpha.seekBar.setEnabled(enabled);
          red.seekBar.setEnabled(enabled);
          green.seekBar.setEnabled(enabled);
          blue.seekBar.setEnabled(enabled);
          if (!enabled) {
            errorText.setVisibility(View.GONE);
            swatch.setBackground(null);
            ignoreHexChanges[0] = true;
            hexEdit.setText("");
            ignoreHexChanges[0] = false;
          } else {
            swatch.setBackground(new ColorDrawable(currentColor[0]));
            ignoreHexChanges[0] = true;
            hexEdit.setText(formatColor(currentColor[0]));
            ignoreHexChanges[0] = false;
          }
        };
    useThemeDefault.setOnCheckedChangeListener((buttonView, isChecked) -> updateEnabledState.run());

    // Initial view state.
    if (initialUseThemeDefault) {
      updateEnabledState.run();
    } else {
      swatch.setBackground(new ColorDrawable(initialColor));
      ignoreHexChanges[0] = true;
      hexEdit.setText(formatColor(initialColor));
      ignoreHexChanges[0] = false;
    }

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(preference.getTitle())
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setNeutralButton(R.string.keyboard_theme_color_picker_reset_to_linked_value, null)
            .setPositiveButton(android.R.string.ok, null)
            .create();

    dialog.setOnShowListener(
        ignored -> {
          dialog
              .getButton(AlertDialog.BUTTON_POSITIVE)
              .setOnClickListener(
                  v -> {
                    final String nextValue;
                    if (useThemeDefault.isChecked()) {
                      nextValue = "";
                    } else {
                      final String raw =
                          hexEdit.getText() == null ? "" : hexEdit.getText().toString().trim();
                      if (raw.isEmpty()) {
                        Toast.makeText(
                                context,
                                R.string.keyboard_theme_appearance_invalid_color_toast,
                                Toast.LENGTH_SHORT)
                            .show();
                        return;
                      }
                      try {
                        Color.parseColor(raw);
                      } catch (IllegalArgumentException e) {
                        Toast.makeText(
                                context,
                                R.string.keyboard_theme_appearance_invalid_color_toast,
                                Toast.LENGTH_SHORT)
                            .show();
                        return;
                      }
                      nextValue = raw;
                    }

                    if (preference.callChangeListener(nextValue)) {
                      dialog.dismiss();
                    }
                  });

          dialog
              .getButton(AlertDialog.BUTTON_NEUTRAL)
              .setOnClickListener(
                  v -> {
                    if (preference.callChangeListener("")) {
                      dialog.dismiss();
                    }
                  });
        });
    dialog.show();
  }

  @NonNull
  private static String formatColor(int argb) {
    final int alpha = (argb >>> 24) & 0xFF;
    if (alpha == 0xFF) {
      return String.format("#%06X", argb & 0x00FF_FFFF);
    }
    return String.format("#%08X", argb);
  }

  private static final class ColorChannelControls {
    @NonNull final SeekBar seekBar;
    @NonNull final TextView valueView;

    ColorChannelControls(@NonNull SeekBar seekBar, @NonNull TextView valueView) {
      this.seekBar = seekBar;
      this.valueView = valueView;
    }
  }

  @NonNull
  private static ColorChannelControls addColorChannelRow(
      @NonNull Context context, @NonNull LinearLayout parent, int titleResId, int initialValue) {
    final LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    final LinearLayout.LayoutParams rowParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = dpToPx(context, 8);
    row.setLayoutParams(rowParams);

    final TextView label = new TextView(context);
    label.setText(titleResId);
    row.addView(
        label,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final SeekBar seek = new SeekBar(context);
    seek.setMax(255);
    seek.setProgress(Math.max(0, Math.min(255, initialValue)));
    final LinearLayout.LayoutParams seekParams =
        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    seekParams.leftMargin = dpToPx(context, 12);
    seekParams.rightMargin = dpToPx(context, 12);
    row.addView(seek, seekParams);

    final TextView value = new TextView(context);
    value.setText(String.valueOf(seek.getProgress()));
    row.addView(
        value,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    parent.addView(row);
    return new ColorChannelControls(seek, value);
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
  }
}
