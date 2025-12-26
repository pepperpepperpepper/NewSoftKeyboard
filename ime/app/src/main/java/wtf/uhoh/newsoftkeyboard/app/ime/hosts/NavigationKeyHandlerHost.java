package wtf.uhoh.newsoftkeyboard.app.ime.hosts;

import androidx.annotation.NonNull;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import wtf.uhoh.newsoftkeyboard.app.ime.InputConnectionRouter;
import wtf.uhoh.newsoftkeyboard.app.ime.NavigationKeyHandler;

public final class NavigationKeyHandlerHost implements NavigationKeyHandler.Host {

  private final BiFunction<Integer, InputConnectionRouter, Boolean> handleSelectionExpending;
  private final IntConsumer sendNavigationKeyEvent;
  private final IntConsumer sendDownUpKeyEvents;

  public NavigationKeyHandlerHost(
      @NonNull BiFunction<Integer, InputConnectionRouter, Boolean> handleSelectionExpending,
      @NonNull IntConsumer sendNavigationKeyEvent,
      @NonNull IntConsumer sendDownUpKeyEvents) {
    this.handleSelectionExpending = handleSelectionExpending;
    this.sendNavigationKeyEvent = sendNavigationKeyEvent;
    this.sendDownUpKeyEvents = sendDownUpKeyEvents;
  }

  @Override
  public boolean handleSelectionExpending(
      int keyEventCode, @NonNull InputConnectionRouter inputConnectionRouter) {
    return handleSelectionExpending.apply(keyEventCode, inputConnectionRouter);
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
