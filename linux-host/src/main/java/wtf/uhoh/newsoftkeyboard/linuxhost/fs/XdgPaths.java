package wtf.uhoh.newsoftkeyboard.linuxhost.fs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class XdgPaths {
  private XdgPaths() {}

  public static Path dataHome(Map<String, String> environment) {
    String xdgDataHome = environment.get("XDG_DATA_HOME");
    if (xdgDataHome != null && !xdgDataHome.isBlank()) {
      return Paths.get(xdgDataHome).toAbsolutePath().normalize();
    }
    return homeDir().resolve(".local/share").toAbsolutePath().normalize();
  }

  public static Path configHome(Map<String, String> environment) {
    String xdgConfigHome = environment.get("XDG_CONFIG_HOME");
    if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
      return Paths.get(xdgConfigHome).toAbsolutePath().normalize();
    }
    return homeDir().resolve(".config").toAbsolutePath().normalize();
  }

  private static Path homeDir() {
    String userHome = System.getProperty("user.home");
    if (userHome == null || userHome.isBlank()) {
      return Paths.get(".").toAbsolutePath().normalize();
    }
    return Paths.get(userHome).toAbsolutePath().normalize();
  }
}
