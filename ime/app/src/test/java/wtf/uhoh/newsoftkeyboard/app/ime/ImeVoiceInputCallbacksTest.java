package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import java.util.concurrent.atomic.AtomicReference;
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

  @Test
  public void testDismissingErrorStripClearsErrorStateToIdle() {
    final AtomicReference<VoiceInputState> lastState = new AtomicReference<>(VoiceInputState.IDLE);
    final ImeVoiceInputCallbacks callbacks =
        new ImeVoiceInputCallbacks(
            mImeServiceUnderTest,
            new ImeVoiceInputCallbacks.Callbacks(
                () -> {},
                ignored -> {},
                state -> {
                  lastState.set(state);
                }),
            () -> false,
            () -> false,
            () -> {});

    callbacks.updateVoiceInputStatus(VoiceInputState.ERROR);
    callbacks.onVoiceError("Boom");
    assertNotNull(mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.voice_error_root));

    mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.voice_error_root).performClick();

    assertSame(VoiceInputState.IDLE, lastState.get());
    assertNull(mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.voice_error_root));
  }
}
