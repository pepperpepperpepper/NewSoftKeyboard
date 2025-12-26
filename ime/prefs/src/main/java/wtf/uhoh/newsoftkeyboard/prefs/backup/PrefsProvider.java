package wtf.uhoh.newsoftkeyboard.prefs.backup;

public interface PrefsProvider {
  String providerId();

  PrefsRoot getPrefsRoot();

  void storePrefsRoot(PrefsRoot prefsRoot);
}
