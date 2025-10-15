package com.anysoftkeyboard.debug;

import android.content.Context;
import com.anysoftkeyboard.saywhat.PublicNotice;
import java.util.ArrayList;
import java.util.List;

public final class DebugNoticesProvider {
  private DebugNoticesProvider() {}

  public static List<PublicNotice> create(Context context) {
    List<PublicNotice> notices = new ArrayList<>();
    notices.addAll(ImeStateTracker.createNotices());
    return notices;
  }
}
