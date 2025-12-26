package wtf.uhoh.newsoftkeyboard.app.debug;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.notices.PublicNotice;

public final class DebugNoticesProvider {
  private DebugNoticesProvider() {}

  public static List<PublicNotice> create(Context context) {
    List<PublicNotice> notices = new ArrayList<>();
    notices.addAll(ImeStateTracker.createNotices());
    return notices;
  }
}
