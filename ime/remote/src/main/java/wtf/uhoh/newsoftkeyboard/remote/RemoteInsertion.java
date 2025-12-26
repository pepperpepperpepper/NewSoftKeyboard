package wtf.uhoh.newsoftkeyboard.remote;

import androidx.annotation.NonNull;

public interface RemoteInsertion {
  void startMediaRequest(
      @NonNull String[] mimeTypes, int requestId, @NonNull InsertionRequestCallback callback);

  void destroy();
}
