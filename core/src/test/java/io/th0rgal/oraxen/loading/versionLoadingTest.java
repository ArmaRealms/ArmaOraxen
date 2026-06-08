package io.th0rgal.oraxen.loading;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class versionLoadingTest {

    private static final Path serverDir = Path.of(System.getProperty("user.home"), "Oraxen", "Servers");
    private static final Path pluginsDir = serverDir.resolve("plugins");
    private static final Path paperDir = serverDir.resolve("Paper");
    private static final List<String> paperVersions = List.of(
            "1.20.4",
            "1.20.5",
            "1.20.6",
            "1.21",
            // 1.21.1 and 1.21.2 are intentionally excluded because they do not have Paper jars.
            "1.21.3",
            "1.21.4",
            "1.21.5",
            "1.21.6",
            "1.21.7",
            "1.21.8",
            "1.21.9",
            "1.21.10",
            "1.21.11",
            "26.1.2"
    );
    private static final Duration serverTimeout = Duration.ofMinutes(3);

    private static final Map<String, String> javaVersions = Map.of(
            "1.20.4-1.21.11", "C:\\Program Files\\Java\\jdk-21\\",
            "26.1.2", "C:\\Program Files\\Java\\jdk-25\\"
    );

    private static final Map<String, String> paperDownloadUrls = Map.ofEntries(
            Map.entry("26.1.2", "https://fill-data.papermc.io/v1/objects/d30fae0c74092b10855f0412ca6b265c60301a013d34bc28a2a41bf5682dd80b/paper-26.1.2-69.jar"),
            Map.entry("1.21.11", "https://fill-data.papermc.io/v1/objects/d30fae0c74092b10855f0412ca6b265c60301a013d34bc28a2a41bf5682dd80b/paper-26.1.2-69.jar"),
            Map.entry("1.21.10", "https://fill-data.papermc.io/v1/objects/158703f75a26f842ea656b3dc6d75bf3d1ec176b97a2c36384d0b80b3871af53/paper-1.21.10-130.jar"),
            Map.entry("1.21.9", "https://fill-data.papermc.io/v1/objects/aec002e77c7566e49494fdf05430b96078ffd1d7430e652d4f338fef951e7a10/paper-1.21.9-59.jar"),
            Map.entry("1.21.8", "https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar"),
            Map.entry("1.21.7", "https://fill-data.papermc.io/v1/objects/83838188699cb2837e55b890fb1a1d39ad0710285ed633fbf9fc14e9f47ce078/paper-1.21.7-32.jar"),
            Map.entry("1.21.6", "https://fill-data.papermc.io/v1/objects/35e2dfa66b3491b9d2f0bb033679fa5aca1e1fdf097e7a06a80ce8afeda5c214/paper-1.21.6-48.jar"),
            Map.entry("1.21.5", "https://fill-data.papermc.io/v1/objects/2ae6ae22adf417699746e0f89fc2ef6cb6ee050a5f6608cee58f0535d60b509e/paper-1.21.5-114.jar"),
            Map.entry("1.21.4", "https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar"),
            Map.entry("1.21.3", "https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar"),
            Map.entry("1.21", "https://fill-data.papermc.io/v1/objects/ab9bb1afc3cea6978a0c03ce8448aa654fe8a9c4dddf341e7cbda1b0edaa73f5/paper-1.21-130.jar"),
            Map.entry("1.20.6", "https://fill-data.papermc.io/v1/objects/4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640/paper-1.20.6-151.jar"),
            Map.entry("1.20.5", "https://fill-data.papermc.io/v1/objects/3cd7da2f8df92e082a501a39c674aab3c0343edd179b86f5baccaebfc9974132/paper-1.20.5-22.jar"),
            Map.entry("1.20.4", "https://fill-data.papermc.io/v1/objects/cabed3ae77cf55deba7c7d8722bc9cfd5e991201c211665f9265616d9fe5c77b/paper-1.20.4-499.jar")
    );

    @Test
    @Tag("version-loading")
    void runPaper() throws Exception {
        Path projectDir = findProjectDir();

        buildPlugin(projectDir);
        prepareServerDirectory();
        copyBuiltJar(findBuiltJar(projectDir));

        for (String version : paperVersions) {
            System.out.println("Testing Paper " + version + "...");
            runPaperAndAssertOraxenLoads(version);
            System.out.println("Paper " + version + " loaded Oraxen successfully.\n");
        }
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("gradlew")) || Files.exists(current.resolve("gradlew.bat"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not find project directory containing gradlew");
    }

    private static void buildPlugin(Path projectDir) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder builder = windows
                ? new ProcessBuilder("cmd", "/c", ".\\gradlew", "build")
                : new ProcessBuilder("./gradlew", "build");
        builder.directory(projectDir.toFile());
        builder.environment().put("CI", "true");
        builder.redirectErrorStream(true);

        Process process = builder.start();
        String output = readAllOutput(process);
        assertTrue(process.waitFor(10, TimeUnit.MINUTES), "Timed out while building Oraxen");
        assertTrue(process.exitValue() == 0, "Gradle build failed:\n" + output);
    }

    private static void prepareServerDirectory() throws IOException {
        Files.createDirectories(serverDir);

        try (Stream<Path> paths = Files.walk(serverDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(serverDir))
                    // Keep Paper jars in place so Paper/<version>.jar can be started after cleanup.
                    .filter(path -> !path.equals(paperDir) && !path.startsWith(paperDir))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new RuntimeException("Failed to delete " + path, exception);
                        }
                    });
        }

        Files.createDirectories(pluginsDir);
        Files.createDirectories(paperDir);
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
    }

    private static Path findBuiltJar(Path projectDir) throws IOException {
        try (Stream<Path> jars = Files.walk(projectDir)) {
            Optional<Path> jar = jars
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("oraxen-.*\\.jar"))
                    .filter(path -> path.toString().contains("build"))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
            return jar.orElseThrow(() -> new IllegalStateException("Could not find built Oraxen jar"));
        }
    }

    private static void copyBuiltJar(Path builtJar) throws IOException {
        Files.copy(builtJar, pluginsDir.resolve(builtJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void runPaperAndAssertOraxenLoads(String version) throws Exception {
        Path paperJar = ensurePaperJarExists(version);
        System.out.println("Starting Paper " + version + " from " + paperJar);

        ProcessBuilder builder = new ProcessBuilder(
                "java", "-Xmx1G", "-jar", "Paper/" + version + ".jar", "--nogui", "--port", "25590"
        );
        builder.directory(serverDir.toFile());
        builder.environment().put("JAVA_HOME", javaHomeFor(version));
        builder.environment().put("PATH", javaHomeFor(version) + "bin;" + builder.environment().getOrDefault("PATH", ""));
        builder.redirectErrorStream(true);

        Process process = builder.start();
        boolean enabledOraxen = false;
        StringBuilder output = new StringBuilder();
        Instant deadline = Instant.now().plus(serverTimeout);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while (Instant.now().isBefore(deadline)) {
                if (reader.ready()) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    output.append(line).append(System.lineSeparator());

                    String lower = line.toLowerCase();
                    if (lower.contains("enabling oraxen") || lower.contains("oraxen") && lower.contains("enabled")) {
                        enabledOraxen = true;
                    }
                    if (lower.contains("disabling oraxen")) {
                        failWithServerOutput(version, "Oraxen was disabled during startup", output);
                    }
                    if (lower.contains("error occurred while enabling oraxen")) {
                        failWithServerOutput(version, "Paper reported an error while enabling Oraxen", output);
                    }

                    if (enabledOraxen && line.contains("Done (")) {
                        return;
                    }
                } else if (!process.isAlive()) {
                    failWithServerOutput(version, "Paper exited with code " + process.exitValue() + " before Oraxen loaded", output);
                } else {
                    Thread.sleep(100);
                }
            }

            failWithServerOutput(version, "Timed out after " + serverTimeout + " waiting for Oraxen to load", output);
        } finally {
            stopServer(process);
        }
    }

    private static Path ensurePaperJarExists(String version) throws IOException {
        Path paperJar = paperDir.resolve(version + ".jar");
        if (Files.exists(paperJar)) {
            System.out.println("Using existing Paper jar for " + version + " at " + paperJar + ".");
            return paperJar;
        }

        String downloadUrl = paperDownloadUrls.get(version);
        if (downloadUrl == null) {
            throw new IllegalArgumentException("No Paper download URL configured for " + version + ".");
        }

        System.out.println("Downloading Paper " + version + " from " + downloadUrl + ".");
        Files.createDirectories(paperDir);
        Path temporaryJar = paperDir.resolve(version + ".jar.download.");
        try (var inputStream = URI.create(downloadUrl).toURL().openStream()) {
            Files.copy(inputStream, temporaryJar, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temporaryJar, paperJar, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Downloaded Paper " + version + " to " + paperJar + ".");
        return paperJar;
    }

    private static void failWithServerOutput(String version, String reason, StringBuilder output) {
        Path latestLog = serverDir.resolve("logs").resolve("latest.log");
        String message = "Paper " + version + " failed with reason \"" + reason + "\".\n\nSee server log at " + latestLog.toAbsolutePath() + ".";
        System.err.println(message);
        fail(message);
    }

    private static String javaHomeFor(String version) {
        String minecraftVersion = version.contains("-") ? version.substring(0, version.indexOf('-')) : version;
        return javaVersions.entrySet().stream()
                .filter(entry -> versionMatches(entry.getKey(), minecraftVersion) || entry.getKey().equals(version))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No JAVA_HOME configured for " + version));
    }

    private static boolean versionMatches(String key, String version) {
        if (!key.contains("-")) {
            return key.equals(version);
        }
        String[] range = key.split("-", 2);
        return compareVersions(version, range[0]) >= 0 && compareVersions(version, range[1]) <= 0;
    }

    private static int compareVersions(String left, String right) {
        int[] leftParts = versionParts(left);
        int[] rightParts = versionParts(right);
        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            int leftPart = i < leftParts.length ? leftParts[i] : 0;
            int rightPart = i < rightParts.length ? rightParts[i] : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private static int[] versionParts(String version) {
        String[] parts = version.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            numbers[i] = Integer.parseInt(parts[i].replaceAll("\\D.*", ""));
        }
        return numbers;
    }

    private static String readAllOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            return output.toString();
        }
    }

    private static void stopServer(Process process) throws IOException, InterruptedException {
        if (!process.isAlive()) {
            return;
        }
        process.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
    }
}
