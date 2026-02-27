package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
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
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetAppBindingStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

final class KeyboardThemePresetPerAppBindingsDialogsUi {

  @NonNull private final KeyboardThemeCustomizationPresetsSection.Host host;
  @NonNull private final KeyboardThemePresetStore presetStore;
  @NonNull private final KeyboardThemePresetAppBindingStore store;

  KeyboardThemePresetPerAppBindingsDialogsUi(
      @NonNull KeyboardThemeCustomizationPresetsSection.Host host,
      @NonNull KeyboardThemePresetStore presetStore,
      @NonNull KeyboardThemePresetAppBindingStore store) {
    this.host = host;
    this.presetStore = presetStore;
    this.store = store;
  }

  void showManagePerAppBindingsDialog(
      @NonNull Context context, @NonNull String baseThemeId, @NonNull String presetId) {
    final AlertDialog loading =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.keyboard_theme_presets_per_app_bindings_title)
            .setMessage(R.string.keyboard_theme_presets_per_app_bindings_loading)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .show();

    final Disposable disposable =
        Single.fromCallable(() -> loadAppBindingEntries(context, baseThemeId, presetId))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                entries -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  showPerAppBindingsManagerDialog(context, baseThemeId, presetId, entries);
                },
                ignored -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_presets_per_app_bindings_loading_failed_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                });
    loading.setOnDismissListener(ignored -> disposable.dispose());
  }

  void showBindAnyAppDialog(
      @NonNull Context context, @NonNull String baseThemeId, @NonNull String presetId) {
    final AlertDialog loading =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.keyboard_theme_presets_bind_any_app_title)
            .setMessage(R.string.keyboard_theme_presets_bind_any_app_loading)
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
                  showBindAnyAppPickerDialog(context, baseThemeId, presetId, apps);
                },
                ignored -> {
                  if (!host.isAdded()) return;
                  if (!loading.isShowing()) return;
                  loading.dismiss();
                  Toast.makeText(
                          context,
                          R.string.keyboard_theme_presets_bind_any_app_failed_toast,
                          Toast.LENGTH_SHORT)
                      .show();
                });
    loading.setOnDismissListener(ignored -> disposable.dispose());
  }

  @NonNull
  private List<KeyboardThemePresetPerAppBindingsLists.AppBindingEntry> loadAppBindingEntries(
      @NonNull Context context, @NonNull String baseThemeId, @NonNull String currentPresetId) {
    final List<KeyboardThemePresetAppBindingStore.AppBinding> bindings =
        store.listBindings(baseThemeId);
    if (bindings.isEmpty()) return Collections.emptyList();

    final PackageManager pm = context.getPackageManager();
    final List<KeyboardThemePresetPerAppBindingsLists.AppBindingEntry> out =
        new ArrayList<>(bindings.size());
    for (KeyboardThemePresetAppBindingStore.AppBinding binding : bindings) {
      final String packageName = binding.packageName();
      final String boundPresetId = binding.presetId();
      final String label = resolveAppLabelOrPackage(pm, packageName);
      @Nullable Drawable icon = null;
      try {
        icon = pm.getApplicationIcon(packageName);
      } catch (Exception ignored) {
        // keep null icon
      }
      final String summary =
          buildAppBindingRowSummary(context, baseThemeId, currentPresetId, boundPresetId);
      out.add(
          new KeyboardThemePresetPerAppBindingsLists.AppBindingEntry(
              packageName, label, boundPresetId, summary, icon));
    }
    out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));
    return out;
  }

  @NonNull
  private String buildAppBindingRowSummary(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String currentPresetId,
      @NonNull String boundPresetId) {
    final String boundPresetName = resolvePresetNameOrId(context, baseThemeId, boundPresetId);
    return boundPresetId.equals(currentPresetId)
        ? context.getString(
            R.string.keyboard_theme_presets_app_binding_row_summary_this_preset, boundPresetName)
        : context.getString(
            R.string.keyboard_theme_presets_app_binding_row_summary_other_preset, boundPresetName);
  }

  private void showPerAppBindingsManagerDialog(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String currentPresetId,
      @NonNull List<KeyboardThemePresetPerAppBindingsLists.AppBindingEntry> entries) {
    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final TextView help = new TextView(context);
    help.setText(R.string.keyboard_theme_presets_per_app_bindings_help);
    root.addView(
        help,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    if (entries.isEmpty()) {
      final TextView empty = new TextView(context);
      empty.setText(R.string.keyboard_theme_presets_per_app_bindings_summary_none);
      final LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = dpToPx(context, 12);
      root.addView(empty, params);

      new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
          .setTitle(R.string.keyboard_theme_presets_per_app_bindings_title)
          .setView(root)
          .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
          .show();
      return;
    }

    final EditText search = new EditText(context);
    search.setSingleLine(true);
    search.setHint(R.string.keyboard_theme_presets_per_app_bindings_search_hint);
    final LinearLayout.LayoutParams searchParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    searchParams.topMargin = dpToPx(context, 12);
    root.addView(search, searchParams);

    final ListView list = new ListView(context);
    final KeyboardThemePresetPerAppBindingsLists.AppBindingEntryAdapter adapter =
        new KeyboardThemePresetPerAppBindingsLists.AppBindingEntryAdapter(context, entries);
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

    final int boundToCurrentCount = countBoundToPreset(entries, currentPresetId);
    final AlertDialog.Builder builder =
        new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
            .setTitle(R.string.keyboard_theme_presets_per_app_bindings_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss());

    if (boundToCurrentCount > 0) {
      builder.setNeutralButton(
          R.string.keyboard_theme_presets_per_app_bindings_clear_current_button,
          (d, w) -> {
            d.dismiss();
            confirmAndClearBindingsForPreset(
                context, baseThemeId, currentPresetId, boundToCurrentCount);
          });
    }

    builder.setPositiveButton(
        R.string.keyboard_theme_presets_per_app_bindings_clear_all_button,
        (d, w) -> {
          d.dismiss();
          confirmAndClearAllBindings(context, baseThemeId, entries.size());
        });

    final AlertDialog dialog = builder.create();
    list.setOnItemClickListener(
        (parent, v, position, id) -> {
          dialog.dismiss();
          final KeyboardThemePresetPerAppBindingsLists.AppBindingEntry entry =
              adapter.getItem(position);
          if (entry != null) {
            showPerAppBindingActionsDialog(
                context, baseThemeId, entry.packageName, entry.presetId, currentPresetId);
          }
        });

    dialog.show();
  }

  private static int countBoundToPreset(
      @NonNull List<KeyboardThemePresetPerAppBindingsLists.AppBindingEntry> entries,
      @NonNull String presetId) {
    int count = 0;
    for (KeyboardThemePresetPerAppBindingsLists.AppBindingEntry entry : entries) {
      if (presetId.equals(entry.presetId)) count++;
    }
    return count;
  }

  private void confirmAndClearAllBindings(
      @NonNull Context context, @NonNull String baseThemeId, int totalCount) {
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.keyboard_theme_presets_per_app_bindings_clear_all_button)
        .setMessage(
            context.getString(
                R.string.keyboard_theme_presets_per_app_bindings_clear_all_message, totalCount))
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.keyboard_theme_presets_per_app_bindings_clear_all_action,
            (d, w) -> {
              d.dismiss();
              store.clearBindings(baseThemeId);
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_presets_per_app_bindings_clear_all_toast,
                      Toast.LENGTH_SHORT)
                  .show();
              host.refreshState();
            })
        .show();
  }

  private void confirmAndClearBindingsForPreset(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      int countBound) {
    final String presetName = resolvePresetNameOrId(context, baseThemeId, presetId);
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.keyboard_theme_presets_per_app_bindings_clear_current_button)
        .setMessage(
            context.getString(
                R.string.keyboard_theme_presets_per_app_bindings_clear_current_message,
                countBound,
                presetName))
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.keyboard_theme_presets_per_app_bindings_clear_current_action,
            (d, w) -> {
              d.dismiss();
              store.removePresetFromBindings(baseThemeId, presetId);
              Toast.makeText(
                      context,
                      R.string.keyboard_theme_presets_per_app_bindings_clear_current_toast,
                      Toast.LENGTH_SHORT)
                  .show();
              host.refreshState();
            })
        .show();
  }

  @NonNull
  private static List<KeyboardThemePresetPerAppBindingsLists.AppEntry> loadLaunchableApps(
      @NonNull Context context) {
    final PackageManager pm = context.getPackageManager();
    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);

    final List<ResolveInfo> infos;
    try {
      infos = pm.queryIntentActivities(intent, 0);
    } catch (Exception e) {
      return Collections.emptyList();
    }
    if (infos == null || infos.isEmpty()) return Collections.emptyList();

    final java.util.HashMap<String, KeyboardThemePresetPerAppBindingsLists.AppEntry> byPackage =
        new java.util.HashMap<>();
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
      byPackage.put(pkg, new KeyboardThemePresetPerAppBindingsLists.AppEntry(pkg, label, icon));
    }

    final List<KeyboardThemePresetPerAppBindingsLists.AppEntry> out =
        new ArrayList<>(byPackage.values());
    out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));
    return out;
  }

  private void showBindAnyAppPickerDialog(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      @NonNull List<KeyboardThemePresetPerAppBindingsLists.AppEntry> apps) {
    if (apps.isEmpty()) {
      Toast.makeText(
              context,
              R.string.keyboard_theme_presets_bind_any_app_failed_toast,
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    final int paddingPx = dpToPx(context, 16);
    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    final EditText search = new EditText(context);
    search.setSingleLine(true);
    search.setHint(R.string.keyboard_theme_presets_bind_any_app_search_hint);
    root.addView(
        search,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    final ListView list = new ListView(context);
    final KeyboardThemePresetPerAppBindingsLists.AppEntryAdapter adapter =
        new KeyboardThemePresetPerAppBindingsLists.AppEntryAdapter(context, apps);
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
            .setTitle(R.string.keyboard_theme_presets_bind_any_app_title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
            .create();

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          dialog.dismiss();
          final KeyboardThemePresetPerAppBindingsLists.AppEntry selected =
              adapter.getItem(position);
          if (selected != null) {
            bindSelectedAppToCurrentPreset(context, baseThemeId, presetId, selected);
          }
        });

    dialog.show();
  }

  private void bindSelectedAppToCurrentPreset(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String presetId,
      @NonNull KeyboardThemePresetPerAppBindingsLists.AppEntry app) {
    @Nullable final String existing = store.getBoundPresetId(baseThemeId, app.packageName);
    if (presetId.equals(existing)) {
      Toast.makeText(
              context,
              R.string.keyboard_theme_presets_bind_any_app_already_bound,
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    if (existing != null && !existing.isEmpty()) {
      final String existingName = resolvePresetNameOrId(context, baseThemeId, existing);
      final String currentName = resolvePresetNameOrId(context, baseThemeId, presetId);
      new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
          .setTitle(app.label)
          .setMessage(
              context.getString(
                  R.string.keyboard_theme_presets_bind_any_app_replace_message,
                  existingName,
                  currentName))
          .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
          .setPositiveButton(
              R.string.keyboard_theme_presets_bind_any_app_replace_action,
              (d, w) -> {
                d.dismiss();
                store.bindAppToPreset(baseThemeId, app.packageName, presetId);
                Toast.makeText(
                        context,
                        R.string.keyboard_theme_presets_bind_last_used_app_toast,
                        Toast.LENGTH_SHORT)
                    .show();
                host.refreshState();
              })
          .show();
      return;
    }

    store.bindAppToPreset(baseThemeId, app.packageName, presetId);
    Toast.makeText(
            context, R.string.keyboard_theme_presets_bind_last_used_app_toast, Toast.LENGTH_SHORT)
        .show();
    host.refreshState();
  }

  private void showPerAppBindingActionsDialog(
      @NonNull Context context,
      @NonNull String baseThemeId,
      @NonNull String packageName,
      @NonNull String currentlyBoundPresetId,
      @NonNull String currentPresetId) {
    final PackageManager pm = context.getPackageManager();
    final String appLabel = resolveAppLabelOrPackage(pm, packageName);

    final String boundName = resolvePresetNameOrId(context, baseThemeId, currentlyBoundPresetId);
    final String currentName = resolvePresetNameOrId(context, baseThemeId, currentPresetId);

    final List<ActionItem> actions = new ArrayList<>();
    if (!currentlyBoundPresetId.equals(currentPresetId)) {
      actions.add(
          new ActionItem(
              context.getString(
                  R.string.keyboard_theme_presets_app_binding_action_bind_to_current, currentName),
              () -> store.bindAppToPreset(baseThemeId, packageName, currentPresetId)));
    }
    actions.add(
        new ActionItem(
            context.getString(R.string.keyboard_theme_presets_app_binding_action_remove),
            () -> store.unbindApp(baseThemeId, packageName)));

    final CharSequence[] items = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) items[i] = actions.get(i).title;

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(appLabel)
        .setMessage(
            context.getString(
                R.string.keyboard_theme_presets_app_binding_actions_message, boundName))
        .setItems(
            items,
            (dialog, which) -> {
              dialog.dismiss();
              actions.get(which).run.run();
              host.refreshState();
            })
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
        .show();
  }

  private static final class ActionItem {
    @NonNull final String title;
    @NonNull final Runnable run;

    private ActionItem(@NonNull String title, @NonNull Runnable run) {
      this.title = title;
      this.run = run;
    }
  }

  @NonNull
  private static String resolveAppLabelOrPackage(
      @NonNull PackageManager pm, @NonNull String packageName) {
    try {
      final android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
      final CharSequence label = pm.getApplicationLabel(appInfo);
      if (!TextUtils.isEmpty(label)) return String.valueOf(label);
    } catch (Exception ignored) {
      // fall through
    }
    return packageName;
  }

  @NonNull
  private String resolvePresetNameOrId(
      @NonNull Context context, @NonNull String baseThemeId, @NonNull String presetId) {
    if (presetId.equals(baseThemeId)) {
      return context.getString(R.string.keyboard_theme_presets_default_entry);
    }
    final String name = presetStore.getPresetName(presetId);
    if (name != null && !name.trim().isEmpty()) return name.trim();
    return presetId;
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    final float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
  }
}
