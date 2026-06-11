package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.AtomicPackFileWriter;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifestJson;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackPath;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardWriter;

final class CustomKeyboardLayoutPackFiles {
  private CustomKeyboardLayoutPackFiles() {}

  @NonNull
  static String createPopupKeyboardFile(@NonNull InstalledKeyboardPack pack) throws IOException {
    File keyboardsDir = new File(pack.directory(), "keyboards");
    if (!keyboardsDir.exists() && !keyboardsDir.mkdirs()) {
      throw new IOException("Failed creating keyboards directory at " + keyboardsDir);
    }

    String fileName = "popup_" + System.currentTimeMillis() + ".xml";
    File xmlFile = new File(keyboardsDir, fileName);
    int i = 0;
    while (xmlFile.exists() && i < 100) {
      i++;
      xmlFile = new File(keyboardsDir, "popup_" + System.currentTimeMillis() + "_" + i + ".xml");
    }

    Map<String, String> keyboardAttrs = new HashMap<>();
    keyboardAttrs.put("xmlns:android", "http://schemas.android.com/apk/res/android");
    keyboardAttrs.put("android:keyWidth", "10%p");
    keyboardAttrs.put("android:keyHeight", "-1");
    KeyboardModel model =
        new KeyboardModel(
            keyboardAttrs,
            Collections.singletonList(
                new KeyboardRow(
                    Collections.singletonList(
                        CustomKeyboardLayoutModelEditor.createPlaceholderKeySpec()))));
    writeKeyboardModel(xmlFile, model);
    return "keyboards/" + xmlFile.getName();
  }

  @NonNull
  static PackEntry createSymbolsEntry(@NonNull PackManifest manifest) {
    String entryId = "symbols";
    int suffix = 2;
    while (containsKeyboardEntryId(manifest, entryId)) {
      entryId = "symbols_" + suffix;
      suffix++;
    }
    PackPath path = PackPath.parse("keyboards/" + entryId + ".xml");
    return new PackEntry(entryId, path);
  }

  static boolean containsKeyboardEntryId(@NonNull PackManifest manifest, @NonNull String id) {
    for (PackEntry entry : manifest.keyboards()) {
      if (entry.id().equals(id)) return true;
    }
    return false;
  }

  @NonNull
  static List<PackEntry> appendEntry(@NonNull List<PackEntry> entries, @NonNull PackEntry entry) {
    List<PackEntry> updated = new ArrayList<>(entries);
    updated.add(entry);
    return Collections.unmodifiableList(updated);
  }

  static void writePackManifest(@NonNull File packDir, @NonNull PackManifest manifest)
      throws IOException {
    File manifestFile = new File(packDir, "manifest.json");
    AtomicPackFileWriter.write(manifestFile, out -> PackManifestJson.write(manifest, out));
  }

  static void writeSymbolsKeyboardFile(
      @NonNull InstalledKeyboardPack pack, @NonNull PackEntry entry, @NonNull String backTargetId)
      throws IOException {
    File xmlFile = new File(pack.directory(), entry.path().value());
    File parent = xmlFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed creating directory at " + parent);
    }

    Map<String, String> keyboardAttrs = new HashMap<>();
    keyboardAttrs.put("xmlns:android", "http://schemas.android.com/apk/res/android");
    keyboardAttrs.put("xmlns:ask", "http://schemas.android.com/apk/res-auto");
    keyboardAttrs.put("android:keyWidth", "10%p");
    keyboardAttrs.put("android:keyHeight", "-1");

    List<KeySpec> row1 = new ArrayList<>();
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("1", "49", "left", null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("2", "50", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("3", "51", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("4", "52", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("5", "53", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("6", "54", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("7", "55", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("8", "56", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("9", "57", null, null));
    row1.add(CustomKeyboardLayoutModelEditor.createKeySpec("0", "48", "right", null));

    List<KeySpec> row2 = new ArrayList<>();
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("!", "33", "left", null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("@", "64", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("#", "35", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("$", "36", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("%", "37", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("^", "94", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("&", "38", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("*", "42", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec("(", "40", null, null));
    row2.add(CustomKeyboardLayoutModelEditor.createKeySpec(")", "41", "right", null));

    List<KeySpec> row3 = new ArrayList<>();
    row3.add(
        CustomKeyboardLayoutModelEditor.createKeySpec(
            "ABC",
            Integer.toString(KeyCodes.CUSTOM_KEYBOARD_SWITCH),
            "left",
            backTargetId,
            "20%p"));
    row3.add(CustomKeyboardLayoutModelEditor.createKeySpec("-", "45", null, null));
    row3.add(CustomKeyboardLayoutModelEditor.createKeySpec("_", "95", null, null));
    row3.add(CustomKeyboardLayoutModelEditor.createKeySpec("/", "47", null, null));
    row3.add(CustomKeyboardLayoutModelEditor.createKeySpec(":", "58", null, null));
    row3.add(CustomKeyboardLayoutModelEditor.createKeySpec(";", "59", null, null));
    row3.add(CustomKeyboardLayoutModelEditor.createKeySpec("\"", "34", null, null));
    row3.add(
        CustomKeyboardLayoutModelEditor.createKeySpec(
            "⌫",
            "-5",
            "right",
            null,
            "20%p",
            Collections.singletonMap("android:isRepeatable", "true")));

    KeyboardModel modelToWrite =
        new KeyboardModel(
            keyboardAttrs,
            Arrays.asList(new KeyboardRow(row1), new KeyboardRow(row2), new KeyboardRow(row3)));
    writeKeyboardModel(xmlFile, modelToWrite);
  }

  static void writeKeyboardModel(@NonNull File xmlFile, @NonNull KeyboardModel model)
      throws IOException {
    AtomicPackFileWriter.write(xmlFile, out -> AskXmlKeyboardWriter.write(model, out));
  }

  @Nullable
  static KeyboardModel readKeyboardModelOrNull(@Nullable File keyboardXmlFile) {
    if (keyboardXmlFile == null) return null;
    try (InputStream in = new FileInputStream(keyboardXmlFile)) {
      return AskXmlKeyboardParser.parse(in);
    } catch (IOException e) {
      return null;
    }
  }

  @Nullable
  static PackEntry findKeyboardEntry(@NonNull InstalledKeyboardPack pack, @NonNull String entryId) {
    for (PackEntry entry : pack.manifest().keyboards()) {
      if (entry.id().equals(entryId)) return entry;
    }
    return null;
  }
}
