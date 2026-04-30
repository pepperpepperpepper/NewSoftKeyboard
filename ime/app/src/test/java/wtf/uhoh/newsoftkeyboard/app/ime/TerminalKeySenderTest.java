package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class TerminalKeySenderTest {

  @Test
  public void connectBot_inputClassZero_isTerminal() {
    final EditorInfo info = new EditorInfo();
    info.packageName = "org.connectbot";
    info.inputType = 0;
    Assert.assertTrue(TerminalKeySender.isTerminalEmulation(info));
  }

  @Test
  public void connectBot_typeClassText_isStillTerminal() {
    final EditorInfo info = new EditorInfo();
    info.packageName = "org.connectbot";
    info.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    Assert.assertTrue(TerminalKeySender.isTerminalEmulation(info));
  }

  @Test
  public void juiceSsh_typeClassText_isTerminal() {
    final EditorInfo info = new EditorInfo();
    info.packageName = "com.sonelli.juicessh";
    info.inputType = EditorInfo.TYPE_CLASS_TEXT;
    Assert.assertTrue(TerminalKeySender.isTerminalEmulation(info));
  }

  @Test
  public void irssiConnectBot_anyInputType_isTerminal() {
    final EditorInfo info = new EditorInfo();
    info.packageName = "org.woltage.irssiconnectbot";
    info.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_NORMAL;
    Assert.assertTrue(TerminalKeySender.isTerminalEmulation(info));
  }

  @Test
  public void unknownPackage_isNotTerminal() {
    final EditorInfo info = new EditorInfo();
    info.packageName = "com.example.notTerminal";
    info.inputType = 0;
    Assert.assertFalse(TerminalKeySender.isTerminalEmulation(info));
  }

  @Test
  public void nullEditorInfo_isNotTerminal() {
    Assert.assertFalse(TerminalKeySender.isTerminalEmulation(null));
  }

  @Test
  public void nullPackageName_isNotTerminal() {
    final EditorInfo info = new EditorInfo();
    info.packageName = null;
    info.inputType = 0;
    Assert.assertFalse(TerminalKeySender.isTerminalEmulation(info));
  }
}
