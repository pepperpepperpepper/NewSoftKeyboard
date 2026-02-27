package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPredictionManagerLogProbTest {

  @Test
  public void logSumExpForEqualLogitsIsLogN() {
    final float[] logits = new float[] {0f, 0f};
    final float logSumExp = NeuralPredictionManager.logSumExp(logits);
    assertEquals((float) Math.log(2d), logSumExp, 1e-4f);
  }

  @Test
  public void logProbMatchesSoftmaxForTwoTokens() {
    final float[] logits = new float[] {1f, 0f};
    final float logSumExp = NeuralPredictionManager.logSumExp(logits);

    final float lp0 = NeuralPredictionManager.logProbForTokenId(logits, logSumExp, 0);
    final float lp1 = NeuralPredictionManager.logProbForTokenId(logits, logSumExp, 1);

    final double p0 = Math.exp(lp0);
    final double p1 = Math.exp(lp1);
    assertEquals(1d, p0 + p1, 1e-6d);
    assertTrue(p0 > p1);
  }
}
