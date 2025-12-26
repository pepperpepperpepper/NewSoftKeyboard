package wtf.uhoh.newsoftkeyboard.app.ime.hosts;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import wtf.uhoh.newsoftkeyboard.app.ime.EmojiSearchController;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickKeyHistoryRecords;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.TagsExtractor;

public final class EmojiSearchControllerHost implements EmojiSearchController.Host {

  private final Supplier<TagsExtractor> quickTextTagsSearcher;
  private final Supplier<QuickKeyHistoryRecords> quickKeyHistoryRecords;
  private final BooleanSupplier handleCloseRequest;
  private final BiConsumer<Integer, Boolean> showToastMessage;
  private final Supplier<KeyboardViewContainerView> inputViewContainer;
  private final Consumer<CharSequence> commitEmojiFromSearch;
  private final Supplier<Context> contextSupplier;

  public EmojiSearchControllerHost(
      @NonNull Supplier<TagsExtractor> quickTextTagsSearcher,
      @NonNull Supplier<QuickKeyHistoryRecords> quickKeyHistoryRecords,
      @NonNull BooleanSupplier handleCloseRequest,
      @NonNull BiConsumer<Integer, Boolean> showToastMessage,
      @NonNull Supplier<KeyboardViewContainerView> inputViewContainer,
      @NonNull Consumer<CharSequence> commitEmojiFromSearch,
      @NonNull Supplier<Context> contextSupplier) {
    this.quickTextTagsSearcher = quickTextTagsSearcher;
    this.quickKeyHistoryRecords = quickKeyHistoryRecords;
    this.handleCloseRequest = handleCloseRequest;
    this.showToastMessage = showToastMessage;
    this.inputViewContainer = inputViewContainer;
    this.commitEmojiFromSearch = commitEmojiFromSearch;
    this.contextSupplier = contextSupplier;
  }

  @Nullable
  @Override
  public TagsExtractor getQuickTextTagsSearcher() {
    return quickTextTagsSearcher.get();
  }

  @NonNull
  @Override
  public QuickKeyHistoryRecords getQuickKeyHistoryRecords() {
    return quickKeyHistoryRecords.get();
  }

  @Override
  public boolean handleCloseRequest() {
    return handleCloseRequest.getAsBoolean();
  }

  @Override
  public void showToastMessage(@StringRes int resId, boolean important) {
    showToastMessage.accept(resId, important);
  }

  @Nullable
  @Override
  public KeyboardViewContainerView getInputViewContainer() {
    return inputViewContainer.get();
  }

  @Override
  public void commitEmojiFromSearch(CharSequence emoji) {
    commitEmojiFromSearch.accept(emoji);
  }

  @NonNull
  @Override
  public Context getContext() {
    return contextSupplier.get();
  }
}
