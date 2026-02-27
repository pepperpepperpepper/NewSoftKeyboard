package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceSessionOverridesStripActionTest extends ImeServiceBaseTest {

  @Test
  public void testShowsIndicatorAndClearsOnClick() {
    final KeyboardViewContainerView container = mImeServiceUnderTest.getInputViewContainer();
    assertNotNull(container);
    assertNull(container.findViewById(R.id.session_overrides_root));

    final var theme = mImeServiceUnderTest.mCurrentTheme;
    assertNotNull(theme);
    final String baseThemeId = theme.getId();
    assertNotNull(baseThemeId);

    try {
      assertTrue(
          mImeServiceUnderTest
              .getProgrammableApiController()
              .setSessionThemePresetForProgrammableApi(baseThemeId));

      final View indicator = container.findViewById(R.id.session_overrides_root);
      assertNotNull(indicator);

      indicator.performClick();
      assertNull(container.findViewById(R.id.session_overrides_root));
    } finally {
      mImeServiceUnderTest.getProgrammableApiController().clearSessionOverridesForProgrammableApi();
    }
  }

  @Test
  public void testRestoresActionStripVisibilityAfterClearIfWasInitiallyHidden() {
    final KeyboardViewContainerView container = mImeServiceUnderTest.getInputViewContainer();
    assertNotNull(container);

    container.setActionsStripVisibility(false);
    assertEquals(View.GONE, getMockCandidateView().getVisibility());

    final var theme = mImeServiceUnderTest.mCurrentTheme;
    assertNotNull(theme);
    final String baseThemeId = theme.getId();
    assertNotNull(baseThemeId);

    try {
      assertTrue(
          mImeServiceUnderTest
              .getProgrammableApiController()
              .setSessionThemePresetForProgrammableApi(baseThemeId));
      assertEquals(View.VISIBLE, getMockCandidateView().getVisibility());

      final View indicator = container.findViewById(R.id.session_overrides_root);
      assertNotNull(indicator);

      indicator.performClick();
      assertNull(container.findViewById(R.id.session_overrides_root));
      assertEquals(View.GONE, getMockCandidateView().getVisibility());
    } finally {
      mImeServiceUnderTest.getProgrammableApiController().clearSessionOverridesForProgrammableApi();
    }
  }
}
