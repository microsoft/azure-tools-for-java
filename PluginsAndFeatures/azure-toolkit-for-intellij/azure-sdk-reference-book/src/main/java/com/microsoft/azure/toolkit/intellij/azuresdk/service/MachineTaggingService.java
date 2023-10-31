/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * refer to com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.EnvironmentDependencyCollector
 */
public class MachineTaggingService {
    private static final List<String> EXECUTABLES = Arrays.asList(
        // container tools
        "docker",
        "kubectl",
        "podman",
        "terraform",

        // clouds
        "aws",
        "gcloud",
        "aliyun",
        "az",

        // services
        "mysql",
        "psql",
        "sqlservr",
        "redis-server",
        "redis-cli",
        "azurite",
        "Microsoft.Azure.Cosmos.Emulator",

        // other
        "azd",
        "vscode"
    );

    @Nonnull
    public static Set<String> getMachineTags(final Project project) {
        final List<Path> paths = getPathNames();
        return EXECUTABLES.stream()
            .filter(it -> hasToolInLocalPath(paths, it)).collect(Collectors.toSet());
    }

    private static boolean hasToolInLocalPath(List<Path> paths, String executableWithoutExt) {
        final var baseNames = SystemInfo.isWindows ?
            Stream.of(".bat", ".com", ".exe", ".cmd").map(exeSuffix -> executableWithoutExt + exeSuffix).toList() :
            Collections.singletonList(executableWithoutExt);
        return paths.stream().flatMap(p -> baseNames.stream().map(p::resolve))
            .filter(Files::isRegularFile)
            .anyMatch(Files::isExecutable);
    }

    private static List<Path> getPathNames() {
        final FileSystem fs = FileSystems.getDefault();
        return Arrays.stream(System.getenv("PATH").split(File.pathSeparator))
            .filter(StringUtils::isNotBlank)
            .map(it -> {
                try {
                    return fs.getPath(it);
                } catch (final InvalidPathException ignored) {
                }
                //noinspection ReturnOfNull
                return null;
            }).filter(Objects::nonNull).filter(Files::exists).toList();
    }
}
