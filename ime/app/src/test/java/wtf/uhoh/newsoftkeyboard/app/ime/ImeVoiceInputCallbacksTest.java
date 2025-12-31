package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ime.hosts.ImeVoiceInputCallbacks;

public class ImeVoiceInputCallbacksTest extends ImeServiceBaseTest {

  @Test
  public void testShowsVoiceErrorsAsImeStripActions() {
    final ImeVoiceInputCallbacks callbacks =
        new ImeVoiceInputCallbacks(
            mImeServiceUnderTest,
            new ImeVoiceInputCallbacks.Callbacks(() -> {}, ignored -> {}, ignored -> {}),
            () -> false,
            () -> false,
            () -> {});

    callbacks.onVoiceError("Boom");
    assertNotNull(mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.voice_error_root));

    callbacks.updateVoiceInputStatus(VoiceInputState.IDLE);
    assertNull(mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.voice_error_root));
  }
}
