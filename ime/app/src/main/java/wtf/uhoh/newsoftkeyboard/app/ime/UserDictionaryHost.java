package wtf.uhoh.newsoftkeyboard.app.ime;

import java.util.function.Supplier;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;

/** Lightweight host for {@link UserDictionaryWorker}. */
final class UserDictionaryHost implements UserDictionaryWorker.Host {

  private final Supplier<Suggest> suggestSupplier;
  private final Supplier<CandidateView> candidateViewSupplier;

  UserDictionaryHost(
      Supplier<Suggest> suggestSupplier, Supplier<CandidateView> candidateViewSupplier) {
    this.suggestSupplier = suggestSupplier;
    this.candidateViewSupplier = candidateViewSupplier;
  }

  @Override
  public Suggest suggest() {
    return suggestSupplier.get();
  }

  @Override
  public CandidateView candidateView() {
    return candidateViewSupplier.get();
  }
}
