package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.intellij.openapi.application.PathManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Slf4j
public class AzureMcpPackageManager {
    private final GithubClient gitHubClient;
    private final String platform;

    public AzureMcpPackageManager() {
        this.gitHubClient = new GithubClient();
        this.platform = getPlatformIdentifier();
    }

    @Nullable
    public synchronized File getAzureMcpExecutable() {
        try {
            final GithubRelease latestRelease = gitHubClient.getLatestAzureMcpRelease();
            if (latestRelease != null && latestRelease.getAssets() != null) {
                final String tagName = latestRelease.getTagName();
                log.info("Latest version of Azure MCP: " + tagName);

                final Optional<GithubAsset> githubAsset = latestRelease.getAssets()
                        .stream()
                        .filter(asset -> asset.getName().contains(platform))
                        .findFirst();

                if (githubAsset.isPresent()) {
                    final GithubAsset asset = githubAsset.get();
                    log.info("Azure MCP package for current platform: " + asset.getName());
                    final long startTime = System.currentTimeMillis();
                    final String azMcpDir = PathManager.getPluginsPath() + "/azure-toolkit-for-intellij/azmcp";
                    final File azMcpDirFile = new File(azMcpDir);
                    if (azMcpDirFile.exists() || azMcpDirFile.mkdirs()) {
                        final Path versionFile = Path.of(azMcpDir + "/version.txt");

                        final File extractedDir = new File(azMcpDirFile, "/azmcp_package_" + tagName);
                        extractedDir.mkdirs();
                        final String executablePath = extractedDir.getAbsolutePath() + getExecutableRelativePath();
                        final File azMcpExe = new File(executablePath);
                        if (!azMcpExe.exists()) {
                            Files.writeString(versionFile, tagName, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                            final File azMcpTgz = new File(azMcpDir, "azmcp_" + tagName + ".tgz");
                            log.info("Downloading Azure MCP Server to: " + azMcpTgz.getAbsolutePath());
                            final boolean downloaded = gitHubClient.downloadToFile(asset.getBrowserDownloadUrl(), azMcpTgz);
                            if (downloaded && digestMatches(azMcpTgz, asset.getDigest())) {
                                log.info("Downloaded Azure MCP Server successfully in " + (System.currentTimeMillis() - startTime) + " ms");
                                log.info("Extracting Azure MCP Server to: " + extractedDir.getAbsolutePath());
                                extractTarGz(azMcpTgz, extractedDir);
                                log.info("Azure MCP Server extracted successfully to: " + extractedDir.getAbsolutePath());
                            }
                        }

                        if (azMcpExe.exists() && (azMcpExe.canExecute() || azMcpExe.setExecutable(true))) {
                            log.info("Azure MCP Server executable found at: " + azMcpExe.getAbsolutePath());
                            return azMcpExe;
                        }
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("Error reading Azure MCP Server version: " + e.getMessage());
        }
        return null;
    }

    private boolean digestMatches(File azMcpTgz, String expectedDigest) {
        try {
            // GitHub releases API computes the SHA-256 digest of the file contents.
            // https://github.blog/changelog/2025-06-03-releases-now-expose-digests-for-release-assets/
            final String downloadFileDigest = DigestUtils.sha256Hex(new FileInputStream(azMcpTgz));
            return StringUtils.equalsIgnoreCase("sha256:" + downloadFileDigest, expectedDigest);
        } catch (final Exception e) {
            log.error("Failed to calculate file digest", e);
            return false;
        }
    }

    public synchronized void cleanup() {
        try {
            final String azMcpDir = PathManager.getPluginsPath() + "/azure-toolkit-for-intellij/azmcp";
            final File azMcpDirFile = new File(azMcpDir);
            if (!azMcpDirFile.exists()) {
                return;
            }

            final Path versionFile = Path.of(azMcpDir + "/version.txt");
            String currentVersion = null;
            if (versionFile.toFile().exists()) {
                currentVersion = new String(Files.readAllBytes(versionFile));
            }

            final Path currentPackage = Path.of(azMcpDir + "/azmcp_package_" + currentVersion).toAbsolutePath();
            Files.list(Path.of(azMcpDir))
                    .filter(path -> !path.equals(currentPackage))
                    .filter(path -> !path.equals(versionFile))
                    .forEach(path -> {
                        delete(path);
                    });

        } catch (final Exception exception) {
            System.err.println("Error cleaning up Azure MCP Server: " + exception.getMessage());
        }
    }

    private static void delete(Path path) {
        try {
            if (path.toFile().isDirectory()) {
                Files.list(path).forEach(AzureMcpPackageManager::delete);
            }
            Files.delete(path);
        } catch (final IOException e) {
            System.err.println("Error deleting file: " + path.toString());
        }
    }

    private @NotNull String getExecutableRelativePath() {
        String executablePath = "/package/dist/azmcp";
        if (SystemUtils.IS_OS_WINDOWS) {
            executablePath += ".exe";
        }
        return executablePath;
    }

    private void extractTarGz(File tarGzFile, File destDir) throws IOException {
        try (final TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new FileInputStream(tarGzFile)))) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                final File outputFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    outputFile.getParentFile().mkdirs();
                    try (final FileOutputStream fos = new FileOutputStream(outputFile)) {
                        tarIn.transferTo(fos);
                    }
                }
            }
        }
    }

    private static String getPlatformIdentifier() {
        // Operating System detection
        String os = null;
        if (SystemUtils.IS_OS_WINDOWS) {
            os = "win32";
        } else if (SystemUtils.IS_OS_LINUX) {
            os = "linux";
        } else if (SystemUtils.IS_OS_MAC) {
            os = "darwin";
        } else {
            throw new RuntimeException("Unsupported OS " + SystemUtils.OS_NAME);
        }
        final String arch = getArch();
        return os + "-" + arch;
    }

    private static String getArch() {
        final String arch = SystemUtils.OS_ARCH.toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64")) {
            return "x64";
        }
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        }
        throw new RuntimeException("Unsupported architecture: " + arch);
    }


}
