package wtf.uhoh.newsoftkeyboard.app.prefs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

public final class PrefsAutoRestorer {

  private static final String TAG = "NSKPrefsAutoRestore";

  private PrefsAutoRestorer() {}

  public static void restoreAutoApplyPrefs(@NonNull Context context, @NonNull File file) {
    Logger.d(TAG, "Starting auto-restore for '%s'", file);

    // NOTE: shared_prefs_provider_name is the only supported prefs. All others require dictionaries
    // to load prior.
    final List<GlobalPrefsBackup.ProviderDetails> providersList =
        GlobalPrefsBackup.getAllAutoApplyPrefsProviders(context);
    final Boolean[] enabledProviders = new Boolean[providersList.size()];
    Arrays.fill(enabledProviders, Boolean.TRUE);

    final Pair<List<GlobalPrefsBackup.ProviderDetails>, Boolean[]> providers =
        Pair.create(providersList, enabledProviders);

    try {
      GlobalPrefsBackup.restore(providers, new FileInputStream(file))
          .blockingForEach(
              providerDetails ->
                  Logger.i(
                      TAG,
                      "Restored prefs for '%s'",
                      context.getString(providerDetails.providerTitle)));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      Logger.w(TAG, e, "Failed to load auto-apply file!");
    }
  }
}
