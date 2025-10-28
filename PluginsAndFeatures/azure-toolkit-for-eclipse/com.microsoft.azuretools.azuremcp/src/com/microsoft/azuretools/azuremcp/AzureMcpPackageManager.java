package com.microsoft.azuretools.azuremcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureMcpPackageManager {
	
	private static final Logger log = LoggerFactory.getLogger(AzureMcpPackageManager.class);
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
                        .filter(asset -> asset.getName().startsWith("Azure.Mcp.Server-" + platform))
                        .findFirst();

                if (githubAsset.isPresent()) {
                    final GithubAsset asset = githubAsset.get();
                    log.info("Azure MCP package for current platform: " + asset.getName());
                    final long startTime = System.currentTimeMillis();
                    
                    final File azMcpDirFile = getPluginDirectory();
                    if (azMcpDirFile.exists() || azMcpDirFile.mkdirs()) {
                        final Path versionFile = Path.of(azMcpDirFile.getAbsolutePath() + "/version.txt");

                        final File extractedDir = new File(azMcpDirFile, "/azmcp_package_" + tagName);
                        extractedDir.mkdirs();
                        final String executablePath = extractedDir.getAbsolutePath() + getExecutableRelativePath();
                        final File azMcpExe = new File(executablePath);
                        if (!azMcpExe.exists()) {
                            Files.writeString(versionFile, tagName, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                            final File azMcpZip = new File(azMcpDirFile.getAbsolutePath(), "azmcp_" + tagName + ".zip");
                            log.info("Downloading Azure MCP Server to: " + azMcpZip.getAbsolutePath());
                            final boolean downloaded = gitHubClient.downloadToFile(asset.getBrowserDownloadUrl(), azMcpZip);
                            if (downloaded && digestMatches(azMcpZip, asset.getDigest())) {
                                log.info("Downloaded Azure MCP Server successfully in " + (System.currentTimeMillis() - startTime) + " ms");
                                log.info("Extracting Azure MCP Server to: " + extractedDir.getAbsolutePath());
                                extractZip(azMcpZip, extractedDir);
                                log.info("Azure MCP Server extracted successfully to: " + extractedDir.getAbsolutePath());
                            }
                        }
                        
                        
                        boolean exists = azMcpExe.exists();
                        boolean canExecute = azMcpExe.canExecute();
                                             

                        if (azMcpExe.exists() && (azMcpExe.canExecute() || azMcpExe.setExecutable(true))) {
                            log.info("Azure MCP Server executable found at: " + azMcpExe.getAbsolutePath());
                            return azMcpExe;
                        }
                    }
                }
            }
        } catch (final IOException e) {
            log.error("Error getting Azure MCP executable: " + e.getMessage());
        }
        return null;
    }

    private File getPluginDirectory() {
    	
    	Location configArea = Platform.getConfigurationLocation();
    	URL configURL = configArea.getURL();
    	File configDir = new File(configURL.getPath(), "com.microsoft.azuretools.azuremcp");
    	configDir.mkdirs();
    	File azMcpDirectory = new File(configDir, "azmcp");
		return azMcpDirectory;
	}

	private boolean digestMatches(File azMcpZip, String expectedDigest) {
        try {
            // GitHub releases API computes the SHA-256 digest of the file contents.
            // https://github.blog/changelog/2025-06-03-releases-now-expose-digests-for-release-assets/
            final String downloadFileDigest = DigestUtils.sha256Hex(new FileInputStream(azMcpZip));
            return StringUtils.equalsIgnoreCase("sha256:" + downloadFileDigest, expectedDigest);
        } catch (final Exception e) {
            log.error("Failed to calculate file digest", e);
            return false;
        }
    }

    public synchronized void cleanup() {
        try {
            
            final File azMcpDirFile = getPluginDirectory();
            if (!azMcpDirFile.exists()) {
                return;
            }

            final Path versionFile = Path.of(azMcpDirFile.getAbsolutePath() + "/version.txt");
            String currentVersion = null;
            if (versionFile.toFile().exists()) {
                currentVersion = new String(Files.readAllBytes(versionFile));
            }

            final Path currentPackage = Path.of(azMcpDirFile.getAbsolutePath() + "/azmcp_package_" + currentVersion).toAbsolutePath();
            Files.list(Path.of(azMcpDirFile.getAbsolutePath()))
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

    private String getExecutableRelativePath() {
        String executablePath = "/azmcp";
        if (SystemUtils.IS_OS_WINDOWS) {
            executablePath += ".exe";
        }
        return executablePath;
    }

    private void extractZip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outputFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    outputFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static String getPlatformIdentifier() {
        // Operating System detection
        String os = null;
        if (SystemUtils.IS_OS_WINDOWS) {
            os = "win";
        } else if (SystemUtils.IS_OS_LINUX) {
            os = "linux";
        } else if (SystemUtils.IS_OS_MAC) {
            os = "osx";
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