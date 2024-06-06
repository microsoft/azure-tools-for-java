/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.coretools;

import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FunctionsCoreToolsManager {
    private ReleaseInfo releaseInfoCache;
    private final String RELEASE_TAG = "v4";
    private static final FunctionsCoreToolsManager instance = new FunctionsCoreToolsManager();

    public static FunctionsCoreToolsManager getInstance() {
        return instance;
    }

    public void downloadReleaseTools(String downloadDirPath) {
        if (Objects.isNull(releaseInfoCache)) {
            cacheReleaseInfoFromFeed();
        }
        doDownloadReleaseTools(releaseInfoCache, downloadDirPath);
    }

    /**
     * refer to https://github.com/JetBrains/azure-tools-for-intellij
     */
    private void cacheReleaseInfoFromFeed() {
        final ReleaseFilter releaseFilter = generateFilter();
        final ReleaseFeedData releaseFeedData = ReleaseService.getInstance().getReleaseFeedData();
        Optional.ofNullable(releaseFeedData).ifPresent(data -> {
            if (!data.getTags().containsKey(RELEASE_TAG)) {
                return;
            }
            final String releaseVersion = data.getTags().get(RELEASE_TAG).getRelease();
            final ReleaseFeedData.ReleaseData releaseData = releaseFeedData.getReleases().get(releaseVersion);
            releaseData.getCoreTools().stream()
                // Match OS and ensure a download link is present
                .filter(it -> releaseFilter.os.equalsIgnoreCase(it.getOs()) && StringUtils.isNotBlank(it.getDownloadLink()))
                // Sort by architecture. Use 9999 when no match is found.
                .sorted((o1, o2) -> {
                    final int rank1 = IntStream.range(0, releaseFilter.architectures.size())
                        .filter(i -> releaseFilter.architectures.get(i).equalsIgnoreCase(o1.getArchitecture()))
                        .findFirst().orElse(9999);
                    final int rank2 = IntStream.range(0, releaseFilter.architectures.size())
                        .filter(i -> releaseFilter.architectures.get(i).equalsIgnoreCase(o2.getArchitecture()))
                        .findFirst().orElse(9999);
                    return rank1 - rank2;
                }).min((o1, o2) -> {    // Sort by size. Use 9999 when no match is found.
                    final int rank1 = IntStream.range(0, releaseFilter.sizes.size())
                        .filter(i -> releaseFilter.sizes.get(i).equalsIgnoreCase(o1.getSize()))
                        .findFirst().orElse(9999);
                    final int rank2 = IntStream.range(0, releaseFilter.sizes.size())
                        .filter(i -> releaseFilter.sizes.get(i).equalsIgnoreCase(o2.getSize()))
                        .findFirst().orElse(9999);
                    return rank1 - rank2;
                }).ifPresent(releaseCoreTool -> this.releaseInfoCache = new ReleaseInfo(releaseVersion.toLowerCase(), releaseCoreTool.getDownloadLink()));
        });
    }

    private void doDownloadReleaseTools(@Nullable ReleaseInfo releaseInfo, String downloadDirPath) {
        if (Objects.isNull(releaseInfo)) {
            return;
        }
        final String unzipRootDir = Paths.get(downloadDirPath, extractFileNameFromUrl(releaseInfo.downloadLink)).toString();
        final String executionFilePath = Paths.get(unzipRootDir, "windows".equalsIgnoreCase(generateFilter().os) ? "func.exe" : "func").toString();
        final String AZURE_FUNCTIONS = "AzureFunctions";
        try {
            final File tempFile = File.createTempFile(
                String.format("%s-%s", AZURE_FUNCTIONS, releaseInfo.releaseVersion),
                ".zip", Files.createTempDirectory(AZURE_FUNCTIONS).toFile());
            final FileOutputStream outputStream = new FileOutputStream(tempFile);
            outputStream.getChannel().transferFrom(Channels.newChannel(new URL(releaseInfo.downloadLink).openStream()), 0, Long.MAX_VALUE);
            unzip(tempFile, unzipRootDir);
            tempFile.deleteOnExit();
            final File executionFile = new File(executionFilePath);
            if (executionFile.exists() && !executionFile.canExecute()) {
                executionFile.setExecutable(true);
            }
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
        Azure.az().config().setFunctionCoreToolsPath(executionFilePath);
        AzureConfigInitializer.saveAzConfig();
        AzureEventBus.emit("function.download_func_core_tools_succeed.version", releaseInfo.releaseVersion);
    }

    private void unzip(File zipFile, String destDirPath) throws Exception {
        Files.createDirectories(Paths.get(destDirPath));
        final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            final File zipEntryFile = new File(destDirPath, zipEntry.getName());
            if (!zipEntryFile.toPath().normalize().startsWith(destDirPath)) {
                throw new Exception("Bad zip entry");
            }
            if (zipEntry.isDirectory()) {
                Files.createDirectories(zipEntryFile.toPath());
            } else {
                Files.createDirectories(zipEntryFile.toPath().getParent());
                try (final OutputStream out = new FileOutputStream(zipEntryFile)) {
                    IOUtils.copy(zipInputStream, out);
                }
            }
            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.closeEntry();
        zipInputStream.close();
    }

    /**
     * refer to https://github.com/JetBrains/azure-tools-for-intellij
     */
    private ReleaseFilter generateFilter() {
        final String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        final String architectureName = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
        final boolean isIntel64 = "x86_64".equalsIgnoreCase(architectureName) || "amd64".equalsIgnoreCase(architectureName);
        final boolean isArm64 = "aarch64".equalsIgnoreCase(architectureName) || "arm64".equalsIgnoreCase(architectureName);
        if (osName.startsWith("windows") && isIntel64) {
            return new ReleaseFilter("windows", List.of("x64"), List.of("full"));
        } else if (osName.startsWith("windows")) {
            return new FunctionsCoreToolsManager.ReleaseFilter("windows", List.of("x86"), List.of("full"));
        } else if (osName.startsWith("mac") && isArm64) {
            return new FunctionsCoreToolsManager.ReleaseFilter("macOS", List.of("arm64", "x64"), List.of("full"));
        } else if (osName.startsWith("mac")) {
            return new FunctionsCoreToolsManager.ReleaseFilter("macOS", List.of("x64"), List.of("full"));
        } else if (osName.startsWith("linux")) {
            return new FunctionsCoreToolsManager.ReleaseFilter("linux", List.of("x64"), List.of("full"));
        }
        return new FunctionsCoreToolsManager.ReleaseFilter("unknown", List.of("x64"), List.of("full"));
    }

    private String extractFileNameFromUrl(String downloadLink) {
        final String[] urlParts = downloadLink.split("/");
        return FilenameUtils.removeExtension(urlParts[urlParts.length - 1]);
    }

    private static class ReleaseInfo {
        private final String releaseVersion;
        private final String downloadLink;

        ReleaseInfo(String releaseVersion, String downloadLink) {
            this.releaseVersion = releaseVersion;
            this.downloadLink = downloadLink;
        }
    }

    public static class ReleaseFilter {
        private final String os;
        private final List<String> architectures;
        private final List<String> sizes;   // todo only one size(full-size), should change list to string

        public ReleaseFilter(String os, List<String> architectures, List<String> sizes) {
            this.os = os;
            this.architectures = architectures;
            this.sizes = sizes;
        }
    }

}


