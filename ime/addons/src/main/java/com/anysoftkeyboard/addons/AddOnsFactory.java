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

import static java.util.Collections.unmodifiableList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import com.anysoftkeyboard.base.utils.Logger;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public abstract class AddOnsFactory<E extends AddOn> {
  @NonNull protected final Context mContext;
  protected final String mTag;
  protected final SharedPreferences mSharedPreferences;
  final ArrayList<E> mAddOns = new ArrayList<>();
  final HashMap<String, E> mAddOnsById = new HashMap<>();
  final String mDefaultAddOnId;

  /**
   * This is the interface name that a broadcast receiver implementing an external addon should say
   * that it supports -- that is, this is the action it uses for its intent filter.
   */
  private final String mReceiverInterface;

  /**
   * Name under which an external addon broadcast receiver component publishes information about
   * itself.
   */
  private final String mReceiverMetaData;

  final List<ReceiverSpec> mReceiverSpecs;

  private final boolean mReadExternalPacksToo;
  private final String mRootNodeTag;
  private final String mAddonNodeTag;
  @XmlRes private final int mBuildInAddOnsResId;
  private final boolean mDevAddOnsIncluded;
  private final AddOnsXmlParser<E> addOnsXmlParser;

  // NOTE: this should only be used when interacting with shared-prefs!
  private final String mPrefIdPrefix;

  public record ReceiverSpec(@NonNull String action, @NonNull String metaData) {}

  protected AddOnsFactory(
      @NonNull Context context,
      @NonNull SharedPreferences sharedPreferences,
      String tag,
      String receiverInterface,
      String receiverMetaData,
      String rootNodeTag,
      String addonNodeTag,
      @NonNull String prefIdPrefix,
      @XmlRes int buildInAddonResId,
      @StringRes int defaultAddOnStringId,
      boolean readExternalPacksToo,
      boolean isDebugBuild,
      ReceiverSpec... additionalReceiverSpecs) {
    mContext = context;
    mTag = tag;
    mReceiverInterface = receiverInterface;
    mReceiverMetaData = receiverMetaData;
    mReceiverSpecs = new ArrayList<>();
    ReceiverSpec primarySpec = new ReceiverSpec(receiverInterface, receiverMetaData);
    mReceiverSpecs.add(primarySpec);
    if (additionalReceiverSpecs != null) {
      for (ReceiverSpec spec : additionalReceiverSpecs) {
        if (spec != null && !mReceiverSpecs.contains(spec)) {
          mReceiverSpecs.add(spec);
        }
      }
    }
    mRootNodeTag = rootNodeTag;
    mAddonNodeTag = addonNodeTag;
    if (TextUtils.isEmpty(prefIdPrefix)) {
      throw new IllegalArgumentException("prefIdPrefix can not be empty!");
    }
    mPrefIdPrefix = prefIdPrefix;
    mBuildInAddOnsResId = buildInAddonResId;
    if (buildInAddonResId == AddOn.INVALID_RES_ID) {
      throw new IllegalArgumentException("A built-in addon list MUST be provided!");
    }
    mReadExternalPacksToo = readExternalPacksToo;
    mDevAddOnsIncluded = isDebugBuild;
    mDefaultAddOnId = defaultAddOnStringId == 0 ? null : context.getString(defaultAddOnStringId);
    mSharedPreferences = sharedPreferences;
    addOnsXmlParser =
        new AddOnsXmlParser<>(mContext, mTag, mRootNodeTag, mAddonNodeTag, mDevAddOnsIncluded);

    if (isDebugBuild && readExternalPacksToo) {
      for (ReceiverSpec spec : mReceiverSpecs) {
        Logger.d(
            mTag,
            "Will read external addons with ACTION '%s' and meta-data '%s'",
            spec.action(),
            spec.metaData());
      }
    }
  }

  @Nullable
  protected static CharSequence getTextFromResourceOrText(
      Context context, AttributeSet attrs, String attributeName) {
    final int stringResId =
        attrs.getAttributeResourceValue(null, attributeName, AddOn.INVALID_RES_ID);
    if (stringResId != AddOn.INVALID_RES_ID) {
      return context.getResources().getString(stringResId);
    } else {
      return attrs.getAttributeValue(null, attributeName);
    }
  }

  public static boolean onExternalPackChanged(Intent eventIntent, AddOnsFactory<?>... factories) {
    return AddOnsFactoryEvents.onExternalPackChanged(eventIntent, factories);
  }

  public static void onConfigurationChanged(
      @NonNull Configuration newConfig, AddOnsFactory<?>... factories) {
    AddOnsFactoryEvents.onConfigurationChanged(newConfig, factories);
  }

  public final List<E> getEnabledAddOns() {
    List<String> enabledIds = getEnabledIds();
    List<E> addOns = new ArrayList<>(enabledIds.size());
    for (String enabledId : enabledIds) {
      E addOn = getAddOnById(enabledId);
      if (addOn != null) addOns.add(addOn);
    }

    return Collections.unmodifiableList(addOns);
  }

  public boolean isAddOnEnabled(String addOnId) {
    return mSharedPreferences.getBoolean(mPrefIdPrefix + addOnId, isAddOnEnabledByDefault(addOnId));
  }

  final void setAddOnEnableValueInPrefs(
      SharedPreferences.Editor editor, String addOnId, boolean enabled) {
    editor.putBoolean(mPrefIdPrefix + addOnId, enabled);
  }

  public abstract void setAddOnEnabled(String addOnId, boolean enabled);

  protected boolean isAddOnEnabledByDefault(@NonNull String addOnId) {
    return false;
  }

  public final E getEnabledAddOn() {
    return getEnabledAddOns().get(0);
  }

  public final synchronized List<String> getEnabledIds() {
    ArrayList<String> enabledIds = new ArrayList<>();
    for (E addOn : getAllAddOns()) {
      final String addOnId = addOn.getId();
      if (isAddOnEnabled(addOnId)) enabledIds.add(addOnId);
    }

    // ensuring at least one add-on is there
    if (enabledIds.size() == 0 && !TextUtils.isEmpty(mDefaultAddOnId)) {
      enabledIds.add(mDefaultAddOnId);
    }

    return Collections.unmodifiableList(enabledIds);
  }

  @CallSuper
  protected synchronized void clearAddOnList() {
    mAddOns.clear();
    mAddOnsById.clear();
  }

  public synchronized E getAddOnById(String id) {
    if (mAddOnsById.size() == 0) {
      loadAddOns();
    }
    return mAddOnsById.get(id);
  }

  public final synchronized List<E> getAllAddOns() {
    Logger.d(mTag, "getAllAddOns has %d add on for %s", mAddOns.size(), getClass().getName());
    if (mAddOns.size() == 0) {
      loadAddOns();
    }
    Logger.d(
        mTag, "getAllAddOns will return %d add on for %s", mAddOns.size(), getClass().getName());
    return unmodifiableList(mAddOns);
  }

  /**
   * Returns a list of add-ons that support UI card functionality. An add-on supports UI cards if it
   * has the uiCard attribute set to true in its XML declaration.
   */
  public final synchronized List<E> getAddOnsWithUICard() {
    return AddOnsUiCardFilter.filterAddOnsWithUiCard(getAllAddOns());
  }

  @CallSuper
  protected void loadAddOns() {
    clearAddOnList();

    List<E> local = getAddOnsFromLocalResId(mBuildInAddOnsResId);
    for (E addon : local) {
      Logger.d(mTag, "Local add-on %s loaded", addon.getId());
    }
    if (local.isEmpty()) {
      throw new IllegalStateException("No built-in addons were found for " + getClass().getName());
    }
    mAddOns.addAll(local);

    List<E> external = getExternalAddOns();
    for (E addon : external) {
      Logger.d(mTag, "External add-on %s loaded", addon.getId());
    }
    // ensures there are no duplicates
    // also, allow overriding internal packs with externals with the same ID
    mAddOns.removeAll(external);
    mAddOns.addAll(external);
    Logger.d(mTag, "Have %d add on for %s", mAddOns.size(), getClass().getName());

    for (E addOn : mAddOns) {
      mAddOnsById.put(addOn.getId(), addOn);
    }
    // removing hidden addons from global list, so hidden addons exist only in the mapping
    for (E addOn : mAddOnsById.values()) {
      if (addOn instanceof AddOnImpl && ((AddOnImpl) addOn).isHiddenAddon()) {
        mAddOns.remove(addOn);
      }
    }

    // sorting the keyboards according to the requested
    // sort order (from minimum to maximum)
    Collections.sort(mAddOns, new AddOnsComparator(mContext.getPackageName()));
    Logger.d(mTag, "Have %d add on for %s (after sort)", mAddOns.size(), getClass().getName());
  }

  private List<E> getExternalAddOns() {
    return ExternalAddOnsScanner.getExternalAddOns(
        mContext, mReceiverSpecs, mReadExternalPacksToo, this::getAddOnsFromActivityInfo, mTag);
  }

  private List<E> getAddOnsFromLocalResId(int addOnsResId) {
    try (final XmlResourceParser xml = mContext.getResources().getXml(addOnsResId)) {
      return addOnsXmlParser.parseAddOnsFromXml(this, mContext, xml, true);
    }
  }

  private List<E> getAddOnsFromActivityInfo(
      Context packContext, ActivityInfo ai, String receiverMetaData) {
    try (final XmlResourceParser xml =
        ai.loadXmlMetaData(mContext.getPackageManager(), receiverMetaData)) {
      if (xml == null) {
        // issue 718: maybe a bad package?
        return Collections.emptyList();
      }
      return addOnsXmlParser.parseAddOnsFromXml(this, packContext, xml, false);
    }
  }

  protected abstract E createConcreteAddOn(
      Context askContext,
      Context context,
      int apiVersion,
      CharSequence prefId,
      CharSequence name,
      CharSequence description,
      boolean isHidden,
      int sortIndex,
      boolean hasUICard,
      AttributeSet attrs);

  private static final class AddOnsComparator implements Comparator<AddOn>, Serializable {
    static final long serialVersionUID = 1276823L;

    private final String mAskPackageName;

    private AddOnsComparator(String askPackageName) {
      mAskPackageName = askPackageName;
    }

    @Override
    public int compare(AddOn k1, AddOn k2) {
      String c1 = k1.getPackageName();
      String c2 = k2.getPackageName();

      if (c1.equals(c2)) {
        return k1.getSortIndex() - k2.getSortIndex();
      } else if (c1.equals(mAskPackageName)) // I want to make sure ASK packages are first
      {
        return -1;
      } else if (c2.equals(mAskPackageName)) {
        return 1;
      } else {
        return c1.compareToIgnoreCase(c2);
      }
    }
  }
}
