package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import org.junit.Test;

public class PackManifestJsonTest {
  @Test
  public void roundTripsManifest() throws Exception {
    PackEntry main = new PackEntry("main", PackPath.parse("keyboards/main.xml"));
    PackEntry theme = new PackEntry("default", PackPath.parse("themes/default.xml"));
    PackManifest manifest =
        new PackManifest(
            1,
            "pack_1",
            "My Pack",
            7,
            "1.0.0",
            Collections.singletonList(main),
            Collections.singletonList(theme));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PackManifestJson.write(manifest, out);

    PackManifest parsed = PackManifestJson.parse(new ByteArrayInputStream(out.toByteArray()));
    assertNotNull(parsed);
    assertEquals(manifest.schemaVersion(), parsed.schemaVersion());
    assertEquals(manifest.id(), parsed.id());
    assertEquals(manifest.name(), parsed.name());
    assertEquals(manifest.version(), parsed.version());
    assertEquals(manifest.minCoreVersion(), parsed.minCoreVersion());

    assertEquals(1, parsed.keyboards().size());
    assertEquals("main", parsed.keyboards().get(0).id());
    assertEquals("keyboards/main.xml", parsed.keyboards().get(0).path().value());

    assertEquals(1, parsed.themes().size());
    assertEquals("default", parsed.themes().get(0).id());
    assertEquals("themes/default.xml", parsed.themes().get(0).path().value());
  }
}
