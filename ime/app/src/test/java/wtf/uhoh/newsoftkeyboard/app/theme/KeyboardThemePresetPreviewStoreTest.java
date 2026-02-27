package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.test.core.app.ApplicationProvider;
import java.io.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardThemePresetPreviewStoreTest {

  @Test
  public void testSaveReadDelete() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final KeyboardThemePresetPreviewStore store = new KeyboardThemePresetPreviewStore(context);
    final String presetId = "user_preset::test-preview";

    store.deletePreviewBestEffort(presetId);
    Assert.assertFalse(store.hasPreview(presetId));

    final Bitmap bitmap = Bitmap.createBitmap(16, 8, Bitmap.Config.ARGB_8888);
    try {
      bitmap.eraseColor(Color.RED);

      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
      store.savePreviewPngBytes(presetId, out.toByteArray());
    } finally {
      bitmap.recycle();
    }

    Assert.assertTrue(store.hasPreview(presetId));

    final Bitmap loaded =
        store.readPreviewBitmap(presetId, /* maxWidthPx= */ 80, /* maxHeightPx= */ 48);
    Assert.assertNotNull(loaded);
    try {
      Assert.assertTrue(loaded.getWidth() > 0);
      Assert.assertTrue(loaded.getHeight() > 0);
    } finally {
      loaded.recycle();
    }

    store.deletePreviewBestEffort(presetId);
    Assert.assertFalse(store.hasPreview(presetId));
  }
}
