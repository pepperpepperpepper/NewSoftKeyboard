package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.SuggestImpl;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordContextTokenizer;

/** Debug-only tiny API to help instrumentation seed IME context. */
public final class ImeTestApi {
  private static volatile WeakReference<ImeSuggestionsController> sService =
      new WeakReference<>(null);

  private ImeTestApi() {}

  static void setService(ImeSuggestionsController svc) {
    sService = new WeakReference<>(svc);
  }

  public static boolean commitText(String text) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    final InputConnection ic = svc.getCurrentInputConnection();
    if (ic == null) return false;
    // commitText(...) typically triggers selection updates from the editor. Marking it as expected
    // avoids test-only seed commits being misinterpreted as a user cursor move, which can clear
    // next-word context (and in incognito/no-personalized-learning fields we can't reseed).
    svc.markExpectingSelectionUpdate();
    ic.commitText(text, 1);
    // Maintain next-word context for tests that seed editor text directly (bypassing the normal
    // separator/pick commit flows). Only treat a token as "committed" when it is terminated by
    // whitespace, mirroring how words are normally finalized by SPACE/newline.
    final Suggest suggest = svc.getSuggest();
    String lastCommittedToken = "";
    if (suggest != null && text != null && !text.isEmpty()) {
      final StringBuilder token = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        final char c = text.charAt(i);
        if (Character.isWhitespace(c)) {
          if (token.length() > 0) {
            final String committed = token.toString();
            suggest.notifyWordCommitted(committed);
            svc.recordLastCommittedWordForNextSuggestionsForTest(committed);
            lastCommittedToken = committed;
            token.setLength(0);
          }
        } else {
          token.append(c);
        }
      }
    }

    // Seed next-word suggestions without relying on editor readback (some editors return null from
    // getTextBeforeCursor). Only do this when we observed a whitespace-terminated token.
    if (suggest != null && !lastCommittedToken.isEmpty()) {
      svc.markKeepSuggestionsStripWhileIdle();
      svc.setSuggestions(suggest.getNextSuggestions(lastCommittedToken, false), -1);
    } else {
      // Fallback: ask for suggestions update after commit.
      svc.performUpdateSuggestions();
    }
    return true;
  }

  /**
   * Commits text into the current editor and updates the IME's next-word context, but does not
   * request a suggestions update.
   *
   * <p>This is useful for reports that want to capture the initial "fast fallback" next-word strip
   * after an explicit request (for example, before HYBRID async neural refresh lands).
   */
  public static boolean commitTextNoSuggestionsForTest(String text) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    final InputConnection ic = svc.getCurrentInputConnection();
    if (ic == null) return false;
    // Avoid selection-update side effects clearing next-word context in reports that seed editor
    // text via commitText(...). This mirrors how separator/pick flows mark selection updates.
    svc.markExpectingSelectionUpdate();
    ic.commitText(text, 1);

    final Suggest suggest = svc.getSuggest();
    String lastCommittedToken = "";
    if (suggest != null && text != null && !text.isEmpty()) {
      final StringBuilder token = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        final char c = text.charAt(i);
        if (Character.isWhitespace(c)) {
          if (token.length() > 0) {
            final String committed = token.toString();
            suggest.notifyWordCommitted(committed);
            svc.recordLastCommittedWordForNextSuggestionsForTest(committed);
            lastCommittedToken = committed;
            token.setLength(0);
          }
        } else {
          token.append(c);
        }
      }
    }

    if (suggest != null && !lastCommittedToken.isEmpty()) {
      svc.markKeepSuggestionsStripWhileIdle();
    }
    return true;
  }

  /** Schedules the IME's delayed suggestions update (mirrors the normal typing path). */
  public static void postUpdateSuggestions() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    svc.postUpdateSuggestions();
  }

  /** Clears the IME's next-word sentence state (for regression tests). */
  public static void resetNextWordSentence() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    final Suggest suggest = svc.getSuggest();
    if (suggest == null) return;
    suggest.resetNextWordSentence();
  }

  /** Sets the IME's incognito mode state (for privacy/context-visibility tests). */
  public static boolean setIncognitoModeForTest(boolean incognitoMode) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    if (svc instanceof ImeIncognito) {
      // Ensure it persists across onStartInputView(...) by treating it as a user toggle.
      ((ImeIncognito) svc).setIncognito(incognitoMode, /* byUser= */ true);
    } else {
      final Suggest suggest = svc.getSuggest();
      if (suggest == null) return false;
      suggest.setIncognitoMode(incognitoMode);
    }
    return true;
  }

  /** Clears the IME's in-memory "previous word" used for next-word fallback requests. */
  public static void clearLastCommittedWordForNextSuggestionsForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    svc.clearLastCommittedWordForNextSuggestions();
  }

  /** Clears the IME's suggestion strip (for deterministic UI test setup). */
  public static void clearSuggestionsForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    svc.clearSuggestions();
  }

  /** Clears user-generated learning data (next-word + auto-dictionary) for tests. */
  public static void clearLearningDataForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    final Suggest suggest = svc.getSuggest();
    if (suggest == null) return;
    suggest.clearLearningData();
    svc.performUpdateSuggestions();
  }

  /** Immediately performs a typed-suggestions refresh (bypasses the normal debounce delay). */
  public static void performUpdateSuggestionsNowForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    svc.performUpdateSuggestions();
  }

  /** Resets the composing state and clears the suggestion strip (best-effort). */
  public static void abortCorrectionAndResetPredictionStateForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    svc.abortCorrectionAndResetPredictionState(false);
  }

  /** Simulates typing a single codepoint through the IME's normal character pipeline. */
  public static boolean typeCodePointForTest(int codePoint) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    final int[] nearByKeyCodes = new int[] {codePoint};
    // For report harnesses we only simulate the "primary" character (no nearby key codes).
    svc.handleCharacter(
        /* primaryCode= */ codePoint,
        /* key= */ (Keyboard.Key) null,
        /* multiTapIndex= */ 0,
        /* nearByKeyCodes= */ nearByKeyCodes);
    return true;
  }

  /** Simulates typing text through the IME's normal character pipeline. */
  public static boolean typeTextForTest(@NonNull String text) {
    if (TextUtils.isEmpty(text)) return false;
    boolean ok = true;
    int index = 0;
    while (index < text.length()) {
      final int codePoint = Character.codePointAt(text, index);
      ok &= typeCodePointForTest(codePoint);
      index += Character.charCount(codePoint);
    }
    return ok;
  }

  /** Closes dictionaries to flush on-disk state (for persistence tests). */
  public static void closeDictionariesForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    final Suggest suggest = svc.getSuggest();
    if (suggest == null) return;
    suggest.closeDictionaries();
  }

  /** Invokes the IME's separator handling (SPACE/newline/punctuation). */
  public static void handleSeparator(int primaryCode) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    svc.handleSeparator(primaryCode);
  }

  /** Forces the IME's auto-space state (bypasses async pref propagation). */
  public static boolean setAutoSpaceEnabledForTest(boolean enabled) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    svc.setAutoSpaceEnabledForTest(enabled);
    return true;
  }

  /** Returns the IME's current auto-space state. */
  public static boolean isAutoSpaceEnabledForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    return svc.isAutoSpaceEnabledForTest();
  }

  /**
   * Forces next-word suggestions using the token immediately before the cursor. Returns the number
   * of suggestions shown.
   */
  public static int forceNextWordFromCursor() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return 0;
    final InputConnection ic = svc.getCurrentInputConnection();
    if (ic == null) return 0;
    CharSequence before = ic.getTextBeforeCursor(64, 0);
    if (before == null) before = "";
    String prev = extractLastToken(before.toString());
    if (prev.isEmpty()) return 0;
    java.util.List<? extends CharSequence> next = svc.getSuggest().getNextSuggestions(prev, false);
    svc.setSuggestions(next, -1);
    return next == null ? 0 : next.size();
  }

  /**
   * Forces next-word suggestions using the IME's in-memory "last committed token" rather than
   * {@link InputConnection#getTextBeforeCursor(int, int)}.
   *
   * <p>This is useful for tests that simulate editors which don't support readback (where {@code
   * getTextBeforeCursor} can return null).
   */
  public static int forceNextWordFromLastCommittedWordForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return 0;
    final Suggest suggest = svc.getSuggest();
    if (suggest == null) return 0;
    final CharSequence last = svc.lastCommittedWordForNextSuggestions();
    if (TextUtils.isEmpty(last)) return 0;
    java.util.List<? extends CharSequence> next = suggest.getNextSuggestions(last, false);
    svc.setSuggestions(next, -1);
    return next == null ? 0 : next.size();
  }

  /** Returns the IME's current in-memory "previous word" used for next-word suggestions. */
  public static String getLastCommittedWordForNextSuggestionsForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return "";
    final CharSequence last = svc.lastCommittedWordForNextSuggestions();
    return last == null ? "" : last.toString();
  }

  /**
   * Best-effort: attempts to seed the next-word engines' context window from existing editor text
   * using {@link InputConnection#getTextBeforeCursor(int, int)}.
   *
   * <p>Returns a JSON string describing:
   *
   * <ul>
   *   <li>whether seeding was allowed (privacy checks)
   *   <li>whether readback succeeded
   *   <li>tokenization output + last token
   * </ul>
   */
  public static String seedNextWordEngineContextFromEditorTextForTest(
      int maxChars, int maxTokens, int tailChars) {
    final JSONObject out = new JSONObject();
    try {
      out.put("requestedChars", maxChars);
      out.put("maxTokens", maxTokens);
      out.put("tailChars", tailChars);

      final ImeSuggestionsController svc = sService.get();
      if (svc == null) {
        out.put("allowed", false);
        out.put("blockedReason", "no_service");
        return out.toString();
      }

      final EditorInfo info = svc.currentInputEditorInfo();
      if (info == null) {
        out.put("allowed", false);
        out.put("blockedReason", "no_editor_info");
        return out.toString();
      }
      out.put("editorInputType", info.inputType);
      out.put("editorImeOptions", info.imeOptions);

      if (ImeClipboard.isTextPassword(info) || ImeIncognito.isNumberPassword(info)) {
        out.put("allowed", false);
        out.put("blockedReason", "password");
        return out.toString();
      }
      if ((info.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
        out.put("allowed", false);
        out.put("blockedReason", "no_personalized_learning");
        return out.toString();
      }
      if ((info.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
        out.put("allowed", false);
        out.put("blockedReason", "no_suggestions_flag");
        return out.toString();
      }

      final Suggest suggest = svc.getSuggest();
      if (suggest != null && suggest.isIncognitoMode()) {
        out.put("allowed", false);
        out.put("blockedReason", "incognito");
        return out.toString();
      }
      out.put("allowed", true);

      final InputConnection ic = svc.getCurrentInputConnection();
      if (ic == null) {
        out.put("readbackOk", false);
        out.put("readbackMethod", "none");
        out.put("blockedReason", "no_input_connection");
        return out.toString();
      }

      CharSequence before = null;
      String readbackMethod = "none";
      String beforeCursorStatus = "";
      try {
        before = ic.getTextBeforeCursor(maxChars, 0);
      } catch (Throwable t) {
        beforeCursorStatus = "exception";
      }
      if (beforeCursorStatus.isEmpty()) {
        if (before == null) {
          beforeCursorStatus = "null";
        } else if (before.length() == 0) {
          beforeCursorStatus = "empty";
        } else {
          beforeCursorStatus = "ok";
        }
      }
      out.put("getTextBeforeCursor", beforeCursorStatus);

      ExtractedText extracted = null;
      String extractedStatus = "";
      if (!TextUtils.isEmpty(before)) {
        readbackMethod = "getTextBeforeCursor";
      } else {
        try {
          final ExtractedTextRequest request = new ExtractedTextRequest();
          request.hintMaxChars = maxChars;
          request.hintMaxLines = 10;
          extracted = ic.getExtractedText(request, 0);
        } catch (Throwable t) {
          extractedStatus = "exception";
        }
        if (extractedStatus.isEmpty()) {
          if (extracted == null) {
            extractedStatus = "null";
          } else if (TextUtils.isEmpty(extracted.text)) {
            extractedStatus = "empty";
          } else if (extracted.selectionEnd < 0) {
            extractedStatus = "no_selection";
          } else {
            extractedStatus = "ok";
          }
        }
        out.put("getExtractedText", extractedStatus);

        if ("ok".equals(extractedStatus)) {
          final int selEnd = Math.min(extracted.selectionEnd, extracted.text.length());
          final int start = Math.max(0, selEnd - maxChars);
          before = extracted.text.subSequence(start, selEnd);
          readbackMethod = "getExtractedText";
          out.put("extractedStartOffset", extracted.startOffset);
          out.put("extractedSelectionEnd", extracted.selectionEnd);
          out.put("extractedSelectionStart", extracted.selectionStart);
        }
      }

      if (TextUtils.isEmpty(before)) {
        out.put("readbackOk", false);
        out.put("readbackMethod", readbackMethod);
        out.put("returnedChars", 0);
        out.put("seeded", false);
        out.put(
            "blockedReason",
            before == null ? "null_text_before_cursor" : "empty_text_before_cursor");
        return out.toString();
      }

      out.put("readbackOk", true);
      out.put("readbackMethod", readbackMethod);
      out.put("returnedChars", before.length());

      CharSequence after = null;
      String afterCursorStatus = "";
      try {
        after = ic.getTextAfterCursor(2, 0);
      } catch (Throwable t) {
        afterCursorStatus = "exception";
      }
      if (afterCursorStatus.isEmpty()) {
        if (after == null) {
          afterCursorStatus = "null";
        } else if (after.length() == 0) {
          afterCursorStatus = "empty";
        } else {
          afterCursorStatus = "ok";
        }
      }
      out.put("getTextAfterCursor", afterCursorStatus);

      if (isCursorInsideTokenForContextSeeding(before, after, extracted)) {
        final CharSequence trimmed = trimTrailingTokenFragment(before);
        if (trimmed.length() != before.length()) {
          out.put("cursorInsideToken", true);
          out.put("returnedCharsAfterTrim", trimmed.length());
          before = trimmed;
        }
      } else {
        out.put("cursorInsideToken", false);
      }

      if (tailChars > 0) {
        final int len = before.length();
        final int start = Math.max(0, len - tailChars);
        out.put("tail", before.subSequence(start, len).toString());
      }

      final List<String> tokens =
          NextWordContextTokenizer.tokenizeTextBeforeCursor(before, maxTokens);
      final JSONArray tokensJson = new JSONArray();
      for (String t : tokens) tokensJson.put(t);
      out.put("tokens", tokensJson);
      out.put("previousWord", tokens.isEmpty() ? "" : tokens.get(tokens.size() - 1));

      if (suggest != null) {
        suggest.seedNextWordEngineContextFromEditorText(before);
        out.put("seeded", true);
      } else {
        out.put("seeded", false);
        out.put("blockedReason", "no_suggest");
      }
    } catch (Exception e) {
      // best-effort: never crash tests
      try {
        out.put("error", e.toString());
      } catch (Exception ignored) {
      }
    }
    return out.toString();
  }

  private static String extractLastToken(String s) {
    int end = s.length() - 1;
    // trim trailing whitespace
    while (end >= 0 && Character.isWhitespace(s.charAt(end))) end--;
    if (end < 0) return "";
    int start = end;
    while (start >= 0 && !Character.isWhitespace(s.charAt(start))) start--;
    return s.substring(start + 1, end + 1);
  }

  private static boolean isContextTokenChar(char c) {
    if (Character.isLetterOrDigit(c)) return true;
    // allow common intra-token joiners (matches NextWordContextTokenizer)
    return c == '\'' || c == '\u2019' || c == '-' || c == '_';
  }

  private static boolean isCursorInsideTokenForContextSeeding(
      @NonNull CharSequence beforeCursor,
      @Nullable CharSequence afterCursor,
      @Nullable ExtractedText extractedText) {
    if (beforeCursor.length() == 0) return false;
    final char lastBefore = beforeCursor.charAt(beforeCursor.length() - 1);
    if (!isContextTokenChar(lastBefore)) return false;
    if (afterCursor != null && afterCursor.length() > 0) {
      return isContextTokenChar(afterCursor.charAt(0));
    }
    if (afterCursor == null
        && extractedText != null
        && extractedText.text != null
        && extractedText.text.length() > 0) {
      final int selEnd = extractedText.selectionEnd;
      if (selEnd >= 0 && selEnd < extractedText.text.length()) {
        return isContextTokenChar(extractedText.text.charAt(selEnd));
      }
    }
    return false;
  }

  @NonNull
  private static CharSequence trimTrailingTokenFragment(@NonNull CharSequence beforeCursor) {
    int i = beforeCursor.length() - 1;
    while (i >= 0 && isContextTokenChar(beforeCursor.charAt(i))) i--;
    return beforeCursor.subSequence(0, i + 1);
  }

  /** Returns a monotonically increasing count of HYBRID async neural refresh callbacks. */
  public static int getHybridNeuralAsyncListenerInvocationCountForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return 0;
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return 0;
    return ((SuggestImpl) suggest).getHybridNeuralAsyncListenerInvocationCountForTest();
  }

  /** Dumps best-effort HYBRID async neural telemetry as a JSON string. */
  public static String dumpHybridNeuralAsyncDebugStateForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return "{}";
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return "{}";
    return ((SuggestImpl) suggest).dumpHybridNeuralAsyncDebugStateForTest();
  }

  /** Clears the collected neural inference samples (used by tap-chain reports). */
  public static void resetNeuralInferenceSamplesForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return;
    ((SuggestImpl) suggest).resetNeuralInferenceSamplesForTest();
  }

  /** Dumps best-effort per-inference neural samples as a JSON array string. */
  public static String dumpNeuralInferenceSamplesForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return "[]";
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return "[]";
    return ((SuggestImpl) suggest).dumpNeuralInferenceSamplesForTest();
  }

  /** Clears best-effort next-word pipeline debug state (used by tap-chain reports). */
  public static void clearNextWordPipelineDebugStateForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return;
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return;
    ((SuggestImpl) suggest).clearNextWordPipelineDebugStateForTest();
  }

  /** Dumps best-effort next-word pipeline debug state as a JSON object string. */
  public static String dumpNextWordPipelineDebugStateForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return "{}";
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return "{}";
    return ((SuggestImpl) suggest).dumpNextWordPipelineDebugStateForTest();
  }

  /** Dumps best-effort next-word engine context tokens as a JSON array string. */
  public static String dumpNextWordEngineContextTokensForTest() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return "[]";
    final Suggest suggest = svc.getSuggest();
    if (!(suggest instanceof SuggestImpl)) return "[]";
    final java.util.Deque<String> tokens =
        ((SuggestImpl) suggest).getNextWordEngineContextSnapshotForTest();
    final JSONArray out = new JSONArray();
    for (String t : tokens) {
      if (t == null) continue;
      out.put(t);
    }
    return out.toString();
  }

  /**
   * Dumps best-effort IME lifecycle state useful for next-word context visibility reports.
   *
   * <p>This is intentionally minimal and should not be used in production flows.
   */
  public static String dumpStartInputViewStateForTest() {
    final JSONObject out = new JSONObject();
    try {
      final ImeSuggestionsController svc = sService.get();
      if (svc == null) {
        out.put("available", false);
        out.put("blockedReason", "no_service");
        return out.toString();
      }
      out.put("available", true);
      out.put("onStartInputViewCount", svc.getOnStartInputViewCountForTest());
      out.put("lastRestarting", svc.getLastOnStartInputViewRestartingForTest());
      out.put("lastInputType", svc.getLastOnStartInputViewInputTypeForTest());
      out.put("lastImeOptions", svc.getLastOnStartInputViewImeOptionsForTest());
      out.put("lastUptimeMs", svc.getLastOnStartInputViewUptimeMsForTest());
      out.put(
          "lastCommittedWordForNextSuggestions", getLastCommittedWordForNextSuggestionsForTest());
    } catch (Exception e) {
      try {
        out.put("error", e.toString());
      } catch (Exception ignored) {
      }
    }
    return out.toString();
  }
}
