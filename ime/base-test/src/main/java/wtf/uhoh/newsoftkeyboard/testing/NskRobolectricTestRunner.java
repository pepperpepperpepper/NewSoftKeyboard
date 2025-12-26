package wtf.uhoh.newsoftkeyboard.testing;

import android.os.Looper;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import net.evendanan.testgrouping.TestClassHashingStrategy;
import net.evendanan.testgrouping.TestsGroupingFilter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.robolectric.DefaultTestLifecycle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.util.Logger;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;

/** Just a way to add general things on-top RobolectricTestRunner. */
public class NskRobolectricTestRunner extends RobolectricTestRunner {
  private boolean mGroupingFilterInitialized = false;

  public NskRobolectricTestRunner(Class<?> testClass) throws InitializationError {
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

  @Nonnull
  @Override
  @SuppressWarnings("rawtypes")
  protected Class<? extends TestLifecycle> getTestLifecycleClass() {
    return NskRobolectricTestLifeCycle.class;
  }

  public static class NskRobolectricTestLifeCycle extends DefaultTestLifecycle {
    @Override
    public void beforeTest(Method method) {
      Logger.info("***** Starting test '%s' *****", method);
      TestRxSchedulers.setSchedulers(Looper.getMainLooper(), new PausedExecutorService());
      super.beforeTest(method);
    }

    @Override
    public void afterTest(Method method) {
      Logger.info("***** Finished test '%s' *****", method);
      super.afterTest(method);
      TestRxSchedulers.destroySchedulers();
    }
  }
}
