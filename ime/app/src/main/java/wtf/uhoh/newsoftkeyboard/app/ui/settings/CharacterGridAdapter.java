/*
 * Copyright (C) 2026 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;

/** Renders a grid of glyphs for {@link CharacterLibrary} codepoints in the character picker. */
final class CharacterGridAdapter extends BaseAdapter {

  private final LayoutInflater inflater;
  private final List<Integer> codepoints = new ArrayList<>();

  CharacterGridAdapter(@NonNull Context context) {
    this.inflater = LayoutInflater.from(context);
  }

  void setCodepoints(@NonNull List<Integer> next) {
    codepoints.clear();
    codepoints.addAll(next);
    notifyDataSetChanged();
  }

  /** The codepoint at a grid position. */
  int codePointAt(int position) {
    return codepoints.get(position);
  }

  @Override
  public int getCount() {
    return codepoints.size();
  }

  @Override
  public Object getItem(int position) {
    return codepoints.get(position);
  }

  @Override
  public long getItemId(int position) {
    return codepoints.get(position);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final TextView cell =
        (TextView)
            (convertView != null
                ? convertView
                : inflater.inflate(R.layout.custom_keyboard_character_cell, parent, false));
    final int cp = codepoints.get(position);
    cell.setText(CharacterLibrary.glyph(cp));
    final String name = CharacterLibrary.nameOf(cp);
    cell.setContentDescription(name != null ? name : CharacterLibrary.glyph(cp));
    return cell;
  }
}
