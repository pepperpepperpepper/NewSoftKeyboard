package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.keyboard.core.parser.AskXmlKeyboardParser;
import wtf.uhoh.newsoftkeyboard.keyboard.core.theme.ThemeXmlParser;

public final class KeyboardPackValidator {
  private KeyboardPackValidator() {}

  public static ValidationResult validate(KeyboardPack pack) {
    var errors = new ArrayList<String>();
    PackManifest manifest = pack.manifest();

    if (manifest.schemaVersion() != PackManifest.SUPPORTED_SCHEMA_VERSION) {
      errors.add(
          "Unsupported manifest schemaVersion "
              + manifest.schemaVersion()
              + " (supported: "
              + PackManifest.SUPPORTED_SCHEMA_VERSION
              + ")");
    }

    if (manifest.id().trim().isEmpty()) errors.add("manifest.id is empty");
    if (manifest.name().trim().isEmpty()) errors.add("manifest.name is empty");
    if (manifest.version() < 0) errors.add("manifest.version must be >= 0");

    validateUniqueEntryIds(errors, "keyboards", manifest.keyboards());
    validateUniqueEntryIds(errors, "themes", manifest.themes());

    for (PackEntry keyboardEntry : manifest.keyboards()) {
      try (InputStream keyboardXml = pack.source().open(keyboardEntry.path().value())) {
        AskXmlKeyboardParser.parse(keyboardXml);
      } catch (IOException e) {
        errors.add(
            "Failed reading/parsing keyboard '"
                + keyboardEntry.id()
                + "' at "
                + keyboardEntry.path()
                + ": "
                + e.getMessage());
      }
    }

    for (PackEntry themeEntry : manifest.themes()) {
      try (InputStream themeXml = pack.source().open(themeEntry.path().value())) {
        var theme = ThemeXmlParser.parse(themeXml);
        for (PackPath iconPath : theme.icons().values()) {
          try (InputStream iconStream = pack.source().open(iconPath.value())) {
            // no-op: existence check only
          } catch (IOException e) {
            errors.add(
                "Theme '"
                    + themeEntry.id()
                    + "' references missing icon "
                    + iconPath
                    + ": "
                    + e.getMessage());
          }
        }
      } catch (IOException e) {
        errors.add(
            "Failed reading/parsing theme '"
                + themeEntry.id()
                + "' at "
                + themeEntry.path()
                + ": "
                + e.getMessage());
      }
    }

    return new ValidationResult(errors);
  }

  private static void validateUniqueEntryIds(
      List<String> errors, String label, List<PackEntry> entries) {
    Set<String> ids = new HashSet<>();
    for (PackEntry entry : entries) {
      if (!ids.add(entry.id())) {
        errors.add("manifest." + label + " contains duplicate id '" + entry.id() + "'");
      }
    }
  }

  public record ValidationResult(List<String> errors) {
    public ValidationResult {
      errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public boolean isValid() {
      return errors.isEmpty();
    }
  }
}
