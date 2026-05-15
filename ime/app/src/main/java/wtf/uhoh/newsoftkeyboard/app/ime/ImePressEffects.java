package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import app.cash.copper.rx2.RxContentResolver;
import com.anysoftkeyboard.api.KeyCodes;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.android.NightMode;
import wtf.uhoh.newsoftkeyboard.app.android.PowerSaving;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibrator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.AboveKeyPositionCalculator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.AboveKeyboardPositionCalculator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.KeyPreviewsController;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.KeyPreviewsManager;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.NullKeyPreviewsManager;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.PositionCalculator;
import wtf.uhoh.newsoftkeyboard.app.prefs.AnimationsLevel;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.rx.GenericOnError;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public abstract class ImePressEffects extends ImeClipboard {

  private static final float SILENT = 0.0f;
  private static final float SYSTEM_VOLUME = -1.0f;
  private static final String HAPTIC_FEEDBACK_INTENSITY_SETTING = "haptic_feedback_intensity";
  private static final String KEYBOARD_VIBRATION_ENABLED_SETTING = "keyboard_vibration_enabled";
  @NonNull private final PublishSubject<Long> mKeyPreviewSubject = PublishSubject.create();

  @NonNull
  private final PublishSubject<Boolean> mKeyPreviewForPasswordSubject = PublishSubject.create();

  private AudioManager mAudioManager;
  private float mCustomSoundVolume = SILENT;
  private PressVibrator mVibrator;
  private boolean mUseSystemHapticFeedback = false;
  private boolean mSystemWideHapticEnabled = true;
  private boolean mSystemKeyboardVibrationEnabled = true;
  private int mSystemVibrationFallbackDuration = 0;
  private boolean mVibratorError = false;
  private int mConfiguredKeyPressVibrationDuration = 0;
  private int mConfiguredLongPressVibrationDuration = 0;
  private int mConfiguredSystemVibrationFallbackDuration = 0;
  private boolean mLastSoundPowerSavingState = false;
  private boolean mLastSoundNightModeState = false;
  private boolean mLastSoundOnPref = false;
  private boolean mLastUseCustomSoundVolumePref = false;
  private int mLastCustomSoundVolumeLevelPref = 0;
  private boolean mLastVibrationPowerSavingState = false;
  private int mLastVibrationDurationPref = 0;
  private boolean mLastLongPressPowerSavingState = false;
  private boolean mLastVibrateOnLongPressPref = false;
  private int mLastVibrationPrimaryCode = 0;
  private boolean mLastVibrationLongPress = false;
  private long mLastVibrationAttemptUptimeMs = 0L;
  @NonNull private String mLastVibrationPath = "none";
  @NonNull private String mSoundStateError = "";
  @NonNull private String mKeyPressVibrationStateError = "";
  @NonNull private String mLongPressVibrationStateError = "";
  @NonNull private String mSystemVibrationStateError = "";
  @NonNull private String mSystemVibrationFallbackStateError = "";
  @NonNull private KeyPreviewsController mKeyPreviewController = new NullKeyPreviewsManager();

  @VisibleForTesting
  interface HapticFeedbackPerformer {
    boolean perform(@NonNull View view, int effect, int flags);
  }

  @VisibleForTesting static volatile HapticFeedbackPerformer sHapticFeedbackPerformer;

  private static final class SystemVibrationSettings {
    final boolean mUseSystemVibration;
    final boolean mSystemWideHapticEnabled;
    final boolean mKeyboardVibrationEnabled;

    SystemVibrationSettings(
        boolean useSystemVibration,
        boolean systemWideHapticEnabled,
        boolean keyboardVibrationEnabled) {
      mUseSystemVibration = useSystemVibration;
      mSystemWideHapticEnabled = systemWideHapticEnabled;
      mKeyboardVibrationEnabled = keyboardVibrationEnabled;
    }
  }

  @VisibleForTesting
  static @Nullable Vibrator selectBestVibrator(
      @Nullable Vibrator systemVibrator, @Nullable Vibrator legacyVibrator) {
    // On some devices/emulators, VibratorManager#getDefaultVibrator can return a non-null vibrator
    // instance that reports having no hardware, while the legacy vibrator service is functional.
    // Prefer the first vibrator that reports having hardware. If neither reports having hardware,
    // prefer the VibratorManager-backed vibrator when available (some OEM builds appear to
    // misreport hasVibrator() for the legacy service).
    final boolean systemHasVibrator = systemVibrator != null && systemVibrator.hasVibrator();
    final boolean legacyHasVibrator = legacyVibrator != null && legacyVibrator.hasVibrator();
    if (systemVibrator != null && (systemHasVibrator || !legacyHasVibrator)) {
      return systemVibrator;
    } else if (legacyVibrator != null) {
      return legacyVibrator;
    } else {
      return systemVibrator;
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    final Vibrator systemVibrator;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      final VibratorManager vibratorManager =
          (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
      systemVibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
    } else {
      systemVibrator = null;
    }
    final Vibrator legacyVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    final Vibrator vibratorToUse = selectBestVibrator(systemVibrator, legacyVibrator);
    mVibrator = NskApplicationBase.getDeviceSpecific().createPressVibrator(vibratorToUse);

    addDisposable(
        Observable.combineLatest(
                PowerSaving.observePowerSavingState(
                    getApplicationContext(),
                    R.string.settings_key_power_save_mode_sound_control,
                    R.bool.settings_default_false),
                NightMode.observeNightModeState(
                    getApplicationContext(),
                    R.string.settings_key_night_mode_sound_control,
                    R.bool.settings_default_false),
                prefs()
                    .getBoolean(R.string.settings_key_sound_on, R.bool.settings_default_sound_on)
                    .asObservable(),
                prefs()
                    .getBoolean(
                        R.string.settings_key_use_custom_sound_volume,
                        R.bool.settings_default_false)
                    .asObservable(),
                prefs()
                    .getInteger(
                        R.string.settings_key_custom_sound_volume,
                        R.integer.settings_default_zero_value)
                    .asObservable(),
                (powerState, nightState, soundOn, useCustomVolume, customVolumeLevel) -> {
                  mLastSoundPowerSavingState = powerState;
                  mLastSoundNightModeState = nightState;
                  mLastSoundOnPref = soundOn;
                  mLastUseCustomSoundVolumePref = useCustomVolume;
                  mLastCustomSoundVolumeLevelPref = customVolumeLevel;
                  if (powerState) return SILENT;
                  if (nightState) return SILENT;
                  if (!soundOn) return SILENT;

                  if (useCustomVolume) {
                    return customVolumeLevel / 100f;
                  } else {
                    return SYSTEM_VOLUME;
                  }
                })
            .subscribe(
                customVolume -> {
                  mSoundStateError = "";
                  if (mCustomSoundVolume != customVolume) {
                    if (customVolume == SILENT) {
                      mAudioManager.unloadSoundEffects();
                    } else if (mCustomSoundVolume == SILENT) {
                      mAudioManager.loadSoundEffects();
                    }
                  }
                  mCustomSoundVolume = customVolume;
                  // demo
                  performKeySound(KeyCodes.SPACE);
                },
                t -> {
                  mSoundStateError = t.getClass().getSimpleName() + ": " + t.getMessage();
                  Logger.w(TAG, t, "Failed to read custom volume prefs");
                }));

    addDisposable(
        Observable.combineLatest(
                PowerSaving.observePowerSavingState(
                    getApplicationContext(),
                    R.string.settings_key_power_save_mode_vibration_control,
                    R.bool.settings_default_false),
                prefs()
                    .getInteger(
                        R.string.settings_key_vibrate_on_key_press_duration_int,
                        R.integer.settings_default_vibrate_on_key_press_duration_int)
                    .asObservable(),
                (powerState, vibrationDuration) -> {
                  mLastVibrationPowerSavingState = powerState;
                  mLastVibrationDurationPref = vibrationDuration;
                  return powerState ? 0 : vibrationDuration;
                })
            .subscribe(
                value -> {
                  mKeyPressVibrationStateError = "";
                  mVibratorError = false;
                  mConfiguredKeyPressVibrationDuration = value;
                  mVibrator.setDuration(value);
                  // demo
                  performKeyVibration(KeyCodes.SPACE, false);
                },
                t -> {
                  mKeyPressVibrationStateError =
                      t.getClass().getSimpleName() + ": " + t.getMessage();
                  Logger.w(TAG, t, "Failed to get vibrate duration");
                }));

    addDisposable(
        Observable.combineLatest(
                PowerSaving.observePowerSavingState(
                    getApplicationContext(),
                    R.string.settings_key_power_save_mode_vibration_control,
                    R.bool.settings_default_false),
                prefs()
                    .getBoolean(
                        R.string.settings_key_vibrate_on_long_press,
                        R.bool.settings_default_vibrate_on_long_press)
                    .asObservable(),
                prefs()
                    .getInteger(
                        R.string.settings_key_vibrate_on_key_press_duration_int,
                        R.integer.settings_default_vibrate_on_key_press_duration_int)
                    .asObservable(),
                (powerState, vibrateOnLongPress, keyPressDuration) -> {
                  mLastLongPressPowerSavingState = powerState;
                  mLastVibrateOnLongPressPref = vibrateOnLongPress;
                  if (powerState || !vibrateOnLongPress) return 0;
                  return keyPressDuration;
                })
            .subscribe(
                value -> {
                  mLongPressVibrationStateError = "";
                  mVibratorError = false;
                  mConfiguredLongPressVibrationDuration = value;
                  mVibrator.setLongPressDuration(value);
                  // demo
                  performKeyVibration(KeyCodes.SPACE, true);
                },
                t -> {
                  mLongPressVibrationStateError =
                      t.getClass().getSimpleName() + ": " + t.getMessage();
                  Logger.w(TAG, t, "Failed to get long-press vibrate duration");
                }));

    addDisposable(
        Observable.combineLatest(
                PowerSaving.observePowerSavingState(
                    getApplicationContext(),
                    R.string.settings_key_power_save_mode_vibration_control,
                    R.bool.settings_default_false),
                prefs()
                    .getInteger(
                        R.string.settings_key_vibrate_on_key_press_duration_int,
                        R.integer.settings_default_vibrate_on_key_press_duration_int)
                    .asObservable(),
                RxContentResolver.observeQuery(
                        getContentResolver(),
                        Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED),
                        null,
                        null,
                        null,
                        null,
                        true,
                        RxSchedulers.mainThread())
                    .subscribeOn(RxSchedulers.background())
                    .observeOn(RxSchedulers.mainThread())
                    .map(
                        query ->
                            Settings.System.getInt(
                                getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, -1))
                    .doOnError(
                        t ->
                            Logger.w(
                                TAG,
                                t,
                                "Failed to read system haptic enabled state from Settings.System."
                                    + " Assuming disabled."))
                    .onErrorReturnItem(0),
                RxContentResolver.observeQuery(
                        getContentResolver(),
                        Settings.Secure.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED),
                        null,
                        null,
                        null,
                        null,
                        true,
                        RxSchedulers.mainThread())
                    .subscribeOn(RxSchedulers.background())
                    .observeOn(RxSchedulers.mainThread())
                    .map(
                        query ->
                            Settings.Secure.getInt(
                                getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, -1))
                    .doOnError(
                        t ->
                            Logger.w(
                                TAG,
                                t,
                                "Failed to read system haptic enabled state from Settings.Secure."
                                    + " Assuming disabled."))
                    .onErrorReturnItem(0),
                RxContentResolver.observeQuery(
                        getContentResolver(),
                        Settings.System.getUriFor(HAPTIC_FEEDBACK_INTENSITY_SETTING),
                        null,
                        null,
                        null,
                        null,
                        true,
                        RxSchedulers.mainThread())
                    .subscribeOn(RxSchedulers.background())
                    .observeOn(RxSchedulers.mainThread())
                    .map(
                        query ->
                            Settings.System.getInt(
                                getContentResolver(), HAPTIC_FEEDBACK_INTENSITY_SETTING, -1))
                    .doOnError(
                        t ->
                            Logger.w(
                                TAG,
                                t,
                                "Failed to read system haptic intensity from Settings.System."
                                    + " Assuming unknown."))
                    .onErrorReturnItem(-1),
                RxContentResolver.observeQuery(
                        getContentResolver(),
                        Settings.Secure.getUriFor(HAPTIC_FEEDBACK_INTENSITY_SETTING),
                        null,
                        null,
                        null,
                        null,
                        true,
                        RxSchedulers.mainThread())
                    .subscribeOn(RxSchedulers.background())
                    .observeOn(RxSchedulers.mainThread())
                    .map(
                        query ->
                            Settings.Secure.getInt(
                                getContentResolver(), HAPTIC_FEEDBACK_INTENSITY_SETTING, -1))
                    .doOnError(
                        t ->
                            Logger.w(
                                TAG,
                                t,
                                "Failed to read system haptic intensity from Settings.Secure."
                                    + " Assuming unknown."))
                    .onErrorReturnItem(-1),
                RxContentResolver.observeQuery(
                        getContentResolver(),
                        Settings.System.getUriFor(KEYBOARD_VIBRATION_ENABLED_SETTING),
                        null,
                        null,
                        null,
                        null,
                        true,
                        RxSchedulers.mainThread())
                    .subscribeOn(RxSchedulers.background())
                    .observeOn(RxSchedulers.mainThread())
                    .map(
                        query ->
                            Settings.System.getInt(
                                getContentResolver(), KEYBOARD_VIBRATION_ENABLED_SETTING, -1))
                    .doOnError(
                        t ->
                            Logger.w(
                                TAG,
                                t,
                                "Failed to read system keyboard vibration enabled state from"
                                    + " Settings.System. Assuming unknown."))
                    .onErrorReturnItem(-1),
                RxContentResolver.observeQuery(
                        getContentResolver(),
                        Settings.Secure.getUriFor(KEYBOARD_VIBRATION_ENABLED_SETTING),
                        null,
                        null,
                        null,
                        null,
                        true,
                        RxSchedulers.mainThread())
                    .subscribeOn(RxSchedulers.background())
                    .observeOn(RxSchedulers.mainThread())
                    .map(
                        query ->
                            Settings.Secure.getInt(
                                getContentResolver(), KEYBOARD_VIBRATION_ENABLED_SETTING, -1))
                    .doOnError(
                        t ->
                            Logger.w(
                                TAG,
                                t,
                                "Failed to read system keyboard vibration enabled state from"
                                    + " Settings.Secure. Assuming unknown."))
                    .onErrorReturnItem(-1),
                (powerState,
                    vibrationDuration,
                    systemWideHapticEnabledSystem,
                    systemWideHapticEnabledSecure,
                    systemWideHapticIntensitySystem,
                    systemWideHapticIntensitySecure,
                    keyboardVibrationEnabledSystem,
                    keyboardVibrationEnabledSecure) -> {
                  final boolean systemVibration = vibrationDuration < 0;
                  // Some Android builds store the global "haptic feedback enabled" toggle under
                  // either Settings.System or Settings.Secure. Treat 0 in either location as
                  // disabled. If neither key is present, assume enabled.
                  final boolean systemWideHapticEnabled =
                      (systemWideHapticEnabledSystem < 0 || systemWideHapticEnabledSystem > 0)
                          && (systemWideHapticEnabledSecure < 0
                              || systemWideHapticEnabledSecure > 0);

                  // On some Android builds, "Touch feedback intensity" is stored in either
                  // Settings.System or Settings.Secure. If we can read any of them and it is 0,
                  // treat touch-haptics as disabled. If neither key is present, assume enabled.
                  final boolean systemWideHapticIntensityEnabled =
                      (systemWideHapticIntensitySystem < 0 || systemWideHapticIntensitySystem > 0)
                          && (systemWideHapticIntensitySecure < 0
                              || systemWideHapticIntensitySecure > 0);

                  // On newer Android builds, IME vibration can be gated behind a dedicated system
                  // toggle (separate from generic touch feedback).
                  // Treat 0 in either location as disabled. If neither key is present, assume
                  // enabled.
                  final boolean keyboardVibrationEnabled =
                      (keyboardVibrationEnabledSystem < 0 || keyboardVibrationEnabledSystem > 0)
                          && (keyboardVibrationEnabledSecure < 0
                              || keyboardVibrationEnabledSecure > 0);

                  return new SystemVibrationSettings(
                      !powerState && systemVibration,
                      systemWideHapticEnabled && systemWideHapticIntensityEnabled,
                      keyboardVibrationEnabled);
                })
            .subscribe(
                value -> {
                  mSystemVibrationStateError = "";
                  mVibratorError = false;
                  mUseSystemHapticFeedback = value.mUseSystemVibration;
                  mSystemWideHapticEnabled = value.mSystemWideHapticEnabled;
                  mSystemKeyboardVibrationEnabled = value.mKeyboardVibrationEnabled;
                  // In system vibration mode, use system predefined effects when available.
                  // On API 29+, we prefer view-based haptic-feedback when system-wide touch haptics
                  // are enabled and fall back to vibrator-based system effects otherwise.
                  mVibrator.setUseSystemVibration(
                      value.mUseSystemVibration, value.mSystemWideHapticEnabled);
                  mVibrator.setSystemKeyboardVibrationEnabled(value.mKeyboardVibrationEnabled);
                  // demo
                  performKeyVibration(KeyCodes.SPACE, false);
                },
                t -> {
                  mSystemVibrationStateError = t.getClass().getSimpleName() + ": " + t.getMessage();
                  Logger.w(TAG, t, "Failed to read system vibration pref");
                }));

    addDisposable(
        Observable.combineLatest(
                PowerSaving.observePowerSavingState(
                    getApplicationContext(),
                    R.string.settings_key_power_save_mode_vibration_control,
                    R.bool.settings_default_false),
                prefs()
                    .getInteger(
                        R.string.settings_key_system_vibration_fallback_duration_int,
                        R.integer.settings_default_system_vibration_fallback_duration_int)
                    .asObservable(),
                (powerState, vibrationDuration) -> powerState ? 0 : vibrationDuration)
            .subscribe(
                value -> {
                  mSystemVibrationFallbackStateError = "";
                  mVibratorError = false;
                  mSystemVibrationFallbackDuration = value;
                  mConfiguredSystemVibrationFallbackDuration = value;
                  mVibrator.setSystemVibrationFallbackDuration(value);
                  // demo
                  performKeyVibration(KeyCodes.SPACE, false);
                },
                t -> {
                  mSystemVibrationFallbackStateError =
                      t.getClass().getSimpleName() + ": " + t.getMessage();
                  Logger.w(TAG, t, "Failed to read system vibration fallback duration pref");
                }));

    addDisposable(
        Observable.combineLatest(
                prefs()
                    .getBoolean(
                        R.string.settings_key_key_press_shows_preview_popup,
                        R.bool.settings_default_key_press_shows_preview_popup)
                    .asObservable(),
                AnimationsLevel.createPrefsObservable(this),
                prefs()
                    .getString(
                        R.string.settings_key_key_press_preview_popup_position,
                        R.string.settings_default_key_press_preview_popup_position)
                    .asObservable(),
                mKeyPreviewSubject.startWith(0L),
                mKeyPreviewForPasswordSubject.startWith(false).distinctUntilChanged(),
                this::createKeyPreviewController)
            .subscribe(
                controller -> onNewControllerOrInputView(controller, getInputView()),
                GenericOnError.onError("key-preview-controller-setup")));
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    // A previous vibrator exception might have been transient (e.g., service created while
    // not actively showing UI). Reset to allow trying again while the IME is actually visible.
    mVibratorError = false;
    mKeyPreviewForPasswordSubject.onNext(isTextPassword(info) || isNumberPassword(info));
  }

  @VisibleForTesting
  protected void onNewControllerOrInputView(
      KeyPreviewsController controller, InputViewBinder inputViewBinder) {
    mKeyPreviewController.destroy();
    mKeyPreviewController = controller;
    if (inputViewBinder instanceof KeyboardViewBase) {
      ((KeyboardViewBase) inputViewBinder).setKeyPreviewController(controller);
    }
  }

  @Override
  protected void onThemeChanged(@NonNull KeyboardTheme theme) {
    super.onThemeChanged(theme);
    // triggering a new controller creation
    mKeyPreviewSubject.onNext(SystemClock.uptimeMillis());
  }

  @Override
  public View onCreateInputView() {
    final View view = super.onCreateInputView();
    // triggering a new controller creation
    mKeyPreviewSubject.onNext(SystemClock.uptimeMillis());
    return view;
  }

  @NonNull
  private KeyPreviewsController createKeyPreviewController(
      Boolean enabled,
      AnimationsLevel animationsLevel,
      String position,
      Long random /*ignoring this one*/,
      Boolean isPasswordField) {
    if (enabled
        && animationsLevel != AnimationsLevel.None
        && Boolean.FALSE.equals(isPasswordField)) {
      final PositionCalculator positionCalculator;
      final int maxPopups;
      if ("above_key".equals(position)) {
        positionCalculator = new AboveKeyPositionCalculator();
        maxPopups = getResources().getInteger(R.integer.maximum_instances_of_preview_popups);
      } else {
        positionCalculator = new AboveKeyboardPositionCalculator();
        maxPopups = 1;
      }
      return new KeyPreviewsManager(this, positionCalculator, maxPopups);
    } else {
      return new NullKeyPreviewsManager();
    }
  }

  private void performKeySound(int primaryCode) {
    if (mCustomSoundVolume != SILENT) {
      final int keyFX;
      switch (primaryCode) {
        case 13:
        case KeyCodes.ENTER:
          keyFX = AudioManager.FX_KEYPRESS_RETURN;
          break;
        case KeyCodes.DELETE:
          keyFX = AudioManager.FX_KEYPRESS_DELETE;
          break;
        case KeyCodes.SPACE:
          keyFX = AudioManager.FX_KEYPRESS_SPACEBAR;
          break;
        case KeyCodes.SHIFT:
        case KeyCodes.SHIFT_LOCK:
        case KeyCodes.CTRL:
        case KeyCodes.CTRL_LOCK:
        case KeyCodes.MODE_ALPHABET:
        case KeyCodes.MODE_SYMBOLS:
        case KeyCodes.KEYBOARD_MODE_CHANGE:
        case KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE:
        case KeyCodes.ALT:
          keyFX = AudioManager.FX_KEY_CLICK;
          break;
        default:
          keyFX = AudioManager.FX_KEYPRESS_STANDARD;
      }
      if (mCustomSoundVolume == SYSTEM_VOLUME) {
        mAudioManager.playSoundEffect(keyFX);
      } else {
        mAudioManager.playSoundEffect(keyFX, mCustomSoundVolume);
      }
    }
  }

  private void performKeyVibration(int primaryCode, boolean longPress) {
    mLastVibrationPrimaryCode = primaryCode;
    mLastVibrationLongPress = longPress;
    mLastVibrationAttemptUptimeMs = SystemClock.uptimeMillis();
    mLastVibrationPath = "start";

    final boolean systemHapticsMode = mUseSystemHapticFeedback;
    final boolean systemWideHapticEnabled = mSystemWideHapticEnabled;
    final boolean systemKeyboardVibrationEnabled = mSystemKeyboardVibrationEnabled;
    final boolean shouldSystemFallbackVibrate =
        systemHapticsMode && !longPress && mSystemVibrationFallbackDuration > 0;
    final boolean hasVibratorService = mVibrator.hasVibrator();

    if (systemHapticsMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      // In system-haptics mode, prefer View.performHapticFeedback(...) (HeliBoard-style).
      // We pass FLAG_IGNORE_GLOBAL_SETTING in system mode, so this can still work even when the
      // system-wide touch-haptics toggle (or intensity) is off or inaccessible.
      // However, on some devices the framework can report an effect as "handled" while producing
      // no physical haptics when system-wide touch haptics are disabled (for example, when the
      // user sets touch feedback intensity to none/0). When we detect that case, treat return
      // values as hints only so we still attempt the vibrator-based path.
      final boolean didHaptic =
          performSystemHapticFeedback(
              longPress, systemWideHapticEnabled && systemKeyboardVibrationEnabled);
      if (didHaptic) {
        mLastVibrationPath =
            shouldSystemFallbackVibrate
                ? "system_haptic_feedback+system_fallback"
                : "system_haptic_feedback";
        if (shouldSystemFallbackVibrate) {
          try {
            mVibrator.vibrateSystemVibrationFallback();
          } catch (Exception e) {
            mVibratorError = true;
            Logger.w(TAG, e, "Failed to interact with vibrator while running system fallback.");
          }
        }
        return;
      }

      // If system-wide haptics are disabled or view haptics failed, prefer vibrator-based system
      // effects when available.
      if (hasVibratorService) {
        if (!mVibratorError) {
          try {
            mVibrator.vibrate(longPress);
            if (shouldSystemFallbackVibrate) {
              mVibrator.vibrateSystemVibrationFallback();
              mLastVibrationPath = "system_vibrator+system_fallback";
            } else {
              mLastVibrationPath = "system_vibrator";
            }
            return;
          } catch (Exception e) {
            mVibratorError = true;
            Logger.w(
                TAG,
                e,
                "Failed to interact with vibrator using system effects. Falling back to a safer"
                    + " vibration path.");
          }
        }

        try {
          if (shouldSystemFallbackVibrate) {
            mVibrator.vibrateSystemVibrationFallback();
            mLastVibrationPath = "system_fallback_only";
          } else {
            mVibrator.vibrateFallback(longPress);
            mLastVibrationPath = "vibrator_fallback";
          }
          return;
        } catch (Exception e) {
          mVibratorError = true;
          Logger.w(
              TAG, e, "Failed to interact with vibrator. Falling back to system haptic-feedback.");
        }
      }

      // If system-wide haptics are enabled, we already attempted system haptic-feedback above.
      // Only try it here when system-wide touch haptics are disabled (or unknown) and we have no
      // viable vibrator path.
      if (!systemWideHapticEnabled) {
        mLastVibrationPath = "last_chance_system_haptic_feedback";
        performSystemHapticFeedback(longPress, false);
      } else {
        mLastVibrationPath = "system_haptic_noop";
      }
      return;
    }

    // After a vibrator exception we fall through to view-haptic; skip that substitution when the
    // user has set this press type to silent (otherwise "off" leaks back into a view-haptic buzz).
    if (systemHapticsMode || (mVibratorError && hasConfiguredVibration(longPress))) {
      final boolean didHaptic = performSystemHapticFeedback(longPress, !systemHapticsMode);
      if (didHaptic && !shouldSystemFallbackVibrate) {
        mLastVibrationPath = "legacy_system_haptic_feedback";
        return;
      }
    }

    try {
      if (systemHapticsMode) {
        if (shouldSystemFallbackVibrate) {
          mVibrator.vibrateSystemVibrationFallback();
          mLastVibrationPath = "legacy_system_fallback";
        } else if (longPress) {
          mVibrator.vibrate(true);
          mLastVibrationPath = "legacy_system_long_press";
        } else {
          mVibrator.vibrateFallback(false);
          mLastVibrationPath = "legacy_system_vibrator_fallback";
        }
      } else {
        mVibrator.vibrate(longPress);
        mLastVibrationPath = "legacy_vibrator";
      }
    } catch (Exception e) {
      mVibratorError = true;
      Logger.w(TAG, e, "Failed to interact with vibrator. Falling back to system haptic-feedback.");
      mLastVibrationPath = "legacy_exception_system_haptic_fallback";
      performSystemHapticFeedback(longPress, !systemHapticsMode);
    }
  }

  private boolean hasConfiguredVibration(boolean longPress) {
    return longPress
        ? mConfiguredLongPressVibrationDuration != 0
        : mConfiguredKeyPressVibrationDuration != 0;
  }

  private boolean performSystemHapticFeedback(boolean longPress) {
    return performSystemHapticFeedback(longPress, true);
  }

  private boolean performSystemHapticFeedback(boolean longPress, boolean trustReturnValues) {
    // We generally trust the framework return values, but in "system haptics" mode some devices can
    // report an effect as "handled" while producing no physical haptics. When we don't trust return
    // values, we still attempt view haptic-feedback as a best-effort, but we don't use it as a
    // signal to skip vibrator-based haptics.
    // Historically we trusted LONG_PRESS return values, but some devices can also report it as
    // "handled" while producing no physical haptics. When running in "use system vibration" mode,
    // treat return values as hints for long-press too so we can still attempt vibrator-based
    // haptics.
    if (longPress && !mUseSystemHapticFeedback) trustReturnValues = true;

    int flags = 0;
    // In system vibration mode, the user has explicitly opted in to haptics from this IME.
    // Align with HeliBoard: ignore the global setting, but do not override the view's own
    // haptic-feedback enable state.
    if (mUseSystemHapticFeedback) {
      flags |= HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING;
    } else {
      // In non-system mode, system haptic-feedback is a fallback path. Prefer producing some
      // haptic feedback even if the view's setting is disabled.
      flags |= HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING;
    }
    final int primaryEffect;
    final int fallbackEffect;
    if (longPress) {
      primaryEffect = HapticFeedbackConstants.LONG_PRESS;
      fallbackEffect = primaryEffect;
    } else {
      // Prefer the keyboard-specific effect over the legacy virtual-key effect.
      // In system-haptics mode, we try both effects because some devices report an effect as
      // "handled"
      // but produce no haptics.
      primaryEffect = HapticFeedbackConstants.KEYBOARD_TAP;
      fallbackEffect = HapticFeedbackConstants.VIRTUAL_KEY;
    }

    final View inputView;
    final InputViewBinder inputViewBinder = getInputView();
    if (inputViewBinder instanceof View) {
      inputView = (View) inputViewBinder;
    } else {
      inputView = null;
    }
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();

    if (!trustReturnValues) {
      // Best-effort: attempt haptic feedback on the input view and, if it does not handle the
      // request at all, on its container too. Return values are treated as hints only, since some
      // devices report "handled" while producing no physical haptics.
      boolean didPrimary = false;
      boolean didFallback = false;
      if (inputView != null) {
        didPrimary = performHapticFeedback(inputView, primaryEffect, flags);
        if (primaryEffect != fallbackEffect) {
          didFallback = performHapticFeedback(inputView, fallbackEffect, flags);
        }
      }
      if (inputViewContainer != null && (inputView == null || (!didPrimary && !didFallback))) {
        performHapticFeedback(inputViewContainer, primaryEffect, flags);
        if (primaryEffect != fallbackEffect) {
          performHapticFeedback(inputViewContainer, fallbackEffect, flags);
        }
      }
      return false;
    }

    boolean didPrimary = false;
    boolean didFallback = false;

    if (inputView != null) {
      didPrimary = performHapticFeedback(inputView, primaryEffect, flags);
      didFallback =
          primaryEffect != fallbackEffect
              && performHapticFeedback(inputView, fallbackEffect, flags);
      if (didPrimary || didFallback) return true;
    }
    if (inputViewContainer != null) {
      if (inputView == null || (!didPrimary && !didFallback)) {
        didPrimary = performHapticFeedback(inputViewContainer, primaryEffect, flags);
        didFallback =
            primaryEffect != fallbackEffect
                && performHapticFeedback(inputViewContainer, fallbackEffect, flags);
        if (didPrimary || didFallback) return true;
      }
    }

    return false;
  }

  private boolean performHapticFeedback(@NonNull View view, int effect, int flags) {
    final HapticFeedbackPerformer performer = sHapticFeedbackPerformer;
    if (performer != null) return performer.perform(view, effect, flags);
    return view.performHapticFeedback(effect, flags);
  }

  @Override
  protected void performHapticFeedbackForUserAction() {
    performKeyVibration(KeyCodes.SPACE, false);
  }

  @VisibleForTesting
  protected AudioManager getAudioManager() {
    return mAudioManager;
  }

  @VisibleForTesting
  protected Vibrator getVibrator() {
    return mVibrator.getVibrator();
  }

  @VisibleForTesting
  /* package */ boolean isUsingSystemHapticFeedback() {
    return mUseSystemHapticFeedback;
  }

  @Override
  public void onPress(int primaryCode) {
    super.onPress(primaryCode);

    performKeySound(primaryCode);
    performKeyVibration(primaryCode, false);
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    super.onText(key, text);

    performKeySound(KeyCodes.QUICK_TEXT);
    performKeyVibration(KeyCodes.QUICK_TEXT, false);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mKeyPreviewSubject.onComplete();
    mKeyPreviewController.destroy();
    mAudioManager.unloadSoundEffects();
  }

  @Override
  public void onLongPressDone(@NonNull Keyboard.Key key) {
    performKeyVibration(key.getPrimaryCode(), true);
  }

  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    super.dump(fd, writer, args);

    writer.println("NSK press-effects:");
    writer.println("  useSystemHapticFeedback=" + mUseSystemHapticFeedback);
    writer.println("  systemWideHapticEnabled=" + mSystemWideHapticEnabled);
    writer.println("  systemKeyboardVibrationEnabled=" + mSystemKeyboardVibrationEnabled);
    writer.println("  vibratorError=" + mVibratorError);
    writer.println("  configuredKeyPressVibrationDuration=" + mConfiguredKeyPressVibrationDuration);
    writer.println(
        "  configuredLongPressVibrationDuration=" + mConfiguredLongPressVibrationDuration);
    writer.println(
        "  configuredSystemVibrationFallbackDuration="
            + mConfiguredSystemVibrationFallbackDuration);
    writer.println("  soundPowerSavingState=" + mLastSoundPowerSavingState);
    writer.println("  soundNightModeState=" + mLastSoundNightModeState);
    writer.println("  soundOnPref=" + mLastSoundOnPref);
    writer.println("  useCustomSoundVolumePref=" + mLastUseCustomSoundVolumePref);
    writer.println("  customSoundVolumeLevelPref=" + mLastCustomSoundVolumeLevelPref);
    writer.println("  vibrationPowerSavingState=" + mLastVibrationPowerSavingState);
    writer.println("  vibrationDurationPref=" + mLastVibrationDurationPref);
    writer.println("  longPressPowerSavingState=" + mLastLongPressPowerSavingState);
    writer.println("  vibrateOnLongPressPref=" + mLastVibrateOnLongPressPref);
    writer.println("  customSoundVolume=" + mCustomSoundVolume);
    writer.println("  systemVibrationFallbackDuration=" + mSystemVibrationFallbackDuration);
    writer.println(
        "  vibratorImpl=" + (mVibrator == null ? "null" : mVibrator.getClass().getName()));
    writer.println(
        "  vibratorServicePresent=" + (mVibrator != null && mVibrator.getVibrator() != null));
    writer.println("  vibratorHasVibrator=" + (mVibrator != null && mVibrator.hasVibrator()));
    writer.println("  soundStateError=" + mSoundStateError);
    writer.println("  keyPressVibrationStateError=" + mKeyPressVibrationStateError);
    writer.println("  longPressVibrationStateError=" + mLongPressVibrationStateError);
    writer.println("  systemVibrationStateError=" + mSystemVibrationStateError);
    writer.println("  systemVibrationFallbackStateError=" + mSystemVibrationFallbackStateError);
    writer.println("  lastVibrationPrimaryCode=" + mLastVibrationPrimaryCode);
    writer.println("  lastVibrationLongPress=" + mLastVibrationLongPress);
    writer.println("  lastVibrationAttemptUptimeMs=" + mLastVibrationAttemptUptimeMs);
    writer.println("  lastVibrationPath=" + mLastVibrationPath);
  }
}
