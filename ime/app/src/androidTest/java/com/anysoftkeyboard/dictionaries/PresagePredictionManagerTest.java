package com.anysoftkeyboard.dictionaries;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog.CatalogEntry;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDownloader;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PresagePredictionManagerTest {

  @Test
  public void testPredictNextWithInstalledModel() {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureModelInstalled(context);
    final PresagePredictionManager manager = new PresagePredictionManager(context);

    final boolean activated = manager.activate();
    final String error = manager.getLastActivationError();
    Logger.d(
        "PresagePredictionManagerTest",
        "Activation result=" + activated + ", error=" + (error == null ? "<none>" : error));
    Log.d(
        "PresagePredictionManagerTest",
        "Activation result=" + activated + ", error=" + (error == null ? "<none>" : error));

    assertTrue("Presage failed to activate: " + error, activated);

    final String[] predictions = manager.predictNext(new String[] {"hello", "how"}, 5);
    Logger.d(
        "PresagePredictionManagerTest",
        "Predictions for [hello, how]: " + Arrays.toString(predictions));
    Log.d(
        "PresagePredictionManagerTest",
        "Predictions for [hello, how]: " + Arrays.toString(predictions));
  }

  @Test
  public void testSuggestionsProviderAppendPresage() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureModelInstalled(context);
    final SuggestionsProvider provider = new SuggestionsProvider(context);

    // Enable prediction engine and next-word flow via reflection.
    enablePresageNextWord(provider);

    // Trigger Presage activation.
    final java.lang.reflect.Method activateMethod =
        SuggestionsProvider.class.getDeclaredMethod("activatePresageIfNeeded");
    activateMethod.setAccessible(true);
    activateMethod.invoke(provider);

    final java.lang.reflect.Field presageManagerField =
        SuggestionsProvider.class.getDeclaredField("mPresagePredictionManager");
    presageManagerField.setAccessible(true);
    final PresagePredictionManager internalManager =
        (PresagePredictionManager) presageManagerField.get(provider);

    int attempts = 0;
    while (!internalManager.isActive() && attempts < 300) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {
      }
      attempts++;
    }
    assertTrue("Presage engine did not activate in time", internalManager.isActive());

    final java.lang.reflect.Field maxSuggestionsField =
        SuggestionsProvider.class.getDeclaredField("mMaxNextWordSuggestionsCount");
    maxSuggestionsField.setAccessible(true);
    maxSuggestionsField.setInt(provider, 5);

    final java.lang.reflect.Field minUsageField =
        SuggestionsProvider.class.getDeclaredField("mMinWordUsage");
    minUsageField.setAccessible(true);
    minUsageField.setInt(provider, 1);

    // Record context words.
    final java.lang.reflect.Method recordMethod =
        SuggestionsProvider.class.getDeclaredMethod("recordPresageContext", String.class);
    recordMethod.setAccessible(true);
    recordMethod.invoke(provider, "hello");
    recordMethod.invoke(provider, "how");

    // Collect predictions via appendPresageSuggestions.
    final java.lang.reflect.Method appendMethod =
        SuggestionsProvider.class.getDeclaredMethod(
            "appendPresageSuggestions", java.util.Collection.class, int.class);
    appendMethod.setAccessible(true);

    final java.util.ArrayList<CharSequence> holder = new java.util.ArrayList<>();
    final Object added = appendMethod.invoke(provider, holder, 5);

    final int addedCount = (Integer) added;
    Logger.d(
        "PresagePredictionManagerTest",
        "appendPresageSuggestions added=" + addedCount + " values=" + holder);
    Log.d(
        "PresagePredictionManagerTest",
        "appendPresageSuggestions added=" + addedCount + " values=" + holder);

    assertTrue("Expected at least one Presage suggestion", addedCount > 0);
  }

  @Test
  public void testSuggestionsProviderGetNextWordsUsesPresage() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureModelInstalled(context);
    final SuggestionsProvider provider = new SuggestionsProvider(context);

    enablePresageNextWord(provider);

    // Trigger Presage activation and wait until it finishes.
    final java.lang.reflect.Method activateMethod =
        SuggestionsProvider.class.getDeclaredMethod("activatePresageIfNeeded");
    activateMethod.setAccessible(true);
    activateMethod.invoke(provider);

    final java.lang.reflect.Field presageManagerField =
        SuggestionsProvider.class.getDeclaredField("mPresagePredictionManager");
    presageManagerField.setAccessible(true);
    final PresagePredictionManager internalManager =
        (PresagePredictionManager) presageManagerField.get(provider);

    int attempts = 0;
    while (!internalManager.isActive() && attempts < 300) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {
      }
      attempts++;
    }
    assertTrue("Presage engine did not activate in time", internalManager.isActive());

    final java.util.ArrayList<CharSequence> holder = new java.util.ArrayList<>();
    provider.getNextWords("hello", holder, 5);
    provider.getNextWords("how", holder, 5);

    Logger.d(
        "PresagePredictionManagerTest",
        "getNextWords returned values=" + holder);
    Log.d(
        "PresagePredictionManagerTest",
        "getNextWords returned values=" + holder);

    assertTrue("Expected Presage-backed suggestions", !holder.isEmpty());
  }

  private void ensureModelInstalled(Context context) {
    final PresageModelStore store = new PresageModelStore(context);
    final PresageModelStore.ActiveModel activeModel = store.ensureActiveModel();
    if (activeModel != null) {
      return;
    }
    try {
      final PresageModelCatalog catalog = new PresageModelCatalog(context);
      final List<CatalogEntry> entries = catalog.fetchCatalog();
      assertFalse("No Presage models available in catalog", entries.isEmpty());
      final PresageModelDownloader downloader = new PresageModelDownloader(context, store);
      downloader.downloadAndInstall(entries.get(0));
    } catch (Exception exception) {
      throw new AssertionError("Failed downloading Presage model", exception);
    }
  }

  private static void enablePresageNextWord(SuggestionsProvider provider) throws Exception {
    final java.lang.reflect.Field engineField =
        SuggestionsProvider.class.getDeclaredField("mPredictionEngineMode");
    engineField.setAccessible(true);
    final Class<?> modeClass = engineField.getType();
    final Object ngramMode = Enum.valueOf((Class<Enum>) modeClass.asSubclass(Enum.class), "NGRAM");
    engineField.set(provider, ngramMode);

    final java.lang.reflect.Field nextWordField =
        SuggestionsProvider.class.getDeclaredField("mNextWordEnabled");
    nextWordField.setAccessible(true);
    nextWordField.setBoolean(provider, true);
  }
}
