package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeResourcesHolder;

final class CandidateStripRenderer {

  private static final int OUT_OF_BOUNDS_X_CORD = -1;

  static final class RenderResult {
    @Nullable CharSequence selectedString = null;
    int selectedIndex = -1;
    int totalWidth = 0;

    void reset() {
      selectedString = null;
      selectedIndex = -1;
      totalWidth = 0;
    }
  }

  private final int[] wordWidth;
  private final int[] wordX;

  private float horizontalGap;
  private @Nullable Drawable divider;
  private @Nullable Drawable selectionHighlight;

  private @Nullable Rect bgPadding;
  private @Nullable Drawable lastBackground;

  CandidateStripRenderer(int maxSuggestions) {
    this.wordWidth = new int[maxSuggestions];
    this.wordX = new int[maxSuggestions];
  }

  void onThemeUpdated(
      @NonNull Drawable divider, @NonNull Drawable selectionHighlight, float horizontalGap) {
    this.divider = divider;
    this.selectionHighlight = selectionHighlight;
    this.horizontalGap = horizontalGap;
    invalidateBackground();
    resetCaches();
    divider.setBounds(0, 0, divider.getIntrinsicWidth(), divider.getIntrinsicHeight());
  }

  void invalidateBackground() {
    bgPadding = null;
    lastBackground = null;
  }

  void resetCaches() {
    Arrays.fill(wordWidth, 0);
    Arrays.fill(wordX, 0);
  }

  void render(
      @NonNull Canvas canvas,
      int height,
      @Nullable Drawable background,
      int touchX,
      int scrollX,
      boolean scrolled,
      boolean showingAddToDictionary,
      boolean alwaysUseDrawText,
      int highlightedIndex,
      @NonNull List<CharSequence> suggestions,
      @NonNull Paint paint,
      @NonNull TextPaint textPaint,
      @NonNull ThemeResourcesHolder themeResources,
      @NonNull RenderResult out) {
    final Drawable divider = this.divider;
    final Drawable selectionHighlight = this.selectionHighlight;

    if (divider == null || selectionHighlight == null) {
      out.totalWidth = 0;
      return;
    }

    final Rect bgPadding = ensureBgPadding(background);
    final int dividerYOffset = (height - divider.getMinimumHeight()) / 2;
    final int count = suggestions.size();

    int x = 0;
    for (int i = 0; i < count; i++) {
      CharSequence suggestion = suggestions.get(i);
      if (suggestion == null) {
        continue;
      }
      final int wordLength = suggestion.length();

      paint.setColor(themeResources.getNameTextColor());
      paint.setTypeface(Typeface.DEFAULT);
      if (i == highlightedIndex) {
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(themeResources.getKeyTextColor().getDefaultColor());
      } else if (i != 0 || (wordLength == 1 && count > 1)) {
        // HACK: even if i == 0, we use hint color when this suggestion's length is 1 and
        // there are multiple suggestions, such as the default punctuation list.
        paint.setColor(themeResources.getHintTextColor());
      }

      int candidateWidth;
      if ((candidateWidth = wordWidth[i]) == 0) {
        float textWidth = paint.measureText(suggestion, 0, wordLength);
        candidateWidth = (int) (textWidth + horizontalGap * 2);
        wordWidth[i] = candidateWidth;
      }

      wordX[i] = x;

      if (touchX != OUT_OF_BOUNDS_X_CORD
          && !scrolled
          && touchX + scrollX >= x
          && touchX + scrollX < x + candidateWidth) {
        if (!showingAddToDictionary) {
          canvas.translate(x, 0);
          selectionHighlight.setBounds(0, bgPadding.top, candidateWidth, height);
          selectionHighlight.draw(canvas);
          canvas.translate(-x, 0);
        }
        out.selectedString = suggestion;
        out.selectedIndex = i;
      }

      if (textPaint != paint) {
        textPaint.setTypeface(paint.getTypeface());
        textPaint.setColor(paint.getColor());
      }

      // (+)This is the trick to get RTL/LTR text correct
      if (alwaysUseDrawText) {
        final int y = (int) (height + paint.getTextSize() - paint.descent()) / 2;
        canvas.drawText(suggestion, 0, wordLength, x + candidateWidth / 2f, y, paint);
      } else {
        final int y = (int) (height - paint.getTextSize() + paint.descent()) / 2;
        // no matter what: StaticLayout
        float textX = x + (candidateWidth / 2.0f) - horizontalGap;
        float textY = y - bgPadding.bottom - bgPadding.top;

        canvas.translate(textX, textY);

        StaticLayout suggestionText =
            new StaticLayout(
                suggestion, textPaint, candidateWidth, Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        suggestionText.draw(canvas);

        canvas.translate(-textX, -textY);
      }
      // (-)

      canvas.translate(x + candidateWidth, 0);
      // Draw a divider unless it's after the hint or the last suggested word.
      if (count > 1 && !showingAddToDictionary && i != (count - 1)) {
        canvas.translate(0, dividerYOffset);
        divider.draw(canvas);
        canvas.translate(0, -dividerYOffset);
      }
      canvas.translate(-x - candidateWidth, 0);

      x += candidateWidth;
    }
    out.totalWidth = x;
  }

  @NonNull
  private Rect ensureBgPadding(@Nullable Drawable background) {
    if (bgPadding == null || background != lastBackground) {
      bgPadding = new Rect(0, 0, 0, 0);
      if (background != null) {
        background.getPadding(bgPadding);
      }
      lastBackground = background;
    }
    return bgPadding;
  }
}
