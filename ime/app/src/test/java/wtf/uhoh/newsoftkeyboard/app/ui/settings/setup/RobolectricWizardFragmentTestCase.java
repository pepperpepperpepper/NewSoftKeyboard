package wtf.uhoh.newsoftkeyboard.app.ui.settings.setup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import org.junit.runner.RunWith;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.LooperMode;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentActivityTestCase;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public abstract class RobolectricWizardFragmentTestCase<F extends Fragment>
    extends RobolectricFragmentActivityTestCase<
        RobolectricWizardFragmentTestCase.TestableSetupWizardActivity<F>, F> {

  @NonNull
  protected abstract F createFragment();

  @Override
  protected Fragment getCurrentFragment() {
    return getActivityController().get().mFragment;
  }

  @Override
  protected ActivityController<TestableSetupWizardActivity<F>> createActivityController() {
    TestableSetupWizardActivity<F> activity = new TestableSetupWizardActivity<F>();
    activity.mFragment = createFragment();
    return ActivityController.of(activity);
  }

  public static class TestableSetupWizardActivity<F extends Fragment> extends SetupWizardActivity {
    private F mFragment;

    @NonNull
    @Override
    protected FragmentStateAdapter createPagesAdapter() {
      return new FragmentStateAdapter(this) {

        @NonNull
        @Override
        public Fragment createFragment(int position) {
          return mFragment;
        }

        @Override
        public int getItemCount() {
          return 1;
        }
      };
    }
  }
}
