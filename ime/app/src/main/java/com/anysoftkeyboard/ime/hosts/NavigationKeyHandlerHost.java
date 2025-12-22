package com.anysoftkeyboard.ime.hosts;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.ime.NavigationKeyHandler;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;

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
