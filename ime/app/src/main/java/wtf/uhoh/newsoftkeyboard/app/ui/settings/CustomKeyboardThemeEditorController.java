package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.AtomicPackFileWriter;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.PackThemeOverride;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlWriter;

final class CustomKeyboardThemeEditorController {

  interface Host {
    @NonNull
    Fragment fragment();

    @NonNull
    KeyboardView keyboardView();

    @NonNull
    TextView statusView();

    @Nullable
    InstalledKeyboardPack pack();

    void setPack(@NonNull InstalledKeyboardPack pack);
  }

  @NonNull private final Host host;
  @Nullable private PackEntry themeEntry;

  CustomKeyboardThemeEditorController(@NonNull Host host) {
    this.host = host;
  }

  void showThemeActionsDialog() {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack == null) return;

    final List<ActionItem> actions = new ArrayList<>();
    if (!currentPack.manifest().themes().isEmpty()) {
      actions.add(
          new ActionItem(
              host.fragment().getString(R.string.custom_keyboards_theme_action_apply),
              this::showThemeSelectDialog));
    }
    actions.add(
        new ActionItem(
            host.fragment().getString(R.string.custom_keyboards_theme_action_create),
            this::showCreateThemeDialog));
    if (themeEntry != null) {
      PackEntry selected = themeEntry;
      actions.add(
          new ActionItem(
              host.fragment().getString(R.string.custom_keyboards_theme_action_edit),
              () -> showEditThemeDialog(selected)));
      actions.add(
          new ActionItem(
              host.fragment().getString(R.string.custom_keyboards_theme_action_clear),
              this::clearThemeOverride));
    }

