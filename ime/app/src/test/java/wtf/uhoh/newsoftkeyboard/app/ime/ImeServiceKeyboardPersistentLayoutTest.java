package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.AddOnTestUtils;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceKeyboardPersistentLayoutTest {
  private TestableImeService mImeServiceUnderTest;
  private ServiceController<TestableImeService> mImeServiceController;

  @Before
  public void setUp() throws Exception {
    getApplicationContext().getResources().getConfiguration().keyboard =
        Configuration.KEYBOARD_NOKEYS;
    // enabling the second english keyboard
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    // starting service
    mImeServiceController = Robolectric.buildService(TestableImeService.class);
    mImeServiceUnderTest = mImeServiceController.create().get();

    mImeServiceUnderTest.onCreateInputView();
  }

  @After
  public void tearDown() throws Exception {}

  private void startInputFromPackage(
      @Nullable String packageId, boolean restarting, boolean configChange) {
    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    editorInfo.packageName = packageId;
    editorInfo.fieldId = packageId == null ? 0 : packageId.hashCode();

    mImeServiceUnderTest.onStartInput(editorInfo, restarting);
    if (mImeServiceUnderTest.onShowInputRequested(InputMethod.SHOW_EXPLICIT, configChange)) {
      mImeServiceUnderTest.onStartInputView(editorInfo, restarting);
    }
  }

  private void startInputFromPackage(@Nullable String packageId) {
    startInputFromPackage(packageId, false, false);
  }

  private void finishInput() {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();
  }

  @Test
  public void testSwitchLayouts() {
    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        "DEFAULT_ADD_ON",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_REVERSE_CYCLE);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_CYCLE);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_CYCLE);
    Assert.assertEquals(
        "DEFAULT_ADD_ON",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_REVERSE_CYCLE);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
  }

  @Test
  public void testLayoutPersistentWithPackageId() {
    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();
  }

  @Test
  public void testLayoutPersistentWithPackageIdOnConfigurationChanged() {
    Configuration configuration = mImeServiceUnderTest.getResources().getConfiguration();
    configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
    mImeServiceUnderTest.onConfigurationChanged(configuration);

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
    mImeServiceUnderTest.onConfigurationChanged(configuration);

    startInputFromPackage(
        "com.app2", true /*restarting the same input*/, true /*this is a config change*/);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
    mImeServiceUnderTest.onConfigurationChanged(configuration);

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
  }

  @Test
  public void testLayoutResetPersistentWithPackageIdWhenLayoutDisabled() {
    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, false);

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();
  }

  @Test
  public void testLayoutNotPersistentWithPackageIdIfPrefIsDisabled() {
    final SharedPreferences sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    final SharedPreferences.Editor editor =
        sharedPreferences
            .edit()
            .putBoolean(
                getApplicationContext()
                    .getString(R.string.settings_key_persistent_layout_per_package_id),
                false);
    editor.apply();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app1");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();
  }

  @Test
  public void testPersistentLastLayoutAcrossServiceRestarts() {
    finishInput();

    startInputFromPackage("com.app2");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    mImeServiceController.destroy();

    mImeServiceController = Robolectric.buildService(TestableImeService.class);
    mImeServiceUnderTest = mImeServiceController.create().get();

    mImeServiceUnderTest.onCreateInputView();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();
  }

  @Test
  public void testDoesNotPersistentLastLayoutAcrossServiceRestartsWhenSettingIsDisabled() {
    final SharedPreferences sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    final SharedPreferences.Editor editor =
        sharedPreferences
            .edit()
            .putBoolean(
                getApplicationContext()
                    .getString(R.string.settings_key_persistent_layout_per_package_id),
                false);
    editor.apply();

    finishInput();

    startInputFromPackage("com.app2");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();

    mImeServiceController.destroy();

    mImeServiceController = Robolectric.buildService(TestableImeService.class);
    mImeServiceUnderTest = mImeServiceController.create().get();

    mImeServiceUnderTest.onCreateInputView();

    startInputFromPackage("com.app2");
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardAddOn().getId());
    finishInput();
  }
}
