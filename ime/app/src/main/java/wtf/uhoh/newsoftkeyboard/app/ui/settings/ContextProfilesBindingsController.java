package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class ContextProfilesBindingsController {

  interface Host {
    @NonNull
    Context requireContext();

    boolean isAdded();

    @NonNull
    ContextProfilesStore store();

    void refreshSummaries();

    void showCreatePresetDialog();
  }

  @NonNull private final Host host;

  ContextProfilesBindingsController(@NonNull Host host) {
    this.host = host;
  }

  void showBindingsLoadingDialog() {
    final Context context = host.requireContext();
    final AlertDialog loading =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.context_profiles_manage_bindings_title)
            .setMessage(R.string.context_profiles_manage_bindings_loading_message)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .show();

    final Disposable disposable =
        Single.fromCallable(() -> loadBindingEntries(context))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                entries -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  showBindingsDialog(entries);
                },
                ignored -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  Toast.makeText(
                          context,
                          R.string.context_profiles_manage_bindings_loading_failed_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                });

    loading.setOnDismissListener(ignored -> disposable.dispose());
  }

  @NonNull
  private List<ContextProfilesBindingsLists.BindingEntry> loadBindingEntries(
      @NonNull Context context) {
    final ContextProfilesStore store = host.store();
    final List<ContextProfilesStore.AppBinding> bindings = store.listBindings();
    if (bindings.isEmpty()) return Collections.emptyList();

    final PackageManager pm = context.getPackageManager();
    final HashMap<String, ContextProfilesStore.Preset> presetCache = new HashMap<>();
    final List<ContextProfilesBindingsLists.BindingEntry> out = new ArrayList<>(bindings.size());
    for (ContextProfilesStore.AppBinding binding : bindings) {
      final String packageName = binding.packageName;
      final ContextFieldSelector selector = binding.selector;
      final String presetId = binding.presetId;

      final String appLabel = resolveAppLabelOrPackage(pm, packageName);
      @Nullable Drawable icon = null;
      try {
        icon = pm.getApplicationIcon(packageName);
      } catch (Exception ignored) {
        // keep null icon
      }

      final ContextProfilesStore.Preset preset =
          presetCache.computeIfAbsent(presetId, store::getPreset);
      final String presetName = preset != null ? preset.name : presetId;
      final String selectorLabel = selectorToLabel(context, selector);

      out.add(
          new ContextProfilesBindingsLists.BindingEntry(
              packageName,
              selector,
              presetId,
              appLabel + " \u2022 " + selectorLabel,
              context.getString(R.string.context_profiles_binding_summary_template, presetName),
              icon));
    }
    out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));
    return out;
  }

  private void showBindingsDialog(
      @NonNull List<ContextProfilesBindingsLists.BindingEntry> entries) {
    final Context context = host.requireContext();
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView help = new TextView(context);
    help.setText(
        context.getString(
            R.string.context_profiles_manage_bindings_help, ContextProfilesStore.MAX_APP_BINDINGS));
    root.addView(
        help,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    if (entries.isEmpty()) {
      new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
          .setTitle(R.string.context_profiles_manage_bindings_title)
          .setView(root)
          .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
          .setPositiveButton(
              R.string.context_profiles_binding_add_action,
              (d, w) -> {
                d.dismiss();
                showAddBindingLoadingDialog();
              })
          .show();
      return;
    }

    final EditText search = new EditText(context);
    search.setSingleLine(true);
    search.setHint(R.string.context_profiles_manage_bindings_search_hint);
    final LinearLayout.LayoutParams searchParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    searchParams.topMargin = dpToPx(context, 12);
    root.addView(search, searchParams);

    final ListView list = new ListView(context);
    final ContextProfilesBindingsLists.BindingEntryAdapter adapter =
        new ContextProfilesBindingsLists.BindingEntryAdapter(context, entries);
    list.setAdapter(adapter);
    final LinearLayout.LayoutParams listParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 360));
    listParams.topMargin = dpToPx(context, 8);
    root.addView(list, listParams);

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

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.context_profiles_manage_bindings_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .setNeutralButton(
                R.string.context_profiles_manage_bindings_clear_all_action,
                (d, w) -> {
                  d.dismiss();
                  confirmClearAllBindings(entries.size());
                })
            .setPositiveButton(
                R.string.context_profiles_binding_add_action,
                (d, w) -> {
                  d.dismiss();
                  showAddBindingLoadingDialog();
                })
            .create();

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          dialog.dismiss();
          final ContextProfilesBindingsLists.BindingEntry entry = adapter.getItem(position);
          if (entry != null) showBindingActionsDialog(entry);
        });

    dialog.show();
  }

  private void confirmClearAllBindings(int total) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_manage_bindings_clear_all_action)
        .setMessage(
            context.getString(R.string.context_profiles_manage_bindings_clear_all_message, total))
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.context_profiles_manage_bindings_clear_all_confirm,
            (d, w) -> {
              d.dismiss();
              store.clearAllBindings();
              host.refreshSummaries();
              Toast.makeText(
                      context,
                      R.string.context_profiles_manage_bindings_clear_all_done_toast,
                      Toast.LENGTH_SHORT)
                  .show();
            })
        .show();
  }

  private void showBindingActionsDialog(@NonNull ContextProfilesBindingsLists.BindingEntry entry) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    final CharSequence[] items =
        new CharSequence[] {
          context.getText(R.string.context_profiles_binding_action_change_preset),
          context.getText(R.string.context_profiles_binding_action_remove)
        };

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(entry.label)
        .setItems(
            items,
            (d, which) -> {
              d.dismiss();
              switch (which) {
                case 0:
                  showPresetPickerDialog(
                      presetId -> {
                        try {
                          store.bindAppToPreset(entry.packageName, entry.selector, presetId);
                        } catch (IllegalStateException e) {
                          Toast.makeText(
                                  context,
                                  context.getString(
                                      R.string.context_profiles_limit_too_many_bindings_toast,
                                      ContextProfilesStore.MAX_APP_BINDINGS),
                                  Toast.LENGTH_SHORT)
                              .show();
                          return;
                        }
                        host.refreshSummaries();
                      });
                  break;
                case 1:
                  store.unbindApp(entry.packageName, entry.selector);
                  host.refreshSummaries();
                  break;
                default:
                  break;
              }
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  private void showAddBindingLoadingDialog() {
    final Context context = host.requireContext();
    final AlertDialog loading =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.context_profiles_binding_add_action)
            .setMessage(R.string.context_profiles_binding_add_loading_message)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .show();

    final Disposable disposable =
        Single.fromCallable(() -> loadLaunchableApps(context))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                apps -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  showAppPickerDialog(apps);
                },
                ignored -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  Toast.makeText(
                          context,
                          R.string.context_profiles_binding_add_loading_failed_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                });

    loading.setOnDismissListener(ignored -> disposable.dispose());
  }

  @NonNull
  private static List<ContextProfilesBindingsLists.AppEntry> loadLaunchableApps(
      @NonNull Context context) {
    final PackageManager pm = context.getPackageManager();
    final Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    final List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

    final HashMap<String, ContextProfilesBindingsLists.AppEntry> byPackage = new HashMap<>();
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

      @Nullable Drawable icon = null;
      try {
        icon = info.loadIcon(pm);
      } catch (Exception ignored) {
        // keep null icon
      }
      byPackage.put(pkg, new ContextProfilesBindingsLists.AppEntry(pkg, label, icon));
    }

    final List<ContextProfilesBindingsLists.AppEntry> out = new ArrayList<>(byPackage.values());
    out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));
    return out;
  }

  private void showAppPickerDialog(@NonNull List<ContextProfilesBindingsLists.AppEntry> apps) {
    if (apps.isEmpty()) {
      Toast.makeText(
              host.requireContext(),
              R.string.context_profiles_binding_add_no_apps_toast,
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    final Context context = host.requireContext();
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final EditText search = new EditText(context);
    search.setSingleLine(true);
    search.setHint(R.string.context_profiles_binding_add_search_hint);
    root.addView(
        search,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final ListView list = new ListView(context);
    final ContextProfilesBindingsLists.AppEntryAdapter adapter =
        new ContextProfilesBindingsLists.AppEntryAdapter(context, apps);
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

    final AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.context_profiles_binding_add_pick_app_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .create();

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          dialog.dismiss();
          final ContextProfilesBindingsLists.AppEntry selected = adapter.getItem(position);
          if (selected == null) return;
          showFieldSelectorDialog(selected);
        });

    dialog.show();
  }

  private void showFieldSelectorDialog(@NonNull ContextProfilesBindingsLists.AppEntry app) {
    final Context context = host.requireContext();
    final ContextFieldSelector[] selectors = ContextFieldSelector.values();
    final CharSequence[] labels = new CharSequence[selectors.length];
    for (int i = 0; i < selectors.length; i++) {
      labels[i] = selectorToLabel(context, selectors[i]);
    }

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(app.label)
        .setItems(
            labels,
            (d, which) -> {
              d.dismiss();
              if (which < 0 || which >= selectors.length) return;
              showPresetPickerDialog(
                  presetId ->
                      bindAppToPresetWithConfirm(app.packageName, selectors[which], presetId));
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  private void showPresetPickerDialog(@NonNull PresetPicked callback) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    final List<ContextProfilesStore.Preset> presets = store.listPresets();
    if (presets.isEmpty()) {
      Toast.makeText(context, R.string.context_profiles_no_presets_toast, Toast.LENGTH_SHORT)
          .show();
      host.showCreatePresetDialog();
      return;
    }

    final CharSequence[] labels = new CharSequence[presets.size()];
    for (int i = 0; i < presets.size(); i++) {
      labels[i] = presets.get(i).name;
    }

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_binding_pick_preset_title)
        .setItems(
            labels,
            (d, which) -> {
              d.dismiss();
              if (which < 0 || which >= presets.size()) return;
              callback.onPresetPicked(presets.get(which).id);
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  private void bindAppToPresetWithConfirm(
      @NonNull String packageName,
      @NonNull ContextFieldSelector selector,
      @NonNull String presetId) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    @Nullable final String existing = store.getBoundPresetId(packageName, selector);
    if (presetId.equals(existing)) {
      Toast.makeText(
              context, R.string.context_profiles_binding_already_set_toast, Toast.LENGTH_SHORT)
          .show();
      return;
    }

    if (existing != null && !existing.isEmpty()) {
      final String existingName = resolvePresetNameOrId(existing);
      final String newName = resolvePresetNameOrId(presetId);
      new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
          .setTitle(R.string.context_profiles_binding_replace_title)
          .setMessage(
              context.getString(
                  R.string.context_profiles_binding_replace_message,
                  selectorToLabel(context, selector),
                  existingName,
                  newName))
          .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
          .setPositiveButton(
              R.string.context_profiles_binding_replace_action,
              (d, w) -> {
                d.dismiss();
                try {
                  store.bindAppToPreset(packageName, selector, presetId);
                } catch (IllegalStateException e) {
                  Toast.makeText(
                          context,
                          context.getString(
                              R.string.context_profiles_limit_too_many_bindings_toast,
                              ContextProfilesStore.MAX_APP_BINDINGS),
                          Toast.LENGTH_SHORT)
                      .show();
                  return;
                }
                host.refreshSummaries();
              })
          .show();
      return;
    }

    if (store.listBindings().size() >= ContextProfilesStore.MAX_APP_BINDINGS) {
      Toast.makeText(
              context,
              context.getString(
                  R.string.context_profiles_limit_too_many_bindings_toast,
                  ContextProfilesStore.MAX_APP_BINDINGS),
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    try {
      store.bindAppToPreset(packageName, selector, presetId);
    } catch (IllegalStateException e) {
      Toast.makeText(
              context,
              context.getString(
                  R.string.context_profiles_limit_too_many_bindings_toast,
                  ContextProfilesStore.MAX_APP_BINDINGS),
              Toast.LENGTH_SHORT)
          .show();
      return;
    }
    host.refreshSummaries();
  }

  @NonNull
  private String resolvePresetNameOrId(@NonNull String presetId) {
    final ContextProfilesStore store = host.store();
    final ContextProfilesStore.Preset preset = store.getPreset(presetId);
    return preset != null ? preset.name : presetId;
  }

  @NonNull
  private static String selectorToLabel(
      @NonNull Context context, @NonNull ContextFieldSelector selector) {
    switch (selector) {
      case ALL_FIELDS:
        return context.getString(R.string.context_profiles_selector_all_fields);
      case TEXT:
        return context.getString(R.string.context_profiles_selector_text);
      case EMAIL:
        return context.getString(R.string.context_profiles_selector_email);
      case URL:
        return context.getString(R.string.context_profiles_selector_url);
      case IM:
        return context.getString(R.string.context_profiles_selector_im);
      case SEARCH:
        return context.getString(R.string.context_profiles_selector_search);
      default:
        return selector.id();
    }
  }

  @NonNull
  private static String resolveAppLabelOrPackage(
      @NonNull PackageManager pm, @NonNull String packageName) {
    try {
      return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
    } catch (PackageManager.NameNotFoundException e) {
      return packageName;
    }
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }

  private interface PresetPicked {
    void onPresetPicked(@NonNull String presetId);
  }
}
