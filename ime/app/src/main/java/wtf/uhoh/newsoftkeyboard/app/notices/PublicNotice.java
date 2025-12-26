package wtf.uhoh.newsoftkeyboard.app.notices;

import androidx.annotation.NonNull;

public interface PublicNotice {
  /** This name MUST be unique */
  @NonNull
  String getName();
}
