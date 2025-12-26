package com.menny.android.anysoftkeyboard;

/**
 * Legacy application class name preserved for backward compatibility.
 *
 * <p>NewSoftKeyboard no longer treats {@code com.menny.android.anysoftkeyboard.*} as the primary
 * internal namespace, but some entrypoints (notably the {@code askCompat} flavor manifest) keep
 * referencing this type by fully-qualified class name. Keep this wrapper so we can continue
 * migrating internals while retaining compatibility.
 */
public class NskApplicationBase extends wtf.uhoh.newsoftkeyboard.app.NskApplicationBase {}
