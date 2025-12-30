package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeFactory;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperResolver;
import wtf.uhoh.newsoftkeyboard.base.utils.CompatUtils;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayDataImpl;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayDataNormalizer;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayDataOverrider;
import wtf.uhoh.newsoftkeyboard.overlay.OverlyDataCreator;
import wtf.uhoh.newsoftkeyboard.overlay.OverlyDataCreatorForAndroid;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;

public abstract class ImeThemeOverlay extends ImeKeyboardTagsSearcher {
  @VisibleForTesting static final OverlayData INVALID_OVERLAY_DATA = new EmptyOverlayData();

  private OverlyDataCreator mOverlyDataCreator;
  private String mLastOverlayPackage = "";
  protected KeyboardTheme mCurrentTheme;
  private boolean mApplyRemoteAppColors;
  @NonNull private OverlayData mCurrentOverlayData = INVALID_OVERLAY_DATA;
  private KeyboardWallpaperResolver keyboardWallpaperResolver;
  private SharedPreferences mWallpaperPrefsNotToUse;
  private final SharedPreferences.OnSharedPreferenceChangeListener mWallpaperPrefListener =
      (ignored, key) -> {
        final KeyboardTheme theme = mCurrentTheme;
        final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
        if (theme == null || inputViewContainer == null || key == null) return;

        final String themeId = theme.getId();
        if (key.equals(KeyboardWallpaperOverrideStore.dimKey(themeId))
            || key.equals(KeyboardWallpaperOverrideStore.changeKey(themeId))) {
          inputViewContainer.post(
              () -> {
                applyKeyboardWallpaper(inputViewContainer);
                // Re-apply theme to update photo background/dim on the actual keyboard view.
                inputViewContainer.setKeyboardTheme(theme);
                inputViewContainer.setThemeOverlay(mCurrentOverlayData);
              });
        }
      };

  private static Map<String, OverlayData> createOverridesForOverlays() {
    return Collections.emptyMap();
  }

