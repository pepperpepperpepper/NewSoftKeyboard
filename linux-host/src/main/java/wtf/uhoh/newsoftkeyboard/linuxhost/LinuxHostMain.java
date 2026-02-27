package wtf.uhoh.newsoftkeyboard.linuxhost;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wtf.uhoh.newsoftkeyboard.linuxhost.fs.XdgPaths;
import wtf.uhoh.newsoftkeyboard.linuxhost.packs.LinuxPackRepository;
import wtf.uhoh.newsoftkeyboard.linuxhost.prefs.FilePrefsStore;

public final class LinuxHostMain {
  private static final String APP_ID = "newsoftkeyboard";

  private LinuxHostMain() {}

  public static void main(String[] args) {
    try {
      mainInternal(args);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static void mainInternal(String[] args) throws Exception {
    Map<String, String> env = System.getenv();
    Path packsRoot = defaultPacksRoot(env);
    Path prefsFile = defaultPrefsFile(env);
    var prefs = FilePrefsStore.load(prefsFile);
    var packRepository = new LinuxPackRepository(packsRoot);

    if (args.length == 0) {
      printUsage(packsRoot, prefsFile);
      System.exit(2);
      return;
    }

    // Backwards-compatible positional args: <pack-dir-or-id> [keyboard-id]
    if (!args[0].startsWith("--")) {
      var parsed = parseRunArgs(packRepository, prefs, args);
      LinuxHostRunner.runInteractive(parsed.packDir(), parsed.keyboardId(), parsed.outputConfig());
      return;
    }

    switch (args[0]) {
      case "--list-packs" -> listPacks(packRepository);
      case "--install-pack" -> {
        if (args.length < 2) {
          System.err.println("Missing argument: --install-pack <pack-dir>");
          System.exit(2);
          return;
        }
        boolean force = Arrays.asList(args).contains("--force");
        boolean allowInvalid = Arrays.asList(args).contains("--allow-invalid");
        installPack(
            packRepository, Path.of(args[1]).toAbsolutePath().normalize(), force, allowInvalid);
      }
      case "--run" -> {
        if (args.length < 2) {
          System.err.println("Missing argument: --run <pack-dir-or-id> [keyboard-id]");
          System.exit(2);
          return;
        }
        var parsed = parseRunArgs(packRepository, prefs, Arrays.copyOfRange(args, 1, args.length));
        LinuxHostRunner.runInteractive(
            parsed.packDir(), parsed.keyboardId(), parsed.outputConfig());
      }
      case "--smoke" -> {
        if (args.length < 2) {
          System.err.println("Missing argument: --smoke <pack-dir-or-id> [keyboard-id]");
          System.exit(2);
          return;
        }
        var parsed =
            parseSmokeArgs(packRepository, prefs, Arrays.copyOfRange(args, 1, args.length));
        LinuxHostRunner.runSmoke(
            parsed.packDir(), parsed.keyboardId(), parsed.outputConfig(), parsed.text());
      }
      default -> {
        printUsage(packsRoot, prefsFile);
        System.exit(2);
      }
    }
  }

  private static Path defaultPacksRoot(Map<String, String> environment) {
    String override = environment.get("NSK_PACKS_DIR");
    if (override != null && !override.isBlank()) {
      return Paths.get(override).toAbsolutePath().normalize();
    }
    return XdgPaths.dataHome(environment)
        .resolve(APP_ID)
        .resolve("packs")
        .toAbsolutePath()
        .normalize();
  }

  private static void printUsage(Path packsRoot) {
    System.err.println("NewSoftKeyboard Linux dev host");
    System.err.println();
    System.err.println("Commands:");
    System.err.println("  --list-packs");
    System.err.println("  --install-pack <pack-dir> [--force] [--allow-invalid]");
    System.err.println(
        "  --run <pack-dir-or-id> [keyboard-id] [--output=<editor|stdout|xdotool|ibus>]");
    System.err.println("       [--xdotool-window=<window-id>] [--xdotool-delay-ms=<n>]");
    System.err.println(
        "       [--ibus-socket=<socket-path>] [--ibus-control-socket=<socket-path>]"
            + " [--no-ibus-activation]");
    System.err.println(
        "  --smoke <pack-dir-or-id> [keyboard-id] [--text=<text>] [--output=<stdout|xdotool>]");
    System.err.println();
    System.err.println("Config keys (prefs file):");
    System.err.println("  output.mode=editor|stdout|xdotool|ibus");
    System.err.println("  xdotool.window=<window-id>");
    System.err.println("  xdotool.delay_ms=<integer>");
    System.err.println("  ibus.socket=<socket-path>");
    System.err.println("  ibus.control_socket=<socket-path>");
    System.err.println("  ibus.activation=true|false");
    System.err.println();
    System.err.println("Legacy usage (positional args):");
    System.err.println("  <pack-dir-or-id> [keyboard-id]");
    System.err.println();
    System.err.println("Default packs root: " + packsRoot);
    System.err.println("Override with NSK_PACKS_DIR=/path/to/packs");
    System.err.println("Override ibus socket with NSK_IBUS_SOCKET=/path/to/ibus.sock");
    System.err.println(
        "Override ibus control socket with NSK_IBUS_CONTROL_SOCKET=/path/to/ibus.control.sock");
  }

  private static Path defaultPrefsFile(Map<String, String> environment) {
    String override = environment.get("NSK_PREFS_FILE");
    if (override != null && !override.isBlank()) {
      return Paths.get(override).toAbsolutePath().normalize();
    }
    return XdgPaths.configHome(environment)
        .resolve(APP_ID)
        .resolve("prefs.properties")
        .toAbsolutePath()
        .normalize();
  }

  private static Path defaultIbusSocketPath(Map<String, String> environment) {
    String override = environment.get("NSK_IBUS_SOCKET");
    if (override != null && !override.isBlank()) {
      return Paths.get(override).toAbsolutePath().normalize();
    }
    return XdgPaths.runtimeDir(environment)
        .resolve(APP_ID)
        .resolve("ibus.sock")
        .toAbsolutePath()
        .normalize();
  }

  private static Path deriveIbusControlSocketPath(Path ibusSocketPath) {
    Objects.requireNonNull(ibusSocketPath);
    String fileName = ibusSocketPath.getFileName().toString();
    if (fileName.endsWith(".sock")) {
      return ibusSocketPath.resolveSibling(
          fileName.substring(0, fileName.length() - 4) + ".control.sock");
    }
    return ibusSocketPath.resolveSibling(fileName + ".control");
  }

  private static void printUsage(Path packsRoot, Path prefsFile) {
    printUsage(packsRoot);
    System.err.println("Default prefs file: " + prefsFile);
    System.err.println("Override with NSK_PREFS_FILE=/path/to/prefs.properties");
  }

  private static void listPacks(LinuxPackRepository packRepository) throws IOException {
    List<LinuxPackRepository.InstalledPack> packs = packRepository.listInstalledPacks();
    if (packs.isEmpty()) {
      System.out.println("No packs installed in " + packRepository.packsRoot());
      return;
    }
    System.out.println("Installed packs (" + packRepository.packsRoot() + "):");
    for (LinuxPackRepository.InstalledPack pack : packs) {
      System.out.println("- " + pack.id() + " (" + pack.name() + "): " + pack.dir());
    }
  }

  private static void installPack(
      LinuxPackRepository packRepository, Path sourceDir, boolean force, boolean allowInvalid)
      throws IOException {
    var result = packRepository.installPack(sourceDir, force, allowInvalid);
    if (!result.success()) {
      System.err.println(result.error().orElse("Unknown install error"));
      System.exit(1);
      return;
    }
    System.out.println("Installed pack at: " + result.installedDir().orElseThrow());
  }

  private static RunArgs parseRunArgs(
      LinuxPackRepository packRepository, FilePrefsStore prefs, String[] args) throws IOException {
    String outputModeRaw = prefs.getString("output.mode");
    OutputMode outputMode =
        outputModeRaw != null ? OutputMode.parseUnchecked(outputModeRaw) : OutputMode.EDITOR;
    String xdotoolWindow = prefs.getString("xdotool.window");
    int xdotoolDelayMs = prefs.getInt("xdotool.delay_ms", 0);
    boolean ibusActivation = prefs.getBoolean("ibus.activation", true);
    String ibusSocketRaw = prefs.getString("ibus.socket");
    Path ibusSocketPath =
        ibusSocketRaw != null ? Paths.get(ibusSocketRaw).toAbsolutePath().normalize() : null;
    if (ibusSocketPath == null) ibusSocketPath = defaultIbusSocketPath(System.getenv());
    String ibusControlSocketRaw = prefs.getString("ibus.control_socket");
    Path ibusControlSocketPath =
        ibusControlSocketRaw != null
            ? Paths.get(ibusControlSocketRaw).toAbsolutePath().normalize()
            : null;
    if (ibusControlSocketPath == null) {
      String controlOverride = System.getenv().get("NSK_IBUS_CONTROL_SOCKET");
      if (controlOverride != null && !controlOverride.isBlank()) {
        ibusControlSocketPath = Paths.get(controlOverride).toAbsolutePath().normalize();
      }
    }
    if (ibusControlSocketPath == null)
      ibusControlSocketPath = deriveIbusControlSocketPath(ibusSocketPath);

    var positional = new ArrayList<String>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--output=")) {
        outputMode = OutputMode.parse(arg.substring("--output=".length()));
      } else if (arg.equals("--output")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --output");
        outputMode = OutputMode.parse(args[++i]);
      } else if (arg.startsWith("--xdotool-window=")) {
        xdotoolWindow = arg.substring("--xdotool-window=".length()).trim();
      } else if (arg.equals("--xdotool-window")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --xdotool-window");
        xdotoolWindow = args[++i].trim();
      } else if (arg.startsWith("--xdotool-delay-ms=")) {
        xdotoolDelayMs =
            parseIntArg("--xdotool-delay-ms", arg.substring("--xdotool-delay-ms=".length()));
      } else if (arg.equals("--xdotool-delay-ms")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --xdotool-delay-ms");
        xdotoolDelayMs = parseIntArg("--xdotool-delay-ms", args[++i]);
      } else if (arg.startsWith("--ibus-socket=")) {
        ibusSocketPath =
            Paths.get(arg.substring("--ibus-socket=".length())).toAbsolutePath().normalize();
      } else if (arg.equals("--ibus-socket")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --ibus-socket");
        ibusSocketPath = Paths.get(args[++i]).toAbsolutePath().normalize();
      } else if (arg.startsWith("--ibus-control-socket=")) {
        ibusControlSocketPath =
            Paths.get(arg.substring("--ibus-control-socket=".length()))
                .toAbsolutePath()
                .normalize();
      } else if (arg.equals("--ibus-control-socket")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --ibus-control-socket");
        ibusControlSocketPath = Paths.get(args[++i]).toAbsolutePath().normalize();
      } else if (arg.equals("--ibus-activation")) {
        ibusActivation = true;
      } else if (arg.equals("--no-ibus-activation")) {
        ibusActivation = false;
      } else if (arg.startsWith("--")) {
        throw new IOException("Unknown option: " + arg);
      } else {
        positional.add(arg);
      }
    }

    if (positional.isEmpty()) {
      throw new IOException("Missing required argument: <pack-dir-or-id>");
    }
    if (positional.size() > 2) {
      throw new IOException(
          "Too many positional arguments. Expected: <pack-dir-or-id> [keyboard-id]");
    }

    Path packDir = packRepository.resolvePackDir(positional.get(0));
    String keyboardId = positional.size() == 2 ? positional.get(1) : null;
    return new RunArgs(
        packDir,
        keyboardId,
        new OutputConfig(
            outputMode,
            Optional.ofNullable(xdotoolWindow).filter(s -> !s.isBlank()),
            xdotoolDelayMs,
            ibusSocketPath,
            ibusControlSocketPath,
            ibusActivation));
  }

  private static SmokeArgs parseSmokeArgs(
      LinuxPackRepository packRepository, FilePrefsStore prefs, String[] args) throws IOException {
    String text = "abc";
    var remainingArgs = new ArrayList<String>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--text=")) {
        text = arg.substring("--text=".length());
      } else if (arg.equals("--text")) {
        if (i + 1 >= args.length) throw new IOException("Missing value for --text");
        text = args[++i];
      } else {
        remainingArgs.add(arg);
      }
    }

    RunArgs parsed = parseRunArgs(packRepository, prefs, remainingArgs.toArray(String[]::new));
    return new SmokeArgs(parsed.packDir(), parsed.keyboardId(), parsed.outputConfig(), text);
  }

  private static int parseIntArg(String flagName, String raw) throws IOException {
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new IOException("Invalid integer for " + flagName + ": " + raw, e);
    }
  }
}
