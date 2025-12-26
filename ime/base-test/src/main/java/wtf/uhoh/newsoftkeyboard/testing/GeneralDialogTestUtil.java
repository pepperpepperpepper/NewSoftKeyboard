package wtf.uhoh.newsoftkeyboard.testing;

import android.app.Dialog;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.Observable;
import net.evendanan.pixel.GeneralDialogController;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDialog;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;

public class GeneralDialogTestUtil {

  public static final AlertDialog NO_DIALOG = Mockito.mock(AlertDialog.class);

  public static AlertDialog getLatestShownDialog() {
    TestRxSchedulers.drainAllTasks();
    return (AlertDialog)
        TestRxSchedulers.blockingGet(
            Observable.fromIterable(ShadowDialog.getShownDialogs())
                .filter(dialog -> dialog instanceof AlertDialog)
                .filter(Dialog::isShowing)
                .filter(
                    dialog ->
                        GeneralDialogController.TAG_VALUE.equals(
                            dialog
                                .getWindow()
                                .getDecorView()
                                .getTag(GeneralDialogController.TAG_ID)))
                .last(NO_DIALOG));
  }

  public static CharSequence getTitleFromDialog(@NonNull Dialog dialog) {
    if (dialog instanceof AlertDialog) {
      return ((TextView) dialog.findViewById(androidx.appcompat.R.id.alertTitle)).getText();
    } else {
      return Shadows.shadowOf(dialog).getTitle();
    }
  }
}
