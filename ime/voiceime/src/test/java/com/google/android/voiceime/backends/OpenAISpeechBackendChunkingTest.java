/*
 * Copyright (C) 2026 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.voiceime.backends;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskPlainTestRunner;

/**
 * Verifies the settings-layer normalization of the chunking-strategy preference: server-side VAD is
 * only ever emitted as bare {@code {"type": "server_vad"}}, unsupported models/values collapse to
 * {@code "none"}, and no server_vad tuning field is ever forwarded (they make OpenAI's endpoint 500
 * as of 2026-07).
 */
@RunWith(NskPlainTestRunner.class)
public class OpenAISpeechBackendChunkingTest {

  private static final String[] TUNING_FIELDS = {
    "threshold", "silence_duration_ms", "prefix_padding_ms"
  };

  @Test
  public void serverVadOnGpt4oEmitsBareServerVad() {
    assertEquals(
        "{\"type\": \"server_vad\"}",
        OpenAISpeechBackend.sanitizeChunkingStrategy("server_vad", "gpt-4o-transcribe"));
    assertEquals(
        "{\"type\": \"server_vad\"}",
        OpenAISpeechBackend.sanitizeChunkingStrategy("auto", "gpt-4o-mini-transcribe"));
  }

  @Test
  public void tunedServerVadJsonIsStrippedToBare() {
    String tuned =
        "{\"type\": \"server_vad\", \"threshold\": 0.6, \"silence_duration_ms\": 700,"
            + " \"prefix_padding_ms\": 300}";
    String sanitized =
        OpenAISpeechBackend.sanitizeChunkingStrategy(tuned, "gpt-4o-transcribe");
    assertEquals("{\"type\": \"server_vad\"}", sanitized);
    for (String field : TUNING_FIELDS) {
      assertFalse("must not forward tuning field: " + field, sanitized.contains(field));
    }
  }

  @Test
  public void noneIsPreserved() {
    assertEquals("none", OpenAISpeechBackend.sanitizeChunkingStrategy("none", "gpt-4o-transcribe"));
  }

  @Test
  public void whitespacePaddedNoneIsPreserved() {
    // sanitizeChunkingStrategy trims before comparing, so a padded "none" still disables chunking.
    assertEquals(
        "none", OpenAISpeechBackend.sanitizeChunkingStrategy("  none  ", "gpt-4o-transcribe"));
  }

  @Test
  public void nullFallsBackToNone() {
    assertEquals("none", OpenAISpeechBackend.sanitizeChunkingStrategy(null, "gpt-4o-transcribe"));
  }

  @Test
  public void unsupportedModelDisablesChunking() {
    // whisper-1 does not accept chunking_strategy — sending server_vad to it would be rejected.
    assertEquals("none", OpenAISpeechBackend.sanitizeChunkingStrategy("server_vad", "whisper-1"));
  }

  @Test
  public void unknownValueOnSupportedModelDisablesChunking() {
    // An unvetted value is dropped to "none" rather than forwarded verbatim.
    assertEquals(
        "none", OpenAISpeechBackend.sanitizeChunkingStrategy("weird", "gpt-4o-transcribe"));
  }
}
