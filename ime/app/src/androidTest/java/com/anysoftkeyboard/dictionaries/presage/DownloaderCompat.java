package com.anysoftkeyboard.dictionaries.presage;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.engine.models.ModelDefinition;
import com.anysoftkeyboard.engine.models.ModelDownloader;
import java.io.IOException;
import org.json.JSONException;

/** Test-only compatibility shim for calling the refactored downloader API. */
public final class DownloaderCompat {
  private DownloaderCompat() {}

  public static ModelDefinition run(
      @NonNull ModelDownloader downloader, @NonNull PresageModelCatalog.CatalogEntry entry)
      throws IOException, JSONException {
    return downloader.downloadAndInstall(
        entry.getDefinition(), entry.getBundleUrl(), entry.getBundleSha256());
  }
}
