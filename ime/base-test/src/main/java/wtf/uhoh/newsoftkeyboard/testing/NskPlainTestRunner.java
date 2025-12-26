package wtf.uhoh.newsoftkeyboard.testing;

import net.evendanan.testgrouping.TestClassHashingStrategy;
import net.evendanan.testgrouping.TestsGroupingFilter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/** Just a way to add general things on-top BlockJUnit4ClassRunner. */
public class NskPlainTestRunner extends BlockJUnit4ClassRunner {
  private boolean mGroupingFilterInitialized = false;

  public NskPlainTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  public void run(RunNotifier notifier) {
    ensureGroupingFilterInitialized();
    super.run(notifier);
  }

  private void ensureGroupingFilterInitialized() {
    if (mGroupingFilterInitialized) {
      return;
    }
    mGroupingFilterInitialized = true;
    TestsGroupingFilter.addTestsGroupingFilterWithSystemPropertiesData(
        this, new TestClassHashingStrategy(), false /*so running from AS will work*/);
  }
}
