package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.R;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;

public final class PackKeyboardAddOnAndBuilder extends KeyboardAddOnAndBuilder {
  private static final String DEFAULT_SENTENCE_SEPARATORS = ".,!?)]:;";

  @NonNull private final Context hostAppContext;
  @NonNull private final InstalledKeyboardPack pack;
  @NonNull private final PackEntry keyboardEntry;
  @NonNull private final PackKeyboardRuntimeLoader runtimeLoader = new PackKeyboardRuntimeLoader();

  public PackKeyboardAddOnAndBuilder(
      @NonNull Context hostAppContext,
      @NonNull InstalledKeyboardPack pack,
      @NonNull PackEntry keyboardEntry,
      int sortIndex) {
    super(
        hostAppContext,
        hostAppContext,
        hostAppContext.getResources().getInteger(R.integer.anysoftkeyboard_api_version_code),
        buildKeyboardId(pack.manifest(), keyboardEntry),
        buildKeyboardName(pack.manifest(), keyboardEntry),
        AddOn.INVALID_RES_ID,
        AddOn.INVALID_RES_ID,
        "",
        wtf.uhoh.newsoftkeyboard.R.drawable.sym_keyboard_notification_icon,
        AddOn.INVALID_RES_ID,
        "",
        DEFAULT_SENTENCE_SEPARATORS,
        "",
        false,
        sortIndex,
        false);

    this.hostAppContext = Objects.requireNonNull(hostAppContext).getApplicationContext();
    this.pack = Objects.requireNonNull(pack);
    this.keyboardEntry = Objects.requireNonNull(keyboardEntry);
  }

  @NonNull
  public static String buildKeyboardId(@NonNull PackManifest manifest, @NonNull PackEntry entry) {
    return "pack::" + manifest.id() + "::" + entry.id();
  }

  @NonNull
  public static CharSequence buildKeyboardName(
      @NonNull PackManifest manifest, @NonNull PackEntry entry) {
    int selectableCount = 0;
    for (PackEntry e : manifest.keyboards()) {
      if (isInternalEntryId(e.id()) && manifest.keyboards().size() > 1) continue;
      selectableCount++;
    }
    if (selectableCount <= 1 && !isInternalEntryId(entry.id())) return manifest.name();
    return manifest.name() + " — " + entry.id();
  }

  private static boolean isInternalEntryId(@NonNull String id) {
    return id.equals("symbols") || id.startsWith("symbols_");
  }

  @Nullable
  @Override
  public KeyboardDefinition createKeyboard(@Keyboard.KeyboardRowModeId int mode) {
    return runtimeLoader.tryLoadKeyboardDefinition(hostAppContext, pack, keyboardEntry.id(), mode);
  }
}
