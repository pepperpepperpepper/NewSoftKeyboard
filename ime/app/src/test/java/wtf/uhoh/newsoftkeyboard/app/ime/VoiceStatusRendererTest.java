package wtf.uhoh.newsoftkeyboard.app.ime;

import com.anysoftkeyboard.api.KeyCodes;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class VoiceStatusRendererTest {

  @Test
  public void testRestoresSpaceLabelAfterVoiceStatusEnds() {
    KeyboardDefinition keyboard = Mockito.mock(KeyboardDefinition.class);
    Mockito.when(keyboard.getKeyboardId()).thenReturn("kbd-1");

    Keyboard.Key spaceKey = Mockito.mock(Keyboard.Key.class);
    Mockito.when(spaceKey.getPrimaryCode()).thenReturn(KeyCodes.SPACE);
    spaceKey.label = "Original Space";

    Mockito.when(keyboard.getKeys()).thenReturn(List.of(spaceKey));

    VoiceStatusRenderer renderer = new VoiceStatusRenderer();

    renderer.updateVoiceInputStatus(keyboard, null, VoiceInputState.RECORDING);
    Assert.assertEquals("Recording", spaceKey.label);

    renderer.updateVoiceInputStatus(keyboard, null, VoiceInputState.WAITING);
    Assert.assertEquals("Waiting", spaceKey.label);

    renderer.updateVoiceInputStatus(keyboard, null, VoiceInputState.IDLE);
    Assert.assertEquals("Original Space", spaceKey.label);
  }

  @Test
  public void testRestoresNullSpaceLabelAfterVoiceStatusEnds() {
    KeyboardDefinition keyboard = Mockito.mock(KeyboardDefinition.class);
    Mockito.when(keyboard.getKeyboardId()).thenReturn("kbd-2");

    Keyboard.Key spaceKey = Mockito.mock(Keyboard.Key.class);
    Mockito.when(spaceKey.getPrimaryCode()).thenReturn(KeyCodes.SPACE);
    spaceKey.label = null;

    Mockito.when(keyboard.getKeys()).thenReturn(List.of(spaceKey));

    VoiceStatusRenderer renderer = new VoiceStatusRenderer();

    renderer.updateVoiceInputStatus(keyboard, null, VoiceInputState.RECORDING);
    Assert.assertEquals("Recording", spaceKey.label);

    renderer.updateVoiceInputStatus(keyboard, null, VoiceInputState.IDLE);
    Assert.assertNull(spaceKey.label);
  }
}