  private boolean isApplyRemoteAppColorsEnabled() {
    return prefs()
        .getBoolean(
            R.string.settings_key_apply_remote_app_colors,
            R.bool.settings_default_apply_remote_app_colors)
        .get();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mApplyRemoteAppColors = isApplyRemoteAppColorsEnabled();
    mOverlyDataCreator = createOverlayDataCreator();
    keyboardWallpaperResolver = new KeyboardWallpaperResolver(this);
    mWallpaperPrefsNotToUse = DirectBootAwareSharedPreferences.create(this);
    mWallpaperPrefsNotToUse.registerOnSharedPreferenceChangeListener(mWallpaperPrefListener);

    addDisposable(
        KeyboardThemeFactory.observeCurrentTheme(getApplicationContext())
            .subscribe(
                this::onThemeChanged,
                GenericOnError.onError("KeyboardThemeFactory.observeCurrentTheme")));

    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_apply_remote_app_colors,
                R.bool.settings_default_apply_remote_app_colors)
            .asObservable()
            .subscribe(
                enabled -> {
                  mApplyRemoteAppColors = enabled;
                  mCurrentOverlayData = INVALID_OVERLAY_DATA;
                  mLastOverlayPackage = "";
                  hideWindow();
                },
                GenericOnError.onError("settings_key_apply_remote_app_colors")));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mWallpaperPrefsNotToUse != null) {
      mWallpaperPrefsNotToUse.unregisterOnSharedPreferenceChangeListener(mWallpaperPrefListener);
      mWallpaperPrefsNotToUse = null;
    }
  }

  protected void onThemeChanged(@NonNull KeyboardTheme theme) {
    mCurrentTheme = theme;

    // we'll need to reload the keyboards
    // TODO(vitalipom) - here recreate the current keyboard and clear all the others

    // and set the theme in the view
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    if (inputViewContainer != null) {
      inputViewContainer.setKeyboardTheme(mCurrentTheme);
      inputViewContainer.setThemeOverlay(mCurrentOverlayData);
      applyKeyboardWallpaper(inputViewContainer);
    }
  }

  protected OverlyDataCreator createOverlayDataCreator() {
    return new OverlyDataCreator() {
      private final OverlyDataCreator mActualCreator =
          new OverlayDataOverrider(
              new OverlayDataNormalizer(
                  new OverlyDataCreatorForAndroid.Light(ImeThemeOverlay.this), 96),
              createOverridesForOverlays());

      @Override
      public OverlayData createOverlayData(ComponentName remoteApp) {
        // read latest pref on every call to respect immediate test toggles
        mApplyRemoteAppColors = isApplyRemoteAppColorsEnabled();
        if (mApplyRemoteAppColors) {
          if (CompatUtils.objectEquals(remoteApp.getPackageName(), mLastOverlayPackage)) {
            return mCurrentOverlayData;
          } else {
            mLastOverlayPackage = remoteApp.getPackageName();
            return mActualCreator.createOverlayData(remoteApp);
          }
        } else {
          return INVALID_OVERLAY_DATA;
        }
      }
    };
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);

    applyThemeOverlay(info);
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    if (inputViewContainer != null) {
      applyKeyboardWallpaper(inputViewContainer);
    }
  }

  protected void applyThemeOverlay(EditorInfo info) {
    // ensure we respect the most recent pref even if the observable hasn't emitted yet
    mApplyRemoteAppColors = isApplyRemoteAppColorsEnabled();

    final Intent launchIntentForPackage =
        info.packageName == null
            ? null
            : getPackageManager().getLaunchIntentForPackage(info.packageName);
    if (launchIntentForPackage != null) {
      mCurrentOverlayData =
          mOverlyDataCreator.createOverlayData(launchIntentForPackage.getComponent());
    } else {
      mCurrentOverlayData = INVALID_OVERLAY_DATA;
      mLastOverlayPackage = "";
    }

    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    if (inputViewContainer != null) {
      inputViewContainer.setThemeOverlay(mCurrentOverlayData);
    }
  }

  @Override
  public void onAddOnsCriticalChange() {
    mLastOverlayPackage = "";
    super.onAddOnsCriticalChange();
  }

  @Override
  public View onCreateInputView() {
    mLastOverlayPackage = "";
    final View view = super.onCreateInputView();
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    inputViewContainer.setKeyboardTheme(mCurrentTheme);
    inputViewContainer.setThemeOverlay(mCurrentOverlayData);
    applyKeyboardWallpaper(inputViewContainer);

    return view;
  }

  private void applyKeyboardWallpaper(@NonNull KeyboardViewContainerView inputViewContainer) {
    inputViewContainer.setBackground(
        keyboardWallpaperResolver.resolveThemeWallpaperOrFallback(mCurrentTheme));
  }

  private static class EmptyOverlayData extends OverlayDataImpl {
    @Override
    public boolean isValid() {
      return false;
    }
  }

  static class ToggleOverlayCreator implements OverlyDataCreator {
    private final OverlyDataCreator mOriginalCreator;
    private final OverlayData mOverrideData;
    private final String mOwner;
    private final ImeThemeOverlay mOverlayController;
    private boolean mUseOverride;

    ToggleOverlayCreator(
        OverlyDataCreator originalCreator,
        ImeThemeOverlay overlayController,
        OverlayData overrideData,
        String owner) {
      mOriginalCreator = originalCreator;
      mOverlayController = overlayController;
      mOverrideData = overrideData;
      mOwner = owner;
    }

    void setToggle(boolean useOverride) {
      mUseOverride = useOverride;

      final EditorInfo currentInputEditorInfo = mOverlayController.currentInputEditorInfo();
      if (currentInputEditorInfo != null) {
        mOverlayController.applyThemeOverlay(currentInputEditorInfo);
      }
    }

    @Override
    public OverlayData createOverlayData(ComponentName remoteApp) {
      if (mUseOverride) {
        return mOverrideData;
      } else {
        return mOriginalCreator.createOverlayData(remoteApp);
      }
    }

    @NonNull
    @Override
    public String toString() {
      return String.format(Locale.ROOT, "ToggleOverlayCreator %s %s", mOwner, super.toString());
    }
  }
}
