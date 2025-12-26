package wtf.uhoh.newsoftkeyboard.app.quicktextkeys;

import android.content.Context;
import android.content.res.Resources;
import java.util.Arrays;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;

public class HistoryQuickTextKey extends QuickTextKey {

  private final QuickKeyHistoryRecords mQuickKeyHistoryRecords;

  public HistoryQuickTextKey(Context appContext, QuickKeyHistoryRecords quickKeyHistoryRecords) {
    super(
        appContext,
        appContext,
        appContext.getResources().getInteger(R.integer.anysoftkeyboard_api_version_code),
        "b0316c86-ffa2-49e9-85f7-6cb6e63e18f9",
        appContext.getResources().getText(R.string.history_quick_text_key_name),
        AddOn.INVALID_RES_ID,
        AddOn.INVALID_RES_ID,
        AddOn.INVALID_RES_ID,
        AddOn.INVALID_RES_ID,
        R.drawable.ic_quick_text_dark_theme,
        "\uD83D\uDD50",
        "\uD83D\uDD50",
        AddOn.INVALID_RES_ID,
        false,
        appContext.getResources().getString(R.string.history_quick_text_key_name),
        0);
    mQuickKeyHistoryRecords = quickKeyHistoryRecords;
  }

  @Override
  public List<String> getPopupListNames() {
    final List<QuickKeyHistoryRecords.HistoryKey> currentHistory =
        mQuickKeyHistoryRecords.getCurrentHistory();
    String[] names = new String[currentHistory.size()];
    int index = names.length - 1;
    for (QuickKeyHistoryRecords.HistoryKey historyKey : currentHistory) {
      names[index] = historyKey.name;
      index--;
    }
    return Arrays.asList(names);
  }

  @Override
  protected String[] getStringArrayFromNamesResId(int popupListNamesResId, Resources resources) {
    return new String[0];
  }

  @Override
  public List<String> getPopupListValues() {
    final List<QuickKeyHistoryRecords.HistoryKey> currentHistory =
        mQuickKeyHistoryRecords.getCurrentHistory();
    String[] values = new String[currentHistory.size()];
    int index = values.length - 1;
    for (QuickKeyHistoryRecords.HistoryKey historyKey : currentHistory) {
      values[index] = historyKey.value;
      index--;
    }
    return Arrays.asList(values);
  }

  @Override
  protected String[] getStringArrayFromValuesResId(int popupListValuesResId, Resources resources) {
    return new String[0];
  }

  public void recordUsedKey(String name, String value) {
    mQuickKeyHistoryRecords.store(name, value);
  }
}
