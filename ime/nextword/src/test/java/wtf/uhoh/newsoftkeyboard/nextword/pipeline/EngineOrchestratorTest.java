package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.PredictionEngine;
import wtf.uhoh.newsoftkeyboard.engine.PredictionResult;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class EngineOrchestratorTest {

  @Test
  public void predictAndMergeDisallowsImmediateRepeatOfLastContextToken() {
    final PredictionEngine engine =
        new PredictionEngine() {
          @Override
          public EngineType getType() {
            return EngineType.NGRAM;
          }

          @Override
          public boolean isReady() {
            return true;
          }

          @Override
          public boolean activate() {
            return true;
          }

          @Override
          public void deactivate() {}

          @Override
          public String getLastError() {
            return null;
          }

          @Override
          public PredictionResult predict(String[] contextTokens, int maxResults) {
            return new PredictionResult(List.of("the", "cat"));
          }
        };

    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("the");

    final List<CharSequence> out = new ArrayList<>();
    final EngineOrchestrator.MergeOutcome outcome =
        EngineOrchestrator.predictAndMerge(engine, context, 5, out, 5, false, "test");

    Assert.assertEquals(1, outcome.added);
    Assert.assertTrue(outcome.hadRaw);
    Assert.assertTrue(outcome.hadNormalized);
    Assert.assertEquals(List.of("cat"), out);
  }

  @Test
  public void predictAndMergeDropsOnlyImmediateRepeatSoPipelineCanFallback() {
    final PredictionEngine engine =
        new PredictionEngine() {
          @Override
          public EngineType getType() {
            return EngineType.NGRAM;
          }

          @Override
          public boolean isReady() {
            return true;
          }

          @Override
          public boolean activate() {
            return true;
          }

          @Override
          public void deactivate() {}

          @Override
          public String getLastError() {
            return null;
          }

          @Override
          public PredictionResult predict(String[] contextTokens, int maxResults) {
            return new PredictionResult(List.of("the"));
          }
        };

    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("the");

    final List<CharSequence> out = new ArrayList<>();
    final EngineOrchestrator.MergeOutcome outcome =
        EngineOrchestrator.predictAndMerge(engine, context, 5, out, 5, false, "test");

    Assert.assertEquals(0, outcome.added);
    Assert.assertTrue(outcome.hadRaw);
    Assert.assertFalse(outcome.hadNormalized);
    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void predictAndMergeFiltersDomainLikeTokensInNormalProse() {
    final PredictionEngine engine =
        new PredictionEngine() {
          @Override
          public EngineType getType() {
            return EngineType.NGRAM;
          }

          @Override
          public boolean isReady() {
            return true;
          }

          @Override
          public boolean activate() {
            return true;
          }

          @Override
          public void deactivate() {}

          @Override
          public String getLastError() {
            return null;
          }

          @Override
          public PredictionResult predict(String[] contextTokens, int maxResults) {
            return new PredictionResult(List.of("com", "cat"));
          }
        };

    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("keep");
    context.add("me");

    final List<CharSequence> out = new ArrayList<>();
    final EngineOrchestrator.MergeOutcome outcome =
        EngineOrchestrator.predictAndMerge(engine, context, 5, out, 5, false, "test");

    Assert.assertEquals(1, outcome.added);
    Assert.assertTrue(outcome.hadRaw);
    Assert.assertTrue(outcome.hadNormalized);
    Assert.assertEquals(List.of("cat"), out);
  }

  @Test
  public void predictAndMergeAllowsDomainLikeTokensInDomainContext() {
    final PredictionEngine engine =
        new PredictionEngine() {
          @Override
          public EngineType getType() {
            return EngineType.NGRAM;
          }

          @Override
          public boolean isReady() {
            return true;
          }

          @Override
          public boolean activate() {
            return true;
          }

          @Override
          public void deactivate() {}

          @Override
          public String getLastError() {
            return null;
          }

          @Override
          public PredictionResult predict(String[] contextTokens, int maxResults) {
            return new PredictionResult(List.of("com", "cat"));
          }
        };

    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("example.com");

    final List<CharSequence> out = new ArrayList<>();
    final EngineOrchestrator.MergeOutcome outcome =
        EngineOrchestrator.predictAndMerge(engine, context, 5, out, 5, false, "test");

    Assert.assertEquals(2, outcome.added);
    Assert.assertTrue(outcome.hadRaw);
    Assert.assertTrue(outcome.hadNormalized);
    Assert.assertEquals(List.of("com", "cat"), out);
  }
}
