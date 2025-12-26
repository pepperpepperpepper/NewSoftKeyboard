package wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;
import wtf.uhoh.newsoftkeyboard.utils.EmojiUtils;

@RunWith(NskRobolectricTestRunner.class)
public class DefaultGenderPrefTrackerTest {

  @Test
  public void getDefaultGender() {
    DefaultGenderPrefTracker tracker =
        new DefaultGenderPrefTracker(NskApplicationBase.prefs(getApplicationContext()));

    // default value is null
    Assert.assertNull(tracker.getDefaultGender());

    final String[] values =
        getApplicationContext()
            .getResources()
            .getStringArray(R.array.settings_key_default_emoji_gender_values);
    Assert.assertNotNull(values);
    // Enum values + random
    Assert.assertEquals(EmojiUtils.Gender.values().length + 1, values.length);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_emoji_gender, values[1]);
    Assert.assertEquals(EmojiUtils.Gender.Woman, tracker.getDefaultGender());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_emoji_gender, values[2]);
    Assert.assertEquals(EmojiUtils.Gender.Man, tracker.getDefaultGender());

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_default_emoji_gender, values[0] /*person*/);
    Assert.assertNull(tracker.getDefaultGender());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_emoji_gender, values[1]);
    Assert.assertEquals(EmojiUtils.Gender.Woman, tracker.getDefaultGender());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_emoji_gender, "blah");
    // failing to person
    Assert.assertNull(tracker.getDefaultGender());

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_default_emoji_gender, values[3] /*random*/);
    Set<EmojiUtils.Gender> seen = new HashSet<>();
    for (int i = 0; i < 10000; i++) {
      final EmojiUtils.Gender gender = tracker.getDefaultGender();
      Assert.assertNotNull(gender);
      seen.add(gender);
    }

    // Do not give Person as a gender
    Assert.assertEquals(2, seen.size());
    Assert.assertTrue(seen.contains(EmojiUtils.Gender.Woman));
    Assert.assertTrue(seen.contains(EmojiUtils.Gender.Man));
  }

  @Test
  public void testDispose() {
    DefaultGenderPrefTracker tracker =
        new DefaultGenderPrefTracker(NskApplicationBase.prefs(getApplicationContext()));
    Assert.assertFalse(tracker.isDisposed());

    Assert.assertNull(tracker.getDefaultGender());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_emoji_gender, "woman");
    Assert.assertEquals(EmojiUtils.Gender.Woman, tracker.getDefaultGender());

    tracker.dispose();
    Assert.assertTrue(tracker.isDisposed());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_emoji_gender, "man");
    // does not change
    Assert.assertEquals(EmojiUtils.Gender.Woman, tracker.getDefaultGender());
  }
}
