package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import java.util.ArrayDeque;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class CandidateRerankerTest {

  @Test
  public void rerankDemotesWordsRepeatedInRecentContext() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("one", "two", "three"));

    final List<String> candidates = List.of("two", "four", "five");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("four", "five", "two"), reranked);
  }

  @Test
  public void rerankPromotesLongerCandidateWhenTopRowIsShortTokens() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("keep", "me"));

    final List<String> candidates = List.of("i", "in", "is", "informed", "it");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("i", "informed", "in", "is", "it"), reranked);
  }

  @Test
  public void rerankPromotesTwoContentOptionsWhenTopRowIsFunctionHeavy() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("hello", "world"));

    final List<String> candidates = List.of("the", "of", "and", "cat", "dog");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("the", "cat", "dog", "of", "and"), reranked);
  }

  @Test
  public void rerankPromotesLongestCandidateWhenNoContentWordAvailable() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("hello", "world"));

    final List<String> candidates = List.of("i", "in", "is", "those", "it");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("i", "those", "in", "is", "it"), reranked);
  }

  @Test
  public void rerankDemotesDeterminerAfterDeterminer() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("the"));

    final List<String> candidates = List.of("a", "new", "study");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("new", "study", "a"), reranked);
  }

  @Test
  public void rerankDemotesContentWordRepeatedInWiderContext() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(
        List.of(
            "one", "two", "three", "four", "five", "shadow", "six", "seven", "eight", "nine", "ten",
            "eleven"));

    final List<String> candidates = List.of("shadow", "cat", "dog");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("cat", "dog", "shadow"), reranked);
  }

  @Test
  public void rerankAvoidsAnBeforeConsonantWord() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("an"));

    final List<String> candidates = List.of("circle", "apple");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("apple", "circle"), reranked);
  }

  @Test
  public void rerankAvoidsABeforeVowelWord() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("a"));

    final List<String> candidates = List.of("apple", "banana");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("banana", "apple"), reranked);
  }

  @Test
  public void rerankPromotesContentWordToTopAfterDeterminer() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.addAll(List.of("the"));

    final List<String> candidates = List.of("and", "cat", "dog");
    final List<String> reranked = CandidateReranker.rerank(context, candidates);

    Assert.assertEquals(List.of("and", "cat", "dog"), reranked);
  }
}
