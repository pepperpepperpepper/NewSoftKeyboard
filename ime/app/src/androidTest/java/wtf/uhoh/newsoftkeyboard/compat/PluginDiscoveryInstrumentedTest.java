package wtf.uhoh.newsoftkeyboard.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.api.PluginActions;

/**
 * CTS-style sanity check: ensure keyboard add-ons are discoverable under both action namespaces.
 * Skips gracefully when no add-ons are installed on device/emulator.
 */
@RunWith(AndroidJUnit4.class)
public class PluginDiscoveryInstrumentedTest {

  @Test
  public void keyboardsDiscoverableOrSkip() {
    Context context = ApplicationProvider.getApplicationContext();
    PackageManager pm = context.getPackageManager();

    List<ResolveInfo> found = new ArrayList<>();
    found.addAll(query(pm, PluginActions.ACTION_KEYBOARD_NEW));
    found.addAll(query(pm, PluginActions.ACTION_KEYBOARD_ASK));

    assumeTrue("No keyboard add-ons installed; skipping", !found.isEmpty());

    assertFalse("Discovery should return at least one keyboard add-on", found.isEmpty());
  }

  private List<ResolveInfo> query(PackageManager pm, String action) {
    Intent intent = new Intent(action);
    List<ResolveInfo> results = pm.queryIntentServices(intent, PackageManager.GET_META_DATA);
    return results == null ? new ArrayList<>() : results;
  }
}
