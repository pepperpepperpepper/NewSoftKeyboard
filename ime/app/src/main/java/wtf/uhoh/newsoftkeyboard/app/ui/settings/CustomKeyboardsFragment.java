package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.evendanan.pixel.RxProgressDialog;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.AtomicPackFileWriter;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPackCreator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.CustomKeyboardPrefs;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.InstalledKeyboardPack;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksInstaller;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.KeyboardPacksRepository;
import wtf.uhoh.newsoftkeyboard.app.keyboards.packs.PackKeyboardAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackEntry;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifest;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.PackManifestJson;
import wtf.uhoh.newsoftkeyboard.rx.RxSchedulers;

public class CustomKeyboardsFragment extends Fragment {
  private static final String TAG = "CustomKeyboardsFragment";

  private RecyclerView recyclerView;
  private TextView statusView;
  private final CustomKeyboardsAdapter adapter = new CustomKeyboardsAdapter();

  @Nullable private List<CustomKeyboardItem> items = null;

  @NonNull private Disposable disposable = Disposables.empty();

  @Nullable private KeyboardPacksRepository repository;
  @Nullable private KeyboardPacksInstaller installer;

  @Nullable private InstalledKeyboardPack pendingExportPack;

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

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.custom_keyboards, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    repository = new KeyboardPacksRepository(requireContext());
    installer = new KeyboardPacksInstaller(requireContext());

    statusView = view.findViewById(R.id.custom_keyboards_status);
    recyclerView = view.findViewById(R.id.custom_keyboards_list);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    recyclerView.setAdapter(adapter);

    Button createButton = view.findViewById(R.id.create_custom_keyboard_button);
    createButton.setOnClickListener(v -> showCreateKeyboardDialog());

