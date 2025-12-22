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

/**
 * A factory where exactly one add-on is enabled at any time.
 *
 * <p>The enabled-set is persisted in SharedPreferences using the parent factory's prefix rules.
 */
public abstract class SingleAddOnsFactory<E extends AddOn> extends AddOnsFactory<E> {

  protected SingleAddOnsFactory(
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
  }

  @Override
  public void setAddOnEnabled(String addOnId, boolean enabled) {
    SharedPreferences.Editor editor = mSharedPreferences.edit();
    if (enabled) {
      // ensuring addons are loaded.
      getAllAddOns();
      // disable any other addon
      for (String otherAddOnId : mAddOnsById.keySet()) {
        setAddOnEnableValueInPrefs(editor, otherAddOnId, TextUtils.equals(otherAddOnId, addOnId));
      }
    } else {
      // enabled the default, disable the requested
      // NOTE: can not directly disable a default addon!
      // you should enable something else, which will cause the current (default?)
      // add-on to be automatically disabled.
      setAddOnEnableValueInPrefs(editor, addOnId, false);
      setAddOnEnableValueInPrefs(editor, mDefaultAddOnId, true);
    }
    editor.apply();
  }
}
