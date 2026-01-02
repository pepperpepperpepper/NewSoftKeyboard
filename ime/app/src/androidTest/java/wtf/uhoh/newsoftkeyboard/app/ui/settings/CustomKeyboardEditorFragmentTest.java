package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPackCreator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;

@RunWith(AndroidJUnit4.class)
public class CustomKeyboardEditorFragmentTest {

  private ActivityScenario<MainSettingsActivity> scenario;

  @After
  public void tearDown() {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
  }

  @Test
  public void tappingKeyboardInEditModeOpensEditDialog() throws Exception {
    InstalledKeyboardPack pack = createFreshTestPack("Test Pack");
    launchEditor(pack, "keyboards/main.xml");

    onView(withId(R.id.custom_keyboard_editor_view)).check(matches(isDisplayed()));
    onView(withId(R.id.custom_keyboard_editor_view)).perform(click());

    onView(withText(R.string.custom_keyboards_edit_key_dialog_title)).check(matches(isDisplayed()));
    onView(withText(android.R.string.cancel)).perform(click());
  }

  @Test
  public void tappingKeyboardInTestTypingUpdatesBuffer() throws Exception {
    InstalledKeyboardPack pack = createFreshTestPack("Test Pack");
    launchEditor(pack, "keyboards/main.xml");

    onView(withId(R.id.custom_keyboard_editor_test_typing_switch)).perform(click());
    onView(withId(R.id.custom_keyboard_editor_view)).perform(click());

    onView(withId(R.id.custom_keyboard_editor_test_typing_buffer))
        .check(matches(withText(not(isEmptyOrNullString()))));
  }

  private static InstalledKeyboardPack createFreshTestPack(String name) throws Exception {
    final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    deleteRecursively(new File(targetContext.getFilesDir(), "keyboard_packs"));
    return CustomKeyboardPackCreator.createBasicQwertyKeyboardPack(targetContext, name);
  }

  private void launchEditor(InstalledKeyboardPack pack, String keyboardPath) {
    if (scenario != null) {
      scenario.close();
      scenario = null;
    }
    final var targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final Intent intent =
        new Intent(targetContext, MainSettingsActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    scenario = ActivityScenario.launch(intent);

    scenario.onActivity(
        activity -> {
          var args = new android.os.Bundle();
          args.putString(CustomKeyboardEditorFragment.ARG_PACK_ID, pack.manifest().id());
          args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_ENTRY_ID, "main");
          args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_PATH, keyboardPath);
          androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
              .navigate(R.id.customKeyboardEditorFragment, args);
        });
  }

  private static void deleteRecursively(File file) {
    if (!file.exists()) return;
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) deleteRecursively(child);
    }
    // Best effort cleanup
    //noinspection ResultOfMethodCallIgnored
    file.delete();
  }
}