    Button importButton = view.findViewById(R.id.import_custom_keyboard_pack_button);
    importButton.setOnClickListener(v -> onUserClickedImportPackZip());
  }

  @Override
  public void onStart() {
    super.onStart();
    requireActivity().setTitle(R.string.custom_keyboards_title);
    refreshList();
  }

  @Override
  public void onStop() {
    disposable.dispose();
    super.onStop();
  }

  private void refreshList() {
    final List<CustomKeyboardItem> next = new ArrayList<>();
    try {
      final KeyboardPacksRepository repo = repository;
      if (repo == null) throw new IOException("Keyboard packs repository not available.");

      List<InstalledKeyboardPack> packs = repo.listInstalledPacks();
      for (InstalledKeyboardPack pack : packs) {
        next.add(CustomKeyboardItem.from(pack));
      }
    } catch (IOException e) {
      Logger.w(TAG, "Failed listing custom keyboards: %s", e.getMessage());
      statusView.setText(e.getMessage());
      items = Collections.emptyList();
      adapter.setItems(items);
      return;
    }

    items = Collections.unmodifiableList(next);
    adapter.setItems(items);
    statusView.setText(
        next.isEmpty() ? "" : getString(R.string.custom_keyboards_status_count, next.size()));
  }

  private void showCreateKeyboardDialog() {
    final View dialogView =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.custom_keyboard_create_dialog, null, false);
    final EditText input = dialogView.findViewById(R.id.custom_keyboard_create_name);
    final RadioGroup templateGroup =
        dialogView.findViewById(R.id.custom_keyboard_create_template_group);

    AlertDialog dialog =
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.custom_keyboards_create_dialog_title)
            .setView(dialogView)
            .setPositiveButton(
                R.string.custom_keyboards_create_dialog_create_button,
                (ignored, which) -> {
                  String name = input.getText() != null ? input.getText().toString() : "";
                  if (TextUtils.isEmpty(name.trim())) return;
                  String templateId =
                      templateGroup.getCheckedRadioButtonId()
                              == R.id.custom_keyboard_create_template_basic
                          ? CustomKeyboardPackCreator.TEMPLATE_BASIC_QWERTY
                          : CustomKeyboardPackCreator.TEMPLATE_FULL_QWERTY;
                  createKeyboardFromTemplate(name.trim(), templateId);
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    dialog.show();

    // A blank name silently dismissed the dialog doing nothing; disable Create instead.
    final Button createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    createButton.setEnabled(false);
    input.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            createButton.setEnabled(s != null && !TextUtils.isEmpty(s.toString().trim()));
          }
        });
  }

  private void createKeyboardFromTemplate(@NonNull String name, @NonNull String templateId) {
    try {
      InstalledKeyboardPack pack =
          CustomKeyboardPackCreator.createKeyboardPack(requireContext(), name, templateId);
      PackEntry primary = selectPrimaryEntry(pack.manifest());
      String addOnId = PackKeyboardAddOnAndBuilder.buildKeyboardId(pack.manifest(), primary);
      CustomKeyboardPrefs.setKeyboardEnabled(requireContext(), addOnId, true);
      refreshList();
      openEditor(pack.manifest().id(), primary.id());
    } catch (IOException e) {
      Logger.w(TAG, "Failed creating keyboard: %s", e.getMessage());
      statusView.setText(e.getMessage());
    }
  }

  private void openEditor(@NonNull String packId, @NonNull String keyboardEntryId) {
    NavController navController = Navigation.findNavController(requireView());
    Bundle args = new Bundle();
    args.putString(CustomKeyboardEditorFragment.ARG_PACK_ID, packId);
    args.putString(CustomKeyboardEditorFragment.ARG_KEYBOARD_ENTRY_ID, keyboardEntryId);
    navController.navigate(
        R.id.action_customKeyboardsFragment_to_customKeyboardEditorFragment, args);
  }

  private void showKeyboardActionsDialog(@NonNull CustomKeyboardItem item) {
    var actions = new ArrayList<ActionItem>();
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_action_export), () -> exportPack(item.pack)));
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_action_rename),
            () -> showRenamePackDialog(item.pack)));
    actions.add(
        new ActionItem(
            getString(R.string.custom_keyboards_action_delete),
            () -> confirmDeletePack(item.pack)));

    CharSequence[] titles = new CharSequence[actions.size()];
    for (int i = 0; i < actions.size(); i++) titles[i] = actions.get(i).title;

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_actions_dialog_title)
        .setItems(
            titles,
            (d, which) -> {
              ActionItem selected = actions.get(which);
              selected.action.run();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void exportPack(@NonNull InstalledKeyboardPack pack) {
    pendingExportPack = pack;
    exportPackZipLauncher.launch(pack.manifest().id() + ".zip");
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
                imported -> {
                  PackEntry primary = selectPrimaryEntry(imported.manifest());
                  String addOnId =
                      PackKeyboardAddOnAndBuilder.buildKeyboardId(imported.manifest(), primary);
                  CustomKeyboardPrefs.setKeyboardEnabled(appContext, addOnId, true);

                  Toast.makeText(
                          appContext,
                          "Imported custom keyboard: " + imported.manifest().name(),
                          Toast.LENGTH_LONG)
                      .show();
                  refreshList();
                },
                e -> {
                  Logger.w(TAG, "Failed importing pack", e);
                  showErrorDialog("Import failed", e.getMessage());
                  statusView.setText("Import failed: " + e.getMessage());
                  refreshList();
                });
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
                  Toast.makeText(
                          appContext,
                          "Exported custom keyboard: " + exported.manifest().name(),
                          Toast.LENGTH_LONG)
                      .show();
                },
                e -> {
                  Logger.w(TAG, "Failed exporting pack", e);
                  showErrorDialog("Export failed", e.getMessage());
                  statusView.setText("Export failed: " + e.getMessage());
                });
  }

  private void showRenamePackDialog(@NonNull InstalledKeyboardPack pack) {
    final EditText input = new EditText(requireContext());
    input.setHint(R.string.custom_keyboards_rename_dialog_name_hint);
    input.setText(pack.manifest().name());

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_rename_dialog_title)
        .setView(input)
        .setPositiveButton(
            R.string.custom_keyboards_rename_dialog_rename_button,
            (dialog, which) -> {
              String name = input.getText() != null ? input.getText().toString() : "";
              if (TextUtils.isEmpty(name.trim())) return;
              renamePack(pack, name.trim());
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void renamePack(@NonNull InstalledKeyboardPack pack, @NonNull String newName) {
    try {
      PackManifest manifest = pack.manifest();
      PackManifest updated =
          new PackManifest(
              manifest.schemaVersion(),
              manifest.id(),
              newName,
              manifest.version() + 1,
              manifest.minCoreVersion(),
              manifest.keyboards(),
              manifest.themes());
      writeManifest(pack.directory(), updated);
      CustomKeyboardPrefs.bumpGeneration(requireContext());
      refreshList();
    } catch (IOException e) {
      statusView.setText(e.getMessage());
    }
  }

  private void confirmDeletePack(@NonNull InstalledKeyboardPack pack) {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.custom_keyboards_confirm_delete_pack_title)
        .setMessage(R.string.custom_keyboards_confirm_delete_pack_message)
        .setPositiveButton(
            R.string.custom_keyboards_action_delete, (dialog, which) -> deletePack(pack))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void deletePack(@NonNull InstalledKeyboardPack pack) {
    final Context appContext = requireContext().getApplicationContext();
    final File packDir = pack.directory();

    disposable.dispose();
    disposable =
        RxProgressDialog.create(this, requireActivity(), R.layout.progress_window)
            .subscribeOn(RxSchedulers.background())
            .map(
                fragment -> {
                  deleteRecursively(packDir);
                  CustomKeyboardPrefs.removeAllDataForPack(appContext, pack.manifest().id());
                  return pack;
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                deleted -> {
                  Toast.makeText(
                          appContext,
                          "Deleted custom keyboard: " + deleted.manifest().name(),
                          Toast.LENGTH_LONG)
                      .show();
                  refreshList();
                },
                e -> {
                  Logger.w(TAG, "Failed deleting pack", e);
                  showErrorDialog("Delete failed", e.getMessage());
                  refreshList();
                });
  }

  private void showErrorDialog(@NonNull String title, @Nullable String message) {
    new AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(message == null ? "(no details)" : message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  @NonNull
  private static PackEntry selectPrimaryEntry(@NonNull PackManifest manifest) {
    if (manifest.keyboards().isEmpty()) {
      throw new IllegalArgumentException("Pack has no keyboards");
    }
    for (PackEntry entry : manifest.keyboards()) {
      if ("main".equals(entry.id())) return entry;
    }
    for (PackEntry entry : manifest.keyboards()) {
      String id = entry.id();
      if (manifest.keyboards().size() > 1 && (id.equals("symbols") || id.startsWith("symbols_"))) {
        continue;
      }
      return entry;
    }
    return manifest.keyboards().get(0);
  }

  private static void writeManifest(@NonNull File packDir, @NonNull PackManifest manifest)
      throws IOException {
    File manifestFile = new File(packDir, "manifest.json");
    AtomicPackFileWriter.write(manifestFile, out -> PackManifestJson.write(manifest, out));
  }

  private static void deleteRecursively(@NonNull File file) throws IOException {
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) {
        deleteRecursively(child);
      }
    }
    if (!file.delete()) {
      throw new IOException("Failed deleting " + file);
    }
  }

  private static final class CustomKeyboardItem {
    @NonNull final String packId;
    @NonNull final String primaryKeyboardEntryId;
    @NonNull final String primaryAddOnId;
    @NonNull final CharSequence displayName;
    @NonNull final CharSequence subtitle;
    @NonNull final InstalledKeyboardPack pack;

    private CustomKeyboardItem(
        @NonNull String packId,
        @NonNull String primaryKeyboardEntryId,
        @NonNull String primaryAddOnId,
        @NonNull CharSequence displayName,
        @NonNull CharSequence subtitle,
        @NonNull InstalledKeyboardPack pack) {
      this.packId = packId;
      this.primaryKeyboardEntryId = primaryKeyboardEntryId;
      this.primaryAddOnId = primaryAddOnId;
      this.displayName = displayName;
      this.subtitle = subtitle;
      this.pack = pack;
    }

    @NonNull
    static CustomKeyboardItem from(@NonNull InstalledKeyboardPack pack) {
      PackEntry primary = selectPrimaryEntry(pack.manifest());
      String addOnId = PackKeyboardAddOnAndBuilder.buildKeyboardId(pack.manifest(), primary);
      return new CustomKeyboardItem(
          pack.manifest().id(),
          primary.id(),
          addOnId,
          pack.manifest().name(),
          pack.manifest().id(),
          pack);
    }
  }

  private final class CustomKeyboardsAdapter
      extends RecyclerView.Adapter<CustomKeyboardViewHolder> {
    @NonNull private List<CustomKeyboardItem> items = Collections.emptyList();

    void setItems(@NonNull List<CustomKeyboardItem> next) {
      items = next;
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomKeyboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View itemView =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.custom_keyboard_list_item, parent, false);
      return new CustomKeyboardViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomKeyboardViewHolder holder, int position) {
      holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
      return items.size();
    }
  }

  private final class CustomKeyboardViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleView;
    private final TextView subtitleView;
    private final androidx.appcompat.widget.SwitchCompat enabledSwitch;
    private final Button editButton;
    private final View actionsButton;

    private CustomKeyboardItem bound;

    CustomKeyboardViewHolder(@NonNull View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.custom_keyboard_title);
      subtitleView = itemView.findViewById(R.id.custom_keyboard_subtitle);
      enabledSwitch = itemView.findViewById(R.id.custom_keyboard_enabled_switch);
      editButton = itemView.findViewById(R.id.custom_keyboard_edit_button);
      actionsButton = itemView.findViewById(R.id.custom_keyboard_actions_button);
    }

    void bind(@NonNull CustomKeyboardItem item) {
      bound = item;
      titleView.setText(item.displayName);
      subtitleView.setText(item.subtitle);

      enabledSwitch.setOnCheckedChangeListener(null);
      enabledSwitch.setChecked(
          CustomKeyboardPrefs.isKeyboardEnabled(requireContext(), item.primaryAddOnId));
      enabledSwitch.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            if (isChecked) {
              CustomKeyboardPrefs.setKeyboardEnabled(requireContext(), bound.primaryAddOnId, true);
            } else {
              CustomKeyboardPrefs.clearEnabledForPack(requireContext(), bound.packId);
            }
          });

      editButton.setOnClickListener(v -> openEditor(bound.packId, bound.primaryKeyboardEntryId));
      actionsButton.setOnClickListener(v -> showKeyboardActionsDialog(bound));
    }
  }

  private record ActionItem(@NonNull CharSequence title, @NonNull Runnable action) {}
}
