package com.brica.extraction.infrastructure.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipFile;

/** Stages only trusted binaries for the Windows launcher, never the server's configuration or storage. */
final class WindowsSourceLaunch {
    private WindowsSourceLaunch() { }

    static ProcessBuilder prepare(Path stage) throws IOException {
        String systemRoot = System.getenv("SystemRoot");
        String localAppData = System.getenv("LOCALAPPDATA");
        if (systemRoot == null || localAppData == null) { throw new IOException("WINDOWS_PROFILE_UNAVAILABLE"); }
        String classPath = System.getProperty("java.class.path");
        if (classPath.contains(java.io.File.pathSeparator)) { throw new IOException("WINDOWS_WORKER_REQUIRES_PACKAGED_SERVER"); }
        return prepare(stage, Path.of(classPath).toAbsolutePath(), systemRoot, localAppData);
    }

    static ProcessBuilder prepare(Path stage, Path server, String systemRoot, String localAppData) throws IOException {
        try (var jar = new ZipFile(server.toFile())) {
            if (jar.getEntry("org/springframework/boot/loader/launch/PropertiesLauncher.class") == null
                    || jar.getEntry("BOOT-INF/classes/com/brica/extraction/infrastructure/worker/SourceWorker.class") == null) {
                throw new IOException("WINDOWS_WORKER_REQUIRES_PACKAGED_SERVER");
            }
        }
        Path javaHome = Path.of(System.getProperty("java.home")).toRealPath();
        Path runtime = stage.resolve("runtime");
        for (String module : List.of("bin", "conf", "lib")) { copyTree(javaHome.resolve(module), runtime.resolve(module)); }
        Files.copy(server, stage.resolve("reader.jar"), StandardCopyOption.COPY_ATTRIBUTES);
        Path launcher = stage.resolve("launch.ps1");
        try (var script = WindowsSourceLaunch.class.getResourceAsStream("/extraction/windows-launch.ps1")) {
            if (script == null) { throw new IOException("WINDOWS_LAUNCHER_UNAVAILABLE"); }
            Files.copy(script, launcher);
        }
        var builder = new ProcessBuilder(Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString(),
                "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "RemoteSigned", "-File", launcher.toString());
        builder.directory(stage.toFile());
        builder.environment().clear();
        builder.environment().put("SystemRoot", systemRoot);
        builder.environment().put("LOCALAPPDATA", localAppData);
        builder.environment().put("TEMP", stage.toString());
        builder.environment().put("TMP", stage.toString());
        return builder;
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path item : paths.toList()) {
                Path destination = target.resolve(source.relativize(item));
                if (Files.isDirectory(item, LinkOption.NOFOLLOW_LINKS)) { Files.createDirectories(destination); }
                else if (Files.isRegularFile(item, LinkOption.NOFOLLOW_LINKS)) { Files.copy(item, destination); }
                else { throw new IOException("UNSUPPORTED_WINDOWS_RUNTIME_LAYOUT"); }
            }
        }
    }
}
