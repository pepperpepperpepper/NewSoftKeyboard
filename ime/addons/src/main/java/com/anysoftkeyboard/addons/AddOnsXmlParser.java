package com.anysoftkeyboard.addons;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class AddOnsXmlParser<E extends AddOn> {

  private static final String XML_PREF_ID_ATTRIBUTE = "id";
  private static final String XML_NAME_RES_ID_ATTRIBUTE = "nameResId";
  private static final String XML_DESCRIPTION_ATTRIBUTE = "description";
  private static final String XML_SORT_INDEX_ATTRIBUTE = "index";
  private static final String XML_DEV_ADD_ON_ATTRIBUTE = "devOnly";
  private static final String XML_HIDDEN_ADD_ON_ATTRIBUTE = "hidden";
  private static final String XML_UI_CARD_ATTRIBUTE = "uiCard";
  private static final String XML_NAME_ATTRIBUTE = "name";

  @NonNull private final Context askContext;
  @NonNull private final String tag;
  @NonNull private final String rootNodeTag;
  @NonNull private final String addonNodeTag;
  private final boolean devAddOnsIncluded;

  AddOnsXmlParser(
      @NonNull Context askContext,
      @NonNull String tag,
      @NonNull String rootNodeTag,
      @NonNull String addonNodeTag,
      boolean devAddOnsIncluded) {
    this.askContext = askContext;
    this.tag = tag;
    this.rootNodeTag = rootNodeTag;
    this.addonNodeTag = addonNodeTag;
    this.devAddOnsIncluded = devAddOnsIncluded;
  }

  @NonNull
  ArrayList<E> parseAddOnsFromXml(
      @NonNull AddOnsFactory<E> factory,
      @NonNull Context packContext,
      @NonNull XmlPullParser xml,
      boolean isLocal) {
    final ArrayList<E> addOns = new ArrayList<>();
    try {
      int event;
      boolean inRoot = false;
      while ((event = xml.next()) != XmlPullParser.END_DOCUMENT) {
        final String tag = xml.getName();
        if (event == XmlPullParser.START_TAG) {
          if (rootNodeTag.equals(tag)) {
            inRoot = true;
          } else if (inRoot && addonNodeTag.equals(tag)) {
            final AttributeSet attrs = Xml.asAttributeSet(xml);
            E addOn = createAddOnFromXmlAttributes(factory, attrs, packContext);
            if (addOn != null) {
              addOns.add(addOn);
            }
          }
        } else if (event == XmlPullParser.END_TAG && rootNodeTag.equals(tag)) {
          inRoot = false;
          break;
        }
      }
    } catch (final IOException e) {
      Logger.e(this.tag, "IO error:" + e);
      if (isLocal) throw new RuntimeException(e);
      e.printStackTrace();
    } catch (final XmlPullParserException e) {
      Logger.e(this.tag, "Parse error:" + e);
      if (isLocal) throw new RuntimeException(e);
      e.printStackTrace();
    }

    return addOns;
  }

  @Nullable
  private E createAddOnFromXmlAttributes(
      @NonNull AddOnsFactory<E> factory,
      @NonNull AttributeSet attrs,
      @NonNull Context packContext) {
    final CharSequence prefId =
        AddOnsFactory.getTextFromResourceOrText(packContext, attrs, XML_PREF_ID_ATTRIBUTE);
    CharSequence name =
        AddOnsFactory.getTextFromResourceOrText(packContext, attrs, XML_NAME_RES_ID_ATTRIBUTE);
    if (TextUtils.isEmpty(name)) {
      name = AddOnsFactory.getTextFromResourceOrText(packContext, attrs, XML_NAME_ATTRIBUTE);
    }

    if (!devAddOnsIncluded
        && attrs.getAttributeBooleanValue(null, XML_DEV_ADD_ON_ATTRIBUTE, false)) {
      Logger.w(
          tag,
          "Discarding add-on %s (name %s) since it is marked as DEV addon, and we're not"
              + " a TESTING_BUILD build.",
          prefId,
          name);
      return null;
    }

    final int apiVersion = getApiVersion(packContext);
    final boolean isHidden =
        attrs.getAttributeBooleanValue(null, XML_HIDDEN_ADD_ON_ATTRIBUTE, false);
    final CharSequence description =
        AddOnsFactory.getTextFromResourceOrText(packContext, attrs, XML_DESCRIPTION_ATTRIBUTE);

    final int sortIndex = attrs.getAttributeUnsignedIntValue(null, XML_SORT_INDEX_ATTRIBUTE, 1);
    final boolean hasUiCard = attrs.getAttributeBooleanValue(null, XML_UI_CARD_ATTRIBUTE, false);

    if (TextUtils.isEmpty(prefId) || TextUtils.isEmpty(name)) {
      Logger.e(
          tag,
          "External add-on does not include all mandatory details! Will not create" + " add-on.");
      return null;
    }

    return factory.createConcreteAddOn(
        askContext,
        packContext,
        apiVersion,
        prefId,
        name,
        description,
        isHidden,
        sortIndex,
        hasUiCard,
        attrs);
  }

  private int getApiVersion(@NonNull Context packContext) {
    try {
      final Resources resources = packContext.getResources();
      final int identifier =
          resources.getIdentifier(
              "anysoftkeyboard_api_version_code", "integer", packContext.getPackageName());
      if (identifier == 0) return 0;

      return resources.getInteger(identifier);
    } catch (Exception e) {
      Logger.w(tag, "Failed to load api-version for package %s", packContext.getPackageName());
      return 0;
    }
  }
}
