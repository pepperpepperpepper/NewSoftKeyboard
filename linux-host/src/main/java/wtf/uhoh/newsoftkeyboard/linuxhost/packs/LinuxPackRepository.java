package wtf.uhoh.newsoftkeyboard.linuxhost.packs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.FileSystemKeyboardPackLoader;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPack;
import wtf.uhoh.newsoftkeyboard.keyboard.core.packs.KeyboardPackValidator;

public final class LinuxPackRepository {
  private static final String MANIFEST_FILENAME = "manifest.json";

  private final Path packsRoot;

  public LinuxPackRepository(Path packsRoot) {
    this.packsRoot = Objects.requireNonNull(packsRoot).toAbsolutePath().normalize();
  }

  public Path packsRoot() {
    return packsRoot;
  }

  public List<InstalledPack> listInstalledPacks() throws IOException {
    if (!Files.exists(packsRoot)) return List.of();

    var result = new ArrayList<InstalledPack>();
    try (var stream = Files.list(packsRoot)) {
      for (Path child : stream.toList()) {
        if (!Files.isDirectory(child)) continue;
        if (!Files.exists(child.resolve(MANIFEST_FILENAME))) continue;

        KeyboardPack pack = FileSystemKeyboardPackLoader.loadPack(child);
        result.add(new InstalledPack(pack.manifest().id(), pack.manifest().name(), child));
      }
    }
    return List.copyOf(result);
  }

  public Path resolvePackDir(String packArg) {
    Path candidatePath = Path.of(packArg).toAbsolutePath().normalize();
    if (isPackDir(candidatePath)) {
      return candidatePath;
    }

    Optional<Path> repoRoot = findRepoRoot(Path.of("").toAbsolutePath().normalize());
    if (repoRoot.isPresent()) {
      Path repoResolved = repoRoot.get().resolve(packArg).toAbsolutePath().normalize();
      if (isPackDir(repoResolved)) return repoResolved;
    }

    return packsRoot.resolve(packArg).toAbsolutePath().normalize();
  }

  private static boolean isPackDir(Path dir) {
    return Files.isDirectory(dir) && Files.exists(dir.resolve(MANIFEST_FILENAME));
  }

  private static Optional<Path> findRepoRoot(Path startDir) {
    Path current = startDir;
    while (current != null) {
      if (Files.exists(current.resolve("settings.gradle"))) {
        return Optional.of(current);
      }
      current = current.getParent();
    }
    return Optional.empty();
  }

  public InstallResult installPack(Path sourcePackDir, boolean force, boolean allowInvalid)
      throws IOException {
    if (!Files.isDirectory(sourcePackDir)) {
      return InstallResult.failure("Source is not a directory: " + sourcePackDir);
    }

    KeyboardPack sourcePack = FileSystemKeyboardPackLoader.loadPack(sourcePackDir);
    var validation = KeyboardPackValidator.validate(sourcePack);
    if (!validation.isValid() && !allowInvalid) {
      return InstallResult.failure(
          "Pack validation failed:\n- " + String.join("\n- ", validation.errors()));
    }

    Path targetDir = packsRoot.resolve(sourcePack.manifest().id()).toAbsolutePath().normalize();
    if (Files.exists(targetDir)) {
      if (!force) {
        return InstallResult.failure(
            "Target already exists: " + targetDir + " (pass --force to overwrite)");
      }
      deleteRecursively(targetDir);
    }

    Files.createDirectories(packsRoot);
    copyDirectory(sourcePackDir, targetDir);

    return InstallResult.success(targetDir);
  }

  private static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
    Files.walkFileTree(
        sourceDir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path relative = sourceDir.relativize(dir);
            Files.createDirectories(targetDir.resolve(relative));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Path relative = sourceDir.relativize(file);
            Files.copy(
                file,
                targetDir.resolve(relative),
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) return;
    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  public record InstalledPack(String id, String name, Path dir) {
    public InstalledPack {
      Objects.requireNonNull(id);
      Objects.requireNonNull(name);
      Objects.requireNonNull(dir);
    }
  }

  public record InstallResult(
      boolean success, Optional<Path> installedDir, Optional<String> error) {
    public static InstallResult success(Path installedDir) {
      return new InstallResult(true, Optional.of(installedDir), Optional.empty());
    }

    public static InstallResult failure(String error) {
      return new InstallResult(false, Optional.empty(), Optional.of(error));
    }

    public InstallResult {
      Objects.requireNonNull(installedDir);
      Objects.requireNonNull(error);
    }
  }
}
