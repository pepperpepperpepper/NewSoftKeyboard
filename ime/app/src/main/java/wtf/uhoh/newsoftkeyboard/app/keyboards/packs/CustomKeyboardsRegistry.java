package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;

public final class CustomKeyboardsRegistry {
  private static final String TAG = "CustomKeyboardsRegistry";

  private CustomKeyboardsRegistry() {}

  @NonNull
  public static List<PackKeyboardAddOnAndBuilder> listAllKeyboardBuilders(
      @NonNull Context context) {
    Objects.requireNonNull(context);
    final List<InstalledKeyboardPack> packs;
    try {
      packs = new KeyboardPacksRepository(context).listInstalledPacks();
    } catch (IOException e) {
      Logger.w(TAG, "Failed listing installed packs: %s", e.getMessage());
      return Collections.emptyList();
    }

    var result = new ArrayList<PackKeyboardAddOnAndBuilder>();
    int sortIndex = 0;
    for (InstalledKeyboardPack pack : packs) {
      for (PackEntry entry : pack.manifest().keyboards()) {
        result.add(new PackKeyboardAddOnAndBuilder(context, pack, entry, sortIndex++));
      }
    }
    return Collections.unmodifiableList(result);
  }

  @NonNull
  public static List<PackKeyboardAddOnAndBuilder> listEnabledKeyboardBuilders(
      @NonNull Context context) {
    Objects.requireNonNull(context);
    Set<String> enabledIds = CustomKeyboardPrefs.getEnabledKeyboardIds(context);
    if (enabledIds.isEmpty()) return Collections.emptyList();

    final List<InstalledKeyboardPack> packs;
    try {
      packs = new KeyboardPacksRepository(context).listInstalledPacks();
    } catch (IOException e) {
      Logger.w(TAG, "Failed listing installed packs: %s", e.getMessage());
      return Collections.emptyList();
    }

    var result = new ArrayList<PackKeyboardAddOnAndBuilder>();
    int sortIndex = 0;
    for (InstalledKeyboardPack pack : packs) {
      for (PackEntry entry : pack.manifest().keyboards()) {
        final String id = PackKeyboardAddOnAndBuilder.buildKeyboardId(pack.manifest(), entry);
        if (!enabledIds.contains(id)) continue;
        if (!isSelectableKeyboardEntry(pack.manifest(), entry)) continue;
        result.add(new PackKeyboardAddOnAndBuilder(context, pack, entry, sortIndex++));
      }
    }
    result.sort(Comparator.comparing(b -> b.getName().toString(), String.CASE_INSENSITIVE_ORDER));
    return Collections.unmodifiableList(result);
  }

  @Nullable
  public static PackKeyboardAddOnAndBuilder findKeyboardBuilderById(
      @NonNull Context context, @NonNull String keyboardId) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(keyboardId);
    ParsedPackKeyboardId parsed = ParsedPackKeyboardId.tryParse(keyboardId);
    if (parsed == null) return null;

    final InstalledKeyboardPack pack;
    try {
      pack = new KeyboardPacksRepository(context).findInstalledPackById(parsed.packId);
    } catch (IOException e) {
      return null;
    }
    if (pack == null) return null;

    for (PackEntry entry : pack.manifest().keyboards()) {
      if (!entry.id().equals(parsed.keyboardEntryId)) continue;
      return new PackKeyboardAddOnAndBuilder(context, pack, entry, 0);
    }

    return null;
  }

  private static boolean isSelectableKeyboardEntry(
      @NonNull PackManifest manifest, @NonNull PackEntry entry) {
    if (manifest.keyboards().size() <= 1) return true;
    String id = entry.id();
    return !(id.equals("symbols") || id.startsWith("symbols_"));
  }

  private static final class ParsedPackKeyboardId {
    private static final String PREFIX = "pack::";
    private static final String SEP = "::";

    @NonNull final String packId;
    @NonNull final String keyboardEntryId;

    private ParsedPackKeyboardId(@NonNull String packId, @NonNull String keyboardEntryId) {
      this.packId = packId;
      this.keyboardEntryId = keyboardEntryId;
    }

    @Nullable
    static ParsedPackKeyboardId tryParse(@NonNull String raw) {
      if (!raw.startsWith(PREFIX)) return null;
      String rest = raw.substring(PREFIX.length());
      int sep = rest.indexOf(SEP);
      if (sep <= 0 || sep + SEP.length() >= rest.length()) return null;
      String packId = rest.substring(0, sep);
      String keyboardEntryId = rest.substring(sep + SEP.length());
      if (packId.isEmpty() || keyboardEntryId.isEmpty()) return null;
      return new ParsedPackKeyboardId(packId, keyboardEntryId);
    }
  }
}
