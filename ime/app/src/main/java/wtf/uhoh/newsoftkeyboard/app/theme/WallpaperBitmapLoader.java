package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;

interface WallpaperBitmapLoader {

  @Nullable
  Bitmap getCached(@NonNull File file, long lastModified, int requestedMaxDimPx);

  void loadAsync(
      @NonNull File file,
      long lastModified,
      int requestedMaxDimPx,
      @NonNull WallpaperBitmapRepository.Callback cb);
}
