package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.SettingsSearchIndex.SearchItem;

public class SettingsSearchFragment extends Fragment {

  public static final String ARG_SCROLL_TO_PREFERENCE_KEY = "scroll_to_preference_key";

  private final List<SearchItem> mAllItems = new ArrayList<>();
  private final List<SearchItem> mVisibleItems = new ArrayList<>();

  @Nullable private SettingsSearchAdapter mAdapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.settings_search_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final RecyclerView recyclerView = view.findViewById(R.id.settings_search_results);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

    SettingsSearchIndex.buildIndex(requireContext(), mAllItems);
    mVisibleItems.clear();
    mVisibleItems.addAll(mAllItems);

    mAdapter = new SettingsSearchAdapter(mVisibleItems, this::onResultClicked);
    recyclerView.setAdapter(mAdapter);

    final SearchView searchView = view.findViewById(R.id.settings_search_view);
    searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            filter(query);
            return true;
          }

          @Override
          public boolean onQueryTextChange(String newText) {
            filter(newText);
            return true;
          }
        });
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_search_title);
  }

  private void filter(@Nullable String query) {
    final String needle = query == null ? "" : query.trim().toLowerCase(Locale.US);
    mVisibleItems.clear();
    if (needle.isEmpty()) {
      mVisibleItems.addAll(mAllItems);
    } else {
      for (SearchItem item : mAllItems) {
        if (item.searchableText.contains(needle)) {
          mVisibleItems.add(item);
        }
      }
    }
    final SettingsSearchAdapter adapter = mAdapter;
    if (adapter != null) adapter.notifyDataSetChanged();
  }

  private void onResultClicked(@NonNull SearchItem item) {
    if (!item.enabled) {
      return;
    }

    final Bundle args = new Bundle();
    if (!TextUtils.isEmpty(item.scrollToPreferenceKey)) {
      args.putString(ARG_SCROLL_TO_PREFERENCE_KEY, item.scrollToPreferenceKey);
    }
    Navigation.findNavController(requireView()).navigate(item.destinationId, args);
  }
}
