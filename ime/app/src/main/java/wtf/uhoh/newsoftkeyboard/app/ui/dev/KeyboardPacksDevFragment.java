package wtf.uhoh.newsoftkeyboard.app.ui.dev;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.f2prateek.rx.preferences2.Preference;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import net.evendanan.pixel.RxProgressDialog;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksDevPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksInstaller;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksRepository;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardRuntimeLoader;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardSpec;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public class KeyboardPacksDevFragment extends Fragment {
  private static final String TAG = "KeyboardPacksDev";

  @Nullable private KeyboardPacksRepository repository;
  @Nullable private KeyboardPacksInstaller installer;

  @NonNull private Disposable disposable = Disposables.empty();

  @Nullable private InstalledKeyboardPack pendingExportPack;

  private TextView installedPacksStatus;
  private TextView activePackKeyboardStatus;
  private TextView lastActionStatus;
  private DemoKeyboardView previewKeyboardView;

  private Preference<String> devOverridePref;
  private final PackKeyboardRuntimeLoader runtimeLoader = new PackKeyboardRuntimeLoader();

  private ActivityResultLauncher<String[]> importPackZipLauncher;

  private ActivityResultLauncher<String> exportPackZipLauncher;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    importPackZipLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onUserPickedPackZip);
    exportPackZipLauncher =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            this::onUserPickedExportPath);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.keyboard_packs_dev, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    repository = new KeyboardPacksRepository(requireContext());
    installer = new KeyboardPacksInstaller(requireContext());

    installedPacksStatus = view.findViewById(R.id.installed_packs_status);
    activePackKeyboardStatus = view.findViewById(R.id.active_pack_keyboard_status);
    lastActionStatus = view.findViewById(R.id.last_action_status);
    previewKeyboardView = view.findViewById(R.id.pack_keyboard_preview);

    devOverridePref =
        NskApplicationBase.prefs(requireContext())
            .getString(
                KeyboardPacksDevPrefs.DEV_PACK_KEYBOARD_OVERRIDE_PREF,
                R.string.settings_default_empty);

    Button importButton = view.findViewById(R.id.import_pack_zip_button);
    importButton.setOnClickListener(v -> onUserClickedImportPackZip());

    Button selectButton = view.findViewById(R.id.select_active_pack_keyboard_button);
    selectButton.setOnClickListener(v -> onUserClickedSelectActiveKeyboard());

    Button clearButton = view.findViewById(R.id.clear_active_pack_keyboard_button);
    clearButton.setOnClickListener(v -> onUserClickedClearOverride());

    Button exportButton = view.findViewById(R.id.export_active_pack_button);
    exportButton.setOnClickListener(v -> onUserClickedExportActivePack());
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, "Keyboard Packs (dev)");
    refreshUi();
  }

  @Override
  public void onStop() {
    super.onStop();
    disposable.dispose();
  }

  private void refreshUi() {
    final KeyboardPacksRepository repo = repository;
    if (repo == null) return;
    try {
      List<InstalledKeyboardPack> packs = repo.listInstalledPacks();
      installedPacksStatus.setText("Installed packs: " + packs.size());
    } catch (Exception e) {
      installedPacksStatus.setText("Installed packs: error (" + e.getMessage() + ")");
    }

    String overrideRaw = devOverridePref.get();
    PackKeyboardSpec spec = PackKeyboardSpec.parse(overrideRaw);
    if (spec == null) {
      activePackKeyboardStatus.setText("Active pack keyboard: (none)");
      previewKeyboardView.setVisibility(View.GONE);
      return;
    }

    try {
      InstalledKeyboardPack pack = repo.findInstalledPackById(spec.packId());
      if (pack == null) {
        activePackKeyboardStatus.setText(
            "Active pack keyboard: missing pack '" + spec.packId() + "'");
        previewKeyboardView.setVisibility(View.GONE);
        return;
      }

      String keyboardId = spec.keyboardId();
      PackEntry entry = selectEntry(pack, keyboardId);
      activePackKeyboardStatus.setText(
          "Active pack keyboard: " + pack.manifest().name() + " — " + entry.id());

      KeyboardDefinition keyboard =
          runtimeLoader.tryLoadKeyboardDefinition(
              requireContext(), pack, entry.id(), Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      if (keyboard != null) {
        keyboard.loadKeyboard(previewKeyboardView.getThemedKeyboardDimens());
        previewKeyboardView.setKeyboard(keyboard, null, null);
        previewKeyboardView.setVisibility(View.VISIBLE);
      } else {
        previewKeyboardView.setVisibility(View.GONE);
      }
    } catch (Exception e) {
      activePackKeyboardStatus.setText("Active pack keyboard: error (" + e.getMessage() + ")");
      previewKeyboardView.setVisibility(View.GONE);
    }
  }

  private void onUserClickedImportPackZip() {
    importPackZipLauncher.launch(
        new String[] {"application/zip", "application/octet-stream", "*/*"});
  }

  private void onUserPickedPackZip(@Nullable Uri uri) {
    if (uri == null) return;
    final KeyboardPacksInstaller packsInstaller = installer;
    if (packsInstaller == null) return;
    final Context appContext = requireContext().getApplicationContext();

    disposable.dispose();
    disposable =
        RxProgressDialog.create(this, requireActivity(), R.layout.progress_window)
            .subscribeOn(RxSchedulers.background())
            .map(
                fragment -> {
                  try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
                    if (in == null)
                      throw new IllegalStateException("Failed opening zip InputStream");
                    return packsInstaller.installPackZip(in);
                  }
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                pack -> {
                  String msg = "Imported pack: " + pack.manifest().name();
                  lastActionStatus.setText(msg);
                  Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show();
                  refreshUi();
                },
                e -> {
                  Logger.w(TAG, "Failed importing pack", e);
                  showErrorDialog("Import failed", e.getMessage());
                  lastActionStatus.setText("Import failed: " + e.getMessage());
                  refreshUi();
                });
  }

  private void onUserClickedSelectActiveKeyboard() {
    final KeyboardPacksRepository repo = repository;
    if (repo == null) return;
    try {
      List<InstalledKeyboardPack> packs = repo.listInstalledPacks();
      if (packs.isEmpty()) {
        Toast.makeText(requireContext(), "No packs installed.", Toast.LENGTH_LONG).show();
        return;
      }

      var options = new ArrayList<Option>();
      for (InstalledKeyboardPack pack : packs) {
        for (PackEntry entry : pack.manifest().keyboards()) {
          options.add(new Option(pack, entry));
        }
      }
      if (options.isEmpty()) {
        Toast.makeText(
                requireContext(), "No keyboards found in installed packs.", Toast.LENGTH_LONG)
            .show();
        return;
      }

      CharSequence[] labels = new CharSequence[options.size()];
      for (int i = 0; i < options.size(); i++) {
        Option option = options.get(i);
        labels[i] = option.pack.manifest().name() + " — " + option.entry.id();
      }

      new AlertDialog.Builder(requireContext())
          .setTitle("Select active pack keyboard")
          .setItems(
              labels,
              (dialog, which) -> {
                Option selected = options.get(which);
                String spec =
                    new PackKeyboardSpec(selected.pack.manifest().id(), selected.entry.id())
                        .serialize();
                devOverridePref.set(spec);
                lastActionStatus.setText("Set active pack keyboard to: " + labels[which]);
                refreshUi();
              })
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    } catch (Exception e) {
      showErrorDialog("Failed listing packs", e.getMessage());
    }
  }

  private void onUserClickedClearOverride() {
    devOverridePref.set("");
    lastActionStatus.setText("Cleared pack keyboard override.");
    refreshUi();
  }

  private void onUserClickedExportActivePack() {
    PackKeyboardSpec spec = PackKeyboardSpec.parse(devOverridePref.get());
    if (spec == null) {
      Toast.makeText(requireContext(), "No active pack keyboard set.", Toast.LENGTH_LONG).show();
      return;
    }

    try {
      final KeyboardPacksRepository repo = repository;
      if (repo == null) return;
      InstalledKeyboardPack pack = repo.findInstalledPackById(spec.packId());
      if (pack == null) {
        Toast.makeText(requireContext(), "Pack not found: " + spec.packId(), Toast.LENGTH_LONG)
            .show();
        return;
      }
      pendingExportPack = pack;
      exportPackZipLauncher.launch(spec.packId() + ".zip");
    } catch (Exception e) {
      showErrorDialog("Export failed", e.getMessage());
    }
  }

  private void onUserPickedExportPath(@Nullable Uri uri) {
    if (uri == null) return;
    final KeyboardPacksInstaller packsInstaller = installer;
    if (packsInstaller == null) return;
    InstalledKeyboardPack pack = pendingExportPack;
    pendingExportPack = null;
    if (pack == null) return;

    final Context appContext = requireContext().getApplicationContext();

    disposable.dispose();
    disposable =
        RxProgressDialog.create(this, requireActivity(), R.layout.progress_window)
            .subscribeOn(RxSchedulers.background())
            .map(
                fragment -> {
                  try (OutputStream out = appContext.getContentResolver().openOutputStream(uri)) {
                    if (out == null)
                      throw new IllegalStateException("Failed opening zip OutputStream");
                    packsInstaller.exportPackZip(pack, out);
                    return pack;
                  }
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                exported -> {
                  String msg = "Exported pack: " + exported.manifest().name();
                  lastActionStatus.setText(msg);
                  Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show();
                },
                e -> {
                  Logger.w(TAG, "Failed exporting pack", e);
                  showErrorDialog("Export failed", e.getMessage());
                  lastActionStatus.setText("Export failed: " + e.getMessage());
                });
  }

  @NonNull
  private static PackEntry selectEntry(
      @NonNull InstalledKeyboardPack pack, @Nullable String keyboardId) {
    List<PackEntry> keyboards = pack.manifest().keyboards();
    if (keyboards.isEmpty()) throw new IllegalArgumentException("Pack has no keyboards");
    if (keyboardId == null || keyboardId.trim().isEmpty()) return keyboards.get(0);
    for (PackEntry entry : keyboards) {
      if (keyboardId.equals(entry.id())) return entry;
    }
    return keyboards.get(0);
  }

  private void showErrorDialog(@NonNull String title, @Nullable String message) {
    new AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(message == null ? "(no details)" : message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private record Option(@NonNull InstalledKeyboardPack pack, @NonNull PackEntry entry) {}
}
