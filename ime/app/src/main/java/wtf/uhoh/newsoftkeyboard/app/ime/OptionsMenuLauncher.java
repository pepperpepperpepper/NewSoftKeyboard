package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import android.content.DialogInterface;
import android.view.inputmethod.InputMethodManager;
import wtf.uhoh.newsoftkeyboard.R;

/** Builds and shows the options menu dialog (settings/override/IME picker/incognito toggle). */
public final class OptionsMenuLauncher {

  private OptionsMenuLauncher() {}

  public interface Host {
    void showOptionsDialogWithData(
        int titleResId,
        int iconResId,
        CharSequence[] items,
        DialogInterface.OnClickListener listener);

    InputMethodManager getInputMethodManager();

    boolean isIncognito();

    void setIncognito(boolean incognito, boolean notify);

    boolean isContextProfilesEnabled();

    boolean isContextProfilesGloballyEnabled();

    void setContextProfilesTemporarilyDisabled(boolean disabled);

    void launchSettings();

    void launchDictionaryOverriding();

    Context getContext();
  }

  public static void show(Host host) {
    final CharSequence[] items =
        new CharSequence[] {
          host.getContext().getText(R.string.ime_settings),
          host.getContext().getText(R.string.override_dictionary),
          host.getContext().getText(R.string.change_ime),
          host.getContext()
              .getString(
                  R.string.switch_incognito_template,
                  host.getContext().getText(R.string.switch_incognito)),
          host.getContext()
              .getString(
                  R.string.context_profiles_options_menu_template,
                  host.isContextProfilesGloballyEnabled()
                      ? (host.isContextProfilesEnabled()
                          ? host.getContext()
                              .getText(R.string.context_profiles_options_menu_state_on)
                          : host.getContext()
                              .getText(
                                  R.string.context_profiles_options_menu_state_temporarily_off))
                      : host.getContext()
                          .getText(
                              R.string.context_profiles_options_menu_state_disabled_in_settings))
        };

    host.showOptionsDialogWithData(
        R.string.ime_name,
        R.mipmap.ic_launcher,
        items,
        (di, position) -> {
          switch (position) {
            case 0:
              host.launchSettings();
              break;
            case 1:
              host.launchDictionaryOverriding();
              break;
            case 2:
              host.getInputMethodManager().showInputMethodPicker();
              break;
            case 3:
              host.setIncognito(!host.isIncognito(), true);
              break;
            case 4:
              if (!host.isContextProfilesGloballyEnabled()) {
                host.launchSettings();
              } else {
                host.setContextProfilesTemporarilyDisabled(host.isContextProfilesEnabled());
              }
              break;
            default:
              throw new IllegalArgumentException(
                  "Position " + position + " is not covered by the options dialog.");
          }
        });
  }
}
