/*
 * Copyright (c) 2013 Menny Even-Danan
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

package com.anysoftkeyboard.addons;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A factory where multiple add-ons may be enabled, and the enabled ordering is user-controlled.
 *
 * <p>The enabled-set and order are persisted in SharedPreferences using the parent factory's prefix
 * rules.
 */
public abstract class MultipleAddOnsFactory<E extends AddOn> extends AddOnsFactory<E> {
  private final String mSortedIdsPrefId;

  protected MultipleAddOnsFactory(
      @NonNull Context context,
      @NonNull SharedPreferences sharedPreferences,
      String tag,
      String receiverInterface,
      String receiverMetaData,
      String rootNodeTag,
      String addonNodeTag,
      String prefIdPrefix,
      @XmlRes int buildInAddonResId,
      @StringRes int defaultAddOnStringId,
      boolean readExternalPacksToo,
      boolean isTestingBuild,
      ReceiverSpec... additionalReceiverSpecs) {
    super(
        context,
        sharedPreferences,
        tag,
        receiverInterface,
        receiverMetaData,
        rootNodeTag,
        addonNodeTag,
        prefIdPrefix,
        buildInAddonResId,
        defaultAddOnStringId,
        readExternalPacksToo,
        isTestingBuild,
        additionalReceiverSpecs);

    mSortedIdsPrefId = prefIdPrefix + "AddOnsFactory_order_key";
  }

  public final void setAddOnsOrder(Collection<E> addOnsOr) {
    List<String> ids = new ArrayList<>(addOnsOr.size());
    for (E addOn : addOnsOr) {
      ids.add(addOn.getId());
    }

    setAddOnIdsOrder(ids);
  }

  public final void setAddOnIdsOrder(Collection<String> enabledAddOnIds) {
    Set<String> storedKeys = new HashSet<>();
    StringBuilder orderValue = new StringBuilder();
    int currentOrderIndex = 0;
    for (String id : enabledAddOnIds) {
      // adding each once.
      if (!storedKeys.contains(id)) {
        storedKeys.add(id);
        if (mAddOnsById.containsKey(id)) {
          final E addOnToReorder = mAddOnsById.get(id);
          mAddOns.remove(addOnToReorder);
          mAddOns.add(currentOrderIndex, addOnToReorder);
          if (currentOrderIndex > 0) {
            orderValue.append(",");
          }
          orderValue.append(id);
          currentOrderIndex++;
        }
      }
    }

    SharedPreferences.Editor editor = mSharedPreferences.edit();
    editor.putString(mSortedIdsPrefId, orderValue.toString());
    editor.apply();
  }

  @Override
  protected void loadAddOns() {
    super.loadAddOns();

    // now forcing order
    String[] order = mSharedPreferences.getString(mSortedIdsPrefId, "").split(",", -1);
    int currentOrderIndex = 0;
    Set<String> seenIds = new HashSet<>();
    for (String id : order) {
      if (mAddOnsById.containsKey(id) && !seenIds.contains(id)) {
        seenIds.add(id);
        E addOnToReorder = mAddOnsById.get(id);
        mAddOns.remove(addOnToReorder);
        mAddOns.add(currentOrderIndex, addOnToReorder);
        currentOrderIndex++;
      }
    }
  }

  @Override
  public void setAddOnEnabled(String addOnId, boolean enabled) {
    SharedPreferences.Editor editor = mSharedPreferences.edit();
    setAddOnEnableValueInPrefs(editor, addOnId, enabled);
    editor.apply();
  }

  @Override
  protected boolean isAddOnEnabledByDefault(@NonNull String addOnId) {
    return super.isAddOnEnabledByDefault(addOnId) || TextUtils.equals(mDefaultAddOnId, addOnId);
  }
}