    CharSequence[] titles = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) titles[i] = actions.get(i).title;

    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_theme_actions_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              ActionItem item = actions.get(which);
              item.action.run();
            })
        .show();
  }

  void applyPersistedThemeIfAny() {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack == null) return;
    String themeId =
        CustomKeyboardPrefs.getSelectedThemeIdForPack(
            host.fragment().requireContext(), currentPack.manifest().id());
    if (TextUtils.isEmpty(themeId)) {
      host.keyboardView().setPackThemeOverride(null);
      themeEntry = null;
      return;
    }
    PackEntry entry = findThemeEntry(currentPack, themeId);
    if (entry == null) {
      host.keyboardView().setPackThemeOverride(null);
      themeEntry = null;
      return;
    }
    applyThemeEntry(entry);
  }

  private void showThemeSelectDialog() {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack == null) return;
    final List<PackEntry> themes = currentPack.manifest().themes();

    CharSequence[] titles = new CharSequence[themes.size() + 1];
    titles[0] = host.fragment().getString(R.string.custom_keyboards_theme_select_none);
    for (int i = 0; i < themes.size(); i++) titles[i + 1] = themes.get(i).id();

    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_theme_select_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              if (which == 0) {
                clearThemeOverride();
              } else {
                applyThemeEntry(themes.get(which - 1));
              }
            })
        .show();
  }

  private void clearThemeOverride() {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack != null) {
      CustomKeyboardPrefs.setSelectedThemeIdForPack(
          host.fragment().requireContext(), currentPack.manifest().id(), null);
    }
    themeEntry = null;
    host.keyboardView().setPackThemeOverride(null);
  }

  private void applyThemeEntry(@NonNull PackEntry entry) {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack == null) return;

    File themeFile = new File(currentPack.directory(), entry.path().value());
    try (InputStream in = new FileInputStream(themeFile)) {
      ThemeModel model = ThemeXmlParser.parse(in);
      host.keyboardView()
          .setPackThemeOverride(new PackThemeOverride(currentPack.directory(), model));
      themeEntry = entry;
      CustomKeyboardPrefs.setSelectedThemeIdForPack(
          host.fragment().requireContext(), currentPack.manifest().id(), entry.id());
    } catch (IOException e) {
      host.statusView().setText(e.getMessage());
    }
  }

  private void showCreateThemeDialog() {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack == null) return;

    View dialogView =
        LayoutInflater.from(host.fragment().requireContext())
            .inflate(R.layout.custom_keyboard_edit_theme_dialog, null, false);
    EditText idEdit = dialogView.findViewById(R.id.edit_theme_id);
    EditText keyboardBgEdit = dialogView.findViewById(R.id.edit_theme_keyboard_background);
    keyboardBgEdit.setText("#202020");

    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_theme_create_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (ignored, which) -> {
              String rawId = idEdit.getText() != null ? idEdit.getText().toString() : "";
              String themeId = sanitizeId(rawId);
              if (TextUtils.isEmpty(themeId)) return;
              if (containsThemeEntryId(currentPack.manifest(), themeId)) {
                host.statusView().setText("Theme id already exists: " + themeId);
                return;
              }
              try {
                PackEntry entry = createThemeEntry(themeId);
                ThemeModel model = readThemeModelFromDialog(dialogView);
                writeThemeFile(currentPack, entry, model);
                PackManifest updatedManifest =
                    new PackManifest(
                        currentPack.manifest().schemaVersion(),
                        currentPack.manifest().id(),
                        currentPack.manifest().name(),
                        currentPack.manifest().version() + 1,
                        currentPack.manifest().minCoreVersion(),
                        currentPack.manifest().keyboards(),
                        CustomKeyboardLayoutPackFiles.appendEntry(
                            currentPack.manifest().themes(), entry));
                CustomKeyboardLayoutPackFiles.writePackManifest(
                    currentPack.directory(), updatedManifest);
                InstalledKeyboardPack updatedPack =
                    new InstalledKeyboardPack(currentPack.directory(), updatedManifest);
                host.setPack(updatedPack);
                applyThemeEntry(entry);
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showEditThemeDialog(@NonNull PackEntry entry) {
    InstalledKeyboardPack currentPack = host.pack();
    if (currentPack == null) return;

    File themeFile = new File(currentPack.directory(), entry.path().value());
    final ThemeModel existing;
    try (InputStream in = new FileInputStream(themeFile)) {
      existing = ThemeXmlParser.parse(in);
    } catch (IOException e) {
      host.statusView().setText(e.getMessage());
      return;
    }

    View dialogView =
        LayoutInflater.from(host.fragment().requireContext())
            .inflate(R.layout.custom_keyboard_edit_theme_dialog, null, false);
    EditText idEdit = dialogView.findViewById(R.id.edit_theme_id);
    idEdit.setText(entry.id());
    idEdit.setEnabled(false);

    EditText keyboardBgEdit = dialogView.findViewById(R.id.edit_theme_keyboard_background);
    EditText keyBgEdit = dialogView.findViewById(R.id.edit_theme_key_background);
    EditText keyTextEdit = dialogView.findViewById(R.id.edit_theme_key_text_color);
    EditText hintTextEdit = dialogView.findViewById(R.id.edit_theme_hint_text_color);
    EditText nameTextEdit = dialogView.findViewById(R.id.edit_theme_name_text_color);
    EditText deleteIconEdit = dialogView.findViewById(R.id.edit_theme_delete_icon);
    EditText shiftIconEdit = dialogView.findViewById(R.id.edit_theme_shift_icon);
    EditText actionIconEdit = dialogView.findViewById(R.id.edit_theme_action_icon);
    EditText spaceIconEdit = dialogView.findViewById(R.id.edit_theme_space_icon);

    keyboardBgEdit.setText(formatColor(existing.colors().get("keyboardBackground")));
    keyBgEdit.setText(formatColor(existing.colors().get("keyBackground")));
    keyTextEdit.setText(formatColor(existing.colors().get("keyTextColor")));
    hintTextEdit.setText(formatColor(existing.colors().get("hintTextColor")));
    nameTextEdit.setText(formatColor(existing.colors().get("keyboardNameTextColor")));

    deleteIconEdit.setText(formatPath(existing.icons().get("delete")));
    shiftIconEdit.setText(formatPath(existing.icons().get("shift")));
    actionIconEdit.setText(formatPath(existing.icons().get("action")));
    spaceIconEdit.setText(formatPath(existing.icons().get("space")));

    new AlertDialog.Builder(host.fragment().requireContext())
        .setTitle(R.string.custom_keyboards_theme_edit_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (ignored, which) -> {
              try {
                ThemeModel model = readThemeModelFromDialog(dialogView, existing.rawAttributes());
                writeThemeFile(currentPack, entry, model);
                applyThemeEntry(entry);
              } catch (IOException e) {
                host.statusView().setText(e.getMessage());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @NonNull
  private static PackEntry createThemeEntry(@NonNull String id) {
    PackPath path = PackPath.parse("themes/" + id + ".xml");
    return new PackEntry(id, path);
  }

  @NonNull
  private static String sanitizeId(@Nullable String raw) {
    if (raw == null) return "";
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return "";

    StringBuilder builder = new StringBuilder(trimmed.length());
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      boolean allowed =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || c == '.'
              || c == '_'
              || c == '-';
      builder.append(allowed ? c : '_');
    }
    return builder.toString();
  }

  private static boolean containsThemeEntryId(@NonNull PackManifest manifest, @NonNull String id) {
    for (PackEntry entry : manifest.themes()) {
      if (entry.id().equals(id)) return true;
    }
    return false;
  }

  private static void writeThemeFile(
      @NonNull InstalledKeyboardPack pack, @NonNull PackEntry entry, @NonNull ThemeModel model)
      throws IOException {
    File xmlFile = new File(pack.directory(), entry.path().value());
    File parent = xmlFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed creating directory at " + parent);
    }
    AtomicPackFileWriter.write(xmlFile, out -> ThemeXmlWriter.write(model, out));
  }

  @NonNull
  private static ThemeModel readThemeModelFromDialog(@NonNull View dialogView) throws IOException {
    return readThemeModelFromDialog(dialogView, Collections.emptyMap());
  }

  @NonNull
  private static ThemeModel readThemeModelFromDialog(
      @NonNull View dialogView, @NonNull Map<String, String> rawAttributes) throws IOException {
    Map<String, Integer> colors = new HashMap<>();
    putOptionalColor(
        colors,
        "keyboardBackground",
        getEditTextValue(dialogView, R.id.edit_theme_keyboard_background));
    putOptionalColor(
        colors, "keyBackground", getEditTextValue(dialogView, R.id.edit_theme_key_background));
    putOptionalColor(
        colors, "keyTextColor", getEditTextValue(dialogView, R.id.edit_theme_key_text_color));
    putOptionalColor(
        colors, "hintTextColor", getEditTextValue(dialogView, R.id.edit_theme_hint_text_color));
    putOptionalColor(
        colors,
        "keyboardNameTextColor",
        getEditTextValue(dialogView, R.id.edit_theme_name_text_color));

    Map<String, PackPath> icons = new HashMap<>();
    putOptionalIcon(icons, "delete", getEditTextValue(dialogView, R.id.edit_theme_delete_icon));
    putOptionalIcon(icons, "shift", getEditTextValue(dialogView, R.id.edit_theme_shift_icon));
    putOptionalIcon(icons, "action", getEditTextValue(dialogView, R.id.edit_theme_action_icon));
    putOptionalIcon(icons, "space", getEditTextValue(dialogView, R.id.edit_theme_space_icon));

    return new ThemeModel(colors, icons, rawAttributes);
  }

  private static void putOptionalColor(
      @NonNull Map<String, Integer> colors, @NonNull String key, @Nullable String raw)
      throws IOException {
    if (raw == null) return;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return;
    try {
      colors.put(key, Color.parseColor(trimmed));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid color for " + key + ": " + trimmed);
    }
  }

  private static void putOptionalIcon(
      @NonNull Map<String, PackPath> icons, @NonNull String key, @Nullable String raw)
      throws IOException {
    if (raw == null) return;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return;
    try {
      icons.put(key, PackPath.parse(trimmed));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid icon path for " + key + ": " + trimmed);
    }
  }

  @Nullable
  private static String getEditTextValue(@NonNull View dialogView, int id) {
    EditText edit = dialogView.findViewById(id);
    return edit.getText() != null ? edit.getText().toString() : null;
  }

  @Nullable
  private static String formatColor(@Nullable Integer argb) {
    if (argb == null) return null;
    int alpha = (argb >>> 24) & 0xFF;
    if (alpha == 0xFF) {
      return String.format("#%06X", argb & 0x00FF_FFFF);
    }
    return String.format("#%08X", argb);
  }

  @Nullable
  private static String formatPath(@Nullable PackPath path) {
    return path != null ? path.value() : null;
  }

  @Nullable
  private static PackEntry findThemeEntry(@NonNull InstalledKeyboardPack pack, @NonNull String id) {
    for (PackEntry entry : pack.manifest().themes()) {
      if (entry.id().equals(id)) return entry;
    }
    return null;
  }

  private static final class ActionItem {
    @NonNull final String title;
    @NonNull final Runnable action;

    private ActionItem(@NonNull String title, @NonNull Runnable action) {
      this.title = title;
      this.action = action;
    }
  }
}
