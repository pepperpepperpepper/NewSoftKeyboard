package wtf.uhoh.newsoftkeyboard.app.notices;

import android.content.Context;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class Notices {
  private static final String DEBUG_PROVIDER_CLASS =
      "wtf.uhoh.newsoftkeyboard.app.debug.DebugNoticesProvider";

  @SuppressWarnings("unchecked")
  public static List<PublicNotice> create(Context context) {
    try {
      Class<?> providerClass = Class.forName(DEBUG_PROVIDER_CLASS);
      Method method = providerClass.getDeclaredMethod("create", Context.class);
      Object result = method.invoke(null, context);
      if (result instanceof List) {
        return (List<PublicNotice>) result;
      }
    } catch (Throwable ignored) {
      // ignore - no debug provider available
    }
    return Collections.emptyList();
  }
}
