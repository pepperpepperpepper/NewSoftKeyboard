/*
 * Copyright (C) 2026 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.voiceime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskPlainTestRunner;

/**
 * Locks in the invariant that the {@code chunking_strategy} we send to OpenAI's transcription
 * endpoint is only ever bare {@code {"type": "server_vad"}} — never with the tuning fields
 * (threshold / silence_duration_ms / prefix_padding_ms) that OpenAI began rejecting with HTTP 500 in
 * 2026-07.
 */
@RunWith(NskPlainTestRunner.class)
public class OpenAITranscriberChunkingTest {

  private static final String[] TUNING_FIELDS = {
    "threshold", "silence_duration_ms", "prefix_padding_ms"
  };

  @Test
  public void serverVadAndAutoEmitBareServerVad() {
    assertEquals("{\"type\": \"server_vad\"}", OpenAITranscriber.formatChunkingStrategy("server_vad"));
    assertEquals("{\"type\": \"server_vad\"}", OpenAITranscriber.formatChunkingStrategy("auto"));
  }

  @Test
  public void tunedServerVadJsonIsStrippedToBare() {
    // A tuning-bearing value must never reach the wire — those fields are exactly what makes the
    // endpoint 500. Even if one slips in (legacy pref, future option, OpenAI-compatible proxy), it
    // is normalized back to bare server_vad.
    String tuned =
        "{\"type\": \"server_vad\", \"threshold\": 0.6, \"silence_duration_ms\": 700,"
            + " \"prefix_padding_ms\": 300}";
    String formatted = OpenAITranscriber.formatChunkingStrategy(tuned);
    assertEquals("{\"type\": \"server_vad\"}", formatted);
    for (String field : TUNING_FIELDS) {
      assertFalse("must not forward tuning field: " + field, formatted.contains(field));
    }
  }

  @Test
  public void singleTuningFieldAloneIsStrippedToBare() {
    // A single tuning field is enough to make the endpoint 500, so even a server_vad object carrying
    // only one of them must be reduced to bare server_vad.
    for (String field : TUNING_FIELDS) {
      String tuned = "{\"type\": \"server_vad\", \"" + field + "\": 0.6}";
      String formatted = OpenAITranscriber.formatChunkingStrategy(tuned);
      assertEquals("{\"type\": \"server_vad\"}", formatted);
      assertFalse("must not forward tuning field: " + field, formatted.contains(field));
    }
  }

  @Test
  public void noneAndUnknownValuesPassThroughUnchanged() {
    // "none" is filtered out upstream (never reaches formatChunkingStrategy for a real request);
    // any other non-server_vad token is forwarded verbatim rather than being mislabeled server_vad.
    assertEquals("none", OpenAITranscriber.formatChunkingStrategy("none"));
    assertEquals("whatever", OpenAITranscriber.formatChunkingStrategy("whatever"));
  }

  @Test
  public void isServerVadStrategyRecognizesEveryServerVadForm() {
    assertTrue(OpenAITranscriber.isServerVadStrategy("auto"));
    assertTrue(OpenAITranscriber.isServerVadStrategy("server_vad"));
    assertTrue(OpenAITranscriber.isServerVadStrategy("  server_vad  "));
    assertTrue(OpenAITranscriber.isServerVadStrategy("{\"type\":\"server_vad\"}"));
    assertTrue(
        OpenAITranscriber.isServerVadStrategy("{\"type\": \"server_vad\", \"threshold\": 0.6}"));
  }

  @Test
  public void isServerVadStrategyRejectsNonServerVad() {
    assertFalse(OpenAITranscriber.isServerVadStrategy(null));
    assertFalse(OpenAITranscriber.isServerVadStrategy(""));
    assertFalse(OpenAITranscriber.isServerVadStrategy("none"));
    // Not a quoted "server_vad" token, so it must not be treated as server_vad.
    assertFalse(OpenAITranscriber.isServerVadStrategy("not_server_vad"));
  }
}
