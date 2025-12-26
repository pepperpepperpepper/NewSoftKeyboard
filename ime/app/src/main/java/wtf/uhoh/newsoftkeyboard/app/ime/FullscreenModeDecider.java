package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.res.Configuration;
import android.view.inputmethod.EditorInfo;

/** Encapsulates the logic for determining whether to enter fullscreen/extract mode. */
public final class FullscreenModeDecider {

  public boolean shouldUseFullscreen(
      EditorInfo editorInfo,
      int currentOrientation,
      boolean useFullScreenInputInPortrait,
      boolean useFullScreenInputInLandscape) {

    if (editorInfo != null) {
      if ((editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_FULLSCREEN) != 0) {
        return false;
      } else if ((editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0) {
        return false;
      }
    }

    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
      return useFullScreenInputInLandscape;
    } else {
      return useFullScreenInputInPortrait;
    }
  }
}
