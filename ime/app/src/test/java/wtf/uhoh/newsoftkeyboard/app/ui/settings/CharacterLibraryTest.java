package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class CharacterLibraryTest {

  @Test
  public void categoriesAreNonEmptyAndBrowseable() {
    Assert.assertFalse(CharacterLibrary.categories().isEmpty());
    for (CharacterLibrary.Category category : CharacterLibrary.categories()) {
      Assert.assertFalse(
          "category " + category.title + " has no pickable chars",
          CharacterLibrary.codepointsIn(category).isEmpty());
    }
  }

  @Test
  public void pickableExcludesControlAndCombining() {
    Assert.assertFalse(CharacterLibrary.isPickable(0x0009)); // tab (control)
    Assert.assertFalse(CharacterLibrary.isPickable(0x0301)); // combining acute accent
    Assert.assertTrue(CharacterLibrary.isPickable((int) 'A'));
    Assert.assertTrue(CharacterLibrary.isPickable(0x20AC)); // €
  }

  @Test
  public void searchFindsByName() {
    final List<Integer> euro = CharacterLibrary.search("euro", 50);
    Assert.assertTrue("expected € in results", euro.contains(0x20AC));

    final List<Integer> arrow = CharacterLibrary.search("rightwards arrow", 50);
    Assert.assertTrue("expected → in results", arrow.contains(0x2192));
  }

  @Test
  public void searchRespectsLimit() {
    final List<Integer> many = CharacterLibrary.search("latin", 10);
    Assert.assertTrue(many.size() <= 10);
  }

  @Test
  public void parseCodepointAcceptsManyForms() {
    Assert.assertEquals(Integer.valueOf(0x20AC), CharacterLibrary.parseCodepoint("U+20AC"));
    Assert.assertEquals(Integer.valueOf(0x20AC), CharacterLibrary.parseCodepoint("20ac"));
    Assert.assertEquals(Integer.valueOf(0x20AC), CharacterLibrary.parseCodepoint("0x20AC"));
    Assert.assertEquals(Integer.valueOf(0x20AC), CharacterLibrary.parseCodepoint("€"));
    Assert.assertEquals(Integer.valueOf(0x1F600), CharacterLibrary.parseCodepoint("U+1F600"));
    // A pasted emoji (supplementary plane) is a single codepoint.
    Assert.assertEquals(
        Integer.valueOf(0x1F600), CharacterLibrary.parseCodepoint(new String(Character.toChars(0x1F600))));
    Assert.assertNull(CharacterLibrary.parseCodepoint("not a codepoint"));
  }

  @Test
  public void glyphHandlesSupplementaryPlane() {
    Assert.assertEquals("😀", CharacterLibrary.glyph(0x1F600));
  }
}
