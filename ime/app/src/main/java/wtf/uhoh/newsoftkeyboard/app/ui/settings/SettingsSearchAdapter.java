package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.SettingsSearchIndex.SearchItem;

final class SettingsSearchAdapter extends RecyclerView.Adapter<SettingsSearchAdapter.ViewHolder> {

  private final List<SearchItem> mItems;
  private final ResultClickListener mClickListener;

  SettingsSearchAdapter(@NonNull List<SearchItem> items, @NonNull ResultClickListener listener) {
    mItems = items;
    mClickListener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(android.R.layout.simple_list_item_2, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    final SearchItem item = mItems.get(position);
    holder.bind(item, mClickListener);
  }

  @Override
  public int getItemCount() {
    return mItems.size();
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {
    private final android.widget.TextView mTitle;
    private final android.widget.TextView mSubtitle;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      mTitle = itemView.findViewById(android.R.id.text1);
      mSubtitle = itemView.findViewById(android.R.id.text2);
    }

    void bind(@NonNull SearchItem item, @NonNull ResultClickListener clickListener) {
      mTitle.setText(item.title);
      final String badgeText =
          item.beta ? item.typeBadge.label + " · [BETA]" : item.typeBadge.label;
      final String disabledSuffix = item.enabled ? "" : " · Requires a physical keyboard";
      mSubtitle.setText(item.path + " · " + badgeText + disabledSuffix);
      itemView.setEnabled(item.enabled);
      itemView.setAlpha(item.enabled ? 1f : 0.5f);
      itemView.setOnClickListener(v -> clickListener.onClick(item));
    }
  }

  interface ResultClickListener {
    void onClick(@NonNull SearchItem item);
  }
}
