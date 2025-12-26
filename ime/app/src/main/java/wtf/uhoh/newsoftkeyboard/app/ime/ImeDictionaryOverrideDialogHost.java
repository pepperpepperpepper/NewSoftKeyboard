package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import androidx.annotation.NonNull;
import net.evendanan.pixel.GeneralDialogController;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ExternalDictionaryFactory;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;

public final class ImeDictionaryOverrideDialogHost implements DictionaryOverrideDialog.Host {

  @NonNull private final ImeServiceBase ime;

  public ImeDictionaryOverrideDialogHost(@NonNull ImeServiceBase ime) {
    this.ime = ime;
  }

  @Override
  public KeyboardDefinition getCurrentAlphabetKeyboard() {
    return ime.getCurrentAlphabetKeyboard();
  }

  @Override
  public ExternalDictionaryFactory getExternalDictionaryFactory() {
    return NskApplicationBase.getExternalDictionaryFactory(ime);
  }

  @Override
  public void showOptionsDialogWithData(
      CharSequence title,
      int iconRes,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener,
      GeneralDialogController.DialogPresenter presenter) {
    ime.showOptionsDialogWithData(title, iconRes, items, listener, presenter);
  }

  @Override
  public Context getContext() {
    return ime;
  }
}
