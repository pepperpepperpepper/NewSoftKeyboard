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
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPackCreator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;

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

  @Test
  public void tappingLetterKeyInTestTypingProducesTextAndDeleteClearsIt() throws Exception {
    InstalledKeyboardPack pack = createFreshTestPack("Test Pack");
    launchEditor(pack, "keyboards/main.xml");

    onView(withId(R.id.custom_keyboard_editor_view)).check(matches(isDisplayed()));
    onView(withId(R.id.custom_keyboard_editor_test_typing_switch)).perform(click());

    String expectedLetter = getExpectedCodePointFromKey(113 /* q */);
    float[] letterCoordinates = getKeyCenterCoordinatesByPrimaryCode(113 /* q */);
    onView(withId(R.id.custom_keyboard_editor_view))
        .perform(clickAtViewCoordinates(letterCoordinates[0], letterCoordinates[1]));
    onView(withId(R.id.custom_keyboard_editor_test_typing_buffer))
        .check(matches(withText(expectedLetter)));

    float[] deleteCoordinates =
        getKeyCenterCoordinatesByPrimaryCode(com.anysoftkeyboard.api.KeyCodes.DELETE);
    onView(withId(R.id.custom_keyboard_editor_view))
        .perform(clickAtViewCoordinates(deleteCoordinates[0], deleteCoordinates[1]));
    onView(withId(R.id.custom_keyboard_editor_test_typing_buffer)).check(matches(withText("")));
  }

  private static InstalledKeyboardPack createFreshTestPack(String name) throws Exception {
    final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    deleteRecursively(new File(targetContext.getFilesDir(), "keyboard_packs"));
    return CustomKeyboardPackCreator.createBasicQwertyKeyboardPack(targetContext, name);
  }

  private static ViewAction clickAtViewCoordinates(float xInViewPx, float yInViewPx) {
    return new GeneralClickAction(
        Tap.SINGLE,
        view -> {
          int[] location = new int[2];
          view.getLocationOnScreen(location);
          return new float[] {location[0] + xInViewPx, location[1] + yInViewPx};
        },
        Press.FINGER);
  }

  private float[] getKeyCenterCoordinatesByPrimaryCode(int primaryCode) {
    final float[] coordinates = new float[2];
    scenario.onActivity(
        activity -> {
          KeyboardView view = activity.findViewById(R.id.custom_keyboard_editor_view);
          KeyboardDefinition keyboard = view.getKeyboard();
          if (keyboard == null || keyboard.getKeys().isEmpty()) {
            throw new AssertionError("Keyboard not loaded.");
          }

          int keyIndex = findKeyIndexByPrimaryCode(keyboard, primaryCode);
          Keyboard.Key key = keyboard.getKeys().get(keyIndex);

          float x = view.getPaddingLeft() + key.x + key.width / 2f;
          float baseY = view.getPaddingTop() + key.y + key.height / 2f;
          float y = findYThatHitsKey(view, keyIndex, x, baseY, key.height);

          coordinates[0] = x;
          coordinates[1] = y;
        });
    return coordinates;
  }

  private String getExpectedCodePointFromKey(int primaryCode) {
    final String[] result = new String[1];
    scenario.onActivity(
        activity -> {
          KeyboardView view = activity.findViewById(R.id.custom_keyboard_editor_view);
          KeyboardDefinition keyboard = view.getKeyboard();
          if (keyboard == null || keyboard.getKeys().isEmpty()) {
            throw new AssertionError("Keyboard not loaded.");
          }

          int keyIndex = findKeyIndexByPrimaryCode(keyboard, primaryCode);
          Keyboard.Key key = keyboard.getKeys().get(keyIndex);

          boolean isShifted = view.getKeyDetector().isKeyShifted(key);
          int codePoint = key.getCodeAtIndex(0, isShifted);
          result[0] = new String(Character.toChars(codePoint));
        });
    return result[0];
  }

  private static int findKeyIndexByPrimaryCode(KeyboardDefinition keyboard, int primaryCode) {
    for (int i = 0; i < keyboard.getKeys().size(); i++) {
      if (keyboard.getKeys().get(i).getPrimaryCode() == primaryCode) return i;
    }
    throw new AssertionError("Key with primaryCode " + primaryCode + " not found.");
  }

  private static float findYThatHitsKey(
      KeyboardView view, int expectedKeyIndex, float x, float baseY, int searchRangePx) {
    int yStep = Math.max(1, searchRangePx / 8);

    int initial = view.getKeyDetector().getKeyIndexAndNearbyCodes((int) x, (int) baseY, null);
    if (initial == expectedKeyIndex) return baseY;

    for (int delta = -searchRangePx; delta <= searchRangePx; delta += yStep) {
      int y = (int) (baseY + delta);
      int found = view.getKeyDetector().getKeyIndexAndNearbyCodes((int) x, y, null);
      if (found == expectedKeyIndex) return baseY + delta;
    }

    throw new AssertionError(
        "Unable to find tap coordinate for key index "
            + expectedKeyIndex
            + " (initialDetectorIndex="
            + initial
            + ").");
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
