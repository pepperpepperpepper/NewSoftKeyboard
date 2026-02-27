package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextFieldSelector;

final class ContextProfilesBindingsLists {

  static final class AppEntry {
    @NonNull final String packageName;
    @NonNull final String label;
    @Nullable final Drawable icon;

    AppEntry(@NonNull String packageName, @NonNull String label, @Nullable Drawable icon) {
      this.packageName = packageName;
      this.label = label;
      this.icon = icon;
    }

    @NonNull
    @Override
    public String toString() {
      return (label + " " + packageName).toLowerCase(Locale.US);
    }
  }

  static final class AppEntryAdapter extends ArrayAdapter<AppEntry> {

    private final List<AppEntry> allEntries;
    private final int iconSizePx;
    private final int iconPaddingPx;

    AppEntryAdapter(@NonNull Context context, @NonNull List<AppEntry> entries) {
      super(
          context,
          android.R.layout.select_dialog_item,
          android.R.id.text1,
          new ArrayList<>(entries));
      allEntries = new ArrayList<>(entries);
      iconSizePx = dpToPx(context, 24);
      iconPaddingPx = dpToPx(context, 12);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      final TextView view = (TextView) super.getView(position, convertView, parent);
      final AppEntry entry = getItem(position);
      if (entry == null) return view;

      view.setText(entry.label);

      final Drawable icon = entry.icon;
      if (icon == null) {
        view.setCompoundDrawables(null, null, null, null);
        return view;
      }

      try {
        icon.setBounds(0, 0, iconSizePx, iconSizePx);
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding(iconPaddingPx);
      } catch (Exception ignored) {
        view.setCompoundDrawables(null, null, null, null);
      }
      return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
      return new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          final FilterResults results = new FilterResults();
          final String needle =
              constraint == null ? "" : constraint.toString().trim().toLowerCase(Locale.US);
          if (needle.isEmpty()) {
            results.values = allEntries;
            results.count = allEntries.size();
            return results;
          }

          final List<AppEntry> filtered = new ArrayList<>();
          for (AppEntry entry : allEntries) {
            if (entry.toString().contains(needle)) filtered.add(entry);
          }
          results.values = filtered;
          results.count = filtered.size();
          return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          clear();
          @SuppressWarnings("unchecked")
          final List<AppEntry> entries = (List<AppEntry>) results.values;
          if (entries != null) addAll(entries);
          notifyDataSetChanged();
        }
      };
    }
  }

  static final class BindingEntry {
    @NonNull final String packageName;
    @NonNull final ContextFieldSelector selector;
    @NonNull final String presetId;
    @NonNull final String label;
    @NonNull final String summary;
    @Nullable final Drawable icon;

    BindingEntry(
        @NonNull String packageName,
        @NonNull ContextFieldSelector selector,
        @NonNull String presetId,
        @NonNull String label,
        @NonNull String summary,
        @Nullable Drawable icon) {
      this.packageName = packageName;
      this.selector = selector;
      this.presetId = presetId;
      this.label = label;
      this.summary = summary;
      this.icon = icon;
    }

    @NonNull
    @Override
    public String toString() {
      return (label + " " + packageName + " " + summary).toLowerCase(Locale.US);
    }
  }

  static final class BindingEntryAdapter extends ArrayAdapter<BindingEntry> {

    private final List<BindingEntry> allEntries;
    private final int iconSizePx;
    private final int iconPaddingPx;

    BindingEntryAdapter(@NonNull Context context, @NonNull List<BindingEntry> entries) {
      super(context, android.R.layout.simple_list_item_2, new ArrayList<>(entries));
      allEntries = new ArrayList<>(entries);
      iconSizePx = dpToPx(context, 24);
      iconPaddingPx = dpToPx(context, 12);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      final android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());
      final View view =
          convertView != null
              ? convertView
              : inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
      final BindingEntry entry = getItem(position);
      if (entry == null) return view;

      final TextView text1 = view.findViewById(android.R.id.text1);
      final TextView text2 = view.findViewById(android.R.id.text2);
      text1.setText(entry.label);
      text2.setText(entry.summary);

      final Drawable icon = entry.icon;
      if (icon == null) {
        text1.setCompoundDrawables(null, null, null, null);
        return view;
      }

      try {
        icon.setBounds(0, 0, iconSizePx, iconSizePx);
        text1.setCompoundDrawables(icon, null, null, null);
        text1.setCompoundDrawablePadding(iconPaddingPx);
      } catch (Exception ignored) {
        text1.setCompoundDrawables(null, null, null, null);
      }

      return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
      return new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          final FilterResults results = new FilterResults();
          final String needle =
              constraint == null ? "" : constraint.toString().trim().toLowerCase(Locale.US);
          if (needle.isEmpty()) {
            results.values = allEntries;
            results.count = allEntries.size();
            return results;
          }

          final List<BindingEntry> filtered = new ArrayList<>();
          for (BindingEntry entry : allEntries) {
            if (entry.toString().contains(needle)) filtered.add(entry);
          }
          results.values = filtered;
          results.count = filtered.size();
          return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          clear();
          @SuppressWarnings("unchecked")
          final List<BindingEntry> entries = (List<BindingEntry>) results.values;
          if (entries != null) addAll(entries);
          notifyDataSetChanged();
        }
      };
    }
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }
}
