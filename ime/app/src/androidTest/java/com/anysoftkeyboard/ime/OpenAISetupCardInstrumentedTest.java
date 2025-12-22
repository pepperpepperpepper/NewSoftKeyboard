package com.anysoftkeyboard.ime;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.provider.Settings;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.menny.android.anysoftkeyboard.R;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OpenAISetupCardInstrumentedTest {

  private ActivityScenario<MainSettingsActivity> mScenario;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.getSharedPreferences("addon_ui_cards", Context.MODE_PRIVATE).edit().clear().commit();
    androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .clear()
        .commit();
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  @Test
  public void clickPermissionsLink_launchesAppSettings() {
    mScenario = ActivityScenario.launch(MainSettingsActivity.class);
    mScenario.moveToState(Lifecycle.State.RESUMED);
    Intents.init();
    try {
      intending(hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
          .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

      onView(withId(R.id.addon_ui_cards_container)).perform(scrollTo());
      onView(
              Matchers.allOf(
                  isAssignableFrom(TextView.class),
                  withText(Matchers.containsString("microphone and notification access"))))
          .perform(scrollTo(), click());

      intended(hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
    } finally {
      Intents.release();
    }
  }

  @Test
  public void clickOpenAISettingsLink_opensOpenAISettingsFragment() {
    mScenario = ActivityScenario.launch(MainSettingsActivity.class);
    mScenario.moveToState(Lifecycle.State.RESUMED);
    onView(withId(R.id.addon_ui_cards_container)).perform(scrollTo());
    onView(
            Matchers.allOf(
                isAssignableFrom(TextView.class),
                withText(Matchers.containsString("OpenAI Speech-to-Text settings"))))
        .perform(scrollTo(), click());

    onView(withText(R.string.openai_speech_settings_title)).check(matches(isDisplayed()));
  }
}
