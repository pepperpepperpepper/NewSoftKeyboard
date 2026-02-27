package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class ProgrammableApiSessionOverridesUi {

  @NonNull private final PreferenceFragmentCompat mFragment;
  @NonNull private final KeyboardApiPrefs mPrefs;

  ProgrammableApiSessionOverridesUi(
      @NonNull PreferenceFragmentCompat fragment, @NonNull KeyboardApiPrefs prefs) {
    mFragment = fragment;
    mPrefs = prefs;
  }

  void showSessionOverridesDialog(
      @NonNull Context context, @NonNull String packageName, @NonNull String controllerLabel) {
    final String[] items = {
      mFragment.getString(R.string.keyboard_api_session_overrides_allowed_presets_title),
      mFragment.getString(R.string.keyboard_api_session_overrides_allowed_theme_presets_title),
      mFragment.getString(R.string.keyboard_api_session_overrides_allowed_keyboards_title),
      mFragment.getString(R.string.keyboard_api_session_overrides_allowed_target_apps_title)
    };

    new AlertDialog.Builder(context)
        .setTitle(
            mFragment.getString(
                R.string.keyboard_api_session_overrides_dialog_title, controllerLabel))
        .setItems(
            items,
            (d, which) -> {
              if (which == 0) {
                showAllowedSessionPresetsDialog(context, packageName);
              } else if (which == 1) {
                showAllowedSessionThemePresetsDialog(context, packageName);
              } else if (which == 2) {
                showAllowedSessionKeyboardsDialog(context, packageName);
              } else if (which == 3) {
                showAllowedSessionTargetAppsDialog(context, packageName);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showAllowedSessionPresetsDialog(
      @NonNull Context context, @NonNull String packageName) {
    final ContextProfilesStore store = new ContextProfilesStore(context);
    final List<ContextProfilesStore.Preset> presets = store.listPresets();
    if (presets.isEmpty()) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_session_overrides_no_presets_title),
          mFragment.getString(R.string.keyboard_api_session_overrides_no_presets_message));
      return;
    }

    final String[] values = new String[presets.size()];
    final String[] labels = new String[presets.size()];
    for (int i = 0; i < presets.size(); i++) {
      final ContextProfilesStore.Preset preset = presets.get(i);
      values[i] = preset.id;
      labels[i] = preset.name;
    }

    final Set<String> allowed = mPrefs.getAllowedSessionPresetIds(packageName);
    final boolean[] checked = new boolean[values.length];
    for (int i = 0; i < values.length; i++) {
      checked[i] = allowed.contains(values[i]);
    }

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_api_session_overrides_allowed_presets_title)
        .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              final HashSet<String> selected = new HashSet<>();
              for (int i = 0; i < values.length; i++) {
                if (checked[i]) selected.add(values[i]);
              }
              mPrefs.setAllowedSessionPresetIds(packageName, selected);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showAllowedSessionThemePresetsDialog(
      @NonNull Context context, @NonNull String packageName) {
    final KeyboardTheme theme =
        NskApplicationBase.getKeyboardThemeFactory(context).getEnabledAddOn();
    if (theme == null) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_session_overrides_no_theme_title),
          mFragment.getString(R.string.keyboard_api_session_overrides_no_theme_message));
      return;
    }

    final String baseThemeId = theme.getId();
    final KeyboardThemePresetStore presetStore = new KeyboardThemePresetStore(context);
    final List<KeyboardThemePresetStore.Preset> presets = presetStore.listPresets(baseThemeId);
    if (presets.isEmpty()) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_session_overrides_no_theme_presets_title),
          mFragment.getString(R.string.keyboard_api_session_overrides_no_theme_presets_message));
      return;
    }

    final String[] values = new String[presets.size()];
    final String[] labels = new String[presets.size()];
    for (int i = 0; i < presets.size(); i++) {
      final KeyboardThemePresetStore.Preset preset = presets.get(i);
      values[i] = preset.id();
      labels[i] = preset.name();
    }

    final Set<String> allowed = mPrefs.getAllowedSessionThemePresetIds(packageName);
    final boolean[] checked = new boolean[values.length];
    for (int i = 0; i < values.length; i++) {
      checked[i] = allowed.contains(values[i]);
    }

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_api_session_overrides_allowed_theme_presets_title)
        .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              final HashSet<String> selected = new HashSet<>();
              for (int i = 0; i < values.length; i++) {
                if (checked[i]) selected.add(values[i]);
              }
              mPrefs.setAllowedSessionThemePresetIds(packageName, selected);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showAllowedSessionKeyboardsDialog(
      @NonNull Context context, @NonNull String packageName) {
    final List<KeyboardAddOnAndBuilder> enabledKeyboards =
        NskApplicationBase.getKeyboardFactory(context).getEnabledAddOns();
    if (enabledKeyboards.isEmpty()) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_session_overrides_no_keyboards_title),
          mFragment.getString(R.string.keyboard_api_session_overrides_no_keyboards_message));
      return;
    }

    final String[] values = new String[enabledKeyboards.size()];
    final String[] labels = new String[enabledKeyboards.size()];
    for (int i = 0; i < enabledKeyboards.size(); i++) {
      final KeyboardAddOnAndBuilder keyboard = enabledKeyboards.get(i);
      values[i] = keyboard.getId();
      labels[i] = String.valueOf(keyboard.getName());
    }

    final Set<String> allowed = mPrefs.getAllowedSessionKeyboardIds(packageName);
    final boolean[] checked = new boolean[values.length];
    for (int i = 0; i < values.length; i++) {
      checked[i] = allowed.contains(values[i]);
    }

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_api_session_overrides_allowed_keyboards_title)
        .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              final HashSet<String> selected = new HashSet<>();
              for (int i = 0; i < values.length; i++) {
                if (checked[i]) selected.add(values[i]);
              }
              mPrefs.setAllowedSessionKeyboardIds(packageName, selected);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showAllowedSessionTargetAppsDialog(
      @NonNull Context context, @NonNull String packageName) {
    final List<TargetAppEntry> apps = loadLaunchableApps(context);
    if (apps.isEmpty()) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_session_overrides_no_apps_title),
          mFragment.getString(R.string.keyboard_api_session_overrides_no_apps_message));
      return;
    }

    final HashSet<String> selected =
        new HashSet<>(mPrefs.getAllowedSessionTargetPackages(packageName));
    final HashSet<String> seen = new HashSet<>();
    for (TargetAppEntry app : apps) {
      seen.add(app.packageName);
    }
    for (String pkg : selected) {
      if (!seen.contains(pkg)) {
        apps.add(new TargetAppEntry(pkg, pkg));
      }
    }
    apps.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));

    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView message = new TextView(context);
    message.setText(R.string.keyboard_api_session_overrides_allowed_target_apps_message);
    root.addView(
        message,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final EditText search = new EditText(context);
    search.setSingleLine(true);
    search.setHint(R.string.keyboard_api_session_overrides_allowed_target_apps_search_hint);
    root.addView(
        search,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final ListView list = new ListView(context);
    final TargetAppEntryAdapter adapter = new TargetAppEntryAdapter(context, apps, selected);
    list.setAdapter(adapter);
    root.addView(
        list,
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 360)));

    search.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            adapter.getFilter().filter(s == null ? "" : s.toString());
          }
        });

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          final TargetAppEntry entry = adapter.getItem(position);
          if (entry == null) return;
          adapter.toggle(entry.packageName);
        });

    new AlertDialog.Builder(context)
        .setTitle(R.string.keyboard_api_session_overrides_allowed_target_apps_title)
        .setView(root)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> mPrefs.setAllowedSessionTargetPackages(packageName, selected))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @NonNull
  private static List<TargetAppEntry> loadLaunchableApps(@NonNull Context context) {
    final PackageManager pm = context.getPackageManager();
    final Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    final List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

    final HashMap<String, TargetAppEntry> byPackage = new HashMap<>();
    for (ResolveInfo info : infos) {
      if (info == null || info.activityInfo == null) continue;
      final String pkg = info.activityInfo.packageName;
      if (pkg == null || pkg.trim().isEmpty()) continue;
      if (byPackage.containsKey(pkg)) continue;

      final String label;
      try {
        final CharSequence loaded = info.loadLabel(pm);
        label = loaded == null || loaded.toString().trim().isEmpty() ? pkg : loaded.toString();
      } catch (Exception ignored) {
        continue;
      }
      byPackage.put(pkg, new TargetAppEntry(pkg, label));
    }

    final List<TargetAppEntry> out = new ArrayList<>(byPackage.values());
    out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));
    return out;
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    final float density = context.getResources().getDisplayMetrics().density;
    return Math.round(density * dp);
  }

  private static final class TargetAppEntry {
    @NonNull final String packageName;
    @NonNull final String label;

    TargetAppEntry(@NonNull String packageName, @NonNull String label) {
      this.packageName = packageName;
      this.label = label;
    }
  }

  private static final class TargetAppEntryAdapter extends ArrayAdapter<TargetAppEntry>
      implements Filterable {

    @NonNull private final List<TargetAppEntry> mAll;
    @NonNull private final List<TargetAppEntry> mFiltered;
    @NonNull private final Set<String> mSelectedPackages;
    @NonNull private final Filter mFilter;

    TargetAppEntryAdapter(
        @NonNull Context context,
        @NonNull List<TargetAppEntry> apps,
        @NonNull Set<String> selectedPackages) {
      super(
          context,
          android.R.layout.simple_list_item_multiple_choice,
          android.R.id.text1,
          new ArrayList<>(apps));
      mAll = new ArrayList<>(apps);
      mFiltered = new ArrayList<>(apps);
      mSelectedPackages = selectedPackages;
      mFilter =
          new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
              final String q = constraint == null ? "" : constraint.toString().trim().toLowerCase();
              final ArrayList<TargetAppEntry> matches = new ArrayList<>();
              if (q.isEmpty()) {
                matches.addAll(mAll);
              } else {
                for (TargetAppEntry app : mAll) {
                  if (app.label.toLowerCase().contains(q)
                      || app.packageName.toLowerCase().contains(q)) {
                    matches.add(app);
                  }
                }
              }

              final FilterResults results = new FilterResults();
              results.count = matches.size();
              results.values = matches;
              return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
              mFiltered.clear();
              final Object values = results == null ? null : results.values;
              if (values instanceof List<?>) {
                for (Object o : ((List<?>) values)) {
                  if (o instanceof TargetAppEntry) {
                    mFiltered.add((TargetAppEntry) o);
                  }
                }
              }
              notifyDataSetChanged();
            }
          };
    }

    void toggle(@NonNull String packageName) {
      if (mSelectedPackages.contains(packageName)) {
        mSelectedPackages.remove(packageName);
      } else {
        mSelectedPackages.add(packageName);
      }
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return mFiltered.size();
    }

    @Nullable
    @Override
    public TargetAppEntry getItem(int position) {
      if (position < 0 || position >= mFiltered.size()) return null;
      return mFiltered.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final View v =
          convertView != null
              ? convertView
              : LayoutInflater.from(getContext())
                  .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
      final CheckedTextView tv = v.findViewById(android.R.id.text1);

      final TargetAppEntry entry = getItem(position);
      if (entry == null) return v;
      tv.setSingleLine(false);
      tv.setText(entry.label + "\n" + entry.packageName);
      tv.setChecked(mSelectedPackages.contains(entry.packageName));
      return v;
    }

    @NonNull
    @Override
    public Filter getFilter() {
      return mFilter;
    }
  }

  private void showSimpleMessageDialog(@NonNull String title, @NonNull String message) {
    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
