package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.PackKeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.keyboard.core.io.DirectoryPackSource;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;

public final class PackKeyboardRuntimeLoader {

  @Nullable
  public PackKeyboardDefinition tryLoadKeyboardDefinition(
      @NonNull Context context,
      @NonNull InstalledKeyboardPack pack,
      @Nullable String keyboardId,
      @Keyboard.KeyboardRowModeId int mode) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(pack);

    final PackEntry entry;
    try {
      entry = selectKeyboardEntry(pack, keyboardId);
    } catch (IllegalArgumentException e) {
      return null;
    }

    final File packDir = pack.directory();
    try {
      return new PackKeyboardDefinition(
          context.getApplicationContext(),
          pack.manifest(),
          entry,
          mode,
          new DirectoryPackSource(packDir));
    } catch (IOException e) {
      return null;
    }
  }

  @NonNull
  private static PackEntry selectKeyboardEntry(
      @NonNull InstalledKeyboardPack pack, @Nullable String keyboardId) {
    List<PackEntry> keyboards = pack.manifest().keyboards();
    if (keyboards.isEmpty()) throw new IllegalArgumentException("Pack has no keyboards");

    if (keyboardId == null || keyboardId.trim().isEmpty()) return keyboards.get(0);
    for (PackEntry entry : keyboards) {
      if (keyboardId.equals(entry.id())) return entry;
    }
    throw new IllegalArgumentException("Keyboard id not found: " + keyboardId);
  }
}
