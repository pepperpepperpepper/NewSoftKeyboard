package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import androidx.annotation.NonNull;
import java.io.File;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeModel;

public final class PackThemeOverride {
  @NonNull private final File packDirectory;
  @NonNull private final ThemeModel themeModel;

  public PackThemeOverride(@NonNull File packDirectory, @NonNull ThemeModel themeModel) {
    this.packDirectory = Objects.requireNonNull(packDirectory);
    this.themeModel = Objects.requireNonNull(themeModel);
  }

  @NonNull
  public File packDirectory() {
    return packDirectory;
  }

  @NonNull
  public ThemeModel themeModel() {
    return themeModel;
  }
}
