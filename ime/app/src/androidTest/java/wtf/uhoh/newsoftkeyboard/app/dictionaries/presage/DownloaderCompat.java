package wtf.uhoh.newsoftkeyboard.app.dictionaries.presage;

import androidx.annotation.NonNull;
import java.io.IOException;
import org.json.JSONException;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDownloader;

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
