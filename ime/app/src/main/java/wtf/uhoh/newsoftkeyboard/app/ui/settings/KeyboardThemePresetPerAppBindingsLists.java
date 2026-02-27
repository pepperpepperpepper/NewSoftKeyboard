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

final class KeyboardThemePresetPerAppBindingsLists {

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
      return label + " " + packageName;
    }
  }

  static final class AppEntryAdapter extends ArrayAdapter<AppEntry> {

    private final int iconSizePx;
    private final int iconPaddingPx;

    AppEntryAdapter(@NonNull Context context, @NonNull List<AppEntry> entries) {
      super(context, android.R.layout.select_dialog_item, android.R.id.text1, entries);
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
  }

  static final class AppBindingEntry {
    @NonNull final String packageName;
    @NonNull final String label;
    @NonNull final String presetId;
    @NonNull final String summary;
    @Nullable final Drawable icon;

    AppBindingEntry(
        @NonNull String packageName,
        @NonNull String label,
        @NonNull String presetId,
        @NonNull String summary,
        @Nullable Drawable icon) {
      this.packageName = packageName;
      this.label = label;
      this.presetId = presetId;
      this.summary = summary;
      this.icon = icon;
    }

    @NonNull
    @Override
    public String toString() {
      return (label + " " + packageName + " " + summary).toLowerCase(Locale.US);
    }
  }

  static final class AppBindingEntryAdapter extends ArrayAdapter<AppBindingEntry> {

    private final List<AppBindingEntry> allEntries;
    private final int iconSizePx;
    private final int iconPaddingPx;

    AppBindingEntryAdapter(@NonNull Context context, @NonNull List<AppBindingEntry> entries) {
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
      final AppBindingEntry entry = getItem(position);
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

          final List<AppBindingEntry> filtered = new ArrayList<>();
          for (AppBindingEntry entry : allEntries) {
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
          final List<AppBindingEntry> entries = (List<AppBindingEntry>) results.values;
          if (entries != null) addAll(entries);
          notifyDataSetChanged();
        }
      };
    }
  }

  private static int dpToPx(@NonNull Context context, int dp) {
    final float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
  }
}
