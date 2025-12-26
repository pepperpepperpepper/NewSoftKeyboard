package wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.OnKeyboardActionListener;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class FrameKeyboardViewClickListenerTest {

  @Test
  public void testOnClickClose() throws Exception {
    OnKeyboardActionListener keyboardActionListener = Mockito.mock(OnKeyboardActionListener.class);
    FrameKeyboardViewClickListener listener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    Mockito.verifyZeroInteractions(keyboardActionListener);
    View view = new View(getApplicationContext());
    view.setId(R.id.quick_keys_popup_close);
    listener.onClick(view);
    Mockito.verify(keyboardActionListener).onKey(KeyCodes.CANCEL, null, 0, null, true);
    Mockito.verifyNoMoreInteractions(keyboardActionListener);
  }

  @Test
  public void testOnClickBackSpace() throws Exception {
    OnKeyboardActionListener keyboardActionListener = Mockito.mock(OnKeyboardActionListener.class);
    FrameKeyboardViewClickListener listener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    Mockito.verifyZeroInteractions(keyboardActionListener);
    View view = new View(getApplicationContext());
    view.setId(R.id.quick_keys_popup_backspace);
    listener.onClick(view);
    Mockito.verify(keyboardActionListener).onKey(KeyCodes.DELETE, null, 0, null, true);
    Mockito.verifyNoMoreInteractions(keyboardActionListener);
  }

  @Test
  public void testOnClickSearch() throws Exception {
    OnKeyboardActionListener keyboardActionListener = Mockito.mock(OnKeyboardActionListener.class);
    FrameKeyboardViewClickListener listener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    Mockito.verifyZeroInteractions(keyboardActionListener);
    View view = new View(getApplicationContext());
    view.setId(R.id.quick_keys_popup_search);
    listener.onClick(view);
    Mockito.verify(keyboardActionListener).onKey(KeyCodes.EMOJI_SEARCH, null, 0, null, true);
    Mockito.verifyNoMoreInteractions(keyboardActionListener);
  }

  @Test
  public void testOnClickMedia() throws Exception {
    OnKeyboardActionListener keyboardActionListener = Mockito.mock(OnKeyboardActionListener.class);
    FrameKeyboardViewClickListener listener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    Mockito.verifyZeroInteractions(keyboardActionListener);
    View view = new View(getApplicationContext());
    view.setId(R.id.quick_keys_popup_quick_keys_insert_media);
    listener.onClick(view);
    Mockito.verify(keyboardActionListener).onKey(KeyCodes.IMAGE_MEDIA_POPUP, null, 0, null, true);
    Mockito.verifyNoMoreInteractions(keyboardActionListener);
  }

  @Test
  public void testOnClearEmoji() throws Exception {
    OnKeyboardActionListener keyboardActionListener = Mockito.mock(OnKeyboardActionListener.class);
    FrameKeyboardViewClickListener listener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    Mockito.verifyZeroInteractions(keyboardActionListener);
    View view = new View(getApplicationContext());
    view.setId(R.id.quick_keys_popup_delete_recently_used_smileys);
    listener.onClick(view);
    Mockito.verify(keyboardActionListener)
        .onKey(KeyCodes.CLEAR_QUICK_TEXT_HISTORY, null, 0, null, true);
    Mockito.verify(keyboardActionListener).onKey(KeyCodes.QUICK_TEXT_POPUP, null, 0, null, true);
    Mockito.verifyNoMoreInteractions(keyboardActionListener);
  }

  @Test
  public void testOnClickSetting() throws Exception {
    OnKeyboardActionListener keyboardActionListener = Mockito.mock(OnKeyboardActionListener.class);
    FrameKeyboardViewClickListener listener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    Mockito.verifyZeroInteractions(keyboardActionListener);
    View view = new View(getApplicationContext());
    view.setId(R.id.quick_keys_popup_quick_keys_settings);
    listener.onClick(view);
    Intent expectedIntent =
        new Intent(
            Intent.ACTION_VIEW,
            Uri.parse(getApplicationContext().getString(R.string.deeplink_url_quick_text)),
            getApplicationContext(),
            MainSettingsActivity.class);
    expectedIntent.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_NO_HISTORY
            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

    Intent settingIntent =
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity();
    Assert.assertEquals(
        expectedIntent.getComponent().flattenToString(),
        settingIntent.getComponent().flattenToString());
    Assert.assertEquals(expectedIntent.getFlags(), settingIntent.getFlags());
    // closes the keyboard
    Mockito.verify(keyboardActionListener).onKey(KeyCodes.CANCEL, null, 0, null, true);
    Mockito.verifyNoMoreInteractions(keyboardActionListener);
  }
}
