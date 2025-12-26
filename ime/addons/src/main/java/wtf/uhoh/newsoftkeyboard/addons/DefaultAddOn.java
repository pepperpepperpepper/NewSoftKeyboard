package wtf.uhoh.newsoftkeyboard.addons;

import android.content.Context;

/** Empty add-on which is to be used to hold simple implementation for context mapping */
public class DefaultAddOn extends AddOnImpl {
  public DefaultAddOn(Context hostAppContext, Context packageContext) {
    this(
        hostAppContext,
        packageContext,
        hostAppContext
            .getResources()
            .getInteger(com.anysoftkeyboard.api.R.integer.anysoftkeyboard_api_version_code));
  }

  public DefaultAddOn(Context hostAppContext, Context packageContext, int apiVersion) {
    super(
        hostAppContext,
        packageContext,
        apiVersion,
        "DEFAULT_ADD_ON",
        "Local Default Add-On",
        "",
        false,
        0);
  }
}
