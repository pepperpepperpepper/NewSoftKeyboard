package wtf.uhoh.newsoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.NavigationKeyHandler;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;

public final class NavigationKeyHandlerHost implements NavigationKeyHandler.Host {

  private final BiFunction<Integer, InputConnection, Boolean> handleSelectionExpending;
  private final IntConsumer sendNavigationKeyEvent;
  private final IntConsumer sendDownUpKeyEvents;

  public NavigationKeyHandlerHost(
      @NonNull BiFunction<Integer, InputConnection, Boolean> handleSelectionExpending,
      @NonNull IntConsumer sendNavigationKeyEvent,
      @NonNull IntConsumer sendDownUpKeyEvents) {
    this.handleSelectionExpending = handleSelectionExpending;
    this.sendNavigationKeyEvent = sendNavigationKeyEvent;
    this.sendDownUpKeyEvents = sendDownUpKeyEvents;
  }

  @Override
  public boolean handleSelectionExpending(int keyEventCode, @Nullable InputConnection ic) {
    return handleSelectionExpending.apply(keyEventCode, ic);
  }

  @Override
  public void sendNavigationKeyEvent(int keyEventCode) {
    sendNavigationKeyEvent.accept(keyEventCode);
  }

  @Override
  public void sendDownUpKeyEvents(int keyCode) {
    sendDownUpKeyEvents.accept(keyCode);
  }
}

