package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

public final class PackKeyboardSpec {
  private static final String SEPARATOR = "::";

  @NonNull private final String packId;
  @Nullable private final String keyboardId;

  public PackKeyboardSpec(@NonNull String packId, @Nullable String keyboardId) {
    this.packId = Objects.requireNonNull(packId);
    this.keyboardId = keyboardId;
  }

  @Nullable
  public static PackKeyboardSpec parse(@Nullable String raw) {
    if (raw == null) return null;
    String value = raw.trim();
    if (value.isEmpty()) return null;

    int splitIndex = value.indexOf(SEPARATOR);
    if (splitIndex == -1) {
      return new PackKeyboardSpec(value, null);
    }

    String packId = value.substring(0, splitIndex).trim();
    String keyboardId = value.substring(splitIndex + SEPARATOR.length()).trim();
    if (packId.isEmpty()) return null;
    if (keyboardId.isEmpty()) keyboardId = null;
    return new PackKeyboardSpec(packId, keyboardId);
  }

  @NonNull
  public String packId() {
    return packId;
  }

  @Nullable
  public String keyboardId() {
    return keyboardId;
  }

  @NonNull
  public String serialize() {
    if (keyboardId == null) return packId;
    return packId + SEPARATOR + keyboardId;
  }
}
