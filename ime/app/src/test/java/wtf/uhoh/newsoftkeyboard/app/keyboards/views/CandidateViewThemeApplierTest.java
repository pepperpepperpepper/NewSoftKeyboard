package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class CandidateViewThemeApplierTest {

  @Test
  public void testAppliesKeyFontFamilyOverrideToSuggestionStrip() {
    final Context context = getApplicationContext();
    final String themeId = "test-theme-candidate-font-family";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);
    overridesStore.setKeyFontFamily(themeId, "monospace");

    final Paint paint = new Paint();
    paint.setTypeface(Typeface.DEFAULT);

    CandidateViewThemeApplier.applyTheme(context, theme, new ThemeOverlayCombiner(), paint, true);

    Assert.assertNotNull(paint.getTypeface());
    Assert.assertEquals(Typeface.NORMAL, paint.getTypeface().getStyle());
    Assert.assertNotSame(Typeface.DEFAULT, paint.getTypeface());
  }

  @Test
  public void testAppliesKeyFontStyleOverrideToSuggestionStrip() {
    final Context context = getApplicationContext();
    final String themeId = "test-theme-candidate-font-style";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);
    overridesStore.setKeyFontFamily(themeId, "default");
    overridesStore.setKeyFontStyle(themeId, Typeface.BOLD);

    final Paint paint = new Paint();
    paint.setTypeface(Typeface.DEFAULT);

    CandidateViewThemeApplier.applyTheme(context, theme, new ThemeOverlayCombiner(), paint, true);

    Assert.assertNotNull(paint.getTypeface());
    Assert.assertEquals(Typeface.BOLD, paint.getTypeface().getStyle());
  }

  @Test
  public void testSuggestionFontStyleOverrideBeatsKeyStyleOverride() {
    final Context context = getApplicationContext();
    final String themeId = "test-theme-candidate-style-override";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);
    overridesStore.setKeyFontFamily(themeId, "default");
    overridesStore.setKeyFontStyle(themeId, Typeface.BOLD);
    overridesStore.setSuggestionFontStyle(themeId, Typeface.ITALIC);

    final Paint paint = new Paint();
    paint.setTypeface(Typeface.DEFAULT);

    CandidateViewThemeApplier.applyTheme(context, theme, new ThemeOverlayCombiner(), paint, true);

    Assert.assertNotNull(paint.getTypeface());
    Assert.assertEquals(Typeface.ITALIC, paint.getTypeface().getStyle());
  }

  @Test
  public void testSuggestionFontFamilyOverrideDoesNotInheritKeyStyle() {
    final Context context = getApplicationContext();
    final String themeId = "test-theme-candidate-family-override";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);
    overridesStore.setKeyFontFamily(themeId, "default");
    overridesStore.setKeyFontStyle(themeId, Typeface.ITALIC);
    overridesStore.setSuggestionFontFamily(themeId, "serif");

    final Paint paint = new Paint();
    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

    CandidateViewThemeApplier.applyTheme(context, theme, new ThemeOverlayCombiner(), paint, true);

    Assert.assertNotNull(paint.getTypeface());
    Assert.assertEquals(Typeface.BOLD, paint.getTypeface().getStyle());
  }

  @Test
  public void testSkipsSuggestionTypographyOverridesWhenDisabled() {
    final Context context = getApplicationContext();
    final String themeId = "test-theme-candidate-disabled-overrides";
    final KeyboardTheme theme = createLocalTheme(context, themeId);

    final KeyboardThemeUserOverridesStore overridesStore =
        new KeyboardThemeUserOverridesStore(context);
    overridesStore.setKeyFontFamily(themeId, "monospace");

    final Paint paint = new Paint();
    final Typeface initialTypeface = Typeface.DEFAULT;
    paint.setTypeface(initialTypeface);

    CandidateViewThemeApplier.applyTheme(context, theme, new ThemeOverlayCombiner(), paint, false);

    Assert.assertSame(initialTypeface, paint.getTypeface());
  }

  private static KeyboardTheme createLocalTheme(Context context, String themeId) {
    return new KeyboardTheme(
        context,
        context,
        1 /*apiVersion*/,
        themeId,
        "Test Theme",
        R.style.NskKeyboardBaseTheme,
        0,
        0,
        0,
        0,
        false,
        "",
        0);
  }
}
