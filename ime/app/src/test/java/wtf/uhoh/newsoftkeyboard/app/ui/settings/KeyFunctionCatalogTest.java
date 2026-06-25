package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import com.anysoftkeyboard.api.KeyCodes;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class KeyFunctionCatalogTest {

  @Test
  public void categoriesAreNonEmpty() {
    Assert.assertFalse(KeyFunctionCatalog.categories().isEmpty());
    for (KeyFunctionCatalog.Category category : KeyFunctionCatalog.categories()) {
      Assert.assertFalse("category " + category.title + " empty", category.entries.isEmpty());
    }
  }

  @Test
  public void codesAreUniqueAcrossCatalog() {
    final Set<Integer> seen = new HashSet<>();
    for (KeyFunctionCatalog.Category category : KeyFunctionCatalog.categories()) {
      for (KeyFunctionCatalog.Entry entry : category.entries) {
        Assert.assertTrue("duplicate code " + entry.code, seen.add(entry.code));
      }
    }
  }

  @Test
  public void findByCodeResolvesKnownFunctions() {
    Assert.assertNotNull(KeyFunctionCatalog.findByCode(KeyCodes.DELETE));
    Assert.assertEquals("Backspace", KeyFunctionCatalog.findByCode(KeyCodes.DELETE).name);
    Assert.assertNotNull(KeyFunctionCatalog.findByCode(KeyCodes.ENTER));
    Assert.assertNotNull(KeyFunctionCatalog.findByCode(KeyCodes.SETTINGS));
  }

  @Test
  public void findByCodeReturnsNullForPrintableCharacter() {
    // 'd' is a character key, not a catalogued function.
    Assert.assertNull(KeyFunctionCatalog.findByCode((int) 'd'));
  }

  @Test
  public void layerSwitchWithTargetIsNotInGenericCatalog() {
    // CUSTOM_KEYBOARD_SWITCH needs a switch target; it is handled by the Layout-switch key type.
    Assert.assertNull(KeyFunctionCatalog.findByCode(KeyCodes.CUSTOM_KEYBOARD_SWITCH));
  }
}
