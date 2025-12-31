package wtf.uhoh.newsoftkeyboard.app.ime;

import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class VoiceStatusRendererTest {

  @Test
  public void testUpdateVoiceInputStatusUpdatesViewState() {
    final InputViewBinder inputView = Mockito.mock(InputViewBinder.class);
    final VoiceStatusRenderer renderer = new VoiceStatusRenderer();

    renderer.updateVoiceInputStatus(inputView, VoiceInputState.RECORDING);
    Mockito.verify(inputView).setVoiceInputState(VoiceInputState.RECORDING);

    renderer.updateVoiceInputStatus(inputView, VoiceInputState.RECORDING);
    Mockito.verify(inputView, Mockito.times(1)).setVoiceInputState(VoiceInputState.RECORDING);

    renderer.updateVoiceInputStatus(inputView, VoiceInputState.WAITING);
    Mockito.verify(inputView).setVoiceInputState(VoiceInputState.WAITING);

    renderer.updateVoiceInputStatus(inputView, VoiceInputState.IDLE);
    Mockito.verify(inputView).setVoiceInputState(VoiceInputState.IDLE);
  }

  @Test
  public void testUpdateVoiceKeyStateUpdatesViewState() {
    final InputViewBinder inputView = Mockito.mock(InputViewBinder.class);
    final VoiceStatusRenderer renderer = new VoiceStatusRenderer();

    renderer.updateVoiceKeyState(true, inputView);
    Mockito.verify(inputView).setVoice(true, false);

    renderer.updateVoiceKeyState(false, inputView);
    Mockito.verify(inputView).setVoice(false, false);

    Assert.assertEquals(VoiceInputState.IDLE, renderer.getCurrentState());
  }
}
