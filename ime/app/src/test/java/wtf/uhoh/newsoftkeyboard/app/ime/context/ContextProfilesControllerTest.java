package wtf.uhoh.newsoftkeyboard.app.ime.context;

import android.view.inputmethod.EditorInfo;
import androidx.test.core.app.ApplicationProvider;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ContextProfilesControllerTest {

  @Test
  public void testAppliesVoiceRulesWithWhitespacePreserved() {
    final var context = ApplicationProvider.getApplicationContext();
    final ContextProfilesStore store = new ContextProfilesStore(context);
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Voice");
    store.setVoiceRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.VoiceRule("how are you?", "hru?", true)));
    store.bindAppToPreset("com.example.app", ContextFieldSelector.TEXT, preset.id);

    final ContextProfilesController controller = new ContextProfilesController(context);
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.packageName = "com.example.app";
    editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT;

    controller.onStartInputView(editorInfo);
    Assert.assertEquals(" hru? ", controller.postProcessVoiceText(" how are you? ", editorInfo));
  }

  @Test
  public void testFieldSelectorOverridesAllFieldsFallback() {
    final var context = ApplicationProvider.getApplicationContext();
    final ContextProfilesStore store = new ContextProfilesStore(context);
    store.setEnabled(true);

    final ContextProfilesStore.Preset allFieldsPreset = store.createPreset("All");
    store.setVoiceRules(
        allFieldsPreset.id,
        Collections.singletonList(
            new ContextProfilesStore.VoiceRule("how are you?", "all-fields", true)));

    final ContextProfilesStore.Preset textPreset = store.createPreset("Text");
    store.setVoiceRules(
        textPreset.id,
        Collections.singletonList(
            new ContextProfilesStore.VoiceRule("how are you?", "text", true)));

    final String packageName = "com.example.app";
    store.bindAppToPreset(packageName, ContextFieldSelector.ALL_FIELDS, allFieldsPreset.id);
    store.bindAppToPreset(packageName, ContextFieldSelector.TEXT, textPreset.id);

    final ContextProfilesController controller = new ContextProfilesController(context);
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.packageName = packageName;
    editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT;

    controller.onStartInputView(editorInfo);
    Assert.assertEquals("text", controller.postProcessVoiceText("how are you?", editorInfo));
  }

  @Test
  public void testSuggestRuleProvidesSuggestionResult() {
    final var context = ApplicationProvider.getApplicationContext();
    final ContextProfilesStore store = new ContextProfilesStore(context);
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Voice");
    store.setVoiceRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.VoiceRule("how are you?", "hru?", false)));
    store.bindAppToPreset("com.example.app", ContextFieldSelector.TEXT, preset.id);

    final ContextProfilesController controller = new ContextProfilesController(context);
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.packageName = "com.example.app";
    editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT;

    controller.onStartInputView(editorInfo);
    final VoiceTextPostProcessResult result =
        controller.postProcessVoiceTextWithSuggestion(" how are you? ", editorInfo);
    Assert.assertEquals(" how are you? ", result.textToCommit());
    Assert.assertNotNull(result.suggestion());
    Assert.assertEquals("hru?", result.suggestion().suggestionText());
    Assert.assertEquals(" how are you? ", result.suggestion().committedText());
    Assert.assertEquals(" hru? ", result.suggestion().replacementText());
  }

  @Test
  public void testSuggestRuleIsSuppressedInNoSuggestionsFields() {
    final var context = ApplicationProvider.getApplicationContext();
    final ContextProfilesStore store = new ContextProfilesStore(context);
    store.setEnabled(true);
    final ContextProfilesStore.Preset preset = store.createPreset("Voice");
    store.setVoiceRules(
        preset.id,
        Collections.singletonList(
            new ContextProfilesStore.VoiceRule("how are you?", "hru?", false)));
    store.bindAppToPreset("com.example.app", ContextFieldSelector.TEXT, preset.id);

    final ContextProfilesController controller = new ContextProfilesController(context);
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.packageName = "com.example.app";
    editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    controller.onStartInputView(editorInfo);
    final VoiceTextPostProcessResult result =
        controller.postProcessVoiceTextWithSuggestion(" how are you? ", editorInfo);
    Assert.assertEquals(" how are you? ", result.textToCommit());
    Assert.assertNull(result.suggestion());
  }
}
