package com.anysoftkeyboard;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.ime.CondenseModeManager;
import com.anysoftkeyboard.keyboards.CondenseType;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.Disposable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

final class AnySoftKeyboardPrefsBinder {

  private AnySoftKeyboardPrefsBinder() {}

  static void wire(
      @NonNull RxSharedPrefs prefs,
      @NonNull Consumer<Disposable> addDisposable,
      @NonNull Consumer<Boolean> autoCapConsumer,
      @NonNull CondenseModeManager condenseModeManager,
      @NonNull IntSupplier currentOrientationSupplier,
      @NonNull Function<String, CondenseType> parseCondenseType,
      @NonNull Consumer<Boolean> keyboardIconInStatusBarConsumer) {

    addDisposable.accept(
        prefs
            .getBoolean(
                R.string.settings_key_auto_capitalization,
                R.bool.settings_default_auto_capitalization)
            .asObservable()
            .subscribe(
                autoCapConsumer::accept,
                GenericOnError.onError("settings_key_auto_capitalization")));

    addDisposable.accept(
        prefs
            .getString(
                R.string.settings_key_default_split_state_portrait,
                R.string.settings_default_default_split_state)
            .asObservable()
            .map(parseCondenseType::apply)
            .subscribe(
                type -> {
                  condenseModeManager.setPortraitPref(type);
                  condenseModeManager.updateForOrientation(currentOrientationSupplier.getAsInt());
                },
                GenericOnError.onError("settings_key_default_split_state_portrait")));

    addDisposable.accept(
        prefs
            .getString(
                R.string.settings_key_default_split_state_landscape,
                R.string.settings_default_default_split_state)
            .asObservable()
            .map(parseCondenseType::apply)
            .subscribe(
                type -> {
                  condenseModeManager.setLandscapePref(type);
                  condenseModeManager.updateForOrientation(currentOrientationSupplier.getAsInt());
                },
                GenericOnError.onError("settings_key_default_split_state_landscape")));

    addDisposable.accept(
        prefs
            .getBoolean(
                R.string.settings_key_keyboard_icon_in_status_bar,
                R.bool.settings_default_keyboard_icon_in_status_bar)
            .asObservable()
            .subscribe(
                keyboardIconInStatusBarConsumer::accept,
                GenericOnError.onError("settings_key_keyboard_icon_in_status_bar")));
  }
}
